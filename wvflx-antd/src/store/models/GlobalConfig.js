import { Model } from '@vuex-orm/core'

export default class GlobalConfig extends Model {
  static entity = 'globalConfigs'

  static fields () {
    return {
      id: this.increment(),
      key: this.string('').nullable(),
      value: this.string('').nullable(),
      remark: this.string('').nullable(),
    }
  }

  static methodConf = {
    http: {
      url: '/globalConfig'
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
      // chenhb 调整
      $create: {
        name: 'create',
        http: {
          url: '/saveOrUpdate',
          method: 'post',
        },
      },
      $update: {
        name: 'update',
        http: {
          url: '/saveOrUpdate',
          method: 'post',
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
