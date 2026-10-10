package gcs.app.vm.component.fields;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.FieldSpec;
import gcs.app.vm.bean.VmClassField;
import gcs.app.vm.constants.VmConstants;
import gcs.base.BaseDomain;
import gcs.base.core.bean.ParamViolation;
import gcs.base.tools.GcsBindingUtils;
import gcs.modules.others.domain.AddrArea;
import gcs.modules.others.domain.AddrCity;
import gcs.modules.others.domain.AddrProvince;
import org.springframework.beans.BeanWrapper;
import org.springframework.stereotype.Component;

import javax.lang.model.element.Modifier;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Component
public class AddrClassField extends VmClassField {

    public static final String PROVINCE_SUFFIX = "Province";
    public static final String PROVINCE_ID_SUFFIX = "ProvinceId";
    public static final String CITY_SUFFIX = "City";
    public static final String CITY_ID_SUFFIX = "CityId";
    public static final String AREA_SUFFIX = "Area";
    public static final String AREA_ID_SUFFIX = "AreaId";

    @Override
    public String getSupportedType() {
        return VmConstants.FIELD_TYPE_ADDR;
    }

    @Override
    public void initConstraints() {
        this.setFieldConstraint(GcsBindingUtils.bind(getConstraintsMap(), FieldConstraint.class));
    }

    @Override
    public List<FieldSpec> generateFieldSpec() {
        FieldSpec provinceIdSpec = FieldSpec.builder(Long.class, this.getFieldName() + PROVINCE_ID_SUFFIX)
                .addModifiers(Modifier.PRIVATE)
                .addAnnotation(AnnotationSpec.builder(Transient.class).build())
                .build();
        FieldSpec provinceSpec = FieldSpec.builder(AddrProvince.class, this.getFieldName() + PROVINCE_SUFFIX)
                .addModifiers(Modifier.PRIVATE)
                .addAnnotation(AnnotationSpec.builder(ManyToOne.class).build())
                .addAnnotation(AnnotationSpec.builder(TableField.class)
                        .addMember("exist", "$L", false)
                        .build())
                .build();
        FieldSpec cityIdSpec = FieldSpec.builder(Long.class, this.getFieldName() + CITY_ID_SUFFIX)
                .addModifiers(Modifier.PRIVATE)
                .addAnnotation(AnnotationSpec.builder(Transient.class).build())
                .build();
        FieldSpec citySpec = FieldSpec.builder(AddrCity.class, this.getFieldName() + CITY_SUFFIX)
                .addModifiers(Modifier.PRIVATE)
                .addAnnotation(AnnotationSpec.builder(ManyToOne.class).build())
                .addAnnotation(AnnotationSpec.builder(TableField.class)
                        .addMember("exist", "$L", false)
                        .build())
                .build();
        FieldSpec areaIdSpec = FieldSpec.builder(Long.class, this.getFieldName() + AREA_ID_SUFFIX)
                .addModifiers(Modifier.PRIVATE)
                .addAnnotation(AnnotationSpec.builder(Transient.class).build())
                .build();
        FieldSpec areaSpec = FieldSpec.builder(AddrArea.class, this.getFieldName() + AREA_SUFFIX)
                .addModifiers(Modifier.PRIVATE)
                .addAnnotation(AnnotationSpec.builder(ManyToOne.class).build())
                .addAnnotation(AnnotationSpec.builder(TableField.class)
                        .addMember("exist", "$L", false)
                        .build())
                .build();
        return Arrays.asList(provinceIdSpec, provinceSpec, cityIdSpec, citySpec, areaIdSpec, areaSpec);
    }

