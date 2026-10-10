import _ from '@/utils/tiny_lodash'
import { COMPO } from '@/constants'

export default {
  title: '权限配置',
  submitText: '保存',
  popWidth: 360,
  layout: 'vertical',
  initValues: function () {
    return {
      cfExtend_cols: 24,
    }
  },
  formFields: [
    {
      title: '标题',
      field: 'title',
      component: COMPO.G_INPUT,
      placeholder: '请输入标题',
      options: {
        rules: [{ required: true, message: '请输入标题' }]
      },
    },
    {
      title: '图标',
      field: 'icon',
      component: COMPO.G_ICON_SELECTOR,
      placeholder: '请选择图标',
      isActived: function (record) {
        return _.get(record, 'type') === 'panel'
      },
      options: {
      },
    },
    {
      title: '类型',
      field: 'type',
      component: COMPO.G_SELECT,
      placeholder: '请选择类型',
      options: {
        rules: [{ required: true, message: '请选择类型' }]
      },
      extend: {
        allowClear: false,
        options: {
          分组: 'panel',
          选择: 'select',
          勾选: 'check',
        }
      }
    },
    {
      title: '权限字符串',
      field: 'permission',
      component: COMPO.G_INPUT,
      placeholder: '例如：Customer_assignee/Contact_relCustomer_assignee',
      isActived: function (record) {
        return _.get(record, 'type') !== 'panel'
      },
      options: {
        rules: [{ required: true, message: '请输入权限字符串' }]
      },
    },
    {
      title: '列占比',
      field: 'cfExtend_cols',
      component: COMPO.G_SLIDER,
      placeholder: '请选择列占比',
      getValue: function (record) {
        return _.get(record, 'cfExtend.cols', 24)
      },
      setValue: function (record, fieldValue) {
        _.set(record, 'cfExtend.cols', fieldValue)
      },
      options: {},
      extend: {
        min: 4,
        max: 24,
        step: 4,
        dots: true,
        marks: {
          4: '1/6',
          8: '1/3',
          12: '1/2',
          16: '2/3',
          20: '5/6',
          24: '1',
        },
      },
    },
    {
      field: 'cfExtend_options',
      component: COMPO.G_OPTIONS_EDITOR,
      title: '选项配置',
      placeholder: '请配置选项',
      isActived: function (record) {
        return ['select'].indexOf(_.get(record, 'type')) !== -1
      },
      getValue: function (record) {
        return _.get(record, 'cfExtend.options')
      },
      setValue: function (record, fieldValue) {
        _.set(record, 'cfExtend.options', fieldValue)
      },
    },
  ],
}

