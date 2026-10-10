/** 精简打包依赖 */
import Vue from 'vue'
import Vuex from 'vuex'
import app from '@/store/modules/app'

// Use plugin
Vue.use(Vuex)

export default new Vuex.Store({
  plugins: [
  ],
  modules: {
    app,
  },
  state: { },
  mutations: { },
  actions: { },
})
