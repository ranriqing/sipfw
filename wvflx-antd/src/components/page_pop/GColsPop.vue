<template>
  <div>
    <GPlainPop
      ref="slotPop"
      class="pop-wrapper"
      :popVisible="popVisible"
      :configObj="configObj"
      :sContext="sContext"
      :data-cptFieldItems="cptFieldItems"
      debugComp="GColsPop">
      <template #default="{record, context}">
        <a-form :form="form">
          <a-form-item
            :label-col="cptLabelCol"
            :wrapper-col="cptWrapperCol"
            label="定义组件">
            <template v-if="targetModel && targetModel.metaDomainId!==0">
              <div class="flex flex-row cross-center">
                <GDomainCols
                  class="item-flex"
                  :echoOptionOn="false"
                  :metaDomains="[targetMetaDomain]"
                  :fieldSupported="_fieldSupportedFunc()"
                  @change="handleDomainCol($event)">
                </GDomainCols>
                <a-button
                  style="margin-left: 6px;"
                  icon="plus"
                  type="dashed"
                  @click="handlePopAdd">
                </a-button>
              </div>
            </template>
            <template v-else>
              <a-button
                icon="plus"
                type="dashed"
                :block="true"
                @click="handlePopAdd">
                添加组件
              </a-button>
            </template>
          </a-form-item>
          <a-form-item
            v-if="extraFields.length>0"
            :label-col="cptLabelCol"
            :wrapper-col="cptWrapperCol"
            label="可选组件">
            <div class="extra-cols-container">
              <div
                class="extra-col"
                style="padding-left: 8px;"
                v-for="(item, index) in extraFields"
                :key="index">
                <a-checkbox
                  :value="item.id"
                  :checked="_itemChecked(item)"
                  @change="handleItemSwitch($event, item)">
                  {{ item.title }}
                </a-checkbox>
              </div>
            </div>
          </a-form-item>
          <a-form-item
            v-if="_modelSupportPanel()"
            :label-col="cptLabelCol"
            :wrapper-col="cptWrapperCol"
            label="">
            <a-button
              icon="plus"
              type="dashed"
              :block="true"
              @click="handleItemAdd({
                title: '折叠分组',
                fieldName: 'panel',
                fieldType: 'Panel',
              })">
              折叠分组
            </a-button>
          </a-form-item>
          <a-form-item
            v-if="datFieldItems.length>0"
            :label-col="cptLabelCol"
            :wrapper-col="cptWrapperCol"
            label="全部组件">
            <div class="draggable-cols-container">
              <draggable
                tag="div"
                :list="datFieldItems"
                v-bind="cptDragTargetOptions"
                @change="(changeEvent) => handleItemDrag(changeEvent)">
                <div
                  class="draggable-col"
                  v-for="(item, index) in datFieldItems"
                  :key="index">
                  <a-icon :type="vDftUtil.fieldIcon(item, 'type')" class="drag drag-widget"/>
                  <span class="column-item">{{ item.title }}</span>
                  <span class="item-actions">
                    <template v-if="cptMetaPopEdit">
                      <a-icon type="edit" @click="vPopUtil.handleItemEdit(_self, item, {metaPopTable: cptMetaPopTable})"/>
                      <a-divider type="vertical" style="margin: 0 8px;" />
                    </template>
                    <a-popconfirm title="确定删除?" @confirm="() => handleItemDelete(item)">
                      <a-icon class="icon-action" type="close" />
                    </a-popconfirm>
                  </span>
                </div>
              </draggable>
            </div>
          </a-form-item>
        </a-form>
      </template>
    </GPlainPop>

    <!-- Ref弹窗(Lab模式) -->
    <GFormPop
      ref="refPopAdd"
      :configObj="addPopConfig">
    </GFormPop>
    <GFormPop
      ref="refMeta"
      :configObj="metaPopConfig">
    </GFormPop>
  </div>
</template>

