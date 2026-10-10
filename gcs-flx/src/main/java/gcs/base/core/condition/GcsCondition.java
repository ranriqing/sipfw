package gcs.base.core.condition;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.base.CaseFormat;
import gcs.app.vm.bean.VmClass;
import gcs.app.vm.bean.VmClassField;
import gcs.base.BaseDomain;
import gcs.base.core.GcsCoreService;
import gcs.base.core.GcsHolder;
import gcs.base.shiro.spi.SpiUser;
import gcs.app.ugrp.service.GcsShiroService;
import gcs.modules.tag.service.GcsTagService;
import gcs.app.vm.component.fields.LinkUserClassField;
import gcs.app.vm.constants.VmConstants;
import gcs.base.core.bean.GcsMap;
import gcs.base.core.condition.convertors.RuleConvertor;
import gcs.base.tools.GcsBindingUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.ReflectUtil;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 通过Json自动绑定
 * eg:
 * ['condition':'or', 'rules': [
 *      [field: 'name', operator:'is_null'],
 *      ['condition':'or',
 *          'rules': [
 *              [field: 'name1', operator:'is_null'],
 *              [field: 'name2', operator:'equal', value: 'value2']
 *          ]
 *      ]
 * ] ] as JSON
 */
@Slf4j
@Data
public class GcsCondition extends BaseCondition{

    private static Integer MAX_PAGE_SIZE = 1000;
    // 分页 && 排序
    private Boolean paginate = true;      // 分页开关 - 分页开启开关
    private Integer pageNum = 1;          // 当前页号 - 从1开始
    private Integer pageSize = 30;        // 页面大小 - 每页数据量
    private String orderBy;               // 排序字段

    // 查询条件
    private VmClass vmClass;
    private String condition = CONDITION_AND;
    private List<GcsRule> rules = new ArrayList<>();

    // 关联字段批量加载：需回填的关联字段名列表（MANY_TO_ONE / ONE_TO_MANY）
    private List<String> enrichFields;

    public Integer getOffset() {
        return (pageNum - 1) * pageSize;
    }

    public Map<String, String> getOrderByMap() {
        Map<String, String> orderBys = new HashMap<>();
        orderBys.put("id", "desc");
        // 自定义排序
        if (this.orderBy!=null) {
            orderBys.clear();
            try {
                for (String groupStr : orderBy.split(",")) {
                    String[] array = groupStr.split("-");
                    orderBys.put(array[0].trim(), array[1].trim());
                }
            } catch (Exception e) {
                // ignore
            }
        }
        return orderBys;
    }

    public <T> IPage<T> buildQueryPage() {
        return new Page<>(pageNum, paginate?pageSize:MAX_PAGE_SIZE);
    }

    public GcsMap buildQueryMap() {
        GcsMap resultMap = new GcsMap();
        if (rules != null && !rules.isEmpty()) {
            for (GcsRule rule : flattenRules(rules)) {
                String fieldName = rule.getField();
                String operator = rule.getOperator();
                Object value = rule.getValue();
                if (fieldName != null || value != null) {
                    resultMap.put(fieldName, value);
                }
            }
        }
        return resultMap;
    }

    public <T> T buildQueryEntity(Class<T> domainClazz) {
        Map<String, Object> resultMap = new HashMap<>();
        if (rules != null && !rules.isEmpty()) {
            for (GcsRule rule : flattenRules(rules)) {
                String fieldName = rule.getField();
                String operator = rule.getOperator();
                Object value = rule.getValue();
                if (fieldName != null || value != null) {
                    resultMap.put(fieldName, value);
                }
            }
        }
        return GcsBindingUtils.bind(resultMap, domainClazz);
    }


    public <T> QueryWrapper<T> buildQueryWrapper(Class<T> domainClazz){
        return this.buildQueryWrapper(domainClazz, true);
    }

    public <T> QueryWrapper<T> buildQueryWrapper(Class<T> domainClazz, Boolean orderBy){
        return buildQueryWrapper(domainClazz, orderBy, () -> Wrappers.query(domainClazz));
    }

