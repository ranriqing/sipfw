<template>
  <div>
    <div v-if="readonly">
      {{ stores.levelGcsGroup(cptValue) }}
    </div>
    <a-tree-select
      v-else
      style="width: 100%;"
      treeNodeFilterProp="title"
      treeNodeLabelProp="label"
      :showSearch="true"
      :disabled="disabled"
      :allowClear="_.defaultTo(_.get(item,'extend.allowClear'), true)"
      :placeholder="placeholder"
      :value="cptValue"
      :treeData="treeOptions()"
      :treeDefaultExpandAll="true"
      :dropdownStyle="{ maxHeight: '400px', overflow: 'auto' }"
      @change="onChange($event)">
    </a-tree-select>
  </div>
</template>

<!--
# 组件说明
  选择部门组件，下拉选择部门并展示；
  为提高兼容性，组件会将选项值转为字符串；
  数据从元信息中加载，递归构造部门树结构；
# 配置组件
  value: [String, Number]   // 选中条目
  disabled: Boolean         // 禁用状态，默认false
  placeholder: String       // 占位字符串
  allowClear: Boolean       // 允许为空，默认true
# 操作方法
  $emit('change', value)
-->

<script>
import ATreeSelect from 'ant-design-vue/lib/tree-select';

import { stores } from '@/utils/util'
import { mapGetters } from "vuex";
import { GETTERS } from '@/constants'
import GcsGroup from '@/store/models/GcsGroup'

export default {
  name: 'GRefGroup',
  components: {
    ATreeSelect,
  },
  /* eslint-disable */
  props: {
    item: {
      type: Object,
      required: true
    },
    value: {
      type: [String, Number],
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
      default: '请选择相关组'
    },
  },
  data() {
    return {
      stores: stores
    }
  },
  computed: {
    ...mapGetters({
      currentUser: GETTERS.GET_USER_CURRENT_USER,
    }),
    cptValue: function() {
      let that = this
      return that._.toString(that.value)===''?undefined:that._.toString(that.value)
    },
    cptAdmin: function(){
      return stores.isSuperAdmin()
    },
  },
  methods: {
    onChange (value) {
      this.$emit('change', value)
    },
    /** 扩展方法 */
    treeOptions() {
      let that = this
      let allGroups = GcsGroup.query().orderBy('sequence', 'asc').orderBy('id', 'asc').get()
      let rootGroup = allGroups.find(item => item.id === -1)
      let rootTree = that._buildGroupTree(rootGroup, allGroups, undefined)
      return rootTree ? [rootTree] : []
    },
    /** 工具方法 */
    _buildGroupTree(pGroup, allGroups, pLabel) {
      let that = this
      if (!pGroup) {
        return null
      }
      let treeNode = {
        id: pGroup.id, key: pGroup.id,
        title: pGroup.name, value: that._.toString(pGroup.id),
        label: `${pLabel ? pLabel + '/' : ''}${pGroup.name}`,
      }
      let subGroups = allGroups.filter(item => item.parentId === pGroup.id)
      if(subGroups.length>0){
        treeNode['children'] = subGroups.map(item => {
          return that._buildGroupTree(item, allGroups, treeNode.label)
        })
      }
      return treeNode
    },
  },
}
</script>
