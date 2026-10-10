<template>
  <div>
    <div v-if="readonly">
      <ul style="height: unset;">
        <li class="current-icon" >
          <a-icon v-if="value" :type="value" />
          <span v-else></span>
        </li>
      </ul>
    </div>
    <a-tabs
      v-else
      tabPosition="top"
      :active-key="activeTab"
      @change="onTabClick">
      <a-tab-pane
        v-for="(iconItem) in tabIcons"
        :tab="iconItem.title"
        :key="iconItem.key">
        <ul>
          <!-- 当前图标 -->
          <li class="current-icon" >
            <a-icon v-if="value" :type="value" />
            <span v-else></span>
          </li>
          <!-- 图标列表 -->
          <li
            v-for="(iconType, key) in iconItem.icons"
            :key="`${iconItem.key}-${key}`"
            :class="{'active': value === iconType}">
            <a-icon :title="iconType" :type="iconType" @click="onChange(iconType)" />
          </li>
        </ul>
      </a-tab-pane>
    </a-tabs>
  </div>
</template>

<!--
# 组件说明
  图标选择组件，支持从图标列表中选择图标;
# 配置组件
  value: String              // 图标字符串
# 操作方法
  $emit('change', value)
-->

<script>
  import ATabs from 'ant-design-vue/lib/tabs';
  import tabIcons from './gIconSelector/tabIcons'
  
  export default {
    name: 'GIconSelector',
    components: {
      ATabs,
      ATabPane: ATabs.TabPane,
    },
    /* eslint-disable */
    props: {
      item: {
        type: Object,
        required: true
      },
      value: {
        type: String,
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
        default: ''
      },
    },
    data () {
      return {
        activeTab: 'tab1',
        tabIcons: tabIcons,
      }
    },
    methods: {
      onChange (value) {
        this.$emit('change', value)
      },
      /** 扩展方法 */
      onTabClick (activeKey) {
        this.activeTab = activeKey
      },
    },
  }
</script>

<style lang="scss" scoped>
  /* 图标展示样式*/
  ul{
    list-style: none;
    padding: 0;
    overflow-y: auto;
    height: 125px;
    // 图标样式
    li{
      display: inline-block;
      padding:5px;
      margin:5px;
      .anticon{
        font-size: 20px;
      }
      &:hover {
        cursor: pointer;
        color: #1f1f1f;
      }
      &.active{
        box-shadow: 0 0 3px 1px grey;
      }
      // 当前图标
      &.current-icon {
        color: #13c2c2;
        cursor: default;
        span{
          min-width: 18px;
          min-height: 18px;
          display: inline-block;
        }
      }
    }
  }
</style>
