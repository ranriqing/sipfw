import { Model } from '@vuex-orm/core'
import MetaPageComponent from '@/store/models/MetaPageComponent'
import MetaDomainField from '@/store/models/MetaDomainField'

export default class MetaPageComponentField extends Model {
  static entity = 'metaPageComponentFields'

  static fields () {
    return {
      id: this.increment(),
      title: this.string('').nullable(),
      field: this.string('').nullable(),
      type: this.string('').nullable(),
      component: this.string('').nullable(),
      placeholder: this.string('').nullable(),
      seqOrder: this.number(100),
      pIdent: this.string('').nullable(), // 用于流程弹窗
      extend: this.string('').nullable(),
      remark: this.string('').nullable(),
      metaPageComponentId: this.number(0),
      relMetaPageComponent: this.belongsTo(MetaPageComponent, 'metaPageComponentId'),
      metaDomainFieldId: this.number(0),
      relMetaDomainField: this.belongsTo(MetaDomainField, 'metaDomainFieldId'),
    }
  }

  static methodConf = {
    http: {
      url: '/orm/metaPageComponentField'
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
