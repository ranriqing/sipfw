<template>
  <div>
    <a-tree-select
      style="width: 100%;"
      :disabled="disabled"
      :allowClear="allowClear"
      :placeholder="placeholder"
      :value="cptValue"
      :treeData="cptTreeOptions"
      :treeDefaultExpandAll="true"
      :dropdownStyle="{ maxHeight: '400px', overflow: 'auto' }"
      @change="onChange($event)">
    </a-tree-select>
  </div>
</template>

<!--
# 组件说明
  树形选择组件，可以按下拉树形选择；
  eg: [
        {
          title: 'Node1',
          value: '0-0',
          key: '0-0',
          children: [
            {
              value: '0-0-1',
              key: '0-0-1',
              title: 'Child Node1',
            },
          ],
        },
      ]
# 配置组件
  value: [String, Number]   // 当前选中条目
  disabled: Boolean         // 禁用状态，默认false
  placeholder: String       // 占位字符串
  treeOptions: Array        // 树形选项
  allowClear: Boolean       // 允许清空
# 操作方法
  $emit('change', value)
-->

<script>
  import ATreeSelect from 'ant-design-vue/lib/tree-select';

  export default {
    name: 'GTreeSelect',
    components: {
      ATreeSelect,
    },
    /* eslint-disable */
    props: {
      value: {
        type: [String, Number],
        required: false
      },
      disabled: {
        type: Boolean,
        default: false
      },
      placeholder: {
        type: String,
        default: '请输入'
      },
      /** 扩展Props */
      treeOptions: {
        type: Array,
        required: true
      },
      allowClear: {
        type: Boolean,
        default: true
      },
    },
    computed: {
      cptValue: function(){
        let that = this
        return that._.isNil(that.value)?undefined:that._.toString(that.value)
      },
      cptTreeOptions: function(){
        return this._convertTreeOptions(this.treeOptions)
      }
    },
    methods: {
      onChange (value) {
        let that = this
        that.$emit('change', value)
      },
      /** 扩展方法 */
      _convertTreeOptions(treeOptions){
        let that = this
        let optionsArray = []
        treeOptions.forEach(item =>{
          let convertedObj = {
            key: item['key'],
            title: item['title'],
            value: that._.toString(item['value']),
          }
          if(item['children']){
            convertedObj['children'] = that._convertTreeOptions(item['children'])
          }
          optionsArray.push(convertedObj)
        })
        return optionsArray
      },
    },
  }
</script>
