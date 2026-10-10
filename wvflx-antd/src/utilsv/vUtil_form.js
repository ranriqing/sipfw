import _ from '@/utils/tiny_lodash'
import { postAction } from '@/api/api'
import { tools, converters } from '@/utils/util'
import { FIELD } from '@/constants'

import MetaDomain from '@/store/models/MetaDomain'
import MetaDomainField from '@/store/models/MetaDomainField'
import FieldFactory from "@/utilsv/fields"

import {
  fieldSupported, candidateFieldIcon,
  candidateFieldQueryComponent, candidateFieldQueryOperator,
  enrichClonedItem,
} from './vUtil_formFunctions'

let utilObj = {
  /** 唯一字段名 */
  singleFieldNames: [
    'name', 'gender',
    'birthday', 'age',
    'mobile', 'phone',
    'address', 'detailAddress',
  ],
  /** 字段工具 */
  fieldSupported: fieldSupported,
  candidateFieldIcon: candidateFieldIcon,
  candidateFieldQueryComponent: candidateFieldQueryComponent,
  candidateFieldQueryOperator: candidateFieldQueryOperator,
  enrichClonedItem: enrichClonedItem,
  /** 通用方法 */
  queryCloneItem: function(that, cdfItem) {
    let cdfExtend = converters.parseObj(cdfItem?.extendJson)
    let clonedItem = {
      id: cdfItem.id,
      title: cdfItem.title,
      field: cdfItem.fieldName,
      type: cdfItem.fieldType,
      component: undefined,
      placeholder: undefined,
      seqOrder: cdfItem.seqOrder,
      extend: cdfExtend,
      // ----------- //
      metaDomainField: cdfItem,
    }
    // 变换组件和扩展(见: GFormItem)
    cdfExtend['operator'] = utilObj.candidateFieldQueryOperator(cdfItem)
    let placeholder = _.get(cdfExtend, 'placeholder', cdfItem.title)
    let cdfComponent = utilObj.candidateFieldQueryComponent(cdfItem)
    let transformComponent = undefined
    let transformExtend = {}
    clonedItem['title'] = cdfItem.title
    clonedItem['field'] = cdfItem.fieldName
    clonedItem['placeholder'] = placeholder
    clonedItem['component'] = transformComponent || cdfComponent
    clonedItem['extend'] = Object.assign(cdfExtend, transformExtend)
    return clonedItem
  },
  tableCloneItem: function(that, dfItem, h){
    h || true
    // 真实 MetaDomainField：参考 vUtil_table.js 委托 FieldFactory，正确处理 Select 等字段的值→标签渲染
    if(!_.isNil(dfItem.metaDomainId)){
      let metaDomain = that.fmDomain || MetaDomain.find(dfItem.metaDomainId)
      let domainName = _.get(metaDomain, 'domainName')
      let fieldInstance = FieldFactory.getInstanceByType(domainName, dfItem.fieldType)
      let sortable = [FIELD.SELECT, FIELD.DATE, FIELD.INTEGER, FIELD.BIG_DECIMAL].indexOf(dfItem.fieldType) !== -1
      let wrapperItem = {
        id: dfItem.id,
        title: dfItem.title,
        field: dfItem.fieldName,
        type: dfItem.fieldType,
        component: undefined,
        placeholder: undefined,
        seqOrder: dfItem.seqOrder,
        extend: sortable?JSON.stringify({sorter: true}):undefined,
        // ----------- //
        metaDomainFieldId: dfItem.id,
        relMetaDomainField: dfItem,
      }
      return fieldInstance.tableCloneItemTmpl(that, domainName, wrapperItem, h)
    }
    // 合成列（ID/操作/创建人/更新人/创建时间/最近更新）：无 metaDomainId，保持原 component 渲染
    let dfExtend = converters.parseObj(dfItem?.extendJson)
    let clonedItem = {
      id: dfItem.id,
      title: dfItem.title,
      field: dfItem.fieldName,
      type: dfItem.fieldType,
      component: dfItem.component, // 为空或linkRender或actionRender
      seqOrder: dfItem.seqOrder,
      extend: dfExtend,
      // ----------- //
      metaDomainField: dfItem,
    }
    // 变换组件和扩展(见: https://1x.antdv.com/components/table/#Table)
    let tableSorter = that.tableSorter || {}
    clonedItem['title'] = dfItem.title              // 列的标题 string|slot
    clonedItem['dataIndex'] = dfItem.fieldName      // 列的标题 string|slot
    clonedItem['align'] = dfExtend['align']         // 对齐方式 'left' | 'right' | 'center'
    clonedItem['sorter'] = [FIELD.SELECT, FIELD.DATE, FIELD.INTEGER, FIELD.BIG_DECIMAL].indexOf(dfItem.fieldType) !== -1
    clonedItem['sortOrder'] = tableSorter.field === dfItem.fieldName && tableSorter.order // 排序方向 'ascend' | 'descend' | false
    // 判断固定列 boolean|string  Note: true(same as left) 'left' 'right'
    if(dfExtend['fixed']){
      clonedItem.fixed = dfExtend['fixed']
    }
    // 丰富ClonedItem
    utilObj.enrichClonedItem(clonedItem, that, h)
    return clonedItem
  },
  formCloneItem: function(that, dfItem) {
    let dfExtend = converters.parseObj(dfItem.extendJson)
    let clonedItem = {
      id: dfItem.id,
      title: dfItem.title,
      field: dfItem.fieldName,
      type: dfItem.fieldType,
      component: undefined,
      placeholder: undefined,
      seqOrder: dfItem.seqOrder,
      extend: dfExtend,
      // ----------- //
      metaDomainField: dfItem,
    }
    // 变换组件和扩展(见: GFormItem)
    let extendComponent = _.get(dfExtend, 'component', 'g-input')
    // Fix MetaDomainField extend[format] conflict
    if(dfItem.fieldType === FIELD.DATE){
      dfExtend['format'] = dfExtend['uiFormat']
    }
    let dfConstraint = converters.parseObj(dfItem.constraintJson)
    dfExtend['required'] = !dfConstraint.nullable
    let transformComponent = undefined
    let transformExtend = {}
    clonedItem['title'] = dfItem.title
    clonedItem['field'] = dfItem.fieldName
    clonedItem['placeholder'] = _.get(dfExtend, 'placeholder', dfItem.title)
    clonedItem['component'] = transformComponent || extendComponent
    clonedItem['extend'] = Object.assign(dfExtend, transformExtend)
    // 变换组件和扩展
    // (见: page_design/configs/partRight.js)
    // (见: 下面handleItemCopy()方法)
    clonedItem['extendJson'] = dfExtend
    clonedItem['constraintJson'] = dfConstraint
    return clonedItem
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
  cptDragTargetOptions: function() {
    return {
      animation: 200,
      group: 'form',
      ghostClass: 'ghost',
      handle: '.drag-item',
    };
  },
  cptDragSourceOptions: function() {
    return {
      animation: 200,
      group: { name:'form', pull:'clone', put:false },
      ghostClass: 'ghost',
      sort:false,
    };
  },
  /** Item操作 */
  handleItemDrag: function(that, changeEvent){
    // 获取MetaDomain字段
    let domainFields = MetaDomainField.query()
      .where('metaDomainId', that.fmDomain?.id).where('visible', true)
      .orderBy('seqOrder', 'asc').orderBy('id', 'asc').get()
    let mappedItems = domainFields.map(item => {
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
      let cdfItem = _.get(changeEvent, 'added.element')
      let fieldName = undefined
      if(utilObj.singleFieldNames.indexOf(cdfItem.fieldName)!==-1){
        fieldName = cdfItem.fieldName
      }else{
        fieldName = tools.randomString(8)
      }
      let itemIndex = domainFields.findIndex( fieldItem => fieldItem['fieldName'] === fieldName)
      if(itemIndex !== -1){
        utilObj._updateCpts(domainFields)
        that.$message.warn('请勿重复添加组件')
        return
      }
      // 特殊处理: 创建MetaDomain字段
      MetaDomainField.$create({
        data: {
          title: cdfItem.title,
          fieldName: fieldName,
          fieldType: cdfItem.fieldType,
          visible: true,
          seqOrder: newIndex,
          metaDomainId: that.fmDomain?.id,
          extendJson: cdfItem.extendJson,
          constraintJson: cdfItem.constraintJson,
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
          that.activeItem = utilObj.formCloneItem(that, addedItem)
          that.$emit('itemEdit', that.activeItem);
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
    that.$emit('itemEdit', that.activeItem);
  },
  handleItemCopy: function(that, item){
    let domainFields = MetaDomainField.query()
      .where('metaDomain', that.fmDomain?.id).where('visible', true)
      .orderBy('seqOrder', 'asc').orderBy('id', 'asc').get()
    let mappedItems = domainFields.map(item => {
      return {
        id: item.id,
        title: item.title,
        seqOrder: item.seqOrder
      }
    })
    let newIndex = item.seqOrder
    MetaDomainField.$create({
      data: {
        title: item.title,
        fieldName: tools.randomString(8),
        fieldType: item.type,
        seqOrder: newIndex,
        metaDomainId: that.fmDomain?.id,
        extendJson : converters.encodeObj(item.extendJson),
        constraintJson : converters.encodeObj(item.constraintJson)
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
        that.activeItem = utilObj.formCloneItem(that, addedItem)
        that.$emit('itemEdit', that.activeItem);
      }
    })
  },
  handleItemUpdate: function(that, item){
    let itemId = item['id']
    let promise = undefined
    if(itemId === 0){ // 特殊处理: 更新声明
      let extendJson = converters.parseObj(item['domainExtendJson'])
      extendJson['title'] = item['title']
      extendJson['statement'] = item['statement']
      promise = MetaDomain.$update({
        params: {
          id: item['domainId'],
        },
        data: {
          extendJson : converters.encodeObj(extendJson),
        }
      })
      promise.then(() => {
        that.$refs.refMeta?.doClose()
      })
    }else{
      // Fix MetaDomainField extend[format] conflict
      if(item.fieldType === 'Date'){
        item.extendJson['format'] = undefined
      }
      promise = MetaDomainField.$update({
        params: {
          id: itemId,
        },
        data: {
          title: item.title,
          extendJson : converters.encodeObj(item.extendJson),
          constraintJson : converters.encodeObj(item.constraintJson)
        }
      })
      promise.then(() => {
        that.$refs.refMeta?.doClose()
      })
    }
    return promise
  },
  handleItemDelete: function(that, item){
    MetaDomainField.$delete({
      params: {
        id: item.id,
      }
    })
  },
  /** 工具方法 */
  _updateCpts(domainFields){
    let firstItem = domainFields[0]
    if(firstItem){ // JavaScript无数组越界
      MetaDomainField.update({
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
    postAction('/metaDomainField/seqOrders', {items: itemOrders}).then(resp => {
      let entitys = resp.gmodel
      MetaDomainField.insertOrUpdate({
        data: entitys
      })
    })
  },
}

export default utilObj