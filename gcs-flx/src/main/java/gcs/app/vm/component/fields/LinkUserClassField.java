package gcs.app.vm.component.fields;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.lang.Console;
import com.baomidou.mybatisplus.annotation.TableField;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import gcs.app.ugrp.domain.GcsUserLink;
import gcs.app.ugrp.service.GcsUserLinkService;
import gcs.app.vm.bean.VmClassField;
import gcs.app.vm.constants.VmConstants;
import gcs.base.BaseDomain;
import gcs.base.core.GcsHolder;
import gcs.base.core.bean.ParamViolation;
import gcs.base.tools.GcsBindingUtils;
import gcs.modules.others.domain.AddrProvince;
import org.springframework.beans.BeanWrapper;
import org.springframework.stereotype.Component;

import javax.lang.model.element.Modifier;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;
import javax.xml.ws.Holder;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class LinkUserClassField extends VmClassField {

    @Override
    public String getSupportedType() {
        return VmConstants.FIELD_TYPE_LINK_USER;
    }

    @Override
    public Boolean supportsImport() {
        return super.supportsImport();
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
        FieldSpec linkGcsUsersSpec = FieldSpec.builder(
                    ParameterizedTypeName.get(List.class, GcsUserLink.class),
                    this.getFieldName()
                ).addModifiers(Modifier.PRIVATE)
                .addAnnotation(AnnotationSpec.builder(Transient.class).build())
                .addAnnotation(AnnotationSpec.builder(TableField.class)
                        .addMember("exist", "$L", false)
                        .build())
                .build();
        return Arrays.asList(linkGcsUsersSpec);
    }

    @Override
    public void bindVmField(BeanWrapper beanWrapper, BaseDomain domainEntity, Map<String, Object> jsonMap){
        String fieldName = this.getFieldName();
        List<HashMap> userInfoList = Convert.toList(HashMap.class, jsonMap.get(this.getFieldName()));
        List<GcsUserLink> gcsUserLinks = new ArrayList<>();
        if(userInfoList!=null){
            userInfoList.forEach(userInfo -> {
                GcsUserLink userLink = new GcsUserLink();
                userLink.setGcsUserId(Convert.toLong(userInfo.get("id")));
                gcsUserLinks.add(userLink);
            });
        }
        if (beanWrapper.isWritableProperty(fieldName)) {
            beanWrapper.setPropertyValue(fieldName, gcsUserLinks);
        }
    }

    @Override
    public void validateVmField(BeanWrapper beanWrapper, BaseDomain domainEntity){
        List fieldValue = null;
        if (beanWrapper.isReadableProperty(this.getRealFieldName())) {
            fieldValue = (List)beanWrapper.getPropertyValue(this.getRealFieldName());
        }
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
    public void postSaveVmField(BeanWrapper beanWrapper, BaseDomain domainEntity){
        List<GcsUserLink> userLinks = null;
        if (beanWrapper.isReadableProperty(this.getRealFieldName())) {
            userLinks = (List<GcsUserLink>)beanWrapper.getPropertyValue(this.getRealFieldName());
        }
        List<Long> userIds = userLinks!=null?userLinks.stream().map(GcsUserLink::getGcsUserId).collect(Collectors.toList()):null;
        GcsUserLinkService gcsUserLinkService = GcsHolder.getBean(GcsUserLinkService.class);
        gcsUserLinkService.setRelUsers(domainEntity, this.getFieldName(), userIds);
    }

    @Override
    public void postUpdateVmField(BeanWrapper beanWrapper, BaseDomain domainEntity){
        List<GcsUserLink> userLinks = null;
        if (beanWrapper.isReadableProperty(this.getRealFieldName())) {
            userLinks = (List<GcsUserLink>)beanWrapper.getPropertyValue(this.getRealFieldName());
        }
        List<Long> userIds = userLinks!=null?userLinks.stream().map(GcsUserLink::getGcsUserId).collect(Collectors.toList()):null;
        GcsUserLinkService gcsUserLinkService = GcsHolder.getBean(GcsUserLinkService.class);
        gcsUserLinkService.setRelUsers(domainEntity, this.getFieldName(), userIds);
    }

    @Override
    public Map<String, Object> enrichMap(BeanWrapper beanWrapper, Object entity, Map<String, Object> entityMap){
        GcsUserLinkService gcsUserLinkService = GcsHolder.getBean(GcsUserLinkService.class);
        List<GcsUserLink> userLinks = gcsUserLinkService.listRelUserLinks(entity, getFieldName());
        entityMap.put(this.getFieldName(), userLinks.stream().map(userLink -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", userLink.getGcsUserId());
            map.put("username", userLink.getUsername());
            map.put("nickname", userLink.getNickname());
            return map;
        }).collect(Collectors.toList()));
        return entityMap;
    }

    @Override
    public String formatDisplay(BeanWrapper beanWrapper, BaseDomain domainEntity){
        GcsUserLinkService gcsUserLinkService = GcsHolder.getBean(GcsUserLinkService.class);
        List<GcsUserLink> userLinks = gcsUserLinkService.listRelUserLinks(domainEntity, this.getFieldName());
        if (userLinks == null || userLinks.isEmpty()) {
            return null;
        }
        List<String> names = userLinks.stream().map(GcsUserLink::getNickname).collect(Collectors.toList());
        return String.join("|", names);
    }
}
