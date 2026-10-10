<template>
  <div
    :style="{width: 'unset', 'margin-top': '6px'}"
    :data-cptRelComponent="cptRelComponent"
    :data-cptFieldItems="cptFieldItems">
    <div class="flex flex-row main-between">
      <div>
        <a-button-group v-if="!(readonly || disabled)">
          <!-- 原有选择模式按钮（refSelectOn 开启时，保持兼容） -->
          <a-button
            v-if="_.get(item, `extend.${OTHER.REF_SELECT_ON}`)"
            type="default"
            icon="plus"
            size="small"
            @click="onRecordAdd"
            v-show="!_.get(item, `extend.${OTHER.REF_QUICK_SELECT_ON}`)">
            选择[{{ _.get(cptRefSelectDomain, 'title', '暂无') }}]
          </a-button>
          <!-- 配置化操作按钮（sExtend 控制） -->
          <template v-if="!_.get(item, `extend.${OTHER.REF_SELECT_ON}`)">
            <a-button
              v-if="cptRecordIdEmpty || sExtend.addEnabled !== false"
              type="default"
              icon="plus"
              size="small"
              @click="onRecordAdd">{{ sExtend.addCaption || '添加' }}</a-button>
          </template>
        </a-button-group>
      </div>
      <div v-if="cptLabOn" class="config-wrapper">
        <!-- 操作项按钮 (Lab state only) -->
        <a-button size="small" @click="handleOpsConfig">
          <a-icon type="control"/>操作项
        </a-button>
        <a-divider type="vertical" />
        <!-- 列定义 (已有) -->
        <a-button size="small" @click="vPopUtil.handleColsConfig(_self)">
          <a-icon type="setting"/>列定义
        </a-button>
      </div>
    </div>
    <div
      v-for="(relRecord, index0) in tableDataSource"
      :key="relRecord.formKey || index0">
      <a-form :ref="relRecord.formKey" :form="formInstances[relRecord.formKey]" :class="{'form-inline': true, 'sub-form':true, 'form-readonly': readonly}" layout="horizontal">
        <a-row :gutter="12">
          <a-col
            v-for="(fieldItem, index1) in datFieldItems"
            :key="index1"
            :md="_.get(fieldItem, 'extend.cols') || 24"
            :sm="24"
            :xs="24">
            <g-form-item
              style="width: 100%;"
              v-if="fieldItem.isActived?fieldItem.isActived(relRecord, {}):true"
              :labelCol="{
                xs: { span: 24 },
                sm: { span: 6 }
              }"
              :wrapperCol="{
                xs: { span: 24 },
                sm: { span: 18 }
              }"
              :item="rowEditableItem(fieldItem, relRecord)"
              :record="relRecord"
              @collapsePanel="vPopUtil.collapsePanel(_self, fieldItem, $event)">
            </g-form-item>
          </a-col>
          <a-col
            v-if="!(readonly || disabled) && (cptRecordIdEmpty || sExtend.deleteEnabled !== false)"
            :md="2"
            :sm="24"
            :xs="24">
            <a-icon
              class="delete-icon"
              type="minus-circle-o"
              @click="onRecordRemove(relRecord, index0)"
            />
          </a-col>
        </a-row>
      </a-form>
      <a-divider style="margin: 5px 0;" type="horizontal"/>
    </div>
    <div
      v-if="!(readonly || disabled)
        && _.get(item, `extend.${OTHER.REF_SELECT_ON}`)
        && _.get(item, `extend.${OTHER.REF_QUICK_SELECT_ON}`)">
      <!-- 快捷选择[{{ _.get(cptRefSelectDomain, 'title', '暂无') }}]-->
      <GRefOne
        v-if="cptRefSelectDomain"
        style="width: 300px;"
        :item="{
          field:'none_ref_one',
          extend: {
            valueOn: false,
            relDomainName: cptRefSelectDomain.domainName,
            relCols: _.defaultTo(_.get(item,'extend.relCols'), []),
          }
        }"
        :placeholder="`选择${_.get(cptRefSelectDomain, 'title', '暂无')}...`"
        @change="onRefQuickSelect">
      </GRefOne>
    </div>
    <GColsPop
      ref="refCols"
      :configObj="Object.assign(
        {},
        colsPopConfig,
        {
          title: '定义字段组件',
          metaPopEdit: true,
          metaPopTable: table,
        }
      )">
    </GColsPop>
    <GFormPop
      ref="refOps"
      :configObj="opsPopConfig">
      <template slot="editConditionSlot">
        <div class="condition-wrapper" style="padding: 8px 0;">
          <div
            v-for="(cond, index) in editConditions"
            :key="index"
            class="condition-item"
            style="display: flex; align-items: center; margin-bottom: 8px; gap: 8px;">
            <a-select
              :value="cond.field"
              placeholder="选择字段"
              style="width: 160px;"
              @change="(v) => { editConditions[index].field = v; editConditions[index].value = undefined; }">
              <a-select-option
                v-for="f in cptConditionFields"
                :key="f.fieldName"
                :value="f.fieldName">
                {{ f.title }}
              </a-select-option>
            </a-select>
            <a-select
              :value="cond.operator"
              style="width: 100px;"
              @change="(v) => { editConditions[index].operator = v; }">
              <a-select-option value="equal">等于</a-select-option>
              <a-select-option value="notEqual">不等于</a-select-option>
            </a-select>
            <a-select
              :value="cond.value"
              placeholder="选择值"
              style="width: 160px;"
              @change="(v) => { editConditions[index].value = v; }">
              <a-select-option
                v-for="opt in getConditionFieldOptions(cond.field)"
                :key="opt.value"
                :value="opt.value">
                {{ opt.label }}
              </a-select-option>
            </a-select>
            <a-icon
              type="close"
              style="cursor: pointer; color: #999;"
              @click="editConditions.splice(index, 1)" />
          </div>
          <a-button
            icon="plus"
            type="dashed"
            :block="true"
            @click="editConditions.push({field: undefined, operator: 'equal', value: undefined})">
            添加条件
          </a-button>
        </div>
      </template>
    </GFormPop>
  </div>
