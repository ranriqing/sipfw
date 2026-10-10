package gcs.modules.websocket;


import cn.hutool.json.JSON;
import cn.hutool.json.JSONUtil;
import gcs.base.core.GcsConstants;
import gcs.base.core.bean.GcsResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Websocket使用样例
 */
@RestController
@RequestMapping("/demoWebsocket")
public class DemoWebsocketController {

    @Autowired
    private SimpMessageSendingOperations brokerMessagingTemplate;

    // 临时运行线程
    private Thread showDateThread;

    /**
     * 连续60次发送时间到/topic/demo中
     */
    @RequestMapping(value = "/index", method = {RequestMethod.GET, RequestMethod.POST})
    public GcsResult index() {
        if (showDateThread == null) {
            showDateThread = new Thread(() -> {
                for (int i = 0; i < 60; i++) {
                    Map<String, String> message = new HashMap<>();
                    message.put("title", "Title");
                    message.put("currentDate", new SimpleDateFormat(GcsConstants.DATETIME_MINSEC_PATTERN).format(new Date()));

                    brokerMessagingTemplate.convertAndSend("/topic/demo", message);
                    // brokerMessagingTemplate.convertAndSend("/user/zhangsan/queue/position", message);

                    try {
                        Thread.sleep(1000L);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                showDateThread = null;
            });

            showDateThread.setDaemon(true);
            showDateThread.start();
        }
        return GcsResult.successResult();
    }

    /**
     * MessageMapping定义接收消息的节点，前面要添加applicationDestinationPrefixes
     * SendTo 简单定义发送消息到的目的路径，可以使用simpMessagingTemplate来发送
     * eg: client.send("/ws/hello", {}, JSON.stringify({name:'chenhb', age: 30}));
     * @param jsonBodyStr 接受到的Json字符串
     */
    @MessageMapping("/hello")
    @SendTo("/topic/hello")
    public GcsResult hello(String jsonBodyStr) {
        JSON jsonBody = JSONUtil.parse(jsonBodyStr);
        System.out.println(jsonBody);
        // simpMessagingTemplate.convertAndSendToUser("chen", "/hello", jsonBody.toString());
        return GcsResult.successResult().singleData(jsonBody.getByPath("name"));
    }
}