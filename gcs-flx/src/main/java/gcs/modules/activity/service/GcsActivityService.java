package gcs.modules.activity.service;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import gcs.app.ugrp.service.GcsShiroService;
import gcs.app.vm.domain.MetaDomain;
import gcs.app.vm.utils.VmUtils;
import gcs.base.BaseDomain;
import gcs.base.core.GcsCoreService;
import gcs.base.shiro.spi.SpiUser;
import gcs.modules.activity.domain.GcsActivity;
import gcs.modules.activity.domain.GcsActivityLink;
import gcs.modules.activity.mapper.GcsActivityLinkMapper;
import gcs.modules.activity.mapper.GcsActivityMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.util.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class GcsActivityService {

    @Autowired
    private GcsCoreService gcsCoreService;

    @Autowired
    private GcsShiroService shiroService;

    @Autowired
    private GcsActivityMapper gcsActivityMapper;

    @Autowired
    private GcsActivityLinkMapper gcsActivityLinkMapper;

    /**
     * 活跃计划列表
     */
    @Transactional(readOnly = true)
    public List<GcsActivity> listActiveSchedules(List<Object> identList, Long userId) {
        // 获取全部活动
        LambdaQueryWrapper<GcsActivity> queryWrapper = Wrappers.lambdaQuery(GcsActivity.class)
                .eq(GcsActivity::getReferenceClass, identList.get(0))
                .eq(GcsActivity::getReferenceId, identList.get(1))
                .eq(GcsActivity::getAssigneeId, userId)
                .eq(GcsActivity::getIsActive, true)
                .isNotNull(GcsActivity::getDueDate)
                .orderByAsc(GcsActivity::getId);
        return gcsActivityMapper.selectList(queryWrapper);
    }

    @Transactional
    public List<Map<String, Object>> summarySchedule(Long userId) {
        // 统计分组数据
        QueryWrapper<GcsActivity> queryWrapper = Wrappers.query(GcsActivity.class);
        queryWrapper.select(
                "reference_class as reference",
                "CASE " +
                "   WHEN due_date < CURRENT_DATE THEN 'before' " +
                "   WHEN due_date >= CURRENT_DATE AND due_date < CURRENT_DATE + INTERVAL '1 day' THEN 'today' " +
                "   WHEN due_date >= CURRENT_DATE + INTERVAL '1 day' THEN 'future' " +
                "   ELSE 'unknown' END as date_type",
                "COUNT(*) as count"
        )
        .groupBy("reference", "date_type")
        .eq("assignee_id", userId)
        .eq("is_active", true)
        .isNotNull("due_date");
        List<Map<String, Object>> groupedRst = gcsActivityMapper.selectMaps(queryWrapper);
        // 变换分组数据
        Map<String, List<Map<String, Object>>> referenceGroup = groupedRst.stream()
                .collect(Collectors.groupingBy(item -> (String) item.get("reference")));
        List<Map<String, Object>> transResult = new ArrayList<>();
        for (Map.Entry<String, List<Map<String, Object>>> entry : referenceGroup.entrySet()) {
            String reference = entry.getKey();
            List<Map<String, Object>> refItems = entry.getValue();
            Map<String, Object> rstItem = new HashMap<>();
            rstItem.put("reference", reference);
            rstItem.put("before", 0);
            rstItem.put("today", 0);
            rstItem.put("future", 0);
            for (Map<String, Object> refItem : refItems) {
                String dateType = Convert.toStr(refItem.get("date_type"));
                Integer count = Convert.toInt(refItem.get("count"));
                rstItem.put(dateType, count);
            }
            transResult.add(rstItem);
        }
        // 完善引用标题
        for (Map<String, Object> rstItem : transResult) {
            String reference = (String) rstItem.get("reference");
            if (StringUtils.isNotBlank(reference)) {
                String camelDomainName = VmUtils.toUpperCamel(reference);
                MetaDomain metaDomain = Db.getOne(Wrappers.lambdaQuery(MetaDomain.class).eq(MetaDomain::getDomainName, camelDomainName));
                if (metaDomain != null) {
                    rstItem.put("referenceTitle", metaDomain.getTitle());
                }
            }
        }
        return transResult;
    }

    @Transactional
    public GcsActivity addNote(String entityIdentifier, GcsActivity gcsActivity) {
        BaseDomain baseDomain = (BaseDomain)gcsCoreService.getReference(entityIdentifier);
        return this.addNote(baseDomain, gcsActivity);
    }

    @Transactional
    public GcsActivity addNote(BaseDomain baseDomain, GcsActivity gcsActivity) {
        List<Object> identList = gcsCoreService.getReferenceIdents(baseDomain);
        // 获取操作人(默认未知)
        SpiUser actorUser = this.getActor();
        gcsActivity.setActorId(actorUser.getId());
        gcsActivity.setActorCaption(actorUser.getDisplayName());
        gcsActivity.setGroupId(actorUser.getGroupId());
        gcsActivity.setGroupCaption(actorUser.getGroupId().toString());
        // 保存计划
        gcsActivity.setReferenceClass((String) identList.get(0));
        gcsActivity.setReferenceId((Long) identList.get(1));
        gcsActivity.setIsActive(false);
        gcsActivity.setDueDate(null);
        gcsActivity.setFinishDate(DateUtil.date());
        // 创建更新
        Date nowDate = new Date();
        gcsActivity.setDateCreated(nowDate);
        gcsActivity.setLastUpdated(nowDate);
        gcsActivityMapper.insert(gcsActivity);
        return gcsActivity;
    }

    @Transactional
    public GcsActivity addSchedule(String entityIdentifier, GcsActivity gcsActivity) {
        BaseDomain baseDomain = (BaseDomain)gcsCoreService.getReference(entityIdentifier);
        return this.addSchedule(baseDomain, gcsActivity);
    }

    @Transactional
    public GcsActivity addSchedule(BaseDomain baseDomain, GcsActivity gcsActivity) {
        List<Object> identList = gcsCoreService.getReferenceIdents(baseDomain);
        // 获取操作人(默认未知)
        SpiUser actorUser = this.getActor();
        gcsActivity.setActorId(actorUser.getId());
        gcsActivity.setActorCaption(actorUser.getDisplayName());
        gcsActivity.setGroupId(actorUser.getGroupId());
        gcsActivity.setGroupCaption(actorUser.getGroupId().toString());
        // 保存计划
        gcsActivity.setReferenceClass((String) identList.get(0));
        gcsActivity.setReferenceId((Long) identList.get(1));
        gcsActivity.setIsActive(true);
        if(gcsActivity.getDueDate()==null){
            gcsActivity.setDueDate(DateUtil.date());
        }
        gcsActivity.setFinishDate(null);
        // 创建更新
        Date nowDate = new Date();
        gcsActivity.setDateCreated(nowDate);
        gcsActivity.setLastUpdated(nowDate);
        gcsActivityMapper.insert(gcsActivity);
        return gcsActivity;
    }

    @Transactional
    public GcsActivity finishSchedule(GcsActivity entity, String comment) {
        Long id = entity.getId();
        if(entity.getIsActive()){
            LambdaUpdateWrapper<GcsActivity> updateWrapper = Wrappers.lambdaUpdate(GcsActivity.class).eq(GcsActivity::getId, id);
            updateWrapper.set(GcsActivity::getIsActive, false); // 标记完成
            updateWrapper.set(GcsActivity::getFinishDate, DateUtil.date());
            updateWrapper.set(GcsActivity::getComment, comment);
            Db.update(updateWrapper);
            // 重新读取记录
            entity = Db.getById(id, GcsActivity.class);
        }
        return entity;
    }

    /**
     * 删除活动
     * @param activityId 活动ID
     */
    @Transactional
    public void delActivity(Long activityId) {
        // 获取活动
        GcsActivity activity = gcsActivityMapper.selectById(activityId);;
        if (activity != null) {
            // 删除关联
            gcsActivityLinkMapper.delete(Wrappers.lambdaQuery(GcsActivityLink.class).eq(GcsActivityLink::getActivityId, activityId));
            // 删除活动
            gcsActivityMapper.deleteById(activityId);
        }
    }

    /**
     * 按引用实体删除关联的所有活动
     * @param referenceClass 实体类名(小写首字母)，如 "demo"
     * @param referenceId 实体ID
     */
    @Transactional
    public void deleteByReference(String referenceClass, Long referenceId) {
        List<GcsActivity> activities = gcsActivityMapper.selectList(
                Wrappers.lambdaQuery(GcsActivity.class)
                        .eq(GcsActivity::getReferenceClass, referenceClass)
                        .eq(GcsActivity::getReferenceId, referenceId)
        );
        for (GcsActivity activity : activities) {
            gcsActivityLinkMapper.delete(Wrappers.lambdaQuery(GcsActivityLink.class)
                    .eq(GcsActivityLink::getActivityId, activity.getId()));
        }
        gcsActivityMapper.delete(Wrappers.lambdaQuery(GcsActivity.class)
                .eq(GcsActivity::getReferenceClass, referenceClass)
                .eq(GcsActivity::getReferenceId, referenceId));
    }

    /**
     * 按引用实体批量删除关联的活动
     * @param referenceClass 实体类名(小写首字母)
     * @param referenceIds 实体ID列表
     */
    @Transactional
    public void deleteByReference(String referenceClass, List<Long> referenceIds) {
        if (referenceIds == null || referenceIds.isEmpty()) {
            return;
        }
        List<GcsActivity> activities = gcsActivityMapper.selectList(
                Wrappers.lambdaQuery(GcsActivity.class)
                        .eq(GcsActivity::getReferenceClass, referenceClass)
                        .in(GcsActivity::getReferenceId, referenceIds)
        );
        for (GcsActivity activity : activities) {
            gcsActivityLinkMapper.delete(Wrappers.lambdaQuery(GcsActivityLink.class)
                    .eq(GcsActivityLink::getActivityId, activity.getId()));
        }
        gcsActivityMapper.delete(Wrappers.lambdaQuery(GcsActivity.class)
                .eq(GcsActivity::getReferenceClass, referenceClass)
                .in(GcsActivity::getReferenceId, referenceIds));
    }

    // 获取操作人
    private SpiUser getActor() {
        SpiUser spiUser = new SpiUser();
        try {
            if(ThreadContext.getSubject()!=null && SecurityUtils.getSubject().getPrincipal()!=null){
                spiUser = shiroService.currentUser();
            }
        } catch (Exception ex) {
            System.out.println("The gcs.modules.activity.service.GcsActivityService threw this exception: " + ex.getMessage());
        }
        return spiUser;
    }
}