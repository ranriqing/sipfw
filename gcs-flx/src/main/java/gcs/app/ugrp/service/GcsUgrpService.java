package gcs.app.ugrp.service;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.lang.Assert;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import gcs.app.ugrp.beans.UgrpGroup;
import gcs.app.ugrp.beans.UgrpUser;
import gcs.app.ugrp.domain.GcsGroup;
import gcs.app.ugrp.domain.GcsRole;
import gcs.app.ugrp.domain.GcsUser;
import gcs.base.core.GcsConstants;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@Service
public class GcsUgrpService {

    // 多租户缓存：key=tenantId，value=（key=userId/groupId，value=缓存对象）
    private static final Map<String, Map<Long, UgrpUser>> tenantUgrpUserCacheMap =
            new ConcurrentHashMap<>();
    private static final Map<String, Map<Long, UgrpGroup>> tenantUgrpGroupCacheMap =
            new ConcurrentHashMap<>();

    // 初始化缓存默认值（模拟withDefault）
    private static final Function<String, Map<Long, UgrpUser>> USER_CACHE_DEFAULT =
            key -> new ConcurrentHashMap<>();
    private static final Function<String, Map<Long, UgrpGroup>> GROUP_CACHE_DEFAULT =
            key -> new ConcurrentHashMap<>();

    /**
     * 用户缓存清理
     */
    public UgrpUser invalidUser(Long userId) {
        Map<Long, UgrpUser> userCache = getOrCreateUserCache(GcsConstants.TENANT_ID);
        return userCache.remove(userId);
    }

    /**
     * 组缓存清理
     */
    public UgrpGroup invalidGroup(Long groupId) {
        Map<Long, UgrpGroup> groupCache = getOrCreateGroupCache(GcsConstants.TENANT_ID);
        return groupCache.remove(groupId);
    }

    /**
     * 获取缓存的 UgrpUser
     */
    public UgrpUser getUgrpUser(Long userId) {
        userId = userId == null ? 0L : userId;
        Map<Long, UgrpUser> userCache = getOrCreateUserCache(GcsConstants.TENANT_ID);
        if (userCache.containsKey(userId)) {
            return userCache.get(userId);
        }

        GcsUser gcsUser = Db.getById(userId, GcsUser.class);
        UgrpUser ugrpUser = new UgrpUser();
        ugrpUser.setId(userId);
        // 用户信息
        ugrpUser.setUsername(gcsUser!=null?gcsUser.getUsername():"");
        ugrpUser.setNickname(gcsUser!=null?gcsUser.getNickname():"");
        ugrpUser.setMobile(gcsUser!=null?gcsUser.getMobile():"");
        ugrpUser.setEmail(gcsUser!=null?gcsUser.getEmail():"");
        ugrpUser.setAvatar(gcsUser!=null?gcsUser.getAvatar():"avatar.png");
        // 关联关系
        Long groupId = (gcsUser != null && gcsUser.getGcsGroupId() != null) ? gcsUser.getGcsGroupId() : Long.valueOf(0L);
        Long roleId = (gcsUser != null && gcsUser.getGcsRoleId() != null) ? gcsUser.getGcsRoleId() : Long.valueOf(0L);
        ugrpUser.setGroupId(groupId);
        ugrpUser.setRoleId(roleId);

        GcsGroup gcsGroup = Db.getById(groupId, GcsGroup.class);
        ugrpUser.setGroupCaption(gcsGroup!=null?gcsGroup.getName():"其它");
        GcsRole gcsRole = Db.getById(roleId, GcsRole.class);
        ugrpUser.setRoleCaption(gcsRole!=null?gcsRole.getName():"其它");

        // 放入缓存
        userCache.put(userId, ugrpUser);
        return ugrpUser;
    }

