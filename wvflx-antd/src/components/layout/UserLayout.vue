<template>
  <div :class="{'login-layout-wrapper': true, 'mobile': isMobile }">
    <div class="container">
      <div class="header">
        <div class="logo">
          <a href="/">
            <img :src="tools.avatarUrl(stores.globalConfig(GLOBAL.LOGO_IMAGE))" class="logo" alt="logo" />
            <span class="title">
              {{ stores.globalConfig(GLOBAL.LOGIN_TITLE) }}
            </span>
          </a>
        </div>
        <div class="desc">
          {{ stores.globalConfig(GLOBAL.LOGIN_DESCRIPTION) }}
        </div>
      </div>
      <div class="content">
        <RouteView></RouteView>
      </div>
      <div class="footer">
        <div class="copyright">
          {{ stores.globalConfig(GLOBAL.COPYRIGHT) }}
        </div>
      </div>
    </div>
  </div>
</template>

<!--
# 组件说明
  用户Layout组件，用于登录与注册页面；
  上方为Logo+文字头部，中间RouteView内容部分，下方版权部分;
# 配置组件
  暂无
# 操作方法
  暂无
-->

<script>
  import { GLOBAL } from '@/constants'
  import { mapGetters } from "vuex";
  import { GETTERS } from '@/constants'
  import { tools, stores } from '@/utils/util'

  import RouteView from "@/components/layout/tools/RouteView"

  export default {
    name: "UserLayout",
    components: {
      RouteView
    },
    data () {
      return {
        /** 配置&状态 */
        GLOBAL: GLOBAL,
        stores: stores,
        tools: tools,
      }
    },
    computed: {
      ...mapGetters({
        /** App state */
        isMobile: GETTERS.CPT_APP_DEVICE_MOBILE,
      }),
    },
  }
</script>

<style lang="scss" scoped>
  .login-layout-wrapper {
    height: 100%;
    // Container样式
    .container {
      width: 100%;
      min-height: 100vh;
      background: #f0f2f5 url(~@/assets/images/background.svg) no-repeat 50%;
      background-size: 100%;
      padding: 110px 0;
      position: relative;
      a {
        text-decoration: none;
      }
      /* Header样式 */
      .header {
        text-align: center;
        .logo {
          height: 56px;
          border-radius: 50%;
          img {
            vertical-align: top;
            margin-right: 15px;
          }
          .title {
            top: 2px;
            font-size: 33px;
            color: rgba(0, 0, 0, .85);
            font-weight: 600;
          }
        }
        .desc {
          font-size: 14px;
          color: rgba(0, 0, 0, 0.45);
          margin-top: 12px;
          margin-bottom: 16px;
        }
      }
      /* Content样式 */
      .content {
        min-width: 260px;
        width: 368px;
        margin: 0 auto;
      }
      /* Footer样式 */
      .footer {
        width: 100%;
        padding: 16px 0;
        text-align: center;
        .copyright {
          color: rgba(0, 0, 0, 0.45);
          font-size: 14px;
        }
      }
    }
  }
  // 定义patch-mobile样式
  .login-layout-wrapper{
    &.mobile {
      .container {
        padding: 30px 0;
        .content {
          max-width: 368px;
          width: 90%;
        }
      }
    }
  }
</style>