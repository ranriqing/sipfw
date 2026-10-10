const settings = require('@/settings')
import _ from '@/utils/tiny_lodash'
import Vue from 'vue'
import axios from 'axios'
import store from '@/store'
import { refreshToken } from '@/api/api'
import { GETTERS, ACTIONS } from '@/constants'
import Modal from 'ant-design-vue/lib/modal';
import Notification from 'ant-design-vue/lib/notification';

/** Axios实例 */
const axiosService = axios.create({
  baseURL: settings.BASE_URL,
  timeout: 60*1000,
})

/** Request拦截器 */
axiosService.interceptors.request.use(req => {
  // 扩展AccessToken
  let accessToken = store.getters[GETTERS.CPT_USER_ACCESS_TOKEN]
  if (accessToken) {
    req.headers[ 'Authorization' ] = accessToken
    req.headers[ 'X-Gcs-Username' ] = `${settings.BASE_URL}/${store.getters[GETTERS.CPT_USER_CURRENT_USERNAME] || 'none'}`
  }
  // 扩展_t参数(Get请求)
  if(req.method === 'get'){
    req.params = {
      _t: Date.parse(new Date())/1000,
      ...req.params
    }
  }
  return req
},(err) => {
  return Promise.reject(err)
})

/** Response拦截器 */
let isRefreshing = false                    // 防止并发刷新标记
let refreshedSubscribers = []               // 刷新Token回调数组
axiosService.interceptors.response.use(async resp => {
  let respData = resp.data
  if (resp.data instanceof Blob) {          // 返回Blob数据
    if(respData.type.toLowerCase().indexOf('json')!==-1){
      let parsedJson = await _readBlobAsJson(resp.data)
      // 内容为JSON(如.fdoc正文)，原样返回 Blob
      if(parsedJson && parsedJson.code !== undefined){
        respData = parsedJson
      }else{
        return resp.data
      }
    }else{
      return respData
    }
  }
  if (respData.code === '0') {              // 返回成功Code(SUCCESS)
    return respData
  }else{
    if(respData.code === '204') {           // 返回错误Code(AUTHORIZATION_AUTHC_REQUIRED)
      _loginExpired()
    }else if(respData.code !== '203') {     // 返回错误Code
      return _respCodeFailed(respData)
    }else{                                  // 返回过期Code(AUTHORIZATION_TOKEN_EXPIRE)
      // 刷新Token,回调过期请求
      if(!isRefreshing){
        isRefreshing = true
        let storeRefreshToken = store.getters[GETTERS.CPT_USER_REFRESH_TOKEN]
        refreshToken({'RefreshToken': storeRefreshToken || 'unknown'}).then(rRespData => {
          isRefreshing = false
          store.dispatch(ACTIONS.SET_USER_AUTH_TOKEN, rRespData.gmodel).then(() => {
            let accessToken = store.getters[GETTERS.CPT_USER_ACCESS_TOKEN]
            _onRrefreshed(accessToken)
          })
        }).catch((rErrData) => {
          if(rErrData.code === '201'){      // 刷新失败Code(AUTHORIZATION_FAILED)
            _loginExpired()
          }
          console.log(`RefreshToken failed[${rErrData.code}]`, rErrData)
        })
      }
      // 过期请求注册到回调数组
      // Note: https://gist.github.com/mkjiau/650013a99c341c9f23ca00ccb213db1c
      let retryOrigReq = resp.config
      return new Promise((resolve, reject) => {
        _subscribeRefreshed(accessToken => {
          retryOrigReq.url = retryOrigReq.url.replace(resp.config.baseURL,'') // 修正url
          retryOrigReq.headers['Authorization'] = accessToken
          axios(retryOrigReq).then( rResp => {
            let rRespData = rResp.data || {}
            if (rRespData instanceof Blob) {
              resolve(rRespData)
            }else if(rRespData.code === '0') {
              resolve(rRespData)
            }else{
              reject(_respCodeFailed(rRespData))
            }
          }).catch(rErr => {
            reject(_httpCodeFailed(rErr))
          })
        });
      });
    }
  }
}, (err) => {
  return _httpCodeFailed(err)
})

/** Token刷新工具方法 */
function _subscribeRefreshed(callback){
  refreshedSubscribers.push(callback);
}
function _onRrefreshed(accessToken){
  refreshedSubscribers.map(cb => cb(accessToken));
  refreshedSubscribers=[] // 清理Subscribers
}
async function _readBlobAsJson(file) {
  return await new Promise((resolve) => {
    let fileReader = new FileReader();
    fileReader.onload = () => {
      try {
        resolve(JSON.parse(fileReader.result))
      } catch (e) {
        resolve(null)   // 非合法 JSON，按内容 Blob 处理
      }
    };
    fileReader.onerror = () => resolve(null);
    fileReader.readAsText(file);
  });
}

/** Req失败工具方法 */
function _respCodeFailed(respData){
  respData.message = respData.message || 'Unknown'
  console.log(`Code failed[${respData.code}]`, JSON.stringify(respData))
  return Promise.reject(respData)
}
function _httpCodeFailed(err){
  let respStatus = _.get(err.response, 'status')
  let respData = _.get(err.response, 'data', {})
  console.log(`Http failed[${respStatus}]`, JSON.stringify(err))
  switch (respStatus) {
    case 400:
      Notification.warning({ message: '系统提示', description: '请求参数有误', duration: 4 })
      break
    case 401:
      Notification.warning({ message: '系统提示', description: '没有访问权限', duration: 4 })
      break
    case 403:
      Notification.warning({ message: '系统提示', description: '拒绝访问', duration: 4 })
      break
    case 404:
      Notification.warning({ message: '系统提示', description: '资源未找到', duration: 4 })
      break
    case 500:
      Notification.warning({ message: '系统提示', description: '服务器错误', duration: 4 })
      break
    case undefined:
      Notification.warning({ message: '网络错误', description: '请检查本地网络', duration: 4 })
      break
    default:
      Notification.warning({
        message: '系统提示',
        description: `[${respStatus}]${respData.message || 'Unknown'}`,
        duration: 4
      })
      break
  }
  return Promise.reject(Object.assign({hcode: respStatus?.toString()}, respData))
}

/** RefreshToken过期工具方法 */
function _loginExpired(){
  let timeoutId = setTimeout(() => {
    store.dispatch(ACTIONS.SET_USER_AUTH_TOKEN, null).then(() => {
      window.location.reload()
    })
  }, 2500);
  Modal.warn({
    title: '登录已过期',
    content: '登录已过期，请重新登录',
    okText: '重新登录',
    mask: false,
    onOk: () => {
      store.dispatch(ACTIONS.SET_USER_AUTH_TOKEN, null).then(() => {
        clearTimeout(timeoutId)
        window.location.reload()
      })
    }
  })
}

/** 绑定Axios实例 */
let VueAxios = {
  install(Vue, axiosInstance) {
    Vue.axios = axiosInstance;
    Object.defineProperties(Vue.prototype, {
      axios: {
        get: function get() {
          return axiosInstance;
        }
      },
      $http: {
        get: function get() {
          return axiosInstance;
        }
      }
    });
  }
};
Vue.use(VueAxios, axiosService)
export default axiosService
