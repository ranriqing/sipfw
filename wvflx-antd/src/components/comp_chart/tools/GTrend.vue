<template>
  <div :class="['trend', reverseColor && 'reverse-color']">
    <span>
      <slot name="term" />
      <span class="item-text">
        <slot />
      </span>
    </span>
    <span :class="[flag]"><a-icon :type="`caret-${flag}`" /></span>
  </div>
</template>

<!--
# 组件说明
  趋势箭头组件，可以展示向上或向下的趋势箭头
# 组件图标
  <a-icon type="caret-up" />
  <a-icon type="caret-down" />
# 配置组件
  flag: String              // 上升下降，up|down
  reverseColor: Boolean     // 颜色反转，默认false
# 操作方法
  暂无
-->

<script>
  export default {
    name: "GTrend",
    props: {
      // 上升下降: up|down
      flag: {
        type: String,
        required: true
      },
      // 颜色反转
      reverseColor: {
        type: Boolean,
        default: false
      }
    }
  }
</script>

<style lang="scss" scoped>
  $red-6: #f5222d;
  $green-6: #52c41a;

  .trend{
    display: inline-block;
    font-size: 14px;
    line-height: 20px;
    /* 上升下降标识 */
    .up, .down {
      margin-left: 4px;
      position: relative;
      font-size: 12px;
      top: 1px;
    }
    .up {
      color: $red-6;
    }
    .down {
      color: $green-6;
      top: -1px;
    }
    /* 反转颜色 */
    &.reverse-color .up {
      color: $green-6;
    }
    &.reverse-color .down {
      color: $red-6;
    }
    /* 内容文本 */
    .item-text {
      display: inline-block;
      margin-left: 8px;
      color: rgba(0,0,0,.85);
    }
  }
</style>