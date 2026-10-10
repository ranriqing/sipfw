import { COMPO } from '@/constants'
import { stores, tools } from '@/utils/util'

export default {
  /* 新增弹窗 */
  addConfig: {
    title: '新增',
    submitText: '保存',
    initValues: function(){
      return stores.isSuperAdmin()?{type: '1'}:{}
    },
    formFields: [
      {
        field: 'name',
        component: COMPO.G_INPUT,
        title: '名 称',
        placeholder: '请输入角色名称',
        extra: '用户角色的名称',
        options: {
          rules: [{ required: true, message: '请输入角色名称' }]
        },
      },
      {
        title: '分 级',
        field: 'type',
        component: COMPO.G_RADIO_GROUP,
        placeholder: '请选择角色分级',
        extra: '限制角色: 只有超级管理员才可用指派、维护管理',
        isActived: function(record, context){
          record || context || true
          return stores.isSuperAdmin()
        },
        options: {
          rules: [{ required: true, message: '请选择角色分级' }]
        },
        extend: {
          options: {
            普通角色: '0',
            限制角色: '1',
          },
        }
      },
      {
        field: 'viewId',
        component: COMPO.G_REF_DOMAIN,
        title: '视 图',
        placeholder: '请选择角色视图',
        options: {},
        extend: {
          showUri: '/metaView/show',
          indexUri: '/metaView/index',
          relQueryOn: false,
          relQueryField: 'name',
        }
      },
      {
        field: 'remark',
        component: COMPO.G_TEXTAREA,
        title: '备 注',
        placeholder: '请输入备注',
        options: {

        },
      }
    ],
  },
  /* 编辑弹窗 */
  editConfig: {
    title: '编辑',
    submitText: '更新',
    formFields: [
      {
        title: '名 称',
        field: 'name',
        component: COMPO.G_INPUT,
        placeholder: '请输入角色名称',
        extra: '用户角色的名称',
        options: {
          rules: [{ required: true, message: '请输入角色名称' }]
        },
      },
      {
        title: '分 级',
        field: 'type',
        component: COMPO.G_RADIO_GROUP,
        placeholder: '请选择角色分级',
        extra: '限制角色: 只有超级管理员才可用指派、维护管理',
        isActived: function(record, context){
          record || context || true
          return stores.isSuperAdmin()
        },
        options: {
          rules: [{ required: true, message: '请选择角色分级' }]
        },
        extend: {
          options: {
            普通角色: '0',
            限制角色: '1',
          },
        }
      },
      {
        title: '视 图',
        field: 'viewId',
        component: COMPO.G_REF_DOMAIN,
        placeholder: '请选择角色视图',
        options: {},
        extend: {
          showUri: '/metaView/show',
          indexUri: '/metaView/index',
          relQueryOn: false,
          relQueryField: 'name',
        }
      },
      {
        title: '权 限',
        field: 'slot_permission',
        component: COMPO.G_SLOT,
        placeholder: '请配置数据权限',
        extra: tools.isApply95()?'范围: 本人(我创建)、本部(所在部门)':'范围: 本人(我创建)、本部(所在部门)、子部(部门的子部门)、本子部(部门和子部门)',
        options: {
        },
      },
      {
        title: '备 注',
        field: 'remark',
        component: COMPO.G_TEXTAREA,
        placeholder: '请输入备注',
        options: {
        },
      }
    ],
  },
}