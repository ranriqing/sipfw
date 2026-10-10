<template>
  <div>
    <GPlainPop
      ref="slotPop"
      class="pop-wrapper"
      :popVisible="popVisible"
      :configObj="configObj"
      :sContext="sContext"
      :data-cptFieldItems="cptFieldItems"
      debugComp="GDynrptPop">
      <template #default="{record, context}">
        <a-form :form="form">
          <a-form-item
            :label-col="cptLabelCol"
            :wrapper-col="cptWrapperCol"
            label="交叉报表">
            <a-switch
              checkedChildren="开启"
              unCheckedChildren="关闭"
              :checked="crossOn"
              @change="(checked) => _targetModelExtend('crossOn', checked)"/>
            <a-divider></a-divider>
          </a-form-item>
          <a-form-item
            :label-col="cptLabelCol"
            :wrapper-col="cptWrapperCol"
            label="报表标题">
            <a-button type="link" @click="handleReportTitle">
              {{ reportTitle || '暂无标题' }}
            </a-button>
            <a-divider></a-divider>
          </a-form-item>
          <a-form-item
            :label-col="cptLabelCol"
            :wrapper-col="cptWrapperCol"
            :label="crossOn?'列分组':'分组列'">
            <div class="draggable-cols-container">
              <draggable
                tag="div"
                :list="datColItems"
                v-bind="cptColDragTargetOptions"
                @change="(changeEvent) => handleItemDrag(changeEvent, 'dynrptCol')">
                <div
                  class="draggable-col"
                  v-for="(item, index) in datColItems"
                  :key="index">
                  <a-icon :type="vDftUtil.fieldIcon(item, 'type')" class="drag drag-widget"/>
                  <span class="column-item">{{ item.title }}</span>
                  <span class="item-actions">
                    <a-popconfirm title="确定删除?" @confirm="() => handleItemDelete(item)">
                      <a-icon class="icon-action" type="close" />
                    </a-popconfirm>
                  </span>
                </div>
              </draggable>
            </div>
            <a-button
              icon="select"
              type="dashed"
              :block="true"
              @click="handleColSelect('dynrptCol')">
              {{ crossOn?'列分组':'分组列' }}
            </a-button>
          </a-form-item>
          <a-form-item
            v-if="crossOn"
            :label-col="cptLabelCol"
            :wrapper-col="cptWrapperCol"
            label="行分组">
            <div class="draggable-cols-container">
              <draggable
                tag="div"
                :list="datRowItems"
                v-bind="cptRowDragTargetOptions"
                @change="(changeEvent) => handleItemDrag(changeEvent, 'dynrptRow')">
                <div
                  class="draggable-col"
                  v-for="(item, index) in datRowItems"
                  :key="index">
                  <a-icon :type="vDftUtil.fieldIcon(item, 'type')" class="drag drag-widget"/>
                  <span class="column-item">{{ item.title }}</span>
                  <span class="item-actions">
                    <a-popconfirm title="确定删除?" @confirm="() => handleItemDelete(item)">
                      <a-icon class="icon-action" type="close" />
                    </a-popconfirm>
                  </span>
                </div>
              </draggable>
            </div>
            <a-button
              icon="select"
              type="dashed"
              :block="true"
              @click="handleColSelect('dynrptRow')">
              行分组
            </a-button>
          </a-form-item>
          <a-form-item
            :label-col="cptLabelCol"
            :wrapper-col="cptWrapperCol"
            label="计算列">
            <div class="draggable-cols-container">
              <draggable
                tag="div"
                :list="datMeasureItems"
                v-bind="cptMeasureDragTargetOptions"
                @change="(changeEvent) => handleItemDrag(changeEvent, 'dynrptMeasure')">
                <div
                  class="draggable-col"
                  v-for="(item, index) in datMeasureItems"
                  :key="index">
                  <a-icon :type="vDftUtil.fieldIcon(item, 'type')" class="drag drag-widget"/>
                  <span class="column-item">{{ item.title }}</span>
                  <span class="item-actions">
                    <a-popconfirm title="确定删除?" @confirm="() => handleItemDelete(item)">
                      <a-icon class="icon-action" type="close" />
                    </a-popconfirm>
                  </span>
                </div>
              </draggable>
            </div>
            <a-button
              icon="select"
              type="dashed"
              :block="true"
              @click="handleColSelect('dynrptMeasure')">
              计算列
            </a-button>
          </a-form-item>
        </a-form>
      </template>
    </GPlainPop>

    <!-- Ref弹窗(Lab模式) -->
    <GFormPop
      ref="refPopAdd"
      :configObj="addPopConfig">
    </GFormPop>
  </div>
