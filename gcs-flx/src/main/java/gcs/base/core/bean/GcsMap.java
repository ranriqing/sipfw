package gcs.base.core.bean;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 支持类型转换的 Map 实现，提供便捷的类型转换方法
 */
public class GcsMap extends AbstractGcsMap {

    /**
     * 构造函数
     */
    public GcsMap() {
        super();
    }
    public GcsMap(Map<String, Object> map) {
        super(map);
    }

    /**
     * 克隆方法
     */
    @Override
    public Object clone() {
        return new GcsMap(new LinkedHashMap<>(super.getWrappedMap()));
    }

    // 类型转换快捷方法
    public Byte byteValue(String name) {
        return getByte(name);
    }

    public Byte byteValue(String name, Integer defaultValue) {
        return getByte(name, defaultValue);
    }

    public Character charValue(String name) {
        return getChar(name);
    }

    public Character charValue(String name, Character defaultValue) {
        return getChar(name, defaultValue != null ? (int) defaultValue : null);
    }

    public Character charValue(String name, Integer defaultValue) {
        return getChar(name, defaultValue);
    }

    public Integer intValue(String name) {
        return getInt(name);
    }

    public Integer intValue(String name, Integer defaultValue) {
        return getInt(name, defaultValue);
    }

    public Long longValue(String name) {
        return getLong(name);
    }

    public Long longValue(String name, Long defaultValue) {
        return getLong(name, defaultValue);
    }

    public Short shortValue(String name) {
        return getShort(name);
    }

    public Short shortValue(String name, Integer defaultValue) {
        return getShort(name, defaultValue);
    }

    public Double doubleValue(String name) {
        return getDouble(name);
    }

    public Double doubleValue(String name, Double defaultValue) {
        return getDouble(name, defaultValue);
    }

    public Float floatValue(String name) {
        return getFloat(name);
    }

    public Float floatValue(String name, Float defaultValue) {
        return getFloat(name, defaultValue);
    }

    public Boolean booleanValue(String name) {
        return getBoolean(name);
    }

    public Boolean booleanValue(String name, Boolean defaultValue) {
        return getBoolean(name, defaultValue);
    }

    public String stringValue(String name) {
        return getString(name);
    }

    public String stringValue(String name, String defaultValue) {
        return getString(name, defaultValue);
    }
}