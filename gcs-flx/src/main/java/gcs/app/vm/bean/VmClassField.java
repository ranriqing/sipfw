package gcs.app.vm.bean;

import cn.hutool.core.convert.Convert;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.squareup.javapoet.FieldSpec;
import gcs.base.BaseDomain;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.BeanWrapper;

import javax.persistence.Transient;
import java.util.List;
import java.util.Map;

@Data
public abstract class VmClassField {

    public static final String ID_SUFFIX = "Id";

    private Long id;
    private VmClass vmClass;
    private String fieldTitle;                         // 字段标题, 如 客户名
    private String fieldName;                          // 字段名，如 cusName
    private String fieldType;                          // 字段类型，如 String
    private Integer seqOrder;                          // 字段排序，越小越靠前
    private JSONObject extendMap;                      // 扩展信息
    private JSONObject constraintsMap;                 // 字段约束

    @Transient // Lombok忽略
    private FieldConstraint _fieldConstraint = new FieldConstraint();
    @Transient // Lombok忽略
    private FieldExtend _fieldExtend = new FieldExtend();

    /* ---------- 加载字段部分 ---------- */
    /**
     * 模板方法：支持类型
     * @return
     */
    public abstract String getSupportedType();

    /**
     * 是否支持导入
     */
    public Boolean supportsImport() {
        return true;
    }

    /**
     * 是否支持批量编辑
     */
    public Boolean supportsBatchEdit(){
        return true;
    }

    /* ---------- 字段初始化部分 ---------- */
    /**
     * 初始化VmField
     */
    public void init(){
        if(constraintsMap == null){
            constraintsMap = JSONUtil.createObj();
        }
        if(extendMap == null){
            extendMap = JSONUtil.createObj();
        }
        this.initExtend();
        this.initConstraints();
    }
    /**
     * 模板方法：初始化约束条件
     */
    public void initConstraints() {
    }
    public void setFieldConstraint(FieldConstraint fieldConstraint){
        this._fieldConstraint = fieldConstraint;
    }
    public FieldConstraint getFieldConstraint(){
        return this._fieldConstraint;
    }

    /**
     * 模板方法：初始化扩展条件
     */
    public void initExtend(){
    };
    public void setFieldExtend(FieldExtend fieldExtend){
        this._fieldExtend = fieldExtend;
    }
    public FieldExtend getFieldExtend(){
        return this._fieldExtend;
    }

    /* ---------- 字段使用部分 ---------- */
    /** 模板方法：字段代码 */
    public abstract List<FieldSpec> generateFieldSpec();

    /** 模板方法：真实字段名 */
    public String getRealFieldName(){
        return fieldName;
    }

    /** 模板方法：字段绑定 */
    public void bindVmField(BeanWrapper beanWrapper, BaseDomain domainEntity, Map<String, Object> jsonMap){
        if (beanWrapper.isWritableProperty(this.getRealFieldName())) {
            // 忽略未显式提交FieldName的字段
            if(jsonMap.containsKey(this.getRealFieldName())){
                beanWrapper.setPropertyValue(this.getRealFieldName(), jsonMap.get(this.getRealFieldName()));
            }
        }
    }

    /** 模板方法：校验字段 */
    public void validateVmField(BeanWrapper beanWrapper, BaseDomain domainEntity){
        // do nothing
    }

    public void beforeSaveVmField(BeanWrapper beanWrapper, BaseDomain domainEntity){
        // do nothing
    }

    public void postSaveVmField(BeanWrapper beanWrapper, BaseDomain domainEntity){
        // do nothing
    }

    public void beforeUpdateVmField(BeanWrapper beanWrapper, BaseDomain domainEntity){
        // do nothing
    }

    public void postUpdateVmField(BeanWrapper beanWrapper, BaseDomain domainEntity){
        // do nothing
    }

    /** 模板方法：响应Map */
    public Map<String, Object> enrichMap(BeanWrapper beanWrapper, Object entity, Map<String, Object> entityMap){
        Object propertyValue = null;
        if(beanWrapper.isReadableProperty(this.getRealFieldName())){
            propertyValue = beanWrapper.getPropertyValue(this.getRealFieldName());
            entityMap.put(this.getRealFieldName(), propertyValue);
        }
        return entityMap;
    }

    /** 模板方法：字段展示值（用于变更记录，默认返回真实字段原始值字符串） */
    public String formatDisplay(BeanWrapper beanWrapper, BaseDomain domainEntity){
        if (beanWrapper.isReadableProperty(this.getRealFieldName())) {
            return Convert.toStr(beanWrapper.getPropertyValue(this.getRealFieldName()), null);
        }
        return null;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("VmClassField{");
        sb.append("fieldTitle='").append(fieldTitle).append('\'');
        sb.append(", fieldType='").append(fieldType).append('\'');
        sb.append(", fieldName='").append(fieldName).append('\'');
        sb.append('}');
        return sb.toString();
    }

    /* ---------- 约束定义部分 ---------- */
    @Data
    @NoArgsConstructor
    public static class FieldConstraint{
        protected Boolean nullable = false;
    }

    /* ---------- 扩展定义部分 ---------- */
    @Data
    @NoArgsConstructor
    public static class FieldExtend{
    }
}
