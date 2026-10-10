package gcs;

import cn.hutool.core.lang.Dict;
import cn.hutool.core.map.MapUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import gcs.app.vm.domain.MetaView;
import gcs.app.vm.init.MetaInitProperties;
import gcs.app.vm.init.MetaInitService;
import gcs.app.vm.mapper.MetaViewMapper;
import gcs.app.ugrp.domain.*;
import gcs.base.YmlConfig;
import gcs.base.shiro.PasswordHashAdapter;
import gcs.modules.config.domain.InitScElement;
import gcs.modules.config.domain.ScConfig;
import gcs.modules.config.mapper.ScConfigMapper;
import gcs.modules.others.service.GcsAvatarService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import javax.sql.DataSource;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;
import java.util.*;

@Component
public class GcsBootStrap  implements ApplicationRunner {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private GcsAvatarService avatarService;

    @Autowired
    private PasswordHashAdapter passwordHashAdapter;

    @Autowired
    private ScConfigMapper scConfigMapper;

    @Autowired
    private YmlConfig ymlConfig;

    @Autowired
    private MetaInitProperties metaInitProperties;

    @Autowired
    private MetaInitService metaInitService;

    @Autowired
    private MetaViewMapper metaViewMapper;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        System.out.println("项目初始化...");
        System.out.println("基础目录 rootBase: " + ymlConfig.getRootBase());

        /* 初始化基础目录 */
        this.initDirs();

        /* 执行Sql语句：脚本路径 -> 说明 */
        Map<String, String> sqlMap = new LinkedHashMap<>();
        sqlMap.put("db/init_sequence.sql", "初始化序列函数");

        try (Connection connection = dataSource.getConnection()) {
            for (Map.Entry<String, String> entry : sqlMap.entrySet()) {
                String sqlScript = StreamUtils.copyToString(
                    new ClassPathResource(entry.getKey()).getInputStream(),
                    StandardCharsets.UTF_8
                );
                try (Statement statement = connection.createStatement()) {
                    statement.execute(sqlScript);
                    System.out.println("成功执行SQL：" + entry.getValue() + "（" + entry.getKey() + "）");
                }
            }
        }

        /* 初始化配置项 */
        this.initializeConfigs();

        /* 初始化元视图（视图/菜单/页面，按配置播种；默认关闭，按 profile 开启） */
        if (metaInitProperties.isEnabled()) {
            this.metaInitService.applyAll();
        }

        /* 初始化头像文件 */
        this.avatarService.ensurePrefixAvatars("seed-");
        this.avatarService.ensurePrefixAvatars("avatar-");

        /* 初始化组织结构 */
        GcsGroup rootGroup = Db.getById(-1L, GcsGroup.class);
        if(rootGroup == null) {
            this.initGroupData();
        }

