import { COMPO } from '@/constants'

export default {
  title: '配置外部链接',
  modal: true,
  layout: 'vertical',
  formFields: [
    {
      title: '卡片标题',
      field: 'title',
      component: COMPO.G_INPUT,
      placeholder: '请输入卡片标题',
      options: {
      },
    },
    {
      title: '链接列表',
      field: 'links',
      component: COMPO.G_SLOT,
      options: {
      },
      // 链接列表由 QuickLinks 的 links slot 内 draggable 就地增删/排序，
      // 不通过表单回写，避免 form 值覆盖已编辑内容
      getValue: function(record) {
        return record.links || []
      },
      setValue: function() {
        /* no-op */
      },
    },
  ],
}
