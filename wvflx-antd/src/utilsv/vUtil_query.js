import _ from '@/utils/tiny_lodash'
import { ormSeqOrders } from '@/api/api'
import { converters } from '@/utils/util'
import { FIELD, COMPO, QUERY, DATE } from '@/constants'
import FieldFactory from '@/utilsv/fields'

import MetaDomain from '@/store/models/MetaDomain'
import MetaDomainField from '@/store/models/MetaDomainField'
import MetaPageComponentField from '@/store/models/MetaPageComponentField'
import MetaPageComponent from "@/store/models/MetaPageComponent";


let utilObj = {
  /** 字段工具 */
  domainFieldIcon: function(dfItem) {
    dfItem = dfItem || {}
    let fieldInstance = FieldFactory.getInstanceByType(null, dfItem.fieldType);
    return fieldInstance.fieldIcon()
  },
  fieldSupported: function(dfItem) {
    let fieldInstance = FieldFactory.getInstanceByType(null, dfItem.fieldType);
    return fieldInstance.querySupported();
  },
  /** 通用方法 */
  queryCloneItem: function(that, item) {
    let metaDomain = MetaDomain.find(that._.get(item, "relMetaDomainField.metaDomainId"))
    if(!metaDomain){ metaDomain = MetaDomain.find(that.sModel?.metaDomainId) } // 兜底metaDomain
    let fieldInstance = FieldFactory.getInstanceByType(metaDomain?.domainName, item.type);
    return fieldInstance.queryCloneItem(that, metaDomain?.domainName, item);
  },
  queryRules: function(values, itemFinder){
    let queryRules = []
    _.forIn(values, function(value, key){
      let item = itemFinder(key)
      if(value && item){
        if(item.rulesGenerator){  // 特殊查询逻辑
          queryRules.push(...item.rulesGenerator(value))
        }else{  // 默认查询逻辑
          let orKeys = key.split('|')
          if(COMPO.G_DATE_RANGE === item.component && value.length === 2){
            let dateFormat = _.get(item, 'extend.format') || DATE.YYYYMMDD
            queryRules.push({
              field: key,
              value: [value[0].format(dateFormat), value[1].format(dateFormat)],
              operator: _.get(item, 'extend.operator') || QUERY.BETWEEN,
            })
          }else if(orKeys.length > 1){
            let orCondition = {
              condition: 'or',
              rules: []
            };
            orKeys.forEach(orKey => {
              orCondition.rules.push({
                field: orKey,
                value: value,
                operator: _.get(item, 'extend.operator') || QUERY.EQUAL,
              })
            })
            queryRules.push(orCondition)
          }else if(_.get(item, 'extend.mode') === 'exists'){ // 字段按是否存在(不为空)查询
            queryRules.push({
              field: key,
              operator: value==='0'?QUERY.IS_NULL:QUERY.IS_NOT_NULL,
            })
          }else{
            queryRules.push({
              field: key.replace('&', '.'), // 使用&替代Nested fields
              value: value,
              operator: _.get(item, 'extend.operator') || QUERY.EQUAL,
            })
          }
        }
      }
    })
    return queryRules
  },
  /** 计算属性 */
  cptComponentData: function(){
    return {
      attrs: {
        gutter: 24,
        style: {
          'margin-left': 0,
          'margin-right': 0,
        }
      },
    }
  },
  cptDragSourceOptions: function() {
    return {
      animation: 200,
      group: { name:'query', pull:'clone', put:false },
      ghostClass: 'ghost',
      sort:false,
    };
  },
  cptDragTargetOptions: function() {
    return {
      animation: 200,
      group: 'query',
      ghostClass: 'ghost',
      handle: '.drag-item-widget',
    };
  },
  /** Cols操作 */
  handleColsConfig: function(that){
    that.$refs.refCols?.showPop({}, {
        targetModel: that.sModel,
        targetColsFields: that._.get(that.sContext, 'colsFields'),
      },
      (item, context) => { /* submit */
        item || context
      }, () => { /* cancel */ }
    )
  },
  /** Item操作 */
  handleItemDrag: function(that, changeEvent){
    // 不能用that.datFieldItems，拖拽时排序已经改变
    let metaFields = MetaPageComponentField.query().where('metaPageComponentId', that.sModel?.id)
      .orderBy('seqOrder', 'asc').orderBy('id', 'asc').get()
    let mappedItems = metaFields.map(item => {
      return {
        id: item.id,
        title: item.title,
        seqOrder: item.seqOrder
      }
    })
    if(changeEvent['moved']){
      let oldIndex = _.get(changeEvent, 'moved.oldIndex')
      let newIndex = _.get(changeEvent, 'moved.newIndex')
      let item = mappedItems[oldIndex]
      mappedItems.splice(oldIndex, 1)
      mappedItems.splice(newIndex, 0, item)
      utilObj._updateItemOrder(mappedItems)
    }
    if(changeEvent['added']){
      let newIndex = _.get(changeEvent, 'added.newIndex')
      let dfItem = _.get(changeEvent, 'added.element')
      let itemIndex = that.datFieldItems.findIndex( fieldItem => fieldItem['field'] === dfItem.fieldName)
      if(itemIndex !== -1){
        utilObj._updateCpts(that.datFieldItems)
        that.$message.warn('请勿重复添加组件')
        return
      }
      MetaPageComponentField.$create({
        data: {
          title: dfItem.title,
          field: dfItem.fieldName,
          type: dfItem.fieldType,
          component: COMPO.G_DYN,
          seqOrder: newIndex,
          metaPageComponentId: that.sModel?.id,
          metaDomainFieldId: dfItem.id,
          extend: dfItem.id?null:dfItem.extendJson
        }
      }).then(resp =>{
        let addedItem = resp
        if(addedItem){
          let mappedItem = {
            id: addedItem.id,
            title: addedItem.title,
            seqOrder: addedItem.seqOrder,
          }
          mappedItems.splice(newIndex, 0, mappedItem)
          utilObj._updateItemOrder(mappedItems)
          that.activeItem = addedItem
        }
      })
    }
  },
  handleItemSelect: function(that, item, e){
    let isFieldComp = !!e.target.attributes['data-__field']
    let isActionIcon = !!e.target.closest('.drag-item-action,.drag-item-widget')
    if(isFieldComp || isActionIcon){
      return
    }
    if(that.activeItem && that.activeItem.id === item.id){
      that.activeItem = {}
    }else{
      that.activeItem = item
    }
  },
  handleItemEdit: function(that, item){
    that.$refs.refMeta?.showPop(item, {},
      (item, context) => { /* submit */
        return utilObj.handleItemUpdate(that, item, context)
      }, () => { /* cancel */ }
    )
  },
  handleItemUpdate: function(that, item){
    let allPromise = []
    // 更新MetaDomainField的extend
    let metaDomainFieldId = item['metaDomainFieldId']
    let dfUpdate = [FIELD.SELECT].indexOf(item.type) !== -1
    if(metaDomainFieldId && dfUpdate){
      let dfPromise = MetaDomainField.$update({
        params: {
          id: metaDomainFieldId,
        },
        data: {
          extendJson : converters.encodeObj(item.dfExtend)
        }
      })
      allPromise.push(dfPromise)
    }
    // 更新MetaPageComponentField的extend
    let cfPromise = MetaPageComponentField.$update({
      params: {
        id: item.id,
      },
      data: {
        title : item.title,
        placeholder : item.placeholder,
        extend : converters.encodeObj(item.cfExtend)
      }
    })
    allPromise.push(cfPromise)
    return Promise.all(allPromise)
  },
  handleItemDelete: function(that, item){
    let promise = MetaPageComponentField.$delete({
      params: {
        id: item.id,
      }
    })
    promise.then(resp =>{
      resp || true
      if(item['type']===FIELD.MANY_TO_ONE){
        let fieldIdent = `${item.id}_${item.field}`
        let relComponent = MetaPageComponent.query().where('ident', fieldIdent).first()
        if(relComponent){
          MetaPageComponent.$delete({
            params: {
              id: relComponent.id
            }
          })
        }
      }
    })
  },
  /** 工具方法 */
  _relDomainInfo(relDomainId){
    let metaDomain = MetaDomain.find(relDomainId)
    let metaDomainField = MetaDomainField.query()
      .where('metaDomain', metaDomain?.id).where('visible', true)
      .where(item => {
        return [FIELD.STRING].indexOf(item.fieldType)!==-1
      })
      .orderBy('seqOrder', 'asc').orderBy('id', 'asc').first()
    return [metaDomain, metaDomainField]
  },
  _updateCpts(fieldItems){
    let firstItem = fieldItems[0]
    if(firstItem){ // JavaScript无数组越界
      MetaPageComponentField.update({
        where: firstItem['id'],
        data: { seqOrder: firstItem['seqOrder'] }
      })
    }
  },
  _updateItemOrder(mappedItems){
    let itemOrders = []
    mappedItems.forEach((item, index) => {
      let itemData = {id: item.id, seqOrder: index+1}
      itemOrders.push(itemData)
    })
    ormSeqOrders('metaPageComponentField', {items: itemOrders}).then(resp => {
      let entitys = resp.gmodel
      MetaPageComponentField.insertOrUpdate({
        data: entitys
      })
    })
  },
}

export default utilObj