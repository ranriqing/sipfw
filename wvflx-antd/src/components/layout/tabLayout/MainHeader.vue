<template>
  <a-layout-header>
    <!-- 说明: 桌面模式为左右模式按需占用空间，手机模式左中右占用空间 -->
    <div class="header">
      <!-- #### 左侧组件 #### -->
      <!-- Desktop时，不占用额外的左侧空间 -->
      <div class="header-left" :style="{'flex-grow': stores.isDesktop()?0:1}">
        <!-- Toggle按钮 -->
        <div class="toggle-trigger">
          <a-icon
            class="trigger-icon"
            :type="sidebarOpened ? 'menu-fold' : 'menu-unfold'"
            @click="toggleSideMenu">
          </a-icon>
        </div>

        <!-- 顶层菜单入口 -->
        <div v-if="!topMenuDisabled" class="top-level-menu">
          <a-menu
            theme="dark"
            mode="horizontal"
            :selectedKeys="[activeTopMenu]"
            :style="{ lineHeight: '60px', display: 'inline-block' }">
            <a-menu-item
              v-for="topMenu in menus.filter(item => item.pId === 0)"
              :key="topMenu.id">
              <!-- 顶层菜单 -->
              <a v-if="topMenu.children && topMenu.children.length>0" @click="onTopMenuClick(topMenu.id)" class="link">
                <a-icon :type="topMenu.icon || 'question'"></a-icon>{{ topMenu.title }}
              </a>
              <!-- 链接菜单：lab 模式展示链接 -->
              <router-link
                v-else-if="topMenu.bindHref && stores.isLabState()"
                :to="{name: 'hrefPage', query: {mId: topMenu.id, href: topMenu.bindHref}}"
                class="link"
                @click.native="onTopMenuClick(topMenu.id)">
                <a-icon :type="topMenu.icon || 'question'"></a-icon>{{ topMenu.title }}
              </router-link>
              <!-- 链接菜单：非 lab 新窗口打开 -->
              <a v-else-if="topMenu.bindHref" :href="topMenu.bindHref" target="_blank" class="link" @click="onTopMenuClick(topMenu.id)">
                <a-icon :type="topMenu.icon || 'question'"></a-icon>{{ topMenu.title }}
              </a>
              <!-- 绑定路由 -->
              <router-link
                v-else-if="topMenu.bindRoute"
                :to="{name: topMenu.bindRoute}"
                class="link"
                @click.native="onTopMenuClick(topMenu.id)">
                <a-icon :type="topMenu.icon || 'question'"></a-icon>{{ topMenu.title }}
              </router-link>
              <!-- 暂无路由 -->
              <router-link
                v-else
                :to="{name: 'entryTmpl', query: {mId: topMenu.id}}"
                class="link"
                @click.native="onTopMenuClick(topMenu.id)">
                <a-icon :type="topMenu.icon || 'question'"></a-icon>{{ topMenu.title }}
              </router-link>
            </a-menu-item>
          </a-menu>
        </div>

        <!-- Lab菜单入口 -->
        <template v-if="stores.isLabState()">
          <LabViewMenu></LabViewMenu>
          <LabFixedMenu></LabFixedMenu>
        </template>

        <!-- 系统欢迎语 -->
        <div v-if="false && !stores.isLabState()" style="margin-left: 10px;">
          <a-icon v-if="!wsConnected" type="loading" style="margin-right: 6px;" />
          <template v-if="stores.isDesktop()">
            {{ stores.globalConfig(GLOBAL.WELCOME_DESKTOP) | Ellipsis(18) }}
          </template>
        </div>
      </div>

      <!-- #### 中间组件 #### -->
      <div class="header-center">
        <!-- 当前菜单(手机) -->
        <span v-if="stores.isMobile()" class="mobile-menu-title">
          {{ _.get(menuTitleRoute($route), 'meta.title') }}
        </span>
      </div>

      <!-- #### 右侧组件 #### -->
      <div class="header-right">
        <a-icon
          v-if="stores.isDesktop()"
          class="global-search-icon"
          type="search"
          @click="openGlobalSearch" />
        <a-icon
          v-if="stores.isDesktop()"
          class="fullscreen-icon"
          :type="fullscreen.isFullscreen ? 'fullscreen-exit' : 'fullscreen'"
          @click="toggleFullscreen">
        </a-icon>
        <ProfileMenu @toggleStoreLab="toggleStoreLab" />
      </div>
    </div>
    <GlobalSearch ref="globalSearch" />
  </a-layout-header>
</template>

<!--
# 组件说明
  主体Layout头部组件，同时兼容手机和PC端；
  只有超管在PC端时支持切换到后台配置模式；
# 配置组件
  暂无
# 操作方法
  $emit('toggleSideMenu')
-->

