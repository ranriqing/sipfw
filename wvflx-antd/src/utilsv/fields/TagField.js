import _ from '@/utils/tiny_lodash'
import AbstractField from './AbstractField';
import {COMPO, FIELD, QUERY} from '@/constants'
import ATag from 'ant-design-vue/lib/tag';

export default class TagField extends AbstractField {

  static getSupportedTypes() {
    return [FIELD.V_TAGS];
  }

  fieldIcon(){
    return 'g-field-reference'
  }

  querySupported(){
    return true
  }

  queryCloneItem(that, domainName, item) {
    let clonedItem = super.queryCloneItem(that, domainName, item)
    _.set(clonedItem, 'field', '_extend_tags')
    _.set(clonedItem, 'component', COMPO.G_LINK_TAG)
    _.set(clonedItem, 'extend.operator', QUERY.IN)
    _.set(clonedItem, 'extend.indexUri', '/gcsTag/names')
    _.set(clonedItem, 'extend.relQueryParams', { referenceClass: _.get(clonedItem, 'extend.metaDomainName') })
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
      let tags = record[clonedItem.field] || []
      if (!Array.isArray(tags)) {
        tags = []
      }
      return h('div', tags.map(tag => {
        let name = typeof tag === 'string' ? tag : (tag && tag.name)
        let color = typeof tag === 'string' ? undefined : (tag && tag.color)
        return h(ATag, { props: { color: color || 'green' } }, name)
      }))
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
    _.set(clonedItem, 'component', COMPO.G_LINK_TAG)
    _.set(clonedItem, 'extend.indexUri', '/gcsTag/names')
    _.set(clonedItem, 'extend.relQueryParams', { referenceClass: _.get(clonedItem, 'extend.metaDomainName') })
    return {
      ...clonedItem,
    }
  }
}
