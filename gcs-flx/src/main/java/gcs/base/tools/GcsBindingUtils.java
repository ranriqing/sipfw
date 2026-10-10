package gcs.base.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import gcs.app.vm.bean.VmClass;
import gcs.app.vm.service.VmClassService;
import gcs.base.BaseDomain;
import gcs.base.FloDomain;
import gcs.base.core.GcsHolder;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.core.convert.ConversionService;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

// 参考WebDataBinder
public class GcsBindingUtils {

    private static ConversionService _conversionService;

    /**
     * 创建对象并绑定
     * @param jsonMap
     * @param clazz
     * @param <T>
     * @return
     */
    public static <T> T deepBind(Map<String, Object> jsonMap, Class<T> clazz) {
        return GcsEntityUtils.getObjectMapper().convertValue(jsonMap, clazz);
    }

    /**
     * 绑定到已有对象
     * @param jsonMap
     * @param entity
     * @param <T>
     * @return
     */
    public static <T> T deepBind(Map<String, Object> jsonMap, T entity) {
        ObjectMapper objectMapper = GcsEntityUtils.getObjectMapper();
        return objectMapper.convertValue(jsonMap, objectMapper.constructType(entity.getClass()));
    }

    /**
     * 创建对象并绑定
     * @param jsonMap
     * @param clazz
     * @param <T>
     * @return
     */
    public static <T> T bind(Map<String, Object> jsonMap, Class<T> clazz) {
        try {
            T entity = clazz.getDeclaredConstructor().newInstance();
            return bind(jsonMap, entity);
        }catch (Exception e) {
            throw new RuntimeException("Failed to create an instance of the class: " + clazz.getName(), e);
        }
    }

    /**
     * 绑定到已有对象
     * @param jsonMap
     * @param entity
     * @param <T>
     * @return
     */
    public static <T> T bind(Map<String, Object> jsonMap, T entity) {
        BeanWrapper beanWrapper = new BeanWrapperImpl(entity);
        beanWrapper.setConversionService(getConversionService());
        return bind(beanWrapper, jsonMap, entity);
    }
    public static <T> T bind(BeanWrapper beanWrapper, Map<String, Object> jsonMap, T entity) {
        for (Map.Entry<String, Object> entry : jsonMap.entrySet()) {
            String propertyName = entry.getKey();
            Object value = entry.getValue();
            // 忽略部分特殊字段
            if(isIgnoreProperty(propertyName)) {
                continue;
            }
            if (beanWrapper.isWritableProperty(propertyName)) {
                Class<?> propertyType = beanWrapper.getPropertyType(propertyName);
                // 基本类型和关联关系处理
                if(isBasicType(propertyType)) {
                    if(propertyType == String.class && !(value instanceof String)) {
                        beanWrapper.setPropertyValue(propertyName, value==null?null:new Gson().toJson(value));
                    }else{
                        beanWrapper.setPropertyValue(propertyName, value);
                    }
                }else if(List.class.isAssignableFrom(propertyType)) {
                    // 仅绑定基本类型元素的 List (List<String>/List<Date>/...)
                    java.lang.reflect.Type genericType = beanWrapper.getPropertyDescriptor(propertyName)
                            .getWriteMethod().getGenericParameterTypes()[0];
                    if (isBasicListElementType(genericType)) {
                        beanWrapper.setPropertyValue(propertyName, value);
                    }
                }
            }
        }
        return entity;
    }

    // 工具方法
    private static boolean isIgnoreProperty(String propertyName) {
        return BaseDomain.ID.equals(propertyName) || BaseDomain.VERSION.equals(propertyName) ||
            BaseDomain.CREATOR_ID.equals(propertyName) || BaseDomain.UPDATER_ID.equals(propertyName) ||
            BaseDomain.DATE_CREATED.equals(propertyName) || BaseDomain.LAST_UPDATED.equals(propertyName) ||
            FloDomain.FLOW_DEF_KEY.equals(propertyName) || FloDomain.FLOW_INST_ID.equals(propertyName) || FloDomain.FLOW_STATUS.equals(propertyName) ||
            FloDomain.FLOW_SUBMIT_TIME.equals(propertyName) || FloDomain.FLOW_UPDATE_TIME.equals(propertyName) || FloDomain.FLOW_FINISH_TIME.equals(propertyName);
    }
    private static boolean isBasicType(Class<?> type) {
        return type == Integer.class || type == int.class ||
                type == Long.class || type == long.class ||
                type == Boolean.class || type == boolean.class ||
                type == String.class || type == Date.class ||
                type == BigDecimal.class;
    }
    /**
     * 判断 List 泛型元素是否为基本类型
     */
    private static boolean isBasicListElementType(java.lang.reflect.Type genericType) {
        if (genericType instanceof java.lang.reflect.ParameterizedType) {
            java.lang.reflect.Type[] typeArgs =
                    ((java.lang.reflect.ParameterizedType) genericType).getActualTypeArguments();
            if (typeArgs.length == 1 && typeArgs[0] instanceof Class) {
                return isBasicType((Class<?>) typeArgs[0]);
            }
        }
        return false;
    }
    public static ConversionService getConversionService(){
        if(_conversionService != null){
            return _conversionService;
        }
        _conversionService = GcsHolder.getBean(ConversionService.class);
        return _conversionService;
    }

}
