package gcs.app.vm.service;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.map.MapUtil;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import gcs.app.ugrp.service.GcsRasaService;
import gcs.app.vm.bean.DynVmClass;
import gcs.app.vm.bean.VmClass;
import gcs.app.vm.constants.VmConstants;
import gcs.app.vm.domain.MetaDomain;
import gcs.app.vm.domain.MetaDomainField;
import gcs.app.vm.mapper.MetaDomainFieldMapper;
import gcs.app.vm.mapper.MetaDomainMapper;
import gcs.base.BaseDomain;
import gcs.base.core.GcsConstants;
import gcs.base.core.condition.BaseCondition;
import gcs.base.core.condition.GcsCondition;
import gcs.base.core.condition.GcsRule;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 全局搜索聚合服务：跨动态领域按关键词（contains）与标签（_extend_tags）查询。
 */
@Slf4j
@Service
public class GlobalSearchService {

    private static final int MAX_DOMAINS = 20;
    private static final int DEFAULT_PAGE_SIZE = 5;

    @Autowired
    private MetaDomainMapper metaDomainMapper;
    @Autowired
    private MetaDomainFieldMapper metaDomainFieldMapper;
    @Autowired
    private VmClassService vmClassService;
    @Autowired
    private GcsRasaService gcsRasaService;

    public List<Map<String, Object>> search(String keyword, List<String> tagNames, List<String> domainNames) {
        List<String> domains = domainNames == null ? Collections.emptyList()
                : domainNames.stream().filter(StringUtils::isNotBlank).distinct().limit(MAX_DOMAINS).collect(Collectors.toList());
        List<String> tags = tagNames == null ? Collections.emptyList()
                : tagNames.stream().filter(StringUtils::isNotBlank).distinct().collect(Collectors.toList());

        boolean hasKeyword = StringUtils.isNotBlank(keyword);
        boolean hasTags = !tags.isEmpty();
        List<Map<String, Object>> groups = new ArrayList<>();
        if (domains.isEmpty() || (!hasKeyword && !hasTags)) {
            return groups;
        }

        // 一次查出 domain 与可搜字段，避免逐 domain 查询
        List<MetaDomain> metaDomains = metaDomainMapper.selectList(
                Wrappers.lambdaQuery(MetaDomain.class).in(MetaDomain::getDomainName, domains));
        Map<String, MetaDomain> domainByName = metaDomains.stream()
                .collect(Collectors.toMap(MetaDomain::getDomainName, Function.identity(), (a, b) -> a));

        Map<Long, List<String>> fieldNamesByDomainId = new HashMap<>();
        if (!metaDomains.isEmpty()) {
            List<Long> domainIds = metaDomains.stream().map(MetaDomain::getId).collect(Collectors.toList());
            List<MetaDomainField> fields = metaDomainFieldMapper.selectList(
                    Wrappers.lambdaQuery(MetaDomainField.class)
                            .eq(MetaDomainField::getGlobalSearch, true)
                            .in(MetaDomainField::getFieldType, VmConstants.FIELD_TYPE_STRING)
                            .in(MetaDomainField::getMetaDomainId, domainIds));
            for (MetaDomainField field : fields) {
                fieldNamesByDomainId.computeIfAbsent(field.getMetaDomainId(), k -> new ArrayList<>()).add(field.getFieldName());
            }
        }

        for (String domainName : domains) {
            MetaDomain metaDomain = domainByName.get(domainName);
            if (metaDomain == null) {
                continue;
            }
            try {
                VmClass vmClass = vmClassService.doResolveVmClass(domainName);
                if (!(vmClass instanceof DynVmClass)) {
                    continue;
                }
                List<String> fieldNames = fieldNamesByDomainId.getOrDefault(metaDomain.getId(), Collections.emptyList());
                if (hasKeyword && fieldNames.isEmpty()) {
                    continue;
                }

                GcsCondition condition = new GcsCondition();
                condition.setVmClass(vmClass);
                condition.setPageNum(1);
                condition.setPageSize(DEFAULT_PAGE_SIZE);

                List<GcsRule> rules = new ArrayList<>();
                if (hasKeyword) {
                    condition.setCondition(BaseCondition.CONDITION_OR);
                    for (String fieldName : fieldNames) {
                        GcsRule rule = new GcsRule();
                        rule.setOperator(BaseCondition.OPERATION_CONTAINS);
                        rule.setField(fieldName);
                        rule.setValue(keyword);
                        rules.add(rule);
                    }
                }
                if (hasTags) {
                    GcsRule tagRule = new GcsRule();
                    tagRule.setField("_extend_tags");
                    tagRule.setValue(tags);
                    rules.add(tagRule);
                }
                condition.setRules(rules);

                BaseMapper vmMapper = vmClass.getDomainMapper();
                IPage<? extends BaseDomain> page = vmMapper.selectPage(
                        condition.buildQueryPage(),
                        gcsRasaService.query(
                                condition.buildQueryWrapper(vmClass.getDomainClass()),
                                vmClass.getDomainName(),
                                MapUtil.of("vmClass", vmClass)));

                List<Map<String, Object>> items = new ArrayList<>();
                for (BaseDomain entity : page.getRecords()) {
                    Map<String, Object> entityMap = vmClass.vmDomainEntityMap(entity);
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", entityMap.get(BaseDomain.ID));

                    List<String> values = new ArrayList<>();
                    for (String fieldName : fieldNames) {
                        Object value = entityMap.get(fieldName);
                        if (value != null && StringUtils.isNotBlank(Convert.toStr(value))) {
                            values.add(Convert.toStr(value));
                            if (values.size() >= 2) {
                                break;
                            }
                        }
                    }
                    item.put("values", values);

                    List<String> tagNameList = new ArrayList<>();
                    Object tagsObj = entityMap.get(GcsConstants.TAGS);
                    if (tagsObj instanceof List) {
                        for (Object tag : (List<?>) tagsObj) {
                            if (tag instanceof Map) {
                                Object name = ((Map<?, ?>) tag).get("name");
                                if (name != null) {
                                    tagNameList.add(Convert.toStr(name));
                                }
                            } else if (tag != null) {
                                tagNameList.add(Convert.toStr(tag));
                            }
                        }
                    }
                    item.put("tags", tagNameList);
                    items.add(item);
                }

                Map<String, Object> group = new HashMap<>();
                group.put("domainName", domainName);
                group.put("domainTitle", metaDomain.getTitle());
                group.put("total", page.getTotal());
                group.put("hasMore", page.getTotal() > DEFAULT_PAGE_SIZE);
                group.put("items", items);
                groups.add(group);
            } catch (Exception e) {
                log.warn("全局搜索 domain [{}] 查询失败: {}", domainName, e.getMessage());
            }
        }
        return groups;
    }
}
