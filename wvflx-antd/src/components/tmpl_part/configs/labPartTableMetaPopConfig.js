import _ from '@/utils/tiny_lodash'
import {COMPO, FIELD, OTHER} from '@/constants'
import MetaDomain from "@/store/models/MetaDomain";

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
      title: '渲染方式',
      field: 'cfExtend_render',
      component: COMPO.G_SELECT,
      placeholder: '请选择渲染方式',
      isActived: function(record){
        let isAction = ['action'].indexOf(record.field) !== -1
        return !(isAction || [FIELD.USER, FIELD.GROUP, FIELD.ADDR, FIELD.FILE, FIELD.PICTURE, FIELD.V_ACTION].indexOf(record.type) !== -1)
      },
      getValue: function(record){
        return _.get(record, 'cfExtend.renderType')
      },
      setValue: function(record, fieldValue){
        _.set(record, 'cfExtend.renderType', fieldValue)
        if('manualRender' !== fieldValue){
          _.set(record, 'cfExtend.render', fieldValue)
        }
      },
      change: function(record, context, value){
        _.set(record, 'cfExtend.renderType', value)
      },
      extend: {
        options: {
          '默认方式': 'defaultRender',
          '链接展示': 'linkRender',
          '手工输入': 'manualRender',
        }
      },
    },
    {
      title: '手工输入',
      field: 'none_advance',
      component: COMPO.G_INPUT,
      placeholder: '输入渲染方式',
      isActived: function(record, context){
        context || true
        return _.get(record, 'cfExtend.renderType') === 'manualRender'
      },
      getValue: function(record){
        return _.get(record, 'cfExtend.render')
      },
      setValue: function(record, fieldValue){
        _.set(record, 'cfExtend.render', fieldValue)
      },
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
        return [FIELD.STRING, FIELD.LONG, FIELD.INTEGER, FIELD.BIG_DECIMAL, FIELD.DATE, FIELD.V_ACTIVITIES].indexOf(record.type) !== -1
      },
      options: {
      },
    },
    {
      title: '行内编辑',
      field: 'cfExtend_inlineEdit',
      component: COMPO.G_SWITCH,
      placeholder: '行内编辑',
      getValue: function(record){
        return !!_.get(record, 'cfExtend.inlineEdit')
      },
      setValue: function(record, fieldValue){
        _.set(record, 'cfExtend.inlineEdit', fieldValue)
      },
      isActived: function(record){
        return [
          FIELD.SELECT, FIELD.SC_EL,
          FIELD.STRING, FIELD.INTEGER, FIELD.LONG, FIELD.BIG_DECIMAL
        ].indexOf(record.type) !== -1
      },
      options: {
      },
    },
    {
      title: '移动端',
      field: 'cfExtend_mobile',
      component: COMPO.G_SWITCH,
      placeholder: '移动端',
      getValue: function(record){
        return !!_.get(record, 'cfExtend.mobile')
      },
      setValue: function(record, fieldValue){
        _.set(record, 'cfExtend.mobile', fieldValue)
      },
      isActived: function(record){
        let isAction = ['action'].indexOf(record.field) !== -1
        let isLinkRender = _.get(record, 'cfExtend.renderType') === 'linkRender'
        return !(isAction || isLinkRender || [FIELD.FILE, FIELD.PICTURE, FIELD.V_ACTION].indexOf(record.type) !== -1)
      },
      options: {
      },
    },
    {
      title: '引用配置',
      field: 'cfExtend_relCols',
      component: COMPO.G_REF_EDITOR,
      placeholder: '请定义引用配置',
      options: {
      },
      isActived: function(record){
        return [FIELD.MANY_TO_ONE, FIELD.ONE_TO_MANY].indexOf(record.type) !== -1
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
  ],
}

export default configObj