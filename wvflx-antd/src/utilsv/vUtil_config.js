import _ from '@/utils/tiny_lodash'
import pageConfigs from '@/pages/pageConfigs'

let utilObj = {
  /** 组件配置 */
  partConfig: function(configKey, defaultValue){
    let configValue = _.get(pageConfigs, configKey)
    return _.defaultTo(configValue, defaultValue)
  },
}

export default utilObj