import AbstractField from './AbstractField';
import {COMPO, FIELD, QUERY} from '@/constants'
import _ from "@/utils/tiny_lodash";
import AvatarUser from "@/components/comp_tools/AvatarUser.vue";

export default class UserField extends AbstractField {

  static getSupportedTypes() {
    return [FIELD.USER];
  }

  fieldIcon(){
    return 'g-field-user'
  }

  querySupported(){
    return true
  }

  queryCloneItem(that, domainName, item) {
    let clonedItem = super.queryCloneItem(that, domainName, item)
    _.set(clonedItem, 'field', clonedItem.field + FIELD.SUFFIX_ID)
    _.set(clonedItem, 'component', COMPO.G_REF_USER)
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
      return h('div', [
        h(AvatarUser, {
          props: {
            userId: _.get(record, suffixField),
          }
        })
      ])
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
    _.set(clonedItem, 'component', COMPO.G_REF_USER)
    return {
      ...clonedItem,
    }
  }

}