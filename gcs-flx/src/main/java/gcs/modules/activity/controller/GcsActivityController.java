package gcs.modules.activity.controller;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import gcs.app.ugrp.service.GcsShiroService;
import gcs.base.BaseController;
import gcs.base.BaseDomain;
import gcs.base.core.GcsConstants;
import gcs.base.core.GcsCoreService;
import gcs.base.core.bean.GcsMap;
import gcs.base.core.bean.GcsResult;
import gcs.base.core.condition.GcsCondition;
import gcs.base.core.validators.ValidatorUtils;
import gcs.base.shiro.spi.SpiUser;
import gcs.base.tools.GcsBindingUtils;
import gcs.base.tools.GcsDateTimeUtils;
import gcs.modules.activity.domain.GcsActivity;
import gcs.modules.activity.mapper.GcsActivityMapper;
import gcs.modules.activity.service.GcsActivityService;
import gcs.modules.message.controller.GcsMessageController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/gcsActivity")
public class GcsActivityController extends ServiceImpl<GcsActivityMapper, GcsActivity> {

    @Autowired
    private GcsCoreService gcsCoreService;

    @Autowired
    private GcsShiroService gcsShiroService;

    @Autowired
    private GcsActivityService gcsActivityService;

    @Autowired
    private SimpMessageSendingOperations brokerMessagingTemplate;

    /**
     * 获取活动列表
     * @return
     */
    @RequestMapping(value = "/index", method = {RequestMethod.POST})
    public GcsResult index(
            @RequestParam String domainId,
            @RequestParam String domainName,
            @RequestBody(required = false) Optional<GcsCondition> payload){
        GcsCondition gcsCondition = payload.orElse(new GcsCondition());
        String entityIdentifier = domainName + "@" + domainId;
        List<Object> identList = gcsCoreService.getReferenceIdents(gcsCoreService.getReference(entityIdentifier));
        SpiUser spiUser = gcsShiroService.currentUser();
        // 活跃计划列表
        List<GcsActivity> activeSchedules = gcsActivityService.listActiveSchedules(identList, spiUser.getId());
        List<Map<String, Object>> activeSchedulesList = activeSchedules.stream()
                .map(GcsActivityController::_entityMap)
                .collect(Collectors.toList());
        // 非活跃计划分页
        QueryWrapper<GcsActivity> queryWrapper = gcsCondition.buildQueryWrapper(GcsActivity.class)
                .eq("reference_class", identList.get(0))
                .eq("reference_id", identList.get(1))
                .and(wrapper -> wrapper.isNull("assignee_id").or().eq("assignee_id", spiUser.getId()));
        queryWrapper.not(subWrapper ->
                subWrapper
                        .eq("is_active", true)
                        .isNotNull("due_date")
        );
        IPage<GcsActivity> page = this.page(
                gcsCondition.buildQueryPage(),
                queryWrapper
        );
        // 返回最终数据
        page.convert(GcsActivityController::_entityMap);
        return GcsResult.successResult().wrapPage(page)
                .put("activeList", activeSchedulesList);
    }

    /**
     * 添加记录
     * @param domainId
     * @param domainName
     * @param jsonMap
     * @return
     */
    @RequestMapping(value = "/saveNote", method = {RequestMethod.POST})
    public GcsResult saveNote(@RequestParam String domainId, @RequestParam String domainName, @RequestBody GcsMap jsonMap) {
        String entityIdentifier = domainName + "@" + domainId;
        GcsActivity gcsActivity = GcsBindingUtils.bind(jsonMap, GcsActivity.class);
        GcsActivity entity = gcsActivityService.addNote(entityIdentifier, gcsActivity);
        return GcsResult.successResult().singleData(_entityMap(entity));
    }

    /**
     * 汇总计划
     * @param jsonMap
     * @return
     */
    @RequestMapping(value = "/summarySchedule", method = {RequestMethod.POST})
    public GcsResult summarySchedule(@RequestBody GcsMap jsonMap) {
        SpiUser spiUser = gcsShiroService.currentUser();
        List<Map<String, Object>> summaryList = gcsActivityService.summarySchedule(spiUser.getId());
        return GcsResult.successResult().singleData(summaryList);
    }

