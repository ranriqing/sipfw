package gcs.modules.storage.bean;

import cn.hutool.core.io.IoUtil;
import gcs.base.YmlConfig;
import gcs.base.core.exception.GcsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

/**
 * 文件存储提供者实现，用于本地文件系统存储
 */
@Component
public class FileStorageProvider implements IStorageProvider {

    private static final Logger log = LoggerFactory.getLogger(FileStorageProvider.class);
    private static final int BUFFER_SIZE = 8192;

    @Autowired
    private YmlConfig ymlConfig = new YmlConfig();

    @Override
    public StorageResult persist(InputStream contentStream) {
        String identifier = UUID.randomUUID().toString();
        File targetFile = getFileFromIdentifier(identifier);

        try {
            createParentDirectories(targetFile);
            writeStreamToFile(contentStream, targetFile);

            return buildSuccessResult(identifier, targetFile);
        } catch (IOException e) {
            log.error("持久化文件失败: {}", identifier, e);
            deleteFileQuietly(targetFile);
            return buildErrorResult(identifier, "文件存储失败");
        } finally {
            IoUtil.close(contentStream);
        }
    }

    @Override
    public StorageResult update(String identifier, InputStream contentStream) {
        File targetFile = getFileFromIdentifier(identifier);

        try {
            createParentDirectories(targetFile);
            writeStreamToFile(contentStream, targetFile);

            return buildSuccessResult(identifier, targetFile);
        } catch (IOException e) {
            log.error("更新文件失败: {}", identifier, e);
            return buildErrorResult(identifier, "文件更新失败");
        } finally {
            IoUtil.close(contentStream);
        }
    }

    @Override
    public StorageResult delete(String identifier) {
        File targetFile = getFileFromIdentifier(identifier);
        try {
            boolean deleted = targetFile.exists() ? targetFile.delete() : true;
            return buildSuccessResult(identifier);
        } catch (SecurityException e) {
            log.error("删除文件失败: {}", identifier, e);
            return buildErrorResult(identifier, "文件删除失败");
        }
    }

    @Override
    public Long writeTo(String identifier, OutputStream outputStream) {
        File sourceFile = getFileFromIdentifier(identifier);

        if (!sourceFile.exists()) {
            throw new GcsException("持久化资源不存在:" + identifier);
        }

        try (InputStream inputStream = new FileInputStream(sourceFile)) {
            return IoUtil.copy(inputStream, outputStream);
        } catch (IOException e) {
            log.error("读取文件失败: {}", identifier, e);
            return 0L;
        }
    }

    // 辅助方法：计算文件的MD5哈希值
    private String calculateMD5(File file) {
        try (InputStream fis = new FileInputStream(file)) {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;

            while ((bytesRead = fis.read(buffer)) != -1) {
                md.update(buffer, 0, bytesRead);
            }

            byte[] digest = md.digest();
            return bytesToHex(digest);
        } catch (IOException | NoSuchAlgorithmException e) {
            log.error("计算文件MD5失败: {}", file.getName(), e);
            return null;
        }
    }

    // 辅助方法：将字节数组转换为十六进制字符串
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    // 辅助方法：从标识符获取文件
    private File getFileFromIdentifier(String identifier) {
        return new File(ymlConfig.getFileStorageRootDir(), identifier + ".dat");
    }

    // 辅助方法：创建父目录
    private void createParentDirectories(File file) throws IOException {
        File parent = file.getParentFile();
        if (!parent.exists() && !parent.mkdirs()) {
            throw new IOException("无法创建目录: " + parent.getAbsolutePath());
        }
    }

    // 辅助方法：将输入流写入文件
    private void writeStreamToFile(InputStream input, File output) throws IOException {
        try (OutputStream fos = new FileOutputStream(output)) {
            IoUtil.copy(input, fos);
        }
    }

    // 辅助方法：静默删除文件
    private void deleteFileQuietly(File file) {
        if (file != null && file.exists()) {
            try {
                file.delete();
            } catch (SecurityException e) {
                log.debug("删除文件失败: {}", file.getAbsolutePath(), e);
            }
        }
    }

    // 辅助方法：构建成功结果
    private StorageResult buildSuccessResult(String identifier) {
        StorageResult storageResult = new StorageResult();
        storageResult.setSuccess(true);
        storageResult.setIdentifier(identifier);
        return storageResult;
    }

    // 辅助方法：构建带有文件信息的成功结果
    private StorageResult buildSuccessResult(String identifier, File file) {
        StorageResult result = this.buildSuccessResult(identifier);
        result.setLength(file.length());
        result.setHash(calculateMD5(file));
        return result;
    }

    // 辅助方法：构建错误结果
    private StorageResult buildErrorResult(String identifier, String message) {
        StorageResult storageResult = new StorageResult();
        storageResult.setSuccess(false);
        storageResult.setIdentifier(identifier);
        storageResult.setMessage(message);
        return storageResult;
    }
}