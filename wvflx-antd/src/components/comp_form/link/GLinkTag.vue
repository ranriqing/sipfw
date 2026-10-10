<template>
  <div>
    <a-select
      style="width: 100%;"
      optionFilterProp="children"
      mode="tags"
      :maxTagCount="3"
      :maxTagTextLength="5"
      :token-separators="[',']"
      :disabled="disabled"
      :allowClear="cptAllowClear"
      :placeholder="placeholder"
      :value="cptValue"
      @focus="onFocus"
      @change="onChange($event)">
      <a-select-option v-for="item in dataSource" :key="vCaption?item._caption:item.id">{{ item._caption }}</a-select-option>
    </a-select>
  </div>
</template>

<!--
# 组件说明
  下拉选项Tag组件，下拉选择数据并展示；
# 配置组件
  value: [String, Number]   // 当前选中条目
  sContext: Object          // 上下文信息
  disabled: Boolean         // 禁用状态，默认false
  vCaption: Boolean         // Key为值，默认false
  placeholder: String       // 占位字符串
  extend.allowClear: Boolean       // 允许为空，默认true
  indexUri: String          // 列表Uri
  conditionParams: Object   // 限制条件
  queryOn: Boolean          // 开启查询
  queryField: String        // 查询字段
# 操作方法
  $emit('change', value)
-->

<script>
  import { postAction } from '@/api/api'

  export default {
    name: 'GTag',
    components: {
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
        type: Array,
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
        default: '请输入'
      },
      /** 扩展Props */
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
      queryOn: {          // 开启查询
        type: Boolean,
        default: true
      },
      queryField: {       // 查询字段
        type: String,
        default: 'id'
      },
    },
    data() {
      return {
        dataSource: [],
      }
    },
    computed: {
      cptValue: function() {
        let that = this
        let values = Array.isArray(that.value) ? that.value : []
        return values.map(item => typeof item === 'string' ? item : (item && item.name))
      },
      cptAllowClear: function(){
        let that = this
        return that._.defaultTo(that._.get(that.item,'extend.allowClear'), true)
      },
      cptQueryFields: function(){
        return this.queryField.split('|')
      },
    },
    methods: {
      onChange (value) {
        this.$emit('change', value)
      },
      /** 扩展方法 */
      // [Focus] 加载列表数据
      onFocus(){
        if(this.dataSource.length === 0){
          this.doSearch(null)
        }
      },
      // [Search] 按条件加载数据
      doSearch(keyword) {
        let that = this
        // 无需查询
        let dftPageSize = 30
        if(!that.queryOn){
          keyword = null
          dftPageSize = 100
          if(that.dataSource.length !== 0){
            return
          }
        }
        // 加载数据
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
          let andValue = value.startsWith('$')?that._.get(that.sContext, value.substring(1)):value
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
          that.cptQueryFields.forEach(orKey => {
            orCondition.rules.push({
              field: orKey,
              value: keyword,
              operator: 'contains',
            })
          })
          queryCondition.rules.push(orCondition)
        }
        // 加载数据
        postAction(that.indexUri, queryCondition).then(resp => {
          if(resp.gmodel){
            // Tag查询时返回的是tag列表，不是实体列表
            that.dataSource = resp.gmodel.map( tagStr => {
              return {
                id: tagStr,
                _caption: tagStr
              }
            })
          }
        }).catch(err => {
          err || true
          that.dataSource = []
        })
      },
    },
  }
</script>
