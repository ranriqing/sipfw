import _ from '@/utils/tiny_lodash'
import router from '@/router'
import store from '@/store'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
const querystring = require('querystring');

import { stores, tools } from '@/utils/util'
import { GETTERS, ACTIONS } from '@/constants'
import { currentUser, ormMetas, getAction, clogin } from '@/api/api'
import settings from '@/settings'
import Modal from 'ant-design-vue/lib/modal';

import GlobalConfig from '@/store/models/GlobalConfig'
import GcsUser from '@/store/models/GcsUser'
import GcsGroup from '@/store/models/GcsGroup'
import MetaDomain from '@/store/models/MetaDomain'
import MetaDomainField from '@/store/models/MetaDomainField'
import MetaPermission from '@/store/models/MetaPermission'
import MetaView from '@/store/models/MetaView'
import MetaViewMenu from '@/store/models/MetaViewMenu'
import ScConfig from '@/store/models/ScConfig'
import ScElement from '@/store/models/ScElement'
import MetaPage from '@/store/models/MetaPage'
import MetaPageLink from '@/store/models/MetaPageLink'
import MetaPageComponent from '@/store/models/MetaPageComponent'
import MetaPageComponentField from '@/store/models/MetaPageComponentField'

let loginPath = '/auth/login'
let whiteList = [loginPath]
NProgress.configure({ showSpinner: false })
router.beforeEach(async(to, from, next) => {
  NProgress.start()
  /** 前置处理部分 */
  let initialUrl = store.getters[GETTERS.GET_APP_INITIAL_URL]
  if(!initialUrl){
    store.dispatch(ACTIONS.SET_APP_INITIAL_URL, document.URL)
  }
  /** 全局配置加载 */
  let globalConfigs = GlobalConfig.all()
  if(globalConfigs.length === 0){
    await GlobalConfig.$fetch().then(resp => {
      if(resp.length === 0){
        GlobalConfig.insertOrUpdate({
          data: [{id: 0, key: '0', value: '0'}]
        })
      }
    })
    /** 后端配置项写入 settings */
    getAction('/globalConfig/index', {}).then(resp => {
      settings.BACKEND_OPTIONS.sipAnalysisEnabled = _.get(resp, 'gmodel.sipAnalysisEnabled', false)
    })
  }

  /** 微信Code登录(场景: 微信内打开) */
  // 触发方式：${webRootUrl}?aPath=wechatClogin
  let queryObj = to.query
  let aPath = queryObj['aPath']            // 认证专用路径
  let vPath = queryObj['vPath']            // 其它vPath路径
  if(aPath === 'wechatClogin') {
    let isDev = tools.isDev()
    // ----- 测试账号 ----- //
    // dev-code-admin         超级管理员
    let authCode = queryObj['code'] || (isDev ? 'dev-code-admin' : null)
    if(!authCode){
      let webRootUrl = store.getters[GETTERS.GET_APP_WEBROOT_URL]
      let backUrl = `${webRootUrl}${to.path}?${querystring.encode(queryObj)}`
      let redirectUri = `${settings.WECHAT_REDIRECT_URI}?backUrl=${encodeURIComponent(backUrl)}`
      window.location.href = `https://open.weixin.qq.com/connect/oauth2/authorize?appid=${settings.WECHAT_APPID}&redirect_uri=${encodeURIComponent(redirectUri)}&response_type=code&scope=snsapi_userinfo&state=gcs#wechat_redirect`
      return
    }
    clogin({code: authCode}).then(resp=>{
      store.dispatch(ACTIONS.SET_USER_AUTH_TOKEN, resp.gmodel)
      if(vPath){
        // 落地页参数：透传 vPath/taskId
        let nextQuery = { vPath: vPath }
        if(queryObj['taskId']){ 
          nextQuery['taskId'] = queryObj['taskId'] 
        }
        next({ path: '/', query: nextQuery })
      }else{
        next({ path: '/', replace: true })
      }
    }).catch(err=>{
      console.log('Clogin failed', err)
      next({ path: loginPath, query:{reason:'clogin_failed'} })
    })
    return
  }
  /** 判断登录情况 */
  let accessToken = store.getters[GETTERS.CPT_USER_ACCESS_TOKEN]
  if(!accessToken){
    if(whiteList.indexOf(to.path) !== -1) {
      next() // 包含在whiteList中
    }else if(to.path.startsWith('/fm/')){
      next() // 允许访问表单填报页面
    }else if(to.path.startsWith('/share/')){
      next() // 允许访问外部分享预览页
    }else{
      // 跳转到登录页(此处to不可能是login)
      next({ path: loginPath, query: { redirect: to.fullPath } })
    }
    return
  }
  /** 加载用户信息 */
  let storeUser = store.getters[GETTERS.GET_USER_CURRENT_USER]
  if(!storeUser){
    await currentUser({}).then(resp => {
      storeUser = resp.gmodel
      let respExten = {
        ccExtenNum: storeUser['ccExtenNum'],
        ccAgentPaused: storeUser['ccAgentPaused'],
        ccApply95Outbound: storeUser['ccApply95Outbound'],
      }
      store.dispatch(ACTIONS.SET_USER_CURRENT_EXTEN, respExten)
      store.dispatch(ACTIONS.SET_USER_CURRENT_USER, resp.gmodel)
    }).catch(err => {
      console.log('CurrentUser failed', err)
      store.dispatch(ACTIONS.SET_USER_AUTH_TOKEN, null)
      next({ path: loginPath, query: { redirect: to.fullPath } })
      return
    })
  }
  /** 加载系统元信息 */
  let metaViews = MetaView.all()
  if(metaViews.length === 0){
    MetaView.$fetch().then(metaViews => {
      metaViews = metaViews.sort(function (s1, s2) { return s1['seqOrder'] - s2['seqOrder'] })
      ormMetas({}).then(resp => {
        let metaEntitys = resp.gmodel
        /* 基础信息 */
        GcsGroup.insertOrUpdate({
          data: _.get(metaEntitys, 'gcsGroup', [])
        })
        GcsUser.insertOrUpdate({
          data: _.get(metaEntitys, 'gcsUser', [])
        })
        MetaDomain.insertOrUpdate({
          data: _.get(metaEntitys, 'metaDomain', [])
        })
        MetaDomainField.insertOrUpdate({
          data: _.get(metaEntitys, 'metaDomainField', [])
        })
        MetaPermission.insertOrUpdate({
          data: _.get(metaEntitys, 'metaPermission', [])
        })
        ScConfig.insertOrUpdate({
          data: _.get(metaEntitys, 'scConfig', [])
        })
        ScElement.insertOrUpdate({
          data: _.get(metaEntitys, 'scElement', [])
        })
        /* 视图信息 */
        MetaView.insertOrUpdate({
          data: _.get(metaEntitys, 'metaView', [])
        })
        MetaViewMenu.insertOrUpdate({
          data: _.get(metaEntitys, 'metaViewMenu', [])
        })
        /* 页面信息 */
        MetaPage.insertOrUpdate({
          data: _.get(metaEntitys, 'metaPage', [])
        })
        MetaPageLink.insertOrUpdate({
          data: _.get(metaEntitys, 'metaPageLink', [])
        })
        MetaPageComponent.insertOrUpdate({
          data: _.get(metaEntitys, 'metaPageComponent', [])
        })
        MetaPageComponentField.insertOrUpdate({
          data: _.get(metaEntitys, 'metaPageComponentField', [])
        })
        /* NOTE: 激活视图(按MetaPage刷路由) */
        let activeView = null
        if(stores.isLabState()){
          activeView = metaViews.find(item => item.active);
          activeView = activeView?activeView:metaViews[0]
        }else{
          activeView = metaViews.find(item => item.id === storeUser.viewId)
        }
        if(activeView){
          store.dispatch(ACTIONS.SET_META_ACTIVE_VIEW, activeView).then(() => {
            let toPath = decodeURIComponent(from.query.redirect || to.fullPath)
            if(toPath === loginPath){
              next({ name: 'root' })
            }else{
              // 落地页参数: 使用toPath的toQueryObj，不使用queryObj解决
              let toUrl = new URL(toPath, window.location.origin);
              let toQueryObj = Object.fromEntries(toUrl.searchParams.entries());
              let vPath = toQueryObj['vPath']            // vPath路径
              let taskId = toQueryObj['taskId']          // 任务Id
              let routePage = MetaPage.query().where('metaViewId', activeView?.id)
                .where('pdent', vPath).orderBy('seqOrder', 'asc').orderBy('id', 'desc').first()
              if(routePage){
                next({ name: routePage['ident'], query: { taskId: taskId }})
              }else{
                next({ path: toPath })
              }
            }
          })
        }else{
          // 无绑定视图
          next({ name: 'root' })
        }
        /* 标记初始化状态 */
        store.dispatch(ACTIONS.SET_APP_INITIALIZED, true)
      }).catch(err => {
        console.log('Initialize failed', err)
        _initializeFailed()
      })
    }).catch(err => {
      console.log('MetaView failed', err)
    })
  }
  next()
})
router.afterEach((to) => {
  NProgress.done()
  document.title = to.matched.some(r => r.name === 'cloud')
    ? 'GCS云文档'
    : (process.env.VUE_APP_TITLE || 'GCS软件平台')
})

/** 工具方法 */
function _initializeFailed(){
  Modal.error({
    title: '初始化失败',
    content: '基础数据加载失败，刷新页面可以重新加载数据',
    okText: '刷新页面',
    mask: false,
    onOk: () => {
      window.location.reload()
    }
  })
}
