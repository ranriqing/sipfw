<template>
  <div v-if="item.id">
    <div
      :title="item.id"
      :style="itemStyle"
      :class="{item: true, active: _.get(activeItem, 'id') === item.id}"
      @click="toggleActive(item)">
      <a-icon v-if="cptLevel===1" :title="item.id" :type="item.icon || 'question'" style="margin-right: 8px;"></a-icon>
      <a-dropdown class="column-item">
        <a-tag v-if="!item.tagStyle" class="sc-tag" style="border: none;">
          {{ item.title }}
        </a-tag>
        <a-tag v-else-if="item.tagStyle==='default'" class="sc-tag" style="border: none;">
          {{ item.title }}
        </a-tag>
        <a-tag v-else :color="item.tagStyle" class="sc-tag">
          {{ item.title }}
        </a-tag>
        <a-menu v-if="allowModify" slot="overlay" @click="(event) => onMenuClick(item, event)">
          <a-menu-item v-for="(styleItem) in tagStyles" :key="styleItem.caption">
            <a-tag v-if="styleItem.color==='default'" style="border: none;">{{ styleItem.caption }}</a-tag>
            <a-tag v-else :color="styleItem.color">{{ styleItem.caption }}</a-tag>
          </a-menu-item>
        </a-menu>
      </a-dropdown>
      <span v-if="!_.isNil(item.count)" style="color: #aaaaaa; margin-left: 5px; font-style: italic;">{{ item.count }}</span>
      <div class="ops-btn" v-if="item.id>0">
        <template v-if="allowModify">
          <template v-if="cptLevel<maxDepth && multiLevel">
            <a-icon type="plus" @click.stop="onItemAdd(item)"/>
            <a-divider type="vertical" style="margin: 0 4px;"/>
          </template>
          <a-icon type="edit" @click.stop="onItemEdit(item, pItem)"/>
          <template v-if="allowRemove && (!item.children || item.children.length===0)">
            <a-divider type="vertical" style="margin: 0 4px;" />
            <a-popconfirm title="确定删除吗?" @confirm="()=>onItemDelete(item)">
              <a-icon type="close"/>
            </a-popconfirm>
          </template>
        </template>
        <div v-else style="min-width: 4px;">&nbsp;</div>
      </div>
    </div>
    <draggable
      tag="div"
      class="item-container item-sub-container"
      v-bind="cptDragOptions"
      :list="item.children||[]"
      @change="onDragChange(cptLevel, item, $event)">
      <div class="item-group" :key="child.id" v-for="child in (item.children||[])">
        <ScelItem
          :item="child"
          :pItem="item"
          :pLevel="cptLevel"
          :maxDepth="maxDepth"
          :levelDrag="levelDrag"
          :allowModify="allowModify"
          :multiLevel="multiLevel"
          :activeItem="activeItem"
          :itemStyle="itemStyle"
          :tagStyles="tagStyles">
        </ScelItem>
      </div>
    </draggable>
  </div>
</template>

<!--
# 组件说明
  可拖拽树条目，用于构造可拖拽树；
  只有顶层展示图标，默认图标为question；
  如果配置了条目数量，则展示出数量，eg: 部门成员数量；
  对于id>0的条目可以新增(未超层级限制)、编辑、删除(无子元素)；
# 组件图标
  <a-icon type="question" />
# 配置组件
  item: Object       // 当前组件
  pItem: Object      // 父组件
  activeItem: Object // 激活组件
  pLevel: Number     // 父组件级别
  dragEnabled: true  // 是否开启拖拽，默认true
  levelDrag: true    // 开启跨层次拖拽，默认false
  maxDepth: Number   // 最大层次深度，默认100
  itemStyle: Object  // 元素样式，见ScelItem
# 操作方法
  暂无
-->

<script>
  import ScelItem from './ScelItem'
  import ScElement from '@/store/models/ScElement'

  export default {
    name: "ScelItem",
    components:{
      ScelItem,
    },
    props: {
      item: {
        type: Object,
        required: true,
      },
      pItem: {
        type: Object,
        default: function(){
          return null
        }
      },
      activeItem: {
        type: Object,
        required: true,
      },
      pLevel: {
        type: Number,
        required: true,
      },
      dragEnabled: {
        type: Boolean,
        default: true,
      },
      levelDrag: {
        type: Boolean,
        required: true,
      },
      allowRemove: {
        type: Boolean,
        default: false,
      },
      allowModify: {
        type: Boolean,
        required: true,
      },
      multiLevel: {
        type: Boolean,
        required: false,
      },
      maxDepth: {
        type: Number,
        required: true,
      },
      itemStyle: {
        type: Object,
        required: true,
      },
      tagStyles: {
        type: Array,
        default: function(){
          return []
        }
      },
    },
    inject: [
      'toggleActive',
      'onItemAdd',
      'onItemEdit',
      'onItemDelete',
      'onDragChange'
    ],
    computed: {
      cptLevel() {
        let that = this
        return that.pLevel+1
      },
      cptDragOptions() {
        let that = this
        return {
          animation: 0,
          group: `nestedTree${that.levelDrag?'':that.cptLevel}`,
          disabled: !this.dragEnabled,
          ghostClass: "ghost",
        };
      },
    },
    methods: {
      /** 扩展方法 */
      onMenuClick(elItem, event) {
        let that = this
        let styleObj = that.tagStyles.find( styleItem => styleItem.caption === event.key)
        if(styleObj){
          ScElement.$update({
            params: {
              id: elItem.id,
            },
            data: {
              tagStyle: styleObj['color'],
            }
          })
        }
      },
    }
  };
</script>

<style lang="scss" scoped>
  .item-container {
    margin: 0;
    /* Item子容器 */
    .item-sub-container {
      margin: 0 0 0 40px;
    }
    /* 拖拽Ghost */
    .ghost{
      border-top: 1px dashed #1890ff;
    }
    /* Item元素 */
    .item {
      cursor: default;
      padding: 8px;
      background-color: #fefefe;
      position: relative;
      // 选中状态
      &.active{
        background-color: rgba(246,255,237,0.4) !important;
        .ops-btn{
          display: inline-block;
        }
      }
      // 悬停状态
      &:hover{
        background-color: #e6f7ff;
        .ops-btn{
          display: inline-block;
        }
      }
      // 操作按钮
      .ops-btn{
        display: none;
        float: right;
        color: gray;
        padding-right: 5px;
        border-right: 3px solid #1890ff;
        position: absolute;
        right: 5px;
        .anticon{
          cursor: pointer;
          font-size: 14px;
          padding: 4px;
          &:hover{
            color: #1f1f1f;
          }
        }
      }
    }
  }
</style>
