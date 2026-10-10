<template>
  <div>
    <draggable v-bind="cptDragOptions" :list="optionsArray" @change="onChange">
      <div
        class="item-group"
        v-for="(option, oIndex) in optionsArray"
        :key="oIndex" >
        <!-- 拖拽Icon -->
        <a-icon class="drag drag-widget" type="menu"/>
        <!-- 颜色选择 -->
        <a-dropdown class="color-dropdown" @click.stop>
          <span class="color-trigger">
            <a-badge status="default" :color="option.color" />
          </span>
          <a-menu slot="overlay" @click="(event) => onColorPick(option, event)">
            <a-menu-item v-for="(styleItem) in tagStyles" :key="styleItem.caption">
              <a-badge v-if="styleItem.color==='default'" status="default" :text="styleItem.caption" />
              <a-badge v-else status="default" :color="styleItem.color" :text="styleItem.caption" />
            </a-menu-item>
          </a-menu>
        </a-dropdown>
        <a-input
          class="input"
          v-model="option.label"
          :placeholder="labelHolder"
          @blur="onInputBlur"> <!-- 使用blur事件，防止为字符串时触发@change="onChange" -->
          <a-icon slot="prefix" type="g-field-number" @click="onOptionNumber(option)"></a-icon>
        </a-input>
        <!-- 删除Icon -->
        <a-icon
          class="trash"
          type="delete"
          @click="onOptionDelete(option, oIndex)">
        </a-icon>
      </div>
    </draggable>
    <a-button type="dashed" :block="true" icon="plus" @click="onOptionAdd">{{ addCaption }}</a-button>
  </div>
</template>

<!--
# 组件说明
  下拉选项配置组件，可以配置(添加、编辑、删除、拖拽排序)下拉选项；
  数据交换使用Array [{label, value, color?}]，支持颜色选择；
# 组件图标
  <a-icon type="plus" />
  <a-icon type="menu" />
  <a-icon type="delete" />
# 配置组件
  labelHolder: String        // 选项Label
  addCaption: String         // 添加选项
# 操作方法
  $emit('change', optionsArray)
-->

<script>
  export default {
    name: 'GOptionsEditor',
    components: {
    },
    /* eslint-disable */
    props: {
      value: {
        type: Array,
        required: false
      },
      /** 扩展Props */
      labelHolder: {
        type: String,
        required: false,
        default: '选项Label'
      },
      addCaption: {
        type: String,
        required: false,
        default: '添加选项'
      },
    },
    watch: {
      value: function() {
        let that = this
        let val = that.value
        if (!val) {
          that.optionsArray = []
        } else if (Array.isArray(val)) {
          that.optionsArray = val
        } else if (typeof val === 'object') {
          // convert old {label: value} format
          that.optionsArray = Object.keys(val).map(k => ({label: k, value: val[k]}))
        } else {
          that.optionsArray = []
        }
      },
    },
    data () {
      return {
        optionsArray: [],
        tagStyles: [
          {color: 'red', caption: '红色'}, {color: 'orange', caption: '橙色'},
          {color: 'green', caption: '绿色'}, {color: 'blue', caption: '蓝色'},
          {color: 'purple', caption: '紫色'}, {color: '#ff3b30', caption: '深红'},
          {color: '#ff9500', caption: '深橙'}, {color: '#2fbc52', caption: '深绿'},
          {color: '#108ee9', caption: '深蓝'}, {color: '#9952e1', caption: '深紫'},
          {color: 'default', caption: '默认'}
        ],
      }
    },
    computed: {
      cptDragOptions: function() {
        return {
          animation: 200,
          group: 'options',
          ghostClass: 'ghost',
          handle: '.drag-widget',
        };
      },
    },
    methods: {
      onChange() {
        let that = this
        let optionsArray = that.optionsArray.filter(item =>{
          return item.label && item.value
        })
        this.$emit('change', optionsArray)
      },
      onInputBlur(e){
        let that = this
        let inputValue = e.target.value
        if(!that._.isEmpty(inputValue)){
          that.onChange(e)
        }else{
          that.onChange(e)
        }
      },
      /** 扩展方法 */
      onColorPick(option, event) {
        let that = this
        let styleObj = that.tagStyles.find(styleItem => styleItem.caption === event.key)
        if(styleObj){
          option.color = styleObj.color === 'default' ? undefined : styleObj.color
          that.onChange()
        }
      },
      onOptionAdd(){
        // 尝试按数值递增：支持 number / 数字字符串
        let maxValue = 0
        this.optionsArray?.forEach(item => {
          let n = Number(item?.value)
          if(Number.isFinite(n)){
            maxValue = n > maxValue ? n : maxValue
          }
        })
        this.optionsArray?.push({
          label: '',
          value: maxValue+1
        })
      },
      onOptionNumber(option){
        let that = this
        // 统一按字符串编辑（value 可为任意类型，最终写回 string）
        let attrs = { style: 'width: 100%' }
        let tempValue = that._.isNil(option?.value) ? '' : that._.toString(option.value)
        that.$confirm({
          title: `调整选项【${option.label}】的值，请谨慎操作?`,
          content: <a-input {...{ attrs }}
            placeholder="选项值"
            defaultValue={tempValue}
            onChange={ e => tempValue = e?.target?.value }
          />,
          onOk() {
            option.value = tempValue
            that.onChange()
          }
        });
      },
      onOptionDelete(option, oIndex){
        this.optionsArray.splice(oIndex, 1)
        this.onChange()
      },
    },
  }
</script>

<style lang="scss" scoped>
  /* 选项编辑样式 */
  .item-group{
    display: flex;
    align-items: center;
    border-bottom: 1px solid #e5e5e5;
    /deep/ .ant-input-affix-wrapper .ant-input{
      padding-left: 40px;
    }
    /deep/ input {
      flex: auto;
      padding: 0 6px;
      border: 0;
      background-color: transparent;
      transition: all .3s;
      outline: unset;
      box-shadow: unset;
      padding-left: 40px;
    }
    .drag{
      font-size: 15px;
      cursor: move;
    }
    .trash {
      font-size: 15px;
      &:hover{
        color: #1f1f1f;
        cursor: pointer;
      }
    }
    .color-trigger {
      cursor: pointer;
      margin-left: 12px;
      margin-right: 0;
      /deep/ .ant-badge-status-text{
        margin-left: 0;
      }
    }
  }
</style>