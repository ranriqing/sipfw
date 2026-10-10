package gcs.app.ugrp.controller;

import cn.hutool.core.lang.Dict;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import gcs.app.ugrp.domain.GcsGroup;
import gcs.app.ugrp.domain.GcsPermission;
import gcs.app.ugrp.domain.GcsRole;
import gcs.app.ugrp.domain.GcsUser;
import gcs.app.ugrp.domain.WechatUser;
import gcs.app.ugrp.service.GcsShiroService;
import gcs.modules.wechat.WechatConfig;
import gcs.base.core.GcsHolder;
import gcs.base.core.bean.GcsResult;
import gcs.base.shiro.PasswordHashAdapter;
import gcs.base.shiro.spi.SpiUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/profile")
public class ProfileController {

    @Autowired
    private GcsShiroService gcsShiroService;

    @Autowired
    private PasswordHashAdapter passwordHashAdapter;

    @Autowired
    private WechatConfig wechatConfig;

    @RequestMapping(value = "/currentUser", method = {RequestMethod.GET, RequestMethod.POST})
    public GcsResult currentUser(){
        Map<String, Object> modelMap = new HashMap<>();
        SpiUser spiUser = gcsShiroService.currentUser();
        if(spiUser.isGcsUser()){
            GcsUser gcsUser = spiUser.getGcsUser();
            String wxOpenId = gcsUser.getWxOpenId();
            modelMap.put("id", spiUser.getId());
            modelMap.put("tenantId", "tenant");
            modelMap.put("username", spiUser.getUsername());
            modelMap.put("displayName", spiUser.getDisplayName());
            modelMap.put("mobile", spiUser.getMobile());
            modelMap.put("avatar", spiUser.getAvatar());
            // 部门相关
            GcsGroup gcsGroup = Db.getById(gcsUser.getGcsGroupId(), GcsGroup.class);
            modelMap.put("groupId", gcsGroup.getId());
            modelMap.put("groupCaption", gcsGroup.getName());
            // 角色相关
            GcsRole gcsRole = Db.getById(gcsUser.getGcsRoleId(), GcsRole.class);
            modelMap.put("roleId", gcsRole.getId());
            modelMap.put("roleCaption", gcsRole.getName());
            modelMap.put("viewId", gcsRole.getViewId());
            modelMap.put("superAdmin", gcsUser.isSuperAdmin());
            // 权限信息（包含 name + permission）
            List<GcsPermission> permissions = Db.list(
                    Wrappers.lambdaQuery(GcsPermission.class).eq(GcsPermission::getRoleId, gcsRole.getId())
            );
            modelMap.put("permissions", permissions);
            // 版本兼容
            modelMap.put("nickname", spiUser.getDisplayName());
            // 关联微信
            if(wxOpenId!=null){
                WechatUser wechatUser = Db.getOne(Wrappers.lambdaQuery(WechatUser.class).eq(WechatUser::getWxOpenId, wxOpenId));
                if(wechatUser!=null){
                    modelMap.put("wxOpenId", wechatUser.getWxOpenId());
                    modelMap.put("wxNickname", wechatUser.getWxNickname());
                    modelMap.put("wxHeadImgUrl", wechatUser.getWxHeadImgUrl());
                    modelMap.put("wxSubscribe", wechatUser.getWxSubscribe());
                    modelMap.put("wxSubscribeTime", wechatUser.getWxSubscribeTime());
                }
            }
        }else if(spiUser.isWechatUser()){
            WechatUser wechatUser = spiUser.getWechatUser();
            modelMap.put("id", spiUser.getId());
            modelMap.put("tenantId", "tenant");
            modelMap.put("username", spiUser.getUsername());        // wxOpenId
            modelMap.put("displayName", spiUser.getDisplayName());  // wxNickname
            modelMap.put("nickname", spiUser.getDisplayName());
            modelMap.put("avatar", wechatUser.getWxHeadImgUrl());
            modelMap.put("viewId", wechatConfig.getViewId());       // 固定视图
            modelMap.put("superAdmin", false);
            modelMap.put("permissions", wechatConfig.getPermissions()); // 固定权限标识
        }
        return GcsResult.successResult().putAll(modelMap);
    }

    @PostMapping("/updateUser")
    public GcsResult updateUser(@RequestBody Map<String, Object> jsonMap){
        Dict jsonDict = new Dict(jsonMap);
        SpiUser currentUser = gcsShiroService.currentUser();
        if(currentUser.isGcsUser()){
            LambdaUpdateWrapper<GcsUser> updateWrapper = Wrappers.lambdaUpdate(GcsUser.class).eq(GcsUser::getId, currentUser.getId());
            if(jsonDict.getStr("nickname")!=null){
                updateWrapper.set(GcsUser::getNickname, jsonDict.getStr("nickname"));
            }
            if(jsonDict.getStr("mobile")!=null){
                updateWrapper.set(GcsUser::getMobile, jsonDict.getStr("mobile"));
            }
            if(jsonDict.getStr("remark")!=null){
                updateWrapper.set(GcsUser::getRemark, jsonDict.getStr("remark"));
            }
            if(jsonDict.getStr("avatar")!=null){
                updateWrapper.set(GcsUser::getAvatar, jsonDict.getStr("avatar"));
            }
            if(jsonDict.getStr("password")!=null){
                String passwordHash = passwordHashAdapter.hash(jsonDict.getStr("password"), null);
                // 防御：避免写入被截断的奇数长度hex（会导致 Shiro Hex.decode 报错）
                if(passwordHash != null && (passwordHash.length() % 2 != 0)){
                    throw new IllegalStateException("password_hash 长度为奇数，可能被数据库截断，请检查 gcs_user.password_hash 字段长度");
                }
                updateWrapper.set(GcsUser::getPasswordHash, passwordHash);
            }
            Db.update(updateWrapper);
        }
        return GcsResult.successResult("更新成功").put("id", currentUser.getId());
    }

}
