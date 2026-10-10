import { COMPO } from '@/constants'

export default {
  /* 新增弹窗 */
  addConfig: {
    modal: true,
    title: '新增',
    submitText: '保存',
    formFields: [
      {
        title: '父菜单',
        field: 'nonsense-parent',
        component: COMPO.G_ALERT,
        placeholder: '请输入父菜单',
        isActived: function(record, context){
          return !!context.id
        },
        getValue: function(record, context){
          return context['title']
        },
        options: {
        },
      },
      {
        title: '菜单名',
        field: 'title',
        component: COMPO.G_INPUT,
        placeholder: '请输入菜单名',
        options: {
          rules: [{ required: true, message: '请输入菜单名' }]
        },
      },
      {
        title: '展示图标',
        field: 'icon',
        component: COMPO.G_ICON_SELECTOR,
        placeholder: '请选择要展示的图标',
        isActived: function(record, context){
          return !context.id || context.pId===0
        },
        options: {
          rules: [{ required: true, message: '请选择要展示的图标' }]
        },
      },
      {
        title: '备 注',
        field: 'remark',
        component: COMPO.G_TEXTAREA,
        placeholder: '请输入备注',
        isActived: function(record, context){
          return !!context.id
        },
        options: {
        },
      },
    ],
  },
  /* 编辑弹窗 */
  editConfig: {
    modal: true,
    title: '编辑',
    submitText: '更新',
    formFields: [
      {
        title: '父菜单',
        field: 'nonsense-parent',
        component: COMPO.G_ALERT,
        placeholder: '请输入父菜单',
        isActived: function(record, context){
          return !!context.id
        },
        getValue: function(record, context){
          return context['title']
        },
        options: {
        },
      },
      {
        title: '菜单名',
        field: 'title',
        component: COMPO.G_INPUT,
        placeholder: '请输入菜单名',
        options: {
          rules: [{ required: true, message: '请输入菜单名' }]
        },
      },
      {
        title: '展示图标',
        field: 'icon',
        component: COMPO.G_ICON_SELECTOR,
        placeholder: '请选择要展示的图标',
        isActived: function(record, context){
          return !context.id || context.pId===0
        },
        options: {
          rules: [{ required: true, message: '请选择要展示的图标' }]
        },
      },
      {
        title: '备 注',
        field: 'remark',
        component: COMPO.G_TEXTAREA,
        placeholder: '请输入备注',
        isActived: function(record, context){
          return !!context.id
        },
        options: {
        },
      },
    ],
  },
}