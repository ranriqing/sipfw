package gcs.modules.relation.service;

import cn.hutool.core.convert.Convert;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import gcs.base.BaseDomain;
import gcs.base.core.GcsCoreService;
import gcs.modules.relation.domain.GcsRelation;
import gcs.modules.relation.mapper.GcsRelationMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class GcsRelationService {

    @Autowired
    private GcsCoreService gcsCoreService;

    @Autowired
    private GcsRelationMapper gcsRelationMapper;

    /**
     * 关联列表
     * @param aInstance        主关联方
     * @param bReferenceClass  关联类型
     * @return 关联列表
     */
    @Transactional(readOnly = true)
    public List<GcsRelation> listRels(Object aInstance, String bReferenceClass) {
        List<Object> aIdentList = gcsCoreService.getReferenceIdents(aInstance);
        LambdaQueryWrapper<GcsRelation> queryWrapper = Wrappers.lambdaQuery(GcsRelation.class)
                .eq(GcsRelation::getAReferenceClass, aIdentList.get(0))
                .eq(GcsRelation::getAReferenceId, aIdentList.get(1));
        if(StringUtils.isNotBlank(bReferenceClass)){
            queryWrapper.eq(GcsRelation::getBReferenceClass, bReferenceClass);
        }
        return gcsRelationMapper.selectList(queryWrapper);
    }

    /**
     * 关联实体列表
     * @param aInstance        主关联方
     * @param bReferenceClass  关联类型
     * @return 关联实体列表
     */
    @Transactional(readOnly = true)
    public List<Object> listRelEntity(Object aInstance, String bReferenceClass) {
        List<Object> aIdentList = gcsCoreService.getReferenceIdents(aInstance);
        // 获取关联
        LambdaQueryWrapper<GcsRelation> queryWrapper = Wrappers.lambdaQuery(GcsRelation.class)
                .eq(GcsRelation::getAReferenceClass, aIdentList.get(0))
                .eq(GcsRelation::getAReferenceId, aIdentList.get(1));
        if(StringUtils.isNotBlank(bReferenceClass)){
            queryWrapper.eq(GcsRelation::getBReferenceClass, bReferenceClass);
        }
        List<GcsRelation> relsList = gcsRelationMapper.selectList(queryWrapper);

        // 获取关联实体
        List<Object> relEntityList = new ArrayList<>();
        for (GcsRelation gcsRelation : relsList) {
            Object reference = gcsCoreService.getReference(gcsRelation.getBReferenceClass(), gcsRelation.getBReferenceId());
            if (reference != null) {
                relEntityList.add(reference);
            }
        }
        return relEntityList;
    }

    /**
     * 关联实体
     * @param aInstance        主关联方
     * @param bReferenceClass  关联类型
     * @return 关联实体
     */
    @Transactional(readOnly = true)
    public Object getRelEntity(Object aInstance, String bReferenceClass) {
        List<Object> aIdentList = gcsCoreService.getReferenceIdents(aInstance);
        // 获取关联
        LambdaQueryWrapper<GcsRelation> queryWrapper = Wrappers.lambdaQuery(GcsRelation.class)
                .eq(GcsRelation::getAReferenceClass, aIdentList.get(0))
                .eq(GcsRelation::getAReferenceId, aIdentList.get(1));
        if(StringUtils.isNotBlank(bReferenceClass)){
            queryWrapper.eq(GcsRelation::getBReferenceClass, bReferenceClass);
        }
        List<GcsRelation> relsList = gcsRelationMapper.selectList(queryWrapper);
        // 获取关联实体
        Object reference = null;
        if (!relsList.isEmpty()) {
            GcsRelation gcsRelation = relsList.get(0);
            reference = gcsCoreService.getReference(gcsRelation.getBReferenceClass(), gcsRelation.getBReferenceId());
        }
        return reference;
    }

    /**
     * 添加或更新关联
     * @param aInstance       主关联方
     * @param bInstance       被关联方
     * @param entityIdentifier  扩展实体标识符
     * @param remark          关联备注
     * @return 关联实体
     */
    @Transactional
    public GcsRelation addRel(Object aInstance, Object bInstance, String entityIdentifier, String remark) {
        BaseDomain baseDomain = (BaseDomain)gcsCoreService.getReference(entityIdentifier);
        return addRel(aInstance, bInstance, baseDomain, remark);
    }
    @Transactional
    public GcsRelation addRel(Object aInstance, Object bInstance, BaseDomain baseDomain, String remark) {
        List<Object> aIdentList = gcsCoreService.getReferenceIdents(aInstance);
        List<Object> bIdentList = gcsCoreService.getReferenceIdents(bInstance);
        // 根据关联方获取实体
        LambdaQueryWrapper<GcsRelation> queryWrapper = Wrappers.lambdaQuery(GcsRelation.class)
                .eq(GcsRelation::getAReferenceClass, aIdentList.get(0))
                .eq(GcsRelation::getAReferenceId, aIdentList.get(1))
                .eq(GcsRelation::getBReferenceClass, bIdentList.get(0))
                .eq(GcsRelation::getBReferenceId, bIdentList.get(1));
        List<GcsRelation> relsList = gcsRelationMapper.selectList(queryWrapper);
        // 获取或新增关联实体
        GcsRelation resultRel;
        if (!relsList.isEmpty()) {
            resultRel = relsList.get(0);
        } else {
            resultRel = new GcsRelation();
            resultRel.setAReferenceClass(aIdentList.get(0).toString());
            resultRel.setAReferenceId(Convert.toLong(aIdentList.get(1)));
            resultRel.setBReferenceClass(bIdentList.get(0).toString());
            resultRel.setBReferenceId(Convert.toLong(bIdentList.get(1)));
        }
        // 处理扩展实体
        if (baseDomain != null) {
            List<Object> extendIdentList = gcsCoreService.getReferenceIdents(baseDomain);
            resultRel.setExtendReferenceClass((String) extendIdentList.get(0));
            resultRel.setExtendReferenceId((Long) extendIdentList.get(1));
        }
        resultRel.setRemark(remark);
        gcsRelationMapper.insert(resultRel);
        return resultRel;
    }

    /**
     * 更新关联
     * @param relId    关联实体ID
     * @param remark   关联备注
     */
    @Transactional
    public GcsRelation updateRel(Long relId, String remark) {
        GcsRelation gcsRelation = gcsRelationMapper.selectById(relId);
        if (gcsRelation != null) {
            gcsRelation.setRemark(remark);
            gcsRelationMapper.updateById(gcsRelation);
        }
        return gcsRelation;
    }

    /**
     * 删除关联
     * @param relId 关联Id
     */
    @Transactional
    public void delRel(Long relId) {
        gcsRelationMapper.deleteById(relId);
    }

    /**
     * 删除关联
     * @param aInstance  主关联方
     * @param bInstance  被关联方
     */
    @Transactional
    public void delRel(Object aInstance, Object bInstance) {
        List<Object> aIdentList = gcsCoreService.getReferenceIdents(aInstance);
        List<Object> bIdentList = gcsCoreService.getReferenceIdents(bInstance);
        // 根据关联方获取实体
        LambdaQueryWrapper<GcsRelation> queryWrapper = Wrappers.lambdaQuery(GcsRelation.class)
                .eq(GcsRelation::getAReferenceClass, aIdentList.get(0))
                .eq(GcsRelation::getAReferenceId, aIdentList.get(1))
                .eq(GcsRelation::getBReferenceClass, bIdentList.get(0))
                .eq(GcsRelation::getBReferenceId, bIdentList.get(1));
        List<GcsRelation> relsList = gcsRelationMapper.selectList(queryWrapper);
        for (GcsRelation rel : relsList) {
            gcsRelationMapper.deleteById(rel.getId());
        }
    }
}