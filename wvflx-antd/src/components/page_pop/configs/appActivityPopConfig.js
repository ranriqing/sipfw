import _ from '@/utils/tiny_lodash'
import moment from "moment"

import { stores } from '@/utils/util'
import {COMPO, DATE} from '@/constants'

let configObj = {
  popConfig: {
    modal: true,
    popWidth: 950,
    title: '添加日程',
    initValues: function(){
      return {
        sciScheduleType: 101,
        dueDate: moment(),
        assigneeId: _.get(stores.currentUser(), 'id')
      }
    },
    formFields: [
      {
        title: '日程类型',
        field: 'sciScheduleType',
        component: COMPO.G_SC_SELECT,
        cols: 12,
        placeholder: '请选择日程类型',
        options: {
          rules: [{ required: true, message: '请选择日程类型' }]
        },
        extend: {
          scKey: 'sciScheduleType'
        },
      },
      {
        title: '截止日期',
        field: 'dueDate',
        component: COMPO.G_DATE_PICKER,
        cols: 12,
        placeholder: '请选择截止日期',
        options: {
          rules: [{ required: true, message: '请选择截止日期' }]
        },
        extend: {
          allowClear: false,
          format: DATE.YYYYMMDD,
          initValue: 'now'
        }
      },
      {
        title: '日程标题',
        field: 'title',
        component: COMPO.G_INPUT,
        cols: 12,
        placeholder: '请输入日程标题',
        options: {
        },
      },
      {
        title: '负责人',
        field: 'assigneeId',
        component: COMPO.G_REF_USER,
        cols: 12,
        placeholder: '请选择负责人',
        options: {
          rules: [
            { required: true, message: '请选择负责人' },
          ]
        },
      },
      {
        title: undefined,
        field: 'detail',
        component: COMPO.G_TIP_TAP_EDITOR,
        placeholder: '请输入日程详情',
        cols: 24,
        options: {
          rules: [
            { required: false, message: '请输入日程详情' },
          ]
        },
        extend: {
          mode: 'simple',
        },
      },
    ],
  },
}

export default configObj