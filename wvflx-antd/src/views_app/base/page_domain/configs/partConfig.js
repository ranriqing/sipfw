import { COMPO } from '@/constants'

export default {
  /* 查询配置 */
  queryConfig: {
    queryFields: [
      {
        field: 'title|domainName|remark',
        title: '综 合',
        component: COMPO.G_INPUT,
        placeholder: '请输入表名称、表标识、备注',
        options: {
        }
      },
    ],
  },
  /* 表格部分 */
  tableConfig: {
    tableColumns: function(){
      return [
        {
          title: '表名称',
          field: 'title',
          component: 'linkRender',
          extend: JSON.stringify({
            sorter: true,
          }),
        },
        {
          title: '表标识',
          field: 'domainName'
        },
        {
          title: '备 注',
          field: 'remark'
        },
        {
          title: '最近更新',
          field: 'lastUpdated',
          extend: JSON.stringify({
            sorter: true,
          }),
        },
        {
          title: '操 作',
          field: 'action',
          component: 'actionRender',
          extend: JSON.stringify({
            align: "center",
          }),
        }
      ]
    }
  },
}