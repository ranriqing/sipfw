package gcs.app.vm.component.fields;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.annotation.TableField;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.FieldSpec;
import gcs.app.vm.bean.VmClassField;
import gcs.app.vm.constants.VmConstants;
import gcs.base.BaseDomain;
import gcs.base.core.GcsHolder;
import gcs.base.core.bean.ParamViolation;
import gcs.base.tools.GcsBindingUtils;
import gcs.modules.config.domain.ScElement;
import gcs.modules.config.service.ScElementService;
import gcs.modules.others.domain.AddrArea;
import gcs.modules.others.domain.AddrCity;
import gcs.modules.others.domain.AddrProvince;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.BeanWrapper;
import org.springframework.stereotype.Component;

import javax.lang.model.element.Modifier;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
public class ScElClassField extends VmClassField {

    public static final String[] DEPTH_SUFFIX = {"", "Sub", "SubSub"};

    @Override
    public String getSupportedType() {
        return VmConstants.FIELD_TYPE_SC_EL;
    }

    @Override
    public void initConstraints() {
        this.setFieldConstraint(GcsBindingUtils.bind(getConstraintsMap(), FieldConstraint.class));
    }

    @Override
    public void initExtend() {
        this.setFieldExtend(GcsBindingUtils.deepBind(getExtendMap(), ScElFieldExtend.class));
    }

    @Override
    public List<FieldSpec> generateFieldSpec() {
        ScElFieldExtend fieldExtend = (ScElFieldExtend)this.getFieldExtend();
        Integer depth = fieldExtend.getRealDepth();
        List<FieldSpec> fieldSpecs = new ArrayList<>();
        // 定义各级别字段名的后缀（1-3级分别对应不同后缀）
        for (int i = 0; i < depth; i++) {
            String fieldName = this.getFieldName() + DEPTH_SUFFIX[i];
            FieldSpec levelSpec = FieldSpec.builder(Long.class, fieldName).addModifiers(Modifier.PRIVATE).build();
            fieldSpecs.add(levelSpec);
        }
        return fieldSpecs;

    }

    @Override
    public void bindVmField(BeanWrapper beanWrapper, BaseDomain domainEntity, Map<String, Object> jsonMap){
        ScElFieldExtend fieldExtend = (ScElFieldExtend)this.getFieldExtend();
        Integer depth = fieldExtend.getRealDepth();
        List identList = Convert.toList(jsonMap.get(this.getFieldName()));
        if(ObjectUtil.isNotEmpty(identList)){
            for (int i = 0; i < depth; i++) {
                String fieldName = this.getFieldName() + DEPTH_SUFFIX[i];
                if (beanWrapper.isWritableProperty(fieldName) && identList.size()>i) {
                    beanWrapper.setPropertyValue(fieldName, identList.get(i));
                }
            }
        }
        // Default value
        List<Long> defaultList = fieldExtend.getDefaultValue();
        if(defaultList != null && !defaultList.isEmpty()){
            boolean isAllNull = true;
            for (int i = 0; i < depth; i++) {
                String fieldName = this.getFieldName() + DEPTH_SUFFIX[i];
                if (beanWrapper.getPropertyValue(fieldName) != null) {
                    isAllNull = false;
                    break;
                }
            }
            if(isAllNull){
                for (int i = 0; i < depth && i < defaultList.size(); i++) {
                    String fieldName = this.getFieldName() + DEPTH_SUFFIX[i];
                    if (beanWrapper.isWritableProperty(fieldName)) {
                        beanWrapper.setPropertyValue(fieldName, defaultList.get(i));
                    }
                }
            }
        }
    }

    @Override
    public void validateVmField(BeanWrapper beanWrapper, BaseDomain domainEntity) {
        boolean isAllNull = true;
        ScElFieldExtend fieldExtend = (ScElFieldExtend)this.getFieldExtend();
        Integer depth = fieldExtend.getRealDepth();
        for (int i = 0; i < depth; i++) {
            String fieldName = this.getFieldName() + DEPTH_SUFFIX[i];
            if (beanWrapper.getPropertyValue(fieldName) != null) {
                isAllNull = false;
            }
        }
        if (isAllNull) {
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
        ScElFieldExtend fieldExtend = (ScElFieldExtend)this.getFieldExtend();
        Integer depth = fieldExtend.getRealDepth();
        List<Object> arrayList = new ArrayList<>();
        for (int i = 0; i < depth; i++) {
            String fieldName = this.getFieldName() + DEPTH_SUFFIX[i];
            arrayList.add(beanWrapper.getPropertyValue(fieldName));
        }
        entityMap.put(this.getFieldName(), arrayList);
        return entityMap;
    }

    @Override
    public String formatDisplay(BeanWrapper beanWrapper, BaseDomain domainEntity){
        ScElFieldExtend fieldExtend = (ScElFieldExtend) this.getFieldExtend();
        Integer depth = fieldExtend.getRealDepth();
        String scKey = fieldExtend.getScKey();
        ScElementService scElementService = GcsHolder.getBean(ScElementService.class);
        List<String> titles = new ArrayList<>();
        for (int i = 0; i < depth; i++) {
            String subFieldName = this.getFieldName() + DEPTH_SUFFIX[i];
            Object propValue = beanWrapper.isReadableProperty(subFieldName)
                    ? beanWrapper.getPropertyValue(subFieldName) : null;
            Long id = Convert.toLong(propValue);
            if (id != null) {
                ScElement scElement = scElementService.getScElement(scKey, id);
                titles.add(scElement != null ? scElement.getTitle() : Convert.toStr(id));
            }
        }
        return titles.isEmpty() ? null : String.join("/", titles);
    }

    @Data
    @NoArgsConstructor
    public static class ScElFieldExtend extends FieldExtend {
        private Integer depth = 1;
        private String scKey;
        private List<Long> defaultValue;

        // 控制最多3级（1-3级）
        public Integer getRealDepth(){
            return (depth == null) ? 1 : Math.min(depth, 3);
        }
    }
}
