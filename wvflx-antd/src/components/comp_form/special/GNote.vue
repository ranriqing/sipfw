<template>
  <div>
    <!-- 属性placeholder不受form的value影响 -->
    <span>{{ placeholder }}</span>
  </div>
</template>

<!--
# 组件说明
  内容展示组件，展示placeholder的字符串；
  与GText区别是，GNote展示的为placeholder的固定内容；
# 配置组件
  value: String              // 展示的内容
# 操作方法
  $emit('change', value)
-->

<script>
  export default {
    name: 'GNote',
    /* eslint-disable */
    props: {
      value: {
        // type: Object,
        required: false
      },
      placeholder: {
        type: String,
        default: '说明内容'
      },
    },
    methods: {
      onChange (value) {
        // Never
        this.$emit('change', value)
      },
    },
  }
</script>
