<template>
  <div>
    <a-alert :message="cptValue" :type="type"></a-alert>
  </div>
</template>

<!--
# 组件说明
  Alert组件，将value展示为字符串
# 配置组件
  value: String              // 展示的内容
  type: String               // Alert类型，success | info | warning | error
  defaultValue: String       // 默认展示内容
# 操作方法
  $emit('change', value)
-->

<script>
  import AAlert from 'ant-design-vue/lib/alert';

  export default {
    name: 'GAlert',
    components: {
      AAlert,
    },
    /* eslint-disable */
    props: {
      value: {
        // type: Object,
        required: false
      },
      /** 扩展Props */
      type: { // success | info | warning | error
        type: String,
        default: "success"
      },
      defaultValue: {
        type: String,
        default: '无内容'
      },
    },
    computed: {
      cptValue: function(){
        let that = this
console.log('that.value', that.value)
        if(that._.isNil(that.value)){
          return that.defaultValue
        }else if(typeof that.value === 'object'){
          return JSON.stringify(that.value)
        }else{
          return that._.toString(that.value)
        }
      }
    },
    methods: {
      onChange (value) {
        // Never
        this.$emit('change', value)
      },
    },
  }
</script>
