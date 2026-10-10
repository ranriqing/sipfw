package gcs.app.vm.component.fields;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.ObjectUtil;
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
import java.util.*;

@Component
public class SelectClassField extends VmClassField {

    @Override
    public String getSupportedType() {
        return VmConstants.FIELD_TYPE_SELECT;
    }

    @Override
    public void initConstraints() {
        this.setFieldConstraint(GcsBindingUtils.bind(getConstraintsMap(), FieldConstraint.class));
    }

    @Override
    public void initExtend() {
        Map<String, Object> extendMap = getExtendMap();
        // 兼容旧格式 {label: value} → 新格式 [{label, value}]
        if (extendMap != null) {
            Object optionsObj = extendMap.get("options");
            if (optionsObj instanceof Map && !((Map<?, ?>) optionsObj).isEmpty()) {
                Map<?, ?> optionsMap = (Map<?, ?>) optionsObj;
                List<Map<String, Object>> newOptions = new ArrayList<>();
                for (Map.Entry<?, ?> entry : optionsMap.entrySet()) {
                    Map<String, Object> opt = new HashMap<>();
                    opt.put("label", String.valueOf(entry.getKey()));
                    opt.put("value", entry.getValue());
                    newOptions.add(opt);
                }
                extendMap.put("options", newOptions);
            }
        }
        this.setFieldExtend(GcsBindingUtils.deepBind(extendMap, SelectFieldExtend.class));
    }

    @Override
    public void bindVmField(BeanWrapper beanWrapper, BaseDomain domainEntity, Map<String, Object> jsonMap) {
        super.bindVmField(beanWrapper, domainEntity, jsonMap);
        SelectFieldExtend extend = (SelectFieldExtend) this.getFieldExtend();
        if (extend.getDefaultValue() != null) {
            Object currentValue = beanWrapper.getPropertyValue(this.getRealFieldName());
            if (currentValue == null) {
                beanWrapper.setPropertyValue(this.getRealFieldName(), extend.getDefaultValue());
            }
        }
    }

    @Override
    public List<FieldSpec> generateFieldSpec() {
        FieldSpec fieldSpec = FieldSpec.builder(Integer.class, this.getFieldName())
                .addModifiers(Modifier.PRIVATE)
                .build();
        return Arrays.asList(fieldSpec);

    }

    @Override
    public void validateVmField(BeanWrapper beanWrapper, BaseDomain domainEntity) {
        SelectFieldExtend fieldExtend = (SelectFieldExtend)this.getFieldExtend();
        Integer fieldValue = (Integer)beanWrapper.getPropertyValue(this.getFieldName());
        if(fieldValue == null){
            if(!this.getFieldConstraint().getNullable()){
                domainEntity.getViolationList().add(
                        new ParamViolation(
                                this.getFieldName(),
                                null,
                                "[" + this.getFieldTitle() + "]不能为空"
                        )
                );
            }
        }else{
            if(!fieldExtend.containsValue(fieldValue)){
                domainEntity.getViolationList().add(
                        new ParamViolation(
                                this.getFieldName(),
                                fieldValue,
                                "[" + this.getFieldTitle() + "]选项不存在"
                        )
                );
            }
        }
    }

    @Override
    public String formatDisplay(BeanWrapper beanWrapper, BaseDomain domainEntity){
        Object propValue = beanWrapper.isReadableProperty(this.getRealFieldName())
                ? beanWrapper.getPropertyValue(this.getRealFieldName()) : null;
        return formatSelectValue(propValue);
    }

    private String formatSelectValue(Object propValue){
        if (propValue == null) {
            return null;
        }
        SelectFieldExtend fieldExtend = (SelectFieldExtend) this.getFieldExtend();
        Integer value = Convert.toInt(propValue);
        Optional<Map.Entry<String, Integer>> firstEntry = fieldExtend.getOptionsMap().entrySet().stream()
                .filter(entry -> entry.getValue() != null && entry.getValue().equals(value))
                .findFirst();
        return firstEntry.map(Map.Entry::getKey).orElse(Convert.toStr(propValue));
    }

    @Data
    @NoArgsConstructor
    public static class SelectFieldExtend extends FieldExtend {
        private List<SelectOption> options = new ArrayList<>();
        private Integer defaultValue;

        public Map<String, Integer> getOptionsMap() {
            Map<String, Integer> map = new LinkedHashMap<>();
            if (options != null) {
                options.forEach(opt -> {
                    if (opt.getLabel() != null && opt.getValue() != null) {
                        map.put(opt.getLabel(), opt.getValue());
                    }
                });
            }
            return map;
        }

        public boolean containsValue(Integer value) {
            if (options == null || value == null) return false;
            return options.stream().anyMatch(opt -> value.equals(opt.getValue()));
        }
    }

    @Data
    @NoArgsConstructor
    public static class SelectOption {
        private String label;
        private Integer value;
        private String color;
    }
}
