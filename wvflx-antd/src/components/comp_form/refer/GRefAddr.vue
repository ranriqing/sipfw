<template>
  <div>
    <div v-if="readonly">
      {{ cptValueOptions.map(item => item.label).join(' / ') }}
    </div>
    <a-cascader
      v-else
      style="width: 100%;"
      :options="optionsTree"
      :disabled="disabled"
      :show-search="{ filter }"
      :allowClear="_.defaultTo(_.get(item,'extend.allowClear'), true)"
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
  extend.allowClear: Boolean       // 允许为空，默认true
# 操作方法
  $emit('change', value)
-->

<script>
import ACascader from 'ant-design-vue/lib/cascader';
import { stores } from '@/utils/util'
import {CACHE} from "@/constants";
import {publicAsset} from "@/api/api";

export default {
  name: 'GRefAddr',
  components: {
    ACascader,
  },
  /* eslint-disable */
  props: {
    item: {
      type: Object,
      required: true
    },
    value: {
      type: [Array],
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
      default: '请选择地址'
    },
  },
  data() {
    return {
      stores: stores,
      realValue: undefined,
      optionsTree: [],
    }
  },
  watch: {
    // [Watch] 回显记录
    value: {
      immediate: true,
      handler(newValue) {
        let that = this
        if (!newValue) {
          that.realValue = undefined
        } else if (that._.isArray(newValue)) {
          let noneNil = newValue.find(item => !that._.isNil(item))
          that.realValue = noneNil?newValue:undefined
        }
      }
    }
  },
  computed: {
    cptValueOptions: function() {
      let that = this
      if (!that.readonly || !that.value || !that.value.length) {
        return []
      }
      return that._findNodesInTree(that.value, that.optionsTree)
    }
  },
  methods: {
    onChange(valueArray) {
      let that = this
      let valueObjArray = []
      if (that._.isArray(valueArray)) {
        valueObjArray = that._findNodesInTree(valueArray, that.optionsTree);
      }
      this.$emit('change', valueArray, valueObjArray)
    },
    filter(inputValue, path) {
      return path.some(option => option.label.toLowerCase().indexOf(inputValue.toLowerCase()) > -1);
    },
    /** 工具方法 */
    _buildAddrTree(allNodes, levelNodes) {
      const that = this;
      return levelNodes.map(pNode => {
        // 创建新对象，仅包含必要属性
        const cleanedNode = {
          label: pNode.name,
          value: pNode.adcode,
          isLeaf: pNode.adcode.toString().slice(-2) !== '00'
        };
        const children = allNodes.filter(item => item['parent'] === pNode['adcode']);
        if (children.length > 0) {
          cleanedNode.children = that._buildAddrTree(allNodes, children);
        }
        return cleanedNode;
      });
    },
    _findNodesInTree(valueArray, optionsTree) {
      if (!Array.isArray(valueArray) || !valueArray.length) {
        return [];
      }
      // 递归查找函数
      const result = [];
      const remainingValues = [...valueArray];
      const recurseSearch = (nodes, values) => {
        if (!nodes || !nodes.length || !values.length) return;
        const currentValue = values[0];
        const foundNode = nodes.find(node => node.value === currentValue);
        if (foundNode) {
          result.push(foundNode);
          recurseSearch(foundNode.children, values.slice(1));
        }
      };
      recurseSearch(optionsTree, remainingValues);
      return result;
    },
  },
  mounted() {
    let that = this
    let optionsTree = stores.getCacheItem(CACHE.ADDR_TREE_NODES)
    if(optionsTree && optionsTree.length>0) {
      that.optionsTree = optionsTree
    }else{
      publicAsset('/addrs/all.json').then(resp => {
        let that = this
        let allNodes = resp.data
        let rootNodes = allNodes.filter(item => item.parent===100000)
        optionsTree = that._buildAddrTree(allNodes, rootNodes)
        if(optionsTree && optionsTree.length>0) {
          that.optionsTree = optionsTree
        }
        stores.setCacheItem(CACHE.ADDR_TREE_NODES, optionsTree)
      })
    }
  }
}
</script>
