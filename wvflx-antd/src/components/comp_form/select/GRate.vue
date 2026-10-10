<template>
  <div>
    <a-rate
      :value="value"
      :disabled="readonly || disabled"
      :placeholder="placeholder"
      :count="_.defaultTo(_.get(item,'extend.count'), 5)"
      @change="onChange"/>
  </div>
</template>

<!--
# 组件说明
  选择评分
# 配置组件
  value: Number       // 输入框内容
  readonly: Boolean   // 是否只读，默认false
  disabled: Boolean   // 是否禁用，默认false
  placeholder: String // 占位字符串
# 操作方法
  $emit('change', value)
-->

<script>
  import ARate from 'ant-design-vue/lib/rate';

  export default {
    name: 'GRate',
    components: {
      ARate,
    },
    /* eslint-disable */
    props: {
      item: {
        type: Object,
        required: true
      },
      value: {
        type: Number,
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
    methods: {
      onChange (value) {
        this.$emit('change', value)
      },
    },
  }
</script>
