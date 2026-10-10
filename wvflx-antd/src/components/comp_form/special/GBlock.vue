<template>
  <div class="block-header">
    <span style="margin-left: 10px;">
      {{ title || '区隔块' }}
    </span>
  </div>
</template>

<!--
# 组件说明
  Block块组件
# 配置组件
  value: undefined                // 未使用
  title: String                   // 标题
# 操作方法
  $emit('change', value)
-->

<script>
  export default {
    name: 'GBlock',
    /* eslint-disable */
    props: {
      value: {
        // type: Object,
        required: false
      },
      /** 扩展Props */
      title: {
        type: String,
        required: false
      },
    },
    methods: {
      onChange (value) {
        // Never
        this.$emit('change', value)
      },
    },
  }
</script>

<style lang="scss" scoped>
  /* Block开头 */
  .block-header{
    text-align: left;
    overflow: hidden;
    // line-height: 25px;
    margin: 15px 0 15px -4px;
    // block扩展
    display: flex;
    flex-direction: row;
    justify-content: flex-start;
    align-items: center;
    border-left: 5px solid rgba(0, 147, 104, 0.8);
    padding: 0 10px 0 0;
    background: linear-gradient(
        -90deg,
        rgba(0, 147, 104, 0),
        rgba(0, 147, 104, 0.1) 90%,
        rgba(0, 147, 104, 0.2));
    & > span{
      // 左侧文本
      position: relative;
      // color: grey;
      font-weight: bold;
      cursor: pointer;
      font-size: 1.1em;
      display: inline-block;
      // 中划线
      /*&:before, &:after{
        position: absolute;
        content: "";
        top: 50%;
        width: 2999px;
        height: 1px;
        background: #d3d3d3;
      }*/
      // 左侧间隔
      &:before{
        right: 100%;
        margin-right: 15px;
      }
      // 右侧间隔
      &:after{
        left: 100%;
        margin-left: 15px;
      }
    }
  }
</style>
