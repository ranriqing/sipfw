<template>
  <div>
    <div
      class="item-group"
      v-for="(option, oIndex) in optionsArray"
      :key="oIndex">
      <a-input
        class="input input-code"
        v-model="option.value"
        :placeholder="codeHolder"
        @blur="onInputBlur">
      </a-input>
      <a-input
        class="input input-label"
        v-model="option.label"
        :placeholder="labelHolder"
        @blur="onInputBlur">
      </a-input>
      <a-icon
        class="trash"
        type="delete"
        @click="onOptionDelete(oIndex)">
      </a-icon>
    </div>
    <a-button type="dashed" :block="true" icon="plus" @click="onOptionAdd">{{ addCaption }}</a-button>
  </div>
</template>

<script>
  export default {
    name: 'GSipCodeMapEditor',
    props: {
      value: {
        type: Object,
        required: false,
        default: function(){
          return {}
        }
      },
      codeHolder: {
        type: String,
        required: false,
        default: 'SIP状态码'
      },
      labelHolder: {
        type: String,
        required: false,
        default: '显示名'
      },
      addCaption: {
        type: String,
        required: false,
        default: '添加映射'
      },
    },
    data() {
      return {
        optionsArray: [],
        skipNextSync: false,
      }
    },
    watch: {
      value: {
        immediate: true,
        handler(newValue) {
          if (this.skipNextSync) {
            this.skipNextSync = false
            return
          }
          this.optionsArray = this.toOptionsArray(newValue)
        }
      }
    },
    methods: {
      onInputBlur() {
        this.emitValue()
      },
      onOptionAdd() {
        this.optionsArray.push({
          value: '',
          label: '',
        })
      },
      onOptionDelete(index) {
        this.optionsArray.splice(index, 1)
        this.emitValue()
      },
      emitValue() {
        let optionObj = {}
        this.optionsArray.forEach(item => {
          let code = String(item.value || '').trim()
          let label = String(item.label || '').trim()
          if (code && label) {
            optionObj[code] = label
          }
        })
        this.skipNextSync = true
        this.$emit('change', optionObj)
      },
      toOptionsArray(sourceObj) {
        let target = []
        let obj = sourceObj || {}
        Object.keys(obj).forEach(code => {
          let sipCode = String(code || '').trim()
          let label = String(obj[code] || '').trim()
          target.push({
            value: sipCode,
            label: label,
          })
        })
        return target
      },
    },
  }
</script>

<style lang="scss" scoped>
  .item-group{
    display: flex;
    align-items: center;
    gap: 8px;
    border-bottom: 1px solid #e5e5e5;
    .input{
      flex: 1 1 auto;
    }
    .input-code{
      flex: 0 0 35%;
      max-width: 180px;
    }
    .trash {
      font-size: 15px;
      &:hover{
        color: #1f1f1f;
        cursor: pointer;
      }
    }
  }
</style>
