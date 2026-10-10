export default {
  /** Obj操作 */
  parseObj: function(str){
    if(typeof str === 'object') return str || {}
    let strObj = str?JSON.parse(str):{}
    return strObj
  },
  encodeObj: function(obj){
    return JSON.stringify(obj)
  },
  /** Array操作 */
  parseArray: function(str){
    if(Array.isArray(str)) return str
    let strArray = str?JSON.parse(str):[]
    return strArray
  },
  encodeArray: function(arrayObj){
    return this.encodeObj(arrayObj)
  },
  /** Options操作 */
  parseOptions: function(str){
    return this.objToOptions(this.parseObj(str))
  },
  encodeOptions: function(options){
    return this.encodeObj(this.optionsToObj(options))
  },
  /** 转换操作 */
  objToOptions: function(obj){
    obj = obj || {}
    let targetArray = []
    for(let key in obj){
      targetArray.push({
        label: key,
        value: obj[key],
      })
    }
    return targetArray
  },
  optionsToObj: function(options){
    let optionObj = {}
    options = options || []
    options.forEach(item =>{
      if(item.label && item.value){
        optionObj[item.label] = item.value
      }
    })
    return optionObj
  },
  reverseObj: function(obj){
    let targetObj = {}
    for(let key in obj){
      let value = obj[key]
      targetObj[value] = key
    }
    return targetObj
  },
}