</template>

<script>
  import AAlert from 'ant-design-vue/lib/alert';
  import AEmpty from 'ant-design-vue/lib/empty';
  import AAvatar from 'ant-design-vue/lib/avatar';
  import ACheckbox from 'ant-design-vue/lib/checkbox';

  import { COMPO, FIELD } from '@/constants'
  import { converters, stores } from '@/utils/util'
  import { vDftUtil, vReportUtil } from '@/utilsv/vUtil'
  import { ormSeqOrders } from '@/api/api'

  import MetaDomainField from '@/store/models/MetaDomainField'
  import MetaPageComponentField from '@/store/models/MetaPageComponentField'

  import GPlainPop from '@/components/page_pop/GPlainPop'
  import GFormPop from '@/components/page_pop/GFormPop'
  import GDomainCols from '@/components/comp_form/GDomainCols'

  import labColsPopAddPopConfig from './configs/labColsPopAddPopConfig'
  import MetaDomain from '@/store/models/MetaDomain'
  import MetaPageComponent from '@/store/models/MetaPageComponent'

  export default {
    name: "GColsPop",
    components: {
      AAlert,
      AEmpty,
      AAvatar,
      ACheckbox,
      GPlainPop,
      GFormPop,
      GDomainCols,
    },
    /* eslint-disable */
    props: {
      popVisible: {
        type: Boolean,
        default: false,
      },
      configObj: {
        type: Object,
        default: function(){
          return {}
        }
      },
      sContext: {
        type: Object,
        default: function(){
          return {}
        }
      },
    },
    data () {
      return {
        /** 配置&状态 */
        form: this.$form.createForm(this),
        loading:false,
        stores: stores,
        vDftUtil: vDftUtil,
        addPopConfig: labColsPopAddPopConfig,
        /** 数据部分 */
        targetModel: undefined,
        targetMetaDomain: undefined,
        record: {},
        context: {},
        crossOn: false,
        reportTitle: undefined,
        datColItems: [],       // 镜像cptFieldItems
        datRowItems: [],       // 镜像cptFieldItems
        datMeasureItems: [],   // 镜像cptFieldItems
      }
    },
    computed: {
      cptLabelCol: function(){
        return this.configObj.labelCol || {
          /*xs: { span: 24 },
          sm: { span: 5 }*/
        }
      },
      cptWrapperCol: function(){
        return this.configObj.wrapperCol || {
          /*xs: { span: 24 },
          sm: { span: 15 }*/
        }
      },
      cptColDragTargetOptions: function() {
        return {
          animation: 200,
          group: 'cols',
          ghostClass: 'ghost',
          handle: '.draggable-col',
        };
      },
      cptRowDragTargetOptions: function() {
        return {
          animation: 200,
          group: 'rows',
          ghostClass: 'ghost',
          handle: '.draggable-col',
        };
      },
      cptMeasureDragTargetOptions: function() {
        return {
          animation: 200,
          group: 'measures',
          ghostClass: 'ghost',
          handle: '.draggable-col',
        };
      },
      cptFieldItems: function(){
        let that = this
        let metaFields = MetaPageComponentField.query().where('metaPageComponentId', that.targetModel?.id)
          .orderBy('seqOrder', 'asc').orderBy('id', 'asc').with('relMetaDomainField').get()
        let cloneItemFunc = vReportUtil.reportCloneItem
        let clonedMetaFields = metaFields.map(item => cloneItemFunc(that, item))
        that.datColItems = clonedMetaFields.filter(item => that._.get(item, 'extend.targetGroup')==='dynrptCol')
        that.datRowItems = clonedMetaFields.filter(item => that._.get(item, 'extend.targetGroup')==='dynrptRow')
        that.datMeasureItems = clonedMetaFields.filter(item => that._.get(item, 'extend.targetGroup')==='dynrptMeasure')
        return metaFields
      },
    },
    methods: {
      /** 弹窗操作 */
      isPage(){
        let that = this
        let slotRef = that.$refs.slotPop
        return slotRef.isPage()
      },
      showPop (record, context, submitCallback, cancelCallback) {
        let that = this
        that.form.resetFields()
        that.record = that._.pickBy(record || {}, (value, key) =>
          !that._.isNil(value) && !key.startsWith('$') && !that._.isFunction(value)
        )
        that.context = context || {}
        that.targetModel = that._.get(context, 'targetModel')
        that.targetMetaDomain = MetaDomain.find(that.targetModel?.metaDomainId)
        let extendObj = converters.parseObj(that.targetModel?.extend)
        that.crossOn = extendObj['crossOn']
        that.reportTitle = extendObj['reportTitle']
        // 展示弹窗
        let slotRef = that.$refs.slotPop
        return slotRef.showPop(record, context,
          (record, context) => { /* submit */
            record || context || true
            // 返回Promise
            return submitCallback(that.record, that.context)
          }, cancelCallback)
      },
      /** Item操作 */
      handleReportTitle(){
        let that = this
        let inputTitle = that.reportTitle
        that.$confirm({
          icon: 'edit',
          title: `请输入报表标题`,
          width: '600px',
          class: 'pop-column-wrapper',
          content: function(h){
            h && true
            let props = {
              value: inputTitle,
              placeholder: '报表标题...'
            }
            let attrs = { rows: 3, }
            const on = {
              change: (e) => {
                inputTitle = e.target.value
              },
            }
            return (<a-textarea {...{ attrs, props, on: on }}></a-textarea>)
          },
          onOk() {
            that._targetModelExtend('reportTitle', inputTitle)
          }
        });
      },
      handleColSelect(columnType){
        let that = this
        let isMeasure = columnType === 'dynrptMeasure'
        let selectedColumn = undefined
        that.$confirm({
          icon: 'check',
          title: `请选择报表列`,
          width: '600px',
          class: 'pop-column-wrapper',
          content: function(h){
            h && true
            let vnodes = [];
            let props = {
              isChart: true,
              isMeasure: isMeasure,
              echoOptionOn: true,
              metaDomains: [that.targetMetaDomain],
              fieldSupported: isMeasure?function(item){
                return [FIELD.INTEGER, FIELD.LONG, FIELD.BIG_DECIMAL].indexOf(item['fieldType'])!==-1
              }:undefined,
              placeholder: '选择列...'
            }
            const on = {
              change: (dfItemId) => {
                if('rowCount' === dfItemId){
                  selectedColumn = {
                    title: '计数',
                    fieldName: 'rowCount',
                    fieldType: FIELD.INTEGER,
                    metaDomainFieldId: undefined,
                  }
                }else if('dateCreated' === dfItemId){
                  selectedColumn = {
                    title: '创建时间',
                    fieldName: 'dateCreated',
                    fieldType: FIELD.DATE,
                  }
                }else if('lastUpdated' === dfItemId){
                  selectedColumn = {
                    title: '最近更新',
                    fieldName: 'lastUpdated',
                    fieldType: FIELD.DATE,
                  }
                }else{
                  let dfItem = MetaDomainField.find(dfItemId)
                  if(dfItem){
                    selectedColumn = {
                      id: dfItem.id,
                      title: dfItem.title,
                      fieldName: dfItem.fieldName,
                      fieldType: dfItem.fieldType,
                    }
                  }
                }

              },
            }
            vnodes.push(<GDomainCols {...{ props, on: on }}></GDomainCols>);
            return (<div {...{ class: 'selectable-wrapper' }}>{ vnodes }</div>)
          },
          onOk() {
            that.handleItemAdd(columnType, selectedColumn)
          }
        });
      },
      handleItemAdd(columnType, dfItem){
        let that = this
        let datTargetItems = []
        if(columnType === 'dynrptCol'){
          datTargetItems = that.datColItems
        }else if(columnType === 'dynrptRow'){
          datTargetItems = that.datRowItems
        }else if(columnType === 'dynrptMeasure'){
          datTargetItems = that.datMeasureItems
        }
        let fieldItemIndex = datTargetItems.findIndex(item => item.field === dfItem.fieldName)
        if(fieldItemIndex !== -1){
          that.$message.warn(`字段为 ${dfItem.fieldName} 的组件已存在`)
          return  // 不重复添加
        }
        let mappedItems = datTargetItems.map(item => {
          return {
            id: item.id,
            title: item.title,
            seqOrder: item.seqOrder
          }
        })
        let promise = MetaPageComponentField.$create({
          data: {
            title: dfItem.title,
            field: dfItem.fieldName,
            type: dfItem.fieldType,
            component: COMPO.G_DYN,
            seqOrder: 100,
            metaPageComponentId: that.targetModel?.id,
            metaDomainFieldId: dfItem.id, // 可能 undefined
            extend: converters.encodeObj({
              targetGroup: columnType,
            })
          }
        })
        promise.then(resp =>{
          let addedItem = resp
          if(addedItem){
            let mappedItem = {
              id: addedItem.id,
              title: addedItem.title,
              seqOrder: addedItem.seqOrder,
            }
            mappedItems.push(mappedItem)
            that._updateItemOrder(mappedItems)
          }
        })
        return promise
      },
      handleItemDrag(changeEvent, targetGroup){
        let that = this
        // 不能用that.datFieldItems，拖拽时排序已经改变
        let metaFields = MetaPageComponentField.query().where('metaPageComponentId', that.targetModel?.id)
          .orderBy('seqOrder', 'asc').orderBy('id', 'asc').get()
        let cloneItemFunc = vReportUtil.reportCloneItem
        let groupMetaFields = metaFields.map(item => cloneItemFunc(that, item)).filter(item => that._.get(item, 'extend.targetGroup')===targetGroup)
        let mappedItems = groupMetaFields.map(item => {
          return {
            id: item.id,
            title: item.title,
            seqOrder: item.seqOrder
          }
        })
        if(changeEvent['moved']){
          let oldIndex = that._.get(changeEvent, 'moved.oldIndex')
          let newIndex = that._.get(changeEvent, 'moved.newIndex')
          let item = mappedItems[oldIndex]
          mappedItems.splice(oldIndex, 1)
          mappedItems.splice(newIndex, 0, item)
          that._updateItemOrder(mappedItems)
        }
        if(changeEvent['added']){
          changeEvent || null
          // do nothing
        }
      },
      handleItemDelete(item){
        return MetaPageComponentField.$delete({
          params: {
            id: item.id,
          }
        })
      },
      /** 工具方法 */
      _targetModelExtend(property, value){
        let that = this
        if(that.targetModel){
          let extendObj = converters.parseObj(that.targetModel?.extend)
          let promise = MetaPageComponent.$update({
            params: {
              id: that.targetModel?.id,
            },
            data: {
              extend : converters.encodeObj(Object.assign(
                extendObj, {
                  [property]: value,
                }
              )),
            }
          })
          promise.then(() => {
            that.$set(that.$data, property, value)
          })
          return promise
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
  }
</script>

<style lang="scss" scoped>
  $tmpl-color-zero6: rgba(241, 139, 98, 0.6);
  $tmpl-color-zero8: rgba(241, 139, 98, 0.8);
  $tmpl-color-zero10: rgba(241, 139, 98, 1);

  .bug-icon {
    color: $tmpl-color-zero6;
    margin-right: 6px;
  }
  .pop-wrapper{
    .ant-form-item {
      margin-bottom: 4px;
      /deep/ .ant-form-item-label {
        line-height: 26px;
      }
    }
    .draggable-cols-container{
      display: flex;
      flex-direction: column;
      // 暂无组件
      &:empty{
        &:before
        {
          content:"暂无组件";
          font-size: 16px;
          color: $tmpl-color-zero6;
          text-align: center;
          display: block;
        }
      }
      // 拖拽条目
      .draggable-col{
        align-items: center;
        border-radius: 5px;
        border: 1px solid #e5e5e5;
        position: relative;
        width: 100%;
        background-color: #f9f9fa;
        line-height: 28px;
        margin-bottom: 8px;
        // cursor: grab;
        // 条目内容
        .column-item {
          display: inline-block;
          margin-left: 6px;
        }
        .drag{
          font-size: 14px;
          padding: 4px 2px 4px 6px;
          cursor: move;
        }
        // 条目操作按钮
        .item-actions{
          position: absolute;
          top: 1px;
          right: 6px;
          .icon-action {
            vertical-align: baseline;
            font-size: 14px;
            padding: 4px 0;
            transform: translateY(2px);
            transition: all .3s;
            cursor: pointer;
            color: #606266;
          }
        }
      }
    }
  }
</style>