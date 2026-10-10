import { converters } from '@/utils/util'

export default class AbstractField {

  static getSupportedDomain() {
    throw new Error("子类必须实现getSupportedDomain静态方法");
  }

  static getSupportedTypes() {
    throw new Error("子类必须实现getSupportedTypes静态方法");
  }

  pageCloneItem(that, domainName, linkItem, componentItem) {
    let extend = converters.parseObj(componentItem['extend'])
    let clonedItem = {
      id: componentItem.id,
      title: componentItem.title,
      ident: componentItem.ident,
      pIdent: componentItem.pIdent,
      type: componentItem.type,
      component: componentItem.component,
      seqOrder: componentItem.seqOrder,
      extend: Object.assign({}, extend),
    }
    // 变换组件和扩展
    clonedItem['metaDomainId'] = componentItem.metaDomainId
    clonedItem['linkId'] = linkItem.id
    clonedItem['linkSeqOrder'] = linkItem.seqOrder
    clonedItem['pageLink'] = linkItem
    clonedItem['pageComponent'] = componentItem
    return clonedItem
  }
}