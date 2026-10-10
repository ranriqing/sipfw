<template>
  <a-card :body-style="{ padding: '15px' }" :bordered="false">
    <!-- Card头部内容 -->
    <div class="chart-card-header">
      <div class="meta">
        <span class="chart-card-title">{{ title }}</span>
        <span class="chart-card-action">
          <slot name="action" />
        </span>
      </div>
      <!-- 大字总数或百分比 -->
      <div v-if="!loading" class="total">
        <div v-if="total">{{ total }}</div>
        <slot v-else name="total" />
      </div>
    </div>
    <!-- Card图表内容 -->
    <div class="chart-card-content">
      <div class="content-fix">
        <a-spin :spinning="loading">
          <slot />
        </a-spin>
      </div>
    </div>
    <!-- Card底部内容 -->
    <div class="chart-card-footer">
      <div v-if="!loading" class="field">
        <slot name="footer" />
      </div>
    </div>
  </a-card>
</template>

<!--
# 组件说明
  卡片图表组件，展示图表缩略图
# 配置组件
  loading: Boolean          // 加载状态，默认false
  title: String             // 卡片标题
  total: String             // 卡片数值
# 操作方法
  暂无
-->

<script>
  import ASpin from 'ant-design-vue/lib/spin';

  export default {
    name: "GChartCard",
    components: {
      ASpin,
    },
    props: {
      // 加载状态
      loading: {
        type: Boolean,
        default: false
      },
      // 卡片标题
      title: {
        type: String,
        default: ''
      },
      // 卡片数值
      total: {
        type: String,
        default: ''
      },
    }
  }
</script>

<style lang="scss" scoped>
  /* Header样式 */
  .chart-card-header {
    position: relative;
    overflow: hidden;
    width: 100%;
    .meta {
      position: relative;
      overflow: hidden;
      width: 100%;
      color: rgba(0, 0, 0, .45);
      font-size: 14px;
      line-height: 22px;
    }
    .chart-card-action {
      cursor: pointer;
      position: absolute;
      top: 0;
      right: 0;
    }
    .total {
      overflow: hidden;
      text-overflow: ellipsis;
      word-break: break-all;
      white-space: nowrap;
      color: #000;
      margin-top: 4px;
      margin-bottom: 0;
      font-size: 30px;
      line-height: 38px;
      height: 38px;
    }
  }
  /* Content样式 */
  .chart-card-content {
    margin-bottom: 12px;
    position: relative;
    width: 100%;
    .content-fix-bak {
      position: absolute;
      left: 0;
      bottom: 0;
      width: 100%;
    }
  }
  /* Footer样式 */
  .chart-card-footer {
    > * {
      position: relative;
    }
    .field {
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
      margin: 0;
    }
  }
</style>