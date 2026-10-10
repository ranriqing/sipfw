import { COMPO, FIELD } from '@/constants'
import { validators } from '@/utils/util'

let configObj = {
  modal: true,
  title: '添加组件',
  submitText: '保存',
  layout: 'vertical',
  formFields: [
    {
      title: '标 题',
      field: 'title',
      component: COMPO.G_INPUT,
      placeholder: '请输入组件标题',
      options: {
        rules: [
          { required: true, message: '请输入组件标题' },
        ]
      },
    },
    {
      title: '字 段',
      field: 'fieldName',
      component: COMPO.G_INPUT,
      placeholder: '请输入组件字段',
      options: {
        rules: [
          { required: true, message: '请输入组件字段' },
          { validator: validators.fieldNameValidator }
        ]
      },
    },
    {
      title: '手输类型',
      field: 'none_type',
      component: COMPO.G_SWITCH,
      change: function(record, context, value){
        context['inputOn'] = value
      },
      options: {
      },
    },
    {
      title: '字段类型',
      field: 'fieldType',
      component: COMPO.G_INPUT,
      placeholder: '请输入字段类型',
      isActived: function(record, context){
        return !!context['inputOn']
      },
      options: {
        rules: [
          { required: true, message: '请输入字段类型' },
          { validator: validators.fieldNameValidator }
        ]
      },
    },
    {
      title: '字段类型',
      field: 'fieldType',
      component: COMPO.G_SELECT,
      placeholder: '请选择字段类型',
      isActived: function(record, context){
        return !context['inputOn']
      },
      options: {
        rules: [
          { required: true, message: '请选择字段类型' },
        ]
      },
      /*change: function(record, context, value){
        context['fieldType'] = value
      },*/
      extend: {
        options: function(record, context) {
          record || context
          let optionsObj = {}
          optionsObj['字符串'] = FIELD.STRING
          optionsObj['选项型'] = FIELD.SELECT
          optionsObj['整数型'] = FIELD.INTEGER
          optionsObj['长整型'] = FIELD.LONG
          optionsObj['精确型'] = FIELD.BIG_DECIMAL
          optionsObj['日期型'] = FIELD.DATE
          optionsObj['用户型'] = FIELD.USER
          optionsObj['部门型'] = FIELD.GROUP
          return optionsObj
        }
      }
    },
  ],
}

export default configObj