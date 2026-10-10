<template>
  <div>
    <div v-if="readonly">
      {{ cptReadOnlyStr }}
    </div>
    <a-range-picker
      v-else-if="DATE.YYYYMM===cptDateFormat"
      style="width: 100%"
      :value="cptValue"
      :disabled="disabled"
      :mode="['month', 'month']"
      :format="cptDateFormat"
      @panelChange="onMonthChange">
    </a-range-picker>
    <a-range-picker
      v-else
      style="width: 100%"
      :value="cptValue"
      :disabled="disabled"
      :allowClear="cptAllowClear"
      :showTime="cptShowTime"
      :format="cptDateFormat"
      :placeholder="[cptPlaceholderFrom, cptPlaceholderTo]"
      @change="onChange">
      <span v-if="_.get(item,'extend.suffixDays')" slot="suffixIcon" style="width: 40px;">{{ cptSuffixIcon }}</span>
      <template slot="renderExtraFooter">
        <a-tag @click="onTimeRange('lastDay')">昨天</a-tag>
        <a-tag color="green" @click="onTimeRange('day')">今天</a-tag>
        <a-tag color="cyan" @click="onTimeRange('week')">本周</a-tag>
        <a-tag color="blue" @click="onTimeRange('month')">本月</a-tag>
        <a-tag @click="onTimeRange('lastMonth')">上月</a-tag>
      </template>
    </a-range-picker>
  </div>
</template>

<!-- https://www.antdv.com/components/date-picker-cn/#API -->
<!--
# 组件说明
  日期范围选择组件，支持三种日期格式；
  可以快捷选中昨天、今天、本周、本月的日期，展示的日期必须通过value设置才生效；
# 配置组件
  value: Array              // 日期范围
  disabled: Boolean         // 是否禁用，默认false
  placeholder: Array        // 占位字符串数组
  extend.dateFormat: String // 日期格式，默认DATE.YYYYMMDD
# 操作方法
  $emit('change', [that.minValue, that.maxValue])
-->

<script>
  import { DATE } from '@/constants'

  export default {
    name: 'GDateRange',
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
        default: ''
      },
      allowClear: {
        type: Boolean,
        default: true
      },

    },
    data () {
      return {
        DATE: DATE,
      }
    },
    computed: {
      cptValue: function() {
        let that = this
        if(that._.isArray(that.value)){
          return that.value
        }
      },
      cptAllowClear: function(){
        let that = this
        return that._.defaultTo(that._.get(that.item,'extend.allowClear'), true)
      },
      cptDateFormat: function(){
        let that = this
        return that._.get(that.item,'extend.format', DATE.YYYYMMDD)
      },
      cptPlaceholderFrom: function(){
        let that = this
        return that._.get(that.item,'extend.placeholderFrom') || `${that.placeholder}(起)`
      },
      cptPlaceholderTo: function(){
        let that = this
        return that._.get(that.item,'extend.placeholderTo') || `${that.placeholder}(止)`
      },
      cptSuffixIcon: function(){
        let that = this
        if(that._.isArray(that.value) && that.value.length === 2 && that.value[0] && that.value[1]) {
          let startDate = that.$moment(that.value[0]);
          let endDate = that.$moment(that.value[1]);
          let days = Math.abs(endDate.diff(startDate, 'days'));
          return days + "天"
        }
      },
      cptReadOnlyStr: function(){
        let that = this
        let fromMoment = that._.get(that.cptValue, '[0]')
        let toMoment = that._.get(that.cptValue, '[1]')
        return `${fromMoment?.format(that.cptDateFormat) || 'min'} ~ ${toMoment?.format(that.cptDateFormat) || 'max'}`
      },
      cptShowTime: function(){
        let that = this
        if([DATE.YYYYMM, DATE.YYYYMMDD].includes(that.cptDateFormat)){
          return false
        }else{
          return {
            // 'HH:mm' 或 'HH:mm:ss'
            format:(
              {
                [DATE.YYYYMMDD_HHMM]:'HH:mm',
                [DATE.YYYYMMDD_HHMMSS]:'HH:mm:ss'
              }[that.cptDateFormat]
            )
          }
        }
      },
    },
    methods: {
      onMonthChange(momentArray, modeArray){
        modeArray || true
        this.onChange (momentArray, null)
      },
      onChange (momentArray, strArray) {
        strArray || true
        this.$emit('change', momentArray)
      },
      /** 扩展方法 */
      onTimeRange(rangeStr){
        let that = this
        if('lastDay' === rangeStr) {
          this.onChange([that.$moment().startOf('day').subtract(1, 'days'), that.$moment().endOf('day').subtract(1, 'days')])
        }else if('day' === rangeStr){
          this.onChange([that.$moment().startOf('day'), that.$moment().endOf('day')])
        }else if('week' === rangeStr){
          this.onChange([that.$moment().startOf('week'), that.$moment().endOf('week')])
        }else if('month' === rangeStr){
          this.onChange([that.$moment().startOf('month'), that.$moment().endOf('month')])
        }else if('lastMonth' === rangeStr){
          this.onChange([that.$moment().month(that.$moment().month()-1).startOf('month'), that.$moment().month(that.$moment().month()-1).endOf('month')])
        }
      }
    },
  }
</script>
