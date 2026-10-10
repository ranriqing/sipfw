package gcs.app.vm.component;

import gcs.app.vm.bean.VmClassField;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class VmClassFieldRegistry implements ApplicationListener<ContextRefreshedEvent> {
    private static final Map<String, Class<? extends VmClassField>> allSupportedFieldsMap = new ConcurrentHashMap<>();

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        // 避免重复注册
        if (event.getApplicationContext().getParent() == null) {
            // 获取所有实现BindingStrategy接口的Bean
            Map<String, VmClassField> strategyBeans = event.getApplicationContext().getBeansOfType(VmClassField.class);
            for (VmClassField strategy : strategyBeans.values()) {
                String supportedType = strategy.getSupportedType();
                allSupportedFieldsMap.put(supportedType, strategy.getClass());
            }
        }
    }

    /**
     * 是否支持字段类型
     * @param fieldTypeStr
     * @return
     */
    public static boolean isSupport(String fieldTypeStr) {
        if (fieldTypeStr == null || fieldTypeStr.isEmpty()) {
            return false;
        }
        return allSupportedFieldsMap.containsKey(fieldTypeStr);
    }

    /**
     * 字段类型转换 String --> Class
     * @param fieldTypeStr
     * @return
     */
    public static Class<? extends VmClassField> getFieldTypeClass(String fieldTypeStr) {
        return allSupportedFieldsMap.get(fieldTypeStr);
    }
}
