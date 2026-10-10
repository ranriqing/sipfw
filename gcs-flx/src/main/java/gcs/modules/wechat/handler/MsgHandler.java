package gcs.modules.wechat.handler;

import gcs.modules.wechat.builder.TextBuilder;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.common.session.WxSessionManager;
import me.chanjar.weixin.mp.api.WxMpMessageHandler;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.message.WxMpXmlMessage;
import me.chanjar.weixin.mp.bean.message.WxMpXmlOutMessage;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class MsgHandler implements WxMpMessageHandler {

    @Override
    public WxMpXmlOutMessage handle(WxMpXmlMessage wxMessage, Map<String, Object> context,
                                    WxMpService wxMpService, WxSessionManager sessionManager) throws WxErrorException {
        // TODO 将消息保存到本地

        // 转发消息给在线客服
        if (StringUtils.startsWithAny(wxMessage.getContent(), "你好", "客服")
                && wxMpService.getKefuService().kfOnlineList() != null) {
            return WxMpXmlOutMessage.TRANSFER_CUSTOMER_SERVICE()
                    .fromUser(wxMessage.getToUser())
                    .toUser(wxMessage.getFromUser())
                    .build();
        }

        try {
            return new TextBuilder().build("回复信息内容", wxMessage, wxMpService);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }
}
