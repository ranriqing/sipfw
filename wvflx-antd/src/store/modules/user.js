import avue from '@/utils/avue'
import { STATES, MUTATIONS, ACTIONS, GETTERS, STORAGE } from '@/constants'

const user = {
  state: {
    [STATES.USER_AUTH_TOKEN]: null,              // Object   | 认证的Token  {access_token:'xxx', expires_in:7200, refresh_token:'yyy'}
    [STATES.USER_CURRENT_USER]: null,            // Object   | 用户信息     {tenantId: 'tenant01', username: 'admin', nickname: 'Admin'}
    [STATES.USER_CURRENT_EXTEN]: null,           // Object   | 分机信息     {ccExtenNum: '8001', ccAgentPaused: false}
  },
  getters: {
    /* Token部分 */
    [GETTERS.GET_USER_AUTH_TOKEN]: state => {
      return state[STATES.USER_AUTH_TOKEN]
    },
    [GETTERS.CPT_USER_ACCESS_TOKEN]: state => {
      let authToken = state[STATES.USER_AUTH_TOKEN]
      return authToken?authToken['access_token']:null
    },
    [GETTERS.CPT_USER_REFRESH_TOKEN]: state => {
      let authToken = state[STATES.USER_AUTH_TOKEN]
      return authToken?authToken['refresh_token']:null
    },
    /* 当前用户 */
    [GETTERS.GET_USER_CURRENT_USER]: state => {
      return state[STATES.USER_CURRENT_USER]
    },
    [GETTERS.CPT_USER_CURRENT_USERNAME]: state => {
      let currentUser = state[STATES.USER_CURRENT_USER]
      return currentUser?currentUser['username']:null
    },
    /* 当前分机 */
    [GETTERS.GET_USER_CURRENT_EXTEN]: state => {
      return state[STATES.USER_CURRENT_EXTEN]
    },
  },
  mutations: {
    [MUTATIONS.SET_USER_AUTH_TOKEN]: (state, stateVal) => {
      state[STATES.USER_AUTH_TOKEN] = stateVal
      if(!stateVal){
        avue.lsRemove(STORAGE.AUTH_TOKEN)
      }else{
        avue.lsSet(STORAGE.AUTH_TOKEN, stateVal, 30)
      }
    },
    [MUTATIONS.SET_USER_CURRENT_USER]: (state, stateVal) => {
      state[STATES.USER_CURRENT_USER] = stateVal
    },
    [MUTATIONS.SET_USER_CURRENT_EXTEN]: (state, stateVal) => {
      state[STATES.USER_CURRENT_EXTEN] = stateVal
    },
  },
  actions: {
    [ACTIONS.SET_USER_AUTH_TOKEN]: ({ commit }, actionVal) => {
      commit(MUTATIONS.SET_USER_AUTH_TOKEN, actionVal)
    },
    [ACTIONS.SET_USER_CURRENT_USER]: ({ commit }, actionVal) => {
      let extenVal = {
        ccExtenNum: actionVal['ccExtenNum'],
        ccAgentPaused: actionVal['ccAgentPaused'],
        ccApply95Outbound: actionVal['ccApply95Outbound'],
      }
      commit(MUTATIONS.SET_USER_CURRENT_EXTEN, extenVal)
      commit(MUTATIONS.SET_USER_CURRENT_USER, actionVal)
    },
    [ACTIONS.SET_USER_CURRENT_EXTEN]: ({ commit }, actionVal) => {
      commit(MUTATIONS.SET_USER_CURRENT_EXTEN, actionVal)
    },
  },
}

export default user
