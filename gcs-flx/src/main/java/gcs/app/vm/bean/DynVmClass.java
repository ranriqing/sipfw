package gcs.app.vm.bean;

import cn.hutool.core.convert.Convert;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import gcs.base.BaseDomain;
import gcs.base.core.GcsConstants;
import gcs.base.core.GcsCoreService;
import gcs.base.core.GcsHolder;
import gcs.base.core.exception.GcsParamException;
import gcs.base.tools.GcsBindingUtils;
import gcs.base.tools.GcsEntityUtils;
import gcs.base.shiro.spi.SpiUser;
import gcs.modules.activity.domain.GcsActivity;
import gcs.modules.activity.service.GcsActivityService;
import gcs.modules.attachment.service.GcsAttachmentService;
import gcs.modules.storage.domain.GcsResource;
import gcs.modules.storage.service.GcsStorageService;
import gcs.modules.tag.service.GcsTagService;
import gcs.app.ugrp.service.GcsShiroService;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 每个实例创建一个对应的DynamicVmClass
 */
public class DynVmClass extends VmClass {

    private Map<String, VmClassField> _classFieldMap = new HashMap<>();

    public String getSupportedDomain(){
        return null;
    }

    @Override
    public Map<String, VmClassField> getClassFieldMap(){
        return _classFieldMap;
    }
    @Override
    public void setClassFieldMap(Map<String, VmClassField> classFieldMap){
        this._classFieldMap = classFieldMap;
    }

    @Override
    public BaseDomain bindVmDomain(BaseDomain domainEntity, Map<String, Object> jsonMap){
        BeanWrapper beanWrapper = new BeanWrapperImpl(domainEntity);
        beanWrapper.setConversionService(GcsBindingUtils.getConversionService());
        for (Map.Entry<String, VmClassField> entry : getClassFieldMap().entrySet()) {
            VmClassField vmClassField = entry.getValue();
            vmClassField.bindVmField(beanWrapper, domainEntity, jsonMap);
        }
        // 移除已由 VmClassField 处理的属性，避免 GcsBindingUtils.bind 重复绑定（如 OneToMany 字段无法从 LinkedHashMap 转换到 BaseDomain）
        for (String fieldName : getClassFieldMap().keySet()) {
            jsonMap.remove(fieldName);
        }
        if(jsonMap.containsKey(GcsConstants.ACTIVITIES)){
            List<?> activities = (List<?>)jsonMap.get(GcsConstants.ACTIVITIES);
            List<JSONObject> activityList = activities.stream().map(JSONUtil::parseObj).collect(Collectors.toList());
            domainEntity.setActivityList(activityList);
            jsonMap.remove(GcsConstants.ACTIVITIES);
        }
        if(jsonMap.containsKey(GcsConstants.ATTACHMENTS)){
            List<?> attachments = (List<?>)jsonMap.get(GcsConstants.ATTACHMENTS);
            List<JSONObject> attachmentList = attachments.stream().map(JSONUtil::parseObj).collect(Collectors.toList());
            domainEntity.setAttachmentList(attachmentList);
            jsonMap.remove(GcsConstants.ATTACHMENTS);
        }
        if(jsonMap.containsKey(GcsConstants.TAGS)){
            List<?> tags = (List<?>)jsonMap.get(GcsConstants.TAGS);
            List<String> tagList = tags.stream()
                    .map(tag -> tag instanceof Map ? Convert.toStr(((Map<?, ?>) tag).get("name")) : Convert.toStr(tag))
                    .collect(Collectors.toList());
            domainEntity.setTagList(tagList);
            jsonMap.remove(GcsConstants.TAGS);
        }
        GcsBindingUtils.bind(jsonMap, domainEntity);
        return domainEntity;
    }

    @Override
    public void validateVmDomain(BaseDomain domainEntity){
        domainEntity.setViolationList(new ArrayList<>());
        BeanWrapper beanWrapper = new BeanWrapperImpl(domainEntity);
        beanWrapper.setConversionService(GcsBindingUtils.getConversionService());
        for (Map.Entry<String, VmClassField> entry : getClassFieldMap().entrySet()) {
            VmClassField vmClassField = entry.getValue();
            vmClassField.validateVmField(beanWrapper, domainEntity);
        }
        if(!domainEntity.getViolationList().isEmpty()){
            throw new GcsParamException("校验失败", domainEntity.getViolationList());
        }
    }

    @Override
    public BaseDomain beforeSaveVmDomain(BaseDomain domainEntity) {
        BeanWrapper beanWrapper = new BeanWrapperImpl(domainEntity);
        for (Map.Entry<String, VmClassField> entry : getClassFieldMap().entrySet()) {
            VmClassField vmClassField = entry.getValue();
            vmClassField.beforeSaveVmField(beanWrapper, domainEntity);
        }
        return domainEntity;
    }

    @Override
    public BaseDomain saveVmDomain(BaseDomain domainEntity){
        this.beforeSaveVmDomain(domainEntity);
        SqlHelper.retBool(getDomainMapper().insert(domainEntity));
        this.postSaveVmDomain(domainEntity);
        return domainEntity;
    }

