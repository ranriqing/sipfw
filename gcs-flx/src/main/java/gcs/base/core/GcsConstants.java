package gcs.base.core;


import cn.hutool.core.collection.ListUtil;
import gcs.base.BaseDomain;
import gcs.base.FloDomain;

import java.util.List;

public class GcsConstants {

    public static final String TENANT_ID = "tenant";
    public static final String DOMAIN_NAME = "domainName";
    public static final String ATTACHMENTS = "attachments";
    public static final String NEXT_ACTIVITY = "nextActivity";
    public static final String ACTIVITIES = "activities";
    public static final String TAGS = "tags";
    public static final String[] DATE_PATTERNS = {
            "yyyy-MM-dd", "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm", "yyyy-MM",
            "yyyy/MM/dd", "yyyy/MM/dd HH:mm:ss", "yyyy/MM/dd HH:mm", "yyyy/MM",
            "yyyy.MM.dd", "yyyy.MM.dd HH:mm:ss", "yyyy.MM.dd HH:mm", "yyyy.MM"};


    /** 数量限制 */
    public static final String LIMIT_1 = "LIMIT 1";

    /** 日期时间 */
    public static final String TIME_PATTERN = "HH:mm:ss";
    public static final String DATE_MONTH_PATTERN = "yyyy-MM";
    public static final String DATE_DAY_PATTERN = "yyyy-MM-dd";
    public static final String DATETIME_MIN_PATTERN = "yyyy-MM-dd HH:mm";
    public static final String DATETIME_MINSEC_PATTERN = "yyyy-MM-dd HH:mm:ss";

    /** 分页结果 */
    public static final String PAGE_NUM = "pageNum";
    public static final String PAGE_SIZE = "pageSize";
    public static final String TOTAL_COUNT = "totalCount";
    public static final String ENTITY_LIST = "entityList";

    /** 待处理计划 */
    public static final String SCHEDULE_NOTIFY_TITLE = "【日程待处理】{title}";
    public static final String SCHEDULE_NOTIFY_CONTENT = "日程内容：{detail}，截止时间：{dueDate}，" +
            "请及时处理。"; // 链接地址：http://localhost:3001/wvapp/?vPath=TPendingPage&taskId={taskId}";

    /** 内部关键字 */
    public static final List<String> KEYWORDS = ListUtil.list(false,
            // BaseDomain
            BaseDomain.ID, BaseDomain.VERSION,
            BaseDomain.CREATOR_ID, BaseDomain.UPDATER_ID, BaseDomain.DATE_CREATED, BaseDomain.LAST_UPDATED,
            BaseDomain.SEARCH_KEYWORD, BaseDomain.ATTACHMENT_LIST, BaseDomain.VIOLATION_LIST,
            // FloDomain
            FloDomain.SHEET_IDENT, FloDomain.APPLY_DESCRIPTION,
            FloDomain.FLOW_DEF_KEY, FloDomain.FLOW_INST_ID, FloDomain.FLOW_STATUS,
            FloDomain.FLOW_SUBMIT_TIME, FloDomain.FLOW_UPDATE_TIME, FloDomain.FLOW_FINISH_TIME,
            // Gcs关键字(报错)
            "desc",
            // Java语言关键字
            "byte", "short", "int", "long", "float", "double", "boolean", "char",
            "class", "interface", "enum", "annotation", "package", "import",
            "if", "else", "switch", "case", "default", "for", "while", "do", "break", "continue", "return",
            "public", "private", "protected", "default", // default为接口默认方法修饰符
            "static", "final", "abstract", "native", "synchronized", "transient", "volatile", "strictfp",
            "extends", "implements", "super", "this", "new", "instanceof",
            "try", "catch", "finally", "throw", "throws",
            "var", "void", "goto", "const", "null", "true", "false",
            "module", "requires", "exports", "opens", "uses", "provides",
            // PostgreSQL关键字
            "integer", "varchar", "char", "text", "numeric", "decimal", "bigint", "smallint",
            "real", "double precision", "boolean", "date", "timestamp", "time", "interval",
            "json", "jsonb", "bytea", "uuid", "array",
            "create", "alter", "drop", "truncate", "rename", "comment", "table", "view",
            "index", "sequence", "schema", "database", "column", "constraint", "primary",
            "foreign", "key", "unique", "not null", "default", "check", "auto_increment",
            "select", "insert", "update", "delete", "into", "from", "where", "set", "values",
            "order by", "group by", "having", "limit", "offset", "distinct", "join",
            "inner", "left", "right", "full", "on", "as", "in", "not in", "between", "like",
            "ilike", "and", "or", "not", "is", "null", "exists", "any", "all",
            "begin", "commit", "rollback", "savepoint", "lock", "unlock", "transaction",
            "function", "procedure", "trigger", "case", "when", "then", "else", "end",
            "cast", "convert", "coalesce", "nullif", "count", "sum", "avg", "max", "min",
            "user", "current_user", "session_user", "public", "owner", "default", "true", "false",
            "yes", "no", "serial", "bigserial", "smallserial"
    );
}
