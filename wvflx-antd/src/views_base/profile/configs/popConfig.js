import { COMPO } from '@/constants'

export default {
  avatarConfig: {
    modal: true,
    title: '更新头像',
    submitText: '确认',
    popWidth: 700,
  },
  resetConfig: {
    modal: true,
    title: '重置密码',
    submitText: '重置',
    formFields: [
      {
        title: '用 户',
        field: 'username',
        component: COMPO.G_ALERT,
        placeholder: '请输入用户名',
        options: {
        },
      },
      {
        title: '新密码',
        field: 'password1',
        component: COMPO.G_INPUT,
        placeholder: '请输入新密码',
        options: {
          rules: [{ required: true, message: '请输入新密码' }]
        },
        extend: {
          inputType: 'password'
        }
      },
      {
        title: '再次输入',
        field: 'password2',
        component: COMPO.G_INPUT,
        placeholder: '请再次输入新密码',
        options: {
          rules: [{ required: true, message: '请再次输入新密码' }]
        },
        extend: {
          inputType: 'password'
        }
      },
    ],
  }
}