import _ from '@/utils/tiny_lodash'
import { COMPO, DATE, FIELD, OTHER } from '@/constants'
import MetaDomain from "@/store/models/MetaDomain";

let configObj = {
  title: '组件属性',
  submitText: '保存',
  popWidth: 340,
  layout: 'vertical',
  initValues: function(){
    return {
      cfExtend_cols: 8,
    }
  },
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
      title: '引导文字',
      field: 'placeholder',
      component: COMPO.G_INPUT,
      placeholder: '请输入引导文字',
      options: {
      },
    },
    /*{
      title: '查询类型',
      field: 'cfExtend_queryType',
      component: COMPO.G_RADIO_GROUP,
      isActived: function(record){
        return ['q-dyn-integer'].indexOf(record.component) !== -1
      },
      getValue: function(record){
        return _.get(record, 'cfExtend.queryType')
      },
      setValue: function(record, fieldValue){
        _.set(record, 'cfExtend.queryType', fieldValue)
      },
      extend: {
        options: {
          equal: 'equal',
          between: 'between',
        },
      }
    },*/
    {
      title: '查询模式',
      field: 'cfExtend_mode',
      component: COMPO.G_RADIO_GROUP,
      isActived: function(record){
        return [COMPO.G_INPUT, COMPO.G_TSELECT].indexOf(record.component) !== -1
      },
      change: function(record, context, value){
        _.set(record, 'cfExtend.mode', value)
      },
      getValue: function(record){
        return _.get(record, 'cfExtend.mode', 'normal')
      },
      setValue: function(record, fieldValue){
        _.set(record, 'cfExtend.mode', fieldValue)
      },
      extend: {
        options: {
          普通: 'normal',
          存在: 'exists',
        },
      }
    },
    {
      title: '查询类型',
      field: 'cfExtend_operator',
      component: COMPO.G_SELECT,
      placeholder: '请选择查询类型',
      isActived: function(record){
        let normalMode = _.get(record, 'cfExtend.mode', 'normal') === 'normal'
        return normalMode && [COMPO.G_INPUT, COMPO.G_TSELECT].indexOf(record.component) !== -1
      },
      getValue: function(record){
        return _.get(record, 'cfExtend.operator')
      },
      setValue: function(record, fieldValue){
        _.set(record, 'cfExtend.operator', fieldValue)
      },
      options: {
      },
      extend: {
        options: function(record, context){
          context || true
          let optionsObj = {}
          if([COMPO.G_INPUT, COMPO.G_TSELECT].indexOf(record.component) !== -1){
            optionsObj['包含'] = 'contains'
            optionsObj['以开头'] = 'begins_with'
            optionsObj['以结尾'] = 'ends_with'
          }
          return optionsObj
        }
      },
    },
    {
      title: '列占比',
      field: 'cfExtend_cols',
      component: COMPO.G_SLIDER,
      placeholder: '请选择列占比',
      getValue: function(record){
        return _.get(record, 'cfExtend.cols', 8)
      },
      setValue: function(record, fieldValue){
        _.set(record, 'cfExtend.cols', fieldValue)
      },
      options: {
      },
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
      field: 'dfExtend_options',
      component: COMPO.G_OPTIONS_EDITOR,
      title: '选项配置',
      placeholder: '请配置选项',
      isActived: function(record){
        return [COMPO.G_SELECT].indexOf(record.component) !== -1
      },
      getValue: function(record){
        return _.get(record, 'dfExtend.options')
      },
      setValue: function(record, fieldValue){
        _.set(record, 'dfExtend.options', fieldValue)
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
      title: '初始范围',
      field: 'cfExtend_initValue',
      component: COMPO.G_SELECT,
      placeholder: '请选择初始范围',
      isActived: function(record){
        return [COMPO.G_DATE_RANGE].indexOf(record.component) !== -1
      },
      getValue: function(record){
        return _.get(record, 'cfExtend.initValue')
      },
      setValue: function(record, fieldValue){
        _.set(record, 'cfExtend.initValue', fieldValue)
      },
      options: {
      },
      extend: {
        allowClear: true,
        options: {
          本天: 'day',
          本周: 'week',
          本月: 'month',
        }
      },
    },
    {
      title: '引用配置',
      field: 'cfExtend_relCols',
      component: COMPO.G_REF_EDITOR,
      placeholder: '请定义引用配置',
      isActived: function(record){
        return [FIELD.MANY_TO_ONE, FIELD.ONE_TO_MANY].indexOf(record.type) !== -1
      },
      getValue: function(record){
        return _.get(record, 'cfExtend.relCols')
      },
      setValue: function(record, fieldValue){
        _.set(record, 'cfExtend.relCols', fieldValue)
      },
      options: {
      },
      extend: {
        colsLimit: 3,
        relDomainNameFunc: function(record, context){
          context || true
          let refSelectDomain = MetaDomain.find(_.get(record, `extend.${OTHER.REL_DOMAIN_ID}`))
          return refSelectDomain?.domainName
        }
      },
    },
  ],
}

export default configObj