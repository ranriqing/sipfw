import { tools } from '@/utils/util'
import { COMPO } from '@/constants'

let configObj = {
  modal: true,
  title: '选择页面',
  submitText: '确定',
  popWidth: 950,
  bodyStyle: {
    'height': '550px',
    'overflow-y':'auto'
  },
  formFields: function(){
    let pageFields = [
      /* ----- 基础部分 ----- */
      {
        title: '模板页面',
        field: 'tpl-panel',
        component: COMPO.G_PANEL,
        cols: 12,
        options: {
        },
        extend: {
          icon: 'appstore',
          collapsible: false,
        },
      },
      {
        title: '系统管理',
        field: 'sys-panel',
        component: COMPO.G_PANEL,
        cols: 12,
        options: {
        },
        extend: {
          icon: 'setting',
          collapsible: false,
        },
      },
      {
        field: 'tplPages',
        component: COMPO.G_SLOT,
        cols: 12,
        options: {
        },
      },
      {
        field: 'sysPages',
        component: COMPO.G_SLOT,
        cols: 12,
        options: {
        },
      },
    ]
    /* ----- 呼叫中心 ----- */
    if(tools.isCallCenter()){
      pageFields.push(
        {
          title: '呼叫中心',
          field: 'cc-panel',
          component: COMPO.G_PANEL,
          options: {
          },
          extend: {
            icon: 'customer-service',
            collapsible: false,
          },
        },
        {
          field: 'ccPages',
          component: COMPO.G_SLOT,
          options: {
          },
        },
      )
    }
    /* ----- 实用工具 ----- */
    pageFields.push(
      {
        title: '实用工具',
        field: 'other-panel',
        component: COMPO.G_PANEL,
        options: {
        },
        extend: {
          icon: 'bulb'
        },
      },
      {
        field: 'otherPages',
        component: COMPO.G_SLOT,
        options: {
        },
      },
    )
    return pageFields
  },
}

export default configObj