import { Model } from '@vuex-orm/core'
import MetaView from './MetaView'
import MetaPage from './MetaPage'

export default class MetaViewMenu extends Model {
  static entity = 'metaViewMenus'

  static fields () {
    return {
      id: this.increment(),
      parentId: this.number(0),
      title: this.string('').nullable(),
      icon: this.string('').nullable(),
      seqOrder: this.number(100),
      bindHref: this.string('').nullable(),
      remark: this.string('').nullable(),
      metaViewId: this.number(0),
      relMetaView: this.belongsTo(MetaView, 'metaViewId'),
      relMetaPages: this.hasMany(MetaPage, 'metaViewMenuId'),
    }
  }

  static methodConf = {
    http: {
      url: '/orm/metaViewMenu'
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
