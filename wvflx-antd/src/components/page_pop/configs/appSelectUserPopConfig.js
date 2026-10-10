export default {
  selectUserConfig: {
    title: '选择用户',
    submitText: '确认',
    popWidth: 950,
    formFields: [
    ],
    tableConfig: {
      tableColumns: function(){
        return [
          {
            title: '用 户',
            field: 'id',
            component: 'avatarUserRender',
            extend: JSON.stringify({
              width: 150,
            }),
          },
          {
            title: '用户角色',
            field: 'gcsRole',
            component: 'gcsRoleRender',
            extend: JSON.stringify({
              width: 150,
            }),
          },
          {
            title: '所在部门',
            field: 'gcsGroup',
            component: 'gcsGroupRender',
            extend: JSON.stringify({
              width: 150,
            }),
          },
        ]
      }
    }
  },
}