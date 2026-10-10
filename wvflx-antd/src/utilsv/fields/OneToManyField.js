import _ from '@/utils/tiny_lodash'
import AbstractField from './AbstractField';
import {COMPO, FIELD, QUERY} from '@/constants'
import GRefMany from "@/components/comp_form/refer/GRefMany.vue";

export default class OneToManyField extends AbstractField {

  static getSupportedTypes() {
    return [FIELD.ONE_TO_MANY];
  }

  fieldIcon(){
    return 'g-field-reference'
  }

  querySupported(){
    return true
  }

  queryCloneItem(that, domainName, item) {
    let clonedItem = super.queryCloneItem(that, domainName, item)
    _.set(clonedItem, 'field', clonedItem.field)
    _.set(clonedItem, 'component', COMPO.G_REF_ONE)
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
    clonedItem.innerRender = function(text, record){
      let fieldName = clonedItem.field
      let relEntities = _.get(record, `${fieldName}_entities`)
      return h(GRefMany, {
        props: {
          item: clonedItem,
          table: true,
          readonly: true,
          disabled: true,
          record: { id: record['id'], field: fieldName },
          prefillData: relEntities,
        }
      });
    }
    return {
      ...clonedItem,
    }
  }

  popSupported(){
    return true
  }

  popCloneItem(that, domainName, item) {
    let clonedItem = super.popCloneItem(that, domainName, item)
    _.set(clonedItem, 'field', clonedItem.field)
    _.set(clonedItem, 'component', COMPO.G_REF_MANY)
    return {
      ...clonedItem,
    }
  }
}