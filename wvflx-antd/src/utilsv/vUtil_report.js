import vUtil_table from '@/utilsv/vUtil_table'
import FieldFactory from "@/utilsv/fields";
import MetaDomain from "@/store/models/MetaDomain";

let utilObj = {
  /** 字段工具 */
  domainFieldIcon: function(dfItem) {
    dfItem = dfItem || {}
    let fieldInstance = FieldFactory.getInstanceByType(null, dfItem.fieldType);
    return fieldInstance.fieldIcon()
  },
  fieldSupported: function(dfItem) {
    let fieldInstance = FieldFactory.getInstanceByType(null, dfItem.fieldType);
    return fieldInstance.tableSupported();
  },
  /** 通用方法 */
  reportDfItem: function(that, dfItem){
    let metaDomain = MetaDomain.find(dfItem.metaDomainId)
    if(!metaDomain){ metaDomain = MetaDomain.find(that.sModel?.metaDomainId) } // 兜底metaDomain
    let fieldInstance = FieldFactory.getInstanceByType(metaDomain?.domainName, dfItem.fieldType);
    return fieldInstance.reportDfItem(that, metaDomain?.domainName, dfItem);
  },
  reportCloneItem: function(that, item, h){
    let metaDomain = MetaDomain.find(that._.get(item, "relMetaDomainField.metaDomainId"))
    if(!metaDomain){ metaDomain = MetaDomain.find(that.sModel?.metaDomainId) } // 兜底metaDomain
    let fieldInstance = FieldFactory.getInstanceByType(metaDomain?.domainName, item.type);
    return fieldInstance.tableCloneItem(that, metaDomain?.domainName, item, h);
  },
  reportReqErr: function(that, err, prefix){
    return vUtil_table.tableReqErr(that, err, prefix)
  },
  /** 计算属性 */
  cptDragSourceOptions: function() {
    return {
      animation: 200,
      group: { name:'report', pull:'clone', put:false },
      ghostClass: 'ghost',
      sort: false,
    };
  },
  cptDragTargetOptions: function() {
    return {
      animation: 200,
      group: 'report',
      ghostClass: 'ghost',
      handle: '.drag-widget',
    };
  },
  /** Cols操作 */
  handleColsConfig: function(that){
    vUtil_table.handleColsConfig(that)
  },
  /** Item操作 */
  handleItemDrag: function(that, changeEvent){
    vUtil_table.handleItemDrag(that, changeEvent)
  },
  handleItemEdit: function(that, item){
    vUtil_table.handleItemEdit(that, item)
  },
  handleItemUpdate: function(that, item){
    return vUtil_table.handleItemUpdate(that, item)
  },
  handleItemDelete: function(that, item){
    return vUtil_table.handleItemDelete(that, item)
  },
}

export default utilObj