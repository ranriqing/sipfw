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
public class IntegerClassField extends VmClassField {

    @Override
    public String getSupportedType() {
        return VmConstants.FIELD_TYPE_INTEGER;
    }

    @Override
    public void initConstraints() {
        this.setFieldConstraint(GcsBindingUtils.bind(getConstraintsMap(), IntegerFieldConstraint.class));
    }

    @Override
    public List<FieldSpec> generateFieldSpec() {
        FieldSpec fieldSpec = FieldSpec.builder(Integer.class, this.getFieldName())
                .addModifiers(Modifier.PRIVATE)
                .build();
        return Arrays.asList(fieldSpec);
    }

    @Override
    public void validateVmField(BeanWrapper beanWrapper, BaseDomain domainEntity){
        IntegerFieldConstraint fieldConstraint = (IntegerFieldConstraint)this.getFieldConstraint();
        Integer fieldValue = (Integer)beanWrapper.getPropertyValue(this.getFieldName());
        if(fieldValue == null){
            if(!fieldConstraint.getNullable()){
                domainEntity.getViolationList().add(
                    new ParamViolation(
                            this.getFieldName(),
                            fieldValue,
                            "[" + this.getFieldTitle() + "]不能为空"
                    )
                );
            }
        }else{
            if(fieldConstraint.getMin()!=null && fieldValue<fieldConstraint.getMin()){
                domainEntity.getViolationList().add(
                        new ParamViolation(
                                this.getFieldName(),
                                fieldValue,
                                "[" + this.getFieldTitle() + "]不能小于" + fieldConstraint.getMin()
                        )
                );
            }
            if(fieldConstraint.getMax()!=null && fieldValue>fieldConstraint.getMax()){
                domainEntity.getViolationList().add(
                        new ParamViolation(
                                this.getFieldName(),
                                fieldValue,
                                "[" + this.getFieldTitle() + "]不能大于" + fieldConstraint.getMax()
                        )
                );
            }
        }
    }

    @Data
    @NoArgsConstructor
    public static class IntegerFieldConstraint extends FieldConstraint {
        private Integer min;
        private Integer max;
    }

    @Override
    public void initExtend() {
        this.setFieldExtend(GcsBindingUtils.deepBind(getExtendMap(), IntegerFieldExtend.class));
    }

    @Override
    public void bindVmField(BeanWrapper beanWrapper, BaseDomain domainEntity, Map<String, Object> jsonMap) {
        super.bindVmField(beanWrapper, domainEntity, jsonMap);
        IntegerFieldExtend extend = (IntegerFieldExtend) this.getFieldExtend();
        if (extend.getDefaultValue() != null) {
            Object currentValue = beanWrapper.getPropertyValue(this.getRealFieldName());
            if (currentValue == null) {
                beanWrapper.setPropertyValue(this.getRealFieldName(), extend.getDefaultValue());
            }
        }
    }

    @Data
    @NoArgsConstructor
    public static class IntegerFieldExtend extends FieldExtend {
        private Integer defaultValue;
    }
}
