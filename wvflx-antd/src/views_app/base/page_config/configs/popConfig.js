import _ from '@/utils/tiny_lodash'
import { COMPO } from '@/constants'
import {validators} from "@/utils/util";

export default {
  logoConfig: {
    modal: true,
    title: '上传Logo',
    submitText: '确认',
    popWidth: 700,
  },
  globalConfig: {
    modal: true,
    title: '配置条目',
    submitText: '确认',
    formFields: [
      {
        title: '配置值',
        field: 'value',
        component: COMPO.G_INPUT,
        placeholder: '请输入配置值',
        options: {
          rules: [{ required: true, message: '请输入配置值' }]
        },
      },
    ],
  },
  apiConfig: {
    modal: true,
    title: '配置条目',
    submitText: '确认',
    formFields: [
      {
        title: '随 机',
        field: 'slot_random',
        component: COMPO.G_SLOT,
        isActived: function(record, context){
          context || true
          return 'apiSecret' === record['key']
        },
      },
      {
        title: '配置值',
        field: 'value',
        component: COMPO.G_INPUT,
        placeholder: '请输入配置值',
        options: {
          rules: [{ required: false, message: '请输入配置值' }]
        },
      },
    ],
  },
  itemConfig: {
    title: '选项配置',
    submitText: '保存',
    popWidth: 340,
    layout: 'vertical',
    formFields: [
      /** 配置部分 */
      {
        title: '下拉选项',
        field: 'valueJson_options',
        component: COMPO.G_OPTIONS_EDITOR,
        placeholder: '请配置下拉选项',
        getValue: function(record){
          return _.get(record, 'valueJson.options')
        },
        setValue: function(record, fieldValue){
          _.set(record, 'valueJson.options', fieldValue)
        },
        options: {
        },
      },
    ],
  },
  scConfig: {
    title: '选项配置',
    cancelText: '关闭',
    modal: true,
    cancelBtn: true,
    submitBtn: false,
    layout: 'vertical',
    formFields: [
      /** 配置部分 */
      {
        title: '下拉选项',
        field: 'slot_elements',
        component: COMPO.G_SLOT,
        placeholder: '请配置下拉选项',
        options: {
        },
      },
    ],
  },
  cateConfig: {
    modal: true,
    submitText: '确认',
    formFields: function(record, context){
      context || true
      return [
        {
          title: '选项标题',
          field: 'title',
          component: COMPO.G_INPUT,
          placeholder: '请输入标题',
          options: {
            rules: [{ required: true, message: '请输入标题' }]
          },
        },
        {
          title: '选项标识',
          field: 'key',
          component: COMPO.G_INPUT,
          placeholder: '请输入选项标识',
          options: {
            rules: [
              { required: true, message: '请输入选项标识' },
              { validator: validators.engValidator, message: '只能输入英文字符' }
            ]
          },
          extend: {
            disabled: !!record.key,
          }
        },
        {
          title: '所属分类',
          field: 'sciScCategory',
          component: COMPO.G_SC_SELECT,
          placeholder: '请选择所属分类',
          options: {
            rules: [{ required: true, message: '请选择所属分类' }]
          },
          extend: {
            disabled: !record.key,
            scKey: 'sciScCategory'
          },
        },
        {
          title: '描述信息',
          field: 'remark',
          component: COMPO.G_TEXTAREA,
          placeholder: '请输入描述信息',
          options: {
          },
        }
      ]
    },
  },
  tagConfig: {
    modal: true,
    title: '标签',
    submitText: '确认',
    formFields: [
      {
        title: '标签名称',
        field: 'name',
        component: COMPO.G_INPUT,
        placeholder: '请输入标签名称',
        options: {
          rules: [{ required: true, message: '请输入标签名称' }]
        },
      },
      {
        title: '标签颜色',
        field: 'slot_color',
        component: COMPO.G_SLOT,
        options: {
        },
      },
    ],
  },
  mergeConfig: {
    modal: true,
    title: '合并标签',
    submitText: '确认',
    formFields: [
      {
        title: '合并到',
        field: 'slot_target',
        component: COMPO.G_SLOT,
        options: {
        },
      },
    ],
  },
}