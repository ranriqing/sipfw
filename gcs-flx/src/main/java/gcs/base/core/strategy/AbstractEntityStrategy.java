package gcs.base.core.strategy;

import com.baomidou.mybatisplus.extension.toolkit.Db;
import gcs.base.BaseController;
import gcs.base.tools.GcsBindingUtils;
import org.springframework.beans.BeanWrapper;

import java.util.Map;

public abstract class AbstractEntityStrategy<T> implements EntityStrategy<T> {
    @Override
    public T bind(BeanWrapper beanWrapper, Map<String, Object> jsonMap, T entity) {
        GcsBindingUtils.bind(beanWrapper, jsonMap, entity);
        return entity;
    }

    @Override
    public T save(T entity, Map<String, Object> contextMap) {
        Db.save(entity);
        return entity;
    }

    @Override
    public T update(T entity, Map<String, Object> contextMap) {
        Db.updateById(entity);
        return entity;
    }

    @Override
    public Map<String, Object> entityMap(T entity, Map<String, Object> contextMap) {
        return BaseController._entityMap(entity);
    }
}
