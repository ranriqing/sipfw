package gcs.modules.others.controller;

import com.baomidou.mybatisplus.extension.toolkit.Db;
import gcs.modules.others.service.GcsAvatarService;
import gcs.app.ugrp.domain.GcsUser;
import gcs.base.core.bean.GcsResult;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;

@RestController
@RequestMapping("/avatar")
public class AvatarController {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private GcsAvatarService gcsAvatarService;

    /**
     * 展示Avatar头像
     */
    @GetMapping("/showAvatar")
    public void showAvatar(@RequestParam(required = false) String avatar,
                           @RequestParam(required = false) Long uid,
                           HttpServletResponse response) {
        String avatarName = avatar;
        if (uid != null) {  // 评论等展示头像
            GcsUser user = Db.getById(uid, GcsUser.class);
            if (user != null) {
                avatarName = user.getAvatar();
            }
        }
        try {
            // 读入文件
            byte[] avatarBytes = gcsAvatarService.getAvatar(avatarName != null ? avatarName : "avatar-default.png", 200);
            String fileSuffix = FilenameUtils.getExtension(avatarName);
            response.setContentType("image/" + fileSuffix);

            // 写出文件
            OutputStream outputStream = response.getOutputStream();
            outputStream.write(avatarBytes);
            outputStream.flush();
        } catch (Exception e) {
            System.out.println("读取头像失败: " + e.getMessage());
            GcsResult gcsResult = GcsResult.failedResult("读取头像失败");
            try {
                response.setContentType("application/json");
                response.getWriter().write(gcsResult.toJson());
            } catch (IOException ioException) {
                logger.warn(ioException.getMessage(), ioException);
            }
        }
    }

    /**
     * 上传Avatar头像
     */
    @PostMapping("/uploadAvatar")
    public GcsResult uploadAvatar(@RequestParam("uploadFile") MultipartFile multipartFile) {
        GcsResult gcsResult;
        try {
            // 上传文件
            String fileSuffix = FilenameUtils.getExtension(multipartFile.getOriginalFilename());
            String avatarName = gcsAvatarService.saveAvatar(multipartFile.getInputStream(), fileSuffix);
            gcsResult = GcsResult.successResult().singleData(avatarName);
        } catch (Exception e) {
            System.out.println("上传头像失败: " + e.getMessage());
            gcsResult = GcsResult.failedResult("上传头像失败");
        }
        return gcsResult;
    }
}
