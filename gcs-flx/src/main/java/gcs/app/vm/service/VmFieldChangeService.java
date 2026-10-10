package gcs.app.vm.service;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import gcs.app.ugrp.service.GcsShiroService;
import gcs.app.vm.bean.VmClass;
import gcs.app.vm.bean.VmClassField;
import gcs.app.vm.constants.VmConstants;
import gcs.app.vm.domain.VmFieldChange;
import gcs.app.vm.mapper.VmFieldChangeMapper;
import gcs.base.BaseDomain;
import gcs.base.core.GcsCoreService;
import gcs.base.core.bean.GcsMap;
import gcs.base.core.condition.GcsCondition;
import gcs.base.shiro.spi.SpiUser;
import gcs.base.tools.GcsBindingUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class VmFieldChangeService {

    private static final String OP_CREATE = "CREATE";
    private static final String OP_UPDATE = "UPDATE";
    private static final String OP_DELETE = "DELETE";

    @Autowired
    private VmFieldChangeMapper vmFieldChangeMapper;

    @Autowired
    private GcsShiroService gcsShiroService;

    @Autowired
    private GcsCoreService gcsCoreService;

    /** 字段展示值快照（跳过 OneToMany） */
    public Map<String, String> formatFieldMap(VmClass vmClass, BaseDomain entity){
        Map<String, String> map = new LinkedHashMap<>();
        BeanWrapper beanWrapper = new BeanWrapperImpl(entity);
        for (VmClassField field : vmClass.getClassFieldMap().values()) {
            if (VmConstants.FIELD_TYPE_ONE2MANY.equals(field.getFieldType())) {
                continue;
            }
            map.put(field.getFieldName(), field.formatDisplay(beanWrapper, entity));
        }
        return map;
    }

    public void recordCreate(VmClass vmClass, BaseDomain entity){
        SpiUser spiUser = currentOperator();
        saveChanges(vmClass, entity.getId(), OP_CREATE, Collections.emptyMap(), formatFieldMap(vmClass, entity),
                operatorId(spiUser), operatorName(spiUser));
    }

    public void recordUpdate(VmClass vmClass, Map<String, String> oldFieldMap, BaseDomain newEntity){
        SpiUser spiUser = currentOperator();
        saveChanges(vmClass, newEntity.getId(), OP_UPDATE, oldFieldMap, formatFieldMap(vmClass, newEntity),
                operatorId(spiUser), operatorName(spiUser));
    }

    public void recordDelete(VmClass vmClass, BaseDomain entity){
        SpiUser spiUser = currentOperator();
        saveChanges(vmClass, entity.getId(), OP_DELETE, formatFieldMap(vmClass, entity), Collections.emptyMap(),
                operatorId(spiUser), operatorName(spiUser));
    }

    public void recordBatchUpdate(VmClass vmClass, List<BaseDomain> oldEntities, List<String> fieldNames,
                                  GcsMap jsonMap, Long operatorId, String operatorName){
        if (oldEntities == null || oldEntities.isEmpty()) {
            return;
        }
        // 用目标字段的原始新值临时封装一个实体，复用 formatFieldMap 统一计算新展示值
        Map<String, String> newFieldMap = formatFieldMap(vmClass, buildTempEntity(vmClass, fieldNames, jsonMap));
        Map<String, String> newFieldValues = new HashMap<>();
        for (String fieldName : fieldNames) {
            newFieldValues.put(fieldName, newFieldMap.get(fieldName));
        }
        for (BaseDomain oldEntity : oldEntities) {
            Map<String, String> oldMap = formatFieldMap(vmClass, oldEntity);
            Map<String, String> oldSubMap = new HashMap<>();
            for (String fieldName : fieldNames) {
                oldSubMap.put(fieldName, oldMap.get(fieldName));
            }
            saveChanges(vmClass, oldEntity.getId(), OP_UPDATE, oldSubMap, newFieldValues, operatorId, operatorName);
        }
    }

    public void recordBatchDelete(VmClass vmClass, List<BaseDomain> oldEntities, Long operatorId, String operatorName){
        if (oldEntities == null || oldEntities.isEmpty()) {
            return;
        }
        for (BaseDomain oldEntity : oldEntities) {
            saveChanges(vmClass, oldEntity.getId(), OP_DELETE, formatFieldMap(vmClass, oldEntity),
                    Collections.emptyMap(), operatorId, operatorName);
        }
    }

    public List<Map<String, Object>> listTimeline(String domainName, Long recordId){
        String referenceClass = gcsCoreService.toReferenceClass(domainName);
        List<VmFieldChange> changes = vmFieldChangeMapper.selectList(
                Wrappers.lambdaQuery(VmFieldChange.class)
                        .eq(VmFieldChange::getDomainName, referenceClass)
                        .eq(VmFieldChange::getRecordId, recordId)
                        .orderByDesc(VmFieldChange::getDateCreated)
                        .orderByDesc(VmFieldChange::getId)
        );
        Map<String, Map<String, Object>> groupMap = new LinkedHashMap<>();
        for (VmFieldChange change : changes) {
            Map<String, Object> group = groupMap.get(change.getChangeBatchId());
            if (group == null) {
                group = new LinkedHashMap<>();
                group.put("changeBatchId", change.getChangeBatchId());
                group.put("operationType", change.getOperationType());
                group.put("operatorName", change.getOperatorName());
                group.put("dateCreated", change.getDateCreated() != null
                        ? DateUtil.format(change.getDateCreated(), "yyyy-MM-dd HH:mm:ss") : null);
                group.put("fields", new ArrayList<Map<String, Object>>());
                groupMap.put(change.getChangeBatchId(), group);
            }
            Map<String, Object> fieldMap = new HashMap<>();
            fieldMap.put("fieldName", change.getFieldName());
            fieldMap.put("fieldTitle", change.getFieldTitle());
            fieldMap.put("fieldType", change.getFieldType());
            fieldMap.put("oldValue", change.getOldValue());
            fieldMap.put("newValue", change.getNewValue());
            ((List<Map<String, Object>>) group.get("fields")).add(fieldMap);
        }
        return new ArrayList<>(groupMap.values());
    }

    public IPage<VmFieldChange> pageIndex(GcsCondition condition){
        return vmFieldChangeMapper.selectPage(
                condition.buildQueryPage(),
                condition.buildQueryWrapper(VmFieldChange.class)
        );
    }

    /* ---------- 内部工具 ---------- */

    private BaseDomain buildTempEntity(VmClass vmClass, List<String> fieldNames, GcsMap jsonMap){
        BaseDomain tempEntity = vmClass.newVmDomain();
        BeanWrapper beanWrapper = new BeanWrapperImpl(tempEntity);
        beanWrapper.setConversionService(GcsBindingUtils.getConversionService());
        Map<String, Object> bindMap = new HashMap<>();
        for (String fieldName : fieldNames) {
            VmClassField field = vmClass.getClassFieldMap().get(fieldName);
            if (field == null || VmConstants.FIELD_TYPE_ONE2MANY.equals(field.getFieldType())) {
                continue;
            }
            Object rawValue = jsonMap.get(fieldName);
            bindMap.clear();
            bindMap.put(field.getFieldName(), rawValue);
            bindMap.put(field.getRealFieldName(), rawValue);
            field.bindVmField(beanWrapper, tempEntity, bindMap);
        }
        return tempEntity;
    }

    private void saveChanges(VmClass vmClass, Long recordId, String operationType,
                             Map<String, String> oldMap, Map<String, String> newMap,
                             Long operatorId, String operatorName){
        String changeBatchId = UUID.randomUUID().toString();
        Date now = new Date();
        List<VmFieldChange> changes = new ArrayList<>();
        for (VmClassField field : vmClass.getClassFieldMap().values()) {
            if (VmConstants.FIELD_TYPE_ONE2MANY.equals(field.getFieldType())) {
                continue;
            }
            String fieldName = field.getFieldName();
            String oldValue = oldMap.get(fieldName);
            String newValue = newMap.get(fieldName);
            if (Objects.equals(oldValue, newValue)) {
                continue;
            }
            VmFieldChange change = new VmFieldChange();
            change.setDomainName(gcsCoreService.toReferenceClass(vmClass.getDomainName()));
            change.setRecordId(recordId);
            change.setChangeBatchId(changeBatchId);
            change.setOperationType(operationType);
            change.setFieldName(fieldName);
            change.setFieldTitle(field.getFieldTitle());
            change.setFieldType(field.getFieldType());
            change.setOldValue(oldValue);
            change.setNewValue(newValue);
            change.setOperatorId(operatorId);
            change.setOperatorName(operatorName);
            change.setDateCreated(now);
            changes.add(change);
        }
        if (!changes.isEmpty()) {
            try {
                for (VmFieldChange change : changes) {
                    vmFieldChangeMapper.insert(change);
                }
            } catch (Exception e) {
                log.error("变更记录写入失败: domainName=" + vmClass.getDomainName() + ", recordId=" + recordId, e);
            }
        }
    }

    private SpiUser currentOperator(){
        try {
            return gcsShiroService.currentUser();
        } catch (Exception e) {
            return null;
        }
    }

    private Long operatorId(SpiUser spiUser){
        return spiUser != null ? spiUser.getId() : null;
    }

    private String operatorName(SpiUser spiUser){
        return spiUser != null ? spiUser.getDisplayName() : null;
    }
}
