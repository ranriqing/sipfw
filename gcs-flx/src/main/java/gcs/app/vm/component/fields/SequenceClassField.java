package gcs.app.vm.component.fields;

import cn.hutool.core.convert.Convert;
import com.squareup.javapoet.FieldSpec;
import gcs.app.vm.bean.VmClassField;
import gcs.app.vm.constants.VmConstants;
import gcs.base.BaseDomain;
import gcs.base.core.bean.ParamViolation;
import gcs.base.tools.GcsBindingUtils;
import gcs.base.tools.GcsSeqUtils;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.stereotype.Component;

import javax.lang.model.element.Modifier;
import java.util.Arrays;
import java.util.List;

@Component
public class SequenceClassField extends VmClassField {

    @Override
    public String getSupportedType() {
        return VmConstants.FIELD_TYPE_SEQUENCE;
    }

    @Override
    public List<FieldSpec> generateFieldSpec() {
        FieldSpec fieldSpec = FieldSpec.builder(String.class, this.getFieldName())
                .addModifiers(Modifier.PRIVATE)
                .build();
        return Arrays.asList(fieldSpec);
    }

    @Override
    public void beforeSaveVmField(BeanWrapper beanWrapper, BaseDomain domainEntity){
        String fieldValue = (String)beanWrapper.getPropertyValue(this.getFieldName());
        if(fieldValue == null){
            String seqName = "sno_yearly_" + this.getFieldName();
            String seqFormat = Convert.toStr(getExtendMap().get("seqFormat"));
            if(StringUtils.isNotBlank(seqFormat)){
                fieldValue = GcsSeqUtils.nextYearlySerial(seqName, seqFormat);
                beanWrapper.setPropertyValue(this.getFieldName(), fieldValue);
            }
        }
    }

    @Override
    public void beforeUpdateVmField(BeanWrapper beanWrapper, BaseDomain domainEntity){
        String fieldValue = (String)beanWrapper.getPropertyValue(this.getFieldName());
        if(fieldValue != null){
            beanWrapper.setPropertyValue(this.getFieldName(), fieldValue);  // 不做更新
        }
    }
}
