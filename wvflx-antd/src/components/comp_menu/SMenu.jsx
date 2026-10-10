import _ from '@/utils/tiny_lodash'
import Menu from 'ant-design-vue/es/menu'
import Icon from 'ant-design-vue/es/icon'
import { stores } from '@/utils/util'
import { findMenuPath } from '@/utils/util_menu'

const { Item, SubMenu } = Menu

/**
 # 组件说明
 菜单功能基础组件，根据menus数组来构建菜单
 通过监听路由变化，动态展开菜单和选中条目
 # 配置组件
 menus: [{
    key: 'menu',
    title: '菜单管理',
    icon: 'edit',
    bindRoute: '',
    children: [
      {
        key: 'menu01',
        title: '动态菜单',
        bindRoute: 'menuEdit',
      },
      {
        key: 'menu02',
        title: '外部链接',
        target: 'http://www.baidu.com',
      },
    ]
  }],
 mode: String            // 同Menu
 # 操作方法
 $emit('select', obj)    // 被选中调用
 */

export default {
  name: 'SMenu',
  props: {
    menus: {              // See notes
      type: Array,
      required: true
    },
    mode: {
      type: String,       // vertical | vertical-right | horizontal | inline
      required: false,
      default: 'inline'
    },
    theme: {
      type: String,       // vertical | vertical-right | horizontal | inline
      required: false,
      default: 'light'
    }
  },
  watch: {
    $route: function() {
      this.updateMenu()
    }
  },
  data() {
    return {
      openKeys: [],       // 子组件中openKeys状态
      selectedKeys: []   // 选中selectedKeys状态
    }
  },
  computed: {
    // 顶层Menu的keys
    topMenuKeys: function(vm) {
      const keys = []
      vm.menus.forEach(menuItem => keys.push(menuItem.key))
      return keys
    }
  },


  methods: {
    /** 事件监听 */
    // SubMenu 展开/关闭的回调
    onOpenChange(openKeys) {
      let that = this
      if (that.mode === 'horizontal') {
        that.openKeys = openKeys
        return
      }
      // 新展开的菜单key
      let openKey = openKeys.find(key => !that.openKeys.includes(key))
      if (!that.topMenuKeys.includes(openKey)) {  // 新展开的为子菜单
        that.openKeys = openKeys                  // 打开多级菜单
      } else {                                    // 新展开的为顶层菜单
        that.openKeys = openKey ? [openKey] : []  // 只打开顶层菜单
      }
    },
    // $Route变化回调
    updateMenu() {
      let routeName = this.$route.name
      let menuId = this.$route.query.mId
      let pathMenus = findMenuPath(this.menus, menuId || routeName)
      // 按路由选中菜单
      let leafMenu = pathMenus.pop()
      if (leafMenu) {
        this.selectedKeys = [leafMenu.key]
      } else {
        this.selectedKeys = []
      }
      // 按路由展开菜单
      let openKeys = []
      pathMenus.forEach(item => {
        openKeys.push(item.key)
      })
      this.openKeys = openKeys
    },

    /** 菜单渲染 */
    // 渲染菜单树
    renderItem(menuItem) {
      if (!menuItem.hidden) {
        return menuItem.children && menuItem.children.length > 0 ?
          this.renderSubMenu(menuItem) : this.renderMenuItem(menuItem)
      }
      return null
    },
    // 渲染SubMenu节点
    renderSubMenu(menuItem) {
      const childItemArr = []
      menuItem.children.forEach(childItem => childItemArr.push(this.renderItem(childItem)))
      return (
        <SubMenu {...{ key: menuItem.key }}>
          <span slot="title">
            {this.renderIcon(menuItem.icon)}
            <span>
              {menuItem.title}
            </span>
          </span>
          {childItemArr}
        </SubMenu>
      )
    },
    // 渲染叶子节点
    renderMenuItem(menuItem) {
      let tag = 'router-link'
      let props = {}
      let attrs = {}
      if (menuItem.bindHref) {
        if (stores.isLabState()) {
          // lab 模式：跳 HrefPage 展示链接，点击新窗口打开
          props = { to: { name: 'hrefPage', query: { mId: menuItem.id, href: menuItem.bindHref } } }
        } else {
          // 非 lab：新窗口打开
          tag = 'a'
          attrs = { href: menuItem.bindHref, target: '_blank' }
        }
      } else if (_.isEmpty(menuItem.bindRoute)) {
        props = { to: { name: 'entryTmpl', query: { mId: menuItem.id } } }
      } else {
        props = { to: { name: menuItem.bindRoute } }
      }
      // 渲染节点
      return (
        <Item {...{ key: menuItem.key }}>
          <tag {...{ props, attrs }}>
            {this.renderIcon(menuItem.icon)}
            <span>{menuItem.title}</span>
          </tag>
        </Item>
      )
    },
    // 渲染菜单图标
    renderIcon(icon) {
      if (!icon) {
        return null
      }
      const props = {}
      typeof (icon) === 'object' ? props.component = icon : props.type = icon
      return (
        <Icon {...{ props }}/>
      )
    }
  },
  render() {
    const { mode, menus } = this
    // 菜单Props
    const props = {
      theme: this.theme,
      mode: mode,             // 菜单类型：vertical vertical-right horizontal inline
      openKeys: this.openKeys // 当前展开的 SubMenu 菜单项 key 数组
    }
    // 菜单事件
    const on = {
      click: obj => {         // 被选中时调用：{ item, key, keyPath }
        this.$emit('select', obj)
      },
      openChange: this.onOpenChange  // SubMenu 展开/关闭的回调
    }
    // 递归渲染
    const menuTree = menus.map(item => {
      return this.renderItem(item)
    })
    // 渲染菜单
    return (
      <Menu vModel={this.selectedKeys} {...{ props, on: on }}>
        {menuTree}
      </Menu>
    )
  },
  mounted() {
    this.updateMenu()
  }
}
