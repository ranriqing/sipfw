package gcs.app.ugrp.rasa;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import gcs.app.ugrp.domain.GcsPermission;
import gcs.app.ugrp.domain.GcsUser;
import gcs.app.ugrp.mapper.GcsPermissionMapper;
import gcs.app.ugrp.service.GcsUgrpService;
import gcs.app.vm.bean.VmClass;
import gcs.app.vm.bean.VmClassField;
import gcs.app.vm.component.fields.ManyToOneClassField;
import gcs.app.vm.constants.VmConstants;
import gcs.app.vm.service.VmClassService;
import gcs.app.vm.utils.VmUtils;
import gcs.base.core.GcsHolder;
import gcs.base.shiro.spi.SpiUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class GcsDftRasaQuery implements IRasaQuery<Object> {

    @Autowired
    private GcsPermissionMapper gcsPermissionMapper;

    @Autowired
    private GcsUgrpService gcsUgrpService;

    public void wrapCondition(QueryWrapper<Object> queryWrapper, SpiUser spiUser, Map<String, Object> context) {
        VmClass vmClass = (VmClass) context.get("vmClass");
        if (spiUser.isSuperAdmin() || vmClass == null) {
            return;
        }

        String domainName = vmClass.getDomainName();

        // 模糊查询权限定义: {domainName}_{fieldName} 或 {domainName}_{relDomainField}_{fieldName}
        List<GcsPermission> permissions = gcsPermissionMapper.selectList(
                Wrappers.lambdaQuery(GcsPermission.class)
                        .eq(GcsPermission::getRoleId, spiUser.getRoleId())
                        .likeRight(GcsPermission::getPermission, domainName + "_")
        );
        GcsPermission permission = CollUtil.getFirst(permissions);
        if (permission == null) {
            return;
        }

        String permValue = permission.getValue();
        if (StrUtil.isBlank(permValue)) {
            return;
        }

        // 去掉 domainName_ 前缀
        String permSuffix = permission.getPermission().substring(domainName.length() + 1);

        // 尝试匹配 ManyToOne 字段
        VmClassField manyToOneField = findManyToOneField(vmClass, permSuffix);
        if (manyToOneField != null) {
            handleManyToOneQuery(queryWrapper, spiUser, manyToOneField, permSuffix, permValue);
        } else {
            handleSimpleQuery(queryWrapper, spiUser, vmClass, domainName, permSuffix, permValue);
        }
    }

    /**
     * 2-part 权限: {domainName}_{fieldName}，直接对当前域字段过滤
     */
    private void handleSimpleQuery(QueryWrapper<Object> queryWrapper, SpiUser spiUser,
                                   VmClass vmClass, String domainName, String fieldName, String permValue) {
        VmClassField field = vmClass.getClassFieldMap().get(fieldName);
        if (field == null) {
            log.warn("权限字段[{}]在域[{}]中不存在，跳过权限过滤", fieldName, domainName);
            return;
        }
        String columnName = VmUtils.toLowerUnderscore(field.getRealFieldName());
        applyPermFilter(queryWrapper, columnName, permValue, spiUser);
    }

    /**
     * 3-part 权限: {domainName}_{relDomainField}_{fieldName}
     * 先按 fieldName 过滤 relDomain，再用 relDomainFieldId IN 过滤主查询
     */
    private void handleManyToOneQuery(QueryWrapper<Object> queryWrapper, SpiUser spiUser,
                                      VmClassField manyToOneField, String permSuffix, String permValue) {
        String relDomainFieldName = manyToOneField.getFieldName();
        String fieldNameInRel = permSuffix.substring(relDomainFieldName.length() + 1);

        // 解析 relDomain
        String relDomainName = manyToOneField.getExtendMap().getStr(ManyToOneClassField.REL_DOMAIN_NAME);
        if (StrUtil.isBlank(relDomainName)) {
            log.warn("关联字段[{}]未配置关联域名，跳过权限过滤", relDomainFieldName);
            return;
        }
        VmClassService vmClassService = GcsHolder.getBean(VmClassService.class);
        VmClass relVmClass = vmClassService.resolveVmClass(relDomainName);
        if (relVmClass == null) {
            log.warn("关联域[{}]解析失败，跳过权限过滤", relDomainName);
            return;
        }

        // 校验 fieldNameInRel 存在于 relDomain
        VmClassField relField = relVmClass.getClassFieldMap().get(fieldNameInRel);
        if (relField == null) {
            log.warn("权限字段[{}]在关联域[{}]中不存在，跳过权限过滤", fieldNameInRel, relDomainName);
            return;
        }

        // 在 relDomain 上执行过滤查询，只查 ID 列
        String relColumnName = VmUtils.toLowerUnderscore(relField.getRealFieldName());
        QueryWrapper<Object> relQueryWrapper = new QueryWrapper<>();
        relQueryWrapper.select("id");
        applyPermFilter(relQueryWrapper, relColumnName, permValue, spiUser);

        List<Object> idObjs = relVmClass.getDomainMapper().selectObjs(relQueryWrapper);
        List<Long> relIds = idObjs.stream().map(o -> (Long) o).collect(Collectors.toList());

        if (CollUtil.isEmpty(relIds)) {
            relIds.add(-1L);
        }

        // 用 relDomainFieldId 过滤主查询
        String fkColumn = VmUtils.toLowerUnderscore(manyToOneField.getRealFieldName());
        queryWrapper.in(fkColumn, relIds);
    }

    /**
     * 查找 VueClass 中匹配权限后缀前缀的 ManyToOne 字段
     */
    private VmClassField findManyToOneField(VmClass vmClass, String permSuffix) {
        for (VmClassField field : vmClass.getClassFieldMap().values()) {
            if (VmConstants.FIELD_TYPE_MANY2ONE.equals(field.getFieldType())) {
                if (permSuffix.startsWith(field.getFieldName() + "_")) {
                    return field;
                }
            }
        }
        return null;
    }

    /**
     * 按 permValue 对指定列应用过滤条件
     */
    private void applyPermFilter(QueryWrapper<Object> queryWrapper, String columnName, String permValue, SpiUser spiUser) {
        Long groupId = spiUser.getGroupId();
        List<Long> userIds;
        switch (permValue) {
            case "self":
                queryWrapper.eq(columnName, spiUser.getId());
                break;
            case "dept":
                userIds = this.getUserIdsByGroups(Collections.singleton(groupId));
                if (CollUtil.isNotEmpty(userIds)) {
                    queryWrapper.in(columnName, userIds);
                }
                break;
            case "subDept":
                Set<Long> subGroupIds = gcsUgrpService.getSubGroupIds(groupId, false);
                userIds = this.getUserIdsByGroups(subGroupIds);
                if (CollUtil.isNotEmpty(userIds)) {
                    queryWrapper.in(columnName, userIds);
                }
                break;
            case "deptAndSubDept":
                Set<Long> groupAndSubIds = gcsUgrpService.getSubGroupIds(groupId, true);
                userIds = this.getUserIdsByGroups(groupAndSubIds);
                if (CollUtil.isNotEmpty(userIds)) {
                    queryWrapper.in(columnName, userIds);
                }
                break;
            case "all":
                break;
            default:
                break;
        }
    }

    private List<Long> getUserIdsByGroups(Set<Long> groupIds) {
        if (CollUtil.isEmpty(groupIds)) {
            return Collections.emptyList();
        }
        return Db.listObjs(
                Wrappers.lambdaQuery(GcsUser.class)
                        .select(GcsUser::getId)
                        .in(GcsUser::getGcsGroupId, groupIds)
        );
    }
}
