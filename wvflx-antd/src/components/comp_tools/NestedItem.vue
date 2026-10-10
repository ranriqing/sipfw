<template>
  <div v-if="item.id">
    <div
      :style="itemStyle"
      :class="{item: true, prodline: item.selectable===false, active: _.get(activeItem, 'id') === item.id}"
      @click="item.selectable===false?undefined:toggleActive(item)">
      <a-icon v-if="cptLevel===1" :type="item.icon || 'question'"></a-icon> {{ item.title }}
      <span v-if="!_.isNil(item.count)" style="color: #aaaaaa; margin-left: 5px; font-style: italic;">{{ item.count }}</span>
      <div class="ops-btn" v-if="item.id>0 && editEnabled">
        <template v-if="cptLevel<maxDepth">
          <a-icon type="plus" @click.stop="onItemAdd(item)"/>
          <a-divider type="vertical" style="margin: 0 4px;"/>
        </template>
        <a-icon type="edit" @click.stop="onItemEdit(item, pItem)"/>
        <template v-if="!item.children || item.children.length===0">
          <a-divider type="vertical" style="margin: 0 4px;" />
          <a-popconfirm title="确定删除吗?" @confirm="()=>onItemDelete(item)">
            <a-icon type="close"/>
          </a-popconfirm>
        </template>
      </div>
    </div>
    <draggable
      tag="div"
      :style="{'margin-left': size==='small'?'25px':'40px'}"
      class="item-container item-sub-container"
      v-bind="cptDragOptions"
      :list="item.children||[]"
      @change="onDragChange(cptLevel, item, $event)">
      <div class="item-group" :key="child.id" v-for="child in (item.children||[])">
        <NestedItem
          :item="child"
          :pItem="item"
          :pLevel="cptLevel"
          :size="size"
          :maxDepth="maxDepth"
          :levelDrag="levelDrag"
          :dragEnabled="dragEnabled"
          :editEnabled="editEnabled"
          :activeItem="activeItem"
          :itemStyle="itemStyle">
        </NestedItem>
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
  itemStyle: Object  // 元素样式，见NestedItem
# 操作方法
  暂无
-->

<script>
import NestedItem from './NestedItem'

export default {
  name: "NestedItem",
  components:{
    NestedItem,
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
    size: {
      type: String,
      default: undefined,
    },
    pLevel: {
      type: Number,
      required: true,
    },
    dragEnabled: {
      type: Boolean,
      default: true,
    },
    editEnabled: {
      type: Boolean,
      default: true,
    },
    levelDrag: {
      type: Boolean,
      required: true,
    },
    maxDepth: {
      type: Number,
      required: true,
    },
    itemStyle: {
      type: Object,
      required: true,
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
    &.prodline{
      // border: 1px solid rgba(135, 143, 171, 0.8);
      // background-color: rgba(135, 143, 171, 0.15);
      background: linear-gradient(90deg, rgba(135, 143, 171, 0.2), rgba(135, 143, 171, 0.1));
      border-radius: 0 20px 20px 0;
      &:hover{
        background: linear-gradient(90deg, rgba(135, 143, 171, 0.2), rgba(135, 143, 171, 0.1));
      }
    }
    // 选中状态
    &.active{
      background-color: #f6f6f6;
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
