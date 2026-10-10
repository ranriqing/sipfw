package gcs.modules.attachment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import gcs.app.ugrp.service.GcsShiroService;
import gcs.base.BaseDomain;
import gcs.base.core.GcsCoreService;
import gcs.base.shiro.spi.SpiUser;
import gcs.modules.attachment.bean.AttachmentWrapper;
import gcs.modules.attachment.domain.GcsAttachment;
import gcs.modules.attachment.mapper.GcsAttachmentMapper;
import gcs.modules.relation.service.GcsRelationService;
import gcs.modules.storage.domain.GcsResource;
import gcs.modules.storage.service.GcsStorageService;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.util.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class GcsAttachmentService {

    @Autowired
    private GcsCoreService gcsCoreService;

    @Autowired
    private GcsShiroService shiroService;

    @Autowired
    private GcsStorageService gcsStorageService;

    @Autowired
    private GcsRelationService gcsRelationService;

    @Autowired
    private GcsAttachmentMapper gcsAttachmentMapper;

    /**
     * 附件列表
     * @param entityIdentifier 实体
     * @return 附件包装器列表
     */
    @Transactional(readOnly = true)
    public List<AttachmentWrapper> listAttachment(String entityIdentifier) {
        List<Object> identList = gcsCoreService.getReferenceIdents(gcsCoreService.getReference(entityIdentifier));
        // 获取全部附件
        LambdaQueryWrapper<GcsAttachment> queryWrapper = Wrappers.lambdaQuery(GcsAttachment.class)
                .eq(GcsAttachment::getReferenceClass, identList.get(0))
                .eq(GcsAttachment::getReferenceId, identList.get(1))
                .orderByAsc(GcsAttachment::getId);
        List<GcsAttachment> attachmentsList = gcsAttachmentMapper.selectList(queryWrapper);
        // 封装全部附件
        List<AttachmentWrapper> wrapperList = new ArrayList<>();
        for (GcsAttachment attachment : attachmentsList) {
            // 获取资源 & 封装资源
            GcsResource gcsResource = (GcsResource) gcsRelationService.getRelEntity(attachment, "gcsResource");
            AttachmentWrapper attachmentWrapper = new AttachmentWrapper();
            attachmentWrapper.setAttachment(attachment);
            attachmentWrapper.setResource(gcsResource);
            wrapperList.add(attachmentWrapper);
        }
        return wrapperList;
    }

    /**
     * 添加附件
     * @param entityIdentifier 附件实体(domainClass@id)
     * @param contentStream    内容流
     * @param fileName         文件名
     * @param contentType      文件类型
     * @param attachmentType   附件类型
     * @return 附件包装器
     */
    @Transactional
    public AttachmentWrapper addAttachment(String entityIdentifier, InputStream contentStream, String fileName, String contentType, String attachmentType) {
        // 保存资源实体
        GcsResource gcsResource = gcsStorageService.save(contentStream, fileName, contentType, attachmentType);
        return addAttachment(entityIdentifier, gcsResource, attachmentType);
    }

    @Transactional
    public AttachmentWrapper addAttachment(String entityIdentifier, GcsResource gcsResource, String attachmentType) {
        BaseDomain baseDomain = (BaseDomain)gcsCoreService.getReference(entityIdentifier);
        return this.addAttachment(baseDomain, gcsResource, attachmentType);
    }

    @Transactional
    public AttachmentWrapper addAttachment(BaseDomain baseDomain, GcsResource gcsResource, String attachmentType) {
        List<Object> identList = gcsCoreService.getReferenceIdents(baseDomain);
        // 获取操作人(默认未知)
        SpiUser actorUser = this.getActor();
        // 保存附件实体
        GcsAttachment gcsAttachment = new GcsAttachment();
        gcsAttachment.setActorId(actorUser.getId());
        gcsAttachment.setActorCaption(actorUser.getDisplayName());
        gcsAttachment.setGroupId(actorUser.getGroupId());
        gcsAttachment.setGroupCaption(actorUser.getGroupId().toString());
        Date nowDate = new Date();
        gcsAttachment.setDateCreated(nowDate);
        gcsAttachment.setLastUpdated(nowDate);
        gcsAttachment.setReferenceClass((String) identList.get(0));
        gcsAttachment.setReferenceId((Long) identList.get(1));
        gcsAttachment.setType(attachmentType);
        gcsAttachmentMapper.insert(gcsAttachment);
        // 关联附件和资源
        gcsRelationService.addRel(gcsAttachment, gcsResource, baseDomain, null);
        // 返回结果
        AttachmentWrapper attachmentWrapper = new AttachmentWrapper();
        attachmentWrapper.setAttachment(gcsAttachment);
        attachmentWrapper.setResource(gcsResource);
        return attachmentWrapper;
    }

    /**
     * 删除附件
     * @param attachmentId 附件ID
     */
    @Transactional
    public void delAttachment(Long attachmentId) {
        // 获取附件
        GcsAttachment attachment = gcsAttachmentMapper.selectById(attachmentId);;
        // 执行删除操作
        if (attachment != null) {
            // 删除资源
            GcsResource gcsResource = (GcsResource) gcsRelationService.getRelEntity(attachment, "gcsResource");
            if (gcsResource != null) {
                gcsRelationService.delRel(attachment, gcsResource);
                // 同一个资源如果被多个实体引用，不应该删除
                // gcsStorageService.delete(gcsResource.id)
            }
            // 删除附件
            gcsAttachmentMapper.deleteById(attachmentId);
        }
    }

    // 获取操作人
    private SpiUser getActor() {
        SpiUser spiUser = new SpiUser();
        try {
            if(ThreadContext.getSubject()!=null && SecurityUtils.getSubject().getPrincipal()!=null){
                spiUser = shiroService.currentUser();
            }
        } catch (Exception ex) {
            System.out.println("The gcs.modules.attachment.service.GcsAttachmentService threw this exception: " + ex.getMessage());
        }
        return spiUser;
    }
}