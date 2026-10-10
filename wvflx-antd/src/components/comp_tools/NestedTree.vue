<template>
  <div class="nested-tree">
    <a-alert v-if="dragEnabled" message="拖拽元素来排序" type="success" style="margin-bottom: 6px;"/>
    <draggable
      tag="div"
      class="item-container"
      v-bind="cptDragOptions"
      :list="itemList"
      @change="onDragChange(0, null, $event)">
      <div class="item-group" :key="item.id" v-for="item in itemList">
        <NestedItem
          :item="item"
          :pItem="null"
          :pLevel="0"
          :size="size"
          :maxDepth="maxDepth"
          :dragEnabled="dragEnabled"
          :editEnabled="editEnabled"
          :levelDrag="levelDrag"
          :activeItem="activeItem"
          :itemStyle="itemStyle">
        </NestedItem>
      </div>
    </draggable>
  </div>
</template>

<!--
# 组件说明
  多层可拖拽树组件，拖拽构造树形结构；
  可配置是否开启拖拽、开启跨层拖拽、最大层次深度、元素样式、树结构列表等；
  eg: [{
    id: -1, pId: null, title: 公司, icon: home: sequence: 1, count: 100,
    children: [
      {id: 1, pId: -1, title: 销售部, sequence: 1, count: 80,}
      id: 2, pId: -1, title: 财务部, sequence: 2, count: 20,
    ]
  }]
# 配置组件
  dragEnabled: true  // 是否开启拖拽，默认true
  levelDrag: true    // 开启跨层次拖拽，默认false
  maxDepth: Number   // 最大层次深度，默认100
  itemStyle: Object  // 元素样式，见NestedItem
  itemList:[{ }]     // 树状数据列表
# 操作方法
  暂无
-->

<script>
import AAlert from 'ant-design-vue/lib/alert';
import NestedItem from './NestedItem'

export default {
  name: "NestedTree",
  components:{
    AAlert,
    NestedItem,
  },
  props: {
    itemList: {
      type: Array,
      default: function() {
        return []
      }
    },
    size: {
      type: String,
      default: undefined,
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
      default: false,
    },
    maxDepth: {
      type: Number,
      default: 100,
    },
    itemStyle: {
      type: Object,
      default: function () {
        return {cursor: 'move'}
      },
    },
  },
  data() {
    return {
      activeItem: {}
    }
  },
  computed: {
    /** Cpt选项 */
    cptDragOptions() {
      return {
        animation: 0,
        group: "nestedTree",
        disabled: !this.dragEnabled,
        ghostClass: "ghost",
      };
    },
  },
  provide: function () {
    let that = this
    return {
      toggleActive: that.toggleActive,
      onItemAdd: that.onItemAdd,
      onItemEdit: that.onItemEdit,
      onItemDelete: that.onItemDelete,
      onDragChange: that.onDragChange,
    }
  },
  methods: {
    /** Item激活 */
    toggleActive: function (item) {
      let that = this
      that.activeItem = (that.activeItem?.id === item?.id) ? {} : item
      that.$emit('toggleActive', that.activeItem);
    },
    /** Item操作 */
    onItemAdd(item) {
      this.$emit('addItem', item);
    },
    onItemEdit(item, pItem) {
      this.$emit('editItem', item, pItem);
    },
    onItemDelete(item) {
      this.$emit('deleteItem', item);
    },
    onDragChange(level, pItem, changeEvent) {
      let that = this
      let cItem = null
      let newIndex = null
      if (changeEvent['moved']) {
        newIndex = that._.get(changeEvent, 'moved.newIndex') // moved.oldIndex
        cItem = that._.get(changeEvent, 'moved.element')
      }
      if (changeEvent['added']) {
        newIndex = that._.get(changeEvent, 'added.newIndex')
        cItem = that._.get(changeEvent, 'added.element')
      }
      if (cItem && !that._.isNil(newIndex)) {
        let itemDepth = that._recurseDepth(cItem, 1)
        if (level + itemDepth > that.maxDepth) {
          that.$message.warn(`层级数量不能超过 ${that.maxDepth} 层`)
        } else {
          that.$emit("dragChange", pItem, cItem, newIndex);
        }
      }
    },
    // 工具方法
    _recurseDepth(cItem, itemDepth) {
      let that = this
      let childArray = that._.get(cItem, 'children', [])
      if (childArray.length > 0 && itemDepth < that.maxDepth) {
        let childDepth = 0
        childArray.forEach(childItem => {
          let childItemDepth = that._recurseDepth(childItem, itemDepth)
          childDepth = childItemDepth > childDepth ? childItemDepth : childDepth
        })
        return itemDepth + childDepth
      } else {
        return itemDepth
      }
    }
  },
};
</script>

<style lang="scss" scoped>
/* Tree样式 */
.nested-tree {
  border-radius: 5px;
  margin: 0 0 15px;
  /* 顶层Ghost */
  .ghost {
    border-top: 2px dashed #1890ff;
  }
}
</style>
