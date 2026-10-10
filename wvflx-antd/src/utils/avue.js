const settings = require('@/settings')
import Vue from 'vue'

// avue - app vue
let utilObj = {
  /** 当前APP存储 */
  lsGet: function(key){
    return Vue.ls.get(`${settings.APP_IDENT}_${key}`)
  },
  lsSet: function(key, value, expireDays){
    expireDays = expireDays || 1  // 1 days
    Vue.ls.set(`${settings.APP_IDENT}_${key}`, value, 60 * 60 * 1000 * 24 * expireDays)
  },
  lsRemove: function(key){
    Vue.ls.remove(`${settings.APP_IDENT}_${key}`)
  },
  /** 指定APP存储 */
  appLsGet: function(appIdent, key){
    return Vue.ls.get(`${appIdent}_${key}`)
  },
  appLsSet: function(appIdent, key, value, expireDays){
    expireDays = expireDays || 1  // 1 days
    Vue.ls.set(`${appIdent}_${key}`, value, 60 * 60 * 1000 * 24 * expireDays)
  },
  appLsRemove: function(appIdent, key){
    Vue.ls.remove(`${appIdent}_${key}`)
  },
}

export default utilObj