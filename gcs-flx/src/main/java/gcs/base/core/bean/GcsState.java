package gcs.base.core.bean;

import lombok.Data;

/**
 * [调用状态]
 */
@Data
public class GcsState {

    // 基础状态
    public static final GcsState SUCCESS = new GcsState("0", "请求成功");
    public static final GcsState FAILED = new GcsState("101", "请求失败");
    public static final GcsState PARAM_INVALID = new GcsState("102", "参数有误");
    public static final GcsState NOT_EXIST = new GcsState("103", "实体不存在");

    // 状态信息
    private String code;
    private String message;

    public GcsState(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
