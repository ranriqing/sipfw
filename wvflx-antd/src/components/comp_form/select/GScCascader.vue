<template>
  <div>
    <!-- 禁用状态下直接显示文本 -->
    <div v-if="readonly">
      <span
        v-for="(option, index) in cptValueOptions"
        :key="index">
        <a-tag v-if="!option.tagStyle" class="sc-tag" style="border: none;">
          {{ option.title }}
        </a-tag>
        <a-tag v-else-if="option.tagStyle==='default'" class="sc-tag" style="border: none;">
          {{ option.title }}
        </a-tag>
        <a-tag v-else :color="option.tagStyle" class="sc-tag">
          {{ option.title }}
        </a-tag>
      </span>
    </div>
    <a-cascader
      v-else
      style="width: 100%;"
      :options="cptTreeNodes"
      :fieldNames="{label:'label', value:'value',children: 'children'}"
      :disabled="disabled"
      :allowClear="_.defaultTo(_.get(item,'extend.allowClear'), true)"
      :placeholder="placeholder"
      :showSearch="true"
      :changeOnSelect="true"
      :value="cptValue"
      @change="onChange">
      <template #displayRender="{ labels, selectedOptions }">
        <div>
          <span
            v-for="(option, index) in selectedOptions"
            :key="index">
            <a-tag v-if="!option.tagStyle" class="sc-tag" style="border: none;">
              {{ option.title }}
            </a-tag>
            <a-tag v-else-if="option.tagStyle==='default'" class="sc-tag" style="border: none;">
              {{ option.title }}
            </a-tag>
            <a-tag v-else :color="option.tagStyle" class="sc-tag">
              {{ option.title }}
            </a-tag>
            <i v-if="selectedOptions.length-1!==index" style="margin:0 10px 0 2px;">/</i>
          </span>
        </div>
      </template>
    </a-cascader>
  </div>
</template>

<!--
# 组件说明
  级联选择组件，按scKey级联加载选项；
# 配置组件
  value: [Array]            // 选中的选项
  readonly: Boolean         // 是否只读，默认false
  disabled: Boolean         // 禁用状态，默认false
  placeholder: String       // 占位字符串
  extend.scKey: String      // 字典标识
# 操作方法
  $emit('change', value)
-->

<script>
  import ACascader from 'ant-design-vue/lib/cascader';
  import ScElement from '@/store/models/ScElement'

  export default {
    name: 'GScCascader',
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
        default: '请选择'
      },
    },
    data () {
      return {
        optionsDataSource: [],
      }
    },
    computed: {
      cptValue: function() {
        let that = this
        if (!that.value) {
          return undefined
        } else if (that._.isArray(that.value)) {
          let noneNil = that.value.find(item => !that._.isNil(item))
          return noneNil?that.value:undefined
        }
      },
      cptTreeNodes: function(){
        let that = this
        let depth = that._.defaultTo(that._.get(that.item, 'extend.depth'), 1)
        let scKey = that._.defaultTo(that._.get(that.item, 'extend.scKey'), 'none')
        let allElements = ScElement.query()
          .where(item => item.scConfigKey === scKey)
          .orderBy('seqOrder').orderBy('id').get()
        // 获取root节点
        let rootElements = []
        allElements.forEach(function(elem) {
          if(elem.parentId === 0){
            rootElements.push({
              id: elem['id'],
              pId: elem['parentId'],
              key: `elem${elem['id']}`,
              title: elem['title'],
              tagStyle: elem['tagStyle'],
              remark: elem['description'],
              value: elem['id'],
              label: elem['title'],
            })
          }
        })
        that._buildNodeTree(rootElements, allElements, 1, depth)
        return rootElements
      },
      cptValueOptions: function() {
        let that = this
        if (!that.value || !that.value.length) {
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
      onChange(value) {
        this.$emit('change', value)
      },
      /** 工具方法 */
      _buildNodeTree(elements, allElements, currentDepth = 1, maxDepth = 3) {
        let that = this
        // 如果当前深度超过最大限制，直接返回不处理
        if (currentDepth >= maxDepth) {
          return;
        }
        elements.forEach(function(pElem) {
          let children = []
          allElements.forEach(function(elem) {
            if(elem.parentId === pElem.id){
              children.push({
                id: elem['id'],
                pId: elem['parentId'],
                key: `elem${elem['id']}`,
                title: elem['title'],
                tagStyle: elem['tagStyle'],
                remark: elem['description'],
                value: elem['id'],
                label: elem['title'],
              })
            }
          })
          // NOTE: 全部都有children字段
          pElem.children = children.length>0?children:[]
          // 只有当还有子节点且未达到最大深度时，才继续递归
          if (children.length > 0 && currentDepth < maxDepth) {
            that._buildNodeTree(children, allElements, currentDepth + 1, maxDepth);
          }
        })
      },
    }
  }
</script>

<style lang="scss" scoped>
  /deep/ .ant-cascader-picker-label{
    height: 24px;
    margin-top: -11px;
  }
</style>