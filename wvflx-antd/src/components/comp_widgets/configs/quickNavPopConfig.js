import { COMPO } from '@/constants'

export default {
  title: '配置快捷导航',
  modal: true,
  popWidth: 640,
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
      title: '导航入口',
      field: 'items',
      component: COMPO.G_SLOT,
      options: {
      },
      // 入口列表由 QuickNavs 的 items slot 内 draggable 就地增删/排序，
      // 不通过表单回写，避免 form 值覆盖已编辑内容
      getValue: function(record) {
        return record.items || []
      },
      setValue: function() {
        /* no-op */
      },
    },
  ],
}
