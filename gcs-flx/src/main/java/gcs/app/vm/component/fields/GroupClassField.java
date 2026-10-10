package gcs.app.vm.component.fields;

import cn.hutool.core.convert.Convert;
import com.baomidou.mybatisplus.annotation.TableField;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.FieldSpec;
import gcs.app.ugrp.beans.UgrpGroup;
import gcs.app.ugrp.domain.GcsGroup;
import gcs.app.ugrp.domain.GcsUser;
import gcs.app.ugrp.service.GcsShiroService;
import gcs.app.ugrp.service.GcsUgrpService;
import gcs.app.vm.bean.VmClassField;
import gcs.app.vm.constants.VmConstants;
import gcs.base.BaseDomain;
import gcs.base.core.GcsHolder;
import gcs.base.core.bean.ParamViolation;
import gcs.base.shiro.spi.SpiUser;
import gcs.base.tools.GcsBindingUtils;
import gcs.modules.others.domain.AddrProvince;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.BeanWrapper;
import org.springframework.stereotype.Component;

import javax.lang.model.element.Modifier;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
public class GroupClassField extends VmClassField {

    @Override
    public String getSupportedType() {
        return VmConstants.FIELD_TYPE_GROUP;
    }

    @Override
    public void initConstraints() {
        this.setFieldConstraint(GcsBindingUtils.bind(getConstraintsMap(), FieldConstraint.class));
    }

    @Override
    public void initExtend() {
        this.setFieldExtend(GcsBindingUtils.deepBind(getExtendMap(), GroupFieldExtend.class));
    }

    @Override
    public List<FieldSpec> generateFieldSpec() {
        FieldSpec groupIdSpec = FieldSpec.builder(Long.class, this.getRealFieldName())
                .addModifiers(Modifier.PRIVATE)
                .addAnnotation(AnnotationSpec.builder(Transient.class).build())
                .build();
        FieldSpec groupSpec = FieldSpec.builder(GcsGroup.class, this.getFieldName())
                .addModifiers(Modifier.PRIVATE)
                .addAnnotation(AnnotationSpec.builder(ManyToOne.class).build())
                .addAnnotation(AnnotationSpec.builder(TableField.class)
                        .addMember("exist", "$L", false)
                        .build())
                .build();
        return Arrays.asList(groupIdSpec, groupSpec);
    }

    @Override
    public String getRealFieldName(){
        return this.getFieldName() + ID_SUFFIX;
    }

    @Override
    public void validateVmField(BeanWrapper beanWrapper, BaseDomain domainEntity){
        Long fieldValue = (Long)beanWrapper.getPropertyValue(this.getRealFieldName());
        if(fieldValue == null){
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
    public void bindVmField(BeanWrapper beanWrapper, BaseDomain domainEntity, Map<String, Object> jsonMap) {
        if (beanWrapper.isWritableProperty(this.getRealFieldName())) {
            if (jsonMap.containsKey(this.getRealFieldName())) {
                beanWrapper.setPropertyValue(this.getRealFieldName(), jsonMap.get(this.getRealFieldName()));
            }
        }
        // 自动写入当前部门（仅在未显式传值时）
        GroupFieldExtend extend = (GroupFieldExtend) this.getFieldExtend();
        if (extend.getAutoSpiUserGroup() != null && extend.getAutoSpiUserGroup()) {
            Object currentValue = beanWrapper.getPropertyValue(this.getRealFieldName());
            if (currentValue == null) {
                GcsShiroService shiroService = GcsHolder.getBean(GcsShiroService.class);
                SpiUser currentUser = shiroService.currentUser();
                if (currentUser != null) {
                    beanWrapper.setPropertyValue(this.getRealFieldName(), currentUser.getGroupId());
                }
            }
        }
    }

    @Override
    public String formatDisplay(BeanWrapper beanWrapper, BaseDomain domainEntity){
        Object propValue = beanWrapper.isReadableProperty(this.getRealFieldName())
                ? beanWrapper.getPropertyValue(this.getRealFieldName()) : null;
        return formatGroupValue(propValue);
    }

    private String formatGroupValue(Object propValue){
        Long groupId = Convert.toLong(propValue);
        if (groupId == null) {
            return null;
        }
        GcsUgrpService gcsUgrpService = GcsHolder.getBean(GcsUgrpService.class);
        UgrpGroup ugrpGroup = gcsUgrpService.getUgrpGroup(groupId);
        if (ugrpGroup.getId() != 0L) {
            return ugrpGroup.getName();
        }
        return Convert.toStr(groupId);
    }

    @Data
    @NoArgsConstructor
    public static class GroupFieldExtend extends FieldExtend {
        private Boolean autoSpiUserGroup;
    }

}
