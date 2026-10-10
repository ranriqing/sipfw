import DATE from '@/constants/consts-date'

const VARS ={
  // Logo部分
  LOGO_IMAGE: 'logoImg',
  LOGO_TITLE: 'logo',
  // Login部分
  LOGIN_TITLE: 'loginTitle',
  LOGIN_DESCRIPTION: 'loginDesc',
  // Welcome部分
  WELCOME_DESKTOP: 'desktopWelcome',
  // Copyright部分
  COPYRIGHT: 'copyright',
  // API密钥部分
  API_SECRET: 'apiSecret',
  // CallCenter部分
  CC_FEE_RATE: 'ccFeeRate',
  // 日期格式
  DATE_FORMATS :[
    DATE.YYYYMM, DATE.YYYYMMDD,
    DATE.YYYYMMDD_HHMM, DATE.YYYYMMDD_HHMMSS
  ]
}
export default VARS
