import AbstractField from './AbstractField';
import {COMPO, FIELD, QUERY} from '@/constants'
import { stores } from '@/utils/util'
import _ from "@/utils/tiny_lodash";

export default class VActionField extends AbstractField {

  static getSupportedTypes() {
    return [FIELD.V_ACTION];
  }

  fieldIcon(){
    return 'g-field-collapse'
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
    // SlotRender
    _.set(clonedItem, 'cfExtend.render', 'actionRender')
    clonedItem.align = 'center'
    clonedItem.width = stores.isMobile()?80:130
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