package gcs.base.shiro.utils;

import gcs.base.shiro.realm.GcsFormRealm;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 用于定时任务中初始化 SecurityManager 并模拟登录
 */
@Component
public class ShiroTaskContext {

    @Autowired
    private SecurityManager securityManager;

    /**
     * 以指定用户身份执行任务
     * @param username 用户名（如 admin）
     * @param task 需要执行的任务
     */
    public void runAsUser(String username, Runnable task) {
        // 保存原始线程上下文，避免污染
        SecurityManager originalSecurityManager = ThreadContext.getSecurityManager();
        Subject originalSubject = ThreadContext.getSubject();

        try {
            // 1. 绑定 SecurityManager 到当前线程
            SecurityUtils.setSecurityManager(securityManager);
            ThreadContext.bind(securityManager);
            // 2. 创建并绑定模拟用户的 Subject
            Subject subject = createSubject(username, GcsFormRealm.class.getSimpleName());
            ThreadContext.bind(subject);
            // 3. 执行任务（此时调用 SecurityUtils.getSubject() 不会报错）
            task.run();
        } finally {
            // 4. 清理线程上下文，恢复原始状态（必须执行，否则影响其他任务）
            ThreadContext.unbindSubject();
            ThreadContext.unbindSecurityManager();
            if (originalSecurityManager != null) {
                ThreadContext.bind(originalSecurityManager);
            }
            if (originalSubject != null) {
                ThreadContext.bind(originalSubject);
            }
        }
    }

    /**
     * 创建模拟登录的用户 Subject
     */
    private Subject createSubject(String username, String realmName) {
        // 构建用户身份信息（principals）
        SimplePrincipalCollection principals = new SimplePrincipalCollection();
        principals.add(username, realmName); // 关联用户名和 Realm
        // 创建已认证的 Subject
        return new Subject.Builder(securityManager)
                .principals(principals)
                .authenticated(true)
                .buildSubject();
    }
}