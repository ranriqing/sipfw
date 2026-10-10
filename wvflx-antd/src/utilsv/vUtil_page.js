import { OTHER } from '@/constants'
import { tools, converters } from '@/utils/util'

import MetaPage from '@/store/models/MetaPage'
import MetaPageLink from '@/store/models/MetaPageLink'
import MetaPageComponent from '@/store/models/MetaPageComponent'
import MetaPageComponentField from '@/store/models/MetaPageComponentField'
import _ from '@/utils/tiny_lodash'
import { ormCopyComponent, ormSeqOrders } from '@/api/api'
import CompFactory from "@/utilsv/comps";
import MetaDomain from "@/store/models/MetaDomain";

let utilObj = {
  /** 克隆方法 */
  pageCloneItem: function(that, linkItem, componentItem) {
    let metaDomain = MetaDomain.find(componentItem.metaDomainId)
    let compoInstance = CompFactory.getInstanceByType(metaDomain?.domainName, componentItem.type);
    return compoInstance.pageCloneItem(that, metaDomain?.domainName, linkItem, componentItem);
  },
  /** 计算属性 */
  cptComponentData: function(){
    return {
      attrs: {
        type: 'flex',
        align: 'stretch',
        gutter: [12, 12],
        /*style: {
          'margin-left': 0,
          'margin-right': 0,
        }*/
      },
    }
  },
  cptDragPopOptions: function() {
    return {
      animation: 200,
      group: 'pop',
      ghostClass: 'ghost',
      handle: '.pop-item-widget',
    };
  },
  cptDragPartOptions: function() {
    return {
      animation: 200,
      group: 'part',
      ghostClass: 'ghost',
      handle: '.part-wrapper-widget',
    };
  },
  /** 拖拽方法 */
  handlePageZone: function(that){
    that.$refs.refPageZone?.showPop({}, {},
      async (item, context) => { /* submit */
        context || true
        // 创建内置组件
        if(item.type === OTHER.PART_WIDGET && !item.id){
          let pageIdent = that.routePage.ident
          await MetaPageComponent.$create({
            data: {
              title: item.title,
              pIdent: pageIdent,
              ident: `${pageIdent}_PI${tools.randomString(8)}`,
              type: OTHER.PART_WIDGET,
              component: item.component,
              metaDomainId: that.metaDomain?.id,
            }
          }).then(resp =>{
            let addedItem = resp
            if(addedItem){
              item = MetaPageComponent.find(addedItem.id)
            }
          })
        }
        // 页面绑定组件
        let promise = undefined
        let itemLinkCount = MetaPageLink.query().where('metaPageId', that.routePage?.id)
          .where('metaPageComponentId', item.id).count()
        if(itemLinkCount>0){
          that.$message.warn('页面已经绑定此组件')
          promise = Promise.resolve()
        }else{
          let pageLinks = MetaPageLink.query().where('metaPageId', that.routePage?.id).get()
          let maxSeqOrder = pageLinks.length ? Math.max(...pageLinks.map(l => l.seqOrder != null ? l.seqOrder : 0)) : 0
          promise = MetaPageLink.$create({
            data: {
              seqOrder: maxSeqOrder + 1,
              metaPageId: that._.get(that.routePage, 'id'),
              metaPageComponentId: item.id,
            }
          })
        }
        return promise
      }, () => {
        // 加载元信息
        if(that._.isFunction(that.loadMetaModels)){
          that.loadMetaModels()
        }
      }
    )
  },
  /** 拖拽方法 */
  handlePopDrag: function(that, changeEvent){
    // 不能用that.datMetaPops，拖拽时排序已经改变
    let pageLinks = MetaPageLink.query().where('metaPageId', that.routePage?.id)
      .orderBy('seqOrder', 'asc').orderBy('id', 'asc').with('relMetaPageComponent').get()
    let metaPopLinks = pageLinks.filter(linkItem => {
      let componentItem = linkItem['relMetaPageComponent']
      return [OTHER.POP_ADD, OTHER.POP_EDIT, OTHER.POP_BATCH, OTHER.POP_FLOW].indexOf(componentItem?.type) !== -1
    })
    let mappedItems = metaPopLinks.map(item => {
      return {
        linkId: item.id,
        linkSeqOrder: item.seqOrder
      }
    })
    if(changeEvent['moved']){
      let oldIndex = _.get(changeEvent, 'moved.oldIndex')
      let newIndex = _.get(changeEvent, 'moved.newIndex')
      let item = mappedItems[oldIndex]
      mappedItems.splice(oldIndex, 1)
      mappedItems.splice(newIndex, 0, item)
      utilObj._updateLinkItemOrder(that, mappedItems)
    }
    if(changeEvent['added']){
      changeEvent || null
      // do nothing
    }
  },
  handlePartDrag: function(that, changeEvent, parts){
    // 不能用that.datMetaParts，拖拽时排序已经改变
    let pageLinks = MetaPageLink.query().where('metaPageId', that.routePage?.id)
      .orderBy('seqOrder', 'asc').orderBy('id', 'desc').with('relMetaPageComponent').get()
    let metaPartLinks = pageLinks.filter(linkItem => {
      let componentItem = linkItem['relMetaPageComponent']
      return (parts || [
        OTHER.PART_QUERY, OTHER.PART_TABLE, OTHER.PART_CHART,
        OTHER.PART_REPORT, OTHER.PART_DYNRPT, OTHER.PART_MDOWN,
        OTHER.PART_WIDGET,
      ]).indexOf(componentItem?.type) !== -1
    })
    let mappedItems = metaPartLinks.map(item => {
      return {
        linkId: item.id,
        linkSeqOrder: item.seqOrder
      }
    })
    if(changeEvent['moved']){
      let oldIndex = _.get(changeEvent, 'moved.oldIndex')
      let newIndex = _.get(changeEvent, 'moved.newIndex')
      let item = mappedItems[oldIndex]
      mappedItems.splice(oldIndex, 1)
      mappedItems.splice(newIndex, 0, item)
      utilObj._updateLinkItemOrder(that, mappedItems)
    }
    if(changeEvent['added']){
      changeEvent || null
      // do nothing
    }
  },
  /** 操作方法 */
  handlePopEdit: function(that, item){
    that.$refs.refPopMeta?.showPop(item, {},
      (item, context) => { /* submit */
        return utilObj.handlePopUpdate(that, item, context)
      }, () => { /* cancel */ }
    )
  },
  handlePopUpdate: function(that, item){
    let promise = MetaPageComponent.$update({
      params: {
        id: item.id,
      },
      data: {
        title : item.title,
        extend : converters.encodeObj(item.extend)
      }
    })
    promise.then(() =>{
      // 加载元信息
      if(that._.isFunction(that.loadMetaModels)){
        that.loadMetaModels()
      }
    })
    return promise
  },
  handlePopDelete: function(that, item){
    let promise = MetaPageLink.$delete({
      params: {
        id: item.linkId,
      }
    })
    promise.then(() =>{
      // 加载元信息
      if(that._.isFunction(that.loadMetaModels)){
        that.loadMetaModels()
      }
    })
    return promise
  },
  /** 拷贝方法 */
  handlePopCopy: function(that, metaPop, options){
    options = options || {}
    if(!metaPop || !metaPop.id){
      if(that && that.$message && that.$message.warn){
        that.$message.warn('没找到要拷贝的弹窗组件')
      }
      return
    }
    let dstType = metaPop.type
    let dstTitle = `${metaPop.title || '弹窗'}-拷贝`
    let h = that.$createElement
    that.$confirm({
      title: `拷贝弹窗组件【${metaPop.title}】`,
      content: h('div', [
        h('a-input', {
          props: { defaultValue: dstTitle },
          style: { width: '100%', marginBottom: '10px' },
          attrs: { placeholder: '请输入目标组件名称' },
          on: {
            change: (e) => { dstTitle = (e && e.target ? e.target.value : '').toString().trim() }
          }
        }),
        h('a-select', {
          props: { defaultValue: dstType },
          style: { width: '100%', marginBottom: '10px' },
          attrs: { placeholder: '请选择目的弹窗类型' },
          on: { change: (val) => { dstType = val } }
        }, [
          h('a-select-option', { props: { value: OTHER.POP_ADD } }, ['新增弹窗']),
          h('a-select-option', { props: { value: OTHER.POP_EDIT } }, ['编辑弹窗']),
          h('a-select-option', { props: { value: OTHER.POP_BATCH } }, ['批量弹窗']),
          h('a-select-option', { props: { value: OTHER.POP_FLOW } }, ['流程弹窗']),
        ])
      ]),
      onOk() {
        let dstComponent = metaPop.component
        if(options.compTypeObj){
          dstComponent = _.get(options.compTypeObj, `[${dstType}].component`) || dstComponent
        }else{
          // 默认推断：批量弹窗使用BatchPop，其它尽量沿用源组件
          if(dstType === OTHER.POP_BATCH){
            dstComponent = 'BatchPop'
          }
        }
        let dstMetaDomainId = metaPop.metaDomainId
        let promise = ormCopyComponent({
          srcComponentId: metaPop.id,
          dstType: dstType,
          dstComponent: dstComponent,
          dstMetaDomainId: dstMetaDomainId,
          dstTitle: (dstTitle || '').trim() || undefined,
        })
        return promise.then(async (resp) => {
          let metaEntitys = resp.gmodel
          let createdComponents = _.get(metaEntitys, 'metaPageComponent', [])
          let createdComponent = createdComponents && createdComponents.length ? createdComponents[0] : null

          MetaPageComponent.insertOrUpdate({
            data: createdComponents
          })
          MetaPageComponentField.insertOrUpdate({
            data: _.get(metaEntitys, 'metaPageComponentField', [])
          })

          if(_.isFunction(options.afterCopy)){
            await options.afterCopy(metaEntitys, createdComponent, { dstType, dstComponent, dstMetaDomainId, dstTitle })
          }

          if(that._.isFunction(that.loadMetaModels)){
            that.loadMetaModels()
          }
          that.$message.success('拷贝成功')
          return resp
        }).catch(err => {
          err || true
          that.$message.warn('拷贝失败')
          return Promise.reject(err)
        })
      }
    })
  },
  handlePartCopy: function(that, metaPart, options){
    options = options || {}
    if(!metaPart || !metaPart.id){
      if(that && that.$message && that.$message.warn){
        that.$message.warn('没找到要拷贝的区块组件')
      }
      return
    }
    let dstTitle = `${metaPart.title || '区块'}-拷贝`
    let h = that.$createElement
    that.$confirm({
      title: `拷贝区块组件【${metaPart.title}】`,
      content: h('div', [
        h('a-input', {
          props: { defaultValue: dstTitle },
          style: { width: '100%', marginBottom: '10px' },
          attrs: { placeholder: '请输入目标组件名称' },
          on: {
            change: (e) => { dstTitle = (e && e.target ? e.target.value : '').toString().trim() }
          }
        }),
        h('div', [`将拷贝到相同类型【${metaPart.type}】`]),
      ]),
      onOk() {
        let dstMetaDomainId = _.isFunction(options.getDstMetaDomainId)
          ? options.getDstMetaDomainId(metaPart)
          : metaPart.metaDomainId

        let promise = ormCopyComponent({
          srcComponentId: metaPart.id,
          dstType: metaPart.type,
          dstComponent: metaPart.component,
          dstMetaDomainId: dstMetaDomainId,
          dstTitle: (dstTitle || '').trim() || undefined,
        })
        return promise.then(async (resp) => {
          let metaEntitys = resp.gmodel
          let createdComponents = _.get(metaEntitys, 'metaPageComponent', [])
          let createdComponent = createdComponents && createdComponents.length ? createdComponents[0] : null

          MetaPageComponent.insertOrUpdate({
            data: createdComponents
          })
          MetaPageComponentField.insertOrUpdate({
            data: _.get(metaEntitys, 'metaPageComponentField', [])
          })

          if(_.isFunction(options.afterCopy)){
            await options.afterCopy(metaEntitys, createdComponent, { dstType: metaPart.type, dstComponent: metaPart.component, dstMetaDomainId, dstTitle })
          }

          if(that._.isFunction(that.loadMetaModels)){
            that.loadMetaModels()
          }
          that.$message.success('拷贝成功')
          return resp
        }).catch(err => {
          err || true
          that.$message.warn('拷贝失败')
          return Promise.reject(err)
        })
      }
    })
  },
  handlePartEdit: function(that, item){
    that.$refs.refPartMeta?.showPop(item, {},
      (item, context) => { /* submit */
        return utilObj.handlePartUpdate(that, item, context)
      }, () => { /* cancel */ }
    )
  },
  handlePartUpdate: function(that, item){
    let promise = MetaPageComponent.$update({
      params: {
        id: item.id,
      },
      data: {
        title : item.title,
        extend : converters.encodeObj(item.extend)
      }
    })
    promise.then(() =>{
      // 加载元信息
      if(that._.isFunction(that.loadMetaModels)){
        that.loadMetaModels()
      }
    })
    return promise
  },
  handlePartDelete: function(that, item){
    let promise = MetaPageLink.$delete({
      params: {
        id: item.linkId,
      }
    })
    promise.then(() =>{
      // 加载元信息
      if(that._.isFunction(that.loadMetaModels)){
        that.loadMetaModels()
      }
    })
    return promise
  },
  /** 配置部分 */
  popConfig: function(that, metaPop, configObj, forceConfigObj){
    configObj = configObj || {}
    forceConfigObj = forceConfigObj || {}
    // 读取配置
    let extendObj = that._.get(metaPop, 'extend') || {}
    let popTitle = extendObj['popTitle']
    let popType = extendObj['popType']
    let submitText = extendObj['submitText']
    // 封装配置
    if(popTitle){
      configObj['title'] = popTitle
    }
    if(popType){
      configObj.modal = undefined
      configObj.drawer = undefined
      configObj.page = undefined
      if(popType === 'Modal'){
        configObj.modal = true
      }else if(popType === 'Drawer'){
        configObj.drawer = true
      }else if(popType === 'Page'){
        configObj.page = true
      }
    }
    if(submitText){
      configObj['submitText'] = submitText
    }
    that._.forIn(forceConfigObj, (value, key)=>{
      configObj[key] = value
    })
    return configObj
  },
  /** 初始化方法 */
  initDefaultParts: function(that){
    if(that.routePage && !that.routePage.initialized){
      let allPromise = []
      let allEmbedPromise = []
      let promise = null
      let embedPromise = null
      let pageIdent = that.routePage.ident
      // 查询部分
      promise = MetaPageComponent.$create({
        data: {
          title: '查询',
          pIdent: pageIdent,
          ident: `${pageIdent}_PQ${tools.randomString(8)}`,
          type: OTHER.PART_QUERY,
          component: 'PartQuery',
          metaDomainId: that.metaDomain?.id,
        }
      })
      promise.then(resp =>{
        let addedItem = resp
        if(addedItem){
          embedPromise = MetaPageLink.$create({
            data: {
              seqOrder: 1,
              metaPageId: that._.get(that.routePage, 'id'),
              metaPageComponentId: addedItem.id,
            }
          })
          allEmbedPromise.push(embedPromise)
        }
      })
      allPromise.push(promise)
      // 表格部分
      promise = MetaPageComponent.$create({
        data: {
          title: '表格',
          pIdent: pageIdent,
          ident: `${pageIdent}_PT${tools.randomString(8)}`,
          type: OTHER.PART_TABLE,
          component: 'PartTable',
          metaDomainId: that.metaDomain?.id,
        }
      })
      promise.then(resp =>{
        let addedItem = resp
        if(addedItem){
          embedPromise = MetaPageLink.$create({
            data: {
              seqOrder: 2,
              metaPageId: that._.get(that.routePage, 'id'),
              metaPageComponentId: addedItem.id,
            }
          })
          allEmbedPromise.push(embedPromise)
        }
      })
      allPromise.push(promise)
      // 新增弹窗
      promise = MetaPageComponent.$create({
        data: {
          title: '新增',
          pIdent: pageIdent,
          ident: `${pageIdent}_PA${tools.randomString(8)}`,
          type: OTHER.POP_ADD,
          component: 'FormPop',
          metaDomainId: that.metaDomain?.id,
        }
      })
      promise.then(resp =>{
        let addedItem = resp
        if(addedItem){
          embedPromise = MetaPageLink.$create({
            data: {
              seqOrder: 1,
              metaPageId: that._.get(that.routePage, 'id'),
              metaPageComponentId: addedItem.id,
            }
          })
          allEmbedPromise.push(embedPromise)
        }
      })
      allPromise.push(promise)
      // 编辑弹窗
      promise = MetaPageComponent.$create({
        data: {
          title: '编辑',
          pIdent: pageIdent,
          ident: `${pageIdent}_PE${tools.randomString(8)}`,
          type: OTHER.POP_EDIT,
          component: 'FormPop',
          metaDomainId: that.metaDomain?.id,
        }
      })
      promise.then(resp =>{
        let addedItem = resp
        if(addedItem){
          embedPromise = MetaPageLink.$create({
            data: {
              seqOrder: 2,
              metaPageId: that._.get(that.routePage, 'id'),
              metaPageComponentId: addedItem.id,
            }
          })
          allEmbedPromise.push(embedPromise)
        }
      })
      allPromise.push(promise)
      // 批量弹窗
      promise = MetaPageComponent.$create({
        data: {
          title: '批量',
          pIdent: pageIdent,
          ident: `${pageIdent}_PB${tools.randomString(8)}`,
          type: OTHER.POP_BATCH,
          component: 'BatchPop',
          metaDomainId: that.metaDomain?.id,
        }
      })
      promise.then(resp =>{
        let addedItem = resp
        if(addedItem){
          embedPromise = MetaPageLink.$create({
            data: {
              seqOrder: 3,
              metaPageId: that._.get(that.routePage, 'id'),
              metaPageComponentId: addedItem.id,
            }
          })
          allEmbedPromise.push(embedPromise)
        }
      })
      allPromise.push(promise)
      Promise.all(allPromise).then(() => {
        Promise.all(allEmbedPromise).then(() => {
          MetaPage.$update({
            params: {
              id: that.routePage.id,
            },
            data: {
              initialized: true
            }
          })
          // 加载元信息
          if(that._.isFunction(that.loadMetaModels)){
            that.loadMetaModels()
          }
        })

      })
    }
  },
  /** 工具方法 */
  _updateLinkItemOrder(that, mappedItems){
    let itemOrders = []
    mappedItems.forEach((item, index) => {
      let itemData = {id: item.linkId, seqOrder: index+1}
      itemOrders.push(itemData)
    })
    ormSeqOrders('metaPageLink', {items: itemOrders}).then(resp => {
      let entitys = resp.gmodel
      let promise = MetaPageLink.insertOrUpdate({
        data: entitys
      })
      promise.then(() =>{
        // 加载元信息
        if(that._.isFunction(that.loadMetaModels)){
          that.loadMetaModels()
        }
      })
    })
  },
}

export default utilObj