<template>
  <a-layout-footer style="padding: 0;">
    <div class="footer">
      <div class="copyright">
        {{ stores.globalConfig(GLOBAL.COPYRIGHT) }}
      </div>
    </div>
  </a-layout-footer>
</template>

<!--
# 组件说明
  主体Layout下方组件，当前用来展示版权；
# 配置组件
  暂无
# 操作方法
  暂无
-->

<script>
  import ALayout from 'ant-design-vue/lib/layout';
  import { GLOBAL } from '@/constants'
  import { stores } from '@/utils/util'

  export default {
    name: "MainFooter",
    components: {
      ALayoutFooter: ALayout.Footer,
    },
    data () {
      return {
        stores: stores,
        GLOBAL: GLOBAL,
      }
    },
  }
</script>

<style lang="scss" scoped>
  .footer {
    margin: 24px 0;
    text-align: center;
    .copyright {
      color: rgba(0, 0, 0, .45);
      font-size: 14px;
    }
  }
</style>