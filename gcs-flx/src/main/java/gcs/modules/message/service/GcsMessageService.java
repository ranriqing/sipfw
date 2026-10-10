package gcs.modules.message.service;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import gcs.app.ugrp.service.GcsShiroService;
import gcs.base.YmlConfig;
import gcs.base.core.GcsHolder;
import gcs.base.shiro.spi.SpiUser;
import gcs.modules.email.service.GcsEmailService;
import gcs.modules.message.controller.GcsMessageController;
import gcs.modules.message.domain.GcsMessage;
import gcs.modules.message.mapper.GcsMessageMapper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class GcsMessageService
{
    private static final Logger logger = LoggerFactory.getLogger(GcsMessageService.class);

    /** 是否阅读：未读/已读（GcsMessage.ifRead 为字符串） */
    private static final String IF_READ_NO = "N";
    private static final String IF_READ_YES = "Y";

    /** 消息模板引用：ref_ 名称 -> 模板内容 */
    private static final Map<String, String> MESSAGE_MAP = new LinkedHashMap<String, String>() {{
        /* 待处理通知 */
        put("ref_notify_title", "【待处理通知】{sheetName}");
        put("ref_notify_content", "用户【{nickname}】于{applyTime}发起的【{sheetName}】，" +
                "目前待处理，请相关同事及时处理。感谢支持！");
        /* 处理反馈通知 */
        put("ref_feedback_title", "【处理进展通知】{sheetName}");
        put("ref_feedback_content1", "您于{applyTime}发起的【{sheetName}】，" +
                "【{taskName}】{approveTitle}，同步最新进展供您知晓。感谢支持！");
        put("ref_feedback_content2", "用户【{nickname}】于{applyTime}发起的【{sheetName}】，" +
                "【{taskName}】{approveTitle}，同步最新进展供您知晓。感谢支持！");
        /* 抄送通知 */
        put("ref_trial_start", "{nickname}在{applyTime}已发起样机申请/延期申请");
        /* Remind通知 */
        put("ref_trial_approaching_expire30_title", "【{sheetName}】试用即将到期");
        put("ref_trial_approaching_expire30_content", "{nickname}在{applyTime}提交的样机申请还有30天将到期，请及时客户沟通返回或延期事项");
        put("ref_trial_approaching_expire15_title", "【{sheetName}】试用即将到期");
        put("ref_trial_approaching_expire15_content", "{nickname}在{applyTime}提交的样机申请还有15天将到期，请及时客户沟通返回或延期事项");
        put("ref_trial_approaching_expire7_title", "【{sheetName}】试用即将到期");
        put("ref_trial_approaching_expire7_content", "{nickname}在{applyTime}提交的样机申请还有7天将到期，请尽快与客户沟通返回或延期事项");
        put("ref_trial_already_expire5_title", "【{sheetName}】试用已逾期");
        put("ref_trial_already_expire5_content", "{nickname}在{applyTime}提交的样机申请已逾期5天");
        put("ref_trial_already_expire10_15_2R_title", "【{sheetName}】试用已逾期");
        put("ref_trial_already_expire10_15_2R_content", "{nickname}在{applyTime}提交的样机申请已逾期10天");
    }};

    @Autowired
    private GcsMessageMapper gcsMessageMapper;

    @Autowired
    private GcsShiroService gcsShiroService;

    @Autowired
    private GcsEmailService gcsEmailService;

    @Autowired
    private SimpMessageSendingOperations brokerMessagingTemplate;

    /** IService风格方法 */
    public GcsMessage getById1(Long id)
    {
        return gcsMessageMapper.selectById(id);
    }
    public List<GcsMessage> listByQuery(GcsMessage gcsMessage){
        return gcsMessageMapper.selectList(Wrappers.query(gcsMessage));
    }
    public int save1(GcsMessage gcsMessage){
        return gcsMessageMapper.insert(gcsMessage);
    }
    public int update1(GcsMessage gcsMessage){
        return gcsMessageMapper.updateById(gcsMessage);
    }
    public int removeById1(Long id) {
        return gcsMessageMapper.deleteById(id);
    }
    public int removeBatchByIds1(Long[] ids) {
        return gcsMessageMapper.deleteByIds(Arrays.asList(ids));
    }

    /**
     * 获取真正消息
     */
    public String getRealMessage(String messageOrRef) {
        if(StringUtils.isBlank(messageOrRef)){
            return null;
        }
        if(messageOrRef.startsWith("ref_")){
            return MESSAGE_MAP.get(messageOrRef);
        }
        return messageOrRef;
    }

    /**
     * 发送消息
     */
    public void sendMessage(Long senderId, JSONArray combinedUsers, String titleTemplate, String contentTemplate, boolean isDebug) {
        Date nowDate = DateUtil.date();
        YmlConfig ymlConfig = GcsHolder.getYmlConfig();
        List<String> batchToList = new ArrayList<>();

        for (Object combinedUser : combinedUsers) {
            Map<String, Object> userInfo = (Map<String, Object>) combinedUser;
            String titleStr = StrUtil.format(titleTemplate, userInfo);
            String contentStr = StrUtil.format(contentTemplate, userInfo);
            boolean canBatch = combinedUsers.size() > 1 && titleStr.equals(titleTemplate) && contentStr.equals(contentTemplate);

            GcsMessage gcsMessage = new GcsMessage();
            gcsMessage.setIfRead(IF_READ_NO);
            gcsMessage.setTitle(titleStr);
            gcsMessage.setContent(contentStr);
            gcsMessage.setSendUserId(senderId);
            gcsMessage.setSendTime(nowDate);
            gcsMessage.setReceiveUserId(Convert.toLong(userInfo.get("userId")));
            gcsMessageMapper.insert(gcsMessage);

            String userEmail = (String) userInfo.get("email");
            String userIdentifier = "tenant_" + userInfo.get("username");
            String messageJson = JSONUtil.toJsonStr(GcsMessageController._entityMap(gcsMessage));

            logger.info("");
            logger.info("发送消息...");
            logger.info("接收用户：{}", userInfo);
            logger.info("消息标题：{}", titleStr);
            logger.info("消息内容：{}", contentStr);

            if (ymlConfig.isDevelopment() || isDebug) {
                if (StringUtils.isBlank(userEmail)) {
                    userEmail = userInfo.get("nickname") + "@not_exit";
                }
                logger.info("DevOrDebug: brokerMessagingTemplate.convertAndSendToUser({}, '/message', messageJson);", userIdentifier);
                logger.info("DevOrDebug: gcsEmailService.sendSimpleMessage({}, null, titleStr, contentStr, null, null)", userEmail);
                break;
            }

            logger.info("RealDo: brokerMessagingTemplate.convertAndSendToUser({}, '/message', messageJson);", userIdentifier);
            brokerMessagingTemplate.convertAndSendToUser(userIdentifier, "/message", messageJson);
            if (StringUtils.isNotBlank(userEmail)) {
                if (!canBatch) {
                    logger.info("RealDo: gcsEmailService.sendSimpleMessage({}, null, titleStr, contentStr, null, null)", userEmail);
                    gcsEmailService.sendSimpleMessage(userEmail, null, null, titleStr, contentStr, null, null);
                } else {
                    batchToList.add(userEmail);
                }
            }
        }

        if (!batchToList.isEmpty()) {
            List<String> toList = new ArrayList<>();
            List<String> ccList = new ArrayList<>();
            List<String> bccList = new ArrayList<>();
            for (Object combinedUser : combinedUsers) {
                Map<String, Object> userInfo = (Map<String, Object>) combinedUser;
                String userEmail = (String) userInfo.get("email");
                if (StringUtils.isNotBlank(userEmail) && batchToList.contains(userEmail)) {
                    String type = ObjectUtil.defaultIfNull(
                            (String) userInfo.get("recipientType"), "to");
                    switch (type.toLowerCase()) {
                        case "cc":
                            ccList.add(userEmail);
                            break;
                        case "bcc":
                            bccList.add(userEmail);
                            break;
                        default:
                            toList.add(userEmail);
                            break;
                    }
                }
            }
            if (toList.isEmpty()) {
                toList.add(ymlConfig.getMailFromUser());
            }
            String ccStr = ccList.isEmpty() ? null : String.join(",", ccList);
            String bccStr = bccList.isEmpty() ? null : String.join(",", bccList);
            logger.info("");
            logger.info("BatchDo: gcsEmailService.sendSimpleMessage(to:{}, cc:{}, bcc:{}, title, content)",
                    toList, ccStr, bccStr);
            gcsEmailService.sendSimpleMessage(toList, ccStr, bccStr, titleTemplate, contentTemplate, null, null);
        }
    }

    /**
     * 未读数量
     */
    public long unreadCount(){
        SpiUser spiUser = gcsShiroService.currentUser();
        GcsMessage queryObject = new GcsMessage();
        queryObject.setReceiveUserId(spiUser.getId());
        queryObject.setIfRead(IF_READ_NO);
        return gcsMessageMapper.selectCount(Wrappers.query(queryObject));
    }

    /**
     * 标记已读
     */
    public int readSingle(Long id) {
        GcsMessage updateObject = new GcsMessage();
        updateObject.setId(id);
        updateObject.setIfRead(IF_READ_YES);
        return gcsMessageMapper.updateById(updateObject);
    }
    public int readAll(){
        SpiUser spiUser = gcsShiroService.currentUser();
        GcsMessage updateObject = new GcsMessage();
        updateObject.setIfRead(IF_READ_YES);
        LambdaQueryWrapper<GcsMessage> queryWrapper = Wrappers.lambdaQuery(GcsMessage.class).ne(GcsMessage::getIfRead, IF_READ_YES);
        if(!GcsHolder.getYmlConfig().isDevelopment()){
            queryWrapper.eq(GcsMessage::getReceiveUserId, spiUser.getId());
        }
        return gcsMessageMapper.update(updateObject, queryWrapper);
    }

}
