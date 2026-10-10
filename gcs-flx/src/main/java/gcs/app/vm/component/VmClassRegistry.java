package gcs.app.vm.component;

import gcs.app.vm.bean.DynVmClass;
import gcs.app.vm.bean.FixVmClass;
import gcs.app.vm.utils.VmUtils;
import gcs.base.BaseDomain;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class VmClassRegistry implements ApplicationListener<ContextRefreshedEvent> {
    private static final Map<Class<? extends BaseDomain>, FixVmClass> fixVmClasses = new ConcurrentHashMap<>();
    private static final Map<String, DynVmClass> dynVmClasses = new ConcurrentHashMap<>();

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        // 避免重复注册
        if (event.getApplicationContext().getParent() == null) {
            // 获取所有实现特定接口的Bean
            Map<String, FixVmClass> strategyFixBeans = event.getApplicationContext().getBeansOfType(FixVmClass.class);
            for (FixVmClass fixedVmClass : strategyFixBeans.values()) {
                Class<? extends BaseDomain> supportedType = fixedVmClass.getSupportedType();
                fixVmClasses.put(supportedType, fixedVmClass);
            }
            Map<String, DynVmClass> strategyDynBeans = event.getApplicationContext().getBeansOfType(DynVmClass.class);
            for (DynVmClass dynVmClass : strategyDynBeans.values()) {
                String  camelDomainName = VmUtils.toUpperCamel(dynVmClass.getSupportedDomain());
                dynVmClasses.put(camelDomainName, dynVmClass);
            }
        }
    }

    /**
     * 是否支持
     */
    public static boolean isFixSupport(Class clazz) {
        return fixVmClasses.containsKey(clazz);
    }
    public static boolean isDynSupport(String domainName) {
        return dynVmClasses.containsKey(domainName);
    }

    /**
     * 查找注册的VmClass
     * @param clazz
     * @return
     */
    public static FixVmClass getFixVmClass(Class clazz) {
        if(isFixSupport(clazz)) {
            return fixVmClasses.get(clazz);
        }else{
            return new FixVmClass();
        }
    }
    public static DynVmClass getDynVmClass(String domainName) {
        String  camelDomainName = VmUtils.toUpperCamel(domainName);
        if(isDynSupport(camelDomainName)) {
            return dynVmClasses.get(camelDomainName);
        }else{
            return new DynVmClass();
        }
    }
}
