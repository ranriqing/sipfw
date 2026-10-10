const settings = require('@/settings')
import Vue from 'vue'
import Storage from 'vue-ls'
import Router  from 'vue-router'

import Login from '@/views_base/auth/Slim'

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
  routes: [
    {
      name: 'login',
      path: '/slim/login',
      meta: {
        title: '系统登录',
      },
      component: Login
    },
  ]
})