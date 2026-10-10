package gcs.base.core.bean;

import gcs.base.core.GcsConstants;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 抽象基类，提供类型转换 Map 的核心功能
 */
public abstract class AbstractGcsMap implements Map<String, Object>, Cloneable {

    private static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.S";
    protected Map<String, Object> wrappedMap;

    /**
     * 默认构造函数
     */
    public AbstractGcsMap() {
        this(new LinkedHashMap<>());
    }

    /**
     * 从现有 Map 构造
     */
    public AbstractGcsMap(Map<String, Object> map) {
        this.wrappedMap = map != null ? map : new LinkedHashMap<>();
    }

    public Map<String, Object> getWrappedMap() {
        return wrappedMap;
    }

    // 对象比较方法
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AbstractGcsMap that = (AbstractGcsMap) o;
        if (wrappedMap.size() != that.wrappedMap.size()) return false;
        for (Entry<String, Object> entry : wrappedMap.entrySet()) {
            String key = entry.getKey();
            Object thisValue = entry.getValue();
            Object thatValue = that.wrappedMap.get(key);
            if (thisValue == null) {
                if (thatValue != null) return false;
            } else if (!thisValue.equals(thatValue)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = wrappedMap != null ? wrappedMap.hashCode() : 0;
        return result;
    }

    // 类型转换方法
    public Byte getByte(String name) {
        Object o = get(name);
        if (o instanceof Number) {
            return ((Number) o).byteValue();
        } else {
            if (o != null) {
                try {
                    String string = o.toString();
                    if (string != null && string.length() > 0) {
                        return Byte.parseByte(string);
                    }
                } catch (NumberFormatException ignored) {
                }
            }
            return null;
        }
    }

    public Byte getByte(String name, Integer defaultValue) {
        Byte value = getByte(name);
        return value != null ? value : (defaultValue != null ? defaultValue.byteValue() : null);
    }

    public Character getChar(String name) {
        Object o = get(name);
        if (o instanceof Character) {
            return (Character) o;
        } else {
            if (o != null) {
                String string = o.toString();
                if (string != null && string.length() == 1) {
                    return string.charAt(0);
                }
            }
            return null;
        }
    }

    public Character getChar(String name, Integer defaultValue) {
        Character value = getChar(name);
        return value != null ? value : (defaultValue != null ? (char) defaultValue.intValue() : null);
    }

    public Integer getInt(String name) {
        Object o = get(name);
        if (o instanceof Number) {
            return ((Number) o).intValue();
        } else {
            if (o != null) {
                try {
                    String string = o.toString();
                    if (string != null) {
                        return Integer.parseInt(string);
                    }
                } catch (NumberFormatException ignored) {
                }
            }
            return null;
        }
    }

    public Integer getInt(String name, Integer defaultValue) {
        Integer value = getInt(name);
        return value != null ? value : defaultValue;
    }

    public Long getLong(String name) {
        Object o = get(name);
        if (o instanceof Number) {
            return ((Number) o).longValue();
        } else {
            if (o != null) {
                try {
                    return Long.parseLong(o.toString());
                } catch (NumberFormatException ignored) {
                }
            }
            return null;
        }
    }

    public Long getLong(String name, Long defaultValue) {
        Long value = getLong(name);
        return value != null ? value : defaultValue;
    }

    public Short getShort(String name) {
        Object o = get(name);
        if (o instanceof Number) {
            return ((Number) o).shortValue();
        } else {
            if (o != null) {
                try {
                    String string = o.toString();
                    if (string != null) {
                        return Short.parseShort(string);
                    }
                } catch (NumberFormatException ignored) {
                }
            }
            return null;
        }
    }

    public Short getShort(String name, Integer defaultValue) {
        Short value = getShort(name);
        return value != null ? value : (defaultValue != null ? defaultValue.shortValue() : null);
    }

    public Double getDouble(String name) {
        Object o = get(name);
        if (o instanceof Number) {
            return ((Number) o).doubleValue();
        } else {
            if (o != null) {
                try {
                    String string = o.toString();
                    if (string != null) {
                        return Double.parseDouble(string);
                    }
                } catch (NumberFormatException ignored) {
                }
            }
            return null;
        }
    }

    public Double getDouble(String name, Double defaultValue) {
        Double value = getDouble(name);
        return value != null ? value : defaultValue;
    }

    public Float getFloat(String name) {
        Object o = get(name);
        if (o instanceof Number) {
            return ((Number) o).floatValue();
        } else {
            if (o != null) {
                try {
                    String string = o.toString();
                    if (string != null) {
                        return Float.parseFloat(string);
                    }
                } catch (NumberFormatException ignored) {
                }
            }
            return null;
        }
    }

    public Float getFloat(String name, Float defaultValue) {
        Float value = getFloat(name);
        return value != null ? value : defaultValue;
    }

    public Boolean getBoolean(String name) {
        Object o = get(name);
        if (o instanceof Boolean) {
            return (Boolean) o;
        } else {
            if (o != null) {
                try {
                    String string = o.toString();
                    if (string != null) {
                        return toBoolean(string);
                    }
                } catch (Exception ignored) {
                }
            }
            return null;
        }
    }

    public Boolean getBoolean(String name, Boolean defaultValue) {
        return containsKey(name) ? getBoolean(name) : defaultValue;
    }

    public Date getDate(String name) {
        for (String format : GcsConstants.DATE_PATTERNS) {
            Date date = getDate(name, format);
            if (date != null) {
                return date;
            }
        }
        return null;
    }

    public Date getDate(String name, String format) {
        Object value = get(name);
        if (value instanceof Date) {
            return (Date) value;
        } else {
            if (value != null) {
                try {
                    return new SimpleDateFormat(format).parse(value.toString());
                } catch (ParseException ignored) {
                }
            }
            return null;
        }
    }

    public Date date(String name) {
        return getDate(name);
    }

    public Date date(String name, String format) {
        return getDate(name, format);
    }

    public Date date(String name, Collection<String> formats) {
        for (String format : formats) {
            Date date = getDate(name, format);
            if (date != null) {
                return date;
            }
        }
        return null;
    }

    public List<Object> getList(String name) {
        Object paramValues = get(name);
        if (paramValues == null) {
            return Collections.emptyList();
        } else if (paramValues.getClass().isArray()) {
            return Arrays.asList((Object[]) paramValues);
        } else if (paramValues instanceof Collection) {
            return new ArrayList<>((Collection<?>) paramValues);
        } else {
            return Collections.singletonList(paramValues);
        }
    }

    public List<Object> list(String name) {
        return getList(name);
    }

    public  <T> List<T> getList(String name, Class<T> elementType) {
        Object paramValues = get(name);
        if (paramValues == null) {
            return Collections.emptyList();
        } else if (paramValues.getClass().isArray()) {
            return Arrays.asList((Object[]) paramValues).stream()
                    .map(element -> convertValue(element, elementType))
                    .collect(Collectors.toList());
        } else if (paramValues instanceof Collection) {
            return new ArrayList<>((Collection<?>) paramValues).stream()
                    .map(element -> convertValue(element, elementType))
                    .collect(Collectors.toList());
        } else {
            return Collections.singletonList(paramValues).stream()
                    .map(element -> convertValue(element, elementType))
                    .collect(Collectors.toList());
        }
    }

    public <T> List<T>  list(String name, Class<T> elementType) {
        return getList(name, elementType);
    }

    public GcsMap getMap(String name) {
        Object value = get(name);
        if (value instanceof Map) {
            Map<String, Object> mapValue = (Map<String, Object>) value;
            return new GcsMap(mapValue);
        }
        return null;
    }

    public GcsMap getMap(String name, GcsMap defaultValue) {
        GcsMap result = getMap(name);
        return result != null ? result : defaultValue;
    }

    public String getString(String name) {
        Object value = get(name);
        if (value == null) {
            return null;
        } else if (value instanceof String) {
            return (String) value;
        } else if (value instanceof Date) {
            return new SimpleDateFormat(DEFAULT_DATE_FORMAT).format((Date) value);
        } else if (value.getClass().isArray()) {
            return Arrays.toString((Object[]) value);
        } else if (value instanceof Collection) {
            return value.toString();
        } else {
            return value.toString();
        }
    }

    // 新增：获取 String 值的方法（带默认值）
    public String getString(String name, String defaultValue) {
        String value = getString(name);
        return value != null ? value : defaultValue;
    }

    // 辅助方法：将字符串转换为布尔值
    private boolean toBoolean(String value) {
        value = value.trim().toLowerCase();
        return "true".equals(value) || "yes".equals(value) || "1".equals(value);
    }

    @SuppressWarnings("unchecked")
    protected <T> T convertValue(Object value, Class<T> targetType) {
        if (value == null) {
            return null;
        }

        // 如果已经是目标类型，直接返回
        if (targetType.isInstance(value)) {
            return (T) value;
        }

        // 处理 Number 类型的转换
        if (value instanceof Number && targetType == Long.class) {
            Number number = (Number) value;
            return (T) Long.valueOf(number.longValue());
        }

        // 处理字符串转换
        if (value instanceof String) {
            String strValue = (String) value;
            if (strValue.isEmpty()) {
                return null;
            }

            try {
                if (targetType == Integer.class || targetType == int.class) {
                    return (T) Integer.valueOf(strValue);
                } else if (targetType == Long.class || targetType == long.class) {
                    return (T) Long.valueOf(strValue);
                } else if (targetType == Double.class || targetType == double.class) {
                    return (T) Double.valueOf(strValue);
                } else if (targetType == Float.class || targetType == float.class) {
                    return (T) Float.valueOf(strValue);
                } else if (targetType == Boolean.class || targetType == boolean.class) {
                    return (T) Boolean.valueOf(strValue);
                } else if (targetType == Short.class || targetType == short.class) {
                    return (T) Short.valueOf(strValue);
                } else if (targetType == Byte.class || targetType == byte.class) {
                    return (T) Byte.valueOf(strValue);
                } else if (targetType == Character.class || targetType == char.class) {
                    if (strValue.length() == 1) {
                        return (T) Character.valueOf(strValue.charAt(0));
                    }
                }
            } catch (NumberFormatException e) {
                // 转换失败，返回 null 或抛出异常
                return null;
            }
        }

        // 处理日期类型
        if (targetType == Date.class) {
            if (value instanceof Date) {
                return (T) value;
            } else if (value instanceof Long) {
                return (T) new Date((Long) value);
            } else if (value instanceof String) {
                try {
                    // 使用默认日期格式
                    return (T) new SimpleDateFormat(DEFAULT_DATE_FORMAT).parse((String) value);
                } catch (ParseException e) {
                    // 尝试其他常见格式
                    try {
                        return (T) new SimpleDateFormat("yyyy-MM-dd").parse((String) value);
                    } catch (ParseException ex) {
                        return null;
                    }
                }
            }
        }

        return null;
    }

    // Map 接口实现
    @Override
    public int size() {
        return wrappedMap.size();
    }

    @Override
    public boolean isEmpty() {
        return wrappedMap.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return wrappedMap.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return wrappedMap.containsValue(value);
    }

    @Override
    public Object get(Object key) {
        return wrappedMap.get(key);
    }

    @Override
    public Object put(String key, Object value) {
        return wrappedMap.put(key, value);
    }

    @Override
    public Object remove(Object key) {
        return wrappedMap.remove(key);
    }

    @Override
    public void putAll(Map<? extends String, ?> m) {
        wrappedMap.putAll(m);
    }

    @Override
    public void clear() {
        wrappedMap.clear();
    }

    @Override
    public Set<String> keySet() {
        return wrappedMap.keySet();
    }

    @Override
    public Collection<Object> values() {
        return wrappedMap.values();
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        return wrappedMap.entrySet();
    }

    @Override
    public String toString() {
        return wrappedMap.toString();
    }

    // 克隆方法
    @Override
    public Object clone() {
        try {
            AbstractGcsMap clone = (AbstractGcsMap) super.clone();
            clone.wrappedMap = new LinkedHashMap<>(this.wrappedMap);
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(); // 不可能发生
        }
    }
}