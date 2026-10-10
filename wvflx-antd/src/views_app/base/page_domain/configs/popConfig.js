import { COMPO } from '@/constants'

export default {
  /* 新增弹窗 */
  addConfig: {
    title: '新增实体',
    submitText: '保存',
    formFields: [
      {
        title: '表名称',
        field: 'title',
        component: COMPO.G_INPUT,
        placeholder: '请输入表名称',
        options: {
          rules: [{ required: true, message: '请输入表名称' }]
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
    title: '编辑实体',
    formFields: [
      {
        title: '表名称',
        field: 'title',
        component: COMPO.G_INPUT,
        placeholder: '请输入表名称',
        options: {
          rules: [{ required: true, message: '请输入表名称' }]
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