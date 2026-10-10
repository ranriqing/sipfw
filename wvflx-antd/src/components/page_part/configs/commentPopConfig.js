import { COMPO } from '@/constants'

export default {
  /* 编辑弹窗 */
  editConfig: {
    title: '编辑评论',
    submitText: '更新',
    formFields: [
      {
        title: '评论内容',
        field: 'content',
        component: COMPO.G_TEXTAREA,
        placeholder: '请输入评论内容',
        options: {
          rules: [{ required: true, message: '请输入评论内容' }]
        },
      },
    ],
  },
}