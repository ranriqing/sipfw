import { Model } from '@vuex-orm/core'
import GcsGroup from '@/store/models/GcsGroup'

export default class GcsUser extends Model {
  static entity = 'gcsUsers'

  static fields () {
    return {
      id: this.increment(),
      username: this.string('').nullable(),
      mobile: this.string('').nullable(),
      nickname: this.string('').nullable(),
      avatar: this.string('').nullable(),
      gcsRoleId: this.number(0),
      gcsGroupId: this.number(0),
      isActive: this.boolean(true),
      ccJobNum: this.string('').nullable(),
      ccJobPwd: this.string('').nullable(),
      gatewayStrategy: this.string('').nullable(),
      strategyPattern: this.string('').nullable(),
      remark: this.string('').nullable(),
      dateCreated: this.string('').nullable(),
      lastUpdated: this.string('').nullable(),
      // ---------- //
      wxUserId: this.string('').nullable(),
      wxNickname: this.string('').nullable(),
      wxSubscribe: this.boolean(false),
      wxSubscribeTime: this.string('').nullable(),
      wxHeadImgUrl: this.string('').nullable(),
      // ---------- //
      gcsRoleCaption: this.string('').nullable(),
      relGcsGroup: this.belongsTo(GcsGroup, 'gcsGroupId'),
    }
  }

  static methodConf = {
    http: {
      url: '/gcsUser'
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
