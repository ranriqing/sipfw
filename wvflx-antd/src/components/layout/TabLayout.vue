<template>
  <MainLayout
    :menus="cptDynamicMenus"
    :activeTopMenu="activeTopMenu"
    @topMenu="onTopMenuClick">
    <!-- 右键菜单 -->
    <Contextmenu :itemList="contextMenuItems" :visible.sync="contextMenuVisible" @select="onContextMenuSelect" />
    <!-- 全部Tabs -->
    <a-tabs
      v-if="isMultipage"
      type="editable-card"
      class="tab-layout-tabs"
      style="height:52px"
      :hideAdd="true"
      :activeKey="activePage"
      @contextmenu.native="e => onContextmenu(e)"
      @change="changePage"
      @edit="editPage" >
      <a-tab-pane :id="page.fullPath" :key="page.fullPath" :closable="page.closable" v-for="page in pageList">
        <span slot="tab" :pagekey="page.fullPath">{{ page.meta.title }}</span>
      </a-tab-pane>
    </a-tabs>
    <!-- 页面内容 -->
    <div :class="isMobile?null:{'desktop-layout-margin': true}">
      <transition name="page-transition">
        <keep-alive v-if="isMultipage && !tools.isDev()">
          <router-view :key="$route.fullPath"/>
        </keep-alive>
        <router-view :key="$route.fullPath" v-else/>
      </transition>
    </div>
  </MainLayout>
</template>

<!--
# 组件说明
  Tab布局组件，Tab展示部分Layout组件;
  ** 此组件负责初始化首个Leaf菜单为初始页 **
  与MainLayout的区别与联系，MainLayout分为左侧，右侧（上、中、下三部分）;
  TabLayout相当于继承MainLayout，扩展了Tabs展示；
# 配置组件
  暂无
# 操作方法
  provide.closeTab(key)  // 关闭Tab
  changePage(key)        // 切换Page
  removePage(pageKey)    // 关闭Page，改变activePage
  removeTab(pageKey)     // 移除Tab，不改变activePage
-->

