<template>
  <div>
    <div v-if="readonly">
      {{ _.get(cptOptionsArray.find(item => item.value === cptValue), 'label') }}
    </div>
    <div
      v-else
      :class="[_.defaultTo(_.get(item,'extend.optionVertical'), false) && 'vertical-group']">
      <a-radio-group
        style="width: 100%;"
        :value="cptValue"
        :disabled="disabled"
        :options="cptOptionsArray"
        @change="onChange($event.target.value)">
      </a-radio-group>
    </div>
  </div>
</template>

<!--
# 组件说明
  单选按钮组件，构造单选按钮组；
  为提高兼容性，组件会将选项值转为字符串；
  eg: [{label:'label1', value: 1}] 转为 [{label:'label1', value: '1'}]
# 配置组件
  value: [String, Number, Boolean]   // 当前选中条目
  readonly: Boolean   // 是否只读，默认false
  disabled: Boolean   // 是否禁用，默认false
  placeholder: String // 占位字符串
  optionsArray: Array                // 下拉选项 eg: [{label:'label1', value: 1}, {label:'label2', value: 2}]
# 操作方法
  $emit('change', value)
-->

<script>
  import ARadio from 'ant-design-vue/lib/radio';

  export default {
    name: 'GRadioGroup',
    components: {
      ARadioGroup: ARadio.Group,
    },
    /* eslint-disable */
    props: {
      item: {
        type: Object,
        required: true
      },
      value: {
        type: [String, Number, Boolean],
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
      /** 扩展Props */
      // [{label:'label1', value: 1}, {label:'label2', value: 2}]
      optionsArray: {
        type: Array,
        required: true
      },
    },
    computed: {
      cptValue: function() {
        let that = this
        return that._.toString(that.value)===''?undefined:that._.toString(that.value)
      },
      cptOptionsArray: function(){
        let that = this
        return that.optionsArray.map(item =>{
          return {label: item.label, value: that._.toString(item.value)}
        })
      }
    },
    methods: {
      onChange (value) {
        this.$emit('change', value)
      },
    },
  }
</script>

<style lang="scss" scoped>
  .vertical-group{
    .ant-radio-group{
      /deep/ .ant-radio-wrapper{
        display: block;
        height: 30px;
        line-height: 30px;
      }
    }
  }
</style>
