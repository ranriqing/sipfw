import { FIELD, OTHER } from '@/constants'
import {converters} from '@/utils/util'
import _ from '@/utils/tiny_lodash'

import MetaDomain from '@/store/models/MetaDomain'
import MetaDomainField from '@/store/models/MetaDomainField'

let utilObj = {
  /** DOMAIN和字段 */
  metaDomain: function (metaDomainId) {
    return MetaDomain.find(metaDomainId)
  },
  metaDomainField: function (metaDomainFieldId) {
    return MetaDomainField.find(metaDomainFieldId)
  },
  metaDomainFields: function (metaDomainId, fieldType) {
    let fieldQuery = MetaDomainField.query()
    if(metaDomainId){ fieldQuery.where('metaDomainId', metaDomainId) }
    if(fieldType){ fieldQuery.where('fieldType', fieldType) }
    return fieldQuery.orderBy('seqOrder', 'asc').orderBy('id')
      .with('relMetaDomain').get()
  },

  /** DOMAIN关联关系 */
  // eg: 联系人(metaDomain)->客户(relDomain), 读取关联客户的字段列表
  many2OneFields: function (metaDomainId, relDomainId) {
    let fields = utilObj.metaDomainFields(metaDomainId, FIELD.MANY_TO_ONE)
    if(relDomainId){
      fields = fields.filter(field => {
        let dfExtend = converters.parseObj(field['extendJson'])
        return _.get(dfExtend, OTHER.REL_DOMAIN_ID)==relDomainId // 忽略类型
      })
    }
    return fields
  },
  // eg: 客户(metaDomain)->联系人(relDomain)|联系人1对n客户字段(relDomainField), 读取关联联系人的字段列表
  one2ManyFields: function (metaDomainId, relDomainId, relDomainFieldId) {
    let fields = utilObj.metaDomainFields(metaDomainId, FIELD.ONE_TO_MANY)
    if(relDomainId){
      fields = fields.filter(field => {
        let dfExtend = converters.parseObj(field['extendJson'])
        return _.get(dfExtend, OTHER.REL_DOMAIN_ID)==relDomainId // 忽略类型
      })
    }
    if(relDomainFieldId){
      fields = fields.filter(field => {
        let dfExtend = converters.parseObj(field['extendJson'])
        return _.get(dfExtend, OTHER.REL_DOMAIN_FIELD_ID)==relDomainFieldId // 忽略类型
      })
    }
    return fields
  },
  // 传入n对1字段，返回[字段Domain、字段DomainField、对端Domain、对端DomainField(1对n)]
  many2OneFieldWithRelInfo: function(fieldId){
    // 关联的MetaDomain
    let metaDomainField = MetaDomainField.find(fieldId)
    let metaDomain = MetaDomain.find(metaDomainField?.metaDomainId)
    let relMetaDomain = undefined
    let relMetaDomainField = undefined
    if(metaDomainField){
      let extendJson = converters.parseObj(metaDomainField['extendJson'])
      relMetaDomain = MetaDomain.find(extendJson?.relDomainId)
      // 当前字段相关的一对多字段
      let one2ManyFields = utilObj.one2ManyFields(relMetaDomain?.id || 0, metaDomain?.id || 0, fieldId)
      relMetaDomainField = _.get(one2ManyFields, [0])
    }
    return [metaDomain, metaDomainField, relMetaDomain, relMetaDomainField]
  },
}
export default utilObj
