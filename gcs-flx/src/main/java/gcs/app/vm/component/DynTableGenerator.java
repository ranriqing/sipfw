package gcs.app.vm.component;

import com.baomidou.mybatisplus.core.toolkit.ClassUtils;
import gcs.app.vm.compiler.DynamicCompiler;
import org.apache.ibatis.io.Resources;
import org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;
import org.hibernate.tool.schema.TargetType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManagerFactory;
import java.io.*;
import java.util.*;

@Component
public class DynTableGenerator {

    private static ClassLoader systemClassLoader;
    static {
        try {
            systemClassLoader = ClassLoader.getSystemClassLoader();
        } catch (SecurityException ignored) {
            // AccessControlException on Google App Engine
        }
    }

    private final EntityManagerFactory entityManagerFactory;

    @Autowired
    public DynTableGenerator(LocalContainerEntityManagerFactoryBean factoryBean) {
        this.entityManagerFactory = factoryBean.getObject();
    }

    public void updateTablesWithoutDrop(List<Class<?>> entityClasses, List<Class<?>> relationClasses) {
        StandardServiceRegistry entityManagerRegistry = getHibernateServiceRegistry();
        try {
            // 参考com.baomidou.mybatisplus.core.toolkit.ClassUtils
            ClassLoader[] classLoaders = new ClassLoader[]{
                    DynamicCompiler.getClassLoader(),
                    Resources.getDefaultClassLoader(),
                    Thread.currentThread().getContextClassLoader(),
                    ClassUtils.class.getClassLoader(),
                    systemClassLoader};

            // 自定义 BootstrapServiceRegistry 包含正确的类加载器
            BootstrapServiceRegistry bsr = new BootstrapServiceRegistryBuilder()
                    // .applyClassLoaderService(new ClassLoaderServiceImpl(Arrays.asList(classLoaders), TcclLookupPrecedence.AFTER ))
                    .applyClassLoader(DynamicCompiler.getClassLoader())
                    .build();

            // 从现有 EntityManagerFactory 复制配置
            StandardServiceRegistryBuilder ssrBuilder = new StandardServiceRegistryBuilder(bsr);
            entityManagerFactory.getProperties().forEach(ssrBuilder::applySetting);
            ssrBuilder.applySetting(AvailableSettings.HBM2DDL_AUTO, "update");
            ssrBuilder.applySetting(AvailableSettings.PHYSICAL_NAMING_STRATEGY, CamelCaseToUnderscoresNamingStrategy.class.getName());

            // 构建新的 StandardServiceRegistry，创建 MetadataSources
            StandardServiceRegistry registry = ssrBuilder.build();
            MetadataSources metadataSources = new MetadataSources(registry);

            // 手动注册每个实体类
            for (Class<?> entityClass : entityClasses) {
                metadataSources.addAnnotatedClass(entityClass);
            }
            for (Class<?> entityClass : relationClasses) {
                metadataSources.addAnnotatedClass(entityClass);
            }

            // 构建并更新表结构
            Metadata metadata = metadataSources.buildMetadata();
            SchemaUpdate schemaUpdate = new SchemaUpdate();
            schemaUpdate.execute(EnumSet.of(TargetType.DATABASE), metadata);

            // 输出数据建表语句
            if(false){
                File tempFile = File.createTempFile("schema-update", ".sql");
                tempFile.deleteOnExit();
                SchemaExport schemaExport = new SchemaExport();
                schemaExport.setFormat(true);
                schemaExport.setOutputFile(tempFile.getAbsolutePath());
                schemaExport.execute(EnumSet.of(TargetType.STDOUT), SchemaExport.Action.CREATE, metadata);
                StringBuilder content = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new FileReader(tempFile))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        content.append(line).append("\n");
                    }
                }
                System.out.println(content);
            }
            System.out.println("成功更新数据表结构:");
            for (Class<?> entityClass : entityClasses) {
                System.out.println("- " + entityClass.getName());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private StandardServiceRegistry getHibernateServiceRegistry() {
        return entityManagerFactory.unwrap(org.hibernate.SessionFactory.class)
                .getSessionFactoryOptions().getServiceRegistry();
    }
}