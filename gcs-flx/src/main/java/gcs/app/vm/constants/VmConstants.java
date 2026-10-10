package gcs.app.vm.constants;

public class VmConstants {

    /** 基础常量 */
    public static final String VM_DOMAIN_PACKAGE = "gcs.app.vm.domain";
    public static final String VM_MAPPER_PACKAGE = "gcs.app.vm.mapper";
    public static final String VM_MAPPER_SUFFIX = "Mapper";
    public static final String VM_CLASS_FIELD = "vmClassField";

    /** 字段类型 */
    // 基础类型
    public static final String FIELD_TYPE_SEQUENCE = "Sequence";
    public static final String FIELD_TYPE_STRING = "String";
    public static final String FIELD_TYPE_BOOLEAN = "Boolean";
    public static final String FIELD_TYPE_INTEGER = "Integer";
    public static final String FIELD_TYPE_LONG = "Long";
    public static final String FIELD_TYPE_BIG_DECIMAL = "BigDecimal";
    public static final String FIELD_TYPE_DATE = "Date";

    // 复合类型
    public static final String FIELD_TYPE_SELECT = "Select";
    public static final String FIELD_TYPE_SC_EL = "ScEl";
    public static final String FIELD_TYPE_USER = "User";
    public static final String FIELD_TYPE_GROUP = "Group";
    public static final String FIELD_TYPE_ADDR = "Addr";
    public static final String FIELD_TYPE_FILE = "File";
    public static final String FIELD_TYPE_PICTURE = "Picture";

    /** 关联关系 */
    public static final String FIELD_TYPE_MANY2ONE = "ManyToOne";
    public static final String FIELD_TYPE_ONE2MANY = "OneToMany";
    public static final String FIELD_TYPE_LINK_USER = "LinkUser";

    /** 是否可开启全局搜索的字段类型（仅文本类字段） */
    public static boolean isGlobalSearchableFieldType(String fieldType) {
        return FIELD_TYPE_STRING.equals(fieldType) || FIELD_TYPE_SC_EL.equals(fieldType);
    }
}
