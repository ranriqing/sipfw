import _ from '@/utils/tiny_lodash'
import { COMPO, FIELD, QUERY } from '@/constants'
import { converters } from '@/utils/util'
import MetaDomainField from '@/store/models/MetaDomainField'

let configObj = {
  modal: true,
  title: '过滤条件',
  submitText: '保存',
  layout: 'vertical',
  formFields: [
    {
      title: '条件列',
      field: 'domainCol',
      component: COMPO.G_SLOT,
    },
    {
      title: '条件类型',
      field: 'ruleOperator',
      component: COMPO.G_SELECT,
      placeholder: '请选择条件类型',
      options: {
        rules: [
          { required: true, message: '请选择条件类型' },
        ]
      },
      change: function(record, context, value){
        context['ruleOperator'] = value
      },
      extend: {
        options: function(record, context){
          let dfItem = MetaDomainField.find(context['domainCol'])
          let optionsObj = {}
          if(dfItem){
            if([FIELD.INTEGER, FIELD.LONG, FIELD.BIG_DECIMAL].indexOf(dfItem.fieldType) !== -1){
              optionsObj['等于'] = QUERY.EQUAL
              optionsObj['大于'] = QUERY.GREATER
              optionsObj['大于等于'] = QUERY.GREATER_OR_EQUAL
              optionsObj['小于'] = QUERY.LESS
              optionsObj['小于等于'] = QUERY.LESS_OR_EQUAL
              optionsObj['之间'] = QUERY.BETWEEN
            }else if([FIELD.STRING].indexOf(dfItem.fieldType) !== -1){
              optionsObj['等于'] = QUERY.EQUAL
              optionsObj['包含'] = QUERY.CONTAINS
              optionsObj['以开头'] = QUERY.BEGINS_WITH
              optionsObj['以结尾'] = QUERY.ENDS_WITH
            }else if([FIELD.SELECT].indexOf(dfItem.fieldType) !== -1){
              optionsObj['等于'] = QUERY.EQUAL
            }else if([FIELD.DATE].indexOf(dfItem.fieldType) !== -1){
              optionsObj['大于'] = QUERY.GREATER
              optionsObj['大于等于'] = QUERY.GREATER_OR_EQUAL
              optionsObj['小于'] = QUERY.LESS
              optionsObj['小于等于'] = QUERY.LESS_OR_EQUAL
              optionsObj['之间'] = QUERY.BETWEEN
              optionsObj['相对日期'] = '_relative'
            }
          }
          return optionsObj
        }
      },
    },
    {
      title: '条件值', // FIELD.INTEGER等 条件值
      field: 'ruleValueInteger',
      component: COMPO.Q_DYN_INTEGER,
      placeholder: '请输入条件数值',
      isActived: function(record, context){
        let dfItem = MetaDomainField.find(context['domainCol'])
        return [FIELD.INTEGER, FIELD.LONG, FIELD.BIG_DECIMAL].indexOf(dfItem?.fieldType) !== -1
      },
      funcItem: function(itemSelf, record, context){
        _.set(itemSelf, 'extend.operator', context['ruleOperator'])
        return itemSelf
      },
      getValue: function(record){
        return _.get(record, 'ruleValue')
      },
      setValue: function(record, fieldValue){
        _.set(record, 'ruleValue', fieldValue)
      },
      options: {
        rules: [
          { required: true, message: '请输入条件数值' },
        ]
      },
    },
    {
      title: '条件值', // FIELD.STRING 条件值
      field: 'ruleValueString',
      component: COMPO.G_INPUT,
      placeholder: '请输入条件值',
      isActived: function(record, context){
        let dfItem = MetaDomainField.find(context['domainCol'])
        return [FIELD.STRING].indexOf(dfItem?.fieldType) !== -1
      },
      getValue: function(record){
        return _.get(record, 'ruleValue')
      },
      setValue: function(record, fieldValue){
        _.set(record, 'ruleValue', fieldValue)
      },
      options: {
        rules: [
          { required: true, message: '请输入条件值' },
        ]
      },
    },
    {
      title: '条件值', // FIELD.SELECT 条件值
      field: 'ruleValueSelect',
      component: COMPO.G_SELECT,
      placeholder: '请选择条件值',
      isActived: function(record, context){
        let dfItem = MetaDomainField.find(context['domainCol'])
        return [FIELD.SELECT].indexOf(dfItem?.fieldType) !== -1
      },
      getValue: function(record){
        return _.get(record, 'ruleValue')
      },
      setValue: function(record, fieldValue){
        _.set(record, 'ruleValue', fieldValue)
      },
      options: {
        rules: [
          { required: true, message: '请选择条件值' },
        ]
      },
      extend: {
        options: function(record, context){
          let dfItem = MetaDomainField.find(context['domainCol'])
          let optionsObj = {}
          if(dfItem){
            let extendJson = converters.parseObj(dfItem.extendJson)
            optionsObj = extendJson['options'] || {}
          }
          return optionsObj
        }
      },
    },
    {
      title: '条件值', // FIELD.DATE 条件值
      field: 'ruleValueDate',
      component: COMPO.Q_DYN_DATE,
      placeholder: '请选择日期值',
      isActived: function(record, context){
        let dfItem = MetaDomainField.find(context['domainCol'])
        return [FIELD.DATE].indexOf(dfItem?.fieldType) !== -1
      },
      funcItem: function(itemSelf, record, context){
        _.set(itemSelf, 'extend.operator', context['ruleOperator'])
        return itemSelf
      },
      getValue: function(record){
        return _.get(record, 'ruleValue')
      },
      setValue: function(record, fieldValue){
        _.set(record, 'ruleValue', fieldValue)
      },
      options: {
        rules: [
          { required: true, message: '请选择日期值' },
        ]
      },
    },
  ],
}

export default configObj