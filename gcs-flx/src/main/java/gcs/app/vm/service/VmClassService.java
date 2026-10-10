package gcs.app.vm.service;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.core.toolkit.reflect.GenericTypeUtils;
import gcs.app.ugrp.domain.GcsGroup;
import gcs.app.ugrp.domain.GcsRole;
import gcs.app.ugrp.domain.GcsUser;
import gcs.app.vm.bean.VmClass;
import gcs.app.vm.bean.VmClassField;
import gcs.app.vm.bean.VmCodePoet;
import gcs.app.vm.component.*;
import gcs.app.vm.compiler.DynamicCompiler;
import gcs.app.vm.constants.VmConstants;
import gcs.app.vm.domain.MetaDomain;
import gcs.app.vm.domain.MetaDomainField;
import gcs.app.vm.domain.MetaView;
import gcs.app.vm.mapper.MetaDomainFieldMapper;
import gcs.app.vm.mapper.MetaDomainMapper;
import gcs.app.vm.utils.VmCacheHolder;
import gcs.app.vm.utils.VmUtils;
import gcs.base.BaseDomain;
import gcs.base.core.GcsHolder;
import gcs.modules.others.domain.AddrArea;
import gcs.modules.others.domain.AddrCity;
import gcs.modules.others.domain.AddrProvince;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class VmClassService {

    private static final List<Class<?>> RELATION_CLASSES = Arrays.asList(
            GcsGroup.class, GcsUser.class, GcsRole.class,
            AddrProvince.class, AddrCity.class, AddrArea.class,
            MetaView.class
    );

    @Autowired
    private MetaDomainMapper metaDomainMapper;
    @Autowired
    private MetaDomainFieldMapper metaDomainFieldMapper;
    @Autowired
    private VmMapperRegistry vmMapperRegistry;
    @Autowired
    private DynTableGenerator dynTableGenerator;

    /**
     * 获取VmClass
     * @param domainName
     * @return
     */
    public VmClass resolveVmClass(String domainName){
        return this.resolveVmClass(domainName, false);
    }
    public VmClass resolveVmClass(String domainName, Boolean allowNull){
        VmClass vmClass = doResolveVmClass(domainName);
        if(!allowNull && vmClass == null){
            throw new RuntimeException("领域定义[" + domainName + "]不存在，请联系管理人员");
        }
        return vmClass;
    }
    public VmClass doResolveVmClass(String domainName) {
        String camelDomainName = VmUtils.toUpperCamel(domainName);

        // 读取缓存
        VmClass vmClass = null;
        Boolean isCached = VmCacheHolder.isCached(camelDomainName);
        if(isCached) { // 可能返回 null
            vmClass = VmCacheHolder.getCachedEntry(camelDomainName);
            return vmClass;
        }
        synchronized (this) {
            // 二次判断
            isCached = VmCacheHolder.isCached(camelDomainName);
            if (isCached) {
                vmClass = VmCacheHolder.getCachedEntry(camelDomainName);
                return vmClass;
            }
            // 兼容FixVmClass和动态DynVmClass
            String mapperName = StringUtils.uncapitalize(camelDomainName) + VmConstants.VM_MAPPER_SUFFIX;
            if(GcsHolder.containsBean(mapperName)){
                BaseMapper vmMapper = (BaseMapper)GcsHolder.getBean(mapperName);
                Class<? extends BaseDomain> domainClass = (Class<? extends BaseDomain>)GenericTypeUtils.resolveTypeArguments(vmMapper.getClass(), BaseMapper.class)[0];
                vmClass = VmClassRegistry.getFixVmClass(domainClass);
                vmClass.setDomainName(camelDomainName);
                vmClass.setDomainClass(domainClass);
                vmClass.setDomainMapper(vmMapper);
                return vmClass;
            }else{
                // 按DynVmClass处理
                MetaDomain metaDomain = metaDomainMapper.selectOne(
                        Wrappers.lambdaQuery(MetaDomain.class)
                                .apply("LOWER(domain_name) = LOWER({0})", camelDomainName)
                );
                if(metaDomain!=null) {
                    vmClass = VmClassRegistry.getDynVmClass(camelDomainName);
                    vmClass.setDomainName(camelDomainName);
                    List<MetaDomainField> metaFieldList = metaDomainFieldMapper.selectList(
                            Wrappers.lambdaQuery(MetaDomainField.class).eq(MetaDomainField::getMetaDomainId, metaDomain.getId())
                    );
                    metaFieldList.sort((aField, bField) -> {
                        if (aField.getSeqOrder() != null && bField.getSeqOrder() != null) {
                            return aField.getSeqOrder().compareTo(bField.getSeqOrder());
                        } else {
                            return aField.getFieldName().compareTo(bField.getFieldName());
                        }
                    });
                    List<VmClassField> vmFieldList = _convertToVmClassFieldList(vmClass, metaFieldList);
                    vmClass.setClassFieldMap(
                            vmFieldList.stream().collect(
                                    Collectors.toMap(
                                            VmClassField::getFieldName,
                                            Function.identity(),
                                            (existing, replacement) -> existing  // 遇到冲突时保留第一个值
                                    )
                            )
                    );
                    // 编译RealClass
                    try{
                        String domainRealClassName = VmUtils.toUpperCamel(metaDomain.getDomainName()) + "_" + RandomStringUtils.randomAlphabetic(8).toLowerCase();
                        String domainRealClassFullName = VmConstants.VM_DOMAIN_PACKAGE + "." + domainRealClassName;
                        String domainRealMapperClassFullName = VmConstants.VM_MAPPER_PACKAGE + "." + domainRealClassName + VmConstants.VM_MAPPER_SUFFIX;

                        // 生成代码
                        VmCodePoet vmCodePoet = VmCodePoetRegistry.getVmCodePoet(metaDomain.getDomainName());
                        String domainCode = vmCodePoet.generateDomainCode(vmClass, domainRealClassName, vmFieldList);
                        String domainMapperCode = vmCodePoet.generateDomainMapperCode(vmClass, domainRealClassName, vmFieldList);

                        // 准备编译
                        Map<String, String> realClassNameToCode = new HashMap<>();
                        realClassNameToCode.put(domainRealClassFullName, domainCode);
                        realClassNameToCode.put(domainRealMapperClassFullName, domainMapperCode);

                        // 动态编译
                        Map<String, Class<?>> compiledClasses = DynamicCompiler.compileMultiple(realClassNameToCode);

                        // 获取实体类和Mapper接口的Class对象
                        Class<? extends BaseDomain> vmDomainClass = (Class<? extends BaseDomain>)compiledClasses.get(domainRealClassFullName);
                        Class<?> vmDomainMapperClass = compiledClasses.get(domainRealMapperClassFullName);
                        System.out.println("成功编译并加载类:");
                        System.out.println("- " + vmDomainClass.getName() + " (父类: " + vmDomainClass.getSuperclass().getName() + ")");
                        System.out.println("- " + vmDomainMapperClass.getName() + " (父类: " + (vmDomainMapperClass.getSuperclass() != null ? vmDomainMapperClass.getSuperclass().getName() : "无(接口)") + ")");
                        List<Class<?>> dynEntityClasses = new ArrayList<>();
                        dynEntityClasses.add(vmDomainClass);
                        dynTableGenerator.updateTablesWithoutDrop(dynEntityClasses, RELATION_CLASSES);
                        // 注册Mapper到MyBatis-Plus
                        vmMapperRegistry.registerMapper(vmDomainMapperClass);
                        vmClass.setDomainClass(vmDomainClass);
                        vmClass.setDomainMapper((BaseMapper) vmMapperRegistry.getMapper(vmDomainMapperClass));
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
                // 缓存Mapper，可能缓存null值
                VmCacheHolder.cacheEntry(camelDomainName, vmClass);
                return vmClass;
            }
        }
    }

    // 工具方法
    private static List<VmClassField> _convertToVmClassFieldList(VmClass vmClass, List<MetaDomainField> metaFieldList) {
        List<VmClassField> vmFieldList = new ArrayList<>();
        for (MetaDomainField metaField : metaFieldList) {
            // 解析VmClassField
            VmClassField vmClassField = null;

            if (VmClassFieldRegistry.isSupport(metaField.getFieldType())) {
                try {
                    // 根据字段类型获取对应的VmClassField实现类并实例化
                    Class<? extends VmClassField> fieldClass = VmClassFieldRegistry.getFieldTypeClass(metaField.getFieldType());
                    vmClassField = fieldClass.getDeclaredConstructor().newInstance();
                    vmClassField.setVmClass(vmClass);
                    vmClassField.setId(metaField.getId());
                    vmClassField.setFieldTitle(metaField.getTitle());
                    vmClassField.setFieldName(metaField.getFieldName());
                    vmClassField.setFieldType(metaField.getFieldType());
                    vmClassField.setSeqOrder(metaField.getSeqOrder());

                    // 解析约束条件
                    if (StringUtils.isNotBlank(metaField.getConstraintJson())) {
                        JSONObject constraintJsonObject = JSONUtil.parseObj(metaField.getConstraintJson());
                        vmClassField.setConstraintsMap(constraintJsonObject);
                    }

                    // 解析扩展条件
                    if (StringUtils.isNotBlank(metaField.getExtendJson())) {
                        JSONObject extendJsonObject = JSONUtil.parseObj(metaField.getExtendJson());
                        vmClassField.setExtendMap(extendJsonObject);
                    }

                    // 初始化VmClassField
                    vmClassField.init();
                } catch (Exception e) {
                    // 处理实例化或JSON解析异常
                    e.printStackTrace();
                    vmClassField = null;
                }
            }

            if (vmClassField != null) {
                vmFieldList.add(vmClassField);
            }
        }

        // 过滤掉空值（已在循环中处理）
        return vmFieldList;
    }
}
