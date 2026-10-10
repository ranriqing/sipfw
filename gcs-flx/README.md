# gcs-flx（后端）

SIP 实时监控后端。技术栈：Java 8 + Spring Boot 2.6 + MyBatis-Plus 3.5 +
Hibernate（仅建表）+ Apache Shiro + Druid + PostgreSQL。

- 入口：`gcs.GcsApplication`，端口 **18087**，上下文路径 `/gcs-flx`。
- SIP 实时分析模块：`src/main/java/gcs/app/sipfw/`（`SipAnalysisRuntime` 于应用就绪后
  拉起 `sipgrep` 抓包，`SipAnalysisEngine` 解析统计，推送到 STOMP `/topic/sipfw`）。
- 元视图播种：`src/main/resources/meta/seed/views/*.json`，由 `gcs.app.vm.init.MetaInitService`
  在 `gcs.meta-init.enabled=true` 时加载（菜单/页面）。

构建与运行、数据库及环境变量配置详见仓库根目录 [README](../README.md)。

```bash
mvn compile                       # 仅编译
mvn spring-boot:run               # 运行（默认 profile dev）
mvn spring-boot:run -Dspring-boot.run.profiles=prod   # 开启 SIP 抓取
```

> 需在 IDE 中启用 Lombok 插件。
