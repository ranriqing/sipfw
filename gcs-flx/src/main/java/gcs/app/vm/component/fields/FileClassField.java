package gcs.app.vm.component.fields;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.lang.Dict;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.FieldSpec;
import gcs.app.vm.bean.VmClassField;
import gcs.app.vm.constants.VmConstants;
import gcs.base.BaseDomain;
import gcs.base.core.bean.ParamViolation;
import gcs.base.tools.GcsBindingUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.stereotype.Component;

import javax.lang.model.element.Modifier;
import javax.persistence.Column;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
public class FileClassField extends VmClassField {


    @Override
    public String getSupportedType() {
        return VmConstants.FIELD_TYPE_FILE;
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
        FieldSpec fieldSpec = FieldSpec.builder(String.class, this.getFieldName())
                .addModifiers(Modifier.PRIVATE)
                .addAnnotation(AnnotationSpec.builder(Column.class)
                        .addMember("columnDefinition","$S", "TEXT").build())
                .build();
        return Arrays.asList(fieldSpec);
    }

    @Override
    public void bindVmField(BeanWrapper beanWrapper, BaseDomain domainEntity, Map<String, Object> jsonMap){
        JSONArray fileList = JSONUtil.parseArray(jsonMap.get(this.getFieldName()));
        if(!fileList.isEmpty()){
            List<Dict> dictList = new ArrayList<>();
            fileList.forEach(fileItem -> {
                JSONObject objItem = (JSONObject)fileItem;
                String fileUuid = objItem.getStr("fileUuid");
                String fileName = objItem.getStr("fileName");
                if(StringUtils.isNotBlank(fileUuid) && StringUtils.isNotBlank(fileName)){
                    dictList.add(Dict.create().set("fileUuid", fileUuid).set("fileName", fileName));
                }
            });
            if (beanWrapper.isWritableProperty(this.getFieldName())) {
                beanWrapper.setPropertyValue(this.getFieldName(), JSONUtil.toJsonStr(dictList));
            }
        }
    }

    @Override
    public void validateVmField(BeanWrapper beanWrapper, BaseDomain domainEntity) {
        String filesJson = (String) beanWrapper.getPropertyValue(this.getFieldName());
        if (StringUtils.isBlank(filesJson)) {
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
        String filesJson = (String) beanWrapper.getPropertyValue(this.getFieldName());
        JSONArray filesArray = JSONUtil.createArray();
        if(StringUtils.isNotBlank(filesJson)){
            filesArray = JSONUtil.parseArray(filesJson);
        }
        entityMap.put(this.getFieldName(), filesArray);
        return entityMap;
    }

    @Override
    public String formatDisplay(BeanWrapper beanWrapper, BaseDomain domainEntity){
        Object propValue = beanWrapper.isReadableProperty(this.getRealFieldName())
                ? beanWrapper.getPropertyValue(this.getRealFieldName()) : null;
        String filesJson = Convert.toStr(propValue, null);
        if (StringUtils.isBlank(filesJson)) {
            return null;
        }
        JSONArray filesArray = JSONUtil.parseArray(filesJson);
        List<String> names = new ArrayList<>();
        filesArray.forEach(fileItem -> {
            JSONObject objItem = (JSONObject) fileItem;
            String fileName = objItem.getStr("fileName");
            if (StringUtils.isNotBlank(fileName)) {
                names.add(fileName);
            }
        });
        return names.isEmpty() ? null : String.join("|", names);
    }
}
