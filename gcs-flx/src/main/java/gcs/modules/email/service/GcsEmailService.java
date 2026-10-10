package gcs.modules.email.service;

import cn.hutool.core.collection.ListUtil;
import gcs.base.YmlConfig;
import gcs.base.core.GcsHolder;
import gcs.modules.email.bean.MailMessageBuilder;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailMessage;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMailMessage;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;
import java.util.Properties;

@Service
public class GcsEmailService {
    private Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private YmlConfig ymlConfig;

    @Autowired
    private JavaMailSender mailSender;

    public void sendSimpleMessage(String to, String ccStr,  String bccStr, String subject, String textContent, String htmlContent, List<File> attachFileList) {
        this.sendSimpleMessage(ListUtil.toList(to), ccStr, bccStr, subject, textContent, htmlContent, attachFileList);
    }

    public void sendSimpleMessage(List<String> toList, String ccStr, String bccStr, String subject, String textContent, String htmlContent, List<File> attachFileList) {
        if(GcsHolder.getYmlConfig().isDevelopment()){
            logger.info("DevReturn: {},{}, {}", toList, subject, textContent);
            return;
        }
        // 发送邮件
        JavaMailSenderImpl javaMailSender = (JavaMailSenderImpl) mailSender;
        /*Properties mailProperties = new Properties();
        mailProperties.put("mail.smtp.auth", "true");
        mailProperties.put("mail.smtp.ssl.enable", "true");
        mailProperties.put("mail.smtp.socketFactory.port", "465");
        mailProperties.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        mailProperties.put("mail.smtp.socketFactory.fallback", "false");
        javaMailSender.setJavaMailProperties(mailProperties);*/
        try{
            MailMessageBuilder msgBuilder = new MailMessageBuilder(mailSender);
            msgBuilder.from(ymlConfig.getMailFromUser());
            msgBuilder.to(toList);
            if(StringUtils.isNotBlank(ccStr)) {
                msgBuilder.cc(ccStr.split(","));
            }
            if(StringUtils.isNotBlank(bccStr)) {
                msgBuilder.bcc(bccStr.split(","));
            }
            msgBuilder.subject(subject);
            msgBuilder.text(textContent);
            if(StringUtils.isNotBlank(htmlContent)) {
                msgBuilder.html(htmlContent);
            }
            if(attachFileList!=null && !attachFileList.isEmpty()){
                attachFileList.forEach( attachFile -> {
                    try {
                        msgBuilder.attach(getFileNamePrefixBeforeUnderscore(attachFile.getName()), "application/octet-stream", attachFile);
                    }catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
            MailMessage mailMessage = msgBuilder.build();
            if (mailMessage instanceof MimeMailMessage) {
                javaMailSender.send(((MimeMailMessage) mailMessage).getMimeMessage());
            } else if (mailMessage instanceof SimpleMailMessage) {
                javaMailSender.send((SimpleMailMessage) mailMessage);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private static String getFileNamePrefixBeforeUnderscore(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }
        // 找到最后一个下划线的索引
        int lastUnderscoreIndex = fileName.lastIndexOf("_");
        // 无下划线则返回原文件名
        if (lastUnderscoreIndex == -1) {
            return fileName;
        }
        // 需要保留文件后缀：如 abc_20250101.pdf -> abc.pdf
        int lastDotIndex = fileName.lastIndexOf(".");
        String prefix = fileName.substring(0, lastUnderscoreIndex);
        if (lastDotIndex > lastUnderscoreIndex) {
            return prefix + fileName.substring(lastDotIndex);
        }
        return prefix;
    }
}
