package gcs.app.vm.component.fields;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.FieldSpec;
import gcs.app.vm.bean.VmClassField;
import gcs.app.vm.constants.VmConstants;
import gcs.base.BaseDomain;
import gcs.base.core.bean.ParamViolation;
import gcs.base.tools.GcsBindingUtils;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.BeanWrapper;
import org.springframework.stereotype.Component;

import javax.lang.model.element.Modifier;
import javax.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
public class BooleanClassField extends VmClassField {

    @Override
    public String getSupportedType() {
        return VmConstants.FIELD_TYPE_BOOLEAN;
    }

    @Override
    public void initConstraints() {
        this.setFieldConstraint(GcsBindingUtils.bind(getConstraintsMap(), FieldConstraint.class));
    }

    @Override
    public List<FieldSpec> generateFieldSpec() {
        FieldSpec fieldSpec = FieldSpec.builder(Boolean.class, this.getFieldName())
                .addModifiers(Modifier.PRIVATE)
                .build();
        return Arrays.asList(fieldSpec);
    }

    @Override
    public void validateVmField(BeanWrapper beanWrapper, BaseDomain domainEntity) {
        Boolean fieldValue = (Boolean) beanWrapper.getPropertyValue(this.getFieldName());
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
        this.setFieldExtend(GcsBindingUtils.deepBind(getExtendMap(), BooleanFieldExtend.class));
    }

    @Override
    public void bindVmField(BeanWrapper beanWrapper, BaseDomain domainEntity, Map<String, Object> jsonMap) {
        super.bindVmField(beanWrapper, domainEntity, jsonMap);
        BooleanFieldExtend extend = (BooleanFieldExtend) this.getFieldExtend();
        if (extend.getDefaultValue() != null) {
            Object currentValue = beanWrapper.getPropertyValue(this.getRealFieldName());
            if (currentValue == null) {
                beanWrapper.setPropertyValue(this.getRealFieldName(), extend.getDefaultValue());
            }
        }
    }

    @Data
    @NoArgsConstructor
    public static class BooleanFieldExtend extends FieldExtend {
        private Boolean defaultValue;
    }
}
