<template>
  <!-- 为组件提供统一配置，包裹全局生效 -->
  <a-config-provider :locale="locale">
    <div id="app">
      <router-view />
    </div>
  </a-config-provider>
</template>

<script>
  import enUS from 'ant-design-vue/es/locale/en_US';
  import zhCN from 'ant-design-vue/lib/locale-provider/zh_CN'
  import enquireScreen from '@/utils/device'
  import { mapMutations, mapActions } from 'vuex'
  import { MUTATIONS, ACTIONS } from '@/constants'

  export default {
    data() {
      return {
        locale: zhCN || enUS,
      }
    },
    methods: {
      ...mapMutations({
        setDeviceType: MUTATIONS.SET_APP_DEVICE_TYPE,
      }),
      ...mapActions({
        setSidebarOpened: ACTIONS.SET_APP_SIDEBAR_OPENED,
      }),
    },
    created () {
      let that = this
      that.$event.on("*",(type, payload)=>{
        console.log("$event", type, payload)
      })
      enquireScreen((deviceTypeCode) => {
        let deviceType = 'desktop'
        let sidebarOpened = true        // sidebarOpened:  true | false
        if (deviceTypeCode in [0,1]) {  // deviceTypeCode: 0 tablet | 1 mobile
          deviceType = 'mobile'
          sidebarOpened = false
        }
        that.setDeviceType(deviceType)
        that.setSidebarOpened(sidebarOpened)
      })
    }
  }
</script>

<style lang="scss">
  /* global.css - 全局样式 */
  @import '~@/assets/scss/global.scss';
  /* iconfont.css - 自定义图标 */
  // @import '~@/common/iconfont.css';
  @import url('//at.alicdn.com/t/c/font_3904252_nffv8ywn32i.css');
</style>