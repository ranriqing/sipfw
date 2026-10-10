import _ from '@/utils/tiny_lodash'
import { COMPO, DATE, FIELD } from '@/constants'
import {converters, validators} from '@/utils/util'
import fieldPartConfig from './fieldPartConfig'

export default {
  /* 新增弹窗 */
  addConfig: {
    modal: true,
    title: '新增字段',
    formFields: [
      {
        title: '字 段',
        field: 'fieldName',
        component: COMPO.G_INPUT,
        placeholder: '请输入字段',
        extra: '提示：字段提交后不能更改',
        options: {
          rules: [
            { required: true, message: '请输入字段' },
            { validator: validators.engValidator, message: '只能输入英文字符' }
          ]
        },
      },
      {
        title: '名 称',
        field: 'title',
        component: COMPO.G_INPUT,
        placeholder: '请输入字段名',
        options: {
          rules: [{ required: true, message: '请输入字段名' }]
        },
      },
      {
        title: '类 型',
        field: 'fieldType',
        component: COMPO.G_SELECT,
        placeholder: '请选择字段类型',
        extra: '提示：字段类型选定后不能更改',
        options: {
          rules: [{ required: true, message: '请选择字段类型' }]
        },
        change: function(record, context, value){
          context['fieldType'] = value
        },
        extend: {
          allowClear: false,
          options: function(record, context){
            context || true
            return converters.reverseObj(fieldPartConfig.otherConfig.fieldTypeOptions)
          }
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
  },/* 编辑弹窗 */
  editConfig: {
    modal: true,
    title: '编辑字段',
    formFields: [
      {
        title: '名 称',
        field: 'title',
        component: COMPO.G_INPUT,
        placeholder: '请输入字段名',
        options: {
          rules: [{ required: true, message: '请输入字段名' }]
        },
      },
      {
        title: '备 注',
        field: 'remark',
        component: COMPO.G_TEXTAREA,
        placeholder: '请输入备注',
        options: {

        },
      },
      {
        title: '全局搜索',
        field: 'globalSearch',
        component: COMPO.G_SWITCH,
        placeholder: '开启后该字段可被全局搜索',
        isActived: function(record){
          return [FIELD.STRING].indexOf(record.fieldType) !== -1
        },
        options: {
        },
      }
    ],
  },
  /* 关联弹窗 */
  relationConfig: {
    modal: true,
    title: '关联字段',
    submitText: '确定',
    formFields: [
      {
        title: '字 段',
        field: 'fieldName',
        component: COMPO.G_INPUT,
        placeholder: '请输入字段',
        extra: '提示：字段提交后不能更改',
        options: {
          rules: [
            { required: true, message: '请输入字段' },
            { validator: validators.engValidator, message: '只能输入英文字符' }
          ]
        },
      },
      {
        title: '名 称',
        field: 'title',
        component: COMPO.G_INPUT,
        placeholder: '请输入字段名',
        options: {
          rules: [{ required: true, message: '请输入字段名' }]
        },
      },
      {
        title: '关联类型',
        field: 'relType',
        component: COMPO.G_NONE,
        getValue: function(record){
          return _.get(record, 'relType')
        },
        setValue: function(record, fieldValue){
          _.set(record, 'relType', fieldValue)
        },
      },
      {
        title: '关联对象',
        field: 'relDomainOrFieldId',
        component: COMPO.G_SLOT,
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
  /* 约束弹窗 */
  constraintConfig: {
    drawer: true,
    title: '字段配置',
    submitText: '保存',
    popWidth: 340,
    layout: 'vertical',
    formFields: [
      /** 约束部分 */
      {
        title: '必填字段',
        field: 'constraintJson_nullable',
        component: COMPO.G_SWITCH,
        placeholder: '必填字段',
        isActived: function(record){
          return [FIELD.SEQUENCE].indexOf(record.fieldType) === -1
        },
        getValue: function(record){
          return !_.get(record, 'constraintJson.nullable')
        },
        setValue: function(record, fieldValue){
          _.set(record, 'constraintJson.nullable', !fieldValue)
        },
        options: {
        },
      },
      {
        title: '最小长度',
        field: 'constraintJson_minSize',
        component: COMPO.G_NUMBER_INTEGER,
        placeholder: '请输入最小长度',
        isActived: function(record){
          return [FIELD.STRING].indexOf(record.fieldType) !== -1
        },
        getValue: function(record){
          return _.get(record, 'constraintJson.minSize')
        },
        setValue: function(record, fieldValue){
          _.set(record, 'constraintJson.minSize', fieldValue)
        },
        options: {
        },
      },
      {
        title: '最大长度',
        field: 'constraintJson_maxSize',
        component: COMPO.G_NUMBER_INTEGER,
        placeholder: '请输入最大长度',
        isActived: function(record){
          return [FIELD.STRING].indexOf(record.fieldType) !== -1
        },
        getValue: function(record){
          return _.get(record, 'constraintJson.maxSize')
        },
        setValue: function(record, fieldValue){
          _.set(record, 'constraintJson.maxSize', fieldValue)
        },
        options: {
        },
      },
      {
        title: '最小值',
        field: 'constraintJson_min',
        component: COMPO.G_NUMBER_INTEGER,
        placeholder: '请输入最小值',
        isActived: function(record){
          return [FIELD.INTEGER, FIELD.LONG, FIELD.BIG_DECIMAL].indexOf(record.fieldType) !== -1
        },
        getValue: function(record){
          return _.get(record, 'constraintJson.min')
        },
        setValue: function(record, fieldValue){
          _.set(record, 'constraintJson.min', fieldValue)
        },
        options: {
        },
      },
      {
        title: '最大值',
        field: 'constraintJson_max',
        component: COMPO.G_NUMBER_INTEGER,
        placeholder: '请输入最大值',
        isActived: function(record){
          return [FIELD.INTEGER, FIELD.LONG, FIELD.BIG_DECIMAL].indexOf(record.fieldType) !== -1
        },
        getValue: function(record){
          return _.get(record, 'constraintJson.max')
        },
        setValue: function(record, fieldValue){
          _.set(record, 'constraintJson.max', fieldValue)
        },
        options: {
        },
      },
      /** 配置部分 */
      {
        title: '序列格式',
        field: 'extendJson_seqFormat',
        component: COMPO.G_INPUT,
        placeholder: '请配置序列格式，eg: {yy}CHN%2$05d',
        isActived: function(record){
          return [FIELD.SEQUENCE].indexOf(record.fieldType) !== -1
        },
        getValue: function(record){
          return _.get(record, 'extendJson.seqFormat')
        },
        setValue: function(record, fieldValue){
          _.set(record, 'extendJson.seqFormat', fieldValue)
        },
        options: {
        },
      },
      {
        title: '字典层级',
        field: 'extendJson_depth',
        component: COMPO.G_NUMBER_INTEGER,
        placeholder: '请配置字典层级',
        isActived: function(record){
          return [FIELD.SC_EL].indexOf(record.fieldType) !== -1
        },
        getValue: function(record){
          return _.get(record, 'extendJson.depth')
        },
        setValue: function(record, fieldValue){
          _.set(record, 'extendJson.depth', fieldValue)
        },
        options: {
        },
        extend: {
          max: 3,
        }
      },
      {
        title: '字典Key',
        field: 'extendJson_scKey',
        component: COMPO.G_INPUT,
        placeholder: '请配置字典Key',
        isActived: function(record){
          return [FIELD.SC_EL].indexOf(record.fieldType) !== -1
        },
        getValue: function(record){
          return _.get(record, 'extendJson.scKey')
        },
        setValue: function(record, fieldValue){
          _.set(record, 'extendJson.scKey', fieldValue)
        },
        options: {
        },
      },
      {
        title: '日期格式',
        field: 'extendJson_format',
        component: COMPO.G_SELECT,
        placeholder: '请选择日期格式',
        isActived: function(record){
          return [FIELD.DATE].indexOf(record.fieldType) !== -1
        },
        getValue: function(record){
          return _.get(record, 'extendJson.format')
        },
        setValue: function(record, fieldValue){
          _.set(record, 'extendJson.format', fieldValue)
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
        title: '下拉选项',
        field: 'extendJson_options',
        component: COMPO.G_OPTIONS_EDITOR,
        placeholder: '请配置下拉选项',
        isActived: function(record){
          return [FIELD.SELECT].indexOf(record.fieldType) !== -1
        },
        getValue: function(record){
          return _.get(record, 'extendJson.options')
        },
        setValue: function(record, fieldValue){
          _.set(record, 'extendJson.options', fieldValue)
        },
        options: {
        },
      },
      /** 反向关联 */
      {
        title: '反向关联',
        field: 'inverseJson_inverseRelOn',
        component: COMPO.G_SWITCH,
        placeholder: '反向关联',
        isActived: function(record){
          return [FIELD.MANY_TO_ONE + 'none'].indexOf(record.fieldType) !== -1
        },
        change: function(record, context, value){
          context['inverseRelOn'] = value
        },
        getValue: function(record){
          return !!_.get(record, 'inverseJson.inverseRelOn')
        },
        setValue: function(record, fieldValue){
          _.set(record, 'inverseJson.inverseRelOn', fieldValue)
        },
        options: {
        },
      },
      {
        title: '反向关联类型',
        field: 'inverseJson_relDomainFieldType',
        component: COMPO.G_RADIO_GROUP,
        placeholder: '请配置反向关联类型',
        isActived: function(record, context){
          let contextRelOn = context['inverseRelOn']
          let inverseRelOn = !!_.get(record, 'inverseJson.inverseRelOn')
          return [FIELD.MANY_TO_ONE + 'none'].indexOf(record.fieldType) !== -1 && (_.isNil(contextRelOn)?inverseRelOn:contextRelOn)
        },
        getValue: function(record){
          return _.get(record, 'inverseJson.relDomainFieldType')
        },
        setValue: function(record, fieldValue){
          _.set(record, 'inverseJson.relDomainFieldType', fieldValue)
        },
        options: {
        },
        extend: {
          options: {
            '关联多个': 'many',
            '关联单个': 'one',
          }
        },
      },
      /** User 字段自动写入当前用户 */
      {
        title: '当前用户',
        field: 'extendJson_autoSpiUser',
        component: COMPO.G_SWITCH,
        placeholder: '新增数据时自动写入当前用户',
        isActived: function(record){
          return [FIELD.USER].indexOf(record.fieldType) !== -1
        },
        getValue: function(record){
          return !!_.get(record, 'extendJson.autoSpiUser')
        },
        setValue: function(record, fieldValue){
          _.set(record, 'extendJson.autoSpiUser', fieldValue)
        },
        options: {
        },
      },
      /** Group 字段自动写入当前部门 */
      {
        title: '当前部门',
        field: 'extendJson_autoSpiUserGroup',
        component: COMPO.G_SWITCH,
        placeholder: '新增数据时自动写入当前部门',
        isActived: function(record){
          return [FIELD.GROUP].indexOf(record.fieldType) !== -1
        },
        getValue: function(record){
          return !!_.get(record, 'extendJson.autoSpiUserGroup')
        },
        setValue: function(record, fieldValue){
          _.set(record, 'extendJson.autoSpiUserGroup', fieldValue)
        },
        options: {
        },
      },
      {
        title: '默认值',
        field: 'extendJson_defaultValue',
        component: COMPO.G_INPUT,
        placeholder: '请输入默认值',
        isActived: function(record){
          return [FIELD.STRING].indexOf(record.fieldType) !== -1
        },
        getValue: function(record){
          return _.get(record, 'extendJson.defaultValue')
        },
        setValue: function(record, fieldValue){
          _.set(record, 'extendJson.defaultValue', fieldValue)
        },
        options: {
        },
      },
      {
        title: '默认值',
        field: 'extendJson_defaultValue',
        component: COMPO.G_NUMBER_INTEGER,
        placeholder: '请输入默认值',
        isActived: function(record){
          return [FIELD.INTEGER, FIELD.LONG].indexOf(record.fieldType) !== -1
        },
        getValue: function(record){
          return _.get(record, 'extendJson.defaultValue')
        },
        setValue: function(record, fieldValue){
          _.set(record, 'extendJson.defaultValue', fieldValue)
        },
        options: {
        },
      },
      {
        title: '默认值',
        field: 'extendJson_defaultValue',
        component: COMPO.G_NUMBER_DECIMAL,
        placeholder: '请输入默认值',
        isActived: function(record){
          return [FIELD.BIG_DECIMAL].indexOf(record.fieldType) !== -1
        },
        getValue: function(record){
          return _.get(record, 'extendJson.defaultValue')
        },
        setValue: function(record, fieldValue){
          _.set(record, 'extendJson.defaultValue', fieldValue)
        },
        options: {
        },
      },
      {
        title: '默认值',
        field: 'extendJson_defaultValue',
        component: COMPO.G_SELECT,
        placeholder: '请选择默认值',
        isActived: function(record){
          return [FIELD.BOOLEAN].indexOf(record.fieldType) !== -1
        },
        getValue: function(record){
          return _.get(record, 'extendJson.defaultValue')
        },
        setValue: function(record, fieldValue){
          _.set(record, 'extendJson.defaultValue', fieldValue)
        },
        options: {
        },
        extend: {
          allowClear: true,
          options: {
            'true': true,
            'false': false,
          }
        },
      },
      {
        title: '默认值',
        field: 'extendJson_defaultValue',
        component: COMPO.G_SELECT,
        placeholder: '请选择默认值',
        isActived: function(record){
          return [FIELD.SELECT].indexOf(record.fieldType) !== -1
        },
        getValue: function(record){
          return _.get(record, 'extendJson.defaultValue')
        },
        setValue: function(record, fieldValue){
          _.set(record, 'extendJson.defaultValue', fieldValue)
        },
        options: {
        },
        extend: {
          allowClear: true,
          options: function(record, context){
            context || true
            let selectOptions = _.get(record, 'extendJson.options')
            if (!selectOptions) return {}
            if (Array.isArray(selectOptions)) {
              let obj = {}
              selectOptions.forEach(item => {
                if (item.label && item.value) {
                  obj[item.label] = item.value
                }
              })
              return obj
            }
            return selectOptions
          }
        },
      },
      {
        title: '默认值',
        field: 'extendJson_defaultValue',
        component: COMPO.G_SC_CASCADER,
        placeholder: '请选择默认值',
        isActived: function(record){
          return [FIELD.SC_EL].indexOf(record.fieldType) !== -1
        },
        getValue: function(record){
          return _.get(record, 'extendJson.defaultValue')
        },
        setValue: function(record, fieldValue){
          _.set(record, 'extendJson.defaultValue', fieldValue)
        },
        options: {
        },
        funcItem: function(item, record, context){
          context || true
          return {
            ...item,
            extend: {
              ...item.extend,
              scKey: _.get(record, 'extendJson.scKey'),
              depth: _.get(record, 'extendJson.depth'),
            },
          }
        },
      },
      {
        title: '默认值',
        field: 'extendJson_defaultValue',
        component: COMPO.G_SELECT,
        placeholder: '请选择默认日期',
        isActived: function(record){
          return [FIELD.DATE].indexOf(record.fieldType) !== -1
        },
        getValue: function(record){
          return _.get(record, 'extendJson.defaultValue')
        },
        setValue: function(record, fieldValue){
          _.set(record, 'extendJson.defaultValue', fieldValue)
        },
        options: {
        },
        extend: {
          allowClear: true,
          options: {
            '当日': 'today',
            '明日': 'tomorrow',
            '月初': 'startOfMonth',
            '月末': 'endOfMonth',
          }
        },
      },
    ],
  },
}