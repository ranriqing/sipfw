<template>
  <div class="logo">
    <router-link :to="{ name:'root' }">
      <img :src="tools.avatarUrl(stores.globalConfig(GLOBAL.LOGO_IMAGE))" alt="logo" />
      <h1 v-if="showTitle">
        {{ stores.globalConfig(GLOBAL.LOGO_TITLE, title) }}
      </h1>
    </router-link>
  </div>
</template>

<!--
# 组件说明
  Logo组件，展示Logo，点击可跳转首页；
  加载后端的avatar头像来当做Logo，可以自定义配置；
# 配置组件
  title: String        // Logo标题，默认GCS-PRO
  showTitle: Boolean   // 展示标题，默认true
# 操作方法
  暂无
-->

<script>
  import { GLOBAL } from '@/constants'
  import { tools, stores } from '@/utils/util'

  export default {
    name: 'Logo',
    props: {
      title: {
        type: String,
        default: 'GCS-FLX',
      },
      showTitle: {
        type: Boolean,
        default: true,
      }
    },
    data(){
      return {
        /** 配置&状态 */
        GLOBAL: GLOBAL,
        tools: tools,
        stores: stores,
      }
    },
  }
</script>

<style lang="scss" scoped>
  $height: 60px;
  .logo {
    height: $height;
    background-color: #002140;
    a {
      display: flex;
      // justify-content: center;
      align-items: center;
      height: $height;
      color: white;
      overflow: hidden;
      white-space: nowrap;
      img {
        margin-left: 14px;
        height: 40px;
        border-radius: 50%;
      }
      h1 {
        color: #f3f3f3;
        font-size: 18px;
        font-weight: 600;
        text-wrap:none;
        margin: 0 0 0 8px;
        display: inline-block;
      }
      &:hover {
        h1{
          color: #fafafa;
        }
      }
    }
  }
</style>