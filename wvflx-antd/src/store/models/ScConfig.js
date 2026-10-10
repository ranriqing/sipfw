import { Model } from '@vuex-orm/core'

export default class ScConfig extends Model {
  static entity = 'scConfigs'

  static fields () {
    return {
      id: this.increment(),
      key: this.string('').nullable(),
      title: this.string('').nullable(),
      seqOrder: this.number(100),
      sciScCategory: this.number(0),
      multiLevel: this.boolean(false),
      allowModify: this.boolean(true),
      freeCreation: this.boolean(false),
      remark: this.string('').nullable(),
    }
  }

  static methodConf = {
    http: {
      url: '/scConfig'
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
          url: '/get/:id',
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
