<template>
  <div>
    <div v-if="readonly">
      {{ $moment(value).format(cptDateFormat) }}
    </div>
    <a-month-picker
      v-else-if="DATE.YYYYMM===cptDateFormat"
      style="width: 100%"
      :value="value"
      :disabled="disabled"
      :placeholder="placeholder"
      :format="cptDateFormat"
      @change="onChange"/>
    <a-date-picker
      v-else
      style="width: 100%"
      :value="value"
      :disabled="disabled"
      :placeholder="placeholder"
      :showToday="[DATE.YYYYMMDD, DATE.YYYYMMDD_HHMM, DATE.YYYYMMDD_HHMMSS].includes(cptDateFormat)"
      :defaultPickerValue="extendPickerValue()"
      :show-time="[ DATE.YYYYMMDD_HHMM, DATE.YYYYMMDD_HHMMSS].includes(cptDateFormat)"
      :format="cptDateFormat"
      @change="onChange">
    </a-date-picker>
  </div>
</template>

<!--
# 组件说明
  日期选择
# 配置组件
  value: String       // 输入框内容
  readonly: Boolean   // 是否只读，默认false
  disabled: Boolean   // 是否禁用，默认false
  placeholder: String // 占位字符串
# 操作方法
  $emit('change', value)
-->

<script>
  import { DATE } from '@/constants'
  import moment from "moment/moment";

  export default {
    name: 'GDatePicker',
    components: {
    },
    /* eslint-disable */
    props: {
      item: {
        type: Object,
        required: true
      },
      value: {
        type: String,
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
    },
    data () {
      return {
        DATE: DATE,
      }
    },
    computed: {
      moment() {
        return moment
      },
      cptDateFormat: function() {
        let that = this
        return that._.defaultTo(that._.get(that.item,'extend.format'), DATE.YYYYMMDD)
      },
    },
    methods: {
      extendPickerValue(pickerValue){
        let that = this
        let pickerValueObj = that._.get(that.item, 'extend.pickerValue') || pickerValue
        if(that._.isFunction(pickerValueObj)){
          let pickerValueFunc = pickerValueObj
          return pickerValueFunc(that.record, that.context)
        }else{
          // g-date-picker 类型处理
          if(that.item.component === 'g-date-picker'){
            if(pickerValueObj === 'now'){
              pickerValueObj = that.$moment()
            }else if(pickerValueObj === 'birthday'){
              pickerValueObj = that.$moment('1980-01-01', 'YYYY-MM-DD')
            }
          }
          return pickerValueObj
        }
      },
      onChange (value) {
        this.$emit('change', value)
      },
    },
  }
</script>
