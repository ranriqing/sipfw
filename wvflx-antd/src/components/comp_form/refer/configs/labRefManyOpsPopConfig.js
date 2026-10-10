import _ from '@/utils/tiny_lodash'
import { COMPO } from '@/constants'

let configObj = {
  modal: true,
  title: '配置操作项',
  submitText: '保存',
  layout: 'vertical',
  formFields: [
    {
      title: '允许新增',
      field: 'addEnabled',
      component: COMPO.G_SWITCH,
      cols: 12,
    },
    {
      title: '新增按钮文字',
      field: 'addCaption',
      component: COMPO.G_INPUT,
      placeholder: '默认: 添加',
      cols: 12,
    },
    {
      title: '允许编辑',
      field: 'editEnabled',
      component: COMPO.G_SWITCH,
      cols: 12,
    },
    {
      title: '编辑按钮文字',
      field: 'editCaption',
      component: COMPO.G_INPUT,
      placeholder: '默认: 编辑',
      cols: 12,
    },
    {
      title: '条件编辑',
      field: 'conditionEditOn',
      component: COMPO.G_SWITCH,
      cols: 12,
      change: function (record, context, value) {
        context['conditionEditOn'] = value
      },
    },
    {
      title: '',
      field: 'editConditionSlot',
      component: COMPO.G_SLOT,
      cols: 24,
      isActived: function (record, context) {
        return !!(_.get(context, 'conditionEditOn'))
      },
    },
    {
      title: '允许删除',
      field: 'deleteEnabled',
      component: COMPO.G_SWITCH,
      cols: 12,
    },
  ],
}

export default configObj
