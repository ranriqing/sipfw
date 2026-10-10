<template>
  <!-- 为空组件 -->
  <GDateRange
    v-if="_.isNil(_.get(item.extend,'operator'))"
    :placeholder="placeholder"
    :dateFormat="_.defaultTo(_.get(item.extend,'format'), DATE.YYYYMMDD)"
    :value="_.isArray(value)?value:undefined"
    @change="onChange">
  </GDateRange>
  <!-- 相对日期组件 -->
  <GSelect
    v-else-if="_.get(item.extend,'operator') === '_relative'"
    :placeholder="placeholder"
    :item="{}"
    :value="(typeof value === 'string')?value:undefined"
    :optionsArray="converters.objToOptions(
      converters.reverseObj(configObj.relativeOptions)
    )"
    @change="onChange">
  </GSelect>
  <!-- 非BETWEEN组件 -->
  <a-date-picker
    v-else-if="_.get(item.extend,'operator') !== QUERY.BETWEEN"
    style="width: 100%"
    :placeholder="placeholder"
    :format="_.defaultTo(_.get(item.extend,'format'), DATE.YYYYMMDD)"
    :value="!_.isArray(value)?value:undefined"
    @change="onChange">
  </a-date-picker>
  <!-- 为BETWEEN组件 -->
  <GDateRange
    v-else
    :placeholder="placeholder"
    :dateFormat="_.defaultTo(_.get(item.extend,'format'), DATE.YYYYMMDD)"
    :value="_.isArray(value)?value:undefined"
    @change="onChange">
  </GDateRange>
</template>

<!--
# 组件说明
  日期查询组件，展示placeholder的字符串；
# 配置组件
  item: Object               // 查询Item
  value: String              // 展示的内容
  placeholder: String        // 占位符
# 操作方法
  $emit('change', value)
-->

<script>
  import { QUERY, DATE } from '@/constants'
  import { converters } from '@/utils/util'

  import GSelect from '../select/GSelect'
  import GDateRange from '../multi/GDateRange'

  import qDynDateConfig from './configs/qDynDateConfig'

  export default {
    name: 'QDynDate',
    components: {
      GSelect,
      GDateRange,
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
        DATE: DATE,
        converters: converters,
        configObj: qDynDateConfig,
      }
    },
    methods: {
      onChange (value) {
        this.$emit('change', value)
      },
    },
  }
</script>
