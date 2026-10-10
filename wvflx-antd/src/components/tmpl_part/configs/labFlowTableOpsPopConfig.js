import { COMPO } from '@/constants'

let configObj = {
  title: '配置操作项',
  submitText: '确认',
  modal: true,
  formFields: [
    {
      title: '定义新增编辑',
      field: 'add-panel',
      component: COMPO.G_PANEL,
      options: {
      },
      extend: {
        icon: 'plus',
        collapsible: 'false',
      },
    },
    {
      title: '允许启动',
      field: 'addEnabled',
      component: COMPO.G_SWITCH,
      cols: 12,
      options: {
      },
    },
    {
      title: '启动按钮',
      field: 'addCaption',
      component: COMPO.G_INPUT,
      placeholder: '启动按钮标题',
      cols: 12,
      options: {
      },
    },
    {
      title: '允许操作',
      field: 'editEnabled',
      component: COMPO.G_SWITCH,
      cols: 24,
      options: {
      },
    },
    {
      title: '定义其它部分',
      field: 'other-panel',
      component: COMPO.G_PANEL,
      options: {
      },
      extend: {
        icon: 'bulb',
        collapsible: 'false',
      },
    },
    {
      title: '允许删除',
      field: 'deleteEnabled',
      component: COMPO.G_SWITCH,
      cols: 24,
      options: {
      },
    },
  ],
}

export default configObj