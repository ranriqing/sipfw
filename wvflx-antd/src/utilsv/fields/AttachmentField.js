import _ from '@/utils/tiny_lodash'
import AbstractField from './AbstractField';
import {COMPO, FIELD, QUERY} from '@/constants'
import GAttachment from "@/components/comp_form/refer/GAttachment.vue";

export default class AttachmentField extends AbstractField {

  static getSupportedTypes() {
    return [FIELD.V_ATTACHMENTS];
  }

  fieldIcon(){
    return 'g-field-reference'
  }

  querySupported(){
    return true
  }

  queryCloneItem(that, domainName, item) {
    let clonedItem = super.queryCloneItem(that, domainName, item)
    _.set(clonedItem, 'field', clonedItem.field + FIELD.SUFFIX_ID)
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
      return h(GAttachment, {
        props: {
          item: clonedItem,
          readonly: true,
          disabled: true,
          record: { id: record['id'], field: fieldName }
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
    _.set(clonedItem, 'component', COMPO.G_ATTACHMENT)
    return {
      ...clonedItem,
    }
  }
}