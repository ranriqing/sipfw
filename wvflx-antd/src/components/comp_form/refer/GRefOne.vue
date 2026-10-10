<template>
  <div>
    <div v-if="readonly">
      {{ _relCaption(dataSource.find(record => record.id === realValue)) }}
    </div>
    <a-auto-complete
      v-else
      style="width: 100%;"
      :disabled="disabled"
      :placeholder="placeholder"
      :allowClear="cptAllowClear"
      v-model="realValue"
      @focus="onFocus"
      @select="onChange"
      @search="onSearch">
      <template slot="dataSource">
        <a-select-option v-for="option in dataSource" :key="cptCaptionAsValue?_relCaption(option):option.id">{{ _relCaption(option) }}</a-select-option>
      </template>
      <a-input>
        <a-icon slot="suffix" type="search" />
      </a-input>
    </a-auto-complete>
    <div v-if="false">
      <div v-for="relCol in cptSubRelCols" :key="relCol.field">
        <span class="text-tips">{{ relCol.title }}:</span>
        <span style="margin-left: 6px;">{{ _recordRelColValue(dataSource.find(record => record.id === realValue), relCol) }}</span>
      </div>
    </div>
  </div>
</template>

<!--
# 组件说明
  NOTE: Select       是在限定的可选项中进行选择，关键词是选择。
        AutoComplete 是输入框而不是选择器，关键词是辅助输入。
  自动完成组件，监听value内容，获取单条数据并展示；
  Focus时，根据选项是否存在，自动加载加载数据列表；
  Search时，如果无需查询，直接接着数据列表；
# 组件图标
  <a-icon type="search" />
# 配置组件
  value: [String, Number]   // 选中条目
  disabled: Boolean         // 是否禁用，默认false
  placeholder: String       // 占位字符串
# 操作方法
  $emit('change', value)
-->

<script>
import AAutoComplete from 'ant-design-vue/lib/auto-complete';

import MetaDomainField from '@/store/models/MetaDomainField'
import MetaPageComponent from '@/store/models/MetaPageComponent'

import {FIELD, OTHER} from '@/constants'
import {converters, tools} from '@/utils/util'
import { postAction, getAction } from '@/api/api'
import _ from 'lodash'

