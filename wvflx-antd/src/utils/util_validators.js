export default {
  /** 基本验证器 */
  attachmentValidator: function(requiredFiles){
    return (rule,value,callback) => {
      let attachmentFiles = value || []
      let rstRequiredFiles = []
      requiredFiles.forEach(item => {
        let fileType = item['fileType']
        let fileNum = item['fileNum'] || 1
        let typeFileList = attachmentFiles.filter(item => item['fileType'] === fileType)
        if(typeFileList.length >= fileNum){
          // do nothing
        }else{
          rstRequiredFiles.push(item)
        }
      })
      if(rstRequiredFiles.length > 0){
        callback(`请先上传${rstRequiredFiles[0]['fileType']}`);
      }else{
        callback()
      }
    }
  },
  fieldNameValidator: function(rule,value,callback){
    if (!value || new RegExp(/^([a-zA-Z0-9_|])+$/).test(value)){
      callback();
    }else{
      callback("仅支持字母、数字、下划线、竖杠");
    }
  },
  engValidator: function(rule,value,callback){
    if (!value || new RegExp(/^([a-zA-Z0-9_.])+$/).test(value)){
      callback();
    }else{
      callback("仅支持字母、数字、下划线、点");
    }
  },
  /**
   * 流程标识：字母 / 数字 / 下划线，1–64 位。
   *
   * 比 fieldNameValidator 少一个竖线、比 engValidator 少一个点——点不能有：
   * 标识会直接当素材目录名（`{mediaDir}/{flowCode}/media/`），而 ".." 能跳出素材根目录。
   * 规则真源在后端 `CcBotFlowService.formatErrorOf`，这里只为即时提示。
   */
  flowCodeValidator: function(rule,value,callback){
    if (!value || new RegExp(/^([a-zA-Z0-9_]){1,64}$/).test(value)){
      callback();
    }else{
      callback("只能输入字母、数字、下划线（1-64 位）");
    }
  },
  numValidator: function(rule,value,callback){
    if (!value || new RegExp(/^([0-9])+$/).test(value)){
      callback();
    }else{
      callback("请输入数值");
    }
  },
  emailValidator: function(rule,value,callback){
    if (!value || new RegExp(/^([a-zA-Z0-9._-])+@([a-zA-Z0-9_-])+((.[a-zA-Z0-9_-]{2,3}){1,2})$/).test(value)){
      callback();
    }else{
      callback("请输入正确的邮箱格式");
    }
  },
  mobileValidator: function(rule,value,callback){
    if (!value || new RegExp(/^1[0-9]{10}$/).test(value)){
      callback();
    }else{
      callback("请输入正确的手机号码");
    }
  },
  phoneValidator: function(rule,value,callback){
    if (!value || new RegExp(/^([0-9]{3,4}-)?[0-9]{7,8}$/).test(value)){
      callback();
    }else{
      callback("请输入正确的固话号码");
    }
  },
  urlValidator: function(rule,value,callback){
    if (!value || new RegExp(/^http[s]?:\/\/.*/).test(value)){
      callback();
    }else{
      callback("请输入正确的Url地址");
    }
  },
  /** 扩展验证器 */
  startWithValidator: function(str){
    return function(rule,value,callback){
      if (!value || value.toString().startsWith(str)){
        callback();
      }else{
        callback(`请输入以 ${str} 开头的字符串`);
      }
    }
  },
  /** Cc工号验证器 */
  ccJobNumValidator: function(rule,value,callback){
    if (!value || new RegExp(/^([0-9]){3,8}$/).test(value)){
      callback();
    }else{
      callback("请输入3到8位数值");
    }
  },
  ccJobPwdValidator: function(rule,value,callback){
    if (!value || new RegExp(/^([0-9]){3,8}$/).test(value)){
      callback();
    }else{
      callback("请输入3到8位数字密码");
    }
  },
}
