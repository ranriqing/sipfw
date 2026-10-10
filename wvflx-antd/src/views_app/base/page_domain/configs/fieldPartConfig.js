export default {
  /* 表格部分 */
  tableConfig: {
    tableColumns: function(){
      return [
        {
          title: '字段名称',
          field: 'fieldName',
        },
        {
          title: '字段类型',
          field: 'fieldType',
        },
        {
          title: '创建日期',
          field: 'dateCreated',
        },
        {
          title: '最近更新',
          field: 'lastUpdated',
        }
      ]
    },
  },
  debugTableConfig: {
    tableColumns: function(){
      return [
        {
          title: '字段名称',
          field: 'fieldName',
        },
        {
          title: '字段类型',
          field: 'fieldType',
        },
        {
          title: '创建日期',
          field: 'dateCreated',
        },
        {
          title: '最近更新',
          field: 'lastUpdated',
        }
      ]
    },
  },
  /* 其它配置 */
  otherConfig: {
    fieldTypeOptions: {
      Sequence: '[普通]序列号',
      String: '[普通]文本型',
      Boolean: '[普通]布尔型',
      ScEl: '[复引]字典型',
      Select: '[普通]选项型',
      /*MSelect: '多选项',*/
      Integer: '[普通]整数型',
      Long: '[普通]长整形',
      BigDecimal: '[普通]精确型',
      Date: '[普通]日期型',
      User: '[单引]用户型',
      Group: '[单引]部门型',
      Addr: '[复引]地址型',
      File: '[JSON]文件型',
      Picture: '[JSON]图片型',
      LinkUser: '[LINK]多用户',
    },
    fieldTypeLetter: {
      Sequence: 'S',
      String: 'T',
      /*Boolean: 'B',*/
      ScEl: 'S',
      Select: 'S',
      /*MSelect: 'M',*/
      Long: 'L',
      Integer: 'N',
      BigDecimal: 'G',
      Date: 'D',
      User: 'U',
      Group: 'G',
      Addr: 'A',
      File: 'F',
      Picture: 'P',
      LinkUser: 'U',
      Reference: 'R',
      RefInverse: 'R',
    },
  },
}