package gcs.app.vm.component;

import gcs.app.vm.bean.VmCodePoet;
import gcs.app.vm.utils.VmUtils;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class VmCodePoetRegistry implements ApplicationListener<ContextRefreshedEvent> {
    private static final Map<String, VmCodePoet> allSupportedMap = new ConcurrentHashMap<>();

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        // 避免重复注册
        if (event.getApplicationContext().getParent() == null) {
            // 获取所有实现特定接口的Bean
            Map<String, VmCodePoet> vmCodePoetBeans = event.getApplicationContext().getBeansOfType(VmCodePoet.class);
            for (VmCodePoet vmCodePoet : vmCodePoetBeans.values()) {
                String  camelDomainName = VmUtils.toUpperCamel(vmCodePoet.getSupportedDomain());
                allSupportedMap.put(camelDomainName, vmCodePoet);
            }
        }
    }

    /** 是否支持 */
    public static boolean isSupport(String domainName) {
        return allSupportedMap.containsKey(domainName);
    }

    /** 查找注册的Bean */
    public static VmCodePoet getVmCodePoet(String domainName) {
        String  camelDomainName = VmUtils.toUpperCamel(domainName);
        if(isSupport(camelDomainName)) {
            return allSupportedMap.get(camelDomainName);
        }else{
            return new VmCodePoet();
        }
    }
}
