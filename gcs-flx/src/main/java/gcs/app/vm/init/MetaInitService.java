package gcs.app.vm.init;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import gcs.app.vm.domain.MetaPage;
import gcs.app.vm.domain.MetaView;
import gcs.app.vm.domain.MetaViewMenu;
import gcs.app.vm.mapper.MetaPageMapper;
import gcs.app.vm.mapper.MetaViewMapper;
import gcs.app.vm.mapper.MetaViewMenuMapper;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 元视图启动播种：按 JSON 种子创建「视图 / 菜单 / 页面」。
 *
 * <p>幂等（自然键：view={@code name}、menu={@code (metaViewId,title)}、page={@code (metaViewId,pdent)}），
 * 重启只补缺失、不覆盖用户编辑。单 seed 失败仅记日志、不影响其他种子；不引事务——幂等保证重跑自愈。</p>
 *
 * <p>设计文档：{@code docs/superpowers/specs/2026-10-10-flx-meta-view-auto-init-design.md}</p>
 */
@Service
public class MetaInitService {

    private static final Logger log = LoggerFactory.getLogger(MetaInitService.class);

    /** 宽松解析：忽略模板里多写的字段，避免小白改坏 JSON 直接启动失败。 */
    private final ObjectMapper objectMapper = new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    @Autowired
    private MetaInitProperties properties;

    @Autowired
    private MetaViewMapper metaViewMapper;

    @Autowired
    private MetaViewMenuMapper metaViewMenuMapper;

    @Autowired
    private MetaPageMapper metaPageMapper;

    /** 启动入口：扫描并应用全部种子。 */
    public void applyAll() {
        List<MetaInitSeed> seeds = loadSeeds();
        if (seeds.isEmpty()) {
            log.info("[meta-init] 未找到任何种子文件（locations={}），跳过", properties.getSeedLocations());
            return;
        }
        log.info("[meta-init] 开始播种 {} 个元视图种子 ...", seeds.size());
        for (MetaInitSeed seed : seeds) {
            try {
                applySeed(seed);
            } catch (Exception e) {
                // 单 seed 失败不阻断其他；幂等设计下修好配置重启即可补齐
                log.error("[meta-init] 应用种子失败 seedKey={}", seedName(seed), e);
            }
        }
        log.info("[meta-init] 元视图播种完成");
    }

    /** 解析 {@link MetaInitProperties#getSeedLocations()} 下所有 JSON 种子。 */
    public List<MetaInitSeed> loadSeeds() {
        List<MetaInitSeed> seeds = new ArrayList<>();
        for (String location : properties.getSeedLocations()) {
            Resource[] resources;
            try {
                resources = resolver.getResources(location);
            } catch (Exception e) {
                log.warn("[meta-init] 解析种子位置失败：{}", location, e);
                continue;
            }
            for (Resource resource : resources) {
                try (InputStream in = resource.getInputStream()) {
                    MetaInitSeed seed = objectMapper.readValue(in, MetaInitSeed.class);
                    if (seed != null && seed.getView() != null && StringUtils.isNotBlank(seed.getView().getName())) {
                        seeds.add(seed);
                    } else {
                        log.warn("[meta-init] 跳过无效种子（view.name 为空）：{}", resource.getFilename());
                    }
                } catch (Exception e) {
                    log.error("[meta-init] 解析种子失败：{}", resource.getFilename(), e);
                }
            }
        }
        return seeds;
    }

    /** 应用单个种子：幂等建视图 → 菜单 → 页面。 */
    public void applySeed(MetaInitSeed seed) {
        String viewName = seed.getView().getName();

        MetaView view = findViewByName(viewName);
        if (view == null) {
            view = createView(seed.getView());
            log.info("[meta-init] 建视图 [{}] id={}", viewName, view.getId());
        } else {
            log.info("[meta-init] 视图已存在，复用 [{}] id={}", viewName, view.getId());
        }

        for (MetaInitSeed.Menu menuSeed : seed.getMenus()) {
            applyMenu(view.getId(), null, menuSeed, viewName);
        }
    }

