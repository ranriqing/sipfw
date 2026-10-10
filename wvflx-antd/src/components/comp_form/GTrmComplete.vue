<template>
  <div>
    <a-auto-complete
      style="width: 100%;"
      :disabled="disabled"
      :allowClear="allowClear"
      :placeholder="placeholder"
      v-model="realValue"
      @focus="onFocus"
      @select="onChange"
      @search="doSearch">
      <!-- 判断是否使用了 customDataSource 插槽 -->
      <slot name="customDataSource"></slot>
      <template slot="dataSource">
        <a-select-option v-for="item in dataSource" :key="vCaption?item._caption:item.id">
          <div class="flex flex-row">
            <icon-font style="font-size: 38px; margin-right: 10px;" class="text-tips" type="icon-tub"/>
            <div class="flex flex-column">
              <div style="font-weight: bold;">{{ item._caption }}</div>
              <div class="text-tips" style="font-size: 14px;">{{ item.description | Ellipsis(12) }}</div>
            </div>
          </div>
        </a-select-option>
      </template>
      <a-input>
        <a-icon slot="suffix" type="search" />
      </a-input>
    </a-auto-complete>
    <div v-for="relCol in cptSubRelCols" :key="relCol.field">
      <span class="text-tips">{{ relCol.title }}:</span>
      <span style="margin-left: 6px;">{{ _recordRelColValue(relCol) }}</span>
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
  可以设置conditionParams的查询限制条件，如： {id: 1}，当前只支持equal类型的限制条件;
  可以设置queryField查询字段，多个字段用|分隔，当前只支持字符串类型的查询字段;
# 组件图标
  <a-icon type="search" />
# 配置组件
  value: [String, Number]   // 选中条目
  sContext: Object          // 上下文信息
  disabled: Boolean         // 是否禁用，默认false
  vCaption: Boolean         // Key为值，默认false
  allowClear: Boolean       // 允许为空，默认true
  placeholder: String       // 占位字符串
  showUri: String           // 展示Uri
  indexUri: String          // 列表Uri
  conditionParams: Object   // 限制条件
  queryOn: Boolean          // 开启查询
  queryField: String        // 查询字段
  relCols: Array            // 展示关联字段
# 操作方法
  $emit('change', value)
-->

