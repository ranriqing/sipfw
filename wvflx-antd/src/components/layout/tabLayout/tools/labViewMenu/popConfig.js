import { COMPO } from '@/constants'

export default {
  /* 新增弹窗 */
  addConfig: {
    modal: true,
    popWidth: 700,
    title: '新增视图',
    submitText: '保存',
    initValues: function(){
      return {type: '1'}
    },
    formFields: [
      {
        field: 'name',
        component: COMPO.G_INPUT,
        title: '视图名',
        placeholder: '请输入视图名',
        options: {
          rules: [{ required: true, message: '请输入视图名' }]
        },
      },
      {
        field: 'type',
        component: COMPO.G_RADIO_GROUP,
        title: '视图类型',
        placeholder: '请选择视图类型',
        extra: '超管视图: 只有超级管理员才可见',
        options: {
          rules: [{ required: true, message: '请选择视图类型' }]
        },
        extend: {
          options: {
            开放视图: '0',
            超管视图: '1',
          },
        }
      },
      {
        field: 'icon',
        component: COMPO.G_ICON_SELECTOR,
        title: '展示图标',
        placeholder: '请选择要展示的图标',
        options: {
          rules: [{ required: true, message: '请选择要展示的图标' }]
        },
      },
    ],
  },
  /* 编辑弹窗 */
  editConfig: {
    modal: true,
    popWidth: 700,
    title: '编辑视图',
    submitText: '更新',
    formFields: [
      {
        title: '视图名',
        field: 'name',
        component: COMPO.G_INPUT,
        placeholder: '请输入视图名',
        options: {
          rules: [{ required: true, message: '请输入视图名' }]
        },
      },
      {
        title: '置顶视图',
        field: 'top',
        component: COMPO.G_SWITCH,
        placeholder: '置顶当前视图',
        options: {
        },
      },
      {
        title: '视图类型',
        field: 'type',
        component: COMPO.G_RADIO_GROUP,
        placeholder: '请选择视图类型',
        extra: '超管视图: 只有超级管理员才可见',
        options: {
          rules: [{ required: true, message: '请选择视图类型' }]
        },
        extend: {
          options: {
            开放视图: '0',
            超管视图: '1',
          },
        }
      },
      {
        title: '展示图标',
        field: 'icon',
        component: COMPO.G_ICON_SELECTOR,
        placeholder: '请选择要展示的图标',
        options: {
          rules: [{ required: true, message: '请选择要展示的图标' }]
        },
      },
    ],
  }
}