import AbstractField from './AbstractField';
import {COMPO, FIELD, QUERY} from '@/constants'
import _ from "@/utils/tiny_lodash";

export default class BooleanField extends AbstractField {

  static getSupportedTypes() {
    return [FIELD.BOOLEAN];
  }

  fieldIcon(){
    return 'g-field-boolean'
  }

  querySupported(){
    return true
  }

  queryCloneItem(that, domainName, item) {
    let clonedItem = super.queryCloneItem(that, domainName, item)
    _.set(clonedItem, 'component', COMPO.G_SELECT)
    _.set(clonedItem, 'extend.operator', QUERY.EQUAL)
    _.set(clonedItem, 'extend.options', {
      是: true,
      否: false,
    })
    return {
      ...clonedItem,
    }
  }

  tableSupported(){
    return true
  }

  tableCloneItem(that, domainName, item, h){
    let clonedItem = super.tableCloneItem(that, domainName, item, h)
    // 组件渲染输出
    clonedItem.innerRender = function(text, record) {
      let fieldName = that._.get(item, 'field')
      if(_.isNil(record[fieldName])){
        return ''
      }
      return h('a-tag', {
        props: {
          color: record[fieldName]?'#f50':'',
        }
      }, [record[fieldName] ? '是' : '否']);
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
    _.set(clonedItem, 'component', COMPO.G_RADIO_GROUP)
    _.set(clonedItem, 'extend.options', {
      是: true,
      否: false,
    })
    return {
      ...clonedItem,
    }
  }

}