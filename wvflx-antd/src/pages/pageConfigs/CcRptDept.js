let configObj = {
  partTable: {
    extend: {
      rptType: 'crosstab',
      header: '部门报表',
      queryUri: '/ccReport/deptCrosstab',
    },
    remark : '按部门统计报表'
  }
}

export default configObj