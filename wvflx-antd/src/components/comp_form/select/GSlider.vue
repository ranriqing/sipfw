<template>
  <div style="padding: 0 10px;">
    <a-slider
      :value="value"
      :disabled="readonly || disabled"
      :placeholder="placeholder"
      :min="_.defaultTo(_.get(item,'extend.min'), 0)"
      :max="_.defaultTo(_.get(item,'extend.max'), 100)"
      :step="_.defaultTo(_.get(item,'extend.step'), 1)"
      :dots="_.defaultTo(_.get(item,'extend.dots'), false)"
      :marks="_.defaultTo(_.get(item,'extend.marks'), [])"
      @change="onChange"/>
  </div>
</template>

<!--
# 组件说明
  选择滑块
# 配置组件
  value: Number       // 输入框内容
  readonly: Boolean   // 是否只读，默认false
  disabled: Boolean   // 是否禁用，默认false
  placeholder: String // 占位字符串
# 操作方法
  $emit('change', value)
-->

<script>
  import ASlider from 'ant-design-vue/lib/slider';

  export default {
    name: 'GSlider',
    components: {
      ASlider,
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
