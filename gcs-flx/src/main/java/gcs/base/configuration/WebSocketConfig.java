package gcs.base.configuration;


import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket配置类
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry messageBrokerRegistry) {
        // 启用内置消息代理，支持 /topic（广播）和 /queue（点对点） 和 /user（用户特定的消息）
        messageBrokerRegistry.enableSimpleBroker("/queue", "/topic", "/user");
        // 客户端发送消息的前缀，客户端发送到/ws/chat的消息会被路由到应用的控制器处理
        messageBrokerRegistry.setApplicationDestinationPrefixes("/ws");
        // 用户消息前缀(自动用户独立队列)
        messageBrokerRegistry.setUserDestinationPrefix("/user/");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry stompEndpointRegistry) {
        // 定义Stomp注册点
        stompEndpointRegistry.addEndpoint("/stomp")
                .setAllowedOriginPatterns("*") // 允许所有域名，但仍支持凭证
                // .setAllowedOrigins() // 清空原有设置
                .withSockJS();
    }
}