    @Override
    public BaseDomain postSaveVmDomain(BaseDomain domainEntity){
        BeanWrapper beanWrapper = new BeanWrapperImpl(domainEntity);
        for (Map.Entry<String, VmClassField> entry : getClassFieldMap().entrySet()) {
            VmClassField vmClassField = entry.getValue();
            vmClassField.postSaveVmField(beanWrapper, domainEntity);
        }
        /** 特殊处理：保存活动部分 */
        if(domainEntity.getActivityList()!=null){
            GcsActivityService gcsActivityService = GcsHolder.getBean(GcsActivityService.class);
            domainEntity.getActivityList().forEach(item -> {
                GcsActivity gcsActivity = GcsBindingUtils.bind(item, GcsActivity.class);
                gcsActivityService.addSchedule(domainEntity, gcsActivity);
            });
        }
        /** 特殊处理：保存附件部分 */
        if(domainEntity.getAttachmentList()!=null){
            GcsStorageService gcsStorageService = GcsHolder.getBean(GcsStorageService.class);
            GcsAttachmentService gcsAttachmentService = GcsHolder.getBean(GcsAttachmentService.class);
            domainEntity.getAttachmentList().forEach(item -> {
                String fileUuid = (String)item.get("fileUuid");
                String fileName = (String)item.get("fileName");
                String fileType = (String)item.get("fileType");
                GcsResource gcsResource = gcsStorageService.get(fileUuid, true);
                if(gcsResource!=null) {
                    gcsAttachmentService.addAttachment(domainEntity, gcsResource, fileType );
                }
            });
        }
        /** 特殊处理：保存标签部分 */
        if(domainEntity.getTagList()!=null){
            GcsTagService gcsTagService = GcsHolder.getBean(GcsTagService.class);
            gcsTagService.setTags(domainEntity, domainEntity.getTagList());
        }
        return domainEntity;
    }

    @Override
    public BaseDomain beforeUpdateVmDomain(BaseDomain domainEntity){
        BeanWrapper beanWrapper = new BeanWrapperImpl(domainEntity);
        for (Map.Entry<String, VmClassField> entry : getClassFieldMap().entrySet()) {
            VmClassField vmClassField = entry.getValue();
            vmClassField.beforeUpdateVmField(beanWrapper, domainEntity);
        }
        return domainEntity;
    }

    @Override
    public BaseDomain updateVmDomain(BaseDomain domainEntity){
        this.beforeUpdateVmDomain(domainEntity);
        SqlHelper.retBool(getDomainMapper().updateById(domainEntity));
        this.postUpdateVmDomain(domainEntity);
        return domainEntity;
    }

    @Override
    public BaseDomain postUpdateVmDomain(BaseDomain domainEntity){
        BeanWrapper beanWrapper = new BeanWrapperImpl(domainEntity);
        for (Map.Entry<String, VmClassField> entry : getClassFieldMap().entrySet()) {
            VmClassField vmClassField = entry.getValue();
            vmClassField.postUpdateVmField(beanWrapper, domainEntity);
        }
        /** 特殊处理：更新标签部分 */
        if(domainEntity.getTagList()!=null){
            GcsTagService gcsTagService = GcsHolder.getBean(GcsTagService.class);
            gcsTagService.setTags(domainEntity, domainEntity.getTagList());
        }
        return domainEntity;
    }

    @Override
    public Map<String, Object> vmDomainEntityMap(BaseDomain domainEntity){
        return vmDomainEntityMap(domainEntity, null);
    }

    @Override
    public Map<String, Object> vmDomainEntityMap(BaseDomain domainEntity,  Map<String, Object> contextMap){
        BeanWrapper beanWrapper = new BeanWrapperImpl(domainEntity);
        beanWrapper.setConversionService(GcsBindingUtils.getConversionService());
        Map<String, Object> entityMap = GcsEntityUtils.toMap(domainEntity);
        for (Map.Entry<String, VmClassField> entry : getClassFieldMap().entrySet()) {
            VmClassField vmClassField = entry.getValue();
            vmClassField.enrichMap(beanWrapper, domainEntity, entityMap);
        }
        // 扩展待办
        GcsCoreService gcsCoreService = GcsHolder.getBean(GcsCoreService.class);
        GcsShiroService gcsShiroService = GcsHolder.getBean(GcsShiroService.class);
        List<Object> identList = gcsCoreService.getReferenceIdents(domainEntity);
        if(identList!=null){
            SpiUser spiUser = gcsShiroService.currentUser();
            GcsActivity nextActivity = Db.getOne(
                    Wrappers.lambdaQuery(GcsActivity.class)
                            .eq(GcsActivity::getReferenceClass, identList.get(0))
                            .eq(GcsActivity::getReferenceId, identList.get(1))
                            .isNotNull(GcsActivity::getDueDate)
                            .eq(GcsActivity::getIsActive, true)
                            .eq(GcsActivity::getAssigneeId, spiUser.getId())
                            .orderByAsc(GcsActivity::getDueDate).last(GcsConstants.LIMIT_1)
            );
            if(nextActivity!=null){
                entityMap.put(GcsConstants.NEXT_ACTIVITY, nextActivity);
            }
        }
        // 扩展标签
        if(domainEntity.getId()!=null){
            GcsTagService gcsTagService = GcsHolder.getBean(GcsTagService.class);
            entityMap.put(GcsConstants.TAGS, gcsTagService.listTagDetails(domainEntity));
        }
        // 扩展信息
        entityMap.put(BaseDomain.ID, beanWrapper.getPropertyValue(BaseDomain.ID));
        entityMap.put(BaseDomain.CREATOR_ID, domainEntity.getCreaterId());
        entityMap.put(BaseDomain.UPDATER_ID, domainEntity.getUpdaterId());
        entityMap.put(BaseDomain.DATE_CREATED, domainEntity.getDateCreated());
        entityMap.put(BaseDomain.LAST_UPDATED, domainEntity.getLastUpdated());
        return entityMap;
    }
}
