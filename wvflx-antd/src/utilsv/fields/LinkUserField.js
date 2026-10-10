import AbstractField from './AbstractField';
import {COMPO, FIELD, QUERY} from '@/constants'
import _ from "@/utils/tiny_lodash";
import GLinkUser from "@/components/comp_form/link/GLinkUser.vue";

export default class LinkUserField extends AbstractField {

  static getSupportedTypes() {
    return [FIELD.LINK_USER];
  }

  fieldIcon(){
    return 'g-field-user'
  }

  querySupported(){
    return true
  }

  queryCloneItem(that, domainName, item) {
    let clonedItem = super.queryCloneItem(that, domainName, item)
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
    clonedItem.innerRender = function(text, record) {
      let fieldName = that._.get(item, 'field')
      return h(GLinkUser, {
        props: {
          readonly: true,
          disabled: true,
          value: record[fieldName],
          extend: {
            showLimit: 5,
          }
        }
      });
    };
    return {
      ...clonedItem,
    }
  }

  popSupported(){
    return true
  }

  popCloneItem(that, domainName, item) {
    let clonedItem = super.popCloneItem(that, domainName, item)
    _.set(clonedItem, 'component', COMPO.G_LINK_USER)
    return {
      ...clonedItem,
    }
  }

}