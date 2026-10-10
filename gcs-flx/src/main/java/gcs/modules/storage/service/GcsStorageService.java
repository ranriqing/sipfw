package gcs.modules.storage.service;

import cn.hutool.core.convert.Convert;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import gcs.base.core.exception.GcsException;
import gcs.base.core.validators.ValidatorUtils;
import gcs.modules.storage.bean.FileStorageProvider;
import gcs.modules.storage.bean.StorageResult;
import gcs.modules.storage.domain.GcsResource;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

@Service
@Transactional
public class GcsStorageService {

    @Autowired
    private FileStorageProvider fileStorageProvider;

    /**
     * 持久化资源
     */
    public GcsResource save(InputStream contentStream, String fileName, String contentType, String uploadType) {
        if (contentStream == null || fileName == null || contentType == null) {
            throw new IllegalArgumentException("内容流、文件名和内容类型不能为空");
        }

        // 基础元信息
        fileName = FilenameUtils.normalize(fileName);
        GcsResource gcsResource = new GcsResource();
        gcsResource.setFileName(fileName);
        gcsResource.setContentType(contentType);
        gcsResource.setUploadType(uploadType);

        // 资源持久化
        StorageResult storageResult = fileStorageProvider.persist(contentStream);
        if (!storageResult.isSuccess()) {
            throw new GcsException("资源持久化保存失败");
        }

        // 完善元信息
        gcsResource.setIdentifier(storageResult.getIdentifier());
        gcsResource.setFileUuid(UUID.randomUUID().toString());
        gcsResource.setLength(storageResult.getLength());
        gcsResource.setHash(storageResult.getHash());
        gcsResource.setResourceUri(storageResult.getResourceUri());

        // 验证并保存
        validateAndSave(gcsResource);

        return gcsResource;
    }

    /**
     * 更新资源
     */
    public GcsResource update(Long resourceId, InputStream contentStream, String fileName, String contentType) {
        if (resourceId == null || contentStream == null) {
            throw new IllegalArgumentException("资源ID和内容流不能为空");
        }
        // 获取资源
        GcsResource gcsResource = findResourceById(resourceId);
        // 更新元信息
        if (fileName != null) {
            gcsResource.setFileName(FilenameUtils.normalize(fileName));
        }
        if (contentType != null) {
            gcsResource.setContentType(contentType);
        }
        // 资源持久化
        StorageResult storageResult = fileStorageProvider.update(gcsResource.getIdentifier(), contentStream);
        if (!storageResult.isSuccess()) {
            throw new GcsException("资源持久化更新失败");
        }

        // 完善元信息
        gcsResource.setLength(storageResult.getLength());
        gcsResource.setHash(storageResult.getHash());
        gcsResource.setResourceUri(storageResult.getResourceUri());

        // 保存更新
        Db.updateById(gcsResource);
        return gcsResource;
    }

    /**
     * 删除资源
     */
    public boolean delete(Long resourceId) {
        if (resourceId == null) {
            throw new IllegalArgumentException("资源ID不能为空");
        }

        // 获取资源
        GcsResource gcsResource = findResourceById(resourceId);

        // 删除资源
        StorageResult storageResult = fileStorageProvider.delete(gcsResource.getIdentifier());

        if (storageResult.isSuccess()) {
            Db.removeById(resourceId, GcsResource.class);
        }

        return storageResult.isSuccess();
    }

    /**
     * 获取资源
     */
    public GcsResource get(Object id, boolean isUuid) {
        if (id == null) {
            return null;
        }
        return isUuid
                ? Db.getOne(Wrappers.lambdaQuery(GcsResource.class).eq(GcsResource::getFileUuid, id.toString()))
                : Db.getById(Convert.toLong(id), GcsResource.class);
    }

    /**
     * 写出资源
     */
    public Long writeTo(GcsResource gcsResource, OutputStream outputStream) {
        if (gcsResource == null) {
            throw new GcsException("资源不存在，写出失败");
        }

        return fileStorageProvider.writeTo(gcsResource.getIdentifier(), outputStream);
    }

    // 辅助方法：查找资源
    private GcsResource findResourceById(Long resourceId) {
        return Db.getById(resourceId, GcsResource.class);
    }

    // 辅助方法：验证并保存资源
    private void validateAndSave(GcsResource resource) {
        // 执行验证逻辑
        ValidatorUtils.validateObj(resource);
        // 保存资源
        Db.save(resource);
    }
}