        /* 超级管理员角色(id=-1)绑定到播种的视图 */
        this.bindAdminRoleView();
    }

    /**
     * 把「激活的」视图绑定到超级管理员角色({@code id=-1})。
     * 仅当该角色尚未绑定视图时设置，避免覆盖已有配置。
     */
    private void bindAdminRoleView() {
        GcsRole adminRole = Db.getById(-1L, GcsRole.class);
        if (adminRole == null || adminRole.getViewId() != null) {
            return;
        }
        MetaView view = metaViewMapper.selectOne(Wrappers.<MetaView>lambdaQuery()
            .eq(MetaView::getActive, true)
            .orderByAsc(MetaView::getSeqOrder)
            .last("limit 1"));
        if (view == null) {
            return;
        }
        adminRole.setViewId(view.getId());
        Db.updateById(adminRole);
        System.out.println("超级管理员角色绑定视图：" + view.getName() + "(" + view.getId() + ")");
    }

    /**
     * 初始化基础目录：{@code gcs.rootBase} 及其下各配置目录（storage / temp / avatar）。
     *
     * <p>等首次落盘时才建目录是不够的：{@code MultipartFile.transferTo} 走 Servlet 的
     * {@code Part.write}，它不创建父目录，目录缺失时直接抛
     * {@code FileNotFoundException: ... (No such file or directory)}。
     * 这里按配置一次性把目录补齐，缺哪层建哪层。</p>
     */
    private void initDirs() {
        List<String> dirPathList = Arrays.asList(
            ymlConfig.getRootBase(),
            ymlConfig.getFileStorageRootDir(),
            ymlConfig.getTempStorageRootDir(),
            ymlConfig.getToolsAvatarRootDir()
        );
        for (String dirPath : dirPathList) {
            File dir = new File(dirPath);
            if (!dir.exists() && !dir.mkdirs()) {
                System.out.println("基础目录创建失败：" + dir.getAbsolutePath());
            }
        }
    }

    /** 初始化配置项 */
    public void initializeConfigs() {
        // 创建配置列表
        List<Dict> dictList = Arrays.asList(
            Dict. of("key", "sciScCategory", "multiLevel", false, "allowModify", true, "freeCreation", false),
            Dict. of("key", "sciDomainCategory", "multiLevel", false, "allowModify", true, "freeCreation", false),
            Dict. of("key", "sciFloCategory", "multiLevel", false, "allowModify", true, "freeCreation", false),
            Dict. of("key", "sciScheduleType", "multiLevel", false, "allowModify", false, "freeCreation", false),
            Dict. of("key", "sciMultiLevel", "multiLevel", true, "allowModify", true, "freeCreation", false)
        );
        // 处理每个配置
        dictList.forEach(dict -> {
            // 不存在则保存
            List<ScConfig> scConfigList = scConfigMapper.selectByMap(MapUtil.of("key", dict.get("key")));
            if (scConfigList.isEmpty()) {
                ScConfig scConfig = dict.toBean(ScConfig.class);
                scConfigMapper.insert(scConfig);
                if("sciScheduleType".equals(dict.get("key"))) {
                    List<Dict> typeList = Arrays.asList(
                            Dict. of("id", 101, "seqOrder", 1, "title", "待办", "tagStyle", "#108ee9"),
                            Dict. of("id", 102, "seqOrder", 2, "title", "会议", "tagStyle", "#ff9500"),
                            Dict. of("id", 103, "seqOrder", 3, "title", "发邮件", "tagStyle", "#2fbc52"),
                            Dict. of("id", 104, "seqOrder", 4, "title", "打电话", "tagStyle", "#2fbc52"),
                            Dict. of("id", 105, "seqOrder", 5, "title", "仅提醒", "tagStyle", "#9952e1")
                    );
                    typeList.forEach(typeDict -> {
                        InitScElement scElement = typeDict.toBean(InitScElement.class);
                        scElement.setScConfigId(scConfig.getId());
                        Db.save(scElement);
                    });
                }
            }
        });
    }

    /** 初始化组织结构 */
    private void initGroupData() {
        // 用户组
        InitGcsGroup rootGroup = new InitGcsGroup();
        rootGroup.setName("公司");
        rootGroup.setSequence(1);
        rootGroup.setId(-1L); // 根部门
        Db.save(rootGroup);

        // 用户角色
        InitGcsRole adminRole = new InitGcsRole();
        adminRole.setName("超级管理员");
        adminRole.setRemark("系统创建");
        adminRole.setId(-1L); // 超级管理员
        Db.save(adminRole);

        // 初始用户：密码取环境变量 GCS_ADMIN_PASSWORD，未配置时使用占位符 admin123。
        String adminPassword = System.getenv().getOrDefault("GCS_ADMIN_PASSWORD", "admin123");
        String passwordHash = passwordHashAdapter.hash(adminPassword, null);
        InitGcsUser adminUser = new InitGcsUser();
        adminUser.setUsername("admin");
        adminUser.setMobile("13800010001");
        adminUser.setNickname("超级管理员");
        adminUser.setPasswordHash(passwordHash);
        adminUser.setAvatar("avatar-default.png");
        adminUser.setGcsRoleId(adminRole.getId());
        adminUser.setGcsGroupId(rootGroup.getId());
        adminUser.setIsActive(true);
        adminUser.setId(-1L);
        Db.save(adminUser);
    }
}
