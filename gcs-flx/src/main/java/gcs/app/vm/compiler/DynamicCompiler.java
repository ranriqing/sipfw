package gcs.app.vm.compiler;

import lombok.Data;

import javax.tools.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

// 动态编译器
@Data
public class DynamicCompiler {
    private static final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    private static final Map<String, JavaClassObject> classObjects = new HashMap<>();
    private static volatile String cachedClasspath;
    // 父类加载器必须是运行时加载本类的 ClassLoader（Spring Boot fat jar 下为 LaunchedURLClassLoader），
    // 否则编译后的类无法解析父类/接口（如 FloDomain、BaseMapper、javax.persistence 等）。
    private static final ClassLoader _classLoader = new ClassLoader(DynamicCompiler.class.getClassLoader()) {
        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            JavaClassObject classObject = classObjects.get(name);
            if (classObject == null) {
                return super.findClass(name);
            }
            byte[] bytes = classObject.getBytes();
            return defineClass(name, bytes, 0, bytes.length);
        }
    };

    public static Map<String, JavaClassObject> getAllClassObjects() {
        return classObjects;
    }

    public static ClassLoader getClassLoader() {
        return _classLoader;
    }

    private static Iterable<String> buildCompilerOptions() {
        return Arrays.asList("-Xlint:unchecked", "-classpath", getCompileClasspath());
    }

    /**
     * 构建编译器 classpath。
     * 开发/爆炸目录下直接使用 java.class.path；Spring Boot 可执行 fat jar 下，
     * 类位于 BOOT-INF/classes、依赖位于 BOOT-INF/lib，javax.tools 编译器无法直接
     * 从嵌套 jar 中解析，需解包到临时目录后加入 classpath。
     */
    private static String getCompileClasspath() {
        if (cachedClasspath == null) {
            synchronized (DynamicCompiler.class) {
                if (cachedClasspath == null) {
                    cachedClasspath = buildCompileClasspath();
                }
            }
        }
        return cachedClasspath;
    }

    private static String buildCompileClasspath() {
        List<String> paths = new ArrayList<>();
        String javaClassPath = System.getProperty("java.class.path");
        if (javaClassPath != null && !javaClassPath.isEmpty()) {
            for (String path : javaClassPath.split(File.pathSeparator)) {
                if (path != null && !path.isEmpty()) {
                    paths.add(path);
                }
            }
        }
        try {
            URL location = DynamicCompiler.class.getProtectionDomain().getCodeSource().getLocation();
            String loc = location.toString();
            if (loc.startsWith("jar:file:") && loc.contains("!/")) {
                String jarPath = URLDecoder.decode(loc.substring("jar:file:".length(), loc.indexOf("!/")), "UTF-8");
                File extracted = extractBootInf(new File(jarPath));
                if (extracted != null) {
                    paths.add(new File(extracted, "classes").getAbsolutePath());
                    File libDir = new File(extracted, "lib");
                    File[] libs = libDir.listFiles((dir, name) -> name.endsWith(".jar"));
                    if (libs != null) {
                        for (File lib : libs) {
                            paths.add(lib.getAbsolutePath());
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            // 解包失败则回退到 java.class.path（开发环境通常已足够）
        }
        return String.join(File.pathSeparator, paths);
    }

    private static File extractBootInf(File jarFile) {
        // 解包到运行时 jar 所在目录（与 gcs-flx.jar 同级），存在则复用不再解包
        File base = new File(jarFile.getParentFile(), "gcs-flx-classpath");
        File classesDir = new File(base, "classes");
        File libDir = new File(base, "lib");
        if (classesDir.exists() && libDir.exists()) {
            return base;
        }
        if (!base.mkdirs()) {
            return null;
        }
        try (JarFile jar = new JarFile(jarFile)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (entry.isDirectory()) {
                    continue;
                }
                if (name.startsWith("BOOT-INF/classes/")) {
                    File target = new File(classesDir, name.substring("BOOT-INF/classes/".length()));
                    target.getParentFile().mkdirs();
                    try (InputStream in = jar.getInputStream(entry);
                         OutputStream out = new FileOutputStream(target)) {
                        copy(in, out);
                    }
                } else if (name.startsWith("BOOT-INF/lib/") && name.endsWith(".jar")) {
                    File target = new File(libDir, name.substring("BOOT-INF/lib/".length()));
                    target.getParentFile().mkdirs();
                    try (InputStream in = jar.getInputStream(entry);
                         OutputStream out = new FileOutputStream(target)) {
                        copy(in, out);
                    }
                }
            }
        } catch (IOException e) {
            return null;
        }
        return base;
    }

    private static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[8192];
        int n;
        while ((n = in.read(buffer)) != -1) {
            out.write(buffer, 0, n);
        }
    }

    /**
     * 一次性编译多个类
     * @param classNameToCode
     * @return
     * @throws ClassNotFoundException
     */
    public static Map<String, Class<?>> compileMultiple(Map<String, String> classNameToCode)
            throws ClassNotFoundException {
        Map<String, Class<?>> result = new HashMap<>();

        // 准备编译单元
        List<JavaFileObject> compilationUnits = new ArrayList<>();
        for (Map.Entry<String, String> entry : classNameToCode.entrySet()) {
            String className = entry.getKey();
            String code = entry.getValue();
            compilationUnits.add(new JavaSourceFromString(className, code));
        }

        // 文件管理器
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        JavaFileManager fileManager = new ForwardingJavaFileManager<JavaFileManager>(
                compiler.getStandardFileManager(diagnostics, null, null)) {
            @Override
            public JavaFileObject getJavaFileForOutput(Location location, String className,
                                                       JavaFileObject.Kind kind, FileObject sibling) {
                JavaClassObject fileObject = new JavaClassObject(className, kind);
                classObjects.put(className, fileObject);
                return fileObject;
            }
        };

        // 执行编译任务

        Iterable<String> options = buildCompilerOptions();
        JavaCompiler.CompilationTask task = compiler.getTask( null, fileManager, diagnostics, options,null, compilationUnits);
        boolean success = task.call();

        // 检查编译错误
        if (!success) {
            StringBuilder errorMsg = new StringBuilder();
            for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
                errorMsg.append("Error on line ")
                        .append(diagnostic.getLineNumber())
                        .append(": ")
                        .append(diagnostic.getMessage(null))
                        .append("\n");
            }
            throw new RuntimeException("Compilation failed:\n" + errorMsg);
        }

        // 加载所有编译的类
        for (String className : classNameToCode.keySet()) {
            Class generatedClass = getClassLoader().loadClass(className);
            /*Method[] methods = generatedClass.getMethods();
            for (Method method : methods) {
                System.out.println(method.getName());
            }*/
            result.put(className, generatedClass);
        }

        return result;
    }

    /**
     * 编译单个类文件
     * @param className
     * @param javaCode
     * @return
     * @throws ClassNotFoundException
     */
    public static Class<?> compile(String className, String javaCode) throws ClassNotFoundException {
        // 准备编译单元
        JavaFileObject javaFileObject = new JavaSourceFromString(className, javaCode);

        // 文件管理器
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        JavaFileManager fileManager = new ForwardingJavaFileManager<JavaFileManager>(
                compiler.getStandardFileManager(diagnostics, null, null)) {
            @Override
            public JavaFileObject getJavaFileForOutput(Location location, String className,
                                                       JavaFileObject.Kind kind, FileObject sibling) {
                JavaClassObject fileObject = new JavaClassObject(className, kind);
                classObjects.put(className, fileObject);
                return fileObject;
            }
        };


        // 执行编译任务
        Iterable<String> options = buildCompilerOptions();

        // 创建编译任务
        JavaCompiler.CompilationTask task = compiler.getTask( null, fileManager, diagnostics, options, null, java.util.Arrays.asList(javaFileObject) );
        boolean success = task.call();

        // 检查编译错误
        if (!success) {
            StringBuilder errorMsg = new StringBuilder();
            for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
                errorMsg.append("Error on line ")
                        .append(diagnostic.getLineNumber())
                        .append(": ")
                        .append(diagnostic.getMessage(null))
                        .append("\n");
            }
            throw new RuntimeException("Compilation failed:\n" + errorMsg);
        }

        // 返回编译后的类
        return getClassLoader().loadClass(className);
    }
}