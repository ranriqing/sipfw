package gcs.base.tools.easyexcel;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
public class GcsProgressInfo {

    public static String TYPE_IMPORT = "import";
    public static String TYPE_EXPORT = "export";

    public static String STATUS_NORMAL = "normal";
    public static String STATUS_ACTIVE = "active";
    public static String STATUS_SUCCESS = "success";
    public static String STATUS_EXCEPTION = "exception";

    private String username;
    private String identifier;
    // 类型状态
    private String type = TYPE_IMPORT;
    private String status = STATUS_NORMAL; //
    // 进度信息
    private Long cursor;
    private Long total;
    private BigDecimal percent;
    // 扩展信息
    private Map<String, Object> extendMap;
}
