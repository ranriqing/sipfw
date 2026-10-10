<template>
  <div class="panel-header">
    <span @click="toggleCollapse">
      <a-icon :type="cptIcon"/> {{ title || '折叠分组' }}
    </span>
  </div>
</template>

<!--
# 组件说明
  Panel折叠分组组件，可以为折叠条定义icon和文字内容；
  只负责触发折叠、取消折叠事件，具体折叠动作由事件监听器负责；
# 组件图标
  <a-icon type="caret-right" />
  <a-icon type="caret-down" />
# 配置组件
  value: undefined                // 未使用
  title: String                   // 折叠标题
  icon: String                    // 折叠图标
# 操作方法
  $emit('change', value)
  $emit('collapsePanel', this.collapsed)
-->

<script>
  export default {
    name: 'GPanel',
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
      icon: {
        type: String,
        required: false
      },
      collapsible: {
        // type: String,
        required: false
      },
    },
    data () {
      return {
        collapsed: false,
      }
    },
    computed: {
      cptIcon: function() {
        if(this.icon){
          return this.icon
        }else{
          return this.collapsed?'caret-right':'caret-down'
        }
      },
    },
    methods: {
      onChange (value) {
        // Never
        this.$emit('change', value)
      },
      /** 扩展方法 */
      toggleCollapse(){
        let that = this
        let collapsibleStr = that._.toString(that.collapsible)
        if(collapsibleStr !== 'false'){
          that.collapsed = !that.collapsed
          that.$emit('collapsePanel', that.collapsed)
        }
      }
    },
  }
</script>

<style lang="scss" scoped>
  /* Panel开头 */
  .panel-header{
    text-align: left;
    overflow: hidden;
    line-height: 25px;
    & > span{
      // 左侧文本
      position: relative;
      color: grey;
      cursor: pointer;
      font-size: 1.1em;
      display: inline-block;
      // 中划线
      &:before, &:after{
        position: absolute;
        content: "";
        top: 50%;
        width: 2999px;
        height: 1px;
        background: #d3d3d3;
      }
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
