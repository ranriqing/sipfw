const settings = require('@/settings')
import Vue from 'vue'
import Storage from 'vue-ls'
import Router  from 'vue-router'
import routerConfig from '@/router/router.config'

// Use plugin
Vue.use(Router)
Vue.use(Storage, {
  name: 'ls',                   // Vue.ls or this.$ls
  namespace: 'app__',           // Storage prefix
  storage: 'local',             // Storage type: ['session', 'local', 'memory']
})

export default new Router({
  mode: 'history',
  base: settings.WEB_PATH,
  scrollBehavior: () => ({ y: 0 }),
  routes: routerConfig.staticRouters()
})