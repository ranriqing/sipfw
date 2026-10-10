import { COMPO } from '@/constants'
import { validators } from '@/utils/util'

export default {
  formFields: [
    {
      title: '用 户',
      field: 'slot_user',
      component: COMPO.G_SLOT,
      placeholder: '',
      options: {
      },
    },
    {
      title: '昵称',
      field: 'nickname',
      component: COMPO.G_INPUT,
      placeholder: '起个好听的名字',
      options: {
        rules: [{ required: true, message: '请输入昵称' }]
      },
    },
    {
      title: '手机号码',
      field: 'mobile',
      component: COMPO.G_INPUT,
      placeholder: '请输入手机号码',
      options: {
        rules: [
          { validator: validators.mobileValidator, message: '手机号码格式不正确' },
          { required: true, message: '请输入手机号码' }
        ]
      },
    },
    {
      title: '个性签名',
      field: 'remark',
      component: COMPO.G_TEXTAREA,
      placeholder: '请输入个性签名',
      options: {
      },
    }
  ],
}