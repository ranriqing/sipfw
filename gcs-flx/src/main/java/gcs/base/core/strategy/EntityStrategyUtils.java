package gcs.base.core.strategy;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import gcs.base.BaseController;
import gcs.base.core.GcsHolder;
import gcs.base.tools.GcsBindingUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

import java.util.Map;

/**
 * 暂未使用
 */
public class EntityStrategyUtils {

    private static EntityStrategyRegistry _entityStrategyRegistry;

    /** 数据绑定 */
    public static <T> T bind(Map<String, Object> jsonMap, Class<T> clazz) {
        try {
            T entity = clazz.getDeclaredConstructor().newInstance();
            return bind(jsonMap, entity);
        }catch (Exception e) {
            throw new RuntimeException("Failed to create an instance of the class: " + clazz.getName(), e);
        }
    }
    public static <T> T bind(Map<String, Object> jsonMap, T entity) {
        BeanWrapper beanWrapper = new BeanWrapperImpl(entity);
        beanWrapper.setConversionService(GcsBindingUtils.getConversionService());
        GcsBindingUtils.bind(beanWrapper, jsonMap, entity);
        EntityStrategy<T> strategy = (EntityStrategy<T>) getEntityStrategyRegistry().getStrategy(entity.getClass());
        if (strategy != null) {
            return strategy.bind(beanWrapper, jsonMap, entity);
        }
        return entity;
    }

    /** 保存更新 */
    public static <T> T save(T entity) {
        return save(entity, null);
    }
    public static <T> T save(T entity, Map<String, Object> contextMap) {
        EntityStrategy<T> strategy = (EntityStrategy<T>) getEntityStrategyRegistry().getStrategy(entity.getClass());
        if (strategy != null) {
            return strategy.save(entity, contextMap);
        }
        Db.save(entity);
        return entity;
    }
    public static <T> T update(T entity) {
        return update(entity, null);
    }
    public static <T> T update(T entity, Map<String, Object> contextMap) {
        EntityStrategy<T> strategy = (EntityStrategy<T>) getEntityStrategyRegistry().getStrategy(entity.getClass());
        if (strategy != null) {
            return strategy.update(entity, contextMap);
        }
        Db.updateById(entity);
        return entity;
    }

    /** 数据转Map*/
    public static <T> Map<String, Object> entityMap(T entity){
        return entityMap(entity, null);
    }
    public static <T> Map<String, Object> entityMap(T entity, Map<String, Object> contextMap) {
        if(ObjectUtil.isNull(entity)){
            return null;
        }
        EntityStrategy<T> strategy = (EntityStrategy<T>) getEntityStrategyRegistry().getStrategy(entity.getClass());
        if (strategy != null) {
            return strategy.entityMap(entity, contextMap);
        }
        return BaseController._entityMap(entity);
    }

    private static EntityStrategyRegistry getEntityStrategyRegistry(){
        if(_entityStrategyRegistry != null){
            return _entityStrategyRegistry;
        }
        _entityStrategyRegistry = GcsHolder.getBean(EntityStrategyRegistry.class);
        return _entityStrategyRegistry;
    }
}
