import _ from '@/utils/tiny_lodash'
import { converters, tools } from '@/utils/util'
import { COMPO } from "@/constants";
import ABadge from "ant-design-vue/lib/badge";
import ATag from "ant-design-vue/lib/tag";
import MetaDomain from '@/store/models/MetaDomain'

export default class AbstractField {

  static getSupportedDomain() {
    return true;
  }

  static getSupportedTypes() {
    throw new Error("子类必须实现getSupportedTypes静态方法");
  }

  fieldIcon(){
    throw 'g-field-unknown';
  }

  querySupported(){
    throw new Error("子类必须实现querySupported方法");
  }

  queryCloneItem(that, domainName, item) {
    let dfItem= item['relMetaDomainField']
    let metaDomain = MetaDomain.find(that.sModel?.metaDomainId)
    let cfExtend = converters.parseObj(item['extend'])
    let dfExtend = converters.parseObj(dfItem?.extendJson)
    let dfToExtend = {
      metaDomainName: tools.toLowerCamel(metaDomain?.domainName)
    }
    let clonedItem = {
      id: item.id,
      title: item.title,
      field: item.field,
      type: item.type,
      component: item.component,
      placeholder: item.placeholder,
      seqOrder: item.seqOrder,
      extend: Object.assign(dfToExtend, cfExtend, dfExtend),
      // ----------- //
      metaDomainFieldId: item.metaDomainFieldId,
      cfExtend: cfExtend,
      dfExtend: dfExtend,
    }
    // 变换组件和扩展(见: GFormItem)
    let transformComponent = undefined
    let transformExtend = {}
    transformExtend['operator'] = _.defaultTo(cfExtend['operator'], dfToExtend['operator'])
    if(cfExtend['mode'] === 'exists'){  // 字段按是否存在(不为空)判断查询
      transformComponent = COMPO.G_TSELECT
      transformExtend['options'] = { 存在: '1', 不存在: '0'}
    }
    clonedItem['title'] = item.title
    clonedItem['field'] = item.field
    clonedItem['placeholder'] = item.placeholder
    clonedItem['component'] = transformComponent || item.component
    clonedItem['extend'] = Object.assign(dfToExtend, cfExtend, transformExtend)
    return clonedItem
  }

  tableSupported(){
    throw new Error("子类必须实现tableSupported方法");
  }

  tableCloneItemTmpl(that, domainName, item, h){
    let clonedItem = this.tableCloneItem(that, domainName, item, h)
    // 丰富ClonedItem
    this._enrichTableClonedItem(clonedItem, that, h)
    return clonedItem
  }
  tableCloneItem(that, domainName, item, h){
    h || true
    let dfItem = item['relMetaDomainField']
    let metaDomain = MetaDomain.find(that.sModel?.metaDomainId)
    let cfExtend = converters.parseObj(item['extend'])
    let dfExtend = converters.parseObj(dfItem?.extendJson)
    let dfToExtend = {
      metaDomainName: tools.toLowerCamel(metaDomain?.domainName)
    }
    let clonedItem = {
      id: item.id,
      title: item.title,
      field: item.field,
      type: item.type,
      component: item.component,
      placeholder: item.placeholder,
      seqOrder: item.seqOrder,
      extend: Object.assign(dfToExtend, cfExtend, dfExtend),
      // ----------- //
      metaDomainFieldId: item.metaDomainFieldId,
      cfExtend: cfExtend,
      dfExtend: dfExtend,
    }
    // 变换组件和扩展(见: https://1x.antdv.com/components/table/#Table)
    let tableSorter = that.tableSorter || that.reportSorter || {}
    clonedItem['title'] = item.title           // 列的标题 string|slot
    clonedItem['dataIndex'] = item.field       // 列的标题 string|slot
    clonedItem['align'] = cfExtend['align']    // 对齐方式 'left' | 'right' | 'center'
    clonedItem['width'] = cfExtend['width']    // 列的宽度  string|number
    clonedItem['sorter'] = cfExtend['sorter']  // 本地排序 Function|boolean
    clonedItem['sortOrder'] = tableSorter.field === item.field && tableSorter.order // 排序方向 'ascend' | 'descend' | false
    // 判断固定列 boolean|string  Note: true(same as left) 'left' 'right'
    if(cfExtend['fixed']){
      clonedItem['fixed'] = cfExtend['fixed']
    }
    return clonedItem
  }
  reportDfItem(that, domainName, dfItem){
    // let metaDomain = MetaDomain.find(dfItem.metaDomainId)
    return {
      // id: dfItem.id,
      title: dfItem.title,
      field: dfItem.fieldName,
      type: dfItem.fieldType,
      // ----------- //
      metaDomainFieldId: dfItem.id,
    }
  }

  popSupported(){
    throw new Error("子类必须实现popSupported方法");
  }

