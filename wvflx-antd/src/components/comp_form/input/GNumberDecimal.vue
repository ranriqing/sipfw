<template>
  <div>
    <div v-if="readonly">
      {{ _.toString(value) }}
    </div>
    <a-input-number
      v-else
      style="width: 100%"
      :value="value"
      :disabled="disabled"
      :placeholder="placeholder"
      :min="_.defaultTo(_.get(item,'extend.min'), 0)"
      :max="_.defaultTo(_.get(item,'extend.max'), undefined)"
      :step="_.defaultTo(_.get(item,'extend.step'), 1)"
      :precision="_.defaultTo(_.get(item,'extend.precision'), 2)"
      @change="onChange">
    </a-input-number>
  </div>
</template>

<!--
# 组件说明
  输入精确数值
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
    name: 'GNumberDecimal',
    components: {
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
