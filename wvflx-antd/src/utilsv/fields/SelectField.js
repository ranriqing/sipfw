import _ from '@/utils/tiny_lodash'
import AbstractField from './AbstractField';
import {COMPO, FIELD, QUERY} from '@/constants'
import GSelect from "@/components/comp_form/select/GSelect.vue";
import ADropdown from 'ant-design-vue/lib/dropdown';
import ABadge from 'ant-design-vue/lib/badge';
import {converters} from "@/utils/util";

export default class SelectField extends AbstractField {

  static getSupportedTypes() {
    return [FIELD.SELECT];
  }

  fieldIcon(){
    return 'g-field-select'
  }

  querySupported(){
    return true
  }

  queryCloneItem(that, domainName, item) {
    let clonedItem = super.queryCloneItem(that, domainName, item)
    _.set(clonedItem, 'component', COMPO.G_MULTI_SELECT)
    _.set(clonedItem, 'extend.operator', QUERY.IN)
    _.set(clonedItem, 'extend.options', _.get(clonedItem, 'dfExtend.options') || [])
    return {
      ...clonedItem,
    }
  }

  tableSupported(){
    return true
  }

  tableCloneItem(that, domainName, item, h){
    let clonedItem = super.tableCloneItem(that, domainName, item, h)
    // 配置转选项数组
    let optionsObj = _.get(clonedItem, 'dfExtend.options') || {}
    let optionsArray = []
    if(that._.isFunction(optionsObj)){
      let optionsFunc = optionsObj
      let result = optionsFunc(that.record, that.context)
      optionsArray = Array.isArray(result) ? result : converters.objToOptions(result)
    }else if(Array.isArray(optionsObj)){
      optionsArray = optionsObj
    }else{
      optionsArray = converters.objToOptions(optionsObj)
    }
    // 组件渲染输出
    clonedItem.innerRender = function(text, record) {
      let fieldName = that._.get(item, 'field')
      let vnodes = [];
      let currentValue = record[fieldName]
      // 查找匹配的选项
      let matchedOption = optionsArray.find(opt =>
        that._.toString(opt.value) === that._.toString(currentValue)
      )
      // 显示组件：有颜色时用 Badge，否则用 GSelect
      if (matchedOption && matchedOption.color) {
        vnodes.push(h(ABadge, {
          props: {
            status: 'default',
            color: matchedOption.color,
            text: matchedOption.label
          }
        }))
      } else {
        vnodes.push(h(GSelect, {
          style: {
            width: 'unset',
            'margin-right': '8px',
          },
          props: {
            readonly: true,
            disabled: true,
            value: currentValue,
            optionsArray: optionsArray
          }
        }))
      }
      // 更改组件
      let inlineEdit = _.get(clonedItem, 'cfExtend.inlineEdit')
      if(inlineEdit){
        let doRecordUpdate = undefined
        if(that._.isFunction(that.doRecordUpdate)){
          doRecordUpdate = that.doRecordUpdate
        }
        if(doRecordUpdate){
          vnodes.push(
            h(ADropdown, [
              h('a', { class: 'ant-dropdown-link' }, [
                '更改',
                h('a-icon', { props: { type: 'down' } })
              ]),
              h('template', { slot: 'overlay' }, [
                h('a-menu',  optionsArray.map(item => {
                  return h('a-menu-item', { key: item.value }, [
                    h('a-popconfirm', {
                      props: { title: '确定改为[' + item.label + ']?' },
                      on: { confirm: () => doRecordUpdate({id: record.id, [fieldName]:item.value}) }
                    }, [
                      item.color
                        ? h(ABadge, { props: { status: 'default', color: item.color, text: item.label } })
                        : h('a', item.label)
                    ])
                  ]);
                }))
              ])
            ])
          )
        }
      }
      return h('div', { class: 'flex flex-row' }, vnodes);
    };
    return {
      ...clonedItem,
    }
  }

  popSupported(){
    return true
  }

  popCloneItem(that, domainName, item) {
    let clonedItem = super.popCloneItem(that, domainName, item)
    _.set(clonedItem, 'component', COMPO.G_SELECT)
    _.set(clonedItem, 'extend.options', _.get(clonedItem, 'dfExtend.options') || [])
    return {
      ...clonedItem,
    }
  }

}