    /** 递归创建菜单（含子菜单）及其绑定页面。幂等：菜单键 (view, parent, title)，页面键 (view, pdent)。 */
    private void applyMenu(Long viewId, Long parentId, MetaInitSeed.Menu menuSeed, String viewName) {
        if (menuSeed == null || StringUtils.isBlank(menuSeed.getTitle())) {
            log.warn("[meta-init] 视图 [{}] 下存在无标题菜单，跳过", viewName);
            return;
        }
        MetaViewMenu menu = findMenu(viewId, parentId, menuSeed.getTitle());
        if (menu == null) {
            menu = createMenu(viewId, parentId, menuSeed);
            log.info("[meta-init] 建菜单 [{}] id={}", menuSeed.getTitle(), menu.getId());
        }

        MetaInitSeed.Page pageSeed = menuSeed.getPage();
        if (pageSeed != null) {
            if (StringUtils.isBlank(pageSeed.getComponent())) {
                log.warn("[meta-init] 菜单 [{}] 未配置 page.component，跳过页面", menuSeed.getTitle());
            } else if (!isComponentAllowed(pageSeed.getComponent())) {
                log.warn("[meta-init] component [{}] 不在白名单，跳过菜单 [{}]",
                    pageSeed.getComponent(), menuSeed.getTitle());
            } else {
                String pdent = StringUtils.defaultIfBlank(pageSeed.getPdent(), pageSeed.getComponent());
                MetaPage existPage = findPage(viewId, pdent);
                if (existPage != null) {
                    // 页面按 (view,pdent) 幂等；若菜单绑定为空（历史菜单被删留下的孤儿），重新绑到当前菜单
                    if (existPage.getMetaViewMenuId() == null) {
                        existPage.setMetaViewMenuId(menu.getId());
                        metaPageMapper.updateById(existPage);
                        log.info("[meta-init] 重绑孤儿页面 pdent={} → 菜单 [{}]", pdent, menuSeed.getTitle());
                    } else {
                        log.info("[meta-init] 页面已存在，跳过 [{}] pdent={}", menuSeed.getTitle(), pdent);
                    }
                } else {
                    MetaPage page = createPage(viewId, menu.getId(), menuSeed, pageSeed, pdent);
                    log.info("[meta-init] 建页面 [{}] pdent={} component={} ident={}",
                        page.getTitle(), pdent, page.getComponent(), page.getIdent());
                }
            }
        }

        for (MetaInitSeed.Menu child : menuSeed.getChildren()) {
            applyMenu(viewId, menu.getId(), child, viewName);
        }
    }

    /* ---------------- 创建 ---------------- */

    private MetaView createView(MetaInitSeed.View s) {
        MetaView view = new MetaView();
        view.setName(s.getName());
        view.setType(s.getType() != null ? s.getType() : MetaView.TYPE_ADMIN);
        view.setIcon(s.getIcon());
        view.setSeqOrder(s.getSeqOrder() != null ? s.getSeqOrder() : 100);
        // 仅当库中尚无任何视图时，把这个播种视图置为激活，避免重启抢用户当前视图
        view.setActive(metaViewMapper.selectCount(null) == 0);
        SqlHelper.retBool(metaViewMapper.insert(view));
        return view;
    }

    private MetaViewMenu createMenu(Long viewId, Long parentId, MetaInitSeed.Menu s) {
        MetaViewMenu menu = new MetaViewMenu();
        menu.setMetaViewId(viewId);
        menu.setParentId(parentId);
        menu.setTitle(s.getTitle());
        menu.setIcon(s.getIcon());
        menu.setSeqOrder(s.getSeqOrder() != null ? s.getSeqOrder() : 100);
        SqlHelper.retBool(metaViewMenuMapper.insert(menu));
        return menu;
    }

    private MetaPage createPage(Long viewId, Long menuId, MetaInitSeed.Menu menuSeed,
                                MetaInitSeed.Page s, String pdent) {
        MetaPage page = new MetaPage();
        page.setMetaViewId(viewId);
        page.setMetaViewMenuId(menuId);
        page.setPdent(pdent);
        // 镜像真实绑定逻辑：handlePageConfirm 的 tools.randomString(8)
        page.setIdent(RandomStringUtils.randomAlphanumeric(8).toLowerCase());
        page.setTitle(StringUtils.defaultIfBlank(s.getTitle(), menuSeed.getTitle()));
        page.setComponent(s.getComponent());
        page.setSeqOrder(s.getSeqOrder() != null ? s.getSeqOrder() : 1);
        page.setInitialized(false);
        SqlHelper.retBool(metaPageMapper.insert(page));
        return page;
    }

    /* ---------------- 查询（自然键） ---------------- */

    private MetaView findViewByName(String name) {
        return metaViewMapper.selectOne(
            Wrappers.<MetaView>lambdaQuery().eq(MetaView::getName, name).last("limit 1"));
    }

    private MetaViewMenu findMenu(Long viewId, Long parentId, String title) {
        return metaViewMenuMapper.selectOne(Wrappers.<MetaViewMenu>lambdaQuery()
            .eq(MetaViewMenu::getMetaViewId, viewId)
            .eq(MetaViewMenu::getTitle, title)
            .isNull(parentId == null, MetaViewMenu::getParentId)
            .eq(parentId != null, MetaViewMenu::getParentId, parentId)
            .last("limit 1"));
    }

    private MetaPage findPage(Long viewId, String pdent) {
        return metaPageMapper.selectOne(Wrappers.<MetaPage>lambdaQuery()
            .eq(MetaPage::getMetaViewId, viewId)
            .eq(MetaPage::getPdent, pdent)
            .last("limit 1"));
    }

    /* ---------------- 工具 ---------------- */

    private boolean isComponentAllowed(String component) {
        List<String> allowed = properties.getAllowedComponents();
        if (allowed == null || allowed.isEmpty()) {
            return true;
        }
        return allowed.contains(component);
    }

    private String seedName(MetaInitSeed seed) {
        if (seed == null) {
            return "?";
        }
        if (StringUtils.isNotBlank(seed.getSeedKey())) {
            return seed.getSeedKey();
        }
        return seed.getView() != null ? seed.getView().getName() : "?";
    }
}
