package gcs.base.configuration;

import gcs.base.shiro.ThrowableAuthenticationStrategy;
import gcs.base.shiro.OAuth2Filter;
import gcs.base.shiro.realm.GcsFormRealm;
import gcs.base.shiro.realm.GcsJwtAuthRealm;
import gcs.base.shiro.realm.GcsPermissionRealm;
import gcs.base.shiro.realm.WechatOauth2Realm;
import gcs.base.shiro.realm.WechatJwtAuthRealm;
import org.apache.shiro.authc.credential.HashedCredentialsMatcher;
import org.apache.shiro.authc.pam.ModularRealmAuthenticator;
import org.apache.shiro.mgt.DefaultSessionStorageEvaluator;
import org.apache.shiro.mgt.DefaultSubjectDAO;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.spring.LifecycleBeanPostProcessor;
import org.apache.shiro.spring.security.interceptor.AuthorizationAttributeSourceAdvisor;
import org.apache.shiro.spring.web.ShiroFilterFactoryBean;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.subject.SubjectContext;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.apache.shiro.web.mgt.DefaultWebSubjectFactory;
import org.apache.shiro.web.session.mgt.DefaultWebSessionManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.servlet.Filter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
public class ShiroConfig {

    /** 配置Shiro的凭证匹配器，用于密码加密和验证 */
    @Bean("credentialMatcher")
    HashedCredentialsMatcher credentialMatcher(){
        HashedCredentialsMatcher hashedCredentialsMatcher = new HashedCredentialsMatcher("SHA-512");
        hashedCredentialsMatcher.setStoredCredentialsHexEncoded(true);
        return hashedCredentialsMatcher;
    }

    /** 整个安全体系的协调者，通过Subject对外提供服务 */
    @Bean("securityManager")
    public SecurityManager securityManager(
        GcsPermissionRealm gcsPermissionRealm, 
        GcsFormRealm gcsFormRealm, GcsJwtAuthRealm gcsJwtAuthRealm, 
        WechatOauth2Realm wechatOauth2Realm, WechatJwtAuthRealm wechatJwtAuthRealm
    ) {
        DefaultWebSecurityManager securityManager = new DefaultWebSecurityManager();
        securityManager.setRealms(
            Arrays.asList(
                gcsPermissionRealm, 
                gcsFormRealm, gcsJwtAuthRealm, 
                wechatOauth2Realm, wechatJwtAuthRealm, 
                gcsPermissionRealm
            )
        );
        securityManager.setRememberMeManager(null);
        // 配置无状态Shiro，注释此区块即变为有状态
        DefaultWebSessionManager webSessionManager = new DefaultWebSessionManager();
        webSessionManager.setSessionValidationSchedulerEnabled(false);
        securityManager.setSessionManager(webSessionManager);
        DefaultWebSubjectFactory statelessSubjectFactory = new DefaultWebSubjectFactory(){
            @Override
            public Subject createSubject(SubjectContext context) {
                context.setSessionCreationEnabled(false); //不创建session
                return super.createSubject(context);
            }
        };
        securityManager.setSubjectFactory(statelessSubjectFactory);
        DefaultSessionStorageEvaluator evaluator = new DefaultSessionStorageEvaluator();
        evaluator.setSessionStorageEnabled(false);
        DefaultSubjectDAO defaultSubjectDAO = new DefaultSubjectDAO();
        defaultSubjectDAO.setSessionStorageEvaluator(evaluator);
        securityManager.setSubjectDAO(defaultSubjectDAO);
        // 使用自定义认证策略(支持抛出Realm异常)
        ModularRealmAuthenticator authenticator = new ModularRealmAuthenticator();
        authenticator.setRealms(securityManager.getRealms());
        authenticator.setAuthenticationStrategy(new ThrowableAuthenticationStrategy());
        securityManager.setAuthenticator(authenticator);
        return securityManager;
    }

    /** Shiro的过滤器 */
    @Bean("shiroFilter")
    public ShiroFilterFactoryBean shiroFilter(SecurityManager securityManager) {
        ShiroFilterFactoryBean shiroFilter = new ShiroFilterFactoryBean();
        shiroFilter.setSecurityManager(securityManager);

        // OAuth过滤
        Map<String, Filter> filters = new HashMap<>();
        filters.put("oauth2", new OAuth2Filter());
        shiroFilter.setFilters(filters);
        Map<String, String> filterMap = new LinkedHashMap<>();

        // 基础访问
        filterMap.put("/", "anon");
        filterMap.put("/application/**", "anon");
        filterMap.put("/globalConfig/index", "anon");
        filterMap.put("/globalConfig/show", "anon");

        // 用户认证
        // 仅放行登录/验证码/刷新Token，其它auth接口需要认证
        filterMap.put("/auth/captcha.jpg", "anon");
        filterMap.put("/auth/login", "anon");
        filterMap.put("/auth/refreshToken", "anon");
        filterMap.put("/auth/clogin", "anon");
        filterMap.put("/auth/switchToken", "oauth2");
        filterMap.put("/auth/logout", "oauth2");

        // 图片加载
        filterMap.put("/captcha.jpg", "anon");
        filterMap.put("/jcaptcha/**", "anon");
        filterMap.put("/avatar/**", "anon");

        // Wechat推送
        filterMap.put("/wechat/**", "anon");

        // 文件下载
        filterMap.put("/gcsStorage/**", "anon");

        // 内容分享落地页（匿名外部预览 / 登录内部跳转）
        filterMap.put("/share/**", "anon");

        // 问卷分享落地页（匿名填报）
        filterMap.put("/fmIndex/**", "anon");

        // Websocket
        filterMap.put("/stomp/**", "anon");
        filterMap.put("/websocket/**", "anon");
        filterMap.put("/demoWebsocket/**", "anon");

        // 呼叫中心：录音静态播放/下载（前端 <audio> 与 window.open 直链，不带 token）
        filterMap.put("/recordings/**", "anon");

        // 需要认证
        filterMap.put("/**", "oauth2");
        shiroFilter.setFilterChainDefinitionMap(filterMap);
        return shiroFilter;
    }

    /** 管理 Shiro Bean 的生命周期，自动调用实现了Lifecycle接口的Bean的start()和stop()方法 */
    @Bean("lifecycleBeanPostProcessor")
    public LifecycleBeanPostProcessor lifecycleBeanPostProcessor() {
        return new LifecycleBeanPostProcessor();
    }

    /** 支持基于注解的权限控制，如：@RequiresPermissions、@RequiresRoles */
    @Bean
    public AuthorizationAttributeSourceAdvisor authorizationAttributeSourceAdvisor(SecurityManager securityManager) {
        AuthorizationAttributeSourceAdvisor advisor = new AuthorizationAttributeSourceAdvisor();
        advisor.setSecurityManager(securityManager);
        return advisor;
    }

}
