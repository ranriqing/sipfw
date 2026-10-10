<template>
  <!-- 为空组件 -->
  <GNumberRange
    v-if="_.isNil(_.get(item.extend,'operator'))"
    :placeholder="placeholder"
    :value="value"
    @change="onChange">
  </GNumberRange>
  <!-- 非BETWEEN组件 -->
  <a-input-number
    v-else-if="_.get(item.extend,'operator') !== QUERY.BETWEEN"
    style="width: 100%"
    :placeholder="placeholder"
    :value="value"
    @change="onChange">
  </a-input-number>
  <!-- 为BETWEEN组件 -->
  <GNumberRange
    v-else
    :placeholder="placeholder"
    :value="value"
    @change="onChange">
  </GNumberRange>
</template>

<!--
# 组件说明
  数值查询组件，展示placeholder的字符串；
# 配置组件
  item: Object               // 查询Item
  value: String              // 展示的内容
  placeholder: String        // 占位符
# 操作方法
  $emit('change', value)
-->

<script>
  import { QUERY } from '@/constants'

  import GNumberRange from '../multi/GNumberRange'

  export default {
    name: 'QDynInteger',
    components: {
      GNumberRange,
    },
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
      placeholder: {
        type: String,
        default: '数值'
      },
    },
    data () {
      return {
        QUERY: QUERY,
      }
    },
    methods: {
      onChange (value) {
        this.$emit('change', value)
      },
    },
  }
</script>
