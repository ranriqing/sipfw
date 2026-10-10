<template>
  <div>
    <div v-if="readonly">
      {{ _.toString(value) || 'None' }}
    </div>
    <a-input
      v-else
      :value="value"
      :disabled="disabled"
      :placeholder="placeholder"
      :type="_.defaultTo(_.get(item,'extend.inputType'), 'text')"
      @change="onChange"/>
  </div>
</template>

<!--
# 组件说明
  输入文本
# 配置组件
  value: String       // 输入框内容
  readonly: Boolean   // 是否只读，默认false
  disabled: Boolean   // 是否禁用，默认false
  placeholder: String // 占位字符串
# 操作方法
  $emit('change', value)
-->

<script>
  export default {
    name: 'GInput',
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
    methods: {
      onChange (value) {
        this.$emit('change', value)
      },
    },
  }
</script>
