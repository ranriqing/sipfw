import { COMPO } from '@/constants'
import { tools, validators, stores, converters } from '@/utils/util'
import partConfig from './userPartConfig'

let configObj = {
  /* 快捷新增 */
  ccQuickAddConfig: {
    title: '快捷添加',
    submitText: '保存',
    formFields: [
      {
        title: '工 号',
        field: 'ccJobNum',
        component: 'g-input',
        placeholder: '工号或工号范围(eg: 1001, 1001-1005)',
        extra: '工号格式为3到8位数字, 创建的账号密码与工号相同',
        options: {
          rules: [
            { required: true, message: '请输入工号' },
            {
              validator: function(rule,value,callback){
                if (!value || new RegExp(/^([0-9]){3,8}|([0-9]){3,8}-([0-9]){3,8}$/).test(value)){
                  callback();
                }else{
                  callback("请输入3到8位数值或数值范围");
                }
              },
              message: '必须为3到8位数字或数值范围'
            }
          ]
        },
      },
      {
        title: '角 色',
        field: 'gcsRoleId',
        component: COMPO.G_REF_DOMAIN,
        placeholder: '请选择承担角色',
        options: {
          rules: [{ required: true, message: '请选择承担角色' }]
        },
        extend: {
          showUri: '/gcsRole/show',
          indexUri: '/gcsRole/index',
          relQueryOn: true,
          relQueryField: 'name',
        }
      },
      {
        title: '部 门',
        field: 'gcsGroupId',
        component: COMPO.G_REF_GROUP,
        placeholder: '请选择所属部门',
        options: {
          rules: [{ required: true, message: '请选择所属部门' }]
        },
      },
      {
        title: '选 线',
        field: 'gatewayStrategy',
        component: COMPO.G_SELECT,
        placeholder: '选线策略',
        extra: '呼出时选择外线的策略',
        cols: 12,
        options: {
        },
        extend: {
          options: function(record, context){
            context || true
            return converters.reverseObj(partConfig.otherConfig.strategyOptions)
          }
        },
      },
      {
        title: '外 线',
        field: 'strategyPattern',
        component: COMPO.G_INPUT,
        placeholder: 'eg: 60840001,60840002',
        extra: '优先级高于话机外线',
        options: {
        },
        cols: 12
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
  /* 新增弹窗 */
  addConfig: {
    title: '用户添加',
    submitText: '保存',
    initValues: function(){
      return {
        passwordHash: tools.randomString(8),
        mobile: `190${tools.randomString(8, '1234567890')}`,
      }
    },
    formFields: function(){
      let fieldsArray = [
        {
          title: '基本信息',
          field: 'basic-panel',
          component: COMPO.G_PANEL,
          options: {
          },
        },
        {
          title: '用 户',
          field: 'username',
          component: COMPO.G_INPUT,
          placeholder: '请输入用户名',
          extra: '用户名不能重复，且只能为字母或数字的组合',
          options: {
            rules: [
              { required: true, message: '请输入用户名' },
              { validator: validators.engValidator, message: '用户名格式不正确' }
            ]
          },
        },
        {
          title: '昵 称',
          field: 'nickname',
          component: COMPO.G_INPUT,
          placeholder: '请输入用户昵称',
          options: {
            rules: [{ required: true, message: '请输入用户昵称' }]
          },
          cols: 12
        },
        {
          title: '手 机',
          field: 'mobile',
          component: COMPO.G_INPUT,
          placeholder: '请输入手机号码',
          options: {
            rules: [
              { required: true, message: '请输入手机号码' },
              { validator: validators.mobileValidator, message: '手机号码格式不正确' }
            ]
          },
          cols: 12
        },
        {
          title: '密 码',
          field: 'passwordHash',
          component: COMPO.G_INPUT,
          placeholder: '请输入用户密码',
          options: {
            rules: [{ required: true, message: '请输入用户密码' }]
          },
        },
        {
          title: '角 色',
          field: 'gcsRoleId',
          component: COMPO.G_REF_DOMAIN,
          placeholder: '请选择承担角色',
          options: {
            rules: [{ required: true, message: '请选择承担角色' }]
          },
          extend: {
            showUri: '/gcsRole/show',
            indexUri: '/gcsRole/index',
            relQueryOn: true,
            relQueryField: 'name',
          }
        },
        {
          title: '部 门',
          field: 'gcsGroupId',
          component: COMPO.G_REF_GROUP,
          placeholder: '请选择所属部门',
          options: {
            rules: [{ required: true, message: '请选择所属部门' }]
          },
        },
      ]
      if(tools.isCallCenter()){
        fieldsArray.push(
          {
            title: '呼叫中心',
            field: 'cc-panel',
            component: COMPO.G_PANEL,
            options: {
            },
          },
          {
            title: '工 号',
            field: 'ccJobNum',
            component: COMPO.G_INPUT,
            placeholder: '请输入用户工号',
            extra: '工号格式为3到8位数字',
            options: {
              rules: [
                { validator: validators.ccJobNumValidator, message: '必须为3到8位数字' }
              ]
            },
            cols: 12
          },
          {
            title: '密 码',
            field: 'ccJobPwd',
            component: COMPO.G_INPUT,
            placeholder: '请输入工号密码',
            extra: '密码格式为3到8位数字',
            options: {
              rules: [
                { validator: validators.ccJobPwdValidator, message: '必须为3到8位数字' }
              ]
            },
            cols: 12
          },
          {
            title: '选 线',
            field: 'gatewayStrategy',
            component: COMPO.G_SELECT,
            placeholder: '选线策略',
            extra: tools.isApply95()?undefined:'呼出时选择外线的策略',
            cols: 12,
            options: {
            },
            extend: {
              options: function(record, context){
                context || true
                return converters.reverseObj(partConfig.otherConfig.strategyOptions)
              }
            },
          },
          {
            title: '外 线',
            field: 'strategyPattern',
            component: COMPO.G_INPUT,
            placeholder: 'eg: 60840001,60840002',
            extra: tools.isApply95()?undefined:'优先级高于话机外线',
            cols: 12,
            options: {
            },
          },
        )
      }
      fieldsArray.push(
        {
          title: '备注信息',
          field: 'remark-panel',
          component: COMPO.G_PANEL,
          options: {
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
      )
      return fieldsArray
    },
  },
  /* 编辑弹窗 */
  editConfig: {
    title: '用户编辑',
    submitText: '更新',
    formFields: function() {
      let fieldsArray = [
        {
          title: '基本信息',
          field: 'basic-panel',
          component: COMPO.G_PANEL,
          options: {
          },
        },
        {
          title: '用 户',
          field: 'username',
          component: COMPO.G_INPUT,
          placeholder: '请输入用户名',
          extra: '用户名不能重复，且只能为字母或数字的组合',
          options: {
            rules: [
              { required: true, message: '请输入用户名' },
              { validator: validators.engValidator, message: '用户名格式不正确' }
            ]
          },
        },
        {
          title: '昵 称',
          field: 'nickname',
          component: COMPO.G_INPUT,
          placeholder: '请输入用户昵称',
          options: {
            rules: [{ required: true, message: '请输入用户昵称' }]
          },
          cols: 12
        },
        {
          title: '手 机',
          field: 'mobile',
          component: COMPO.G_INPUT,
          placeholder: '请输入手机号码',
          options: {
            rules: [
              { required: true, message: '请输入手机号码' },
              { validator: validators.mobileValidator, message: '手机号码格式不正确' }
            ]
          },
          cols: 12
        },
        {
          title: '角 色',
          field: 'gcsRoleId',
          component: COMPO.G_REF_DOMAIN,
          placeholder: '请选择承担角色',
          options: {
            rules: [{ required: true, message: '请选择承担角色' }]
          },
          extend: {
            showUri: '/gcsRole/show',
            indexUri: '/gcsRole/index',
            relQueryOn: true,
            relQueryField: 'name',
          }
        },
        {
          title: '部 门',
          field: 'gcsGroupId',
          component: COMPO.G_REF_GROUP,
          placeholder: '请选择所属部门',
          options: {
            rules: [{ required: true, message: '请选择所属部门' }]
          },
        },
        {
          title: '账号启用',
          field: 'isActive',
          component: 'g-radio-group',
          placeholder: '请选择账号启用',
          isActived: function(record, context){
            record || context || true
            return stores.isSuperAdmin() && record.id > 0
          },
          options: {
            rules: [{ required: true, message: '请选择账号启用' }]
          },
          extend: {
            options: {
              启用: true,
              禁用: false,
            },
          }
        },
      ]
      if(tools.isCallCenter()){
        fieldsArray.push(
          {
            title: '呼叫中心',
            field: 'cc-panel',
            component: COMPO.G_PANEL,
            options: {
            },
          },
          {
            title: '工 号',
            field: 'ccJobNum',
            component: COMPO.G_INPUT,
            placeholder: '请输入用户工号',
            extra: '工号格式为3到8位数字',
            options: {
              rules: [
                { validator: validators.ccJobNumValidator, message: '必须为3到8位数字' }
              ]
            },
            cols: 12
          },
          {
            title: '密 码',
            field: 'ccJobPwd',
            component: COMPO.G_INPUT,
            placeholder: '请输入工号密码',
            extra: '密码格式为3到8位数字',
            options: {
              rules: [
                { validator: validators.ccJobPwdValidator, message: '必须为3到8位数字' }
              ]
            },
            cols: 12
          },
          {
            title: '选 线',
            field: 'gatewayStrategy',
            component: COMPO.G_SELECT,
            placeholder: '选线策略',
            extra: '呼出时选择外线的策略',
            cols: 12,
            options: {
            },
            extend: {
              options: function(record, context){
                context || true
                return converters.reverseObj(partConfig.otherConfig.strategyOptions)
              }
            },
          },
          {
            title: '外 线',
            field: 'strategyPattern',
            component: COMPO.G_INPUT,
            placeholder: 'eg: 60840001,60840002',
            extra: '优先级高于话机外线',
            cols: 12,
            options: {
            },
          },
        )
        if(tools.isApply95()){
          fieldsArray.push({
            title: '呼叫权限',
            field: 'callPermission',
            component: COMPO.G_RADIO_GROUP,
            placeholder: '请选择呼叫权限',
            options: {
              rules: [{ required: true, message: '请选择呼叫权限' }]
            },
            extend: {
              options: {
                仅呼入: false,
                临时呼出: true,
              },
            }
          })
        }
      }
      fieldsArray.push(
        {
          title: '备注信息',
          field: 'remark-panel',
          component: COMPO.G_PANEL,
          options: {
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
      )
      return fieldsArray
    },
  },
  resetConfig:{
    modal: true,
    title: '重置密码',
    submitText: '更新',
    formFields: [
      {
        title: '用 户',
        field: 'nonsense-username',
        component: COMPO.G_ALERT,
        placeholder: '请输入用户名',
        getValue: function(record, context){
          return context['title']
        },
        options: {
        },
      },
      {
        title: '密 码',
        field: 'newPassword',
        component: COMPO.G_INPUT,
        placeholder: '请输入新密码',
        options: {
          rules: [{ required: true, message: '请输入新密码' }]
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
  /* 批量弹窗 */
  batchConfig: {
    modal: true,
    title: '批量操作',
    submitText: '更新',
    formFields: function(){
      let fieldsArray = [
        {
          title: '基本信息',
          field: 'basic-panel',
          component: COMPO.G_PANEL,
          options: {
          },
        },
        {
          title: '用户角色',
          field: 'gcsRoleId',
          component: COMPO.G_REF_DOMAIN,
          placeholder: '请选择承担角色',
          options: {
            rules: [{ required: true, message: '请选择承担角色' }]
          },
          extend: {
            showUri: '/gcsRole/show',
            indexUri: '/gcsRole/index',
            relQueryOn: false,
            relQueryField: 'name',
          }
        },
        {
          title: '所属部门',
          field: 'gcsGroupId',
          component: COMPO.G_REF_GROUP,
          placeholder: '请选择所属部门',
          options: {
            rules: [{ required: true, message: '请选择所属部门' }]
          },
        },
      ]
      if(tools.isCallCenter()){
        fieldsArray.push(
          {
            title: '呼叫中心',
            field: 'cc-panel',
            component: COMPO.G_PANEL,
            options: {
            },
          },
          {
            title: '工号密码',
            field: 'ccJobPwd',
            component: COMPO.G_INPUT,
            placeholder: '请输入工号密码',
            extra: '密码格式为3到8位数字',
            options: {
              rules: [
                { validator: validators.ccJobPwdValidator, message: '必须为3到8位数字' }
              ]
            },
          },
          {
            title: '选线策略',
            field: 'gatewayStrategy',
            component: COMPO.G_SELECT,
            placeholder: '选线策略',
            extra: '呼出时选择外线的策略',
            options: {
            },
            extend: {
              options: function(record, context){
                context || true
                return converters.reverseObj(partConfig.otherConfig.strategyOptions)
              }
            },
          },
          {
            title: '策略外线',
            field: 'strategyPattern',
            component: COMPO.G_INPUT,
            placeholder: 'eg: 60840001,60840002',
            extra: '优先级高于话机外线',
            options: {
            },
          },
        )
      }
      return fieldsArray
    },
  },
}

export default configObj