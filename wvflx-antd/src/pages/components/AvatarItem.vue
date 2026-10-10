<template>
  <a-popconfirm
    okText="应用"
    cancelText="取消"
    placement="bottomLeft"
    v-model="popVisible"
    @confirm="onItemEmit('itemConfirm')">
    <template slot="title">
      应用此版本页面? 
      <a @click="onItemEmit('itemPreview');"><a-icon type="eye"/></a>
      <a style="margin-left: 6px;" @click="popVisible=false; onItemEmit('itemDelete');"><a-icon type="delete"/></a>
    </template>
    <a-icon slot="icon" type="question-circle-o" style="color: #2db7f5" />
    <avatar v-if="_matchIcon(src)" :size="avatarSize" :icon="src" :title="tip" />
    <avatar v-else :size="avatarSize" :src="src" :title="tip" />
  </a-popconfirm>
</template>

<!--
# 组件说明
  页面版本条目
# 配置组件
  tip: String         // Tip提示信息
  src: String         // 图标名或图片路径
# 操作方法
  $emit(eventName)
-->

<script>
  import Avatar from 'ant-design-vue/es/avatar'
  import Tooltip from 'ant-design-vue/es/tooltip'

  export default {
    name: 'AvatarItem',
    components: {
      Avatar,
      Tooltip,
    },
    props: {
      tip: {
        type: String,
        default: '',
        required: false
      },
      src: {
        type: String,
        default: '',
      },
    },
    data () {
      return {
        size: this.$parent.avatarSize,
        popVisible: false,
      }
    },
    computed: {
      avatarSize () {
        return this.size !== 'mini' && this.size || 20
      }
    },
    methods: {
      onItemEmit(eventName){
        this.$emit(eventName);
      },
      // 工具方法
      _matchIcon(srcStr){
        return new RegExp(/^[\w|-]+$/).test(srcStr)
      }
    },
    watch: {
      '$parent.avatarSize' (val) {
        this.size = val
      }
    }
  }
</script>
