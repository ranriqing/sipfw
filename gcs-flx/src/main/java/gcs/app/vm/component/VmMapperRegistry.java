package gcs.app.vm.component;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.mapper.MapperFactoryBean;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class VmMapperRegistry implements ApplicationContextAware {

    private ApplicationContext applicationContext;
    private BeanDefinitionRegistry registry;
    private SqlSessionTemplate sqlSessionTemplate;
    private Configuration configuration;
    private final ConcurrentMap<String, Class<?>> registeredMappers = new ConcurrentHashMap<>();

    @Autowired
    public void setSqlSessionFactory(SqlSessionFactory sqlSessionFactory) {
        this.sqlSessionTemplate = new SqlSessionTemplate(sqlSessionFactory);
        this.configuration = sqlSessionFactory.getConfiguration();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
        this.registry = (BeanDefinitionRegistry) ((ConfigurableApplicationContext) applicationContext).getBeanFactory();
    }

    // 注册Mapper接口
    public synchronized void registerMapper(Class<?> mapperClass) {
        String beanName = mapperClass.getName();

        // 检查是否已注册
        if (registeredMappers.containsKey(beanName)) {
            return;
        }

        // 检查是否是BaseMapper的子类
        if (!BaseMapper.class.isAssignableFrom(mapperClass)) {
            throw new IllegalArgumentException("Mapper interface must extend BaseMapper: " + mapperClass.getName());
        }

        // 注册到MyBatis配置
        if (!configuration.hasMapper(mapperClass)) {
            if (configuration instanceof MybatisConfiguration) {
                ((MybatisConfiguration) configuration).addMapper(mapperClass);
            } else {
                configuration.addMapper(mapperClass);
            }
        }

        // 注册为Spring Bean
        if (!registry.containsBeanDefinition(beanName)) {
            BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(MapperFactoryBean.class);
            builder.addPropertyValue("mapperInterface", mapperClass);
            builder.addPropertyValue("sqlSessionFactory", applicationContext.getBean("sqlSessionFactory"));
            registry.registerBeanDefinition(beanName, builder.getBeanDefinition());
        }

        registeredMappers.put(beanName, mapperClass);
        refreshMapperProxy(mapperClass);
    }

    // 刷新Mapper代理
    private void refreshMapperProxy(Class<?> mapperClass) {
        if (configuration instanceof MybatisConfiguration) {
            ((MybatisConfiguration) configuration).getMapperRegistry().getMappers();
        }
    }

    // 获取已注册的Mapper
    public <T> T getMapper(Class<T> mapperClass) {
        return sqlSessionTemplate.getMapper(mapperClass);
    }
}