<template>
  <div style="width: 100%;">
    <div v-if="readonly">
      {{ _.get(cptOptionsArray.find(item => item.value === cptValue), 'label') }}
    </div>
    <a-select
      v-else
      style="width: 100%;"
      optionFilterProp="children"
      :disabled="disabled"
      :allowClear="_.defaultTo(_.get(item,'extend.allowClear'), true)"
      :showSearch="_.defaultTo(_.get(item,'extend.showSearch'), false)"
      :placeholder="placeholder"
      :value="cptValue"
      :options="cptOptionsArray"
      @change="onChange($event)">
    </a-select>
  </div>
</template>

<!--
# 组件说明
  NOTE: Select       是在限定的可选项中进行选择，关键词是选择。
        AutoComplete 是输入框而不是选择器，关键词是辅助输入。
  下拉选项组件，下拉选择数据并展示；
  为提高兼容性，组件会将选项值转为字符串；
# 配置组件
  value: [String, Number]   // 当前选中条目
  disabled: Boolean         // 禁用状态，默认false
  placeholder: String       // 占位字符串
  optionsArray: Array       // 下拉选项
# 操作方法
  $emit('change', value)
-->

<script>
  export default {
    name: 'GSelect',
    components: {
    },
    /* eslint-disable */
    props: {
      item: {
        type: Object,
        required: true
      },
      value: {
        type: [String, Number, Boolean],
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
      // [{label:'label1', value: 1}, {label:'label2', value: 2}]
      optionsArray: {
        type: Array,
        required: true
      },
    },
    computed: {
      cptValue: function() {
        let that = this
        return that._.toString(that.value)===''?undefined:that._.toString(that.value)
      },
      cptOptionsArray: function(){
        let that = this
        return that.optionsArray.map(item =>{
          return {label: item.label, value: that._.toString(item.value), color: item.color}
        })
      }
    },
    methods: {
      onChange (value) {
        this.$emit('change', value)
      },
    },
  }
</script>
