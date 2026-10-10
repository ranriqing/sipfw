import router from '@/router/slim'
import store from '@/store/slim'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
const settings = require('@/settings')
const querystring = require('querystring');
import { GETTERS, ACTIONS } from '@/constants'
import { postAction } from '@/api/api_slim'

NProgress.configure({ showSpinner: false })
router.beforeEach(async(to, from, next) => {
  NProgress.start()
  /** 前置处理部分 */
  let initalUrl = store.getters[GETTERS.GET_APP_INITIAL_URL]
  if(!initalUrl){
    store.dispatch(ACTIONS.SET_APP_INITIAL_URL, document.URL)
  }
  let webRootUrl = store.getters[GETTERS.GET_APP_WEBROOT_URL]
  let queryObj = to.query
  /** 问卷调查逻辑 */
  if (to.name==='fmIndex' && to.meta.title) {
    document.title = to.meta.title
    let isDev = 'development' === process.env.NODE_ENV && true
    let formUrl = `${webRootUrl}${to.path}`
    formUrl = formUrl.endsWith('/')?formUrl.slice(0, -1):formUrl
    if(queryObj['vPath'] === 'wechatClogin') {
      // 获取code
      let authCode = queryObj['code'] || (isDev?'dev-code':null)
      if(!authCode){
        let authBackUrl = `${formUrl}/?${querystring.encode(queryObj)}`
        let redirectUri = `${settings.WECHAT_REDIRECT_URI}?backUrl=${encodeURIComponent(authBackUrl)}`
        window.location.href = `https://open.weixin.qq.com/connect/oauth2/authorize?appid=${settings.WECHAT_APPID}&redirect_uri=${encodeURIComponent(redirectUri)}&response_type=code&scope=snsapi_userinfo&state=gcs#wechat_redirect`
        return
      }
      // 换取openId
      postAction('/auth/cUserInfo', {code: authCode}).then(resp=>{
        let wxUserInfo = resp.gmodel['userInfo']
        next({ path: to.path, query: {oid: wxUserInfo['openId'] || 'invalid'} })
      }).catch(err => {
        next({ path: to.path, query: {oid: 'invalid'} })
        console.log('CUserInfo failed', err)
      })
      // window.location.href = `${formUrl}/?oid=${openId}`
      return
    }
    // 校验OpenID参数
    if(!queryObj['oid']){
      queryObj['vPath'] = 'wechatClogin'
      next({ path: to.path, query: queryObj })
      // window.location.href = `${formUrl}/?${querystring.encode(queryObj)}`
    }else{
      next()
    }
    return
  }
  next()
})
router.afterEach(() => {
  NProgress.done()
})