<script>
  import AAlert from 'ant-design-vue/lib/alert';
  import AEmpty from 'ant-design-vue/lib/empty';
  import AAvatar from 'ant-design-vue/lib/avatar';
  import ACheckbox from 'ant-design-vue/lib/checkbox';

  import { FIELD, COMPO, OTHER, DATE } from '@/constants'
  import { converters, tools, stores } from '@/utils/util'
  import { vDftUtil, vQueryUtil, vTableUtil, vReportUtil, vPopUtil } from '@/utilsv/vUtil'
  import { ormSeqOrders } from '@/api/api'

  import MetaDomainField from '@/store/models/MetaDomainField'
  import MetaPageComponentField from '@/store/models/MetaPageComponentField'

  import GPlainPop from '@/components/page_pop/GPlainPop'
  import GFormPop from '@/components/page_pop/GFormPop'
  import GDomainCols from '@/components/comp_form/GDomainCols'

  import labColsPopAddPopConfig from './configs/labColsPopAddPopConfig'
  import labPopMetaPopConfig from '@/components/tmpl_pop/configs/labPopMetaPopConfig'
  import MetaDomain from '@/store/models/MetaDomain'
  import MetaPageComponent from "@/store/models/MetaPageComponent";

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
        vPopUtil: vPopUtil,
        addPopConfig: labColsPopAddPopConfig,
        metaPopConfig: labPopMetaPopConfig,
        /** 数据部分 */
        targetModel: undefined,
        targetNodeIdent: undefined,
        targetMetaDomain: undefined,
        targetColsFields: undefined,
        extraFields: [],
        record: {},
        context: {},
        datFieldItems: [],  // 镜像cptFieldItems
      }
    },
    computed: {
      cptMetaPopEdit: function(){
        return this.configObj.metaPopEdit || false
      },
      cptMetaPopTable: function(){
        return this.configObj.metaPopTable || false
      },
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
      cptDragTargetOptions: function() {
        return {
          animation: 200,
          group: 'cols',
          ghostClass: 'ghost',
          handle: '.draggable-col',
        };
      },
      cptFieldItems: function(){
        let that = this
        let fieldQuery = MetaPageComponentField.query()
          .where('metaPageComponentId', that.targetModel?.id)
        if(that.targetNodeIdent){
          fieldQuery.where('pIdent', that.targetNodeIdent)
        }
        let metaFields = fieldQuery.orderBy('seqOrder', 'asc')
          .orderBy('id', 'asc').with('relMetaDomainField').get()
        let cloneItemFunc = that._cloneItemFunc()
        that.datFieldItems = metaFields.map(item => cloneItemFunc(that, item))
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
        that.targetNodeIdent = that._.get(context, 'targetNodeIdent')
        that.targetMetaDomain = MetaDomain.find(that.targetModel?.metaDomainId)
        that.targetColsFields = that._.get(context, 'targetColsFields')
        that.extraFields = that._modelExtraFields()
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
      handlePopAdd: function(){
        let that = this
        let slotRef = that.$refs.refPopAdd
        slotRef.showPop({}, {},
          (record, context) => { /* submit */
            context || true
            let dfItem = {
              title: record.title,
              fieldName: record.fieldName,
              fieldType: record.fieldType
            }
            return that.handleItemAdd(dfItem)
          }, () => { /* cancel */ }
        )
      },
      handleDomainCol(dfItemId){
        let that = this
        let dfItem = MetaDomainField.find(dfItemId)
        if(dfItem) {
          that.handleItemAdd(dfItem)
        }
      },
      handleItemSwitch(e, dfItem){
        let that = this
        let checked = e.target.checked
        if(checked){
          return that.handleItemAdd(dfItem)
        }else{
          let fieldItem = that.datFieldItems.find(item => item.field === dfItem.fieldName)
          if(fieldItem){
            return that.handleItemDelete(fieldItem)
          }
        }
      },
      handleItemAdd(dfItem){
        let that = this
        let fieldItemIndex = that.datFieldItems.findIndex(item => item.field === dfItem.fieldName)
        if(fieldItemIndex !== -1){
          that.$message.warn(`字段为 ${dfItem.fieldName} 的组件已存在`)
          return  // 不重复添加
        }
        let mappedItems = that.datFieldItems.map(item => {
          return {
            id: item.id,
            title: item.title,
            seqOrder: item.seqOrder
          }
        })
        // 特殊处理: 支持创建多个Panel组件
        let fieldName = dfItem.fieldName
        if(dfItem['fieldType']===FIELD.V_PANEL){
          fieldName = `${fieldName}_${tools.randomString(5)}`
        }
        let promise = MetaPageComponentField.$create({
          data: {
            title: dfItem.title,
            field: fieldName,
            type: dfItem.fieldType,
            pIdent: that.targetNodeIdent, // 可能为null
            component: COMPO.G_DYN,
            seqOrder: 100,
            metaPageComponentId: that.targetModel?.id,
            metaDomainFieldId: dfItem.id, // 可能 undefined
            // extend: dfItem.id?dfItem.extendJson:undefined
            extend: dfItem.dftExtend || undefined,
          }
        })
        // 关联字段添加
        promise.then(resp =>{
          let addedItem = resp
          if([FIELD.MANY_TO_ONE, FIELD.ONE_TO_MANY].indexOf(addedItem['type'])!==-1){
            let dfExtend = converters.parseObj(dfItem?.extendJson)
            let relMetaDomain = MetaDomain.find(dfExtend[OTHER.REL_DOMAIN_ID])
            let fieldIdent = `${addedItem.id}_${addedItem.field}`
            MetaPageComponent.$create({
              data: {
                title: `字段组件_${dfItem.title}`,
                pIdent: that.targetNodeIdent,
                ident: fieldIdent,
                type: OTHER.FIELD_COMPONENT,
                component: COMPO.G_NONE,
                metaDomainId: relMetaDomain?.id,
              }
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
      handleItemDrag(changeEvent){
        let that = this
        // 不能用that.datFieldItems，拖拽时排序已经改变
        let fieldQuery = MetaPageComponentField.query()
          .where('metaPageComponentId', that.targetModel?.id)
        if(that.targetNodeIdent){
          fieldQuery.where('pIdent', that.targetNodeIdent)
        }
        let metaFields = fieldQuery.orderBy('seqOrder', 'asc').orderBy('id', 'asc').get()
        let mappedItems = metaFields.map(item => {
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
      _modelSupportPanel(){
        let that = this
        let modelType = that.targetModel?.type
        return [
          OTHER.POP_ADD, OTHER.POP_EDIT, OTHER.POP_BATCH,
          OTHER.POP_FLOW, OTHER.POP_PROCESS
        ].indexOf(modelType) !== -1
      },
      _modelExtraFields(){
        let that = this
        let extraFields = []
        let modelType = that.targetModel?.type
        let modelMetaDomainId = that.targetModel?.metaDomainId
        if(that.targetColsFields){
          return that.targetColsFields
        }
        if([OTHER.PART_QUERY].indexOf(modelType) !== -1){
          if(modelMetaDomainId && modelMetaDomainId!==0){
            extraFields.push(...[
              {
                title: '创建人',
                fieldName: 'creater',
                fieldType: FIELD.USER,
              },
              {
                title: '更新人',
                fieldName: 'updater',
                fieldType: FIELD.USER,
              },
              {
                title: '创建时间',
                fieldName: 'dateCreated',
                fieldType: FIELD.DATE,
                dftExtend: JSON.stringify({format: DATE.YYYYMMDD_HHMMSS})
              },
              {
                title: '最近更新',
                fieldName: 'lastUpdated',
                fieldType: FIELD.DATE,
                dftExtend: JSON.stringify({format: DATE.YYYYMMDD_HHMMSS})
              },
              {
                title: '标签',
                fieldName: FIELD.V_TAGS.toLowerCase(),
                fieldType: FIELD.V_TAGS,
              },
            ])
          }
        }else if([OTHER.PART_TABLE].indexOf(modelType) !== -1){
          extraFields.unshift({
            title: 'ID',
            fieldName: 'id',
            fieldType: FIELD.V_ID,
          })
          if(modelMetaDomainId && modelMetaDomainId!==0){
            extraFields.push(...[
              {
                title: '创建人',
                fieldName: 'creater',
                fieldType: FIELD.USER,
              },
              {
                title: '更新人',
                fieldName: 'updater',
                fieldType: FIELD.USER,
              },
              {
                title: '创建时间',
                fieldName: 'dateCreated',
                fieldType: FIELD.DATE,
                dftExtend: JSON.stringify({format: DATE.YYYYMMDD_HHMMSS})
              },
              {
                title: '最近更新',
                fieldName: 'lastUpdated',
                fieldType: FIELD.DATE,
                dftExtend: JSON.stringify({format: DATE.YYYYMMDD_HHMMSS})
              },
            ])
            extraFields.push(...[
              {
                title: '下一活动',
                fieldName: FIELD.V_ACTIVITIES.toLowerCase(),
                fieldType: FIELD.V_ACTIVITIES,
              },
            ])
            extraFields.push(...[
              {
                title: '流程状态',
                fieldName: FIELD.V_PROC_STATUS.toLowerCase(),
                fieldType: FIELD.V_PROC_STATUS,
              },
            ])
            extraFields.push(...[
              {
                title: '相关附件',
                fieldName: FIELD.V_ATTACHMENTS.toLowerCase(),
                fieldType: FIELD.V_ATTACHMENTS,
              },
            ])
            extraFields.push(...[
              {
                title: '标签',
                fieldName: FIELD.V_TAGS.toLowerCase(),
                fieldType: FIELD.V_TAGS,
              },
            ])
          }
          extraFields.push({
            title: '操作',
            fieldName: 'action',
            fieldType: FIELD.V_ACTION,
          })
        }else if([OTHER.PART_REPORT, OTHER.PART_DYNRPT].indexOf(modelType) !== -1){
          extraFields.unshift({
            title: '计数',
            fieldName: 'rowCount',
            fieldType: FIELD.INTEGER,
          })
          if(modelMetaDomainId && modelMetaDomainId!==0){
            extraFields.push(...[
              {
                title: '创建时间',
                fieldName: 'dateCreated',
                fieldType: FIELD.DATE,
                dftExtend: JSON.stringify({format: DATE.YYYYMMDD_HHMMSS})
              },
              {
                title: '最近更新',
                fieldName: 'lastUpdated',
                fieldType: FIELD.DATE,
                dftExtend: JSON.stringify({format: DATE.YYYYMMDD_HHMMSS})
              },
            ])
          }
        }else if([OTHER.POP_ADD, OTHER.POP_EDIT].indexOf(modelType) !== -1) {
          if(modelMetaDomainId && modelMetaDomainId!==0){
            extraFields.push(...[
              {
                title: '相关活动',
                fieldName: FIELD.V_ACTIVITIES.toLowerCase(),
                fieldType: FIELD.V_ACTIVITIES,
                dftExtend: JSON.stringify({ required: false })
              },
            ])
            extraFields.push(...[
              {
                title: '相关附件',
                fieldName: FIELD.V_ATTACHMENTS.toLowerCase(),
                fieldType: FIELD.V_ATTACHMENTS,
              },
            ])
            extraFields.push(...[
              {
                title: '标签',
                fieldName: FIELD.V_TAGS.toLowerCase(),
                fieldType: FIELD.V_TAGS,
              },
            ])
          }
        }else if([OTHER.POP_FLOW, OTHER.POP_PROCESS].indexOf(modelType) !== -1) {
          extraFields.unshift({
            title: '操作备注',
            fieldName: '_opsRemark',
            fieldType: FIELD.STRING,
          })
        }
        return extraFields
      },
      _itemChecked(item){
        let that = this
        let fieldQuery = MetaPageComponentField.query()
          .where('metaPageComponentId', that.targetModel?.id)
          .where('field', item.fieldName)
        if(that.targetNodeIdent){
          fieldQuery.where('pIdent', that.targetNodeIdent)
        }
        let metaFields = fieldQuery.orderBy('seqOrder', 'asc').orderBy('id', 'asc').get()
        return metaFields.length>0
      },
      _cloneItemFunc(){
        let that = this
        let modelType = that.targetModel?.type
        if([OTHER.PART_QUERY].indexOf(modelType)!==-1){
          return vQueryUtil.queryCloneItem
        }else if([OTHER.PART_TABLE, OTHER.PART_TAB].indexOf(modelType)!==-1){
          return vTableUtil.tableCloneItem
        }else if([OTHER.PART_REPORT, OTHER.PART_DYNRPT].indexOf(modelType)!==-1){
          return vReportUtil.reportCloneItem
        }else if([
          OTHER.POP_ADD, OTHER.POP_EDIT, OTHER.POP_BATCH,
          OTHER.POP_FLOW, OTHER.POP_PROCESS,
          OTHER.FIELD_COMPONENT, OTHER.FIELD_POP_ADD, OTHER.FIELD_POP_EDIT
        ].indexOf(modelType) !== -1){
          return vPopUtil.popCloneItem
        }
      },
      _fieldSupportedFunc(){
        let that = this
        let modelType = that.targetModel?.type
        if([OTHER.PART_QUERY].indexOf(modelType)!==-1){
          return vQueryUtil.fieldSupported
        }else if([OTHER.PART_TABLE].indexOf(modelType)!==-1){
          return vTableUtil.fieldSupported
        }else if([OTHER.PART_REPORT, OTHER.PART_DYNRPT].indexOf(modelType)!==-1){
          return vReportUtil.fieldSupported
        }else if([
          OTHER.POP_ADD, OTHER.POP_EDIT, OTHER.POP_BATCH,
          OTHER.POP_FLOW, OTHER.POP_PROCESS,
          OTHER.FIELD_POP_ADD, OTHER.FIELD_POP_EDIT
        ].indexOf(modelType) !== -1){
          return vPopUtil.fieldSupported
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
    .extra-cols-container{
      display: flex;
      flex-direction: column;
      .extra-col {
        align-items: center;
        position: relative;
        width: 100%;
        line-height: 24px;
        border-radius: 5px;
        // border: 1px solid #e5e5e5;
        padding: 2px 0;
        &:hover{
          background-color: #f9f9fa;
        }
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