package gcs.app.vm.component.fields;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.lang.Console;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.google.common.base.CaseFormat;
import com.squareup.javapoet.*;
import gcs.app.ugrp.domain.GcsUser;
import gcs.app.ugrp.domain.GcsUserLink;
import gcs.app.vm.bean.VmClass;
import gcs.app.vm.bean.VmClassField;
import gcs.app.vm.constants.VmConstants;
import gcs.app.vm.service.VmClassService;
import gcs.base.BaseDomain;
import gcs.base.core.GcsConstants;
import gcs.base.core.GcsHolder;
import gcs.base.core.bean.ParamViolation;
import gcs.base.tools.GcsBindingUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.stereotype.Component;

import javax.lang.model.element.Modifier;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class OneToManyClassField extends VmClassField {

    public static final String REL_DOMAIN_ID = "relDomainId";
    public static final String REL_DOMAIN_NAME = "relDomainName";
    public static final String REL_DOMAIN_FIELD_ID = "relDomainFieldId";
    public static final String REL_DOMAIN_FIELD_NAME = "relDomainFieldName";

    @Override
    public String getSupportedType() {
        return VmConstants.FIELD_TYPE_ONE2MANY;
    }

    @Override
    public Boolean supportsImport() {
        return false;
    }

    @Override
    public Boolean supportsBatchEdit(){
        return false;
    }

    @Override
    public void initConstraints() {
        this.setFieldConstraint(GcsBindingUtils.bind(getConstraintsMap(), FieldConstraint.class));
    }


    @Override
    public List<FieldSpec> generateFieldSpec() {
        /** 关联关系存储在对端，此处定义瞬态List字段 */
        FieldSpec transientSpec = FieldSpec.builder(
                ParameterizedTypeName.get(List.class, BaseDomain.class),
                this.getFieldName()
            ).addModifiers(Modifier.PRIVATE)
            .addAnnotation(AnnotationSpec.builder(Transient.class).build())
            .addAnnotation(AnnotationSpec.builder(TableField.class)
                    .addMember("exist", "$L", false)
                    .build())
            .build();
        return Arrays.asList(transientSpec);
    }

    @Override
    public void bindVmField(BeanWrapper beanWrapper, BaseDomain domainEntity, Map<String, Object> jsonMap){
        if (beanWrapper.isWritableProperty(this.getRealFieldName())) {
            /** 请求参数包含一对多字段 */
            if(jsonMap.containsKey(this.getRealFieldName())){
                // 1、获取关联relVmClass
                VmClassService vmClassService = GcsHolder.getBean(VmClassService.class);
                String relDomainName = this.getExtendMap().getStr(REL_DOMAIN_NAME);
                VmClass relVmClass = vmClassService.resolveVmClass(relDomainName);
                // 2、整理多个relJson到List(逐个绑定已有实体或新增实体)
                List<BaseDomain> manyLists = new ArrayList<>();
                List<HashMap> manyInfoList = Convert.toList(HashMap.class, jsonMap.get(this.getFieldName()));
                manyInfoList.forEach(manyInfo -> {
                    Long entityId = Convert.toLong(manyInfo.get(BaseDomain.ID));
                    BaseDomain vmDomainEntity = entityId!=null?(BaseDomain)relVmClass.getDomainMapper().selectById(entityId):relVmClass.newVmDomain();
                    relVmClass.bindVmDomain(vmDomainEntity, manyInfo);
                    manyLists.add(vmDomainEntity);
                });
                // 3、绑定List到beanWrapper
                beanWrapper.setPropertyValue(this.getRealFieldName(), manyLists);
            }
        }
    }

    @Override
    public void validateVmField(BeanWrapper beanWrapper, BaseDomain domainEntity){
        List fieldValue = (List)beanWrapper.getPropertyValue(this.getRealFieldName());
        if(fieldValue == null || fieldValue.isEmpty()){
            if(!this.getFieldConstraint().getNullable()){
                domainEntity.getViolationList().add(
                    new ParamViolation(
                            this.getFieldName(),
                            fieldValue,
                            "[" + this.getFieldTitle() + "]不能为空"
                    )
                );
            }
        }
    }

    @Override
    public void postSaveVmField(BeanWrapper beanWrapper, BaseDomain domainEntity){
        /** 保存主记录后，保存多条子记录逻辑 */
        List<BaseDomain> newManyLists = null;
        if (beanWrapper.isReadableProperty(this.getRealFieldName())) {
            newManyLists = (List<BaseDomain>)beanWrapper.getPropertyValue(this.getRealFieldName());
        }
        // 1、获取关联relVmClass
        VmClassService vmClassService = GcsHolder.getBean(VmClassService.class);
        String relDomainName = this.getExtendMap().getStr(REL_DOMAIN_NAME);
        String relDomainFieldName = this.getExtendMap().getStr(REL_DOMAIN_FIELD_NAME);
        String realRelDomainFieldName = relDomainFieldName + ID_SUFFIX;
        VmClass relVmClass = vmClassService.resolveVmClass(relDomainName);
        // 2、逐条保存子记录
        if (newManyLists!=null && !newManyLists.isEmpty()) {
            Long currentId = (Long) beanWrapper.getPropertyValue(BaseDomain.ID);
            newManyLists.forEach(entity -> {
                BeanUtil.setProperty(entity, realRelDomainFieldName, currentId);
                relVmClass.saveVmDomain(entity);
            });
        }
    }

    @Override
    public void postUpdateVmField(BeanWrapper beanWrapper, BaseDomain domainEntity){
        /** 更新主记录后，更新多条子记录逻辑 */
        List<BaseDomain> newManyLists = null;
        if (beanWrapper.isReadableProperty(this.getRealFieldName())) {
            newManyLists = (List<BaseDomain>)beanWrapper.getPropertyValue(this.getRealFieldName());
        }
        List<Long> newManyIds = new ArrayList<>();
        if (newManyLists != null && !newManyLists.isEmpty()) {
            for (BaseDomain relEntity : newManyLists) {
                Object idObj = BeanUtil.getProperty(relEntity, BaseDomain.ID);
                if (idObj instanceof Long) { // 确保ID是Long类型且非空
                    newManyIds.add((Long) idObj);
                }
            }
        }
        // 1、获取关联relVmClass
        VmClassService vmClassService = GcsHolder.getBean(VmClassService.class);
        String relDomainName = this.getExtendMap().getStr(REL_DOMAIN_NAME);
        String relDomainFieldName = this.getExtendMap().getStr(REL_DOMAIN_FIELD_NAME);
        String realRelDomainFieldName = relDomainFieldName + ID_SUFFIX;
        String relDomainColumnField = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, realRelDomainFieldName);
        VmClass relVmClass = vmClassService.resolveVmClass(relDomainName);
        // 2、读取数据库中已有关联（旧关联）
        List<BaseDomain> oldManyLists = relVmClass.getDomainMapper().selectList(
            Wrappers.query(relVmClass.getDomainClass())
                .eq(relDomainColumnField, beanWrapper.getPropertyValue(BaseDomain.ID))
        );
        List<Long> oldManyIds = oldManyLists.stream().map(relEntity -> (Long)BeanUtil.getProperty(relEntity, BaseDomain.ID)).collect(Collectors.toList());
        // 3、比对处理关联更新(多删少增)
        List<Long> toRemoveIds = new ArrayList<>();
        List<BaseDomain> toAddEntities = new ArrayList<>();
        List<BaseDomain> toUpdateEntities = new ArrayList<>();
        if (newManyLists == null || newManyLists.isEmpty()) {
            // 新关联为空：删除所有旧关联
            toRemoveIds.addAll(oldManyIds);
        } else {
            // 计算需要删除的旧关联：旧ID不在新ID列表中
            for (Long oldId : oldManyIds) {
                if (!newManyIds.contains(oldId)) {
                    toRemoveIds.add(oldId);
                }
            }
            // 计算需要新增的关联：
            for (BaseDomain newEntity : newManyLists) {
                Object idObj = BeanUtil.getProperty(newEntity, BaseDomain.ID);
                Long newId = (idObj instanceof Long) ? (Long) idObj : null;
                if (newId == null) {
                    // ID为空：新增实体
                    toAddEntities.add(newEntity);
                } else {
                    // ID非空：判断是否在旧关联中
                    if (!oldManyIds.contains(newId)) {
                        toAddEntities.add(newEntity);
                    }else{
                        toUpdateEntities.add(newEntity);
                    }
                }
            }
        }
        // 4、执行删除、新增、更新操作
        if (!toRemoveIds.isEmpty()) {
            relVmClass.getDomainMapper().deleteByIds(toRemoveIds);
        }
        if (!toAddEntities.isEmpty()) {
            Long currentId = (Long) beanWrapper.getPropertyValue(BaseDomain.ID);
            toAddEntities.forEach(entity -> {
                BeanUtil.setProperty(entity, realRelDomainFieldName, currentId);
                relVmClass.saveVmDomain(entity);
            });
        }
        if (!toUpdateEntities.isEmpty()) {
            Long currentId = (Long) beanWrapper.getPropertyValue(BaseDomain.ID);
            toUpdateEntities.forEach(entity -> {
                BeanUtil.setProperty(entity, realRelDomainFieldName, currentId);
                relVmClass.updateVmDomain(entity);
            });
        }
    }
}
