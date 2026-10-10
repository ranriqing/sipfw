import _ from '@/utils/tiny_lodash'
import settings from '@/settings'

// 取某菜单在当前视图下的最新页面（seqOrder 升序、id 降序取第一条）
export function latestMenuPage(menuPages, viewId) {
  if (!menuPages) {
    return null
  }
  const mappedItems = menuPages
    .filter(item => item.metaViewId === viewId)
    .map(item => ({ id: item.id, ident: item.ident, pdent: item.pdent, seqOrder: item.seqOrder }))
    .sort((a, b) => {
      if (a.seqOrder - b.seqOrder !== 0) {
        return a.seqOrder - b.seqOrder
      } else {
        return b.id - a.id
      }
    })
  return mappedItems ? mappedItems[0] : null
}

// 构建单个菜单节点（Datav 在新窗口打开）
export function buildMenuNode(item, latestPage) {
  const webPath = settings.WEB_PATH
  if (latestPage && latestPage.pdent && latestPage.pdent.startsWith('Datav')) {
    return {
      id: item.id,
      pId: item.parentId,
      key: `menu${item.id}`,
      title: item.title,
      icon: item.icon,
      bindRoute: undefined,
      bindHref: `${webPath}/b/${latestPage.ident}`,
    }
  }
  return {
    id: item.id,
    pId: item.parentId,
    key: `menu${item.id}`,
    title: item.title,
    icon: item.icon,
    bindRoute: latestPage?.ident,
    bindHref: item.bindHref,
  }
}

// 递归为父节点挂载 children（无子节点置 children: null）
export function attachChildren(pMenus, menuArray, viewId) {
  pMenus.forEach(pMenu => {
    const children = []
    menuArray.forEach(item => {
      if (item.parentId === pMenu.id) {
        children.push(buildMenuNode(item, latestMenuPage(_.get(item, 'relMetaPages'), viewId)))
      }
    })
    pMenu.children = children.length > 0 ? children : null
    if (children.length > 0) {
      attachChildren(children, menuArray, viewId)
    }
  })
}

// 构建菜单树；rootFilter(item) 为可选顶层节点筛选
export function buildMenuTree(menuArray, viewId, { rootFilter } = {}) {
  const rootMenus = []
  menuArray.forEach(item => {
    if (item.parentId === 0 && (!rootFilter || rootFilter(item))) {
      rootMenus.push(buildMenuNode(item, latestMenuPage(_.get(item, 'relMetaPages'), viewId)))
    }
  })
  attachChildren(rootMenus, menuArray, viewId)
  return rootMenus
}

// 按菜单 id 或 bindRoute 查找其路径
export function findMenuPath(menus, menuIdOrRouteName) {
  let matchedItem = null
  let path = []
  for (let i = 0; matchedItem == null && i < menus.length; i++) {
    path = []
    matchedItem = searchRecurse(menus[i], menuIdOrRouteName, path)
  }
  return matchedItem ? path : []
}

function searchRecurse(menuItem, menuIdOrRouteName, path) {
  if (`${menuItem.id}` === `${menuIdOrRouteName}` || menuItem.bindRoute === menuIdOrRouteName) {
    path.push(menuItem)
    return menuItem
  } else if (menuItem.children) {
    path.push(menuItem)
    let childItem = null
    for (let i = 0; childItem == null && i < menuItem.children.length; i++) {
      childItem = searchRecurse(menuItem.children[i], menuIdOrRouteName, path)
    }
    return childItem
  }
  return null
}
