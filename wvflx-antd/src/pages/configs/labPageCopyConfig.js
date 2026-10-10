import { COMPO } from '@/constants'

let configObj = {
  modal: true,
  title: '拷贝页面到视图',
  submitText: '确定',
  formFields: [
    {
      field: 'slot_content',
      component: COMPO.G_SLOT,
      options: {
      },
    },
  ],
}

export default configObj