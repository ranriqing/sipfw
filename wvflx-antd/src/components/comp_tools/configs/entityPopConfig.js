import { COMPO } from '@/constants'
import {validators} from "@/utils/util";

export default {
  /* 新增弹窗 */
  addConfig: {
    title: '新增数据表',
    submitText: '保存',
    popWidth: 660,
    formFields: [
      {
        title: '实 体',
        field: 'domainName',
        component: COMPO.G_INPUT,
        placeholder: '请输入表实体',
        options: {
          rules: [
            { required: true, message: '请输入表实体' },
            { validator: validators.engValidator, message: '只能输入英文字符' }
          ]
        },
      },
      {
        title: '名 称',
        field: 'title',
        component: COMPO.G_INPUT,
        placeholder: '请输入表名称',
        options: {
          rules: [{ required: true, message: '请输入表名称' }]
        },
      },
      {
        title: '分 类',
        field: 'sciDomainCategory',
        component: 'g-sc-select',
        placeholder: '请选择分类',
        extend: {
          scKey: 'sciDomainCategory',
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
    title: '编辑数据表',
    popWidth: 660,
    formFields: [
      {
        title: '实 体',
        field: 'domainName',
        component: COMPO.G_TEXT,
        placeholder: '请输入表实体',
        options: {
          rules: [{ required: true, message: '请输入表实体' }]
        },
      },
      {
        title: '名 称',
        field: 'title',
        component: COMPO.G_INPUT,
        placeholder: '请输入表名称',
        options: {
          rules: [{ required: true, message: '请输入表名称' }]
        },
      },
      {
        title: '分 类',
        field: 'sciDomainCategory',
        component: 'g-sc-select',
        placeholder: '请选择分类',
        extend: {
          scKey: 'sciDomainCategory',
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
}