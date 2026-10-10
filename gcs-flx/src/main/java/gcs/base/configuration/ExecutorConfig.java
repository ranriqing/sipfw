package gcs.base.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync // 启用异步方法支持
public class ExecutorConfig {

    /**
     * 配置线程池
     */
    @Bean(name = "gcsExecutor") // 自定义线程池名称，@Async可指定使用
    public Executor gcsExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);                // 核心线程数（默认线程数）
        executor.setMaxPoolSize(10);                // 最大线程数
        executor.setQueueCapacity(20);              // 队列容量（超过核心线程数后，任务会先进入队列）
        executor.setKeepAliveSeconds(60);           // 线程空闲时间（超过核心线程数的线程，空闲多久后销毁）
        executor.setThreadNamePrefix("GcsAsync-");  // 线程名称前缀（便于日志排查）
        // 拒绝策略（当队列满且线程数达到最大值时如何处理新任务），此处为：由提交任务的线程（调用者）直接执行
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();                      // 初始化线程池
        return executor;
    }
}