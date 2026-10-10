import { Model } from '@vuex-orm/core'

export default class MetaView extends Model {
  static entity = 'metaViews'

  static fields () {
    return {
      id: this.increment(),
      type: this.number(1),                 // 0 开放视图  1 超管视图
      name: this.string('').nullable(),
      icon: this.string('').nullable(),
      seqOrder: this.number(100),           // 视图排序
      active: this.boolean(false),          // 默认未激活
    }
  }

  static beforeDelete (model) {
    model || true
    return true
  }

  static methodConf = {
    http: {
      url: '/orm/metaView'
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
