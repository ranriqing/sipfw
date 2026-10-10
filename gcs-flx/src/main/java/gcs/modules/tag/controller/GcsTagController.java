package gcs.modules.tag.controller;

import cn.hutool.core.convert.Convert;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import gcs.base.core.bean.GcsResult;
import gcs.base.core.condition.GcsCondition;
import gcs.base.tools.GcsBindingUtils;
import gcs.modules.tag.domain.GcsTag;
import gcs.modules.tag.mapper.GcsTagMapper;
import gcs.modules.tag.service.GcsTagService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/gcsTag")
public class GcsTagController extends ServiceImpl<GcsTagMapper, GcsTag> {

    @Autowired
    private GcsTagService gcsTagService;

    /**
     * 标签候选名称列表（按 referenceClass 过滤，供 GLinkTag 下拉）
     */
    @RequestMapping(value = "/names", method = {RequestMethod.GET, RequestMethod.POST})
    public GcsResult names(@RequestBody(required = false) Optional<GcsCondition> payload) {
        GcsCondition gcsCondition = payload.orElse(new GcsCondition());
        String referenceClass = gcsCondition.getRules().stream()
                .filter(rule -> "referenceClass".equals(rule.getField()))
                .findFirst()
                .map(rule -> Convert.toStr(rule.getValue()))
                .orElse(null);
        List<String> tagNames = gcsTagService.listTagNames(referenceClass);
        return GcsResult.successResult().singleData(tagNames);
    }

    /**
     * 标签分页列表（含使用数）
     */
    @RequestMapping(value = "/index", method = {RequestMethod.GET, RequestMethod.POST})
    public GcsResult index(@RequestBody(required = false) Optional<GcsCondition> payload) {
        GcsCondition gcsCondition = payload.orElse(new GcsCondition());
        IPage<GcsTag> page = this.page(
                gcsCondition.buildQueryPage(),
                gcsCondition.buildQueryWrapper(GcsTag.class)
        );
        List<Long> ids = page.getRecords().stream().map(GcsTag::getId).collect(Collectors.toList());
        Map<Long, Long> usageCountMap = gcsTagService.usageCountByIds(ids);
        page.getRecords().forEach(tag -> tag.setUsageCount(usageCountMap.getOrDefault(tag.getId(), 0L)));
        page.convert(GcsTagController::_entityMap);
        return GcsResult.successResult().wrapPage(page);
    }

    /**
     * 保存标签
     */
    @PostMapping("/save")
    public GcsResult save(@RequestBody Map<String, Object> jsonMap) {
        GcsTag entity = GcsBindingUtils.bind(jsonMap, GcsTag.class);
        String name = gcsTagService.normalizeName(entity.getName());
        if (StringUtils.isBlank(name)) {
            return GcsResult.paramInvalidResult("标签名称不能为空");
        }
        entity.setName(name);
        if (gcsTagService.nameExists(name, null)) {
            return GcsResult.paramInvalidResult("标签已存在");
        }
        this.save(entity);
        return GcsResult.successResult().putAll(_entityMap(entity));
    }

    /**
     * 更新标签（改名/改色）
     */
    @PutMapping("/update/{id}")
    public GcsResult update(@PathVariable("id") Long id, @RequestBody Map<String, Object> jsonMap) {
        GcsTag entity = this.getById(id);
        if (entity == null) {
            return GcsResult.notExistResult().put("id", id);
        }
        GcsBindingUtils.bind(jsonMap, entity);
        String name = gcsTagService.normalizeName(entity.getName());
        if (StringUtils.isBlank(name)) {
            return GcsResult.paramInvalidResult("标签名称不能为空");
        }
        entity.setName(name);
        if (gcsTagService.nameExists(name, id)) {
            return GcsResult.paramInvalidResult("标签已存在");
        }
        this.updateById(entity);
        return GcsResult.successResult().putAll(_entityMap(entity));
    }

    /**
     * 删除标签（级联删除关联）
     */
    @DeleteMapping("/delete/{id}")
    public GcsResult delete(@PathVariable("id") Long id) {
        GcsTag entity = this.getById(id);
        if (entity == null) {
            return GcsResult.notExistResult().put("id", id);
        }
        gcsTagService.deleteTag(id);
        return GcsResult.successResult().put("id", id);
    }

    /**
     * 合并标签：源标签并入目标标签
     */
    @PostMapping("/merge")
    public GcsResult merge(@RequestBody Map<String, Object> jsonMap) {
        Long sourceId = Convert.toLong(jsonMap.get("sourceId"));
        Long targetId = Convert.toLong(jsonMap.get("targetId"));
        if (sourceId == null || targetId == null || sourceId.equals(targetId)) {
            return GcsResult.paramInvalidResult("无效的合并参数");
        }
        gcsTagService.mergeTag(sourceId, targetId);
        return GcsResult.successResult();
    }

    /** ----- 工具方法 ----- */
    public static Map<String, Object> _entityMap(GcsTag entity) {
        Map<String, Object> entityMap = new HashMap<>();
        entityMap.put("id", entity.getId());
        entityMap.put("name", entity.getName());
        entityMap.put("color", entity.getColor());
        entityMap.put("seqOrder", entity.getSeqOrder());
        entityMap.put("usageCount", entity.getUsageCount());
        return entityMap;
    }
}