    /**
     * 获取缓存的 UgrpGroup（无则从数据库加载）
     */
    public UgrpGroup getUgrpGroup(Long groupId) {
        groupId = groupId == null ? 0L : groupId;
        Map<Long, UgrpGroup> groupCache = getOrCreateGroupCache(GcsConstants.TENANT_ID);
        if (groupCache.containsKey(groupId)) {
            return groupCache.get(groupId);
        }

        GcsGroup gcsGroup = Db.getById(groupId, GcsGroup.class);
        UgrpGroup ugrpGroup = new UgrpGroup();
        ugrpGroup.setId(groupId);
        ugrpGroup.setName(gcsGroup!=null?gcsGroup.getName():"其它");
        Integer sequence = (gcsGroup != null && gcsGroup.getSequence() != null) ? gcsGroup.getSequence() : Integer.valueOf(100);
        ugrpGroup.setSequence(sequence);
        ugrpGroup.setParentId(gcsGroup!=null?gcsGroup.getParentId():null);

        // 放入缓存
        groupCache.put(groupId, ugrpGroup);
        return ugrpGroup;
    }

    /**
     * 按层级获取 UgrpGroup（0=顶层，依次向下）
     */
    public UgrpGroup getLevelGroup(Long userId, Integer groupLevel) {
        // 默认为1级
        groupLevel = groupLevel==null?1:groupLevel;
        UgrpUser ugrpUser = getUgrpUser(userId);
        Assert.notNull(ugrpUser);

        Long groupId = ugrpUser.getGroupId();
        // 递归获取组层级（子→父），反转后变为（父→子）
        List<UgrpGroup> groupList = _recurseUgrpGroupList(new ArrayList<>(), GcsConstants.TENANT_ID, groupId);
        groupList = ListUtil.reverse(groupList);

        // 计算索引（groupLevel=0 对应索引0，以此类推）
        int groupIndex = groupLevel>0?groupLevel:0;
        if (groupList.size() > groupIndex) {
            return groupList.get(groupIndex);
        } else {
            // 层级不存在时返回默认组
            UgrpGroup defaultGroup = new UgrpGroup();
            defaultGroup.setId(0L);
            defaultGroup.setName("其它");
            defaultGroup.setSequence(100);
            defaultGroup.setParentId(null);
            return defaultGroup;
        }
    }

    /**
     * 获取所有子组ID（递归）
     * @param groupId     当前组ID
     * @param includeSelf 是否包含当前组
     * @return 子组ID集合
     */
    public Set<Long> getSubGroupIds(Long groupId, boolean includeSelf) {
        Set<Long> result = new HashSet<>();
        if (includeSelf && groupId != null) {
            result.add(groupId);
        }
        _collectSubGroupIds(groupId, result);
        return result;
    }

    /** ------------------------------ 工具方法 ------------------------------ */
    // 递归收集子组ID（从当前组向下遍历）
    private void _collectSubGroupIds(Long parentId, Set<Long> result) {
        if (parentId == null) return;
        List<GcsGroup> children = Db.lambdaQuery(GcsGroup.class)
                .eq(GcsGroup::getParentId, parentId)
                .list();
        for (GcsGroup child : children) {
            if (result.add(child.getId())) {
                _collectSubGroupIds(child.getId(), result);
            }
        }
    }

    // 递归获取组层级（从当前组向上追溯到顶层）
    private List<UgrpGroup> _recurseUgrpGroupList(List<UgrpGroup> groupList, String tenantId, Long groupId) {
        groupId = groupId == null ? 0L : groupId;
        UgrpGroup ugrpGroup = getUgrpGroup(groupId); // 复用缓存逻辑
        groupList.add(ugrpGroup);
        // 有父组则继续递归
        if (ugrpGroup.getParentId() != null && ugrpGroup.getParentId() != 0L) {
            _recurseUgrpGroupList(groupList, tenantId, ugrpGroup.getParentId());
        }
        return groupList;
    }

    // 获取或创建用户缓存
    private Map<Long, UgrpUser> getOrCreateUserCache(String tenantId) {
        return tenantUgrpUserCacheMap.computeIfAbsent(tenantId, USER_CACHE_DEFAULT);
    }

    // 获取或创建组缓存
    private Map<Long, UgrpGroup> getOrCreateGroupCache(String tenantId) {
        return tenantUgrpGroupCacheMap.computeIfAbsent(tenantId, GROUP_CACHE_DEFAULT);
    }
}