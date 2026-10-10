<template>
  <div>
    <span
      v-if="_.get(item, 'component') === COMPO.G_DATE_PICKER">
      {{ value?$moment(value).format(_.defaultTo(_.get(item,'extend.format'), DATE.YYYYMMDD)):cptValue }}
    </span>
    <span
      v-else-if="_.get(item, 'component') === COMPO.G_SELECT">
      {{ value?(reverseOptionsObj()[value] || cptValue): cptValue }}
    </span>
    <span v-else>{{ cptValue }}</span>
  </div>
</template>

<!--
# 组件说明
  内容展示组件，展示value的字符串;
  与GNote区别是，GText展示的为value动态内容；
# 配置组件
  value: String              // 展示的内容
  defaultValue: String       // 默认展示内容
# 操作方法
  $emit('change', value)
-->

<script>
  import { COMPO, DATE } from '@/constants'
  import { converters } from '@/utils/util'

  export default {
    name: 'GText',
    /* eslint-disable */
    props: {
      item: {
        // type: Object,
        required: false
      },
      value: {
        // type: Object,
        required: false
      },
      /** 扩展Props */
      defaultValue: {
        type: String,
        default: '无内容'
      },
    },
    computed: {
      cptValue: function(){
        let that = this
        if(that._.isNil(that.value)){
          return that.defaultValue
        }else if(typeof that.value === 'object'){
          return JSON.stringify(that.value)
        }else{
          return that._.toString(that.value)
        }
      }
    },
    data(){
      return {
        COMPO: COMPO,
        DATE: DATE,
      }
    },
    methods: {
      // 选项数组
      reverseOptionsObj() {
        let that = this
        // eg: {Caption1: value1, Caption2: value2, Caption3: value3}
        let optionsObj = that._.get(that.item,'extend.options') || {}
        return converters.reverseObj(optionsObj)
      },
      onChange (value) {
        // Never
        this.$emit('change', value)
      },
    },
  }
</script>
