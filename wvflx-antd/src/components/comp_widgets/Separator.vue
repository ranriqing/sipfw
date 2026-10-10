<template>
  <div class="block-header separator">
    <span class="header-title" style="margin-left: 10px;">
      {{ cptTitle }}
    </span>
  </div>
</template>

<!--
# 组件说明
  分隔符Widget组件，展示带渐变背景的分隔标题;
# 配置组件
  sModel: Object        // 当前组件SModel(MetaPageComponent), 配置存于 sModel.extend
  sContext: Object      // 上下文sContext
# 操作方法
  暂无
-->

<script>
  import { converters } from '@/utils/util'

  export default {
    name: "Separator",
    props: {
      sModel: {
        type: Object,
        required: true,
      },
      sContext: {
        type: Object,
        required: false,
        default: function(){
          return {}
        }
      }
    },
    data() {
      return {
        /** 配置&状态 */
        converters: converters,
      };
    },
    computed: {
      sExtend: function() {
        return converters.parseObj(this.sModel?.extend)
      },
      cptTitle: function() {
        return this.sModel?.title || '分隔符'
      },
    },
  }
</script>

<style lang="scss" scoped>
  @import '~@/assets/scss/theme.scss';
  /* 分隔符 */
  .block-header{
    text-align: left;
    overflow: hidden;
    margin: 15px 0 15px 0;
    display: flex;
    flex-direction: row;
    justify-content: flex-start;
    align-items: center;
    padding: 10px 10px 10px 0;
    background: linear-gradient(
        -90deg,
        rgba(3, 146, 103, 0),
        rgba(3, 146, 103, 0.1) 90%,
        rgba(3, 146, 103, 0.2));
    .header-title{
      position: relative;
      font-weight: bold;
      cursor: pointer;
      font-size: 1.1em;
      display: inline-block;
    }
  }
</style>
