package gcs.base.core.convertors;

import org.springframework.core.convert.support.DefaultConversionService;

/**
 * 类型转换工具
 */
public class TypeConvertorUtils {

    // Spring的类型转换Service
    static DefaultConversionService conversionService = new DefaultConversionService();
    static {
        // 扩展Convertors
        conversionService.addConverter(new StringToIntRangeConverter(conversionService));
    }

    /**
     * 根据 targetClazz 来对 constraintValue 执行转换
     * @param source
     * @param targetType
     * @return
     */
    public static <T> T convert(String source, Class<T> targetType){
        // 执行类型转换
        if(source!=null && targetType!=null){
            if(conversionService.canConvert(String.class, targetType)){
                return conversionService.convert(source, targetType);
            }else{
                return null;
            }
        }else{
            return null;
        }
    }
}
