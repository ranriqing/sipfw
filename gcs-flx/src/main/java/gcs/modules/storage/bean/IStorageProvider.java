package gcs.modules.storage.bean;


import java.io.InputStream;
import java.io.OutputStream;

/**
 * 存储管理器接口
 */
public interface IStorageProvider {

    /**
     * 持久化内容
     * @param contentStream
     * @return
     */
    public StorageResult persist(InputStream contentStream);

    /**
     * 更新内容
     * @param identifier
     * @param contentStream
     * @return
     */
    public StorageResult update(String identifier, InputStream contentStream);

    /**
     * 删除内容
     * @param identifier
     * @return
     */
    public StorageResult delete(String identifier);

    /**
     * 读取(写出)内容
     * @param identifier
     * @param outputStream
     * @return 写出流长度
     */
    public Long writeTo(String identifier, OutputStream outputStream);
}
