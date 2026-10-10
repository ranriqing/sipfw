package gcs.modules.attachment.controller;

import cn.hutool.core.date.DateUtil;
import gcs.base.core.GcsConstants;
import gcs.base.core.bean.GcsMap;
import gcs.base.core.bean.GcsResult;
import gcs.modules.attachment.bean.AttachmentWrapper;
import gcs.modules.attachment.domain.GcsAttachment;
import gcs.modules.attachment.service.GcsAttachmentService;
import gcs.modules.storage.domain.GcsResource;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/gcsAttachment")
public class AttachmentController {

    @Autowired
    private GcsAttachmentService gcsAttachmentService;

    /**
     * 获取附件列表
     * @return 附件列表视图
     */
    @RequestMapping(value = "/index", method = {RequestMethod.POST})
    public GcsResult index(@RequestParam String domainId, @RequestParam String domainName) {
        String entityIdentifier = domainName + "@" + domainId;
        List<AttachmentWrapper> attachmentList = gcsAttachmentService.listAttachment(entityIdentifier);
        attachmentList.removeIf(wrapper -> wrapper.getResource() == null || wrapper.getAttachment() == null);
        List<Map<String, Object>> attachmentListMap = attachmentList.stream().map(this::_entityMap).collect(Collectors.toList());
        return GcsResult.successResult().put(GcsConstants.ENTITY_LIST, attachmentListMap);
    }

    /**
     * 上传附件
     * @param request HTTP请求
     */
    @RequestMapping(value = "/upload", method = {RequestMethod.POST})
    public GcsResult upload(@RequestParam String domainId, @RequestParam String domainName, HttpServletRequest request) {
        List<MultipartFile> filesToUpload = new ArrayList<>();
        String attachmentType = null;
        if (request instanceof MultipartHttpServletRequest) {
            MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
            attachmentType = multipartRequest.getParameter("upload_type");
            multipartRequest.getFileMap().forEach((inputName, file) -> {
                if (file instanceof List) {
                    ((List<MultipartFile>) file).forEach(filesToUpload::add);
                } else {
                    filesToUpload.add((MultipartFile) file);
                }
            });
        }

        if (filesToUpload.isEmpty()) {
            return GcsResult.notExistResult();
        } else {
            try {
                MultipartFile multipartFile = filesToUpload.get(0);
                String fileName = FilenameUtils.getName(multipartFile.getOriginalFilename());
                String entityIdentifier = domainName + "@" + domainId;

                AttachmentWrapper attachmentWrapper = gcsAttachmentService.addAttachment(
                        entityIdentifier,
                        multipartFile.getInputStream(),
                        fileName,
                        multipartFile.getContentType(),
                        attachmentType
                );
                return GcsResult.successResult().singleData(_entityMap(attachmentWrapper));
            } catch (IOException e) {
                e.printStackTrace();
                return GcsResult.failedResult(e.getMessage());
            }
        }
    }

    /**
     * 删除附件
     * @param id 附件ID
     * @return 删除结果视图
     */
    @DeleteMapping("/delete/{id}")
    public GcsResult delete(@PathVariable Long id) {
        gcsAttachmentService.delAttachment(id);
        return GcsResult.successResult().put("id", id);
    }

    // 工具方法
    private Map<String, Object> _entityMap(AttachmentWrapper attachmentWrapper) {
        GcsAttachment entity = attachmentWrapper.getAttachment();
        GcsResource gcsResource = attachmentWrapper.getResource();

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("fileName", gcsResource.getFileName());
        map.put("fileUuid", gcsResource.getFileUuid());
        map.put("contentType", gcsResource.getContentType());
        map.put("length", gcsResource.getLength());
        map.put("id", entity.getId());
        map.put("type", entity.getType());
        map.put("actorId", entity.getActorId());
        map.put("actorCaption", entity.getActorCaption());
        if (entity.getDateCreated() != null) {
            map.put("dateCreated", DateUtil.formatDateTime(entity.getDateCreated()));
        }
        if (entity.getLastUpdated() != null) {
            map.put("lastUpdated", DateUtil.formatDateTime(entity.getLastUpdated()));
        }

        return map;
    }
}