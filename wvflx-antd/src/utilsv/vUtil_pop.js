import _ from '@/utils/tiny_lodash'
import { ormSeqOrders } from '@/api/api'
import { tools, converters } from '@/utils/util'
import FieldFactory from '@/utilsv/fields'

import MetaDomainField from '@/store/models/MetaDomainField'
import MetaPageComponentField from '@/store/models/MetaPageComponentField'

import {  FIELD } from '@/constants'
import MetaPageComponent from "@/store/models/MetaPageComponent";
import MetaDomain from "@/store/models/MetaDomain";
import GSelectUserPop from '@/components/page_pop/GSelectUserPop.vue'

let utilObj = {
  /** 字段工具 */
  domainFieldIcon: function(dfItem) {
    dfItem = dfItem || {}
    let fieldInstance = FieldFactory.getInstanceByType(null, dfItem.fieldType);
    return fieldInstance.fieldIcon()
  },
  fieldSupported: function(dfItem) {
    let fieldInstance = FieldFactory.getInstanceByType(null, dfItem.fieldType);
    return fieldInstance.popSupported();
  },
  /** 通用方法 */
  // 打开“选择用户”弹窗（原 vFlowUtil.selectTransferUser；与流程无关的通用用户选择）
  selectTransferUser: function(that, record, context, submitCallback, cancelCallback){
    let dynComp = that.$dynamicModal({
      configObj: {
        modal: true,
        submitText: '确定'
      },
    }, GSelectUserPop)
    dynComp.showPop(record, context, submitCallback, cancelCallback)
  },
  collapsePanel: function(that, item, collapsed){
    if(that.collapsedState && item.pnlIdent){
      that.$set(that.collapsedState, item.pnlIdent, collapsed)
    }
  },
  popCloneItem: function(that, item) {
    let metaDomain = MetaDomain.find(that._.get(item, "relMetaDomainField.metaDomainId"))
    if(!metaDomain){ metaDomain = MetaDomain.find(that.sModel?.metaDomainId) } // 兜底metaDomain
    let fieldInstance = FieldFactory.getInstanceByType(metaDomain?.domainName, item.type);
    return fieldInstance.popCloneItem(that, metaDomain?.domainName, item);
  },
  /** 计算属性 */
  cptDragTargetOptions: function() {
    return {
      animation: 200,
      group: 'pop',
      ghostClass: 'ghost',
      handle: '.drag-item-widget',
    };
  },
  cptDragSourceOptions: function() {
    return {
      animation: 200,
      group: { name:'pop', pull:'clone', put:false },
      ghostClass: 'ghost',
      sort: false,
    };
  },
  /** Cols操作 */
  handleColsConfig: function(that){
    that.$refs.refCols?.showPop({}, {
        targetModel: that.sModel,
        targetNodeIdent: that.flowNode?.id, // 流程节点Ident
      },
      (item, context) => { /* submit */
        item || context
      }, () => { /* cancel */ }
    )
  },
  /** Item操作 */
  handleItemDrag: function(that, changeEvent, pIdent){
    // 不能用that.datFieldItems，拖拽时排序已经改变
    let fieldQuery = MetaPageComponentField.query().where('metaPageComponentId', that.sModel?.id)
    // 特殊处理: 在LabFlowPop中使用，用于区分不同流程节点
    if(pIdent){ fieldQuery = fieldQuery.where('pIdent', pIdent)}
    let metaFields = fieldQuery.orderBy('seqOrder', 'asc').orderBy('id', 'asc').get()
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
      let itemIndex = metaFields.findIndex( fieldItem => fieldItem['field'] === dfItem.fieldName)
      if(itemIndex !== -1){
        utilObj._updateCpts(metaFields)
        that.$message.warn('请勿重复添加组件')
        utilObj._updateItemOrder(mappedItems)
        return
      }
      // 特殊处理: 支持创建多个Panel组件
      let fieldName = dfItem.fieldName
      if(dfItem['fieldType']===FIELD.V_PANEL){
        fieldName = `${fieldName}_${tools.randomString(5)}`
      }
      MetaPageComponentField.$create({
        data: {
          title: dfItem.title,
          field: fieldName,
          type: dfItem.fieldType,
          component: utilObj.domainFieldComponent(dfItem),
          seqOrder: newIndex,
          pIdent: pIdent?pIdent:undefined, // 特殊处理: 支持pIdent
          metaPageComponentId: that.sModel?.id,
          metaDomainFieldId: dfItem.id,
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
  handleItemEdit: function(that, item, context){
    context = context || {}
    // 从父组件 extend 中提取 flowSteps，注入到字段编辑上下文
    if (!context.flowSteps) {
      var parentModel = that.sModel || that.targetModel
      if (parentModel) {
        var sExtend = converters.parseObj(parentModel.extend)
        var flowSteps = sExtend.flowSteps
        if (flowSteps && flowSteps.length > 0) {
          context.flowSteps = flowSteps
        }
      }
    }
    that.$refs.refMeta?.showPop(item, context,
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
        component: item.component, // 需要更新组件？
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
  _updateCpts(metaFields){
    let firstItem = metaFields[0]
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