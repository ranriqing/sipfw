package gcs.app.vm.component.fields;

import com.baomidou.mybatisplus.annotation.TableField;
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
import javax.persistence.Column;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
public class StringClassField extends VmClassField {

    @Override
    public String getSupportedType() {
        return VmConstants.FIELD_TYPE_STRING;
    }

    @Override
    public void initConstraints() {
        this.setFieldConstraint(GcsBindingUtils.bind(getConstraintsMap(), StringFieldConstraint.class));
    }

    @Override
    public List<FieldSpec> generateFieldSpec() {
        FieldSpec fieldSpec = FieldSpec.builder(String.class, this.getFieldName())
                .addModifiers(Modifier.PRIVATE)
                .addAnnotation(AnnotationSpec.builder(Column.class)
                        .addMember("columnDefinition", "$S", "TEXT")
                        .build())
                .build();
        return Arrays.asList(fieldSpec);
    }

    @Override
    public void validateVmField(BeanWrapper beanWrapper, BaseDomain domainEntity){
        StringFieldConstraint fieldConstraint = (StringFieldConstraint)this.getFieldConstraint();
        String fieldValue = (String)beanWrapper.getPropertyValue(this.getFieldName());
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
            if(fieldConstraint.getMinSize()!=null && fieldValue.length()<fieldConstraint.getMinSize()){
                domainEntity.getViolationList().add(
                        new ParamViolation(
                                this.getFieldName(),
                                fieldValue,
                                "[" + this.getFieldTitle() + "]字符长度不能少于" + fieldConstraint.getMinSize() + "个"
                        )
                );
            }
            if(fieldConstraint.getMaxSize()!=null && fieldValue.length()>fieldConstraint.getMaxSize()){
                domainEntity.getViolationList().add(
                        new ParamViolation(
                                this.getFieldName(),
                                fieldValue,
                                "[" + this.getFieldTitle() + "]字符长度不能多于" + fieldConstraint.getMaxSize() + "个"
                        )
                );
            }
        }
    }

    @Data
    @NoArgsConstructor
    protected static class StringFieldConstraint extends FieldConstraint {
        private Integer minSize;
        private Integer maxSize;
    }

    @Data
    @NoArgsConstructor
    public static class StringFieldExtend extends FieldExtend {
        private String defaultValue;
    }

    @Override
    public void initExtend() {
        this.setFieldExtend(GcsBindingUtils.deepBind(getExtendMap(), StringFieldExtend.class));
    }

    @Override
    public void bindVmField(BeanWrapper beanWrapper, BaseDomain domainEntity, Map<String, Object> jsonMap) {
        super.bindVmField(beanWrapper, domainEntity, jsonMap);
        StringFieldExtend extend = (StringFieldExtend) this.getFieldExtend();
        if (extend.getDefaultValue() != null) {
            Object currentValue = beanWrapper.getPropertyValue(this.getRealFieldName());
            if (currentValue == null) {
                beanWrapper.setPropertyValue(this.getRealFieldName(), extend.getDefaultValue());
            }
        }
    }
}
