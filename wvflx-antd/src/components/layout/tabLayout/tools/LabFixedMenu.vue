<template>
  <div class="header-dropdown-wrapper">
    <a-dropdown v-model="menuVisible">
      <a class="header-dropdown-link">
        <a-icon type="appstore"></a-icon>
        <span>系统设计</span>
      </a>
      <a-menu 
        slot="overlay" 
        class="header-dropdown-menu" 
        @click="onMenuClick">
        <a-menu-item key="menuEdit">
          <a-icon type="menu"/>
          <span>视图菜单</span>
        </a-menu-item>
      </a-menu>
    </a-dropdown>

    <!-- Slot弹窗 -->
    <DomainModal
      ref="slotDomain"
      @popSubmit="1">
    </DomainModal>
  </div>
</template>

<!--
# 组件说明
  Lab模式配置入口；
  在Lab模式下，表格数据管理入口；
  在Lab模式下，可以配置视图菜单；
# 配置组件
  暂无
# 操作方法
  暂无
-->

<script>
  import DomainModal from '@/components/comp_tools/DomainModal'

  export default {
    name: "LabFixedMenu",
    components: {
      DomainModal,
    },
    data () {
      return {
        menuVisible: false,
      }
    },
    methods: {
      onMenuClick({ key }){
        let that = this
        if('modelDomain' === key){
          that.$refs.slotDomain.showPop()
        }else{
          that.$router.push({ name: key })
        }
        that.menuVisible = false
      },
    },
  }
</script>

<style lang="scss" scoped>
  /* 菜单样式修正 */
  .header-dropdown-wrapper {
    display: inline-block;
    height: 100%;
    .header-dropdown-link {
      padding: 10px 14px;
      height: 100%;
      .anticon{
        margin-right: 6px;
      }
      &:hover {
        background: rgba(255, 255, 255, 0.3);
      }
    }
  }
  .header-dropdown-menu {
    color: #fff;
    padding: 4px 0;
    .ant-dropdown-menu-item {
      width: 190px;
      position: relative;
      // 操作按钮
      .ops-btn {
        display: none;
        float: right;
        color: gray;
        padding-right: 5px;
        position: absolute;
        right: 5px;
        .anticon {
          cursor: pointer;
          font-size: 14px;
          /*padding: 4px;*/
          &:hover{
            color: #1f1f1f;
          }
        }
      }
    }
    .ant-dropdown-menu-item-active{
      .ops-btn{
        display: inline-block;
      }
    }
  }
</style>