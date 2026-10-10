package gcs.base.core.strategy;

import org.springframework.beans.BeanWrapper;

import java.util.Map;

public interface EntityStrategy<T> {
    /** 数据绑定 */
    T bind(BeanWrapper beanWrapper, Map<String, Object> jsonMap, T entity);

    /** 保存更新 */
    T save(T entity, Map<String, Object> contextMap);
    T update(T entity, Map<String, Object> contextMap);

    /** 响应Map */
    Map<String, Object>  entityMap(T entity, Map<String, Object> contextMap);

    Class<?> getSupportedType();
}
