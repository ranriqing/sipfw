import routerConfig from '@/router/router.config'
import MetaPage from '@/store/models/MetaPage'
import { stores } from '@/utils/util'

/** 递归收集路由 name(含 children) */
function collectRouteNames(routes, names) {
  (routes || []).forEach(route => {
    if (route.name) {
      names.push(route.name)
    }
    if (route.children && route.children.length) {
      collectRouteNames(route.children, names)
    }
  })
  return names
}

let staticRouteNames = null

/** 从 router.config.js 提取全部固定 routeName(缓存) */
function getStaticRouteNames() {
  if (!staticRouteNames) {
    staticRouteNames = collectRouteNames(routerConfig.staticRouters(), [])
  }
  return staticRouteNames
}

/**
 * 解析跳转目标为路由 name
 * 支持: 固定routeName(router.config.js) / MetaPage.ident / MetaPage.pdent
 * @param {String} target 跳转目标(routeName 或 routePdent)
 * @returns {String|null} 解析到的路由 name
 */
export function resolveRouteName(target) {
  if (!target) {
    return null
  }
  // 1. 固定routeName(router.config.js)
  if (getStaticRouteNames().includes(target)) {
    return target
  }
  // 2. 动态页面: MetaPage.ident(routeName) 或 MetaPage.pdent
  let activeView = stores.activeView()
  let routePage = MetaPage.query()
    .where('metaViewId', activeView?.id)
    .where(item => item.ident === target || item.pdent === target)
    .orderBy('seqOrder', 'asc').orderBy('id', 'desc').first()
  if (routePage) {
    return routePage['ident']
  }
  return null
}
