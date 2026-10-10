const settings = require('@/settings')
import Vue from 'vue'
import Vuex from 'vuex'
import ormApi from '@/api/orm_api'
import VuexORM from '@vuex-orm/core'
import VuexORMAxios from '@vuex-orm/plugin-axios'
import meta from '@/store/modules/meta'
import app from '@/store/modules/app'
import user from '@/store/modules/user'

import GlobalConfig from './models/GlobalConfig'
import GcsGroup from './models/GcsGroup'
import GcsUser from './models/GcsUser'
import MetaDomain from './models/MetaDomain'
import MetaDomainField from './models/MetaDomainField'
import MetaView from './models/MetaView'
import MetaViewMenu from './models/MetaViewMenu'
import ScConfig from './models/ScConfig'
import ScElement from './models/ScElement'
import MetaPage from './models/MetaPage'
import MetaPageLink from './models/MetaPageLink'
import MetaPageComponent from './models/MetaPageComponent'
import MetaPageComponentField from './models/MetaPageComponentField'

import MetaPermission from './models/MetaPermission'

// Use plugin
Vue.use(Vuex)

// 注册Models
let database = new VuexORM.Database()
database.register(GlobalConfig)

database.register(GcsGroup)
database.register(GcsUser)

database.register(MetaDomain)
database.register(MetaDomainField)
database.register(MetaView)
database.register(MetaViewMenu)

database.register(ScConfig)
database.register(ScElement)

database.register(MetaPage)
database.register(MetaPageLink)
database.register(MetaPageComponent)
database.register(MetaPageComponentField)

database.register(MetaPermission)

VuexORM.use(VuexORMAxios, {
  database,
  http: {
    baseURL: settings.BASE_URL,
    headers: {
      'Accept': 'application/json',
      'Content-Type': 'application/json'
    },
    axios: ormApi,
    onResponse: function(resp, instance) {
      instance || true
      return resp.data.gmodel;
    },
  }
})

export default new Vuex.Store({
  plugins: [
    VuexORM.install(database)
  ],
  modules: {
    app,
    user,
    meta,
  },
  state: { },
  mutations: { },
  actions: { },
})
