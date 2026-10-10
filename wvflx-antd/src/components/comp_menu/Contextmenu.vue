<template>
  <a-menu
    v-show="visible"
    class="contextmenu"
    :style="cptStyle"
    :selectedKeys="selectedKeys"
    @click="handleClick">
    <a-menu-item :key="item.key" v-for="item in itemList">
      <!-- Role右键菜单选项 -->
      <a-icon role="menuitemicon" v-if="item.icon" :type="item.icon" />
      {{ item.text }}
    </a-menu-item>
  </a-menu>
</template>

<!--
# 组件说明
  右键菜单功能组件，点击右键弹出菜单
# 配置组件
  visible: Boolean    // 是否可见，默认false
  itemList: [{
    key: '1',
    icon: 'arrow-left',
    text: '关闭左侧'
  }]
# 操作方法
  $emit('select', key, this.target)
-->

<script>
  export default {
    name: 'Contextmenu',
    components: {
    },
    props: {
      visible: {
        type: Boolean,
        required: false,
        default: false
      },
      // 菜单选项
      // [ {key: '1', icon: 'arrow-left', text: '关闭左侧'}, { ... }]
      itemList: {
        type: Array,
        required: true,
        default: function(){
          return []
        }
      }
    },
    data () {
      return {
        selectedKeys: [], // 始终为空
        left: 0,
        top: 0,
        target: null,
      }
    },
    computed: {
      cptStyle: function(){
        return {
          left: this.left + 'px',
          top: this.top + 'px'
        }
      }
    },
    created () {
      window.addEventListener('mousedown', e => this.closeMenu(e))
      // 监听右键菜单
      window.addEventListener('contextmenu', e => this.setPosition(e))
    },
    methods: {
      // 点击非菜单
      closeMenu (e) {
        if (['menuitemicon', 'menuitem'].indexOf(e.target.getAttribute('role')) < 0) {
          this.$emit('update:visible', false)
        }
      },
      // 获取右键参数
      setPosition (e) {
        this.left = e.clientX
        this.top = e.clientY
        this.target = e.target
      },
      // 选中菜单选项
      handleClick ({ key }) {
        this.$emit('select', key, this.target)
        this.$emit('update:visible', false)
      }
    }
  }
</script>

<style lang="scss" scoped>
  .contextmenu{
    position: fixed;
    z-index: 1;
    border: 1px solid #9e9e9e;
    border-radius: 4px;
    box-shadow: 2px 2px 10px #aaaaaa;
  }
</style>
