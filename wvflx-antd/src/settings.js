const VARS ={
  WEB_PATH: '/wvflx',                         // 前端URI
  BASE_URL: '/gcs-flx',                       // 后端URI
  APP_IDENT: 'flx',                           // 应用标识
  APP_LAB_STATE: 'labState',                  // Lab状态变量
  APP_AUTH_TOKEN: 'authToken',                // Auth状态变量
  APP_CACHE_POOL: 'cachePool',                // Cache状态变量
  APP_WORK_MODE: 'zero_code',                 // Mode.ZERO_CODE
  VAR_IMG_URL: '',                            // 暂未使用
  BACKEND_OPTIONS: {                          // 后端配置项
    sipAnalysisEnabled: false,                // Sip监控模块
  },
  DFT_GLOBAL_CONFIG: {                        // 默认全局配置
    logo: 'SIPFW',                            // Global.LOGO_TITLE
    logoImg: 'avatar-default.png',            // Global.LOGO_IMAGE
    loginTitle: 'SIPFW',                      // Global.LOGIN_TITLE
    loginDesc: 'SIP 实时监控',                 // Global.LOGIN_DESCRIPTION
    desktopWelcome: '欢迎使用 SIP 实时监控',    // Global.WELCOME_DESKTOP
    copyright: 'Copyright © 2025 SIPFW',       // Global.COPYRIGHT
    ccFeeRate: '0.1',                          // Global.CC_FEE_RATE
  },
}
// 微信登录（可选）：如需启用，填写你自己的 appid 与回调地址
VARS['WECHAT_APPID'] = ''
VARS['WECHAT_REDIRECT_URI'] = ''
// export default VARS
module.exports = VARS