    /**
     * 添加计划
     * @param domainId
     * @param domainName
     * @param jsonMap
     * @return
     */
    @RequestMapping(value = "/saveSchedule", method = {RequestMethod.POST})
    public GcsResult saveSchedule(@RequestParam String domainId, @RequestParam String domainName, @RequestBody GcsMap jsonMap) {
        String entityIdentifier = domainName + "@" + domainId;
        GcsActivity gcsActivity = GcsBindingUtils.bind(jsonMap, GcsActivity.class);
        GcsActivity entity = gcsActivityService.addSchedule(entityIdentifier, gcsActivity);
        // 保存 + 完成
        if(jsonMap.containsKey("_opType") && jsonMap.getInt("_opType")>0){
            entity = gcsActivityService.finishSchedule(entity, null);
        }
        // 推送Summary消息
        SpiUser spiUser = gcsShiroService.currentUser();
        List<Map<String, Object>> summaryList = gcsActivityService.summarySchedule(spiUser.getId());
        String messageJson = JSONUtil.toJsonStr(summaryList);
        brokerMessagingTemplate.convertAndSendToUser("tenant_" + spiUser.getUsername(), "/summarySchedule", messageJson);
        return GcsResult.successResult().singleData(_entityMap(entity));
    }

    /**
     * 更新计划
     * @param jsonMap
     * @return
     */
    @RequestMapping(value = "/updateSchedule", method = {RequestMethod.POST})
    public GcsResult updateSchedule(@RequestBody GcsMap jsonMap) {
        Long id = jsonMap.getLong(BaseDomain.ID);
        GcsActivity entity = this.getById(id);
        if(entity == null){
            return GcsResult.notExistResult().put("id", id);
        }
        GcsBindingUtils.bind(jsonMap, entity);
        ValidatorUtils.validateObj(entity);
        this.updateById(entity);
        // 更新 + 完成
        if(jsonMap.getInt("_opType")>0){
            entity = gcsActivityService.finishSchedule(entity, null);
        }
        // 推送Summary消息
        SpiUser spiUser = gcsShiroService.currentUser();
        List<Map<String, Object>> summaryList = gcsActivityService.summarySchedule(spiUser.getId());
        String messageJson = JSONUtil.toJsonStr(summaryList);
        brokerMessagingTemplate.convertAndSendToUser("tenant_" + spiUser.getUsername(), "/summarySchedule", messageJson);
        return GcsResult.successResult().putAll(_entityMap(entity));
    }

    /**
     * 完成计划
     * @param jsonMap
     * @return
     */
    @RequestMapping(value = "/finishSchedule", method = {RequestMethod.POST})
    public GcsResult finishSchedule(@RequestBody GcsMap jsonMap) {
        Long id = jsonMap.longValue(BaseDomain.ID);
        GcsActivity entity = this.getById(id);
        if(entity == null){
            return GcsResult.notExistResult().put("id", id);
        }
        entity = gcsActivityService.finishSchedule(entity, jsonMap.getString("comment"));
        // 推送Summary消息
        SpiUser spiUser = gcsShiroService.currentUser();
        List<Map<String, Object>> summaryList = gcsActivityService.summarySchedule(spiUser.getId());
        String messageJson = JSONUtil.toJsonStr(summaryList);
        brokerMessagingTemplate.convertAndSendToUser("tenant_" + spiUser.getUsername(), "/summarySchedule", messageJson);
        return GcsResult.successResult().putAll(_entityMap(entity));
    }

    /**
     * 删除活动
     * @param id 活动ID
     * @return 
     */
    @DeleteMapping("/delete/{id}")
    public GcsResult delete(@PathVariable Long id) {
        gcsActivityService.delActivity(id);
        // 推送Summary消息
        SpiUser spiUser = gcsShiroService.currentUser();
        List<Map<String, Object>> summaryList = gcsActivityService.summarySchedule(spiUser.getId());
        String messageJson = JSONUtil.toJsonStr(summaryList);
        brokerMessagingTemplate.convertAndSendToUser("tenant_" + spiUser.getUsername(), "/summarySchedule", messageJson);
        return GcsResult.successResult().put("id", id);
    }

    // 工具方法
    private static Map<String, Object> _entityMap(GcsActivity entity) {
        Map<String, Object> entityMap = BaseController._entityMap(entity);
        entityMap.put("extWeekDateStr", GcsDateTimeUtils.weekDateStr(entity.getFinishDate()!=null?entity.getFinishDate():entity.getDueDate()));
        return entityMap;
    }
}