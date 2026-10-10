import AbstractField from './AbstractField';
import {COMPO, FIELD, QUERY} from '@/constants'
import _ from "@/utils/tiny_lodash";

export default class VIdField extends AbstractField {

  static getSupportedTypes() {
    return [FIELD.V_ID];
  }

  fieldIcon(){
    return 'g-field-id'
  }

  querySupported(){
    return false
  }

  queryCloneItem(that, domainName, item) {
    let clonedItem = super.queryCloneItem(that, domainName, item)
    _.set(clonedItem, 'component', COMPO.G_INPUT)
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
    return {
      ...clonedItem,
    }
  }

  popSupported(){
    return false
  }

  popCloneItem(that, domainName, item) {
    let clonedItem = super.popCloneItem(that, domainName, item)
    _.set(clonedItem, 'component', COMPO.G_PANEL)
    return {
      ...clonedItem,
    }
  }

}