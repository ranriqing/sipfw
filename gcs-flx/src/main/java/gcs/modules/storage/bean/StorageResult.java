package gcs.modules.storage.bean;

import lombok.Data;

@Data
public class StorageResult{

    private Boolean success;

    /**
     * 文件标识
     */
    private String identifier;

    /**
     * 文件长度
     */
    private Long length;

    /**
     * 文件hash值，用于校验文件是否被篡改
     */
    private String hash;

    /**
     * 本地存储此值为空
     * 远端存储此值为文件Uri路径
     */
    private String resourceUri;

    // 结果消息字段
    private String message;

    public Boolean isSuccess() {
        return success;
    }
}
