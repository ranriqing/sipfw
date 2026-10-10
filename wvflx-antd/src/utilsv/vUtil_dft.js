import FieldFactory from '@/utilsv/fields'

let utilObj = {
  /** 字段工具 */
  fieldIcon: function(dfItem, typeField='fieldType') {
    dfItem = dfItem || {}
    let fieldInstance = FieldFactory.getInstanceByType(null, dfItem[typeField]);
    return fieldInstance.fieldIcon()
  },
}

export default utilObj