<script>
  import AAutoComplete from 'ant-design-vue/lib/auto-complete';

  import MetaDomainField from '@/store/models/MetaDomainField'

  import { FIELD } from '@/constants'
  import { converters } from '@/utils/util'
  import { postAction, getAction } from '@/api/api'

  export default {
    name: 'GAutoComplete',
    components: {
      AAutoComplete,
    },
    /* eslint-disable */
    props: {
      sContext: {
        type: Object,
        default: function(){
          return {}
        }
      },
      value: {
        type: [String, Number],
        required: false
      },
      disabled: {
        type: Boolean,
        default: false
      },
      vCaption: {
        type: Boolean,
        default: false
      },
      allowClear: {
        type: Boolean,
        default: true
      },
      placeholder: {
        type: String,
        default: '请选择'
      },
      /** 扩展Props */
      showUri: {          // 展示Uri
        type: String,
        required: true
      },
      indexUri: {         // 列表Uri
        type: String,
        required: true
      },
      conditionParams: {  // 限制条件
        type: Object,
        default: function(){
          return {}
        }
      },
      valueOn: {          // 显示Value
        type: Boolean,
        default: true
      },
      queryOn: {          // 开启查询
        type: Boolean,
        default: true
      },
      queryField: {       // 查询字段
        type: String,
        default: 'id'
      },
      relCols: {
        type: Array,
        default: function(){
          return []
        }
      }
    },
    data() {
      return {
        realValue: undefined,
        dataSource: [],
      }
    },
    watch: {
      // [Watch] 回显单条记录
      value: function(newValue) {
        let that = this
        if(that.valueOn){
          that.realValue = that._.toString(newValue)===''?undefined:that._.toString(newValue)
        }else{
          that.realValue = undefined
        }
        that.doSearch(null, that.realValue)
      },
      relCols: function(newRefCols){
        newRefCols || true
        let that = this
        that.dataSource = [] // 清理缓存
      },
    },
    computed: {
      cptQueryFields: function(){
        return this.queryField.split('|')
      },
      cptSubRelCols: function(){
        let that = this
        let subRelCols = []
        that.relCols.forEach((relCol, index) => {
          if(index!==0){
            let dfItem = MetaDomainField.find(relCol.metaDomainFieldId)
            if(dfItem){
              let extendOptions = that._.get(converters.parseObj(dfItem.extendJson), 'options', {})
              let transformOptions = converters.reverseObj(extendOptions)
              subRelCols.push({
                title: dfItem.title,
                field: dfItem.fieldName,
                type: dfItem.fieldType,
                metaDomainFieldId: dfItem.id,
                transformOptions: transformOptions
              })
            }
          }
        })
        return subRelCols
      }
    },
    methods: {
      onChange (value) {
        let that = this
        if(that.valueOn){
          that.realValue = value
        }else{
          that.realValue = undefined
        }
        that.$emit('change', value)
      },
      /** 扩展方法 */
      // [Focus] 加载列表数据
      onFocus(){
        if(this.dataSource.length === 0){
          this.doSearch(null, null)
        }
      },
      // [Search] 按条件加载数据
      doSearch(keyword, entityId) {
        let that = this
        // 参数校验
        if(that.indexUri && that.indexUri.indexOf('/demo/') === -1){
          // continue
        }else{
          that.dataSource = []
          return
        }
        // 无需查询
        let dftPageSize = 30
        if(!that.queryOn){
          keyword = null
          entityId = null
          dftPageSize = 100
          if(that.dataSource.length !== 0){
            return
          }
        }
        // 加载数据
        let displayField = that.cptQueryFields[0] || 'id'
        let relCol = that._.get(that.relCols, '[0]')
        let transformOptions = []
        if(relCol){
          displayField = relCol.field
          let dfItem = MetaDomainField.find(relCol.metaDomainFieldId)
          let extendOptions = that._.get(converters.parseObj(dfItem?.extendJson), 'options', {})
          transformOptions = converters.reverseObj(extendOptions)
        }
        if(entityId){
          /* 单条查询 */
          getAction(`${that.showUri}/${entityId}`).then(resp => {
            let item = resp.gmodel
            if(item){
              let itemValueStr = that._.toString(item[displayField])
              that.dataSource = [Object.assign(item, {
                id: that._.toString(item.id),
                _caption: that._.get(transformOptions, itemValueStr, itemValueStr)
              })]
            }
          })
        }else{
          /* 多条查询 */
          let queryCondition = {
            pageNum: 1,
            pageSize: dftPageSize,
            orderBy: null,
            condition: 'and',
            rules: []
          }
          // 条件参数(支持动态参数)
          // eg: conditionParams={id: $field || 100}，sContext={field: 1}，支持equal限制条件
          that._.forIn(that.conditionParams || {}, (value, andKey)=>{
            value = that._.toString(value)
            let andValue = (value && value.startsWith('$'))?that._.get(that.sContext, value.substring(1)):value
            if(andValue){
              queryCondition.rules.push({
                field: andKey,
                value: andValue,
                operator: 'equal',
              })
            }
          })
          // 查询字段(字符串模糊查询)
          if(keyword){
            let orCondition = {
              condition: 'or',
              rules: []
            };
            if(relCol){
              if(relCol.type === FIELD.STRING){
                orCondition.rules.push({
                  field: relCol.field,
                  value: keyword,
                  operator: 'contains',
                })
              }
            }else{
              that.cptQueryFields.forEach(orKey => {
                orCondition.rules.push({
                  field: orKey,
                  value: keyword,
                  operator: 'contains',
                })
              })
            }
            queryCondition.rules.push(orCondition)
          }
          // 加载数据
          postAction(that.indexUri, queryCondition).then(resp => {
            if(resp.gmodel && resp.gmodel.entityList){
              that.dataSource = resp.gmodel.entityList.map( item => {
                let itemValueStr = that._.toString(item[displayField])
                return Object.assign(item, {
                  id: that._.toString(item.id),
                  _caption: that._.get(transformOptions, itemValueStr, itemValueStr)
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
      _recordRelColValue(relCol){
        let that = this
        let record = that.dataSource.find(item => item.id === that.realValue )
        let itemValueStr = that._.toString(that._.get(record, relCol.field))
        return that._.get(relCol.transformOptions, itemValueStr, itemValueStr)
      }
    },
  }
</script>
