package gcs.modules.email.bean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamSource;
import org.springframework.mail.MailMessage;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMailMessage;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.util.Assert;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Mail消息生成器
 */
public class MailMessageBuilder {

    private static final Logger log = LoggerFactory.getLogger(MailMessageBuilder.class);

    // 构造器参数
    private final MailSender mailSender;            // 邮件发送类

    // 缓存内容
    private MimeMessageHelper helper;       // 由getMessage()生成
    private MailMessage message;            // 由getMessage()结果缓存

    // 邮件元信息
    private Integer multipart = MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED;

    // 邮件内容(支持Html和Text两种格式)
    private String textContent;
    private String htmlContent;

    // 附件文件
    private List<Inline> inlines = new ArrayList<>();      // Inline资源

    public MailMessageBuilder(MailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * 生成邮件消息
     * @return MailMessage
     */
    public MailMessage build() throws MessagingException {
        // 1 设置邮件内容
        MailMessage mailMessage = getMessage();
        if (htmlContent != null) {
            if (textContent != null) {
                helper.setText(textContent, htmlContent); // 为邮件客户端提供两种格式支持
            } else {
                helper.setText(htmlContent, true);
            }
        } else {
            if (textContent == null) {
                throw new RuntimeException("message has no content, use text(), html() or body() methods to set content");
            }
            mailMessage.setText(textContent);
        }
        // 2 设置邮件附件
        for (Inline inline : inlines) {
            doAddInline(inline.id, inline.contentType, inline.toAdd, false);
        }
        // 3 发送日期
        mailMessage.setSentDate(new Date());
        return message;
    }

    /**
     * 添加附件或Inline
     * @param id
     * @param contentType
     * @param toAdd
     * @param isAttachment
     * @throws MessagingException
     */
    private void doAddInline(String id, String contentType, InputStreamSource toAdd, boolean isAttachment) throws MessagingException {
        if (!isMimeCapable()) {
            throw new RuntimeException("Not support MimeMessage, cannot attach bytes!");
        }
        this.getMessage(); // 会初始化helper
        try {
            if (isAttachment) {
                helper.addAttachment(id, toAdd, contentType);
            } else {
                helper.addInline(id, toAdd, contentType);
            }
        } catch (MessagingException e) {
            log.error("Error adding inline/attachment", e);
            throw e;
        }
    }

    /**
     * 返回 或 初始化消息
     * @return MailMessage
     */
    private MailMessage getMessage() {
        if (this.message == null) {
            if (isMimeCapable()) {
                // 初始化 helper
                try {
                    MimeMessage mimeMessage = ((JavaMailSender) mailSender).createMimeMessage();
                    helper = new MimeMessageHelper(mimeMessage, multipart);
                    this.message = new MimeMailMessage(helper);
                } catch (MessagingException e) {
                    log.error("Error creating MimeMessage", e);
                    throw new RuntimeException("Error creating MimeMessage", e);
                }
            } else {
                this.message = new SimpleMailMessage();
            }
        }
        return this.message;
    }

    /**
     * 为 MailMessage 设置头信息
     * @param hdrs
     */
    public void headers(Map<String, String> hdrs) {
        Assert.notEmpty(hdrs, "headers cannot be null");

        // MimeMailMessage可以设置headers
        if (!isMimeCapable()) {
            throw new RuntimeException("You must use a JavaMailSender to customise the headers.");
        }

        // 设置头信息
        MailMessage msg = getMessage();
        if (msg instanceof MimeMailMessage) {
            // MimeMailMessage设置头信息
            MimeMessage mimeMessage = ((MimeMailMessage) msg).getMimeMessageHelper().getMimeMessage();
            try {
                for (Map.Entry<String, String> entry : hdrs.entrySet()) {
                    String nameString = entry.getKey();
                    String valueString = entry.getValue();
                    // 判断参数
                    Assert.hasText(nameString, "header names cannot be null or empty");
                    Assert.hasText(valueString, "header value for '" + nameString + "' cannot be null");
                    // 设置头信息
                    mimeMessage.setHeader(nameString, valueString);
                }
            } catch (MessagingException e) {
                log.error("Error setting headers", e);
                throw new RuntimeException("Error setting headers", e);
            }
        } else {
            throw new RuntimeException("mail message mime capable so headers cannot be set");
        }
    }

    /**
     * 设置邮件Multipart模式
     * @param multipartMode
     */
    public void multipart(int multipartMode) {
        this.multipart = multipartMode;
    }

    public void multipart(boolean multipart) {
        this.multipart = MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED;
    }

    /**
     * 设置发送人
     * @param from
     */
    public void from(String from) {
        Assert.hasText(from, "from cannot be null or 0 length");
        getMessage().setFrom(from);
    }

    /**
     * 设置收件人
     * @param args
     */
    public void to(List<String> args) {
        to(args.toArray(new String[0]));
    }

    public void to(String... args) {
        Assert.notEmpty(args, "to cannot be null or empty");
        Assert.noNullElements(args, "to cannot contain null elements");
        this.getMessage().setTo(toDestinationAddresses(args));
    }

    /**
     * 抄送 Carbon Copy
     * @param args
     */
    public void cc(List<String> args) {
        cc(args.toArray(new String[0]));
    }

    public void cc(String... args) {
        Assert.notEmpty(args, "cc cannot be null or empty");
        Assert.noNullElements(args, "cc cannot contain null elements");
        getMessage().setCc(toDestinationAddresses(args));
    }

    /**
     * 隐藏抄送 Blind Carbon Copy
     * @param args
     */
    public void bcc(List<String> args) {
        bcc(args.toArray(new String[0]));
    }

    public void bcc(String... args) {
        Assert.notEmpty(args, "bcc cannot be null or empty");
        Assert.noNullElements(args, "bcc cannot contain null elements");
        getMessage().setBcc(toDestinationAddresses(args));
    }

    /**
     * 回复地址(Reply-TO)，可能某些客户端不支持
     * @param replyTo
     */
    public void replyTo(String replyTo) {
        Assert.hasText(replyTo, "replyTo cannot be null or 0 length");
        getMessage().setReplyTo(replyTo);
    }

    /**
     * 邮件主题
     * @param subject
     */
    public void subject(String subject) {
        Assert.notNull(subject, "subject cannot be null");
        getMessage().setSubject(subject);
    }

    /**
     * 邮件内容(text)
     * @param text
     */
    public void text(CharSequence text) {
        Assert.notNull(text, "text cannot be null");
        textContent = text.toString();
    }

    /**
     * 邮件内容(html)
     * @param html
     */
    public void html(CharSequence html) {
        if (this.isMimeCapable()) {
            htmlContent = html.toString();
        } else {
            throw new RuntimeException("Not support MimeMessage");
        }
    }

    /**
     * 添加Attach文件或资源
     * @param file
     */
    public void attach(File file) throws MessagingException, FileNotFoundException {
        attach(file.getName(), file);
    }

    public void attach(String fileName, File file) throws MessagingException, FileNotFoundException {
        if (!this.isMimeCapable()) {
            throw new RuntimeException("Not support MimeMessage, cannot attach bytes!");
        }
        attach(fileName, helper.getFileTypeMap().getContentType(file), file);
    }

    public void attach(String fileName, String contentType, byte[] bytes) throws MessagingException {
        attach(fileName, contentType, new ByteArrayResource(bytes));
    }

    public void attach(String fileName, String contentType, File file) throws MessagingException, FileNotFoundException {
        if (!file.exists()) {
            throw new FileNotFoundException("cannot use " + file + " as an attachment as it does not exist");
        }
        attach(fileName, contentType, new FileSystemResource(file));
    }

    public void attach(String fileName, String contentType, InputStreamSource source) throws MessagingException {
        doAddInline(fileName, contentType, source, true);
    }

    /**
     * 添加Inline文件或资源
     * @param file
     */
    public void inline(File file) throws MessagingException, FileNotFoundException {
        inline(file.getName(), file);
    }

    public void inline(String fileName, File file) throws MessagingException, FileNotFoundException {
        if (!this.isMimeCapable()) {
            throw new RuntimeException("Not support MimeMessage, cannot attach bytes!");
        }
        inline(fileName, helper.getFileTypeMap().getContentType(file), file);
    }

    public void inline(String contentId, String contentType, byte[] bytes) throws MessagingException {
        inline(contentId, contentType, new ByteArrayResource(bytes));
    }

    public void inline(String contentId, String contentType, File file) throws MessagingException, FileNotFoundException {
        if (!file.exists()) {
            throw new FileNotFoundException("Cannot use " + file + " as an attachment as it does not exist");
        }
        inline(contentId, contentType, new FileSystemResource(file));
    }

    public void inline(String contentId, String contentType, InputStreamSource source) {
        inlines.add(new Inline(contentId, contentType, source));
    }

    // 工具方法：转换为String数组
    private String[] toDestinationAddresses(String[] addresses) {
        return Arrays.copyOf(addresses, addresses.length);
    }

    // 工具方法：是否支持Mime类型
    private boolean isMimeCapable() {
        return mailSender instanceof JavaMailSender;
    }

    // 自定义Inline类
    private static class Inline {
        String id;
        String contentType;
        InputStreamSource toAdd;

        public Inline(String id, String contentType, InputStreamSource toAdd) {
            this.id = id;
            this.contentType = contentType;
            this.toAdd = toAdd;
        }
    }
}