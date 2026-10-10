import { Model } from '@vuex-orm/core'

export default class MetaDomain extends Model {
  static entity = 'metaDomains'

  static fields () {
    return {
      id: this.increment(),
      title: this.string('').nullable(),
      domainName: this.string('').nullable(),
      // 后端标识：该 Domain 是否有 code 中对应的实体扩展（VmCodePoet）
      codeEntity: this.boolean(false),
      sciDomainCategory: this.number(null).nullable(),
      seqOrder: this.number(100),
      visible: this.boolean(true),
      extendJson: this.string('').nullable(),
      remark: this.string('').nullable(),
      dateCreated: this.string('').nullable(),
      lastUpdated: this.string('').nullable(),
    }
  }
  
  static methodConf = {
    http: {
      url: '/metaDomain'
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
