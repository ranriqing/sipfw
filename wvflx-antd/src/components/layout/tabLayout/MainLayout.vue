<template>
  <div>
    <a-alert
      v-if="userAuthToken['origin_user']"
      type="warning"
      :banner="true"
      :show-icon="false"
      style="padding: 2px 15px; text-align: center;">
      <span slot="message">
        <a-icon @click="onDemoEvent" type="bulb" theme="filled" style="margin-right: 4px; color: #faad14;" />
        {{ stores.isMobile()?'切换用户模式':'您正处于切换用户模式' }}，
        切换回<a @click="onToOriginUser" style="margin-left: 6px;">之前用户</a>
      </span>
    </a-alert>
    <a-alert
      v-else-if="stores.isStoreLab()"
      type="warning"
      :banner="true"
      :show-icon="false"
      style="padding: 2px 15px; text-align: center;">
      <span slot="message">
        <a-icon type="bulb" theme="filled" style="margin-right: 4px; color: #faad14;" />
        {{ stores.isMobile()?'零代码开发模式':'您正处于零代码开发模式' }}，
        切换回<a @click="$refs.slotHeader.toggleStoreLab()" style="margin-left: 6px;">普通模式</a>
      </span>
    </a-alert>
    <a-alert
      v-else-if="userAuthToken['origin']"
      type="warning"
      :banner="true"
      :show-icon="false"
      style="padding: 2px 15px; text-align: center;">
      <span slot="message">
        <a-icon type="bulb" theme="filled" style="margin-right: 4px; color: #faad14;" />
        {{ stores.isMobile()?'租户管理模式':'您正处于租户管理模式' }}，
        切换回<a @click="onToOrigin" style="margin-left: 6px;">管理系统</a>
      </span>
    </a-alert>
    <a-layout class="layout" :class="[deviceType]">
      <!-- 左侧菜单 -->
      <template>
        <!-- Mobile菜单 -->
        <template v-if="isMobile">
          <i></i> <!-- 保留元素解决bug -->
          <a-drawer
            v-if="isMobile"
            width="200px"
            placement="left"
            wrap-class-name="drawer-sider"
            :closable="false"
            :visible="collapsed"
            @close="() => collapsed = false" >
            <SideMenu
              mode="inline"
              theme="dark"
              :collapsed="false"
              :collapsible="true"
              :topMenuDisabled="isMobile"
              :activeTopMenu="activeTopMenu"
              @menuSelect="menuSelect"/>
          </a-drawer>
        </template>
        <!-- Desktop菜单 -->
        <SideMenu
          v-else
          mode="inline"
          theme="light"
          :collapsible="true"
          :collapsed="collapsed"
          :activeTopMenu="activeTopMenu"
          @menuSelect="menuSelect" />
      </template>
      <!-- 主体内容 -->
      <a-layout :style="{ paddingLeft: 0 }" >
        <MainHeader
          ref="slotHeader"
          :menus="menus"
          :topMenuDisabled="isMobile"
          :activeTopMenu="activeTopMenu"
          @toggleSideMenu="toggleSideMenu" />
        <a-layout-content style="flex: 1; display: flex; flex-direction: column;">
          <slot />
        </a-layout-content>
        <!-- <MainFooter /> -->
      </a-layout>
    </a-layout>
  </div>
</template>

<!--
# 组件说明
  主体Layout内容组件
  页面布局为左侧功能菜单，右侧主体部分(头部、中间、尾部共上中下三部分)
# 配置组件
  暂无
# 操作方法
  $emit('toggleSideMenu')
-->

<script>
  const settings = require('@/settings')
  import AAlert from 'ant-design-vue/lib/alert';
  import ALayout from 'ant-design-vue/lib/layout';
  import ADrawer from 'ant-design-vue/lib/drawer';
  import { stores, tools } from '@/utils/util'
  import { mapGetters, mapActions } from 'vuex'
  import { GETTERS, ACTIONS, STORAGE } from '@/constants'
  import { postAction } from '@/api/api'

  import SideMenu from '@/components/comp_menu/SideMenu'
  import MainHeader from './MainHeader'
  import MainFooter from './MainFooter'

  export default {
    name: 'MainLayout',
    components: {
      AAlert,
      ALayout,
      ALayoutContent: ALayout.Content,
      ADrawer,
      SideMenu,
      MainHeader,
      MainFooter,
   },
    props: {
      menus: {              // See notes
        type: Array,
        required: true
      },
      activeTopMenu: {
        type: Number,
        required: false,
        default: 0,
      },
    },
    data() {
      return {
        /** 配置&状态 */
        stores: stores,
        collapsed: false,
      }
    },
    computed: {
      ...mapGetters({
        /** App state */
        deviceType: GETTERS.GET_APP_DEVICE_TYPE,
        isMobile: GETTERS.CPT_APP_DEVICE_MOBILE,
        sidebarOpened: GETTERS.GET_APP_SIDEBAR_OPENED,
        userAuthToken: GETTERS.GET_USER_AUTH_TOKEN,
      }),
    },
    provide: function () {
      let that = this
      return {
        onTopMenuClick: that.onTopMenuClick,
      }
    },
    methods: {
      ...mapActions({
        setSidebarOpened: ACTIONS.SET_APP_SIDEBAR_OPENED,
      }),
      onDemoEvent(e){
        this.$event.emit("icon:click",{name: e})
      },
      // 侧边状态切换
      toggleSideMenu() {
        let that = this
        that.collapsed = !that.collapsed // 切换状态
        that.setSidebarOpened(!that.collapsed)
        tools.triggerWindowResizeEvent()
      },
      // 选中菜单条目
      menuSelect() {
        if (this.isMobile) {
          this.collapsed = false
        }
      },
      // 切回管理系统
      onToOrigin(){
        let that = this
        let webPath = that.userAuthToken['origin']
        if(webPath){
          let stripedWebPath = webPath.startsWith('/')?webPath.substring(1):webPath
          window.location.href=`/${stripedWebPath}`
        }
      },
      // 切回之前用户
      onToOriginUser(){
        let that = this
        let promise = postAction('/auth/switchToken', {webPath: that.userAuthToken['origin']})
        promise.then(resp => {
          that.$avue.lsSet(STORAGE.AUTH_TOKEN, resp.gmodel, 30)
          window.location.href = settings.WEB_PATH
        }).catch(err => {
          that.$message.warn(`切换失败：${err['message']}`)
        })
        return promise
      },
      // 点击顶层菜单
      onTopMenuClick(topMenuId){
        let that = this
        if(topMenuId !== that.activeTopMenu){
          that.$emit('topMenu', topMenuId)
        }
      },
    },
    watch: {
      // 监听侧边状态
      sidebarOpened(val) {
        this.collapsed = !val
      }
    },
  }
</script>

<style lang="scss">
  .ant-drawer{
    &.drawer-sider {
      box-shadow: none;
      .ant-drawer-content {
        background-color: #001529;
      }
      .ant-drawer-body {
        padding: 0
      }
    }
  }
</style>
