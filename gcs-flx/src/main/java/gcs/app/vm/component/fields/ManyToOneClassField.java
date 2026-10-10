package gcs.app.vm.component.fields;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import com.squareup.javapoet.FieldSpec;
import gcs.app.vm.bean.VmClass;
import gcs.app.vm.bean.VmClassField;
import gcs.app.vm.constants.VmConstants;
import gcs.app.vm.service.VmClassService;
import gcs.base.BaseDomain;
import gcs.base.core.GcsHolder;
import gcs.base.core.bean.ParamViolation;
import gcs.base.tools.GcsBindingUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.stereotype.Component;

import javax.lang.model.element.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
public class ManyToOneClassField extends VmClassField {

    public static final String REL_DOMAIN_ID = "relDomainId";
    public static final String REL_DOMAIN_NAME = "relDomainName";

    @Override
    public String getSupportedType() {
        return VmConstants.FIELD_TYPE_MANY2ONE;
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
        FieldSpec relIdSpec = FieldSpec.builder(Long.class, this.getRealFieldName())
                .addModifiers(Modifier.PRIVATE)
                // .addAnnotation(AnnotationSpec.builder(Transient.class).build())
                .build();
        // Class名称是动态的，无法生成FieldSpec代码给ManyToOne 见：VmClassService|domainRealClassName
        return Arrays.asList(relIdSpec);
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
    public String formatDisplay(BeanWrapper beanWrapper, BaseDomain domainEntity){
        Long fk = Convert.toLong(beanWrapper.isReadableProperty(this.getRealFieldName())
                ? beanWrapper.getPropertyValue(this.getRealFieldName()) : null);
        if (fk == null) {
            return null;
        }
        String relDomainName = this.getExtendMap().getStr(REL_DOMAIN_NAME);
        if (StrUtil.isBlank(relDomainName)) {
            return Convert.toStr(fk);
        }
        VmClassService vmClassService = GcsHolder.getBean(VmClassService.class);
        VmClass relVmClass = vmClassService.resolveVmClass(relDomainName, true);
        if (relVmClass == null) {
            return Convert.toStr(fk);
        }
        BaseDomain relEntity = (BaseDomain) relVmClass.getDomainMapper().selectById(fk);
        if (relEntity == null) {
            return Convert.toStr(fk);
        }
        Map<String, Object> relMap = relVmClass.vmDomainEntityMap(relEntity);
        return resolveRelTitle(relMap, fk);
    }

    private String resolveRelTitle(Map<String, Object> relMap, Long fk){
        for (String key : Arrays.asList("title", "name", "caption", "subject")) {
            Object value = relMap.get(key);
            if (value != null && !"".equals(Convert.toStr(value))) {
                return Convert.toStr(value);
            }
        }
        for (Map.Entry<String, Object> entry : relMap.entrySet()) {
            if ("id".equals(entry.getKey())) {
                continue;
            }
            if (entry.getValue() != null && !"".equals(Convert.toStr(entry.getValue()))) {
                return Convert.toStr(entry.getValue());
            }
        }
        return Convert.toStr(fk);
    }
}
