import AbstractField from './AbstractField';
import {COMPO, FIELD, QUERY} from '@/constants'
import _ from "@/utils/tiny_lodash";

export default class StringField extends AbstractField {

  static getSupportedTypes() {
    return [FIELD.SEQUENCE];
  }

  fieldIcon(){
    return 'g-field-string'
  }

  querySupported(){
    return true
  }

  queryCloneItem(that, domainName, item) {
    let clonedItem = super.queryCloneItem(that, domainName, item)
    _.set(clonedItem, 'component', COMPO.G_INPUT)
    _.set(clonedItem, 'extend.operator', QUERY.CONTAINS)
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
    return true
  }

  popCloneItem(that, domainName, item) {
    let clonedItem = super.popCloneItem(that, domainName, item)
    _.set(clonedItem, 'component', COMPO.G_INPUT)
    _.set(clonedItem, 'extend.readonly', true)
    _.set(clonedItem, 'extend.required', false)
    return {
      ...clonedItem,
    }
  }

}