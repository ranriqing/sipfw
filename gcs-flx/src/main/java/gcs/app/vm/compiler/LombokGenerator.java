package gcs.app.vm.compiler;

import com.squareup.javapoet.*;

import javax.lang.model.element.Modifier;
import java.util.List;

public class LombokGenerator {

    /**
     * 生成 Getter 方法
     */
    public static MethodSpec generateGetter(FieldSpec fieldSpec) {
        String methodName = "get" + capitalize(fieldSpec.name);
        if (fieldSpec.type == TypeName.BOOLEAN) {
            methodName = "is" + capitalize(fieldSpec.name);
        }
        return MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC)
                .returns(fieldSpec.type)
                .addStatement("return this.$L", fieldSpec.name)
                .build();
    }

    /**
     * 生成 Setter 方法
     */
    public  static MethodSpec generateSetter(FieldSpec fieldSpec) {
        return MethodSpec.methodBuilder("set" + capitalize(fieldSpec.name))
                .addModifiers(Modifier.PUBLIC)
                .returns(void.class)
                .addParameter(fieldSpec.type, fieldSpec.name)
                .addStatement("this.$L = $L", fieldSpec.name, fieldSpec.name)
                .build();
    }

    /**
     * 生成 toString 方法
     */
    public static MethodSpec generateToString(
            String className,
            List<FieldSpec> allFields,
            List<String> includeFields
    ) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("toString")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(String.class);

        CodeBlock.Builder codeBuilder = CodeBlock.builder();
        codeBuilder.add("$S", className + "{");
        boolean first = true;
        for (FieldSpec field : allFields) {
            if (includeFields.contains(field.name)) {
                if (!first) {
                    codeBuilder.add(" + $S + ", ", ");
                }else{
                    codeBuilder.add(" + ");
                }
                codeBuilder.add("$S + this.$L + $S", field.name+"='", field.name, "'");
                first = false;
            }
        }
        codeBuilder.add(" + $S", "}");
        // 使用完整的 CodeBlock 而不是访问私有字段
        builder.addStatement("return $L", codeBuilder.build());
        return builder.build();
    }

    /**
     * 生成 hashCode 方法
     */
    public static MethodSpec generateHashCode(
            List<FieldSpec> allFields,
            List<String> includeFields
    ) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("hashCode")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(int.class);

        CodeBlock.Builder fieldsCode = CodeBlock.builder();
        boolean first = true;

        for (FieldSpec field : allFields) {
            if (includeFields.contains(field.name)) {
                if (!first) fieldsCode.add(", ");
                fieldsCode.add("$L", field.name);
                first = false;
            }
        }

        builder.addStatement("return java.util.Objects.hash($L)", fieldsCode.build());
        return builder.build();
    }

    /**
     * 生成 equals 方法
     */
    public  static MethodSpec generateEquals(
            String className,
            List<FieldSpec> allFields,
            List<String> includeFields
    ) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("equals")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(boolean.class)
                .addParameter(Object.class, "o");

        builder.beginControlFlow("if (this == o)")
                .addStatement("return true")
                .endControlFlow()
                .beginControlFlow("if (o == null || getClass() != o.getClass())")
                .addStatement("return false")
                .endControlFlow()
                .addStatement("$T other = ($T) o",
                        ClassName.get("", className),
                        ClassName.get("", className));

        CodeBlock.Builder equalsCode = CodeBlock.builder();
        boolean first = true;

        for (FieldSpec field : allFields) {
            if (includeFields.contains(field.name)) {
                if (!first) equalsCode.add(" &&\n           ");

                if (field.type.isPrimitive()) {
                    equalsCode.add("this.$L == other.$L", field.name, field.name);
                } else {
                    equalsCode.add("java.util.Objects.equals(this.$L, other.$L)", field.name, field.name);
                }
                first = false;
            }
        }

        builder.addStatement("return $L", equalsCode.build());
        return builder.build();
    }

    /**
     * 首字母大写
     */
    public static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}