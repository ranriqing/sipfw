import { COMPO } from '@/constants'
import { tools } from '@/utils/util'

export default {
  /* 查询部分 */
  queryConfig: {
    queryFields: [
      {
        title: '综 合',
        field: 'name|remark',
        component: COMPO.G_INPUT,
        placeholder: '请输入角色名、备注',
        extend: {
          operator: 'contains',
        }
      },
    ]
  },
  /* 表格部分 */
  tableConfig: {
    queryUri: '/gcsRole/index',
    saveUri: '/gcsRole/save',
    updateUri: '/gcsRole/update',
    deleteUri: '/gcsRole/delete',
    extend: {
      subjectNote: '角色',
      subjectField: 'name',
      addCaption: undefined,
      editCaption: undefined,
      addEnabled: true,
      editEnabled: true,
      deleteEnabled: true,
      batchEnabled: false,
    },
    tableColumns: function(){
      return tools.isApply95()?[
        {
          title: '角色名',
          field: 'name',
          component: 'linkRender',
        },
        {
          title: '分 级',
          field: 'type',
          component: 'typeRender',
        },
        {
          title: '角色视图',
          field: 'viewId',
          component: 'viewRender',
        },
        {
          title: '备 注',
          field: 'remark'
        },
      ]:[
        {
          title: '角色名',
          field: 'name',
          component: 'linkRender',
        },
        {
          title: '分 级',
          field: 'type',
          component: 'typeRender',
        },
        {
          title: '角色视图',
          field: 'viewId',
          component: 'viewRender',
        },
        {
          title: '备 注',
          field: 'remark'
        },
        {
          title: '创建时间',
          field: 'dateCreated',
          extend: JSON.stringify({
            sorter: true
          }),
        },
        {
          title: '最近更新',
          field: 'lastUpdated',
          extend: JSON.stringify({
            sorter: true
          }),
        },
        {
          title: '操 作',
          field: 'ops',
          component: 'actionRender',
          extend: JSON.stringify({
            align: "center",
          }),
        }
      ]
    }
  }
}