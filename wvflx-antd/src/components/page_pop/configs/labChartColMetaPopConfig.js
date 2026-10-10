import { COMPO, FIELD, DATE } from '@/constants'

let configObj = {
  title: '分组属性',
  submitText: '保存',
  popWidth: 340,
  layout: 'vertical',
  formFields: [
    {
      title: '标题',
      field: 'title',
      component: COMPO.G_INPUT,
      placeholder: '请输入标题',
      options: {
      },
    },
    {
      title: '日期格式',
      field: 'format',
      component: COMPO.G_SELECT,
      placeholder: '请选择日期格式',
      isActived: function(record, context){
        context || true
        return [FIELD.DATE].indexOf(record.type) !== -1
      },
      options: {
      },
      extend: {
        allowClear: true,
        options: {
          [DATE.YYYYMM]: DATE.YYYYMM,
          [DATE.YYYYMMDD]: DATE.YYYYMMDD,
          [DATE.YYYYMMDD_HHMM]: DATE.YYYYMMDD_HHMM,
          [DATE.YYYYMMDD_HHMMSS]: DATE.YYYYMMDD_HHMMSS,
        }
      },
    },
    {
      title: '统计方式',
      field: 'projection',
      component: COMPO.G_SELECT,
      placeholder: '请选择统计方式',
      isActived: function(record, context){
        context || true
        return  [FIELD.INTEGER, FIELD.LONG, FIELD.BIG_DECIMAL].indexOf(record.type) !== -1
      },
      options: {
      },
      extend: {
        options: {
          '计数(COUNT)': 'COUNT',
          '相加(SUM)': 'SUM',
          '平均(AVG)': 'AVG',
          '最大(MAX)': 'MAX',
          '最小(MIN)': 'MIN',
        }
      },
    },
  ],
}

export default configObj