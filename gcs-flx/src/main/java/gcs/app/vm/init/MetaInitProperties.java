package gcs.app.vm.init;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 元视图自动初始化配置（{@code gcs.meta-init}）。
 *
 * <p>启动时按 JSON 种子创建「视图 / 菜单 / 页面」，仅绑定 {@code ref_*} 等自包含页面，
 * 使装机后直接进入可用状态。默认关闭，按部署/profile 开启，避免对存量库意外建视图。</p>
 *
 * <p>设计文档：{@code docs/superpowers/specs/2026-10-10-flx-meta-view-auto-init-design.md}</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "gcs.meta-init")
public class MetaInitProperties {

    /** 是否启用启动播种；默认关。 */
    private boolean enabled = false;

    /** 种子文件位置（支持 classpath 通配）。一文件一视图。 */
    private List<String> seedLocations = new ArrayList<>(Arrays.asList("classpath*:meta/seed/views/*.json"));

    /**
     * 允许的 page {@code component} 白名单（对应前端 {@code pageMapper} 的 key）。
     *
     * <p>留空 = 不校验（只要求非空）。默认取 {@code wvflx-antd/src/pages/pageMapper.js} 中的
     * {@code ref_*} / {@code tpl_*} key，用于把"配了但前端没有的 component"挡在库外。
     * 新增自定义页面时需同步此处（该 JS 文件是前端唯一真源，本列表为后端投影）。</p>
     */
    private List<String> allowedComponents = new ArrayList<>(DEFAULT_ALLOWED_COMPONENTS);

    /** 与 {@code pageMapper.js} 保持一致的默认白名单。 */
    public static final List<String> DEFAULT_ALLOWED_COMPONENTS = Arrays.asList(
        // base
        "ref_ConfigPage", "ref_GroupPage", "ref_RolePage", "ref_TenantPage",
        // gcs
        "ref_AddrPage", "ref_MessagePage", "ref_ChangePage"
    );
}
