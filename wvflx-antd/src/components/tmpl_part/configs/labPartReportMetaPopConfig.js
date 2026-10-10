import _ from '@/utils/tiny_lodash'
import { COMPO, FIELD, DATE } from '@/constants'

let configObj = {
  title: '组件属性',
  submitText: '保存',
  popWidth: 340,
  layout: 'vertical',
  formFields: [
    {
      title: '标题',
      field: 'title',
      component: COMPO.G_INPUT,
      placeholder: '请输入标题',
      options: {
      },
    },
    {
      title: '字段排序',
      field: 'cfExtend_sorter',
      component: COMPO.G_SWITCH,
      placeholder: '字段排序',
      getValue: function(record){
        return !!_.get(record, 'cfExtend.sorter')
      },
      setValue: function(record, fieldValue){
        _.set(record, 'cfExtend.sorter', fieldValue)
      },
      isActived: function(record){
        return [
          FIELD.LONG, FIELD.INTEGER, FIELD.BIG_DECIMAL,
          FIELD.STRING, FIELD.DATE, FIELD.SELECT,
        ].indexOf(record.type) !== -1
      },
      options: {
      },
    },
    {
      title: '日期格式',
      field: 'cfExtend_format',
      component: COMPO.G_SELECT,
      placeholder: '请选择日期格式',
      isActived: function(record){
        // 如果存在关联的metaDomainFieldId，则使用metaDomainFieldId的配置
        return [FIELD.DATE].indexOf(record.type) !== -1 && !record.metaDomainFieldId
      },
      getValue: function(record){
        return _.get(record, 'cfExtend.format')
      },
      setValue: function(record, fieldValue){
        _.set(record, 'cfExtend.format', fieldValue)
      },
      options: {
      },
      extend: {
        allowClear: true,
        options: {
          [DATE.YYYYMM]: DATE.YYYYMM,
          [DATE.YYYYMMDD]: DATE.YYYYMMDD,
          [DATE.YYYYMMDD_HHMM]: DATE.YYYYMMDD_HHMM,
          [DATE.YYYYMMDD_HHMMSS]: DATE.YYYYMMDD_HHMMSS,
        }
      },
    },
    {
      title: '统计方式',
      field: 'cfExtend_projection',
      component: COMPO.G_SELECT,
      placeholder: '请选择统计方式',
      getValue: function(record){
        return _.get(record, 'cfExtend.projection')
      },
      setValue: function(record, fieldValue){
        _.set(record, 'cfExtend.projection', fieldValue)
      },
      isActived: function(record, context){
        context || true
        let isRowCount = 'rowCount'===record['field']
        return !isRowCount && [FIELD.INTEGER, FIELD.LONG, FIELD.BIG_DECIMAL].indexOf(record.type) !== -1
      },
      options: {
      },
      extend: {
        options: {
          '计数(COUNT)': 'COUNT',
          '相加(SUM)': 'SUM',
          '平均(AVG)': 'AVG',
          '最大(MAX)': 'MAX',
          '最小(MIN)': 'MIN',
        }
      },
    },
  ],
}

export default configObj