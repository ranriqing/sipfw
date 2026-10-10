package gcs.app.vm.component.fields;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.FieldSpec;
import gcs.app.vm.bean.VmClassField;
import gcs.app.vm.constants.VmConstants;
import gcs.base.BaseDomain;
import gcs.base.core.GcsConstants;
import gcs.base.core.bean.ParamViolation;
import gcs.base.tools.GcsBindingUtils;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.BeanWrapper;
import org.springframework.stereotype.Component;

import javax.lang.model.element.Modifier;
import javax.validation.constraints.NotNull;
import cn.hutool.core.date.DateUtil;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Component
public class DateClassField extends VmClassField {

    @Override
    public String getSupportedType() {
        return VmConstants.FIELD_TYPE_DATE;
    }

    @Override
    public void initConstraints() {
        this.setFieldConstraint(GcsBindingUtils.bind(getConstraintsMap(), FieldConstraint.class));
    }

    @Override
    public List<FieldSpec> generateFieldSpec() {
        FieldSpec fieldSpec = FieldSpec.builder(Date.class, this.getFieldName())
                .addModifiers(Modifier.PRIVATE)
                .build();
        return Arrays.asList(fieldSpec);
    }

    @Override
    public void validateVmField(BeanWrapper beanWrapper, BaseDomain domainEntity) {
        Date fieldValue = (Date) beanWrapper.getPropertyValue(this.getFieldName());
        if (fieldValue == null) {
            if (!this.getFieldConstraint().getNullable()) {
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
    public void initExtend() {
        this.setFieldExtend(GcsBindingUtils.deepBind(getExtendMap(), DateFieldExtend.class));
    }

    @Override
    public void bindVmField(BeanWrapper beanWrapper, BaseDomain domainEntity, Map<String, Object> jsonMap) {
        super.bindVmField(beanWrapper, domainEntity, jsonMap);
        DateFieldExtend extend = (DateFieldExtend) this.getFieldExtend();
        if (extend.getDefaultValue() != null) {
            Object currentValue = beanWrapper.getPropertyValue(this.getRealFieldName());
            if (currentValue == null) {
                Date defaultDate = resolveRelativeDate(extend.getDefaultValue());
                if (defaultDate != null) {
                    beanWrapper.setPropertyValue(this.getRealFieldName(), defaultDate);
                }
            }
        }
    }

    private Date resolveRelativeDate(String key) {
        switch (key) {
            case "today":
                return DateUtil.beginOfDay(new Date()).toJdkDate();
            case "tomorrow":
                return DateUtil.beginOfDay(DateUtil.offsetDay(new Date(), 1)).toJdkDate();
            case "startOfMonth":
                return DateUtil.beginOfMonth(new Date()).toJdkDate();
            case "endOfMonth":
                return DateUtil.endOfMonth(new Date()).toJdkDate();
            default:
                return null;
        }
    }

    @Data
    @NoArgsConstructor
    public static class DateFieldExtend extends FieldExtend {
        private String format;
        private String defaultValue;
    }

    @Override
    public String formatDisplay(BeanWrapper beanWrapper, BaseDomain domainEntity){
        Object propValue = beanWrapper.isReadableProperty(this.getRealFieldName())
                ? beanWrapper.getPropertyValue(this.getRealFieldName()) : null;
        if (propValue == null) {
            return null;
        }
        return DateUtil.format((Date) propValue, this.getJavaDateFormat());
    }

    public String getJavaDateFormat(){
        DateFieldExtend fieldExtend = (DateFieldExtend) this.getFieldExtend();
        String momentDateFormat = fieldExtend.getFormat();
        if("YYYY-MM".equals(momentDateFormat)){
            return GcsConstants.DATE_MONTH_PATTERN;
        }else if("YYYY-MM-DD".equals(momentDateFormat)){
            return GcsConstants.DATE_DAY_PATTERN;
        }else if("YYYY-MM-DD HH:mm".equals(momentDateFormat)){
            return GcsConstants.DATETIME_MIN_PATTERN;
        }else if("YYYY-MM-DD HH:mm:ss".equals(momentDateFormat)){
            return GcsConstants.DATETIME_MINSEC_PATTERN;
        }else{
            return GcsConstants.DATE_DAY_PATTERN;
        }
    }
}
