package gcs.app.vm.bean;

import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import gcs.base.BaseDomain;
import gcs.base.core.exception.GcsParamException;
import gcs.base.core.validators.ValidatorUtils;
import gcs.base.tools.GcsBindingUtils;
import gcs.base.tools.GcsEntityUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * 每个实例对应一个CodeVmClass的实现或CodeVmClass的对象
 */
public class FixVmClass extends VmClass{

    protected Map<String, VmClassField> _classFieldMap = new HashMap<>();

    public Class<? extends BaseDomain> getSupportedType(){
        return null;
    }

    @Override
    public Map<String, VmClassField> getClassFieldMap(){
        return _classFieldMap;
    }
    @Override
    public void setClassFieldMap(Map<String, VmClassField> classFieldMap){
        this._classFieldMap = classFieldMap;
    }

    @Override
    public BaseDomain bindVmDomain(BaseDomain domainEntity, Map<String, Object> jsonMap){
        // 基础绑定
        GcsBindingUtils.bind(jsonMap, domainEntity);
        // 字段绑定
        BeanWrapper beanWrapper = new BeanWrapperImpl(domainEntity);
        beanWrapper.setConversionService(GcsBindingUtils.getConversionService());
        for (Map.Entry<String, VmClassField> entry : getClassFieldMap().entrySet()) {
            VmClassField vmClassField = entry.getValue();
            vmClassField.bindVmField(beanWrapper, domainEntity, jsonMap);
        }
        return domainEntity;
    }

    @Override
    public void validateVmDomain(BaseDomain domainEntity){
        // 基础校验
        ValidatorUtils.validateObj(domainEntity);
        // 字段校验
        domainEntity.setViolationList(new ArrayList<>());
        BeanWrapper beanWrapper = new BeanWrapperImpl(domainEntity);
        beanWrapper.setConversionService(GcsBindingUtils.getConversionService());
        for (Map.Entry<String, VmClassField> entry : getClassFieldMap().entrySet()) {
            VmClassField vmClassField = entry.getValue();
            vmClassField.validateVmField(beanWrapper, domainEntity);
        }
        if(!domainEntity.getViolationList().isEmpty()){
            throw new GcsParamException("校验失败", domainEntity.getViolationList());
        }
    }

    @Override
    public BaseDomain beforeSaveVmDomain(BaseDomain domainEntity){
        BeanWrapper beanWrapper = new BeanWrapperImpl(domainEntity);
        for (Map.Entry<String, VmClassField> entry : getClassFieldMap().entrySet()) {
            VmClassField vmClassField = entry.getValue();
            vmClassField.beforeSaveVmField(beanWrapper, domainEntity);
        }
        return domainEntity;
    }

    @Override
    public BaseDomain saveVmDomain(BaseDomain domainEntity){
        this.beforeSaveVmDomain(domainEntity);
        SqlHelper.retBool(getDomainMapper().insert(domainEntity));
        this.postSaveVmDomain(domainEntity);
        return domainEntity;
    }

    @Override
    public BaseDomain postSaveVmDomain(BaseDomain domainEntity){
        BeanWrapper beanWrapper = new BeanWrapperImpl(domainEntity);
        for (Map.Entry<String, VmClassField> entry : getClassFieldMap().entrySet()) {
            VmClassField vmClassField = entry.getValue();
            vmClassField.postSaveVmField(beanWrapper, domainEntity);
        }
        return domainEntity;
    }

    @Override
    public BaseDomain beforeUpdateVmDomain(BaseDomain domainEntity){
        BeanWrapper beanWrapper = new BeanWrapperImpl(domainEntity);
        for (Map.Entry<String, VmClassField> entry : getClassFieldMap().entrySet()) {
            VmClassField vmClassField = entry.getValue();
            vmClassField.beforeUpdateVmField(beanWrapper, domainEntity);
        }
        return domainEntity;
    }

    @Override
    public BaseDomain updateVmDomain(BaseDomain domainEntity){
        this.beforeUpdateVmDomain(domainEntity);
        SqlHelper.retBool(getDomainMapper().updateById(domainEntity));
        this.postUpdateVmDomain(domainEntity);
        return domainEntity;
    }

    @Override
    public BaseDomain postUpdateVmDomain(BaseDomain domainEntity){
        BeanWrapper beanWrapper = new BeanWrapperImpl(domainEntity);
        for (Map.Entry<String, VmClassField> entry : getClassFieldMap().entrySet()) {
            VmClassField vmClassField = entry.getValue();
            vmClassField.postUpdateVmField(beanWrapper, domainEntity);
        }
        return domainEntity;
    }

    @Override
    public Map<String, Object> vmDomainEntityMap(BaseDomain domainEntity){
        return vmDomainEntityMap(domainEntity, null);
    }

    @Override
    public Map<String, Object> vmDomainEntityMap(BaseDomain domainEntity,  Map<String, Object> contextMap){
        BeanWrapper beanWrapper = new BeanWrapperImpl(domainEntity);
        beanWrapper.setConversionService(GcsBindingUtils.getConversionService());
        Map<String, Object> entityMap = GcsEntityUtils.toMap(domainEntity);
        for (Map.Entry<String, VmClassField> entry : getClassFieldMap().entrySet()) {
            VmClassField vmClassField = entry.getValue();
            vmClassField.enrichMap(beanWrapper, domainEntity, entityMap);
        }
        // 扩展信息
        entityMap.put(BaseDomain.ID, beanWrapper.getPropertyValue(BaseDomain.ID));
        entityMap.put(BaseDomain.CREATOR_ID, domainEntity.getCreaterId());
        entityMap.put(BaseDomain.UPDATER_ID, domainEntity.getUpdaterId());
        entityMap.put(BaseDomain.DATE_CREATED, domainEntity.getDateCreated());
        entityMap.put(BaseDomain.LAST_UPDATED, domainEntity.getLastUpdated());
        return entityMap;
    }

}
