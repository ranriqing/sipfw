import _ from '@/utils/tiny_lodash'

import {COMPO, OTHER} from '@/constants'
import { domains } from '@/utils/util'

let configObj = {
  /* 新增弹窗 */
  addConfig: {
    modal: true,
    title: '新增Tab',
    submitText: '保存',
    popWidth: 700,
    formFields: [
      {
        title: '名 称',
        field: 'title',
        component: COMPO.G_INPUT,
        placeholder: '请输入Tab名称',
        options: {
          rules: [{ required: true, message: '请输入Tab名称' }]
        },
      },
      {
        title: '类 型',
        field: 'component',
        component: COMPO.G_SELECT,
        placeholder: '请选择Tab类型',
        options: {
          rules: [{ required: true, message: '请选择Tab类型' }]
        },
        setValue: function(record, fieldValue){
          // 系统内置Tab页
          if(_.isNaN(parseInt(fieldValue))){
            record['component'] = fieldValue
          }else{
            let relFieldId = parseInt(fieldValue)
            let relMetaDomainField = domains.metaDomainField(relFieldId)
            let relMetaDomain = domains.metaDomain(relMetaDomainField?.metaDomainId)
            if(relMetaDomainField){
              record['component'] = COMPO.PART_RELATIONS
              record['metaDomainId'] = relMetaDomain['id']                      // 组件的metaDomainId(从选中字段获取)
              record['extend'] = {                                              // 关联相关信息存储到组件extend
                [OTHER.REL_DOMAIN_ID]: relMetaDomain['id'],                     // 关联的参数1
                [OTHER.REL_DOMAIN_NAME]: relMetaDomain['domainName'],           // 关联的参数2
                [OTHER.REL_DOMAIN_FIELD_ID]: relMetaDomainField['id'],          // 关联的参数3
                [OTHER.REL_DOMAIN_FIELD_NAME]: relMetaDomainField['fieldName'], // 关联的参数4
              }
            }
          }
        },
        extend: {
          allowClear: false,
          options: function(record, context){
            context || true
            // 基础Tab功能
            let optionsObj = {
              '附件Tab': 'GPartAttachment',
              '活动Tab': 'GPartActivity',
              '变更Tab': 'GPartChangeLog',
              // '评论Tab': 'GPartComment',
            }
            // 多对一Tab(与当前弹窗domainId相关的全部多对一字段)
            domains.many2OneFields(undefined, _.get(context, 'domainId')).forEach(option => {
              optionsObj[`${_.get(option, 'relMetaDomain.title')} (${option['title']})`] = option['id']
            })
            return optionsObj
          },
        },
      },
      {
        title: '备 注',
        field: 'remark',
        component: COMPO.G_TEXTAREA,
        placeholder: '请输入备注',
        options: {
        },
      }
    ],
  },
  /* 编辑弹窗 */
  editConfig: {
    title: '组件属性',
    submitText: '保存',
    popWidth: 340,
    layout: 'vertical',
    initValues: function(){
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
        },
      },
      {
        title: '引用操作',
        field: 'cfExtend_ops',
        component: COMPO.G_LABEL,
        options: {
        },
        isActived: function(record){
          return [COMPO.PART_RELATIONS].indexOf(record.component) !== -1
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
          return [COMPO.PART_RELATIONS].indexOf(record.component) !== -1
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
          return [COMPO.PART_RELATIONS].indexOf(record.component) !== -1
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
          return [COMPO.PART_RELATIONS].indexOf(record.component) !== -1
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
    ],
  },
}

export default configObj