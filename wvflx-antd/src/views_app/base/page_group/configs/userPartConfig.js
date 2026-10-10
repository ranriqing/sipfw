import { tools } from '@/utils/util'
import { COMPO } from '@/constants'

export default {
  /* 查询配置 */
  queryConfig: {
    queryFields: [
      {
        title: '',
        field: 'slot_keyword',
        component: COMPO.G_SLOT,
      },
    ],
  },
  /* 表格配置 */
  tableConfig: {
    tableColumns: function(){
      let columnsArray = [
        {
          title: '角色/部门',
          field: 'gcsRole',
        },
        {
          title: '创建/更新',
          field: 'dateCreated',
        },
      ]
      if(tools.isCallCenter()){
        columnsArray.unshift(
          {
            title: '工号/线路',
            field: 'ccJobNum',
          },
        )
      }
      return columnsArray
    },
  },
  /* 其它配置 */
  otherConfig: {
    strategyOptions: {
      rrmemory: '轮询',
      random: '随机',
      specified: '固定',
    },
  },
}