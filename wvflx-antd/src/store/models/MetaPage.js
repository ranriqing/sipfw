import { Model } from '@vuex-orm/core'
import MetaView from '@/store/models/MetaView'
import MetaViewMenu from '@/store/models/MetaViewMenu'

export default class MetaPage extends Model {
  static entity = 'metaPages'

  static fields () {
    return {
      id: this.increment(),
      pdent: this.string('').nullable(),           // 页面标识，用于跳转
      ident: this.string('').nullable(),           // 唯一标识
      title: this.string('').nullable(),           // 页面标题
      component: this.string('').nullable(),       // 模板页面, eg: tmpl_simple/SimpleTmpl
      seqOrder: this.number(100),                  // 页面排序
      initialized: this.boolean(false),            // 是否已经初始化
      remark: this.string('').nullable(),
      metaViewId: this.number(0),
      relMetaView: this.belongsTo(MetaView, 'metaViewId'),
      metaViewMenuId: this.number(0),
      relMetaViewMenu: this.belongsTo(MetaViewMenu, 'metaViewMenuId'),
      extend: this.string('').nullable(),
    }
  }

  static methodConf = {
    http: {
      url: '/orm/metaPage'
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
