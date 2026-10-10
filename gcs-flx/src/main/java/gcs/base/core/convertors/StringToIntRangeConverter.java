package gcs.base.core.convertors;

import org.apache.commons.lang.math.IntRange;
import org.apache.commons.lang3.Range;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.Set;

public class StringToIntRangeConverter implements ConditionalGenericConverter {

    private final ConversionService conversionService;

    public StringToIntRangeConverter(ConversionService conversionService) {
        this.conversionService = conversionService;
    }

    @Override
    public Set<ConvertiblePair> getConvertibleTypes() {
        return Collections.singleton(new GenericConverter.ConvertiblePair(String.class, Range.class));
    }

    @Override
    public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
        return this.conversionService.canConvert(sourceType, targetType.getElementTypeDescriptor());
    }

    @Override
    public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
        if (source == null) {
            return null;
        }
        String string = (String) source;
        String[] fields = StringUtils.delimitedListToStringArray(string, "..");

        // 执行类型转换
        Integer rangeFrom = this.conversionService.convert(fields[0].trim(), Integer.class);
        Integer rangeTo = this.conversionService.convert(fields[1].trim(), Integer.class);
        IntRange targetRange = new IntRange(rangeFrom, rangeTo);
        return targetRange;
    }
}
