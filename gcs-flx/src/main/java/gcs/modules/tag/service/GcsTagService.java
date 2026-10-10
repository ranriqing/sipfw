package gcs.modules.tag.service;

import cn.hutool.core.convert.Convert;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import gcs.base.core.GcsCoreService;
import gcs.modules.tag.domain.GcsTag;
import gcs.modules.tag.domain.GcsTagLink;
import gcs.modules.tag.mapper.GcsTagLinkMapper;
import gcs.modules.tag.mapper.GcsTagMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GcsTagService {

    @Autowired
    private GcsCoreService gcsCoreService;

    @Autowired
    private GcsTagMapper gcsTagMapper;

    @Autowired
    private GcsTagLinkMapper gcsTagLinkMapper;

    /**
     * 获取实体关联的所有标签名称
     * @param instance 实体对象
     * @return 标签名称列表
     */
    public List<String> listTags(Object instance) {
        List<Object> identList = gcsCoreService.getReferenceIdents(instance);
        Long referenceId = (Long)identList.get(1);
        String referenceClass = (String)identList.get(0);
        if (referenceId == null) {
            throw new RuntimeException("读取标签前需要先保存实体");
        }
        List<GcsTagLink> tagLinks = gcsTagLinkMapper.selectListByReference(referenceId, referenceClass, null);
        return tagLinks.stream()
                .map(GcsTagLink::getGcsTagName)
                .collect(Collectors.toList());
    }

    /**
     * 获取实体关联的所有标签（含颜色）
     * @param instance 实体对象
     * @return 标签列表 [{name, color}]
     */
    public List<Map<String, Object>> listTagDetails(Object instance) {
        List<Object> identList = gcsCoreService.getReferenceIdents(instance);
        Long referenceId = (Long)identList.get(1);
        String referenceClass = (String)identList.get(0);
        if (referenceId == null) {
            throw new RuntimeException("读取标签前需要先保存实体");
        }
        List<GcsTagLink> tagLinks = gcsTagLinkMapper.selectListByReference(referenceId, referenceClass, null);
        return tagLinks.stream().map(tagLink -> {
            Map<String, Object> map = new HashMap<>();
            map.put("name", tagLink.getGcsTagName());
            map.put("color", StringUtils.isBlank(tagLink.getGcsTagColor()) ? TAG_COLORS[0] : tagLink.getGcsTagColor());
            return map;
        }).collect(Collectors.toList());
    }

    /**
     * 为实体添加单个标签
     * @param instance 实体对象
     * @param tagName 标签名称
     * @return 实体对象
     */
    public Object addTag(Object instance, String tagName) {
        List<Object> identList = gcsCoreService.getReferenceIdents(instance);
        Long referenceId = (Long)identList.get(1);
        String referenceClass = (String)identList.get(0);
        if (referenceId == null) {
            throw new RuntimeException("添加标签前需要先保存实体");
        }

        // 获取或创建标签
        tagName = tagName.toLowerCase();
        GcsTag gcsTag = gcsTagMapper.selectOne(
            Wrappers.lambdaQuery(GcsTag.class).eq(GcsTag::getName, tagName)
        );
        if (gcsTag == null) {
            gcsTag = new GcsTag();
            gcsTag.setName(tagName);
            gcsTag.setColor(resolveTagColor(tagName));
            gcsTagMapper.insert(gcsTag);
        }

        // 检查标签关联是否已存在
        GcsTagLink gcsTagLink = gcsTagLinkMapper.selectOne(
            Wrappers.lambdaQuery(GcsTagLink.class)
                .eq(GcsTagLink::getGcsTagId, gcsTag.getId())
                .eq(GcsTagLink::getReferenceId, referenceId)
                .eq(GcsTagLink::getReferenceClass, referenceClass)
        );

        // 如果关联不存在则创建
        if (gcsTagLink == null) {
            gcsTagLink = new GcsTagLink();
            gcsTagLink.setGcsTagId(gcsTag.getId());
            gcsTagLink.setGcsTag(gcsTag);
            gcsTagLink.setReferenceId(referenceId);
            gcsTagLink.setReferenceClass(referenceClass);
            gcsTagLinkMapper.insert(gcsTagLink);
        }

        return instance;
    }

    /**
     * 为实体添加多个标签
     * @param instance 实体对象
     * @param tagNames 标签名称列表
     * @return 实体对象
     */
    public Object addTags(Object instance, List<String> tagNames) {
        if (tagNames != null) {
            for (String tagName : tagNames) {
                addTag(instance, tagName);
            }
        }
        return instance;
    }

    /**
     * 设置实体的标签，会移除不在列表中的旧标签
     * @param instance 实体对象
     * @param tagNames 要设置的标签列表
     * @return 实体对象
     */
    public Object setTags(Object instance, List<String> tagNames) {
        List<Object> identList = gcsCoreService.getReferenceIdents(instance);
        Long referenceId = (Long)identList.get(1);
        String referenceClass = (String)identList.get(0);
        // 读取当前关联标签
        List<GcsTagLink> tagLinks = gcsTagLinkMapper.selectListByReference(referenceId, referenceClass, null);
        // 标签转为小写并去空
        List<String> processedTagNames = null;
        if (tagNames != null) {
            processedTagNames = tagNames.stream().filter(StringUtils::isNotBlank).map(String::toLowerCase).collect(Collectors.toList());
        }
        // 更新为最新标签
        if (processedTagNames != null && !processedTagNames.isEmpty()) {
            // 迭代现有标签关联
            List<Long> toRemoveLinkIds = new ArrayList<>();
            for (GcsTagLink link : tagLinks) {
                String tagName = link.getGcsTagName();
                if (processedTagNames.contains(tagName)) {
                    // 如果标签仍在新列表中，从待添加列表中移除
                    processedTagNames.remove(tagName);
                } else {
                    // 如果标签不在新列表中，删除关联
                    toRemoveLinkIds.add(link.getId());
                }
            }
            // 添加剩余的新标签
            gcsTagLinkMapper.deleteByIds(toRemoveLinkIds);
            addTags(instance, processedTagNames);
        } else {
            // 如果没有提供新标签，删除所有现有标签关联
            gcsTagLinkMapper.deleteByIds(tagLinks.stream().map(GcsTagLink::getId).collect(Collectors.toList()));
        }
        return instance;
    }

    /**
     * 从实体移除指定标签
     * @param instance 实体对象
     * @param tagName 标签名称
     * @return 实体对象
     */
    public Object removeTag(Object instance, String tagName) {
        List<Object> identList = gcsCoreService.getReferenceIdents(instance);
        Long referenceId = (Long)identList.get(1);
        String referenceClass = (String)identList.get(0);
        if (referenceId == null) {
            throw new RuntimeException("移除标签前需要先保存实体");
        }

        // 删除指定标签的关联
        tagName = tagName.toLowerCase();
        List<GcsTagLink> tagLinks = gcsTagLinkMapper.selectListByReference(referenceId, referenceClass, tagName);
        if(!tagLinks.isEmpty()){
            gcsTagLinkMapper.deleteByIds(tagLinks.stream().map(GcsTagLink::getId).collect(Collectors.toList()));
        }
        return instance;
    }

    /**
     * 获取标签候选名称列表
     * @param referenceClass 实体引用类名，为空时返回全部标签名
     * @return 标签名称列表
     */
    public List<String> listTagNames(String referenceClass) {
        if (StringUtils.isBlank(referenceClass)) {
            return gcsTagMapper.selectList(
                    Wrappers.lambdaQuery(GcsTag.class).orderByAsc(GcsTag::getName)
            ).stream().map(GcsTag::getName).distinct().collect(Collectors.toList());
        }
        return gcsTagMapper.listNamesByReferenceClass(referenceClass);
    }

    /**
     * 构建标签过滤子查询 SQL（参考 PsPictureController.buildTagFilterSql）
     * @param referenceClass 实体引用类名
     * @param tagNames 标签名称列表
     * @return 子查询 SQL
     */
    public String buildTagFilterSql(String referenceClass, List<String> tagNames) {
        String names = tagNames.stream()
                .filter(StringUtils::isNotBlank)
                .map(name -> "'" + name.replace("'", "''") + "'")
                .collect(Collectors.joining(","));
        String refClass = referenceClass == null ? "" : referenceClass.replace("'", "''");
        return "SELECT reference_id FROM gcs_tag_link WHERE reference_class = '" + refClass + "' "
                + "AND gcs_tag_id IN (SELECT id FROM gcs_tag WHERE name IN (" + names + "))";
    }

    /** ----- 标签管理 ----- */

    /**
     * 删除标签（级联删除关联）
     * @param id 标签ID
     */
    @Transactional
    public void deleteTag(Long id) {
        gcsTagLinkMapper.delete(Wrappers.lambdaQuery(GcsTagLink.class).eq(GcsTagLink::getGcsTagId, id));
        gcsTagMapper.deleteById(id);
    }

    /**
     * 合并标签：源标签并入目标标签（去重 + 删除源标签）
     * @param sourceId 源标签ID（合并后删除）
     * @param targetId 目标标签ID（保留）
     */
    @Transactional
    public void mergeTag(Long sourceId, Long targetId) {
        GcsTag source = gcsTagMapper.selectById(sourceId);
        GcsTag target = gcsTagMapper.selectById(targetId);
        if (source == null || target == null) {
            throw new RuntimeException("标签不存在");
        }
        List<GcsTagLink> sourceLinks = gcsTagLinkMapper.selectList(
                Wrappers.lambdaQuery(GcsTagLink.class).eq(GcsTagLink::getGcsTagId, sourceId));
        for (GcsTagLink link : sourceLinks) {
            long exists = gcsTagLinkMapper.selectCount(
                    Wrappers.lambdaQuery(GcsTagLink.class)
                            .eq(GcsTagLink::getGcsTagId, targetId)
                            .eq(GcsTagLink::getReferenceId, link.getReferenceId())
                            .eq(GcsTagLink::getReferenceClass, link.getReferenceClass()));
            if (exists > 0) {
                // 同一记录已同时挂源与目标标签，去重（删除源 link）
                gcsTagLinkMapper.deleteById(link.getId());
            } else {
                link.setGcsTagId(targetId);
                gcsTagLinkMapper.updateById(link);
            }
        }
        gcsTagMapper.deleteById(sourceId);
    }

    /**
     * 批量获取标签使用数
     * @param ids 标签ID列表
     * @return 标签ID -> 使用数
     */
    public Map<Long, Long> usageCountByIds(List<Long> ids) {
        Map<Long, Long> result = new HashMap<>();
        if (ids == null || ids.isEmpty()) {
            return result;
        }
        gcsTagLinkMapper.countByTagIds(ids).forEach(map -> {
            Long tagId = Convert.toLong(map.get("gcs_tag_id"));
            Long cnt = Convert.toLong(map.get("cnt"), 0L);
            if (tagId != null) {
                result.put(tagId, cnt);
            }
        });
        return result;
    }

    /**
     * 标签名归一化：trim + 小写
     */
    public String normalizeName(String name) {
        return StringUtils.isBlank(name) ? null : name.trim().toLowerCase();
    }

    /**
     * 判断标签名是否已存在（大小写不敏感；excludeId 用于更新时排除自身）
     */
    public boolean nameExists(String name, Long excludeId) {
        if (StringUtils.isBlank(name)) {
            return false;
        }
        return gcsTagMapper.selectCount(
                Wrappers.lambdaQuery(GcsTag.class)
                        .eq(GcsTag::getName, name)
                        .ne(excludeId != null, GcsTag::getId, excludeId)
        ) > 0;
    }

    /** 标签预设颜色（与前端 a-tag 预设色一致） */
    private static final String[] TAG_COLORS = {"#f50", "#2db7f5", "#87d068", "#108ee9"};

    /**
     * 根据标签名稳定地分配一个颜色
     */
    private String resolveTagColor(String tagName) {
        return TAG_COLORS[Math.floorMod(tagName.hashCode(), TAG_COLORS.length)];
    }
}
