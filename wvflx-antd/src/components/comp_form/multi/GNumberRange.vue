<template>
  <div>
    <div v-if="readonly">
      {{ _.toString(minValue) || 'min' }} ~ {{ _.toString(maxValue) || 'max' }}
    </div>
    <a-input-group
      v-else
      class="range-input-group">
      <a-input-number
        v-model="minValue"
        :disabled="disabled"
        @change="onChange"
        class="range-number range-number-min"
        :placeholder="cptPlaceholderFrom">
      </a-input-number>
      <a-input :class="['range-center-mark', disabled?'gray': 'white']" placeholder="~" disabled />
      <a-input-number
        v-model="maxValue"
        :disabled="disabled"
        @change="onChange"
        class="range-number range-number-max"
        :placeholder="cptPlaceholderTo">
      </a-input-number>
    </a-input-group>
  </div>
</template>

<!--
# 组件说明
  数值区间选择组件，可以选择一个数值范围，组件不会校验最小和最大值的范围是否符合要求;
  必须value[0]的minValue和value[1]的maxValue同时满足_.isNumber时才会触发change事件;
# 组件图标
  <a-icon type="up" />
  <a-icon type="down" />
# 配置组件
  value: Array              // 数值范围
  placeholder: String       // 占位字符串
  placeholderFrom: String   // 占位字符串(开始)
  placeholderTo: String     // 占位字符串(结束)
# 操作方法
  $emit('change', [that.minValue, that.maxValue])
-->

<script>
  export default {
    name: 'GNumberRange',
    components: {
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
        default: '数值'
      },
    },
    data () {
      return {
        minValue: null,
        maxValue: null,
      }
    },
    watch: {
      value: function(newValue) {
        let that = this
        if(!newValue){
          that.minValue = null
          that.maxValue = null
        }else{
          that.minValue = newValue[0]
          that.maxValue = newValue[1]
        }
      }
    },
    computed: {
      cptPlaceholderFrom: function(){
        let that = this
        return that._.get(that.item,'extend.placeholderFrom') || `${that.placeholder}(从)`
      },
      cptPlaceholderTo: function(){
        let that = this
        return that._.get(that.item,'extend.placeholderTo') || `${that.placeholder}(到)`
      },
    },
    methods: {
      onChange () {
        let that = this
        if(that._.isNumber(that.minValue) || that._.isNumber(that.maxValue)){
          that.$emit('change', [that.minValue, that.maxValue])
        }else{
          that.$emit('change', null)
        }
      },
    },
  }
</script>

<style lang="scss" scoped>
  /* 范围输入框 */
  .ant-input-group.range-input-group{
    width: 100%;
    display: flex;
    // 两边输入框
    .range-number{
      float: left;
      flex: auto;
      &.range-number-min{
        border-top-right-radius: unset;
        border-bottom-right-radius: unset;
        border-right: 0;
      }
      &.range-number-max{
        border-top-left-radius: unset;
        border-bottom-left-radius: unset;
        border-left: 0;
      }
    }
    // 中间标识符
    .range-center-mark{
      width: 25px;
      border-left: 0;
      border-right: 0;
      pointer-events: none;
      &.white{
        background-color: #fff;
      }
      display: inline-block;
      text-align: center;
      padding: 0;
      /deep/ .ant-input{
        padding: 4px 2px;
        background: white;
        border-left: 0;
        border-right: 0;
      }
    }
  }
</style>