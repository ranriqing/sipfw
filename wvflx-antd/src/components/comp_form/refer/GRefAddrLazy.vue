<template>
  <div>
    <!-- 禁用状态下直接显示文本 -->
    <div v-if="disabled">
      {{ cptValueOptions.map(item => item.label).join(' / ') }}
    </div>
    <!-- 非禁用状态显示级联选择器 -->
    <a-cascader
      v-else
      style="width: 100%;"
      :options="cptTreeNodes"
      :loadData="cascaderLoadData"
      :disabled="disabled"
      :show-search="{ filter }"
      :allowClear="allowClear"
      :placeholder="placeholder"
      :changeOnSelect="true"
      v-model="realValue"
      @change="onChange"
    ></a-cascader>
  </div>
</template>

<!--
# 组件说明
  级联选择地址组件，按选中内容级联加载省市区；
# 配置组件
  value: [Array]            // 选中的地址
  disabled: Boolean         // 禁用状态，默认false
  placeholder: String       // 占位字符串
  allowClear: Boolean       // 允许为空，默认true
# 操作方法
  $emit('change', value)
-->

<script>
import ACascader from 'ant-design-vue/lib/cascader';
import {getAction} from '@/api/api'

export default {
  name: 'GRefAddr',
  components: {
    ACascader,
  },
  /* eslint-disable */
  props: {
    value: {
      type: [Array],
      required: false
    },
    disabled: {
      type: Boolean,
      default: false
    },
    placeholder: {
      type: String,
      default: '请选择地址'
    },
    /** 扩展Props */
    allowClear: {
      type: Boolean,
      default: true
    },
  },
  data() {
    return {
      realValue: [],
      loadedNodeIds: [],
      optionsDataSource: [],
    }
  },
  watch: {
    // [Watch] 回显记录
    value: {
      immediate: true,
      handler(newValue) {
        let that = this
        that._loadData(0)  // 将0作为根节点缓存标记
        if (!newValue) {
          that.realValue = []
        } else if (that._.isArray(newValue)) {
          that.realValue = newValue
          for (let index = 0; index < newValue.length; index++) {
            if (index <= 1) {  // 0省\1市\2区，加载省市子数据
              that._loadData(newValue[index])
            }
          }
        }
      }
    }
  },
  computed: {
    cptTreeNodes: function () {
      let that = this
      let rootNodes = that.optionsDataSource.filter(item => that._.isNil(item.pId))
      that._buildNodeTree(rootNodes)
      return rootNodes
    },
    cptValueOptions: function() {
      let that = this
      if (!that.disabled || !that.value || !that.value.length) {
        return []
      }
      let optionNodes = []
      let currentNodes = that.cptTreeNodes
      let remainingValues = [...this.value]
      // 逐级查找匹配的节点
      while (remainingValues.length > 0 && currentNodes.length > 0) {
        const currentValue = remainingValues[0]
        const foundNode = currentNodes.find(node => node.value === currentValue)
        if (foundNode) {
          optionNodes.push(foundNode)
          remainingValues.shift()
          currentNodes = foundNode.children || []
        } else {
          break
        }
      }
      return optionNodes
    }
  },
  methods: {
    onChange(valueArray) {
      let that = this
      let valueObjArray = []
      if (that._.isArray(valueArray)) {
        valueObjArray = valueArray.map(value => that.optionsDataSource.find(item => item.value === value) || {})
      }
      this.$emit('change', valueArray, valueObjArray)
    },
    filter(inputValue, path) {
      return path.some(option => option.label.toLowerCase().indexOf(inputValue.toLowerCase()) > -1);
    },
    /** 查询数据 */
    cascaderLoadData(selectedOptions) {
      let that = this
      let targetOption = selectedOptions ? selectedOptions[selectedOptions.length - 1] : {};
      that._loadData(targetOption['value'] || 0)
    },
    _loadData(nodeId, targetOption) {
      let that = this
      let loadedIndex = that.loadedNodeIds.indexOf(nodeId)
      if (loadedIndex !== -1) {
        return
      } else {
        that.loadedNodeIds.push(nodeId)
      }
      that.loading = true
      if (targetOption) {
        targetOption.loading = true
      }
      let promise = getAction('/addr/nodes', {id: nodeId ? nodeId : undefined})
      promise.then(resp => {
        that.loading = false
        if (targetOption) {
          targetOption.loading = false
        }
        if (resp.gmodel) {
          resp.gmodel.forEach(record => {
            let recordIndex = that.optionsDataSource.findIndex(item => item.value === record['value'])
            if (recordIndex === -1) {
              that.optionsDataSource.push({
                label: record['name'],
                value: record['id'],
                isLeaf: record['id'].toString().slice(-2) !== '00',
                pId: record['pId'],
              })
            }
          })
        }
      }).catch(err => {
        err || true
        that.loading = false
      })
      return promise
    },
    /** 工具方法 */
    _buildNodeTree(nodes) {
      let that = this
      nodes.forEach(function (pNode) {
        let children = that.optionsDataSource.filter(item => item['pId'] === pNode['value'])
        if (children.length > 0) {
          pNode.children = children
          that._buildNodeTree(children)
        }
      })
    },
  },
  mounted() {
    this._loadData(0)
  }
}
</script>
