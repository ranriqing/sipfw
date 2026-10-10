import { Model } from '@vuex-orm/core'
import MetaDomain from '@/store/models/MetaDomain'

export default class MetaPageComponent extends Model {
  static entity = 'metaPageComponents'

  static fields () {
    return {
      id: this.increment(),
      title: this.string('').nullable(),
      ident: this.string('').nullable(),
      pIdent: this.string('').nullable(),
      type: this.string('').nullable(),
      component: this.string('').nullable(),
      seqOrder: this.number(100),
      initialized: this.boolean(false),
      extend: this.string('').nullable(),
      remark: this.string('').nullable(),
      metaDomainId: this.number(0),
      relMetaDomain: this.belongsTo(MetaDomain, 'metaDomainId'),
    }
  }

  /*
   component ->  TabPage:
   extend: {
     relDomainId: $id,               // 关联MetaDomain->id
     relDomainFieldId: $id,          // 关联MetaDomainField->id
     relDomainFieldType: many|one    // many|one
   }
  */

  static methodConf = {
    http: {
      url: '/orm/metaPageComponent'
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
