import _ from '@/utils/tiny_lodash'
import { COMPO, FIELD, QUERY, DATE } from '@/constants'
import { converters, stores } from '@/utils/util'
import moment from '@/utils/moment'

/** 是否支持字段 */
export function fieldSupported(dfItem) {
  dfItem = dfItem || {}
  let supportedFields = [
    FIELD.STRING,
  ]
  if(dfItem.fieldType){
    return supportedFields.indexOf(dfItem.fieldType) !== -1
  }else{
    return true // 默认支持
  }
}

/** 映射字段图标 */
const FIELD_ICON_MAP = {
  // 选择题
  radio: 'g-field-radio',
  select: 'g-field-select',
  // 填空简答题
  input: 'g-field-input',
  textarea: 'g-field-textarea',
  integer: 'g-field-input-number',
  decimal: 'g-field-input-number',
  // 评分题
  rate: 'g-field-label',
  // 个人信息
  name: 'g-field-input',
  gender: 'g-field-radio',
  birthday: 'g-field-date',
  age: 'g-field-input-number',
  mobile: 'g-field-input',
  phone: 'g-field-input',
  address: 'g-field-addr',
  detailAddress: 'g-field-addr',
  // 其它题型
  upload: 'g-field-file',
  date: 'g-field-date',
  block: 'g-block',
  note: 'g-field-label',
  pics: 'g-field-picture',
}
export function candidateFieldIcon(cdfItem) {
  cdfItem = cdfItem || {}
  return _.get(FIELD_ICON_MAP, cdfItem.fieldName, 'g-field-unknown')
}

/** 映射字段查询组件 */
const FIELD_QUERY_COMPONENT_MAP = {
  [FIELD.STRING]: COMPO.G_INPUT,
  [FIELD.ADDR]: COMPO.G_REF_ADDR,
  [FIELD.SELECT]: COMPO.G_SELECT,
  [FIELD.DATE]: COMPO.G_DATE_RANGE,
  [FIELD.INTEGER]: COMPO.G_NUMBER_RANGE,
}
export function candidateFieldQueryComponent(cdfItem) {
  cdfItem = cdfItem || {}
  return _.get(FIELD_QUERY_COMPONENT_MAP, cdfItem.fieldType, COMPO.G_INPUT)
}

/** 映射字段查询操作符 */
const FIELD_QUERY_OPERATOR_MAP = {
  [FIELD.STRING]: QUERY.CONTAINS,
  [FIELD.BOOLEAN]: QUERY.EQUAL,
  [FIELD.SELECT]: QUERY.EQUAL,
  [FIELD.INTEGER]: QUERY.BETWEEN,
  [FIELD.LONG]: QUERY.BETWEEN,
  [FIELD.BIG_DECIMAL]: QUERY.BETWEEN,
  [FIELD.DATE]: QUERY.BETWEEN,
  [FIELD.USER]: QUERY.EQUAL,
  [FIELD.GROUP]: QUERY.EQUAL,
  [FIELD.ADDR]: QUERY.EQUAL,
}
export function candidateFieldQueryOperator(dfItem) {
  dfItem = dfItem || {}
  return _.get(FIELD_QUERY_OPERATOR_MAP, dfItem.fieldType, QUERY.EQUAL)
}


/** 丰富ClonedItem */
export function enrichClonedItem(clonedItem, that, h) {
  h || true
  // 1、按字段类型来丰富Render
  if([FIELD.USER, FIELD.GROUP, FIELD.MANY_TO_ONE, FIELD.ADDR].indexOf(clonedItem.type)!==-1){
    // EntityRenderFunction
    clonedItem.customRender = function(text, record){
      return _.get(record, `${clonedItem.field}_entity._title`)
    };
  }else if([FIELD.FILE, FIELD.PICTURE].indexOf(clonedItem.type)!==-1){
    // FileRenderFunction
    clonedItem.customRender = function(text, record){
      let vnodes = []
      _.get(record, clonedItem.field, []).forEach(item => {
        vnodes.push(<span>{item.fileName}</span>)
        vnodes.push(<br/>)
      })
      return <span>{vnodes}</span>
    }
  }else if(FIELD.DATE.indexOf(clonedItem.type)!==-1){
    // DateRenderFunction
    clonedItem.customRender = function(text, record){
      let dateFormat = _.defaultTo(_.get(clonedItem, 'extend.uiFormat'), DATE.YYYYMMDD)
      let itemValue = _.get(record, clonedItem.field)
      return itemValue?moment(itemValue, DATE.YYYYMMDD).format(dateFormat):null
    };
  }else if(['Action'].indexOf(clonedItem.type)!==-1){
    // SlotRender
    let fieldRender = clonedItem.component
    clonedItem.align = 'center'
    clonedItem.width = stores.isMobile()?80:130
    clonedItem.scopedSlots = { customRender: fieldRender}
  }else{
    // 2、按component来丰富Render
    let extendOptions = _.get(clonedItem, 'extend.options', {})
    let transformOptions = converters.reverseObj(extendOptions)
    let fieldRender = clonedItem.component
    if('linkRender' === fieldRender){
      let linkRender = function(text, record){
        let itemValue = _.get(record, clonedItem.field)
        let itemValueStr = _.toString(itemValue)
        let attrs = {title: record.id}
        let on = { click: () => { that.onRecordEdit(record) } }
        return <a {...{attrs, on}}>{ _.get(transformOptions, itemValueStr, itemValueStr) }</a>
      };
      linkRender || true // 暂未启用
      // SlotRender
      clonedItem.align = 'left'
      clonedItem.scopedSlots = { customRender: fieldRender}
    }else if('tagRender' === fieldRender){
      clonedItem.customRender = function(text, record){
        let itemValue = _.get(record, clonedItem.field)
        let itemValueStr = _.toString(itemValue)
        let props = {color: 'green'}
        return <a-tag {...{props}}>{ _.get(transformOptions, itemValueStr, itemValueStr) }</a-tag>
      };
    }else if('badgeRender' === fieldRender){
      clonedItem.customRender = function(text, record){
        let itemValue = _.get(record, clonedItem.field)
        let itemValueStr = _.toString(itemValue)
        let props = {status: 'success', text: that._.get(transformOptions, itemValueStr, itemValueStr)}
        return <a-badge {...{props}} />
      };
    }else if('entityRender' === fieldRender){
      clonedItem.customRender = function(text, record){
        return _.get(record, `${clonedItem.field}_entity._title`)
      };
    }else if('defaultRender' === fieldRender || _.isNil(fieldRender)){
      clonedItem.customRender = function(text, record){
        let itemValue = _.get(record, clonedItem.field)
        let itemValueStr = _.toString(itemValue)
        let rstStr = _.get(transformOptions, itemValueStr, itemValueStr)
        return rstStr?(rstStr.length>20?`${rstStr.slice(0, 20)}...`:rstStr):undefined
      };
    }else{
      // 按SlotRender处理
      clonedItem.scopedSlots = { customRender: fieldRender}
    }
  }
}

