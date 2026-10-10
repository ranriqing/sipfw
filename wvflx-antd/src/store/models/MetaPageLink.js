import { Model } from '@vuex-orm/core'
import MetaPage from '@/store/models/MetaPage'
import MetaPageComponent from '@/store/models/MetaPageComponent'

export default class MetaPageLink extends Model {
  static entity = 'metaPageLinks'

  static fields () {
    return {
      id: this.increment(),
      seqOrder: this.number(100),
      metaPageId: this.number(0),
      relMetaPage: this.belongsTo(MetaPage, 'metaPageId'),
      metaPageComponentId: this.number(0),
      relMetaPageComponent: this.belongsTo(MetaPageComponent, 'metaPageComponentId'),
    }
  }

  static methodConf = {
    http: {
      url: '/orm/metaPageLink'
    },
    methods: {
      $fetch: {
        name: 'fetch',
        http: {
          url: '/index',
          method: 'get',
        },
      },
      $get: {
        name: 'get',
        http: {
          url: '/show/:id',
          method: 'get',
        },
      },
      $create: {
        name: 'create',
        http: {
          url: '/save',
          method: 'post',
        },
      },
      $update: {
        name: 'update',
        http: {
          url: '/update/:id',
          method: 'put',
        },
      },
      $delete: {
        name: 'delete',
        http: {
          url: '/delete/:id',
          method: 'delete',
        },
      },
    },
  }
}
