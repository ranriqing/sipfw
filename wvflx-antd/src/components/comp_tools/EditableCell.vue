<template>
  <div class="editable-cell">
    <!-- 编辑模式 -->
    <div v-if="editable" class="input-wrapper">
      <!-- 字符串类型 -->
      <a-input
        v-if="type === 'String'"
        v-model="innerValue"
        @pressEnter="doConfirmCheck"
        size="small"
        autofocus />
      <!-- 数字类型（Integer/Long/BigDecimal） -->
      <a-input-number
        v-else
        v-model="innerValue"
        @pressEnter="doConfirmCheck"
        size="small"
        autofocus
        :precision="type === 'Integer' || type === 'Long' ? 0: precision"
        :min="type === 'Integer' || type === 'Long' ? -Infinity : 0"
        :step="1" /> <!-- :parser="_parseNumber" -->
      <a-icon type="check" class="icon-check" @click="doConfirmCheck" />
    </div>

    <!-- 展示模式 -->
    <div v-else class="text-wrapper">
      {{ displayValue }}
      <a-icon type="edit" class="edit-icon" @click="onEdit" />
    </div>
  </div>
</template>

<script>

export default {
  name: 'EditableCell',
  props: {
    value: {
      type: [String, Number],
      default: ''
    },
    // 数据类型（String/Integer/Long/BigDecimal）
    type: {
      type: String,
      default: 'String',
      validator: val => ['String', 'Integer', 'Long', 'BigDecimal'].includes(val)
    },
    // 数字精度（仅BigDecimal生效）
    precision: {
      type: Number,
      default: 2
    }
  },
  data() {
    return {
      editable: false,
      innerValue: this._formatInitialValue(this.value)
    };
  },
  watch: {
    value: {
      immediate: true,
      handler(newVal) {
        this.innerValue = this._formatInitialValue(newVal);
      }
    }
  },
  computed: {
    // 格式化展示值
    displayValue() {
      let that = this
      if (that._.isNil(that.innerValue)) {
        return '-';
      }
      // BigDecimal按精度展示
      if (that.type === 'BigDecimal') {
        return Number(that.innerValue).toFixed(that.precision);
      }
      return that.innerValue;
    }
  },
  methods: {
    onEdit() {
      this.editable = true;
    },
    doConfirmCheck() {
      let that = this
      that.editable = false;
      // 根据类型格式化最终值
      let finalValue = that.innerValue;
      if (that.type === 'Integer' || that.type === 'Long') {
        finalValue = parseInt(finalValue, 10);
      } else if (that.type === 'BigDecimal') {
        finalValue = parseFloat(finalValue).toFixed(that.precision);
      }
      that.$emit('change', finalValue);
    },
    /** 工具方法 */
    _parseNumber(value) {
      let that = this
      if (that.type === 'BigDecimal') {
        return parseFloat(value).toFixed(that.precision);
      }
      return parseInt(value, 10);
    },
    _formatInitialValue(val) {
      let that = this
      if (that._.isNil(val)) {
        return that.type === 'String' ? '' : undefined;
      }
      if (['Integer', 'Long', 'BigDecimal'].includes(that.type)) {
        return Number(val);
      }
      return val;
    },
  }
};
</script>

<style lang="scss" scoped>
.editable-cell {
  position: relative;
  .input-wrapper {
    display: flex;
    align-items: center;
    gap: 8px;
    .icon-check {
      font-size: 14px;
      color: #039267;
      cursor: pointer;
    }
  }
  .text-wrapper {
    padding: 5px 0;
    cursor: pointer;
    display: flex;
    align-items: center;
    gap: 8px;
    .edit-icon {
      font-size: 14px;
      color: #039267;
      visibility: hidden;
    }
    &:hover{
      .edit-icon {
        visibility: visible;
      }
    }
  }
}

</style>