import AbstractField from './AbstractField';
import {COMPO, FIELD, QUERY, GLOBAL, DATE} from '@/constants'
import _ from "@/utils/tiny_lodash";

export default class DateField extends AbstractField {

  static getSupportedTypes() {
    return [FIELD.DATE];
  }

  fieldIcon(){
    return 'g-field-date'
  }

  querySupported(){
    return true
  }

  queryCloneItem(that, domainName, item) {
    let clonedItem = super.queryCloneItem(that, domainName, item)
    let format = _.get(clonedItem, 'dfExtend.format', _.get(clonedItem, 'extend.format', DATE.YYYYMMDD))
    _.set(clonedItem, 'component', COMPO.G_DATE_RANGE)
    _.set(clonedItem, 'extend.operator', QUERY.BETWEEN)
    _.set(clonedItem, 'extend.format', format)
    return {
      ...clonedItem,
    }
  }

  tableSupported(){
    return true
  }

  tableCloneItem(that, domainName, item, h){
    let clonedItem = super.tableCloneItem(that, domainName, item, h)
    let format = _.get(clonedItem, 'dfExtend.format', _.get(clonedItem, 'extend.format', DATE.YYYYMMDD))
    clonedItem.innerRender = function(text, record){
      let fieldName = that._.get(item, 'field')
      let fieldValue = that._.get(record, fieldName)
      return fieldValue?that.$moment(fieldValue, GLOBAL.DATE_FORMATS).format(format):'';
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
    let format = _.get(clonedItem, 'dfExtend.format', DATE.YYYYMMDD)
    _.set(clonedItem, 'component', COMPO.G_DATE_PICKER)
    _.set(clonedItem, 'extend.format', format)
    return {
      ...clonedItem,
    }
  }

}