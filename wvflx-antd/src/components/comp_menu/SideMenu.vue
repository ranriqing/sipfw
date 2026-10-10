<template>
  <ALayoutSider
    width="200px"
    class="sider"
    :theme="theme"
    :collapsible="true"
    :trigger="null"
    v-model="collapsed">
    <Logo :showTitle="!collapsed" />
    <SMenu
      :mode="mode"
      :theme="theme"
      :menus="cptDynamicMenus"
      @select="onSelect" />
  </ALayoutSider>
</template>

<!--
# 组件说明
  基于SMenu.jsx的系统菜单组件；
  从MetaViewMenu中读取activeView(激活视图)下的全部菜单；
  菜单支持绑定路由和绑定href路径两种模式，默认绑定最新页面；
# 配置组件
  mode: String          // 同Menu，默认inline
  collapsed: Boolean    // 是否收起，默认false
# 操作方法
  $emit('menuSelect', obj)
-->

<script>
  import ALayout from 'ant-design-vue/lib/layout';
  import { mapGetters } from "vuex";
  import { GETTERS } from '@/constants'

  import MetaViewMenu from '@/store/models/MetaViewMenu'
  import { buildMenuTree } from '@/utils/util_menu'
  import Logo from '@/components/comp_tools/Logo'
  import SMenu from './SMenu'

  export default {
    name: 'SideMenu',
    components: {
      ALayoutSider: ALayout.Sider,
      Logo,
      SMenu,
    },
    props: {
      activeTopMenu: {
        type: Number,
        required: false,
        default: 0,
      },
      // 菜单mode
      mode: {
        type: String,
        required: false,
        default: 'inline'
      },
      theme: {
        type: String,
        required: false,
        default: 'light'
      },
      // 同步折叠状态
      collapsed: {
        type: Boolean,
        required: false,
        default: false
      },
      // 启用顶部菜单
      topMenuDisabled: {
        type: Boolean,
        required: false,
        default: false
      },
    },
    data(){
      return {
      }
    },
    computed:{
      ...mapGetters({
        /** Meta state */
        activeView: GETTERS.GET_META_ACTIVE_VIEW,
      }),
      cptDynamicMenus: function(){
        let that = this
        let viewId = that._.get(that.activeView, 'id')
        let menuArray = MetaViewMenu.query().where('metaViewId', viewId).orderBy('seqOrder').orderBy('id')
          .with('relMetaPages').get()
        let rootFilter = that.topMenuDisabled ? null : (item => item.id === that.activeTopMenu)
        let rootMenus = buildMenuTree(menuArray, viewId, { rootFilter })
        return that.topMenuDisabled ? rootMenus : that._.get(rootMenus, '[0].children') || []
      }
    },
    methods: {
      onSelect (obj) {
        this.$emit('menuSelect', obj)
      },
    }
  }
</script>

<style lang="scss" scoped>
  .sider {
    /* Sider组件 */
    position: relative;
    z-index: 10;
    background-color: #fff;
    box-shadow: 2px 5px 8px 0 rgba(29, 35, 41, 0.05);
    .ant-menu-light {
      border-right-color: transparent;
    }
    /* 定义滚动条*/
    $scrollBarSize: 10px;
    ul.ant-menu {
      &::-webkit-scrollbar {
        width: $scrollBarSize;
        height: $scrollBarSize;
        background-color: transparent;
        display: none;
      }
      & .-o-scrollbar {
        display: none;
      }
      /* 兼容IE */
      -ms-overflow-style: none;
      -ms-scroll-chaining: chained;
      -ms-content-zooming: zoom;
      -ms-scroll-rails: none;
      -ms-content-zoom-limit-min: 100%;
      -ms-content-zoom-limit-max: 500%;
      -ms-scroll-snap-type: proximity;
      -ms-scroll-snap-points-x: snapList(100%, 200%, 300%, 400%, 500%);
      /* 定义滚动条轨道 */
      &::-webkit-scrollbar-track {
        background-color: transparent;
      }
      /* 定义滑块 */
      &::-webkit-scrollbar-thumb {
        border-radius: $scrollBarSize;
        background-color: #eee;
        box-shadow: inset 0 0 6px rgba(0, 0, 0, 0.1);
        &:hover {
          background-color: #dddddd;
        }
        &:active {
          background-color: #bbbbbb;
        }
      }
    }
  }
</style>