  popCloneItem(that, domainName, item) {
    let dfItem = item['relMetaDomainField']
    let metaDomain = MetaDomain.find(that.sModel?.metaDomainId)
    let cfExtend = converters.parseObj(item['extend'])
    let dfExtend = converters.parseObj(dfItem?.extendJson)
    let dfConstraint = converters.parseObj(dfItem?.constraintJson)
    let dfToExtend = {
      dfRequired: dfItem?!dfConstraint.nullable:false,
      metaDomainName: tools.toLowerCamel(metaDomain?.domainName)
    }
    let dfToExtendRequired = dfToExtend['dfRequired']
    cfExtend['required'] = dfToExtendRequired?true:_.defaultTo(cfExtend['required'], dfToExtendRequired)
    let clonedItem = {
      id: item.id,
      title: item.title,
      field: item.field,
      type: item.type,
      component: COMPO.G_DYN,
      placeholder: item.placeholder,
      seqOrder: item.seqOrder,
      extend: Object.assign(dfToExtend, cfExtend, dfExtend),
      // ----------- //
      metaDomainFieldId: item.metaDomainFieldId,
      cfExtend: cfExtend,
      dfExtend: dfExtend,
    }
    // 变换组件和扩展(见: GFormItem)
    let transformComponent = undefined
    let transformExtend = {}
    clonedItem['title'] = item.title
    clonedItem['field'] = item.field
    clonedItem['placeholder'] = item.placeholder
    clonedItem['component'] = transformComponent || item.component
    clonedItem['extend'] = Object.assign(dfToExtend, cfExtend, transformExtend)
    return clonedItem
  }

  /** 工具方法 */
  _enrichTableClonedItem(clonedItem, that, h) {
    // 子类已自定义 customRender，跳过框架默认渲染
    if (clonedItem.customRender) {
      return
    }
    // 1、按字段类型来丰富Render
    // 按component来丰富Render
    let extendOptions = _.get(clonedItem, 'extend.options', {})
    let transformOptions = converters.reverseObj(extendOptions)
    let fieldRender = undefined
    if(clonedItem.id){ // Meta组件方式
      fieldRender = _.get(clonedItem, 'cfExtend.render', 'defaultRender')
    }else{  // 配置文件方式
      fieldRender = _.get(clonedItem, 'component', 'defaultRender')
    }
    let textRender = function(text, record){
      let itemValue = _.get(record, clonedItem.field)
      let itemValueStr = _.toString(itemValue)
      return _.get(transformOptions, itemValueStr, itemValueStr) || '';
    }
    if(clonedItem.id && 'linkRender' === fieldRender){
      clonedItem.customRender = function(text, record){
        let attrs = {title: record.id}
        let on = { click: () => { that.onRecordEdit(record) } }
        let vNodes = []
        if(clonedItem.innerRender){
          vNodes.push(clonedItem.innerRender(text, record))
        }else{
          vNodes = textRender(text, record)
        }
        return h('a', {
          ...attrs,  // 扩展属性
          on: on     // 绑定事件
        }, vNodes);
      };
      // SlotRender
      clonedItem.align = 'left'
      clonedItem.scopedSlots = { customRender: fieldRender}
    }else if('tagRender' === fieldRender){
      clonedItem.customRender = function(text, record){
        let props = {color: 'green'}
        let tagText = textRender(text, record)
        return tagText?h(ATag, { props }, tagText):undefined;
      };
    }else if('badgeRender' === fieldRender){
      clonedItem.customRender = function(text, record){
        let tagText = textRender(text, record)
        let props = {status: 'success', text: textRender(text, record)}
        return tagText?h(ABadge, { props }):undefined;
      };
    }else if('entityRender' === fieldRender){
      clonedItem.customRender = function(text, record){
        return _.get(record, `${clonedItem.field}_entity._title`)
      };
    }else if('defaultRender' === fieldRender || _.isNil(fieldRender)){
      clonedItem.customRender = function(text, record){
        let vNodes = []
        if(clonedItem.innerRender){
          vNodes.push(clonedItem.innerRender(text, record))
        }else{
          vNodes = textRender(text, record)
        }
        return h('div', null, vNodes);
      };
    }else{
      let renderFunc = this._extendRenderFunc(clonedItem, that, h)
      if(_.isFunction(renderFunc)){
        // 扩展Render部分
        clonedItem.customRender = renderFunc
      }else{
        // 按自定义Slot处理
        clonedItem.scopedSlots = { customRender: fieldRender}
      }

    }
  }

  _extendRenderFunc(clonedItem, that, h) {
    that || h || true
    // 按component来丰富Render
    let extendOptions = _.get(clonedItem, 'extend.options', {})
    let transformOptions = converters.reverseObj(extendOptions)
    let fieldRender = clonedItem.component
    if('roleLevelRender' === fieldRender){ // 角色分级示例
      return function(text, record){
        let itemValue = _.get(record, clonedItem.field)
        let itemValueStr = _.toString(itemValue)
        let itemValueTransformStr = _.get(transformOptions, itemValueStr, itemValueStr)
        let props = {color: itemValueTransformStr==='1'?'blue':'green'}
        return itemValueTransformStr
          ? h(ATag, { props },
            itemValueTransformStr === '1' ? '限制角色' : '普通角色'
          )
          : undefined;
      };
    }
  }
}