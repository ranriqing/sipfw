import { Model } from '@vuex-orm/core'
import MetaDomain from './MetaDomain'

export default class MetaDomainField extends Model {
  static entity = 'metaDomainFields'

  static fields () {
    return {
      id: this.increment(),
      title: this.string('').nullable(),
      fieldName: this.string('').nullable(),
      fieldType: this.string('').nullable(),
      // 后端标识：该字段是否在 code 的 superClass 中已定义
      codeField: this.boolean(false),
      seqOrder: this.number(100),
      visible: this.boolean(true),
      globalSearch: this.boolean(false),
      extendJson: this.string('').nullable(),
      constraintJson: this.string('').nullable(),
      remark: this.string('').nullable(),
      dateCreated: this.string('').nullable(),
      lastUpdated: this.string('').nullable(),
      metaDomainId: this.number(0),
      relMetaDomain: this.belongsTo(MetaDomain, 'metaDomainId'),
    }
  }

  /*
  fieldType -> Reference:
  extendJson: {
    relDomainId: $id,               // 关联MetaDomain->id
    relDomainName: $domainName,     // 关联MetaDomain->domainName
  }
  fieldType -> RefInverse:
  extendJson: {
    relDomainId: $id,               // 关联MetaDomain->id
    relDomainName: $domainName,     // 关联MetaDomain->domainName
    relDomainFieldId: $id,          // 关联MetaDomainField->id
    relDomainFieldName: $fieldName, // 关联MetaDomainField->fieldName
    relDomainFieldType: many|one    // many|one
  }
  */

  static methodConf = {
    http: {
      url: '/metaDomainField'
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
