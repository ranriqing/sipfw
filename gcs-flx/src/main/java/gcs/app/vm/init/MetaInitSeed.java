package gcs.app.vm.init;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 元视图种子（JSON）反序列化目标。一文件一视图。
 *
 * <p>字段取值对齐前端 {@code LabPageSelect.vue} 的 {@code cpt*PageItems} 条目：
 * {@code component} = 条目 {@code pagePath}，{@code pdent} = 条目 {@code key}；
 * 页面 {@code ident} 不写，由后端按真实绑定逻辑生成。</p>
 */
@Data
public class MetaInitSeed {

    /** 幂等锚点；留空取 {@code view.name}。 */
    private String seedKey;

    private View view = new View();

    private List<Menu> menus = new ArrayList<>();

    /** 视图壳。 */
    @Data
    public static class View {
        private String name;
        /** 0 开放 / 1 超管；默认 1（与 {@code MetaView} 实体默认一致）。 */
        private Integer type = 1;
        private String icon;
        private Integer seqOrder = 100;
    }

    /** 菜单节点（支持子菜单 children）。 */
    @Data
    public static class Menu {
        private String title;
        private String icon;
        private Integer seqOrder = 100;
        private Page page;
        /** 子菜单（可选）；带 children 的节点通常自身不绑定 page。 */
        private List<Menu> children = new ArrayList<>();
    }

    /** 页面（绑定一个自包含 component）。 */
    @Data
    public static class Page {
        /** = 条目 pagePath，如 {@code ref_ConfigPage}；必须在白名单内。 */
        private String component;
        /** = 条目 key，如 {@code SysConfig}；留空取 component。 */
        private String pdent;
        /** 留空取菜单 title（真实逻辑 {@code title: menuTitle}）。 */
        private String title;
        private Integer seqOrder = 1;
    }
}
