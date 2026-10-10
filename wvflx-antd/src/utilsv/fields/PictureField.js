import AbstractField from './AbstractField';
import {COMPO, FIELD, QUERY} from '@/constants'
import _ from "@/utils/tiny_lodash";
import GUploadPic from '@/components/comp_form//refer/GUploadPic.vue'

export default class PictureField extends AbstractField {

  static getSupportedTypes() {
    return [FIELD.PICTURE];
  }

  fieldIcon(){
    return 'g-field-picture'
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
    clonedItem.innerRender = function(text, record){
      let fieldName = that._.get(item, 'field')
      return h(GUploadPic, {
        props: {
          readonly: true,
          disabled: true,
          value: record[fieldName]
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
    _.set(clonedItem, 'component', COMPO.G_UPLOAD_PIC)
    return {
      ...clonedItem,
    }
  }

}