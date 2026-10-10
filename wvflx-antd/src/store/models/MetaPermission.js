import { Model } from '@vuex-orm/core'

export default class MetaPermission extends Model {
  static entity = 'metaPermissions'

  static fields () {
    return {
      id: this.increment(),
      title: this.string('').nullable(),
      icon: this.string('').nullable(),
      seqOrder: this.number(100),
      type: this.string('').nullable(),
      permission: this.string('').nullable(),
      extend: this.string('').nullable(),         // JSON string
      gcsRoleId: this.number(null).nullable(),
      dateCreated: this.string('').nullable(),
      lastUpdated: this.string('').nullable(),
    }
  }

  static methodConf = {
    http: {
      url: '/metaPermission'
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