</template>

<!--
# 组件说明
  一对多关联组件，动态加载数据
# 配置组件
  value: String              // 展示的内容
# 操作方法
  $emit('change', value)
-->

<script>
  import MetaPageComponent from '@/store/models/MetaPageComponent'

  import {QUERY, OTHER, FIELD, COMPO, DATE} from '@/constants'
  import {tools, stores, domains, converters} from '@/utils/util'
  import {vTableUtil, vPopUtil} from '@/utilsv/vUtil'
  import { postAction } from '@/api/api'

  import GRefOne from "./GRefOne.vue";
  import GColsPop from "@/components/page_pop/GColsPop.vue";
  import GFormPop from "@/components/page_pop/GFormPop";
  import MetaDomainField from "@/store/models/MetaDomainField";
  import labRefManyOpsPopConfig from "./configs/labRefManyOpsPopConfig";
  import labPopColsPopConfig from "@/components/tmpl_pop/configs/labPopColsPopConfig";
  import MetaPageComponentField from "@/store/models/MetaPageComponentField";
  import MetaDomain from "@/store/models/MetaDomain";

  export default {
    name: 'GRefMany',
    components: {
      GColsPop,
      GFormPop,
      GRefOne
    },
    /* eslint-disable */
    props: {
      item: {
        type: Object,
        required: true
      },
      value: {
        type: Array,
        required: false
      },
      table: {
        type: Boolean,
        default: false
      },
      readonly: {
        type: Boolean,
        default: false
      },
      disabled: {
        type: Boolean,
        default: false
      },
      placeholder: {
        type: String,
        default: ''
      },
      /** 扩展Props */
      record: {
        type: Object,
        required: false
      },
      prefillData: {  // 预填的关联子记录列表（批量加载回填 {field}_entities）
        type: Array,
        required: false,
      },
    },
    watch: {
      record: {
        immediate: true,
        handler(newValue, oldValue) {
          let that = this
          oldValue || true
          if (that.prefillData) {
            that.tableDataSource = that.prefillData.map(r => Object.assign({}, r))
            that.tablePagination.total = that.prefillData.length
            that._refreshFormInstances()
            that._populateFormInstances()
            return
          }
          if(that._.get(newValue, 'id')){
            that._loadData(1)
          }else{
            that.tableDataSource = []
            that.tablePagination.total = 0
            that._refreshFormInstances()
          }
        }
      },
    },
    data() {
      return {
        /** 配置&状态 */
        OTHER: OTHER,
        stores: stores,
        vPopUtil: vPopUtil,
        colsPopConfig: labPopColsPopConfig,
        opsPopConfig: labRefManyOpsPopConfig,
        editConditions: [],
        loading:false,
        sModel: undefined,
        datFieldItems: [],
        formInstances: {},
        /** 查询&数据源 */
        debouncedLoad: undefined,
        tablePagination: {
          current: 1,
          pageSize: 10,
          pageSizeOptions: ['30', '50', '100'],
          showTotal: (total, range) => {
            return range[0] + "-" + range[1] + " 共" + total + "条"
          },
          showQuickJumper: true,
          showSizeChanger: true,
          total: 0
        },
        tableDataSource: [],
        defaultDataSource: [],
      }
    },
    computed: {
      sExtend: function() {
        return converters.parseObj(this.sModel?.extend)
      },
      // Lab 内容是否展示：全局 lab 模式 + 字段级 showLab 开关（默认展示）
      cptLabOn: function() {
        return stores.isLabState() && this._.get(this.item, 'extend.showLab') !== false
      },
      // 父记录 id 为空（新建场景）时，强制允许添加/编辑/删除，优先级高于 sExtend 配置
      cptRecordIdEmpty: function() {
        let that = this
        return !that._.get(that.record, 'id')
      },
      cptConditionFields: function () {
        const relDomainId = this._.get(this.item, `dfExtend.${OTHER.REL_DOMAIN_ID}`)
        if (!relDomainId) return []
        const allFields = MetaDomainField.query()
          .where('metaDomainId', relDomainId)
          .get()
        return allFields.filter(item =>
          [FIELD.SELECT, FIELD.SC_EL].indexOf(item.fieldType) !== -1
        )
      },
      cptRelDomainName: function(){
        let that = this
        return that._.defaultTo(that._.get(that.item,`extend.${OTHER.REL_DOMAIN_NAME}`), true)
      },
      cptRelDomainFieldName: function(){
        let that = this
        return that._.defaultTo(that._.get(that.item,`extend.${OTHER.REL_DOMAIN_FIELD_NAME}`), true)
      },
      cptRefSelectDomain: function(){
        let that = this
        let domainId = that._.get(that.item, `extend.${OTHER.REF_SELECT_DOMAIN_ID}`)
        return MetaDomain.find(domainId)
      },
      // 字段相关扩展组件
      cptRelComponent: function(){
        let that = this
        let fieldIdent = `${that.item.id}_${that.item.field}`
        let relComponent = MetaPageComponent.query().where('ident', fieldIdent)
          .where('type', OTHER.FIELD_COMPONENT).orderBy('id', 'desc').first()
        that.sModel = relComponent
        return relComponent
      },
      cptFieldItems: function(){
        let that = this
        let metaFields = MetaPageComponentField.query().where('metaPageComponentId', that.sModel?.id)
          .orderBy('seqOrder', 'asc').orderBy('id', 'asc').with('relMetaDomainField').get()
        let cloneItemFunc = vPopUtil.popCloneItem
        let panelItem = null
        // 特殊处理，读取表格配置，是否允许编辑
        let editEnabled = that.cptRecordIdEmpty || that._.defaultTo(
          that._.get(that.sExtend, OTHER.EDIT_ENABLED),
          that._.defaultTo(that._.get(that.context, OTHER.EDIT_ENABLED), true)
        )
        that.datFieldItems = metaFields.map(item => {
          let clonedItem = cloneItemFunc(that, item)
          if(item['component']===COMPO.G_PANEL){
            clonedItem['pnlIdent'] = item['id']
            panelItem = item
          }else if(panelItem){
            clonedItem['panelIdent'] = panelItem['id']
          }
          let required = that._.get(clonedItem, 'extend.required')
          clonedItem['options'] = that.readonly?{}:{
            rules: [{ required: !!required, message: `${item['title']}为必填项` }],
          }
          that._.set(clonedItem, 'extend.labelAlign', 'left')
          if(!editEnabled || that.readonly){
            that._.set(clonedItem, 'extend.readonly', true)
          }
          if(that.disabled){
            that._.set(clonedItem, 'extend.disabled', true)
          }
          return clonedItem
        })
        return metaFields
      },
      cptLabelCol: function(){
        return {
          xs: { span: 24 },
          sm: { span: 5 }
        }
      },
      cptWrapperCol: function(){
        return {
          xs: { span: 24 },
          sm: { span: 15 }
        }
      },
    },
    methods: {
      handleOpsConfig() {
        const that = this
        const extendObj = converters.parseObj(that.sModel?.extend)
        const recordObj = {
          addEnabled: that._.defaultTo(extendObj[OTHER.ADD_ENABLED], true),
          addCaption: that._.trim(extendObj[OTHER.ADD_CAPTION]),
          editEnabled: that._.defaultTo(extendObj[OTHER.EDIT_ENABLED], true),
          editCaption: that._.trim(extendObj[OTHER.EDIT_CAPTION]),
          conditionEditOn: that._.defaultTo(extendObj['conditionEditOn'], false),
          deleteEnabled: that._.defaultTo(extendObj[OTHER.DELETE_ENABLED], true),
        }
        that.editConditions = extendObj['editConditions'] || []
        that.$refs.refOps?.showPop(recordObj,
          { targetModel: that.sModel, conditionEditOn: recordObj.conditionEditOn },
          (item) => that.handleOpsUpdate(item),
          () => {} // cancel
        )
      },

      handleOpsUpdate(item) {
        const that = this
        const extendObj = converters.parseObj(that.sModel?.extend)
        extendObj[OTHER.ADD_ENABLED] = item[OTHER.ADD_ENABLED]
        extendObj[OTHER.ADD_CAPTION] = that._.trim(item[OTHER.ADD_CAPTION])
        extendObj[OTHER.EDIT_ENABLED] = item[OTHER.EDIT_ENABLED]
        extendObj[OTHER.EDIT_CAPTION] = that._.trim(item[OTHER.EDIT_CAPTION])
        extendObj['conditionEditOn'] = item['conditionEditOn']
        extendObj['editConditions'] = that.editConditions || []
        extendObj[OTHER.DELETE_ENABLED] = item[OTHER.DELETE_ENABLED]
        return MetaPageComponent.$update({
          params: { id: that.sModel.id },
          data: { extend: converters.encodeObj(extendObj) },
        }).then(() => {
          that.$message.success('操作项配置已保存')
        })
      },

      getConditionFieldOptions(fieldName) {
        const that = this
        if (!fieldName) return []
        const relDomainId = that._.get(that.item, `dfExtend.${OTHER.REL_DOMAIN_ID}`)
        const df = MetaDomainField.query()
          .where('fieldName', fieldName)
          .where('metaDomainId', relDomainId)
          .first()
        if (!df) return []
        const extendJson = converters.parseObj(df.extendJson)
        const rawOptions = extendJson['options']
        if (!rawOptions) return []
        if (Array.isArray(rawOptions)) return rawOptions
        return converters.objToOptions(rawOptions)
      },

      isRowEditable(row) {
        const that = this
        if (that.cptRecordIdEmpty) return true
        const extendObj = converters.parseObj(that.sModel?.extend)
        const editEnabled = that._.defaultTo(extendObj[OTHER.EDIT_ENABLED], true)
        if (!editEnabled) return false
        const conditionEditOn = extendObj['conditionEditOn']
        if (!conditionEditOn) return true
        const editConditions = extendObj['editConditions'] || []
        if (editConditions.length === 0) return true
        return editConditions.every(cond => {
          const rowValue = row[cond.field + FIELD.SUFFIX_ID] || row[cond.field]
          if (cond.operator === 'equal') return String(rowValue) === String(cond.value)
          if (cond.operator === 'notEqual') return String(rowValue) !== String(cond.value)
          return false
        })
      },

      rowEditableItem(fieldItem, rowRecord) {
        const that = this
        const editable = that.isRowEditable(rowRecord)
        if (!editable) {
          const cloned = that._.cloneDeep(fieldItem)
          that._.set(cloned, 'extend.disabled', true)
          return cloned
        }
        return fieldItem
      },

      onRefQuickSelect(value, valueObj){
        let that = this
        if(value && valueObj){
          /** 找到子记录的所有ManyToOne字段，找到和选中Domain的相关的字段，设置选中值到此相关字段 */
          // 1、选择的DomainId(eg: 产品Domain)
          let selectDomainId = that._.get(that.item, `extend.${OTHER.REF_SELECT_DOMAIN_ID}`)
          // 2、对端实体的DomainId(eg: 采购详情Domain)
          let relDomainId = that._.get(that.item, `dfExtend.${OTHER.REL_DOMAIN_ID}`)
          // 3、对端实体的ManyToOne字段(eg: 采购详情Domain的全部ManyToOne字段，过滤关联商品字段)
          let relDomainManyToOneField = that._.get(domains.many2OneFields(relDomainId, selectDomainId), [0])
          // 4、添加新的条目（eg: 自动关联商品字段）
          let rstValue = {}
          if(relDomainManyToOneField){
            rstValue[relDomainManyToOneField.fieldName + FIELD.SUFFIX_ID] = value
          }
          that.tableDataSource.push(rstValue)
          that._refreshFormInstances()
          that._populateFormInstances()
          that.onChange()
        }
      },
      // 此处值变化父验证器调用
      onFormValuesChange(formProps, formValues){
        let that = this
        formValues || true
        that.onChange()
      },
      fieldValidator(rule, value, callback) {
        let that = this;
        let validPromises = that.tableDataSource.map(record => {
          let formKey = record.formKey;
          let formInstance = that.formInstances[formKey];
          return new Promise(resolve => {
            formInstance.validateFields((errors, values) => {
              resolve({
                formKey,
                valid: !errors,
                values
              });
            });
          });
        });
        // 使用 Promise.all 处理所有验证
        Promise.all(validPromises).then(allResults => {
          let allValid = allResults.every(res => res.valid);
          allValid ? callback() : callback('校验失败');
        }).catch(() => {
          callback('校验过程出错');
        });
      },
      _loadData(arg) {
        let that = this
        if (arg === 1) {
          that.tablePagination.current = 1;
        }
        // 参数校验
        let recordId = that._.get(that.record, 'id')
        if(!(that.cptRelDomainName && that.cptRelDomainFieldName && recordId)){
          return
        }
        // 查询条件
        let queryCondition = {
          paginate: false,
          pageNum: that.tablePagination.current,
          pageSize: that.tablePagination.pageSize,
          orderBy: 'id-asc',
          condition: 'and',
          rules: [{
            field: that.cptRelDomainFieldName + FIELD.SUFFIX_ID,
            value: recordId,
            operator: QUERY.EQUAL,
          }],
        }
        // 加载数据
        that.loading = true
        let indexFullUri = that._domainUri('index')
        postAction(indexFullUri, queryCondition).then(resp => {
          that.loading = false
          if(resp.gmodel && resp.gmodel.entityList){
            that.tableDataSource = resp.gmodel.entityList
            that.tablePagination.total = resp.gmodel.totalCount
            that._refreshFormInstances()
            that._populateFormInstances()
          }
        }).catch(err => {
          that.loading = false
          that.tableDataSource = that.defaultDataSource
          vTableUtil.tableReqErr(that, err, '加载失败')
        })
      },
      onRecordAdd(){
        let that = this
        that.tableDataSource.push({})
        that._refreshFormInstances()
        that.onChange()
      },
      onRecordRemove(relRecord, index){
        let that = this
        that.tableDataSource.splice(index, 1)
        that._refreshFormInstances()
        that.onChange()
      },
      onChange () {
        let that = this
        let recordArray = []
        // 等待值变化已经影响到form
        tools.sleep(200).then(()=>{
          that.tableDataSource.map(srcRecord => {
            let formKey = srcRecord.formKey;
            let formInstance = that.formInstances[formKey];
            let values = formInstance.getFieldsValue()
            let record = {id: srcRecord.id}
            that.datFieldItems.forEach(item => {
              if([COMPO.G_PANEL, COMPO.G_SLOT, COMPO.G_LOCK].indexOf(item.component)!==-1){ return }
              let fieldValue = values[item.field]
              fieldValue = that._.isNil(fieldValue)?undefined:fieldValue
              if(item.setValue) {
                item.setValue(record, fieldValue)
              }else if([COMPO.G_INPUT, COMPO.G_TEXTAREA].indexOf(item.component) !== -1) {
                record[item.field] = that._.isEmpty(fieldValue)?null:that._.trim(fieldValue)
              }else if(COMPO.G_DATE_PICKER === item.component){
                let dateFormat = that._.get(item, 'extend.format') || DATE.YYYYMMDD
                record[item.field] = fieldValue?fieldValue.format(dateFormat):null
              }else if(COMPO.G_TIME_PICKER === item.component){
                record[item.field] = fieldValue?fieldValue.format('HH:mm:ss'):null
              }else{
                record[item.field] = fieldValue
              }
            })
            recordArray.push(record)
          })
          that.$emit('change', recordArray)
        })
      },
      /** 工具方法 */
      _domainUri(suffix){
        let that = this
        let relDomainName = that._.defaultTo(that._.get(that.item,`extend.${OTHER.REL_DOMAIN_NAME}`), 'demo')
        return `/vm/${tools.toLowerCamel(relDomainName)}/${suffix}`
      },
      _refreshFormInstances(){
        let that = this
        // 创建不存在formInstance
        that.tableDataSource.forEach(record => {
          if(!record.formKey){
            record.formKey = record.id?`ref${record.id}`:tools.randomString(5);
          }
          if (!that.formInstances[record.formKey]) {
            that.formInstances[record.formKey] = that.$form.createForm(that, {
              name: record.formKey,
              onValuesChange: that.onFormValuesChange,
            });
          }
        });
        // 移除无用formInstance
        Object.keys(that.formInstances).forEach(formKey => {
          let instanceRecord = that.tableDataSource.find(record => record.formKey===formKey)
          if (!instanceRecord) {
            delete that.formInstances[formKey];
          }
        });
      },
      _populateFormInstances(){
        let that = this
        // 回显form数据
        that.tableDataSource.map(record => {
          let formKey = record.formKey;
          let formInstance = that.formInstances[formKey];
          let values = {}
          that.datFieldItems.forEach( item => {
            if([COMPO.G_PANEL, COMPO.G_SLOT, COMPO.G_LOCK].indexOf(item.component)!==-1){ return }
            let fieldValue = item.getValue?item.getValue(record, that.context):
              that._.isNil(record[item.field])?values[item.field]:record[item.field]
            fieldValue = that._.isNil(fieldValue)?undefined:fieldValue
            if(COMPO.G_DATE_PICKER === item.component){
              let dateFormat = that._.get(item, 'extend.format') || DATE.YYYYMMDD
              values[item.field] = fieldValue?that.$moment(fieldValue, dateFormat):null
            }else if(COMPO.G_TIME_PICKER === item.component){
              values[item.field] = fieldValue?that.$moment(fieldValue, 'HH:mm:ss'):null
            }else{
              values[item.field] = fieldValue
            }
          })
          that._.forIn(values, (value, key)=>{ formInstance.getFieldDecorator(key, {}) })
          formInstance.setFieldsValue(values)
        })
      },
    },
  }
</script>

<style lang="scss" scoped>
  .delete-icon{
    cursor: pointer;
    margin-top: 6px;
    font-size: 20px;
    color: #999;
    transition: all 0.3s;
  }
  .sub-form{
    border-radius: 6px;
    padding: 6px 8px;
    // border: 2px solid rgba(153, 153, 153, 0.4);
    box-shadow: 0 0 0 2px rgba(153, 153, 153, 0.2);
    // background-color: rgba(153, 153, 153, 0.2);
  }
</style>