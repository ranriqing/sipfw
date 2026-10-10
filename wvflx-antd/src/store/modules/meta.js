import _ from '@/utils/tiny_lodash'
import Router from 'vue-router'
import router from '@/router'
import routerConfig from '@/router/router.config'
import MetaPage from '@/store/models/MetaPage'
import { createEnhancedRouter  } from '@/utils/EnhancedRouter'
import { STATES, MUTATIONS, ACTIONS, GETTERS } from '@/constants'
import pageMapper from '@/pages/pageMapper'

const meta = {
  state: {
    [STATES.META_ACTIVE_VIEW]: null,   // Object    |  激活模板 {id: 1, name: '管理模板', icon: 'team' }
  },
  getters: {
    [GETTERS.GET_META_ACTIVE_VIEW]: state => {
      return state[STATES.META_ACTIVE_VIEW]
    },
  },
  mutations: {
    [MUTATIONS.SET_META_ACTIVE_VIEW](state, stateVal){
      state[STATES.META_ACTIVE_VIEW] = stateVal
    },
  },
  actions: {
    [ACTIONS.SET_META_ACTIVE_VIEW]: ({ commit }, actionVal) => {
      commit(MUTATIONS.SET_META_ACTIVE_VIEW, actionVal)
      // Lab状态判断
      let rootRoutes = []
      let blankRoutes = []
      let activePages = MetaPage.query().where('metaViewId', actionVal.id).get()
      _.uniqBy(activePages, 'ident').map(item => {
        let tmpRoute = {
          pdent: item['pdent'],
          name: item['ident'],
          path: `/${item['ident']}`,
          meta: {
            title: item['title'],
          },
        }
        // 兼容历史页面路径 && 支持短路径映射长路径
        let appMapperPage = _.get(pageMapper.appMapper, item['component'])
        if(appMapperPage){
          tmpRoute['component'] = () => import('@/views_app/' + appMapperPage)
        }
        if(tmpRoute['pdent'] && tmpRoute['pdent'].startsWith('Datav')){
          blankRoutes.push(tmpRoute)
        }else{
          rootRoutes.push(tmpRoute)
        }
        return tmpRoute
      })
      // ActiveRoutes激活
      let createdRouter = new Router({
        mode: 'history',
        routes: routerConfig.staticRouters()
      })
      let routerHelpers = createEnhancedRouter(createdRouter)
      routerHelpers.addRoutes(rootRoutes,'/')
      routerHelpers.addRoutes(blankRoutes,'/b')
      router.matcher = createdRouter.matcher
      router.options = createdRouter.options
    },
  },
}

export default meta
