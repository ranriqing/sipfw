import { COMPO } from '@/constants'

let configObj = {
  addConfig: {
    modal: true,
    popWidth: 500,
    title: '添加选项',
    submitText: '保存',
    layout: 'vertical',
    formFields: [
      {
        title: '父选项',
        field: 'nonsense-parent',
        component: COMPO.G_ALERT,
        placeholder: '请输入父选项',
        isActived: function(record, context){
          return !!context.id
        },
        getValue: function(record, context){
          return context['title']
        },
        options: {
        },
      },
      {
        title: '选项名称',
        field: 'title',
        component: COMPO.G_INPUT,
        placeholder: '请输入选项名称',
        options: {
          rules: [
            { required: true, message: '请输入选项名称' },
          ]
        },
      },
      {
        title: '选项描述',
        field: 'description',
        component: COMPO.G_TEXTAREA,
        placeholder: '请输入选项描述',
        options: {
          rules: [
            { required: false, message: '请输入选项描述' },
          ]
        },
      },
    ],
  },
  editConfig: {
    modal: true,
    popWidth: 500,
    title: '编辑选项',
    submitText: '更新',
    layout: 'vertical',
    formFields: [
      {
        title: '父选项',
        field: 'nonsense-parent',
        component: COMPO.G_ALERT,
        placeholder: '请输入父选项',
        isActived: function(record, context){
          return !!context.id
        },
        getValue: function(record, context){
          return context['title']
        },
        options: {
        },
      },
      {
        title: '选项名称',
        field: 'title',
        component: COMPO.G_INPUT,
        placeholder: '请输入选项名称',
        options: {
          rules: [
            { required: true, message: '请输入选项名称' },
          ]
        },
      },
      {
        title: '选项描述',
        field: 'description',
        component: COMPO.G_TEXTAREA,
        placeholder: '请输入选项描述',
        options: {
          rules: [
            { required: false, message: '请输入选项描述' },
          ]
        },
      },
    ],
  },
  /* 其它配置 */
  otherConfig: {
    tagStyles: [
      {color: 'red', caption: '红色'}, {color: 'orange', caption: '橙色'},
      {color: 'green', caption: '绿色'}, {color: 'blue', caption: '蓝色'},
      {color: 'purple', caption: '紫色'}, {color: '#ff3b30', caption: '深红'},
      {color: '#ff9500', caption: '深橙'}, {color: '#2fbc52', caption: '深绿'},
      {color: '#108ee9', caption: '深蓝'}, {color: '#9952e1', caption: '深紫'},
      {color: 'default', caption: '默认'}
    ],
  }
}

export default configObj