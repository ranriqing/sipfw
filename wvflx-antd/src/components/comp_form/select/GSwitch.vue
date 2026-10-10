<template>
  <div>
    <div v-if="readonly">
      {{ `${_.get(item,'extend.btnTitle')}(${(!!value)?'开':'关'})` }}
    </div>
    <a-switch
      v-else
      :checked="value"
      :disabled="disabled"
      :checkedChildren="_.get(item,'extend.btnTitle')"
      :unCheckedChildren="_.get(item,'extend.btnTitle')"
      @change="onChange"/>
  </div>
</template>

<!--
# 组件说明
  Switch开关组件，支持开启、关闭功能；
# 配置组件
  value: Boolean      // 当前Switch状态
  readonly: Boolean   // 是否只读，默认false
  disabled: Boolean   // 是否禁用，默认false
  placeholder: String // 占位字符串
# 操作方法
  $emit('change', value)
-->

<script>
  export default {
    name: 'GSwitch',
    components: {
    },
    /* eslint-disable */
    props: {
      item: {
        type: Object,
        required: true
      },
      value: {
        type: Boolean,
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
