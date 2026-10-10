import { Model } from '@vuex-orm/core'

export default class GcsGroup extends Model {
  static entity = 'gcsGroups'

  static fields () {
    return {
      id: this.increment(),
      name: this.string('').nullable(),
      icon: this.string('').nullable(),
      sequence: this.number(100),
      remark: this.string('').nullable(),
      parentId: this.number(0),
      dateCreated: this.string('').nullable(),
      lastUpdated: this.string('').nullable(),
    }
  }

  static methodConf = {
    http: {
      url: '/gcsGroup'
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