<script>
  import ALayout from 'ant-design-vue/lib/layout';

  import { tools, stores } from '@/utils/util'
  import { mapGetters, mapActions } from "vuex";
  import { GETTERS, ACTIONS, GLOBAL, MODE } from '@/constants'
  import { postAction } from '@/api/api'
  import { api as fullscreen } from 'vue-fullscreen'

  import MetaViewMenu from '@/store/models/MetaViewMenu'
  import MetaPage from '@/store/models/MetaPage'

  import ProfileMenu from './tools/ProfileMenu'
  import LabFixedMenu from './tools/LabFixedMenu'
  import LabViewMenu from './tools/LabViewMenu'
  import ActivityMenu from './tools/ActivityMenu'
  import GlobalSearch from './tools/GlobalSearch'

  export default {
    name: 'MainHeader',
    components: {
      ALayoutHeader: ALayout.Header,
      ProfileMenu,
      LabFixedMenu,
      LabViewMenu,
      ActivityMenu,
      GlobalSearch,
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
      topMenuDisabled: {
        type: Boolean,
        required: false,
        default: false
      },
    },
    data() {
      return {
        MODE: MODE,
        GLOBAL: GLOBAL,
        tools: tools,
        stores: stores,
        fullscreen: fullscreen,
        dialPhone: undefined,
        queryLab: undefined,
      }
    },
    computed: {
      ...mapGetters({
        /** App state */
        wsConnected: GETTERS.GET_APP_WS_CONNECTED,
        sidebarOpened: GETTERS.GET_APP_SIDEBAR_OPENED,
        currentUser: GETTERS.GET_USER_CURRENT_USER,
        currentExten: GETTERS.GET_USER_CURRENT_EXTEN,
      }),
    },
    inject: ['onTopMenuClick'],
    methods: {
      ...mapActions({
        setAppLabState: ACTIONS.SET_APP_LAB_STATE,
      }),
      toggleStoreLab(){
        this.setAppLabState(stores.isStoreLab()?'app':'lab')
      },
      toggleSideMenu() {
        this.$emit('toggleSideMenu')
      },
      toggleFullscreen(){
        fullscreen.toggle()
      },
      openGlobalSearch(){
        if (this.$refs.globalSearch) {
          this.$refs.globalSearch.open()
        }
      },
      onGlobalSearchKeydown(event){
        if ((event.metaKey || event.ctrlKey) && (event.key === 'k' || event.key === 'K')) {
          event.preventDefault()
          this.openGlobalSearch()
        }
      },
      // 为route设置meta.title
      menuTitleRoute(route) {
        let menuId = route.query.mId
        if(!menuId){
          let routePage = MetaPage.query().where('ident', route.name)
            .orderBy('seqOrder', 'asc').orderBy('id', 'desc').first()
          menuId = this._.get(routePage, 'menuId', 0)
        }
        let routeMenu = MetaViewMenu.find(menuId)
        if(routeMenu){
          let titledMeta = Object.assign({},route.meta,{title:routeMenu.title})
          return Object.assign({},route,{meta:titledMeta})
        }
        return Object.assign({},route)
      },
      doOriginate(){
        let that = this
        // 发起呼叫
        postAction('/ccDial/userDial', {phone: that.dialPhone}).then(resp => {
          resp || true
          that.dialPhone = undefined
          that.$message.success(`呼叫成功`);
        }).catch(err => {
          err || true
          that.$message.success(err.message);
        })
      }
    },
    created() {
      this.queryLab = this.$route.query.lab
    },
    mounted() {
      window.addEventListener('keydown', this.onGlobalSearchKeydown)
    },
    beforeDestroy() {
      window.removeEventListener('keydown', this.onGlobalSearchKeydown)
    },
  }
</script>

<style lang="scss" scoped>
  $height: 60px;
  /* Layout下的Header定义 */
  .layout {
    .ant-layout-header {
      height: $height;
      line-height: $height;
      padding: 0;
    }
    .header {
      background-color: #002140;
      color: #b7b7b7;
      transition: background 300ms;
      display: flex;
      flex-direction: row;
      justify-content: space-between;
      align-items: center;
      .header-left{
        border-left: 1px solid rgba(0, 0, 0, 0.5);
        flex: 1;
        display: flex;
        flex-direction: row;
        justify-content: flex-start;
        white-space: nowrap;
        column-gap: 6px;
        .toggle-trigger{
          display: flex;
          flex-direction: row;
          justify-content: center;
          align-items: center;
          .trigger-icon {
            font-size: 22px;
            padding: 8px 8px;
            cursor: pointer;
            vertical-align: middle;
            transition: color 300ms, background 300ms;
            &:hover {
              background: rgba(255, 255, 255, 0.3);
            }
          }
        }
        .top-level-menu{
          .ant-menu.ant-menu-dark .ant-menu-item-selected{
            background-color: #039267;
          }
          .link{
            font-size: 15px;
            font-weight: 500;
            i.anticon{
              font-size: 16px;
            }
          }
        }
      }
      .header-center{
        line-height: 1;
        display: flex;
        flex-direction: row;
        justify-content: center;
        align-items: center;
        .mobile-menu-title{
          font-weight: bold;
          position: absolute;
        }
      }
      .header-right{
        flex: 1;
        line-height: 1;
        display: flex;
        flex-direction: row;
        justify-content: flex-end;
        align-items: center;
        column-gap: 8px;
        .fullscreen-icon{
          display: inline-flex;
          align-items: center;
          justify-content: center;
          width: 32px;
          height: 32px;
          border-radius: 6px;
          font-size: 24px;
          cursor: pointer;
          transition: all 0.18s ease;
          &:hover {
            background: rgba(255, 255, 255, 0.15);
            color: #ffffff;
          }
        }
        .global-search-icon{
          display: inline-flex;
          align-items: center;
          justify-content: center;
          width: 32px;
          height: 32px;
          border-radius: 6px;
          font-size: 24px;
          cursor: pointer;
          transition: all 0.18s ease;
          &:hover {
            background: rgba(255, 255, 255, 0.15);
            color: #ffffff;
          }
        }
        .cc-wrapper{
          .input-part{
            width: 174px;
            display: inline-block;
            line-height: 1;
            vertical-align: middle;
            margin-right: 16px;
          }
        }
      }
    }
  }
</style>