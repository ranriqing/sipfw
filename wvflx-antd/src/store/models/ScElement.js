import { Model } from '@vuex-orm/core'
import ScConfig from '@/store/models/ScConfig'

export default class ScElement extends Model {
  static entity = 'scElements'

  static fields () {
    return {
      id: this.increment(),
      title: this.string('').nullable(),
      seqOrder: this.number(100),
      dfault: this.boolean(false),
      tagStyle: this.string('').nullable(),
      description: this.string('').nullable(),
      remark: this.string('').nullable(),
      parentId: this.number(0),
      scConfigId: this.number(0),
      scConfigKey:  this.string('').nullable(),
      relScConfig: this.belongsTo(ScConfig, 'scConfigId'),
    }
  }

  static methodConf = {
    http: {
      url: '/scElement'
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
