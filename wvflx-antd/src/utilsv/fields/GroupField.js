import AbstractField from './AbstractField';
import {COMPO, FIELD, QUERY} from '@/constants'
import _ from "@/utils/tiny_lodash";
import {stores} from "@/utils/util";

export default class GroupField extends AbstractField {

  static getSupportedTypes() {
    return [FIELD.GROUP];
  }

  fieldIcon(){
    return 'g-field-dept'
  }

  querySupported(){
    return true
  }

  queryCloneItem(that, domainName, item) {
    let clonedItem = super.queryCloneItem(that, domainName, item)
    _.set(clonedItem, 'field', clonedItem.field + FIELD.SUFFIX_ID)
    _.set(clonedItem, 'component', COMPO.G_REF_GROUP)
    _.set(clonedItem, 'extend.operator', QUERY.EQUAL)
    return {
      ...clonedItem,
    }
  }

  tableSupported(){
    return true
  }

  tableCloneItem(that, domainName, item, h){
    let clonedItem = super.tableCloneItem(that, domainName, item, h)
    let suffixField = clonedItem.field + FIELD.SUFFIX_ID
    _.set(clonedItem, 'field', suffixField)       // 报表用
    _.set(clonedItem, 'dataIndex', suffixField)   // 报表用
    clonedItem.innerRender = function(text, record) {
      record || true
      let vnodes = [];
      // 使用 h 函数创建 span 元素，内容为 stores.levelGcsGroup 的返回值
      vnodes.push(h('span', null,
        stores.levelGcsGroup(_.get(record, suffixField))
      ));
      return vnodes;
    };
    return {
      ...clonedItem,
    }
  }

  reportDfItem(that, domainName, dfItem){
    let clonedItem = super.reportDfItem(that, domainName, dfItem)
    _.set(clonedItem, 'field', clonedItem.field + FIELD.SUFFIX_ID)
    return clonedItem
  }

  popSupported(){
    return true
  }

  popCloneItem(that, domainName, item) {
    let clonedItem = super.popCloneItem(that, domainName, item)
    _.set(clonedItem, 'field', clonedItem.field + FIELD.SUFFIX_ID)
    _.set(clonedItem, 'component', COMPO.G_REF_GROUP)
    return {
      ...clonedItem,
    }
  }

}