    /**
     * @param wrapperFactory 生成 QueryWrapper 的工厂，缺省即 MP 原生 {@link QueryWrapper}。
     *        目前唯一需要传子类的场合是派生列表达式（如 {@code to_char(start_stamp,'HH24')}）：
     *        MP 3.5.11 的注入启发式把它误判成注入，而它是调用方的编译期常量、不进 WHERE，
     *        只能由子类按白名单放行 —— 见 {@code gcs.app.callcenter.util.CcDerivedQueryWrapper}。
     */
    public <T> QueryWrapper<T> buildQueryWrapper(Class<T> domainClazz, Boolean orderBy,
                                                 Supplier<QueryWrapper<T>> wrapperFactory){
        String conditionStr = condition!=null?(CONDITION_OR.equals(condition.toLowerCase())?CONDITION_OR: CONDITION_AND):CONDITION_AND;
        QueryWrapper<T> queryWrapper = wrapperFactory.get();
        queryWrapper.checkSqlInjection();
        if (CONDITION_OR.equals(conditionStr)) {
            for (GcsRule gcsRule : rules) {
                if (gcsRule.getCondition() != null && gcsRule.getRules() != null) {
                    queryWrapper.or(!gcsRule.getRules().isEmpty(), wrapper -> {
                        if (CONDITION_OR.equals(gcsRule.getCondition())) {
                            wrapper.and(!gcsRule.getRules().isEmpty(), subWrapper -> {
                                for (GcsRule subVmRule : gcsRule.getRules()) {
                                    String typeStr = resolvePropertyType(domainClazz, subVmRule.getField());
                                    subWrapper.or(typeStr!=null, thirdWrapper -> {
                                        gcsRuleToClosure(thirdWrapper, subVmRule, typeStr);
                                    });
                                }
                            });
                        } else {
                            wrapper.and(!gcsRule.getRules().isEmpty(), subWrapper -> {
                                for (GcsRule subVmRule : gcsRule.getRules()) {
                                    String typeStr = resolvePropertyType(domainClazz, subVmRule.getField());
                                    gcsRuleToClosure(subWrapper, subVmRule, typeStr);
                                }
                            });
                        }
                    });
                } else {
                    String typeStr = resolvePropertyType(domainClazz, gcsRule.getField());
                    queryWrapper.or(typeStr!=null, wrapper -> {
                        gcsRuleToClosure(wrapper, gcsRule, typeStr);
                    });
                }
            }
        } else {
            for (GcsRule gcsRule : rules) {
                if (gcsRule.getCondition() != null && gcsRule.getRules() != null) {
                    queryWrapper.and(!gcsRule.getRules().isEmpty(), wrapper -> {
                        if (CONDITION_OR.equals(gcsRule.getCondition())) {
                            wrapper.and(!gcsRule.getRules().isEmpty(), subWrapper -> {
                                for (GcsRule subVmRule : gcsRule.getRules()) {
                                    String typeStr = resolvePropertyType(domainClazz, subVmRule.getField());
                                    subWrapper.or(typeStr!=null, thirdWrapper -> {
                                        gcsRuleToClosure(thirdWrapper, subVmRule, typeStr);
                                    });
                                }
                            });
                        } else {
                            wrapper.and(!gcsRule.getRules().isEmpty(), subWrapper -> {
                                for (GcsRule subVmRule : gcsRule.getRules()) {
                                    String typeStr = resolvePropertyType(domainClazz, subVmRule.getField());
                                    gcsRuleToClosure(subWrapper, subVmRule, typeStr);
                                }
                            });
                        }
                    });
                } else {
                    String typeStr = resolvePropertyType(domainClazz, gcsRule.getField());
                    queryWrapper.and(typeStr!=null, wrapper -> {
                        gcsRuleToClosure(wrapper, gcsRule, typeStr);
                    });
                }
            }
        }
        // 标签过滤（_extend_tags 子查询，参考 buildTagFilterSql）
        GcsRule tagsRule = rules.stream()
                .filter(rule -> "_extend_tags".equals(rule.getField()))
                .findFirst().orElse(null);
        if (tagsRule != null && tagsRule.getValue() != null) {
            List<String> tagNames;
            if (tagsRule.getValue() instanceof List) {
                tagNames = (List<String>) tagsRule.getValue();
            } else {
                tagNames = Collections.singletonList(Convert.toStr(tagsRule.getValue()));
            }
            if (!tagNames.isEmpty()) {
                GcsCoreService gcsCoreService = GcsHolder.getBean(GcsCoreService.class);
                GcsTagService gcsTagService = GcsHolder.getBean(GcsTagService.class);
                String referenceClass = gcsCoreService.toReferenceClass(domainClazz);
                queryWrapper.inSql(BaseDomain.ID, gcsTagService.buildTagFilterSql(referenceClass, tagNames));
            }
        }
        // 数据排序
        if(orderBy!=null && orderBy){
            Map<String, String> orderMap = this.getOrderByMap();
            if(orderMap.containsKey("activities") && this.getVmClass() != null){
                // activities 特殊处理 → 统一走 last() 注入子查询 ORDER BY
                StringBuilder orderSql = new StringBuilder(" ORDER BY ");
                for (Map.Entry<String, String> entry : orderMap.entrySet()) {
                    if(orderSql.length() > 11) orderSql.append(", ");
                    if("activities".equals(entry.getKey())){
                        String domainName = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, vmClass.getDomainName());
                        String refClass = vmClass.getDomainClass().getSimpleName().split("_")[0];
                        refClass = refClass.substring(0, 1).toLowerCase() + refClass.substring(1);
                        Long assigneeId = null;
                        try {
                            GcsShiroService gcsShiroService = GcsHolder.getBean(GcsShiroService.class);
                            SpiUser spiUser = gcsShiroService.currentUser();
                            assigneeId = spiUser != null ? spiUser.getId() : null;
                        } catch (Exception e) {
                            // 非 Web 上下文或未登录时忽略用户过滤
                        }
                        String assigneeFilter = assigneeId != null
                            ? String.format(" AND ga.assignee_id = %d", assigneeId)
                            : "";
                        orderSql.append(String.format(
                            "(SELECT MIN(ga.due_date) FROM gcs_activity ga " +
                            "WHERE ga.reference_class = '%s' AND ga.reference_id = %s.id " +
                            "AND ga.is_active = TRUE AND ga.due_date IS NOT NULL%s)",
                            refClass, domainName, assigneeFilter
                        ));
                    }else{
                        String columnField = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, entry.getKey());
                        orderSql.append(columnField);
                    }
                    orderSql.append(" ").append(entry.getValue());
                }
                queryWrapper.last(orderSql.toString());
            }else{
                orderMap.forEach((field, order) ->{
                    String columnField = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, field);
                    queryWrapper.orderBy(true, "asc".equals(order), columnField);
                });
            }
        }
        return queryWrapper;
    }

    private String resolvePropertyType(Class<?> domainClazz, String field) {
        if(field!=null && field.startsWith("_extend")){
            return null;
        }
        if(this.getVmClass()!=null) {
            VmClassField vmClassField = this.getVmClass().getClassFieldMap().get(field);
            if(vmClassField!=null && VmConstants.FIELD_TYPE_ONE2MANY.equals(vmClassField.getFieldType())){
                return List.class.getSimpleName();
            }
        }
        Field declaredField = ReflectUtil.getField(domainClazz, field);
        if (declaredField != null) {
            return declaredField.getType().getSimpleName();
        }
        log.warn("未找到字段: " + field + " 在类 " + domainClazz.getName() + " 中");
        return null;
    }

    // 假设 gcsRuleToClosure 方法的实现
    private void gcsRuleToClosure(QueryWrapper<?> wrapper, GcsRule gcsRule, String typeStr) {
        log.debug("gcsRuleToClosure - type:" + typeStr + " - field:" + gcsRule.field + " - value:" + gcsRule.value);
        if(gcsRule == null || typeStr == null){ return; }
        // 支持按字段类型、按VmClassField类型解析RuleConvertor
        Map<String, Object> context = null;
        RuleConvertor ruleConvertor = RuleConvertorRegistry.resolveConvertor(typeStr);
        if(this.getVmClass()!=null){
            VmClassField vmClassField = this.getVmClass().getClassFieldMap().get(gcsRule.field);
            if(vmClassField!=null){
                RuleConvertor vmFieldRuleConvertor = RuleConvertorRegistry.resolveConvertor(vmClassField.getSupportedType());
                if(vmFieldRuleConvertor!=null){
                    ruleConvertor = vmFieldRuleConvertor;
                    context = new HashMap<>();
                    context.put(VmConstants.VM_CLASS_FIELD, vmClassField);
                }
            }
        }
        String columnField = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, gcsRule.field);
        if(ruleConvertor!=null){
            if(OPERATION_EQUAL.equals(gcsRule.operator)){
                ruleConvertor.convertEqual(wrapper, columnField, gcsRule.value, context);
            }else if(OPERATION_NOT_EQUAL.equals(gcsRule.operator)){
                ruleConvertor.convertNotEqual(wrapper, columnField, gcsRule.value, context);
            }else if(OPERATION_CONTAINS.equals(gcsRule.operator)){
                ruleConvertor.convertContains(wrapper, columnField, gcsRule.value, context);
            }else if(OPERATION_BEGINS_WITH.equals(gcsRule.operator)){
                ruleConvertor.convertBeginsWith(wrapper, columnField, gcsRule.value, context);
            }else if(OPERATION_ENDS_WITH.equals(gcsRule.operator)){
                ruleConvertor.convertEndsWith(wrapper, columnField, gcsRule.value, context);
            }else if(OPERATION_IN.equals(gcsRule.operator)){
                ruleConvertor.convertIn(wrapper, columnField, gcsRule.value, context);
            }else if(OPERATION_IDSUB_IN.equals(gcsRule.operator)){
                ruleConvertor.convertIdsubIn(wrapper, columnField, gcsRule.value, context);
            }else if(OPERATION_LESS.equals(gcsRule.operator)){
                ruleConvertor.convertLess(wrapper, columnField, gcsRule.value, context);
            }else if(OPERATION_LESS_OR_EQUAL.equals(gcsRule.operator)){
                ruleConvertor.convertLessOrEqual(wrapper, columnField, gcsRule.value, context);
            }else if(OPERATION_GREATER.equals(gcsRule.operator)){
                ruleConvertor.convertGreater(wrapper, columnField, gcsRule.value, context);
            }else if(OPERATION_GREATER_OR_EQUAL.equals(gcsRule.operator)){
                ruleConvertor.convertGreaterOrEqual(wrapper, columnField, gcsRule.value, context);
            }else if(OPERATION_BETWEEN.equals(gcsRule.operator)){
                ruleConvertor.convertBetween(wrapper, columnField, gcsRule.value, context);
            }else if(OPERATION_IS_NULL.equals(gcsRule.operator)){
                ruleConvertor.convertIsNull(wrapper, columnField, gcsRule.value, context);
            }else if(OPERATION_IS_NOT_NULL.equals(gcsRule.operator)){
                ruleConvertor.convertIsNotNull(wrapper, columnField, gcsRule.value, context);
            }else{
                log.warn("Operator [" + gcsRule.operator + "] not supported now");
            }
        }
    }

    // 将嵌套规则展平为一维列表
    private List<GcsRule> flattenRules(List<GcsRule> rules) {
        List<GcsRule> flatRules = new ArrayList<>();
        if (rules == null) return flatRules;
        for (GcsRule rule : rules) {
            if (rule.getRules() != null && !rule.getRules().isEmpty()) {
                flatRules.addAll(flattenRules(rule.getRules()));
            } else {
                flatRules.add(rule);
            }
        }
        return flatRules;
    }
}
