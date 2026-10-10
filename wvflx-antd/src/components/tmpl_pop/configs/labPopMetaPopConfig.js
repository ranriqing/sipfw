import _ from '@/utils/tiny_lodash'
import {COMPO, FIELD, OTHER, DATE} from '@/constants'
import MetaDomainField from "@/store/models/MetaDomainField";
import {converters, domains} from "@/utils/util";
import MetaDomain from "@/store/models/MetaDomain";

let configObj = {
  title: '组件属性',
  submitText: '保存',
  popWidth: 340,
  layout: 'vertical',
  initValues: function(){
    return {
      cfExtend_cols: 24,
    }
  },
  defaultActive: function(record){
    // 非G_PANEL为active
    return [COMPO.G_PANEL].indexOf(record.component) === -1
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
      isActived: function(record, context){
        return [COMPO.G_PANEL, COMPO.G_TEXT, COMPO.G_ALERT].indexOf(record.component) === -1 // 不是这些组件
          && !context.metaPopTable
      },
    },
    {
      title: '组件状态',
      field: 'cfExtend_disabled',
      component: COMPO.G_RADIO_GROUP,
      isActived: function(record, context){
        return configObj.defaultActive(record) && !context.metaPopTable
      },
      getValue: function(record){
        let rstValue = 1
        let readonly = _.get(record, 'cfExtend.readonly', false)
        if(readonly){
          rstValue = 2
        }else{
          rstValue = _.get(record, 'cfExtend.disabled', false)?0:1
        }
        return rstValue
      },
      setValue: function(record, fieldValue){
        let fieldValueStr = _.toString(fieldValue)
        if(fieldValueStr === '2'){
          _.set(record, 'cfExtend.readonly', true)
        }else{
          _.set(record, 'cfExtend.readonly', undefined)
          _.set(record, 'cfExtend.disabled', fieldValueStr === '0')
        }
      },
      extend: {
        options: {
          启用: 1,
          禁用: 0,
          只读: 2,
        },
      }
    },
    {
      title: '起始可见节点',
      field: 'cfExtend_showFromNode',
      component: COMPO.G_SELECT,
      placeholder: '选择从哪个节点开始可见',
      isActived: function(record, context){
        context = context || {}
        var flowSteps = context.flowSteps
        return _.isArray(flowSteps) && flowSteps.length > 0
          && configObj.defaultActive(record)
      },
      getValue: function(record){
        return _.get(record, 'cfExtend.showFromNode')
      },
      setValue: function(record, fieldValue){
        _.set(record, 'cfExtend.showFromNode', fieldValue)
      },
      options: {
      },
      extend: {
        allowClear: true,
        options: function(record, context){
          context = context || {}
          var flowSteps = context.flowSteps || []
          var optionsObj = {}
          flowSteps.forEach(function(step){
            optionsObj[step] = step
          })
          return optionsObj
        },
      },
    },
    {
      title: '可编辑节点',
      field: 'cfExtend_editableOnNodes',
      component: COMPO.G_MULTI_SELECT,
      placeholder: '选择哪些节点可以编辑（可多选）',
      isActived: function(record, context){
        context = context || {}
        var flowSteps = context.flowSteps
        return _.isArray(flowSteps) && flowSteps.length > 0
          && configObj.defaultActive(record)
      },
      getValue: function(record){
        return _.get(record, 'cfExtend.editableOnNodes')
      },
      setValue: function(record, fieldValue){
        _.set(record, 'cfExtend.editableOnNodes', fieldValue)
      },
      options: {
      },
      extend: {
        allowClear: true,
        options: function(record, context){
          context = context || {}
          var flowSteps = context.flowSteps || []
          var optionsObj = {}
          flowSteps.forEach(function(step){
            optionsObj[step] = step
          })
          return optionsObj
        },
      },
    },
    {
      title: '列占比',
      field: 'cfExtend_cols',
      component: COMPO.G_SLIDER,
      placeholder: '请选择列占比',
      options: {
      },
      isActived: function(record, context){
        return configObj.defaultActive(record) && !context.metaPopTable
      },
      getValue: function(record){
        return _.get(record, 'cfExtend.cols', 24)
      },
      setValue: function(record, fieldValue){
        _.set(record, 'cfExtend.cols', fieldValue)
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
      title: '选项配置',
      field: 'dfExtend_options',
      component: COMPO.G_OPTIONS_EDITOR,
      placeholder: '请配置选项',
      options: {
      },
      isActived: function(record){
        return [COMPO.G_SELECT].indexOf(record.component) !== -1
      },
      getValue: function(record){
        return _.get(record, 'dfExtend.options')
      },
      setValue: function(record, fieldValue){
        _.set(record, 'dfExtend.options', fieldValue)
      },
    },
    /*{
      title: '小数位数',
      field: 'cfExtend_precision',
      component: COMPO.G_NUMBER_INTEGER,
      placeholder: '请输入小数位数',
      options: {
      },
      isActived: function(record){
        return [COMPO.G_NUMBER_DECIMAL].indexOf(record.component) !== -1
      },
      getValue: function(record){
        return _.get(record, 'cfExtend.precision')
      },
      setValue: function(record, fieldValue){
        _.set(record, 'cfExtend.precision', fieldValue)
      },
    },*/
    {
      title: '组件类型',
      field: 'cfExtend_inputType',
      component: COMPO.G_SELECT,
      placeholder: '请选择组件类型',
      options: {
      },
      isActived: function(record){
        return [FIELD.STRING].indexOf(record.type) !== -1
      },
      getValue: function(record){
        return _.get(record, 'cfExtend.inputType')
      },
      setValue: function(record, fieldValue){
        _.set(record, 'cfExtend.inputType', fieldValue)
      },
      extend: {
        options: {
          '普通输入': 'text',
          '多行输入': 'textarea',
        }
      },
    },
    {
      title: '日期格式',
      field: 'cfExtend_format',
      component: COMPO.G_SELECT,
      placeholder: '请选择日期格式',
      options: {
      },
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
      extend: {
        options: {
          [DATE.YYYYMM]: DATE.YYYYMM,
          [DATE.YYYYMMDD]: DATE.YYYYMMDD,
          [DATE.YYYYMMDD_HHMM]: DATE.YYYYMMDD_HHMM,
          [DATE.YYYYMMDD_HHMMSS]: DATE.YYYYMMDD_HHMMSS,
        }
      },
    },
    {
      title: '是否必填',
      field: 'cfExtend_required',
      component: COMPO.G_SWITCH,
      placeholder: '是否必填',
      options: {
      },
      isActived: function(record){
        let dfRequired = _.get(record, 'extend.dfRequired')
        return !dfRequired && [COMPO.G_PANEL].indexOf(record.component) === -1 // 不是这些组件
      },
      getValue: function(record){
        return _.get(record, 'cfExtend.required')
      },
      setValue: function(record, fieldValue){
        _.set(record, 'cfExtend.required', fieldValue)
      },
    },
    /*{
      title: '上下文',
      field: 'extend_contextExpr',
      component: COMPO.G_INPUT,
      placeholder: 'field写入、field=value或field!=value验证',
      options: {
      },
      isActived: function(record){
        return configObj.defaultActive(record)
      },
      getValue: function(record){
        return _.get(record, 'extend.contextExpr')
      },
      setValue: function(record, fieldValue){
        _.set(record, 'extend.contextExpr', fieldValue)
      },
    },*/
    {
      title: '引用字段',
      field: 'cfExtend_relCols',
      component: COMPO.G_REF_EDITOR,
      placeholder: '请定义引用字段',
      options: {
      },
      isActived: function(record){
        return [FIELD.MANY_TO_ONE].indexOf(record.type) !== -1
      },
      getValue: function(record){
        return _.get(record, 'cfExtend.relCols')
      },
      setValue: function(record, fieldValue){
        _.set(record, 'cfExtend.relCols', fieldValue)
      },
      extend: {
        relDomainNameFunc: function(record, context){
          context || true
          let refSelectDomain = MetaDomain.find(_.get(record, `extend.${OTHER.REL_DOMAIN_ID}`))
          return refSelectDomain?.domainName
        }
      },
    },
    {
      title: '引用操作',
      field: 'cfExtend_ops',
      component: COMPO.G_LABEL,
      options: {
      },
      isActived: function(record){
        return [FIELD.ONE_TO_MANY + 'none'].indexOf(record.type) !== -1
      },
    },
    {
      title: undefined,
      field: 'cfExtend_addEnabled',
      component: COMPO.G_SWITCH,
      cols: 8,
      options: {
      },
      isActived: function(record){
        return [FIELD.ONE_TO_MANY + 'none'].indexOf(record.type) !== -1
      },
      getValue: function(record){
        return _.get(record, 'cfExtend.addEnabled')
      },
      setValue: function(record, fieldValue){
        _.set(record, 'cfExtend.addEnabled', fieldValue)
      },
      extend: {
        btnTitle: '新增'
      }
    },
    {
      title: undefined,
      field: 'cfExtend_editEnabled',
      component: COMPO.G_SWITCH,
      cols: 8,
      options: {
      },
      isActived: function(record){
        return [FIELD.ONE_TO_MANY + 'none'].indexOf(record.type) !== -1
      },
      getValue: function(record){
        return _.get(record, 'cfExtend.editEnabled')
      },
      setValue: function(record, fieldValue){
        _.set(record, 'cfExtend.editEnabled', fieldValue)
      },
      extend: {
        btnTitle: '编辑'
      }
    },
    {
      title: undefined,
      field: 'cfExtend_deleteEnabled',
      component: COMPO.G_SWITCH,
      cols: 8,
      options: {
      },
      isActived: function(record){
        return [FIELD.ONE_TO_MANY + 'none'].indexOf(record.type) !== -1
      },
      getValue: function(record){
        return _.get(record, 'cfExtend.deleteEnabled')
      },
      setValue: function(record, fieldValue){
        _.set(record, 'cfExtend.deleteEnabled', fieldValue)
      },
      extend: {
        btnTitle: '删除'
      }
    },
    {
      title: '选择启用状态',
      field: 'cfExtend_ref',
      component: COMPO.G_LABEL,
      options: {
      },
      isActived: function(record){
        return [FIELD.ONE_TO_MANY].indexOf(record.type) !== -1
      },
    },
    {
      title: undefined,
      field: 'cfExtend_refSelectOn',
      component: COMPO.G_SWITCH,
      cols: 12,
      options: {
      },
      isActived: function(record){
        return [FIELD.ONE_TO_MANY].indexOf(record.type) !== -1
      },
      change: function(record, context, value){
        _.set(record, `cfExtend.${OTHER.REF_SELECT_ON}`, value)
      },
      getValue: function(record){
        return _.get(record, `cfExtend.${OTHER.REF_SELECT_ON}`)
      },
      setValue: function(record, fieldValue){
        _.set(record, `cfExtend.${OTHER.REF_SELECT_ON}`, fieldValue)
      },
      extend: {
        btnTitle: '启用选择'
      }
    },
    {
      title: undefined,
      field: 'cfExtend_refQuickSelectOn',
      component: COMPO.G_SWITCH,
      cols: 12,
      options: {
      },
      isActived: function(record){
        return [FIELD.ONE_TO_MANY].indexOf(record.type) !== -1
        && _.get(record, `cfExtend.${OTHER.REF_SELECT_ON}`)
      },
      change: function(record, context, value){
        _.set(record, `cfExtend.${OTHER.REF_QUICK_SELECT_ON}`, value)
      },
      getValue: function(record){
        return _.get(record, `cfExtend.${OTHER.REF_QUICK_SELECT_ON}`)
      },
      setValue: function(record, fieldValue){
        _.set(record, `cfExtend.${OTHER.REF_QUICK_SELECT_ON}`, fieldValue)
      },
      extend: {
        btnTitle: '快捷选择'
      }
    },
    {
      title: undefined,
      field: 'cfExtend_refSelectDomain',
      component: COMPO.G_SELECT,
      cols: 24,
      placeholder: '选择数据表',
      options: {
      },
      isActived: function(record){
        return [FIELD.ONE_TO_MANY].indexOf(record.type) !== -1
          && _.get(record, `cfExtend.${OTHER.REF_SELECT_ON}`)
      },
      change: function(record, context, value){
        _.set(record, `cfExtend.${OTHER.REF_SELECT_DOMAIN_ID}`, value)
      },
      getValue: function(record){
        return _.get(record, `cfExtend.${OTHER.REF_SELECT_DOMAIN_ID}`)
      },
      setValue: function(record, fieldValue){
        _.set(record, `cfExtend.${OTHER.REF_SELECT_DOMAIN_ID}`, fieldValue)
      },
      extend: {
        allowClear: false,
        options: function(record, context){
          context || true
          // 1、本端实体的DomainId(eg: 采购单Domain)
          let selfMetaDomainId = MetaDomainField.find(record.metaDomainFieldId)?.metaDomainId
          // 2、对端实体的DomainId(eg: 采购详情Domain)
          let relDomainId = _.get(record, `dfExtend.${OTHER.REL_DOMAIN_ID}`)
          // 3、对端实体的ManyToOne字段(eg: 采购详情Domain的全部ManyToOne字段)
          let relDomainManyToOneFields = domains.many2OneFields(relDomainId, undefined)
          // 4、对端实体的ManyToOne字段所关联的Domain(eg: 采购详情Domain的全部ManyToOne字段关联Domain，采购单、商品等)
          //    过滤关联非本端实体的全部Domain(eg: 去除采购单、可以使用商品等实体选择)
          let availableDomains = relDomainManyToOneFields?.map( field => {
            let dfExtend = converters.parseObj(field['extendJson'])
            return MetaDomain.find(_.get(dfExtend, OTHER.REL_DOMAIN_ID))
          }).filter(domain => { return !!domain && domain.id !== selfMetaDomainId })
          // 5、返回最终可选项
          let optionsObj = {}
          availableDomains.forEach(domain => {
            optionsObj[domain.title] = domain['id']
          })
          return optionsObj
        },
      }
    },
    {
      title: '选择引用字段',
      field: 'cfExtend_relCols',
      component: COMPO.G_REF_EDITOR,
      placeholder: '请定义引用字段',
      options: {
      },
      isActived: function(record){
        return [FIELD.ONE_TO_MANY].indexOf(record.type) !== -1
          && _.get(record, `cfExtend.${OTHER.REF_SELECT_ON}`)
          && _.get(record, `cfExtend.${OTHER.REF_QUICK_SELECT_ON}`)
      },
      getValue: function(record){
        return _.get(record, 'cfExtend.relCols')
      },
      setValue: function(record, fieldValue){
        _.set(record, 'cfExtend.relCols', fieldValue)
      },
      extend: {
        relDomainNameFunc: function(record, context){
          context || true
          let refSelectDomain = MetaDomain.find(_.get(record, `cfExtend.${OTHER.REF_SELECT_DOMAIN_ID}`))
          return refSelectDomain?.domainName
        }
      },
    },
  ],
}

export default configObj