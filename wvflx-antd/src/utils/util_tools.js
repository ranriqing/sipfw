import _ from '@/utils/tiny_lodash'
import { toPng } from 'html-to-image';
import { MODE } from '@/constants'
const settings = require('@/settings')
import { downloadAction } from '@/api/api'

let utilObj = {
  /** 系统模式 */
  isDev: function(){
    return process.env.NODE_ENV === 'development'
  },
  isWorkMode: function(mode){
    return settings.APP_WORK_MODE === mode
  },
  isCallCenter: function(){
    return settings.APP_WORK_MODE === MODE.CALL_CENTER
  },
  isApply95: function(){
    return settings.WEB_PATH === '/wvta28'
  },
  /** 对象类型 */
  isString: function(str){
    return typeof str === 'string'
  },
  toLowerCamel: function(str){
    return str ? str[0].toLowerCase() + str.slice(1) : undefined;
  },
  toUpperCamel: function(str){
    return str ? str[0].toUpperCase() + str.slice(1) : undefined;
  },
  /** Sleep函数 */
  sleep: function(ms){
    return new Promise((resolve)=>setTimeout(resolve,ms));
  },
  /** Avatar地址 */
  avatarUrl: function(objOrStr, avatarPath){
    let avatarName = undefined
    if(utilObj.isString(objOrStr)){
      avatarName = objOrStr
    }else{
      avatarName = _.defaultTo(_.get(objOrStr, avatarPath), 'avatar-default.png')
    }
    return `${settings.BASE_URL}/avatar/showAvatar?avatar=${avatarName}`
  },
  authorizedDownload: function(downloadUri, params, rstFileName){
    let reqParams = params || {}
    reqParams['fileName'] = rstFileName
    return downloadAction(downloadUri, params || {}).then(resp => {
      utilObj.triggerBlobDownload(resp, rstFileName);
    })
  },
  triggerBlobDownload: function(resp, rstFileName){
    let blob = new Blob([resp], {type: resp.type})
    let href = window.URL.createObjectURL(blob);
    let downloadElement = document.createElement('a') // 创建元素
    downloadElement.href = href;                      // 绑定路径
    downloadElement.download = rstFileName;           // 文件名称
    document.body.appendChild(downloadElement);       // 添加元素
    downloadElement.click();                          // 点击下载
    document.body.removeChild(downloadElement);       // 移除元素
    window.URL.revokeObjectURL(href);                 // 释放Blob
  },
  /** Html2Png */
  // https://github.com/bubkoo/html-to-image
  // this.$el || this.$el.querySelectorAll('.class-name')[0]
  html2png: function($el){
    if($el){
      let filter = (node) => {
        let exclusionClasses = ['header-action'];
        return !exclusionClasses.some((classname) => node.classList?.contains(classname));
      }
      toPng($el, {quality: 1, pixelRatio: 1, filter: filter, canvasWidth: $el.offsetWidth*2, canvasHeight: $el.offsetHeight*2}).then(function (dataUrl) {
        let fileName = 'chart.png'
        let downloadElement = document.createElement('a') // 创建元素
        downloadElement.href = dataUrl;                   // 绑定路径
        downloadElement.download = fileName;              // 文件名称
        document.body.appendChild(downloadElement);       // 添加元素
        downloadElement.click();                          // 点击下载
        document.body.removeChild(downloadElement);       // 移除元素
        window.URL.revokeObjectURL(dataUrl);              // 释放Blob
      });
    }
  },
  /** RandomUuid */
  randomUuid: function(){
    let d = _.now();
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function (c) {
      let r = (d + _.random(16)) % 16 | 0;
      d = Math.floor(d / 16);
      return (c==='x'?r:(r & 0x3 | 0x8)).toString(16);
    })
  },
  /** Random数值 */
  randomNumber: function(min, max){
    return Math.floor(Math.random() * (min - max) + max);
  },
  /** Random字符串 */
  randomString: function(length, charStr) {
    // 默认长度为1，默认字符集包含数字和小写字母
    if (!length) length = 1;
    let chars = charStr || '0123456789qwertyuioplkjhgfdsazxcvbnm';

    // 分离数字字符和非数字字符，用于最后一位的特殊处理
    const digits = '0123456789';
    const nonDigits = chars.split('').filter(c => !digits.includes(c)).join('');

    // 如果没有非数字字符且需要生成字符串，抛出错误
    if (length > 0 && nonDigits.length === 0) {
      throw new Error('字符集中必须包含非数字字符，以满足最后一位不为数字的要求');
    }

    let str = '';
    for (let i = 0; i < length; i++) {
      let availableChars = chars;
      // 最后一位特殊处理：只能使用非数字字符
      if (i===0 || i===length - 1) {
        availableChars = nonDigits;
      }
      // 生成随机索引并拼接字符
      let num = _.random(0, availableChars.length - 1);
      str += availableChars[num];
    }
    return str;
  },
  /** Template字符串 */
  templateString(tmplStr, data) {
    return tmplStr?tmplStr.replace(/\$\{([\w_\-.]+)\}/g, function(match, key) {
      return _.get(data, key, '')
    }):'';
  },
  /** Resize事件 */
  triggerWindowResizeEvent: function() {
    let event = document.createEvent('HTMLEvents')
    event.initEvent('resize', true, true)
    event.eventType = 'message'
    window.dispatchEvent(event)
  },
  /** Flatten对象 */
  flattenObj(record, seprator, initObj, path) {
    seprator = seprator || '.'
    let pickedRecord = _.pickBy(record || {}, (value, key) =>
      !_.isNil(value) && !key.startsWith('$') && !_.isFunction(value)
    )
    return _.reduce(pickedRecord, function(result, value, key){
      let pathKey = path?(path+seprator+key):key
      if(_.isPlainObject(value)){
        utilObj.flattenObj(value, seprator, result, pathKey)
      }else{
        result[pathKey] = value
      }
      return result
    }, initObj || {})
  },
}
export default utilObj
