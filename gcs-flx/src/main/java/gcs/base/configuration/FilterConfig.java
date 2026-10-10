package gcs.base.configuration;

import gcs.base.YmlConfig;
import gcs.base.chain.filter.ContentCachingFilter;
import gcs.base.chain.filter.LogFilter;
import gcs.base.chain.filter.XSSFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.filter.DelegatingFilterProxy;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import java.nio.charset.StandardCharsets;

@Configuration
public class FilterConfig {

    @Autowired
    private YmlConfig ymlConfig;

    /**
     * 注册字符编码过滤器，确保所有请求和响应都使用UTF-8编码
     * 该过滤器优先级最高，保证在所有其他过滤器之前处理编码问题
     */
    @Bean
    public FilterRegistrationBean<CharacterEncodingFilter> characterEncodingFilter() {
        FilterRegistrationBean<CharacterEncodingFilter> registration = new FilterRegistrationBean<>();
        CharacterEncodingFilter characterEncodingFilter = new CharacterEncodingFilter();
        characterEncodingFilter.setEncoding(StandardCharsets.UTF_8.name());
        characterEncodingFilter.setForceEncoding(true);
        registration.setFilter(characterEncodingFilter);
        registration.addUrlPatterns("/*");
        registration.setName("characterEncodingFilter");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }

    /**
     * 注册内容缓存过滤器，用于缓存请求和响应内容
     * 允许后续处理程序多次读取请求/响应体，而不影响原始数据流
     */
    @Bean
    public FilterRegistrationBean<ContentCachingFilter> contentCachingFilterRegistration() {
        FilterRegistrationBean<ContentCachingFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new ContentCachingFilter());
        registration.addUrlPatterns("/*");
        registration.setName("contentCachingFilter");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 100);
        return registration;
    }

    /**
     * 注册Shiro安全过滤器，用于处理身份验证和授权
     * 通过DelegatingFilterProxy委托给Spring管理的ShiroFilter实例
     */
    @Bean
    public FilterRegistrationBean<?> shiroFilterRegistration() {
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new DelegatingFilterProxy("shiroFilter"));
        // 该值缺省为false，表示生命周期由SpringApplicationContext管理，设置为true则表示由ServletContainer管理
        registration.addInitParameter("targetFilterLifecycle", "true");
        registration.setEnabled(true);
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + Ordered.LOWEST_PRECEDENCE + 10);
        registration.addUrlPatterns("/*");
        return registration;
    }

    /**
     * 注册日志过滤器，用于记录HTTP请求和响应信息
     * 使用配置文件中的设置自定义日志行为
     */
    @Bean
    public FilterRegistrationBean<LogFilter> logFilterRegistration() {
        FilterRegistrationBean<LogFilter> registration = new FilterRegistrationBean<>();
        registration.setDispatcherTypes(DispatcherType.REQUEST);
        registration.setFilter(new LogFilter(ymlConfig));
        registration.addUrlPatterns("/*");
        registration.setName("logFilter");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + Ordered.LOWEST_PRECEDENCE + 20);
        return registration;
    }

    /**
     * 注册XSS防护过滤器，用于防止跨站脚本攻击
     * 对请求参数进行净化处理，移除潜在的恶意脚本
     * 对 getParameter 和 getHeader 去除特殊字符
     */
    @Bean
    public FilterRegistrationBean<XSSFilter> xssFilterRegistration() {
        FilterRegistrationBean<XSSFilter> registration = new FilterRegistrationBean<>();
        registration.setDispatcherTypes(DispatcherType.REQUEST);
        registration.setFilter(new XSSFilter());
        registration.addUrlPatterns("/*");
        registration.setName("xssFilter");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + Ordered.LOWEST_PRECEDENCE + 30);
        return registration;
    }


}
