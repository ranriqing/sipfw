package gcs.app.vm.bean;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import gcs.base.BaseController;
import gcs.base.BaseDomain;
import gcs.base.tools.GcsBindingUtils;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public abstract class VmClass {

    private Class<? extends BaseDomain> domainClass;
    private BaseMapper domainMapper;
    private String domainName;

    public BaseDomain newVmDomain(){
        try{
            return domainClass.getDeclaredConstructor().newInstance();
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    public abstract Map<String, VmClassField> getClassFieldMap();
    public abstract void setClassFieldMap(Map<String, VmClassField> classFieldMap);

    public abstract BaseDomain bindVmDomain(BaseDomain domainEntity, Map<String, Object> jsonMap);

    public abstract void validateVmDomain(BaseDomain domainEntity);

    public abstract BaseDomain beforeSaveVmDomain(BaseDomain domainEntity);

    public abstract BaseDomain saveVmDomain(BaseDomain domainEntity);

    public abstract BaseDomain postSaveVmDomain(BaseDomain domainEntity);

    public abstract BaseDomain updateVmDomain(BaseDomain domainEntity);

    public abstract BaseDomain beforeUpdateVmDomain(BaseDomain domainEntity);

    public abstract BaseDomain postUpdateVmDomain(BaseDomain domainEntity);

    public abstract Map<String, Object> vmDomainEntityMap(BaseDomain domainEntity);

    public abstract Map<String, Object> vmDomainEntityMap(BaseDomain domainEntity,  Map<String, Object> contextMap);
}
