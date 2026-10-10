<template>
  <div class="draggable-cols-container">
    <draggable v-bind="cptDragOptions" :list="colsArray" @change="onChange">
      <div
        :class="['draggable-col', (cIndex===0)&&'first-col' ]"
        v-for="(colItem, cIndex) in colsArray"
        :key="cIndex">
        <a-icon :type="vDftUtil.fieldIcon(colItem, 'type')" class="drag drag-widget"/>
        <div class="column-item">
          <span class="item-title">{{ colItem.title }}</span>
          <span v-if="cIndex===0" class="item-note text-tips">(主)</span>
        </div>
        <div class="item-actions">
          <a @click="onColDelete(colItem, cIndex)">
            <a-icon class="icon-action" type="close" />
          </a>
        </div>
      </div>
    </draggable>
    <a-button v-if="colsArray.length<colsLimit" type="dashed" :block="true" icon="plus" @click="onColChoose">展示字段</a-button>
  </div>
</template>

<!--
# 组件说明
  引用配置组件，可以配置(添加、编辑、删除、拖拽排序)引用字段；
# 操作方法
  $emit('change', colsArray)
-->

<script>
  import { FIELD } from '@/constants'
  import { vDftUtil } from '@/utilsv/vUtil'

  import MetaDomain from '@/store/models/MetaDomain'
  import MetaDomainField from '@/store/models/MetaDomainField'

  import GDomainCols from '@/components/comp_form/GDomainCols'

  export default {
    name: 'GRefEditor',
    components: {
      GDomainCols,
    },
    /* eslint-disable */
    props: {
      item: {
        type: Object,
        required: true
      },
      value: {
        type: Object,
        required: false
      },
      /** 扩展Props */
      relDomainName: {
        type: String,
        required: false,
      },
      colsLimit: {
        type: Number,
        default: 5
      },
    },
    watch: {
      value: function() {
        let that = this
        if (that.value) {
          that.colsArray = that._.cloneDeep(that.value || [])
        } else {
          that.colsArray = []
        }
      },
    },
    data () {
      return {
        vDftUtil: vDftUtil,
        colsArray: [] // 此处用model绑定，不用计算属性
      }
    },
    computed: {
      cptDragOptions: function() {
        return {
          animation: 200,
          group: 'cols',
          ghostClass: 'ghost',
          handle: '.drag-widget',
        };
      },
    },
    methods: {
      onChange() {
        let that = this
        that.$emit('change', that.colsArray)
      },
      /** 扩展方法 */
      onColChoose(){
        let that = this
        let selectedColumn = undefined
        let targetMetaDomain = MetaDomain.query().where('domainName', that.relDomainName).first()
        that.$confirm({
          icon: 'check',
          title: `请选择展示列`,
          width: '600px',
          class: 'pop-column-wrapper',
          content: function(h){
            h && true
            let vnodes = [];
            let props = {
              isChart: false,
              isMeasure: false,
              echoOptionOn: true,
              metaDomains: [targetMetaDomain],
              fieldSupported: function(item){
                return [
                  FIELD.MANY_TO_ONE, FIELD.ONE_TO_MANY,
                  FIELD.FILE, FIELD.PICTURE,
                ].indexOf(item['fieldType'])===-1
              },
              placeholder: '选择列...'
            }
            const on = {
              change: (dfItemId) => {
                let dfItem = MetaDomainField.find(dfItemId)
                if(dfItem){
                  selectedColumn = {
                    title: dfItem.title,
                    field: dfItem.fieldName,
                    type: dfItem.fieldType,
                    metaDomainFieldId: dfItem.id,
                  }
                }
              }
            }
            vnodes.push(<GDomainCols {...{ props, on: on }}></GDomainCols>);
            return (<div {...{ class: 'selectable-wrapper' }}>{ vnodes }</div>)
          },
          onOk() {
            if(selectedColumn){
              that.colsArray.push(selectedColumn)
              that.onChange()
            }
          }
        });
      },
      onColDelete(item, cIndex){
        this.colsArray.splice(cIndex, 1)
        this.onChange()
      },
    },
  }
</script>

<style lang="scss" scoped>
  .draggable-cols-container{
    margin-top: 4px;
    display: flex;
    flex-direction: column;

    // 拖拽条目
    .draggable-col{
      align-items: center;
      border-radius: 5px;
      border: 1px solid #e5e5e5;
      position: relative;
      width: 100%;
      background-color: #f9f9fa;
      line-height: 28px;
      margin-bottom: 8px;
      &.first-col{
        .column-item{
          .item-title{
            font-weight: bold;
          }
          .item-note{
            margin-left: 4px;
          }
        }
      }
      // cursor: grab;
      // 条目内容
      .column-item {
        display: inline-block;
        margin-left: 6px;
      }
      .drag{
        font-size: 14px;
        padding: 4px 2px 4px 6px;
        cursor: move;
      }
      // 条目操作按钮
      .item-actions{
        position: absolute;
        top: 1px;
        right: 6px;
        .icon-action {
          vertical-align: baseline;
          font-size: 14px;
          padding: 4px 0;
          transform: translateY(2px);
          transition: all .3s;
          cursor: pointer;
          color: #606266;
        }
      }
    }
  }
</style>