<script>
  import ATabs from 'ant-design-vue/lib/tabs';
  import { tools, watermark } from '@/utils/util'
  import { buildMenuTree, latestMenuPage, findMenuPath } from '@/utils/util_menu'
  import { mapGetters } from 'vuex'
  import { GETTERS } from '@/constants'
  import MainLayout from './tabLayout/MainLayout'
  import Contextmenu from '@/components/comp_menu/Contextmenu'
  import MetaPage from '@/store/models/MetaPage'
  import MetaViewMenu from '@/store/models/MetaViewMenu'
  import settings from '@/settings'

  export default {
    name: 'TabLayout',
    components: {
      ATabs,
      ATabPane: ATabs.TabPane,
      MainLayout,
      Contextmenu
    },
    data() {
      return {
        tools: tools,
        // 页面信息
        pageList: [],   // 全部页面$route对象
        linkList: [],   // 全部页面$route.fullPath列表
        activePage: '', // 激活页面key
        activeTopMenu: undefined,  // 当前激活顶层菜单
        // 右键菜单选项
        contextMenuVisible: false,
        contextMenuItems: [
          { key: '1', icon: 'arrow-left', text: '关闭左侧' },
          { key: '2', icon: 'arrow-right', text: '关闭右侧' },
          { key: '3', icon: 'close', text: '关闭其它' }
        ]
      }
    },
    computed: {
      ...mapGetters({
        isMobile: GETTERS.CPT_APP_DEVICE_MOBILE,
        multitab: GETTERS.GET_APP_CONFIG_MULTITAB,
        activeView: GETTERS.GET_META_ACTIVE_VIEW,
      }),
      // 计算属性
      isMultipage() {
        if (this.isMobile) {
          return false // 手机为单页面模式
        } else {
          return this.multitab
        }
      },
      cptDynamicMenus: function(){
        let that = this
        let viewId = that._.get(that.activeView, 'id')
        let menuArray = MetaViewMenu.query().where('metaViewId', viewId).orderBy('seqOrder').orderBy('id')
          .with('relMetaPages').get()
        return buildMenuTree(menuArray, viewId)
      }
    },
    provide: function () {
      let that = this
      return {
        closeTab: function(key){
          that.removeTab(key)
        }
      }
    },
    methods: {
      // 切换页面
      changePage(key) {
        this.activePage = key
      },
      // 新增或删除页面
      editPage(key, action) {
        if(action === 'remove'){
          this.removePage(key)
        }
      },
      // Tab的右键菜单
      onContextmenu(e) {
        let that = this
        const pagekey = that.getPageKey(e.target)
        if (pagekey !== null) {
          e.preventDefault()
          that.contextMenuVisible = true
        }
      },
      // 选中右键菜单
      onContextMenuSelect(menuKey, clickedTarget) {
        let that = this
        let pageKey = that.getPageKey(clickedTarget)
        switch (menuKey) {
          case '1':
            that.closeLeft(pageKey)
            break
          case '2':
            that.closeRight(pageKey)
            break
          case '3':
            that.closeOthers(pageKey)
            break
          default:
            break
        }
      },
      getPageKey(target, depth) {   // 获取页签key
        let that = this
        depth = depth || 0
        // 递归深度控制
        if (depth > 2) {
          return null
        }
        let pageKey = target.getAttribute('pagekey')
        pageKey = pageKey || (target.previousElementSibling ? target.previousElementSibling.getAttribute('pagekey') : null)
        // 尝试通过递归子元素获取pageKey
        return pageKey || (target.firstElementChild ? that.getPageKey(target.firstElementChild, ++depth) : null)
      },
      closeLeft(pageKey) {          // 关闭左侧菜单
        let that = this
        // 关闭左侧菜单(非首页)
        let index = that.linkList.indexOf(pageKey)
        if(index>1){
          that.linkList.splice(1,index-1)
          that.pageList.splice(1,index-1)
        }
        // 设置首页为激活页
        if (that.linkList.indexOf(that.activePage) < 0) {
          that.activePage = that.linkList[0]
        }
      },
      closeRight(pageKey) {         // 关闭右侧菜单
        let that = this
        // 关闭全部右侧菜单
        let index = that.linkList.indexOf(pageKey)
        that.linkList = that.linkList.slice(0, index + 1)
        that.pageList = that.pageList.slice(0, index + 1)
        // 设置尾页为激活页
        if (that.linkList.indexOf(that.activePage < 0)) {
          that.activePage = that.linkList[that.linkList.length - 1]
        }
      },
      closeOthers(pageKey) {        // 关闭其它菜单
        let that = this
        let index = that.linkList.indexOf(pageKey)
        if (index === 0) {
          // 首页直接删除之后的页面
          that.linkList = that.linkList.slice(index, index + 1)
          that.pageList = that.pageList.slice(index, index + 1)
          that.activePage = that.linkList[0]
        } else {
          // 只保留首页和目标页
          that.linkList = [that.linkList[0], that.linkList[index]]
          that.pageList = [that.pageList[0], that.pageList[index]]
          // 设置目标页为激活页
          that.activePage = that.linkList[1]
        }
      },
      // 删除页面：editPage 调用 removePage
      removePage(pageKey) {
        let that = this
        if (that.pageList.length === 1) {
          that.$message.warning('这是最后一页，不能再关闭了啦')
          return
        }
        // 要删除的Index
        let toDeleteIndex = that.linkList.indexOf(pageKey)
        // 删除Index页面
        that.pageList = that.pageList.filter(item => item.fullPath !== pageKey)
        that.linkList = that.linkList.filter(item => item !== pageKey)
        // 显示激活页面
        let toActiveIndex = toDeleteIndex >= that.linkList.length ? that.linkList.length - 1 : toDeleteIndex
        that.activePage = that.linkList[toActiveIndex]
      },
      // 移除Tab，不改变页面
      removeTab(pageKey) {
        let that = this
        // 删除Index页面
        that.pageList = that.pageList.filter(item => item.fullPath !== pageKey)
        that.linkList = that.linkList.filter(item => item !== pageKey)
      },
      // 为route设置meta.title
      menuTitleRoute(route) {
        let that = this
        // 通过Page找到Menu
        let menuId = that._.get(route, 'query.mId')
        if(!menuId){
          let routePage = MetaPage.query().where('ident', route.name)
            .orderBy('seqOrder', 'asc').orderBy('id', 'desc').first()
          menuId = routePage?routePage.metaViewMenuId:0
        }
        // 补充元信息
        let routeMenu = MetaViewMenu.find(menuId)
        if(routeMenu){
          let titledMeta = Object.assign({},route.meta,{title:routeMenu.title})
          return Object.assign({},route,{meta:titledMeta})
        }
        return Object.assign({},route)
      },
      // 首个bindRoute的Leaf菜单
      firstLeafMenuRoute(activeView){
        let that = this
        let viewId = that._.get(activeView, 'id')
        let menuArray = MetaViewMenu.query().where('metaViewId', viewId).orderBy('seqOrder').orderBy('id')
          .with('relMetaPages').get()
        for(let i=0; i<menuArray.length; i++){
          let item = menuArray[i]
          if(item.parentId === 0){
            let leafMenus = that._recurseLeafMenus(item, menuArray)
            // 首个绑定页面的子菜单（跳过 Datav 大屏页：不作为视图首页自动打开）
            let firstLeafMenu = leafMenus.find(leaf => {
              let leafPage = latestMenuPage(that._.get(leaf,'relMetaPages'), viewId)
              return leafPage && !(leafPage.pdent && leafPage.pdent.startsWith('Datav'))
            })
            // 路由到子菜单相关页面
            if(firstLeafMenu){
              let metaPage = latestMenuPage(that._.get(firstLeafMenu,'relMetaPages'), viewId)
              let bindRoute = metaPage.ident
              return {
                name: bindRoute,
                path: `/${metaPage.ident}`,
                fullPath: `/${metaPage.ident}`,
                closable: false, // 首页
                meta: {
                  // title: metaPage.title,
                  keepAlive: true,
                }
              }
            }
            // 视图只有 Datav 大屏页：首页直接落地到 hrefPage（不内嵌渲染大屏）
            let datavMenu = leafMenus.find(leaf => {
              let leafPage = latestMenuPage(that._.get(leaf,'relMetaPages'), viewId)
              return leafPage && leafPage.pdent && leafPage.pdent.startsWith('Datav')
            })
            if(datavMenu){
              let metaPage = latestMenuPage(that._.get(datavMenu,'relMetaPages'), viewId)
              let query = { mId: datavMenu.id, href: `${settings.WEB_PATH}/b/${metaPage.ident}` }
              return {
                name: 'hrefPage',
                query: query,
                fullPath: that.$router.resolve({ name: 'hrefPage', query: query }).href,
                closable: false, // 首页
                meta: { keepAlive: true },
              }
            }
          }
        }
      },
      // 激活newRoute相关Tab页面
      activeRouteTab(newRoute, activePage = true){
        let that = this
        // 到root展示空白页
        if(!newRoute){
          that.$router.push({ name: 'root' }).catch(err => { err || true });
          return
        }
        // 到root的路由的处理
        let isToRoot = that._.get(newRoute, 'name') === 'root'
        if(isToRoot){
          newRoute = that.firstLeafMenuRoute(that.activeView)
          if(!newRoute){
            return
          }
        }
        // 到非root页面
        if (that.linkList.indexOf(newRoute.fullPath) < 0) {
          // 目标页面不存在
          that.linkList.push(newRoute.fullPath)
          that.pageList.push(that.menuTitleRoute(newRoute))
        } else if (that.linkList.indexOf(newRoute.fullPath) >= 0) {
          // 目标页面存在则pageList保持原样
        }
        if(activePage){
          let oldActivePage = that.activePage
          that.activePage = newRoute.fullPath
          if(oldActivePage === newRoute.fullPath){ // 此处防止 activePage 没有改变
            that.forceSwitchRoute(newRoute.fullPath)
          }
          // 查找顶层菜单
          let routeName = that._.get(newRoute, 'name')
          let menuId = that._.get(newRoute, 'query.mId')
          let pathMenus = findMenuPath(that.cptDynamicMenus, menuId || routeName)
          that.activeTopMenu = that._.get(pathMenus.find(item => item.pId === 0), 'id') || 0
        }
      },
      forceSwitchRoute(pageKey){
        let activeIndex = this.linkList.lastIndexOf(pageKey)
        let activeRouter = this.pageList[activeIndex]
        // 进行路由跳转(NavigationDuplicated)
        // https://github.com/vuejs/vue-router/issues/2872#issuecomment-519073998
        this.$router.push(Object.assign({},activeRouter)).catch(err => { err || true });
      },
      onTopMenuClick(topMenuId){
        let that = this
        that.activeTopMenu = topMenuId
      },
      /** 工具方法 */
      _recurseLeafMenus(pMenu, menuArray){
        let that = this
        let leafMenus = []
        let subMenus = menuArray.filter(item => item.parentId === pMenu.id)
        if(subMenus.length>0){
          // 有子菜单
          subMenus.forEach(subMenu => {
            let children = that._recurseLeafMenus(subMenu, menuArray)
            leafMenus.push(...children)
          })
        }else{
          // 没有子菜单
          leafMenus.push(pMenu)
        }
        return leafMenus
      },
    },
    // 初始化首个Tab页面: created | watch->activeView
    created() {
      let that = this
      if(that.activeView){
        // 获取首个bindRoute菜单并激活
        let newRoute = that.firstLeafMenuRoute(that.activeView)
        that.activeRouteTab(newRoute, false)
        that.activeRouteTab(that.$route)
      }else{
        // 监听activeView
        // 获取首个bindRoute菜单并激活
        // 见： watch.activeView
      }
      if(tools.isDev()){
        watermark.setWaterMark('Development', '开发版本')
      }
    },
    watch: {
      // 监听初始化其它Tab页面
      $route: function(newRoute) {
        this.activeRouteTab(newRoute)
      },
      // 初始化首个Tab页面: created | watch->activeView
      activeView: function(activeView){
        let that = this
        // 关闭全部Tab页面
        that.linkList = []
        that.pageList = []
        // 激活首个Leaf菜单Tab页面（Datav 大屏页已在 firstLeafMenuRoute 中跳过，不会作为首页自动打开）
        let newRoute = that.firstLeafMenuRoute(activeView)
        that.activeRouteTab(newRoute)
      },
      // 监听激活页面，改变路由
      activePage: function(pageKey) {
        this.forceSwitchRoute(pageKey)
      },
    },
  }
