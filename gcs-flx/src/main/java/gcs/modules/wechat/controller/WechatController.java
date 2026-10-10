package gcs.modules.wechat.controller;

import gcs.base.core.bean.GcsResult;
import gcs.modules.wechat.GcsWechatService;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.mp.bean.message.WxMpXmlMessage;
import me.chanjar.weixin.mp.bean.message.WxMpXmlOutMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/wechat")
public class WechatController {

    @Autowired
    private GcsWechatService gcsWechatService;

    /**
     * 服务器配置校验（GET）
     */
    @GetMapping(value = "/receiveMsg", produces = "text/plain;charset=UTF-8")
    public ResponseEntity<String> receiveMsgGet(@RequestParam("signature") String signature,
                                                @RequestParam("timestamp") String timestamp,
                                                @RequestParam("nonce") String nonce,
                                                @RequestParam(value = "echostr", required = false) String echostr) {
        if (!gcsWechatService.checkSignature(timestamp, nonce, signature)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("请求参数非法，伪造的请求！");
        }
        return ResponseEntity.ok(echostr);
    }

    /**
     * 接收消息推送（POST）
     */
    @PostMapping(value = "/receiveMsg", produces = "text/xml;charset=UTF-8")
    public ResponseEntity<String> receiveMsgPost(@RequestParam("signature") String signature,
                                                 @RequestParam("timestamp") String timestamp,
                                                 @RequestParam("nonce") String nonce,
                                                 @RequestParam(value = "encrypt_type", required = false) String encryptType,
                                                 @RequestParam(value = "msg_signature", required = false) String msgSignature,
                                                 @RequestBody String requestBody) {
        if (!gcsWechatService.checkSignature(timestamp, nonce, signature)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("请求参数非法，伪造的请求！");
        }

        String outMsgStr;
        if (encryptType == null) {
            // 明文传输的消息
            WxMpXmlMessage inMessage = WxMpXmlMessage.fromXml(requestBody);
            WxMpXmlOutMessage outMessage = gcsWechatService.route(inMessage);
            outMsgStr = outMessage != null ? outMessage.toXml() : "";
        } else if ("aes".equals(encryptType)) {
            // AES 加密的消息
            WxMpXmlMessage inMessage = WxMpXmlMessage.fromEncryptedXml(
                    requestBody, gcsWechatService.getWxMpConfigStorage(), timestamp, nonce, msgSignature);
            WxMpXmlOutMessage outMessage = gcsWechatService.route(inMessage);
            outMsgStr = outMessage != null ? outMessage.toEncryptedXml(gcsWechatService.getWxMpConfigStorage()) : "";
        } else {
            outMsgStr = "";
        }
        return ResponseEntity.ok(outMsgStr);
    }

    /**
     * 刷新全部粉丝
     */
    @PostMapping("/refreshAll")
    public GcsResult refreshAll() {
        gcsWechatService.refreshAll();
        return GcsResult.successResult();
    }
}
