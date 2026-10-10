import MetaDomain from "@/store/models/MetaDomain";

const settings = require('@/settings')
import _ from '@/utils/tiny_lodash'
import store from '@/store'
import { GETTERS, ACTIONS } from '@/constants'
import GlobalConfig from '@/store/models/GlobalConfig'
import ScElement from '@/store/models/ScElement'
import GcsUser from '@/store/models/GcsUser'
import GcsGroup from '@/store/models/GcsGroup'

let utilObj = {
  /** 用户相关 */
  isSuperAdmin: function(){
    let currentUser = store.getters[GETTERS.GET_USER_CURRENT_USER]
    return currentUser && currentUser.superAdmin === true
  },
  currentUser: function(){
    return store.getters[GETTERS.GET_USER_CURRENT_USER]
  },
  userAuthToken: function(){
    return store.getters[GETTERS.CPT_USER_ACCESS_TOKEN]
  },
  activeView: function(){
    return store.getters[GETTERS.GET_META_ACTIVE_VIEW]
  },
  /** 权限相关 */
  hasPermission: function(metaDomainId, permission){
    let metaDomain = MetaDomain.find(metaDomainId)
    let permissionStr = `${_.get(metaDomain, 'domainName', 'none')}_${permission}`
    return utilObj.isPermitted(permissionStr)
  },
  isPermitted: function(permissionStr){
    // 无权限点/超管：默认允许
    if(utilObj.isSuperAdmin() || !permissionStr){
      return true
    }
    permissionStr = permissionStr.toLowerCase()
    let currentUser = utilObj.currentUser() || {}
    let permissions = _.get(currentUser, 'permissions', [])
    // permissions 不存在：默认允许
    if(!Array.isArray(permissions)){
      return true
    }
    // 规则：如果权限字符串对应的配置为 allowed 或不存在则允许，否则不允许
    let matched = permissions.find(p => _.get(p, 'permission')?.toLowerCase() === permissionStr)
    if(!matched){
      return true
    }
    return _.get(matched, 'value').toLowerCase() === 'allow'
  },
  /** Ws相关 */
  wsUserPath: function(subPath){
    let currentUser = utilObj.currentUser()
    let stripSubPath = subPath.startsWith('/')?subPath.substring(1):subPath
    return `/user/${currentUser['tenantId']}_${currentUser['username']}/${stripSubPath}`
  },
  wsTopicPath: function(subPath){
    let currentUser = utilObj.currentUser()
    let stripSubPath = subPath.startsWith('/')?subPath.substring(1):subPath
    return `/topic/${currentUser['tenantId']}_${stripSubPath}`
  },
  /** 设备模式 */
  isMobile: function(){
    return store.getters[GETTERS.CPT_APP_DEVICE_MOBILE]
  },
  isDesktop: function(){
    return store.getters[GETTERS.CPT_APP_DEVICE_DESKTOP]
  },
  /** Lab模式 */
  isStoreLab: function(){
    return store.getters[GETTERS.GET_APP_LAB_STATE] === 'lab'
  },
  isLabState: function(){
    return utilObj.isStoreLab() && utilObj.isSuperAdmin() && utilObj.isDesktop()
  },
  labComponent: function(component){
    let mobilePartTable = (component === 'PartTable') && utilObj.isMobile()
    return utilObj.isLabState()?`Lab${component}`:mobilePartTable?'AppPartList':`App${component}`
  },
  /** 全局配置 */
  globalConfig: function(itemKey, defaultValue){
    let dftConfig = settings.DFT_GLOBAL_CONFIG
    let item = GlobalConfig.query().where('key', itemKey).orderBy('id', 'desc').first()
    defaultValue = _.defaultTo(defaultValue, _.get(dftConfig, itemKey))
    return _.defaultTo(_.get(item, 'value'), defaultValue)
  },
  scElement(scKey, scElementId ){
    let scQuery = ScElement.query()
      .where(item => item.scConfigKey === scKey)
    scQuery.where(item => item.id == scElementId)
    return scQuery.orderBy('seqOrder').orderBy('id').first() || {}
  },
  gcsUser(userId) {
    return GcsUser.find(userId)
  },
  /** 全局缓存 */
  setCacheItem: function(itemKey, itemValue){
    let cachePool = _.defaultTo(store.getters[GETTERS.GET_APP_CACHE_POOL], {})
    cachePool[itemKey] = itemValue
    return store.dispatch(ACTIONS.SET_APP_CACHE_POOL, _.cloneDeep(cachePool))
  },
  getCacheItem: function(itemKey, defaultValue){
    let cachePool = _.defaultTo(store.getters[GETTERS.GET_APP_CACHE_POOL], {})
    return _.cloneDeep(_.defaultTo(cachePool[itemKey], defaultValue))
  },
  cleanCacheByPrefix: function(itemKeyPrefix){
    let cachePool = _.defaultTo(store.getters[GETTERS.GET_APP_CACHE_POOL], {})
    _.forIn(cachePool, (value, key)=>{
      if(key.startsWith(itemKeyPrefix)){
        cachePool = _.omit(cachePool, [key])
      }
    })
    store.dispatch(ACTIONS.SET_APP_CACHE_POOL, _.cloneDeep(cachePool))
  },
  levelGcsGroup(groupId){
    let levelNames = []
    let levelGroup = GcsGroup.find(groupId)
    while(levelGroup){
      if(levelGroup['parentId'] === -1){
        levelNames.unshift(levelGroup['name'])
      }else if(levelGroup['id'] !== -1 || levelGroup['id'] === groupId){
        levelNames.unshift(levelGroup['name'])
      }
      levelGroup = GcsGroup.find(levelGroup['parentId'])
    }
    return levelNames.join('/')
  },
  /** 构建部门树 */
  _buildGroupTree(pGroup, allGroups){
    let that = this
    if(!pGroup){ return null }
    let treeNode = {id: pGroup.id, pId: pGroup.parentId, title: pGroup.name, icon: pGroup.icon, sequence: pGroup.sequence}
    let allSubGroups = that._recurseSubGroups(pGroup, allGroups, true)
    let allSubGroupIds = allSubGroups.map(item => item.id)
    let allSubGroupUserQuery = GcsUser.query().where(item => allSubGroupIds.indexOf(item.gcsGroupId) !== -1)
    treeNode['count'] = allSubGroupUserQuery.count()
    let subGroups = allGroups.filter(item => item.parentId === pGroup.id)
    if(subGroups.length>0){
      treeNode['children'] = subGroups.map(item => {
        return that._buildGroupTree(item, allGroups)
      })
    }
    return treeNode
  },
  _recurseSubGroups(group, allGroups, includeSelf){
    let that = this
    let subGroups = allGroups.filter(item => item.parentId === group.id)
    subGroups.forEach(subGroup => {
      let children = that._recurseSubGroups(subGroup, allGroups, false)
      subGroups.push(...children)
    })
    includeSelf && subGroups.unshift(group)
    return subGroups
  },
}

export default utilObj