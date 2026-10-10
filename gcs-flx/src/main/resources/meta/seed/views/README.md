# 元视图种子（meta-init）

启动时按这里的 JSON 文件创建「视图 / 菜单 / 页面」，让装机后直接进入可用状态。
**一文件一视图**，文件名任意，扩展名必须是 `.json`（本目录下的 `*.json` 会被扫描）。

> `example.json.example` 是**惰性示例**（扩展名不是 `.json`，不会被扫描）。需要时复制为 `xxx.json` 再改内容。

## 启用

默认关闭。在需要自动播种的部署/profile 里打开：

```yaml
gcs:
  meta-init:
    enabled: true
    # 可选：覆盖种子位置（默认 classpath*:meta/seed/views/*.json）
    # seed-locations:
    #   - "classpath*:meta/seed/views/*.json"
```

## 格式

```json
{
  "seedKey": "ops",
  "view": { "name": "运维视图", "type": 0, "icon": "team", "seqOrder": 10 },
  "menus": [
    {
      "title": "全局配置", "icon": "compass", "seqOrder": 1,
      "page": { "component": "ref_ConfigPage", "pdent": "SysConfig" }
    },
    {
      "title": "组织结构", "icon": "home", "seqOrder": 2,
      "page": { "component": "ref_GroupPage", "pdent": "SysGroup" }
    }
  ]
}
```

### 字段

| 字段 | 说明 |
|---|---|
| `seedKey` | 幂等锚点；留空取 `view.name`。仅用于日志/排查 |
| `view.name` | 视图名（幂等键）。必填 |
| `view.type` | `0` 开放 / `1` 超管。默认 `1` |
| `menus[].title` | 菜单标题（幂等键）。必填 |
| `menus[].page.component` | 前端组件映射 key，**必须命中 `wvflx-antd/src/pages/pageMapper.js`**（后端白名单见 `MetaInitProperties`） |
| `menus[].page.pdent` | 逻辑页面类型（对应 `pageMapper` 条目的 `key`，如 `SysConfig`）；留空取 `component` |
| `menus[].page.title` | 页面标题；留空取菜单 `title` |

### 命名对齐（务必照做）

取值来自 `wvflx-antd/src/pages/LabPageSelect.vue` 的 `cpt*PageItems` 条目：

- `component` = 条目的 **`pagePath`**（如 `ref_ConfigPage`）
- `pdent` = 条目的 **`key`**（如 `SysConfig`）
- 页面 `ident`（路由名）**不要写**，由后端按真实绑定逻辑随机生成

## 行为

- **幂等**：按自然键 `view.name` / `(view, 菜单title)` / `(view, pdent)` 判存在，只补缺失，**重启不覆盖你的编辑**。
- **可编辑**：播种产物就是普通元数据，登录后可在「视图管理 / LabPageSelect」里改。
- **首建置 active**：仅当库中还没有任何视图时，第一个播种视图才置为激活，避免重启抢你当前视图。
- **失败隔离**：单个种子出错只记日志、不影响其他；修好配置重启即补齐。
- **普通用户可见性**：`type=1` 只超管可见；`type=0` 开放视图仍需角色绑定（后续能力，见设计文档 §9）。

设计文档：`docs/superpowers/specs/2026-10-10-flx-meta-view-auto-init-design.md`
