import avue from '@/utils/avue'
import { STATES, MUTATIONS, ACTIONS, GETTERS, STORAGE } from '@/constants'

const app = {
  state: {
    /* 状态部分 */
    [STATES.APP_INITIALIZED]: false,          // Boolean               | 初始化完毕
    [STATES.APP_INITIAL_URL]: null,           // String                | 初始化Url
    [STATES.APP_LAB_STATE]: 'app',            // app | lab             | 应用Lab状态
    [STATES.APP_CACHE_POOL]: {},              // Object                | 缓存条目
    [STATES.APP_DEVICE_TYPE]: 'desktop',      // mobile   | desktop    | 设备类型(手机、桌面模式)
    [STATES.APP_WS_CONNECTED]: false,         // Boolean               | WebSocket连接
    [STATES.APP_SIDEBAR_OPENED]: true,        // Boolean               | 菜单展开(菜单展示图标+文字)
    /* Config部分 */
    [STATES.APP_CONFIG_MULTITAB]: true,       // Boolean               | 多页签模式
  },
  getters: {
    /* 状态部分 */
    [GETTERS.GET_APP_INITIALIZED]: state => {
      return state[STATES.APP_INITIALIZED]
    },
    [GETTERS.GET_APP_INITIAL_URL]: state => {
      return state[STATES.APP_INITIAL_URL]
    },
    [GETTERS.GET_APP_WEBROOT_URL]: state => {
      return state[STATES.APP_WEBROOT_URL]
    },
    [GETTERS.GET_APP_LAB_STATE]: state => {
      return state[STATES.APP_LAB_STATE]
    },
    [GETTERS.GET_APP_CACHE_POOL]: state => {
      return state[STATES.APP_CACHE_POOL]
    },
    [GETTERS.GET_APP_DEVICE_TYPE]: state => {
      return state[STATES.APP_DEVICE_TYPE]
    },
    [GETTERS.GET_APP_WS_CONNECTED]: state => {
      return state[STATES.APP_WS_CONNECTED]
    },
    [GETTERS.CPT_APP_DEVICE_DESKTOP]: state => {
      return state[STATES.APP_DEVICE_TYPE] === 'desktop'
    },
    [GETTERS.CPT_APP_DEVICE_MOBILE]: state => {
      return state[STATES.APP_DEVICE_TYPE] === 'mobile'
    },
    [GETTERS.GET_APP_SIDEBAR_OPENED]: state => {
      return state[STATES.APP_SIDEBAR_OPENED]
    },
    /* Config部分 */
    [GETTERS.GET_APP_CONFIG_MULTITAB]: state => {
      return state[STATES.APP_CONFIG_MULTITAB]
    },
  },
  mutations: {
    [MUTATIONS.SET_APP_INITIALIZED](state, stateVal){
      state[STATES.APP_INITIALIZED] = stateVal
    },
    [MUTATIONS.SET_APP_INITIAL_URL]: (state, stateVal) => {
      state[STATES.APP_INITIAL_URL] = stateVal
      // 同时初始化WebRootUrl
      let currentUrl = window.location.href
      let currentPath = window.document.location.pathname;
      let webBaseUrl = currentUrl.substring(0, currentUrl.indexOf(currentPath))
      let pathEndIndex = currentPath.indexOf('/', 1)
      if(pathEndIndex === -1){
        currentPath = `${currentPath}/`
        pathEndIndex = currentPath.length-1
      }
      let webRootPath = currentPath.substring(0, pathEndIndex)
      state[STATES.APP_WEBROOT_URL] = `${webBaseUrl}${webRootPath}`
    },
    [MUTATIONS.SET_APP_WEBROOT_URL]: (state, stateVal) => {
      state[STATES.APP_WEBROOT_URL] = stateVal
    },
    [MUTATIONS.SET_APP_LAB_STATE]: (state, stateVal) => {
      state[STATES.APP_LAB_STATE] = stateVal
      if(!stateVal){
        avue.lsRemove(STORAGE.LAB_STATE)
      }else{
        avue.lsSet(STORAGE.LAB_STATE, stateVal, 1)
      }
    },
    [MUTATIONS.SET_APP_CACHE_POOL]: (state, stateVal) => {
      state[STATES.APP_CACHE_POOL] = stateVal
      if(!stateVal){
        avue.lsRemove(STORAGE.CACHE_POOL)
      }else{
        avue.lsSet(STORAGE.CACHE_POOL, stateVal, 30)
      }
    },
    [MUTATIONS.SET_APP_DEVICE_TYPE](state, stateVal){
      state[STATES.APP_DEVICE_TYPE] = stateVal
    },
    [MUTATIONS.SET_APP_WS_CONNECTED](state, stateVal){
      state[STATES.APP_WS_CONNECTED] = stateVal
    },
    [MUTATIONS.SET_APP_SIDEBAR_OPENED](state, stateVal){
      state[STATES.APP_SIDEBAR_OPENED] = stateVal
    },
    [MUTATIONS.SET_APP_CONFIG_MULTITAB](state, stateVal){
      state[STATES.APP_CONFIG_MULTITAB] = stateVal
    },
  },
  actions: {
    [ACTIONS.SET_APP_INITIALIZED]: ({ commit }, actionVal) => {
      commit(MUTATIONS.SET_APP_INITIALIZED, actionVal)
    },
    [ACTIONS.SET_APP_INITIAL_URL]: ({ commit }, actionVal) => {
      commit(MUTATIONS.SET_APP_INITIAL_URL, actionVal)
    },
    [ACTIONS.SET_APP_WEBROOT_URL]: ({ commit }, actionVal) => {
      commit(MUTATIONS.SET_APP_WEBROOT_URL, actionVal)
    },
    [ACTIONS.SET_APP_LAB_STATE]: ({ commit }, actionVal) => {
      commit(MUTATIONS.SET_APP_LAB_STATE, actionVal)
    },
    [ACTIONS.SET_APP_DEVICE_TYPE]: ({ commit }, actionVal) => {
      commit(MUTATIONS.SET_APP_DEVICE_TYPE, actionVal)
    },
    [ACTIONS.SET_APP_WS_CONNECTED]: ({ commit }, actionVal) => {
      commit(MUTATIONS.SET_APP_WS_CONNECTED, actionVal)
    },
    [ACTIONS.SET_APP_SIDEBAR_OPENED]: ({ commit }, actionVal) => {
      commit(MUTATIONS.SET_APP_SIDEBAR_OPENED, actionVal)
    },
    [ACTIONS.SET_APP_CONFIG_MULTITAB]: ({ commit }, actionVal) => {
      commit(MUTATIONS.SET_APP_CONFIG_MULTITAB, actionVal)
    },
    [ACTIONS.SET_APP_CACHE_POOL]: ({ commit }, actionVal) => {
      commit(MUTATIONS.SET_APP_CACHE_POOL, actionVal)
    },
  },
}

export default app
