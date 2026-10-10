package gcs.base.core.strategy;

import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class EntityStrategyRegistry implements ApplicationListener<ContextRefreshedEvent> {
    private final Map<Class<?>, EntityStrategy<?>> registeredStrategies = new ConcurrentHashMap<>();

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        // 避免重复注册
        if (event.getApplicationContext().getParent() == null) {
            // 获取所有实现BindingStrategy接口的Bean
            Map<String, EntityStrategy> strategyBeans = event.getApplicationContext().getBeansOfType(EntityStrategy.class);
            for (EntityStrategy<?> strategy : strategyBeans.values()) {
                Class<?> supportedType = strategy.getSupportedType();
                registeredStrategies.put(supportedType, strategy);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public <T> EntityStrategy<T> getStrategy(Class<T> clazz) {
        return (EntityStrategy<T>) registeredStrategies.entrySet().stream()
                .filter(e -> e.getValue().getSupportedType().isAssignableFrom(clazz))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }
}