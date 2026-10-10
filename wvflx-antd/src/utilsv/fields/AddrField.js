import AbstractField from './AbstractField';
import {COMPO, FIELD, QUERY} from '@/constants'
import _ from "@/utils/tiny_lodash";
import GRefAddr from "@/components/comp_form/refer/GRefAddr.vue";

export default class AddrField extends AbstractField {

  static getSupportedTypes() {
    return [FIELD.ADDR];
  }

  fieldIcon(){
    return 'g-field-addr'
  }

  querySupported(){
    return true
  }

  queryCloneItem(that, domainName, item) {
    let clonedItem = super.queryCloneItem(that, domainName, item)
    _.set(clonedItem, 'component', COMPO.G_REF_ADDR)
    _.set(clonedItem, 'extend.operator', QUERY.EQUAL)
    let suffixArray=['ProvinceId', 'CityId', 'AreaId']
    clonedItem.rulesGenerator = function(valueArray){
      let ruleArray = []
      if(_.isArray(valueArray) && valueArray.length > 0){
        for(let i=0; i<valueArray.length; i++) {
          let ruleItem = {
            field: clonedItem['field'] + suffixArray[i],
            value: valueArray[i],
            operator: QUERY.EQUAL,
          }
          ruleArray.push(ruleItem)
        }
      }
      return ruleArray
    }

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
      return h(GRefAddr, {
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
    _.set(clonedItem, 'component', COMPO.G_REF_ADDR)
    return {
      ...clonedItem,
    }
  }

}