export default {
  name: 'GRefOne',
  components: {
    AAutoComplete,
  },
  /* eslint-disable */
  props: {
    item: {
      type: Object,
      required: true
    },
    value: {
      type: [String, Number],
      required: false
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
      default: '请选择'
    },
    /** 扩展Props */
    conditionParams: {  // 限制条件
      type: Object,
      default: function(){
        return {}
      }
    },
    prefillData: {  // 预填的关联实体（批量加载回填 {field}_entity）
      type: Object,
      required: false,
    },
  },
  data() {
    return {
      realValue: undefined,
      dataSource: [],
      debouncedSearch: undefined,
      prevConditionParams: undefined,
    }
  },
  watch: {
    // [Watch] 回显单条记录
    value: {
      immediate: true,
      handler(newValue, oldValue) {
        let that = this
        if(that.cptValueOn){
          that.realValue = that._.toString(newValue)===''?undefined:that._.toString(newValue)
        }else{
          that.realValue = undefined
        }
        if (that.prefillData) {
          that.dataSource = [Object.assign({}, that.prefillData, { id: that._.toString(that.prefillData.id) })]
          return
        }
        that.onSearch(null, that.realValue)
      }
    },
    conditionParams: function(newConditionParams){
      let that = this
      // 回显数据时，和newValue竞速问题
      if(that._.isNil(that.prevConditionParams)){
        that.prevConditionParams = newConditionParams
      }else if(!that._.isEqual(newConditionParams, that.prevConditionParams)){
        that.prevConditionParams = newConditionParams
        that.dataSource = []
        this.onChange(undefined)
        this.onSearch(null, null)
      }
    },
  },
  computed: {
    // 选中是否赋值
    cptValueOn: function(){
      let that = this
      return that._.defaultTo(that._.get(that.item,'extend.valueOn'), true)
    },
    // 查询是否开启
    cptQueryOn: function(){
      let that = this
      return that._.defaultTo(that._.get(that.item,'extend.queryOn'), true)
    },
    // 显示内容作为值
    cptCaptionAsValue: function(){
      let that = this
      return that._.defaultTo(that._.get(that.item,'extend.captionAsValue'), false)
    },
    // 字段相关扩展组件
    cptRelComponent: function(){
      let that = this
      let fieldIdent = `${that.item.id}_${that.item.field}`
      return MetaPageComponent.query().where('ident', fieldIdent)
        .where('type', OTHER.FIELD_COMPONENT).orderBy('id', 'desc').first()
    },
    // 字段相关实体字段
    cptSubRelCols: function(){
      let that = this
      let subRelCols = []
      // extend.relCols 为编辑组件时 GRefEditor 写入
      let relCols = that._.defaultTo(that._.get(that.item,'extend.relCols'), [])

      relCols.forEach((relCol, index) => {
        let dfItem = MetaDomainField.find(relCol.metaDomainFieldId)
        if(dfItem){
          let extendOptions = that._.get(converters.parseObj(dfItem?.extendJson), 'options', {})
          let transformOptions = converters.reverseObj(extendOptions)
          subRelCols.push({
            title: dfItem.title,
            field: dfItem.fieldName,
            type: dfItem.fieldType,
            metaDomainFieldId: dfItem.id,
            transformOptions: transformOptions
          })
        }
      })
      return subRelCols
    },
    // 是否允许置空字段
    cptAllowClear: function(){
      let that = this
      return that._.defaultTo(that._.get(that.item,'extend.allowClear'), true)
    },
  },
  methods: {
    onChange (value) {
      let that = this
      if(!that.cptValueOn){
        that.realValue = undefined
      }else{
        that.realValue = value
      }
      let valueObj = that.dataSource.find(item => item.id === value) || {}
      that.$emit('change', value, valueObj)
    },
    /** 扩展方法 */
    // [Focus] 加载列表数据
    onFocus(){
      if(this.dataSource.length === 0){
        this.onSearch(null, null)
      }
    },
    // [Search] 按条件加载数据
    onSearch(keyword, entityId){
      let that = this
      if(!entityId){
        that.$emit('change', undefined, {})
      }
      if(!that.debouncedSearch){
        that.debouncedSearch = _.debounce((key, eId) => {
          that.doSearch(key, eId);
        }, 300);
      }
      that.debouncedSearch(keyword, entityId)
    },
    // [Search] 按条件加载数据
    doSearch(keyword, entityId) {
      let that = this
      // 参数校验
      let indexFullUri = that._domainUri('index')
      if (indexFullUri && indexFullUri.indexOf('/demo/') === -1) {
        // continue
      } else {
        that.dataSource = []
        return
      }
      // 无需查询
      let dftPageSize = 100
      if (!that.cptQueryOn) {
        keyword = null
        entityId = null
        dftPageSize = 100
        if (that.dataSource.length !== 0) {
          return
        }
      }
      // 加载数据
      if (entityId) {
        /* 限制查询 */
        let recordIndex = that.dataSource.findIndex(option => option.id == entityId)
        if (recordIndex === -1) {
          let showFullUri = that._domainUri('show')
          if (showFullUri.indexOf('?') != -1) {
            showFullUri = `${showFullUri.substring(0, showFullUri.indexOf('?'))}/${entityId}${showFullUri.substring(showFullUri.indexOf('?'))}`
          } else {
            showFullUri = `${showFullUri}/${entityId}`
          }
          getAction(showFullUri).then(resp => {
            let record = resp.gmodel
            if (record) {
              that.dataSource = [Object.assign(record, {
                id: that._.toString(record.id),
              })]
            }
          })
        }
      } else {
        /* 多条查询 */
        let queryCondition = {
          pageNum: 1,
          pageSize: dftPageSize,
          orderBy: null,
          condition: 'and',
          rules: []
        }
        // 条件参数
        that._.forIn(that.conditionParams || {}, (value, andKey) => {
          let andValue = that._.toString(value)
          if (andValue) {
            queryCondition.rules.push({
              field: andKey,
              value: andValue,
              operator: 'equal',
            })
          }
        })
        // 查询字段(字符串模糊查询)
        if (keyword) {
          let orCondition = {
            condition: 'or',
            rules: []
          };
          that.cptSubRelCols.forEach(relCol => {
            if (relCol.type === FIELD.STRING) {
              orCondition.rules.push({
                field: relCol.field,
                value: keyword,
                operator: 'contains',
              })
            }
          })
          queryCondition.rules.push(orCondition)
        }
        // 加载数据
        postAction(indexFullUri, queryCondition).then(resp => {
          if (resp.gmodel && resp.gmodel.entityList) {
            that.dataSource = resp.gmodel.entityList.map((record) => {
              return Object.assign(record, {
                id: that._.toString(record.id),
              })
            })
          }
        }).catch(err => {
          err || true
          that.dataSource = []
        })
      }
    },
    /** 工具方法 */
    _domainUri(suffix){
      let that = this
      let relDomainName = that._.defaultTo(_.get(that.item,`extend.${OTHER.REL_DOMAIN_NAME}`), 'demo')
      return `/vm/${tools.toLowerCamel(relDomainName)}/${suffix}`
    },
    _relCaption(record){
      let that = this
      let captionArray = []
      that.cptSubRelCols.forEach(relCol => {
        if (relCol.type === FIELD.STRING) {
          captionArray.push(that._recordRelColValue(record, relCol))
        } else if (relCol.type === FIELD.SEQUENCE) {
          captionArray.push(that._recordRelColValue(record, relCol))
        } else if (relCol.type === FIELD.SELECT) {
          captionArray.push(that._recordRelColValue(record, relCol))
        }
      })
      return captionArray.length>0?captionArray.filter(str => !!str).join(' ~ '):that._.toString(record?.id)
    },
    _recordRelColValue(record, relCol) {
      let that = this
      if(!record){ return ''; }
      let itemValueStr = that._.toString(that._.get(record, relCol.field))
      return that._.get(relCol.transformOptions, itemValueStr, itemValueStr)
    }
  },
}
</script>
