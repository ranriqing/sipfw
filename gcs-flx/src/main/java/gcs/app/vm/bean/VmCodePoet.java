package gcs.app.vm.bean;

import cn.hutool.core.util.ReflectUtil;
import com.baomidou.mybatisplus.annotation.*;
import com.google.common.base.CaseFormat;
import com.squareup.javapoet.*;
import gcs.app.vm.compiler.LombokGenerator;
import gcs.app.vm.constants.VmConstants;
import gcs.base.BaseDomain;
import gcs.base.FloDomain;
import lombok.Data;
import org.apache.ibatis.annotations.Mapper;

import javax.lang.model.element.Modifier;
import javax.persistence.*;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class VmCodePoet {

    public String getSupportedDomain(){
        return null;
    }

    /** 定义父类 */
    public Class<? extends FloDomain> getSuperClass(){
        return FloDomain.class;
    }

    /** 生成Domain代码 */
    public String generateDomainCode(VmClass vmClass, String domainRealClassName, List<VmClassField> vmClassFieldList) throws Exception{
        String tableName = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, vmClass.getDomainName());
        String sequenceName = "seq_" + tableName.replace("_", "") + "_id";

        // 生成类定义
        TypeSpec.Builder builder = this.genTypeSpec(vmClass, domainRealClassName, tableName, sequenceName).toBuilder();

        // 生成ID字段
        List<FieldSpec> fieldSpecList = new ArrayList<>();
        List<String> toStringFieldList = new ArrayList<>();
        List<String> hashCodeEqualsFieldList = new ArrayList<>();
        FieldSpec idField = this.genIdFieldSpec(vmClass, domainRealClassName, sequenceName);
        fieldSpecList.add(idField);
        toStringFieldList.add(idField.name);
        hashCodeEqualsFieldList.add(idField.name);

        // 生成Vm定义字段
        List<FieldSpec> vmFieldSpecList = this.genVmFieldSpecList(vmClass, domainRealClassName, vmClassFieldList);
        if(vmFieldSpecList != null){
            Class<? extends FloDomain> superClass = this.getSuperClass();
            vmFieldSpecList.forEach(fieldSpec -> { // 存在则不再添加
                if(!ReflectUtil.hasField(superClass, fieldSpec.name)){
                    fieldSpecList.add(fieldSpec);
                }
            });
            toStringFieldList.addAll(vmFieldSpecList.stream().map(item -> item.name).collect(Collectors.toList()));
        }

        // 生成Extra字段
        List<FieldSpec> extraFieldSpecList = this.genExtraFieldSpecList(vmClass, domainRealClassName);
        fieldSpecList.addAll(extraFieldSpecList);

        // 全部字段添加到类
        fieldSpecList.forEach(builder::addField);

        // 生成 Getter 和 Setter
        for (FieldSpec fieldSpec : fieldSpecList) {
            builder.addMethod(LombokGenerator.generateGetter(fieldSpec));
            builder.addMethod(LombokGenerator.generateSetter(fieldSpec));
        }

        // 生成 toString、hashCode 和 equals 方法
        builder.addMethod(LombokGenerator.generateToString(domainRealClassName, fieldSpecList, toStringFieldList));
        builder.addMethod(LombokGenerator.generateHashCode(fieldSpecList, hashCodeEqualsFieldList));
        builder.addMethod(LombokGenerator.generateEquals(domainRealClassName, fieldSpecList, hashCodeEqualsFieldList));

        // 生成Java文件
        JavaFile javaFile = JavaFile.builder(VmConstants.VM_DOMAIN_PACKAGE, builder.build())
                .build();

        StringWriter writer = new StringWriter();
        javaFile.writeTo(writer);
        return writer.toString();
    }

    /** 生成DomainMapper代码 */
    public String generateDomainMapperCode(VmClass vmClass, String domainRealClassName, List<VmClassField> vmFieldList) throws Exception {
        // 定义Mapper接口
        TypeSpec mapperInterface = TypeSpec.interfaceBuilder(domainRealClassName + VmConstants.VM_MAPPER_SUFFIX)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(AnnotationSpec.builder(Mapper.class).build())
                .addSuperinterface(ParameterizedTypeName.get(
                        ClassName.get("com.baomidou.mybatisplus.core.mapper", "BaseMapper"),
                        ClassName.get(VmConstants.VM_DOMAIN_PACKAGE, domainRealClassName)
                ))
                .build();

        // 生成Java文件
        JavaFile javaFile = JavaFile.builder(VmConstants.VM_MAPPER_PACKAGE, mapperInterface).build();

        StringWriter writer = new StringWriter();
        javaFile.writeTo(writer);
        return writer.toString();
    }

    /** 生成类定义 */
    public TypeSpec genTypeSpec(VmClass vmClass, String domainRealClassName, String tableName, String sequenceName){
        TypeSpec vmTypeSpec = TypeSpec.classBuilder(domainRealClassName)
                .addModifiers(Modifier.PUBLIC)
                .superclass(this.getSuperClass())
                .addSuperinterface(Serializable.class)
                .addAnnotation(Data.class)
                .addAnnotation(Entity.class)
                .addAnnotation(AnnotationSpec.builder(Table.class)
                        .addMember("name", "$S", tableName)
                        .build())
                .addAnnotation(AnnotationSpec.builder(TableName.class)
                        .addMember("value", "$S", tableName)
                        .build())
                .addAnnotation(AnnotationSpec.builder(KeySequence.class)
                        .addMember("value", "$S", sequenceName)
                        .build())
                .addField(FieldSpec.builder(long.class, "serialVersionUID", Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                        .initializer("1L")
                        .build())
                .build();
        return vmTypeSpec;
    }

    /** 生成ID字段 */
    public FieldSpec genIdFieldSpec(VmClass vmClass, String domainRealClassName, String sequenceName){
        FieldSpec idFieldSpec = FieldSpec.builder(Long.class, "id")
                .addModifiers(Modifier.PRIVATE)
                .addAnnotation(Id.class)
                .addAnnotation(TableId.class)
                .addAnnotation(AnnotationSpec.builder(GeneratedValue.class)
                        .addMember("strategy", "$T.SEQUENCE", GenerationType.class)
                        .addMember("generator", "$S", sequenceName)
                        .build())
                .addAnnotation(AnnotationSpec.builder(SequenceGenerator.class)
                        .addMember("name", "$S", sequenceName)
                        .addMember("initialValue", "$L", 1001)
                        .addMember("allocationSize", "$L", 1)
                        .build())
                .build();
        return idFieldSpec;
    }

    /** 生成Vm定义字段 */
    public List<FieldSpec> genVmFieldSpecList(VmClass vmClass, String domainRealClassName, List<VmClassField> vmClassFieldList){
        List<FieldSpec> vmFieldSpecList = new ArrayList<>();
        for (VmClassField vmClassField : vmClassFieldList) {
            List<FieldSpec> fieldSpecs = vmClassField.generateFieldSpec();
            vmFieldSpecList.addAll(fieldSpecs);
        }
        return vmFieldSpecList;
    }

    /** 生成Extra字段 */
    public List<FieldSpec> genExtraFieldSpecList(VmClass vmClass, String domainRealClassName){
        List<FieldSpec> fieldSpecList = new ArrayList<>();
        // 生成createrId字段
        FieldSpec createrIdField = FieldSpec.builder(Long.class, BaseDomain.CREATOR_ID)
                .addModifiers(Modifier.PRIVATE)
                .addAnnotation(AnnotationSpec.builder(TableField.class)
                        .addMember("fill", "$T.INSERT", FieldFill.class)
                        .build())
                .build();
        fieldSpecList.add(createrIdField);

        // 生成updaterId字段
        FieldSpec updaterIdField = FieldSpec.builder(Long.class, BaseDomain.UPDATER_ID)
                .addModifiers(Modifier.PRIVATE)
                .addAnnotation(AnnotationSpec.builder(TableField.class)
                        .addMember("fill", "$T.INSERT_UPDATE", FieldFill.class)
                        .build())
                .build();
        fieldSpecList.add(updaterIdField);

        // 生成dateCreated字段
        FieldSpec dateCreatedField = FieldSpec.builder(Date.class, BaseDomain.DATE_CREATED)
                .addModifiers(Modifier.PRIVATE)
                .addAnnotation(AnnotationSpec.builder(TableField.class)
                        .addMember("fill", "$T.INSERT", FieldFill.class)
                        .build())
                .build();
        fieldSpecList.add(dateCreatedField);

        // 生成lastUpdated字段
        FieldSpec lastUpdatedField = FieldSpec.builder(Date.class, BaseDomain.LAST_UPDATED)
                .addModifiers(Modifier.PRIVATE)
                .addAnnotation(AnnotationSpec.builder(TableField.class)
                        .addMember("fill", "$T.INSERT_UPDATE", FieldFill.class)
                        .build())
                .build();
        fieldSpecList.add(lastUpdatedField);

        return fieldSpecList;
    }
}