    @Override
    public void bindVmField(BeanWrapper beanWrapper, BaseDomain domainEntity, Map<String, Object> jsonMap){
        List addrList = Convert.toList(jsonMap.get(this.getFieldName()));
        if(ObjectUtil.isNotEmpty(addrList)){
            String provinceIdField = this.getFieldName() + PROVINCE_ID_SUFFIX;
            String cityIdField = this.getFieldName() + CITY_ID_SUFFIX;
            String areaIdField = this.getFieldName() + AREA_ID_SUFFIX;
            if (beanWrapper.isWritableProperty(provinceIdField)) {
                beanWrapper.setPropertyValue(provinceIdField, addrList.get(0));
            }
            if (beanWrapper.isWritableProperty(cityIdField) && addrList.size() > 1) {
                beanWrapper.setPropertyValue(cityIdField, addrList.get(1));
            }
            if (beanWrapper.isWritableProperty(areaIdField) && addrList.size() > 2) {
                beanWrapper.setPropertyValue(areaIdField, addrList.get(2));
            }
        }
    }

    @Override
    public void validateVmField(BeanWrapper beanWrapper, BaseDomain domainEntity) {
        Long provinceId = (Long) beanWrapper.getPropertyValue(this.getFieldName() + PROVINCE_ID_SUFFIX);
        Long cityId = (Long) beanWrapper.getPropertyValue(this.getFieldName() + CITY_ID_SUFFIX);
        Long areaId = (Long) beanWrapper.getPropertyValue(this.getFieldName() + AREA_ID_SUFFIX);
        if (provinceId==null && cityId==null && areaId==null) {
            if (!this.getFieldConstraint().getNullable()) {
                domainEntity.getViolationList().add(
                        new ParamViolation(
                                this.getFieldName(),
                                null,
                                "[" + this.getFieldTitle() + "]不能为空"
                        )
                );
            }
        }
    }

    @Override
    public Map<String, Object> enrichMap(BeanWrapper beanWrapper, Object entity, Map<String, Object> entityMap){
        Object provinceId = beanWrapper.getPropertyValue(this.getFieldName() + PROVINCE_ID_SUFFIX);
        Object cityId = beanWrapper.getPropertyValue(this.getFieldName() + CITY_ID_SUFFIX);
        Object areaId = beanWrapper.getPropertyValue(this.getFieldName() + AREA_ID_SUFFIX);
        entityMap.put(this.getFieldName(), Arrays.asList(provinceId, cityId, areaId));
        return entityMap;
    }

    @Override
    public String formatDisplay(BeanWrapper beanWrapper, BaseDomain domainEntity){
        String provinceIdField = this.getFieldName() + PROVINCE_ID_SUFFIX;
        String cityIdField = this.getFieldName() + CITY_ID_SUFFIX;
        String areaIdField = this.getFieldName() + AREA_ID_SUFFIX;
        Long provinceId = Convert.toLong(beanWrapper.isReadableProperty(provinceIdField) ? beanWrapper.getPropertyValue(provinceIdField) : null);
        Long cityId = Convert.toLong(beanWrapper.isReadableProperty(cityIdField) ? beanWrapper.getPropertyValue(cityIdField) : null);
        Long areaId = Convert.toLong(beanWrapper.isReadableProperty(areaIdField) ? beanWrapper.getPropertyValue(areaIdField) : null);
        return formatAddrValue(provinceId, cityId, areaId);
    }

    private String formatAddrValue(Long provinceId, Long cityId, Long areaId){
        List<String> parts = new ArrayList<>();
        if (provinceId != null) {
            AddrProvince addrProvince = Db.getById(provinceId, AddrProvince.class);
            parts.add(addrProvince != null ? addrProvince.getName() : Convert.toStr(provinceId));
        }
        if (cityId != null) {
            AddrCity addrCity = Db.getById(cityId, AddrCity.class);
            parts.add(addrCity != null ? addrCity.getName() : Convert.toStr(cityId));
        }
        if (areaId != null) {
            AddrArea addrArea = Db.getById(areaId, AddrArea.class);
            parts.add(addrArea != null ? addrArea.getName() : Convert.toStr(areaId));
        }
        return parts.isEmpty() ? null : String.join("/", parts);
    }
}