</script>

<style lang="scss">
  .desktop-layout-margin{
    flex: 1;
    margin: 12px;
    display: flex;
    flex-direction: column;
  }
  /* 定义过渡效果 */
  .page-transition-enter{
    opacity: 0;
  }
  .page-transition-enter-to{
    opacity: 0.7;
  }
  .page-transition-enter-active {
    transition: all 1s;
  }
  .page-transition-leave-active {
    transition: all 0s
  }
  .page-transition-leave{
    opacity: 0;
  }
  /* 调整Tabs样式 */
  .ant-tabs-nav-container {
    margin-top: 4px;
  }
  .tab-layout-tabs.ant-tabs {
    border-bottom: 1px solid #ccc;
    border-left: 1px solid #ccc;
    background-color: white;
    padding: 0 20px;
    .ant-tabs-bar {
      margin: 4px 0 0;
      border: none;
    }
  }
  .ant-tabs {
    &.ant-tabs-card .ant-tabs-tab {
      padding: 0 24px !important;
      background-color: white !important;
      margin-right: 10px !important;
      .ant-tabs-close-x {
        width: 12px !important;
        height: 12px !important;
        opacity: 0 !important;
        cursor: pointer !important;
        font-size: 12px !important;
        margin: 0 !important;
        position: absolute;
        top: 36%;
        right: 6px;
      }
      &:hover .ant-tabs-close-x {
        opacity: 1 !important;
      }
    }
  }
  .ant-tabs.ant-tabs-card > .ant-tabs-bar {
    .ant-tabs-tab {
      border: none !important;
      border-bottom: 1px solid transparent !important;
    }
    .ant-tabs-tab-active {
      border-color: #039267 !important;
    }
  }
</style>