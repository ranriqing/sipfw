import _ from '@/utils/tiny_lodash'
import { FIELD, OTHER } from '@/constants'
import { converters } from '@/utils/util'
import { ormSeqOrders } from '@/api/api'
import FieldFactory from "@/utilsv/fields";

import MetaPageComponent from '@/store/models/MetaPageComponent'
import MetaPageComponentField from '@/store/models/MetaPageComponentField'
import MetaDomain from '@/store/models/MetaDomain'
import MetaDomainField from '@/store/models/MetaDomainField'

let utilObj = {
  /** 字段工具 */
  domainFieldIcon: function(dfItem, typeField='fieldType') {
    dfItem = dfItem || {}
    let fieldInstance = FieldFactory.getInstanceByType(null, dfItem[typeField]);
    return fieldInstance.fieldIcon()
  },
  fieldSupported: function(dfItem) {
    let fieldInstance = FieldFactory.getInstanceByType(null, dfItem.fieldType);
    return fieldInstance.tableSupported();
  },
  /** 通用方法 */
  tableCloneItem: function(that, item, h){
    let metaDomain = MetaDomain.find(that._.get(item, "relMetaDomainField.metaDomainId"))
    if(!metaDomain){ metaDomain = MetaDomain.find(that.sModel?.metaDomainId) } // 兜底metaDomain
    let fieldInstance = FieldFactory.getInstanceByType(metaDomain?.domainName, item.type);
    return fieldInstance.tableCloneItemTmpl(that, metaDomain?.domainName, item, h);
  },
  tableReqErr: function(that, err, prefix){
    if(!err.hcode){
      if(err.code === '102'){
        that.$notification.warn({
          message: `[${err.code}]${err.message}`,
          description: function(h){
            h && true
            let vnodes = []
            if(err.gmodel.errors){
              err.gmodel.errors.forEach(item => {
                vnodes.push(<div>{item.message}</div>)
              })
            }
            return (
              <div>
                {vnodes}
              </div>
            )
          },
          duration: 4
        });
      }else{
        that.$message.warn(`[${err.code}]${prefix}: ${err.message}`)
      }
    }
  },
  tableEnableState: function(that, sExtend, markArray, dftEnabled = true){
    let extendObj = sExtend || {}
    let allMarkEnabled = dftEnabled
    markArray.forEach((mark) => {
      let markEnabled = _.defaultTo(extendObj[_.toString(mark)], true)
      if(!markEnabled){
        allMarkEnabled = false
      }
    })
    return allMarkEnabled
  },
  /** 计算属性 */
  cptDragSourceOptions: function() {
    return {
      animation: 200,
      group: { name:'table', pull:'clone', put:false },
      ghostClass: 'ghost',
      sort: false,
    };
  },
  cptDragTargetOptions: function() {
    return {
      animation: 200,
      group: 'table',
      ghostClass: 'ghost',
      handle: '.drag-widget',
    };
  },
  /** Cols操作 */
  handleColsConfig: function(that){
    that.$refs.refCols?.showPop({}, {
        targetModel: that.sModel,
      },
      (item, context) => { /* submit */
        item || context
      }, () => { /* cancel */
        that.$emit('metaModel');
      }
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
      changeEvent || null
      // do nothing
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
    // 更新MetaPageComponentField的extend
    let cfPromise = MetaPageComponentField.$update({
      params: {
        id: item.id,
      },
      data: {
        title : item.title,
        component : item.component,
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
  handleOpsConfig: function(that, sModel){
    if(sModel){
      let recordObj = {}
      let extendObj = converters.parseObj(sModel.extend)
      // 定义请求路径
      let modelMetaDomain = _.get(sModel, 'metaDomain')
      if(modelMetaDomain===0){
        let uriPrefix = _.trim(extendObj['uriPrefix'])
        recordObj = Object.assign(recordObj, {
          uriPrefix: uriPrefix,
        })
      }
      // 定义新增编辑
      let addEnabled = _.defaultTo(extendObj[OTHER.ADD_ENABLED], true)
      let addCaption = _.trim(extendObj[OTHER.ADD_CAPTION])
      let editEnabled = _.defaultTo(extendObj[OTHER.EDIT_ENABLED], true)
      let editCaption = _.trim(extendObj[OTHER.EDIT_CAPTION])
      recordObj = Object.assign(recordObj, {
        addEnabled: addEnabled,
        addCaption: addCaption,
        editEnabled: editEnabled,
        editCaption: editCaption,
      })
      // 定义其它部分
      let deleteEnabled = _.defaultTo(extendObj[OTHER.DELETE_ENABLED], true)
      let batchEnabled = _.defaultTo(extendObj[OTHER.BATCH_ENABLED], true)
      let importEnabled = _.defaultTo(extendObj[OTHER.IMPORT_ENABLED], true)
      recordObj = Object.assign(recordObj, {
        deleteEnabled: deleteEnabled,
        batchEnabled: batchEnabled,
        importEnabled: importEnabled,
      })
      that.$refs.refOperation?.showPop(recordObj, {targetModel: sModel},
        (item, context) => { /* submit */
          return utilObj.handleOpsUpdate(that, sModel, item, context)
        }, () => { /* cancel */ }
      )
    }
  },
  handleOpsUpdate: function(that, sModel, item){
    if(sModel){
      let extendObj = converters.parseObj(sModel.extend)
      // 定义请求路径
      let modelMetaDomain = _.get(sModel, 'metaDomain')
      if(modelMetaDomain===0){
        extendObj['uriPrefix'] = item['uriPrefix']
      }
      // 定义新增编辑
      extendObj[OTHER.ADD_ENABLED] = item[OTHER.ADD_ENABLED]
      extendObj[OTHER.ADD_CAPTION] = _.trim(item[OTHER.ADD_CAPTION])
      extendObj[OTHER.EDIT_ENABLED] = item[OTHER.EDIT_ENABLED]
      extendObj[OTHER.EDIT_CAPTION] = _.trim(item[OTHER.EDIT_CAPTION])
      // 定义其它部分
      extendObj[OTHER.IMPORT_ENABLED] = item[OTHER.IMPORT_ENABLED]
      extendObj[OTHER.BATCH_ENABLED] = item[OTHER.BATCH_ENABLED]
      extendObj[OTHER.DELETE_ENABLED] = item[OTHER.DELETE_ENABLED]
      let promise = MetaPageComponent.$update({
        params: {
          id: sModel.id,
        },
        data: {
          extend: converters.encodeObj(extendObj)
        }
      })
      promise.then(() => {
        that.$emit('metaModel');
      })
      return promise
    }
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