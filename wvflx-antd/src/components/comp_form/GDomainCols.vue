<template>
  <div>
    <a-auto-complete
      style="width: 100%;"
      :disabled="disabled"
      :allowClear="allowClear"
      :placeholder="placeholder"
      optionLabelProp="label"
      v-model="realValue"
      @focus="onFocus"
      @select="onChange"
      @search="doSearch">
      <template slot="dataSource">
        <a-select-opt-group v-for="group in groupDataSource" :key="group.title">
          <a-select-option
            v-for="item in group.children"
            :key="item.id"
            :value="vCaption?item._caption:item.id"
            :label="item._caption">
            <a-icon :type="vDftUtil.fieldIcon(item, 'fieldType')"/> {{ item._caption }}
          </a-select-option>
        </a-select-opt-group>
      </template>
      <a-input>
        <a-icon slot="suffix" type="search" />
      </a-input>
    </a-auto-complete>
  </div>
</template>

<!--
# 组件说明
  NOTE: Select       是在限定的可选项中进行选择，关键词是选择。
        AutoComplete 是输入框而不是选择器，关键词是辅助输入。
  自动完成组件，只发出事件，不设置value；
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
  allowClear: Boolean       // 允许为空，默认true
  placeholder: String       // 占位字符串
  conditionParams: Object   // 限制条件
  queryOn: Boolean          // 开启查询
  queryField: String        // 查询字段
  echoOptionOn: Boolean     // 是否回显选中项
  metaDomains: Array        // 数据源为哪些MetaDomain
  fieldSupported: Function  // 字段是否有效回调函数
# 操作方法
  $emit('change', value)
-->

<script>
  import AAutoComplete from 'ant-design-vue/lib/auto-complete';

  import { FIELD } from '@/constants'
  import { vDftUtil } from '@/utilsv/vUtil'

  import MetaDomainField from '@/store/models/MetaDomainField'

  import GDomainCols from '@/components/comp_form/GDomainCols'

  export default {
    name: 'GDomainCols',
    components: {
      AAutoComplete,
      GDomainCols,
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
        default: '添加列...'
      },
      /** 扩展Props */
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
        default: 'fieldName|title'
      },
      isChart: {          // 是否图表(显示扩展列)
        type: Boolean,
        default: false
      },
      isMeasure: {        // 是否计算列
        type: Boolean,
        default: false
      },
      echoOptionOn: {     // 是否回显选中项
        type: Boolean,
        default: true
      },
      metaDomains: {      // 数据源为哪些MetaDomain
        type: Array,
        default: function(){
          return []
        }
      },
      fieldSupported:{    // 字段是否有效回调函数
        type: Function,
        default: undefined
      },
    },
    data() {
      return {
        vDftUtil: vDftUtil,
        realValue: undefined,
        groupDataSource: [],
      }
    },
    watch: {
      // [Watch] 回显单条记录
      value: function(newValue) {
        let that = this
        if(that.echoOptionOn){
          that.realValue = that._.toString(newValue)===''?undefined:that._.toString(newValue)
        }else{
          that.realValue = undefined
        }
        that.doSearch(null, that.realValue)
      }
    },
    computed: {
      cptQueryFields: function(){
        return this.queryField.split('|')
      },
    },
    methods: {
      onChange (value) {
        let that = this
        if(that.echoOptionOn){
          that.realValue = value
        }else{
          that.realValue = undefined
        }
        that.$emit('change', value)
      },
      /** 扩展方法 */
      // [Focus] 加载列表数据
      onFocus(){
        if(this.groupDataSource.length === 0){
          this.doSearch(null)
        }else{
          this.doSearch(null) // 清除查询关键字导致选项的过滤
        }
      },
      // [Search] 按条件加载数据
      doSearch(keyword) {
        let that = this
        // 无需查询
        if(!that.queryOn){
          keyword = null
          if(that.groupDataSource.length !== 0){
            return
          }
        }
        /* 查询条件 */
        let queryCondition = {
          pageNum: 1,
          pageSize: 30,
          orderBy: null,
          condition: 'and',
          rules: []
        }
        // 限制参数(支持动态参数)
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
        // 查询字段
        if(keyword){
          that.cptQueryFields.forEach(orKey => {
            queryCondition.rules.push({
              field: orKey,
              value: keyword,
              operator: 'contains',
            })
          })
        }
        // 加载数据
        let groupFields = that.metaDomains.filter(item => item).map( metaDomain => {
          let metaDomainFields = MetaDomainField.query()
            .where('metaDomainId', metaDomain?.id)
            .where('visible', true).where(item => {
              let isMatched = true
              if(queryCondition.rules.length>0){
                isMatched = false
                queryCondition.rules.forEach((rule)=>{
                  let field = rule.field
                  let value = rule.value
                  if(item[field] && item[field].match(value)){
                    isMatched = true
                  }
                })
              }
              return isMatched
            }).orderBy('seqOrder', 'asc').orderBy('id', 'asc').get()
          if(that._.isFunction(that.fieldSupported)){
            metaDomainFields = metaDomainFields.filter(item => that.fieldSupported(item) )
          }
          let childrenFields = metaDomainFields.map( item => {
            return {
              id: that._.toString(item['id']),
              fieldType: item['fieldType'],
              _caption: item['title']
            }
          })
          return {
            id: metaDomain['id'],
            title: metaDomain['title'],
            children: childrenFields
          }
        })
        if(that.isMeasure){
          groupFields.unshift({
            id: 0,
            title: '扩展计算',
            children: [
              {
                id: 'rowCount',
                fieldType: FIELD.INTEGER,
                fieldName: 'rowCount',
                _caption: '计数'
              }
            ]
          })
        }else if(that.isChart){
          groupFields.push({
            id: 0,
            title: '扩展字段',
            children: [
              {
                id: 'dateCreated',
                fieldType: FIELD.DATE,
                fieldName: 'dateCreated',
                _caption: '创建时间'
              },
              {
                id: 'lastUpdated',
                fieldType: FIELD.DATE,
                fieldName: 'lastUpdated',
                _caption: '最近更新'
              },
            ]
          })
        }
        that.groupDataSource = groupFields.filter( item => item.children.length>0 )
      },
    },
  }
</script>
