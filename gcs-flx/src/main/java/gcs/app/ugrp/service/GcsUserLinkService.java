package gcs.app.ugrp.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import gcs.app.ugrp.domain.GcsUserLink;
import gcs.app.ugrp.mapper.GcsUserLinkMapper;
import gcs.app.ugrp.mapper.GcsUserMapper;
import gcs.base.core.GcsCoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GcsUserLinkService {

    @Autowired
    private GcsCoreService gcsCoreService;

    @Autowired
    private GcsUserLinkMapper gcsUserLinkMapper;

    /**
     * 获取实体所有关联
     * @param instance 实体对象
     * @param referenceType 关联类型
     */
    public List<GcsUserLink> listRelUserLinks(Object instance, String referenceType) {
        List<Object> identList = gcsCoreService.getReferenceIdents(instance);
        Long referenceId = (Long)identList.get(1);
        String referenceClass = (String)identList.get(0);
        return listRelUserLinks(referenceClass, referenceId, referenceType);
    }
    public List<GcsUserLink> listRelUserLinks(String referenceClass, Long referenceId, String referenceType) {
        return gcsUserLinkMapper.selectListByReference(referenceClass, referenceId, referenceType);
    }

    /**
     * 为实体添加单个关联
     * @param instance 实体对象
     * @param referenceType 关联类型
     * @param relUserId 关联用户ID
     */
    public GcsUserLink addRelUser(Object instance, String referenceType, Long relUserId) {
        List<Object> identList = gcsCoreService.getReferenceIdents(instance);
        Long referenceId = (Long)identList.get(1);
        String referenceClass = (String)identList.get(0);
        return this.addRelUser(referenceClass, referenceId, referenceType, relUserId);
    }
    public GcsUserLink addRelUser(String referenceClass, Long referenceId, String referenceType, Long relUserId) {
        // 检查关联是否已存在
        GcsUserLink gcsUserLink = gcsUserLinkMapper.selectOne(
            Wrappers.lambdaQuery(GcsUserLink.class)
                .eq(GcsUserLink::getReferenceId, referenceId)
                .eq(GcsUserLink::getReferenceClass, referenceClass)
                .eq(GcsUserLink::getReferenceType, referenceType)
                .eq(GcsUserLink::getGcsUserId, relUserId)
        );

        // 如果关联不存在则创建
        if (gcsUserLink == null) {
            gcsUserLink = new GcsUserLink();
            gcsUserLink.setReferenceId(referenceId);
            gcsUserLink.setReferenceClass(referenceClass);
            gcsUserLink.setReferenceType(referenceType);
            gcsUserLink.setGcsUserId(relUserId);
            gcsUserLinkMapper.insert(gcsUserLink);
        }

        return gcsUserLink;
    }

    /**
     * 为实体添加多个关联
     * @param instance 实体对象
     * @param referenceType 关联类型
     * @param relUserIds 关联用户IDs
     */
    public List<GcsUserLink> addRelUsers(Object instance, String referenceType, List<Long> relUserIds) {
        List<Object> identList = gcsCoreService.getReferenceIdents(instance);
        Long referenceId = (Long)identList.get(1);
        String referenceClass = (String)identList.get(0);
        return this.addRelUsers(referenceClass, referenceId, referenceType, relUserIds);
    }
    public List<GcsUserLink> addRelUsers(String referenceClass, Long referenceId, String referenceType, List<Long> relUserIds) {
        List<GcsUserLink> userLinks = new ArrayList<>();
        if (relUserIds != null) {
            for (Long relUserId : relUserIds) {
                userLinks.add(this.addRelUser(referenceClass, referenceId, referenceType, relUserId));
            }
        }
        return userLinks;
    }

    /**
     * 设置实体的关联，会移除不在列表中的旧关联
     * @param instance 实体对象
     * @param referenceType 关联类型
     * @param relUserIds 关联用户IDs
     */
    public List<GcsUserLink> setRelUsers(Object instance, String referenceType, List<Long> relUserIds) {
        List<Object> identList = gcsCoreService.getReferenceIdents(instance);
        Long referenceId = (Long)identList.get(1);
        String referenceClass = (String)identList.get(0);
        return setRelUsers(referenceClass, referenceId, referenceType, relUserIds);
    }
    public List<GcsUserLink> setRelUsers(String referenceClass, Long referenceId, String referenceType, List<Long> relUserIds) {
        if (referenceId == null) {
            throw new RuntimeException("需要先保存实体");
        }
        // 读取当前关联
        List<GcsUserLink> userLinks = gcsUserLinkMapper.selectList(
                Wrappers.lambdaQuery(GcsUserLink.class)
                        .eq(GcsUserLink::getReferenceId, referenceId)
                        .eq(GcsUserLink::getReferenceClass, referenceClass)
                        .eq(GcsUserLink::getReferenceType, referenceType)
        );
        // 更新为最新关联
        if (relUserIds != null && !relUserIds.isEmpty()) {
            // 迭代现有关联
            List<Long> toRemoveLinkIds = new ArrayList<>();
            for (GcsUserLink link : userLinks) {
                Long relUserId = link.getGcsUserId();
                if (relUserIds.contains(relUserId)) {
                    // 如果关联仍在新列表中，从待添加列表中移除
                    relUserIds.remove(relUserId);
                } else {
                    // 如果关联不在新列表中，删除关联
                    toRemoveLinkIds.add(link.getId());
                }
            }
            // 添加剩余的新关联
            gcsUserLinkMapper.deleteByIds(toRemoveLinkIds);
            addRelUsers(referenceClass, referenceId, referenceType, relUserIds);
        } else {
            // 如果没有提供新关联，删除所有现有关联
            gcsUserLinkMapper.deleteByIds(userLinks.stream().map(GcsUserLink::getId).collect(Collectors.toList()));
        }
        return userLinks;
    }


    /**
     * 从实体移除指定关联
     * @param instance 实体对象
     * @param referenceType 关联类型
     * @param relUserId 关联用户ID
     * @return 实体对象
     */
    public Boolean removeRelUser(Object instance, String referenceType, Long relUserId) {
        List<Object> identList = gcsCoreService.getReferenceIdents(instance);
        Long referenceId = (Long)identList.get(1);
        String referenceClass = (String)identList.get(0);
        return this.removeRelUser(referenceClass, referenceId, referenceType, relUserId);
    }
    public Boolean removeRelUser(String referenceClass, Long referenceId, String referenceType, Long relUserId) {
        // 删除指定的关联
        List<GcsUserLink> userLinks = gcsUserLinkMapper.selectList(
                Wrappers.lambdaQuery(GcsUserLink.class)
                        .eq(GcsUserLink::getReferenceId, referenceId)
                        .eq(GcsUserLink::getReferenceClass, referenceClass)
                        .eq(GcsUserLink::getReferenceType, referenceType)
                        .eq(GcsUserLink::getGcsUserId, relUserId)
        );
        if(!userLinks.isEmpty()){
            return SqlHelper.retBool(gcsUserLinkMapper.deleteByIds(userLinks.stream().map(GcsUserLink::getId).collect(Collectors.toList())));
        }
        return true;
    }
}
