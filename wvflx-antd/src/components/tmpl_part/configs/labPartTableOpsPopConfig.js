import _ from '@/utils/tiny_lodash'
import { COMPO } from '@/constants'

let configObj = {
  title: '配置操作项',
  submitText: '确认',
  modal: true,
  formFields: [
    {
      title: '定义请求路径',
      field: 'uri-panel',
      component: COMPO.G_PANEL,
      isActived: function(record, context){
        return _.get(context, 'targetModel.metaDomain') === 0
      },
      options: {
      },
      extend: {
        icon: 'link',
        collapsible: 'false',
      },
    },
    {
      title: 'URI前缀',
      field: 'uriPrefix',
      component: COMPO.G_INPUT,
      placeholder: '调用URI前缀',
      isActived: function(record, context){
        return _.get(context, 'targetModel.metaDomain') === 0
      },
      cols: 24,
      options: {
      },
    },
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
      title: '允许新增',
      field: 'addEnabled',
      component: COMPO.G_SWITCH,
      cols: 12,
      options: {
      },
    },
    {
      title: '新增按钮',
      field: 'addCaption',
      component: COMPO.G_INPUT,
      placeholder: '新增按钮标题',
      cols: 12,
      options: {
      },
    },
    {
      title: '允许编辑',
      field: 'editEnabled',
      component: COMPO.G_SWITCH,
      cols: 12,
      options: {
      },
    },
    {
      title: '编辑按钮',
      field: 'editCaption',
      component: COMPO.G_INPUT,
      placeholder: '编辑按钮标题',
      cols: 12,
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
      title: '允许导入',
      field: 'importEnabled',
      component: COMPO.G_SWITCH,
      cols: 12,
      options: {
      },
    },
    {
      title: '批量操作',
      field: 'batchEnabled',
      component: COMPO.G_SWITCH,
      cols: 12,
      options: {
      },
    },
    {
      title: '允许删除',
      field: 'deleteEnabled',
      component: COMPO.G_SWITCH,
      cols: 12,
      options: {
      },
    },
  ],
}

export default configObj