import AbstractField from './AbstractField';
import {COMPO, FIELD, QUERY} from '@/constants'
import _ from "@/utils/tiny_lodash";
import {converters} from "@/utils/util";
import EditableCell from '@/components/comp_tools/EditableCell.vue'

export default class StringField extends AbstractField {

  static getSupportedTypes() {
    return [FIELD.STRING];
  }

  fieldIcon(){
    return 'g-field-string'
  }

  querySupported(){
    return true
  }

  queryCloneItem(that, domainName, item) {
    let clonedItem = super.queryCloneItem(that, domainName, item)
    _.set(clonedItem, 'component', COMPO.G_INPUT)
    _.set(clonedItem, 'extend.operator', QUERY.CONTAINS)
    return {
      ...clonedItem,
    }
  }

  tableSupported(){
    return true
  }

  tableCloneItem(that, domainName, item, h){
    let clonedItem = super.tableCloneItem(that, domainName, item, h)
    // 选项映射：把存储值翻译成展示文案(未配置options时原样输出)
    let transformOptions = converters.reverseObj(_.get(clonedItem, 'extend.options', {}))
    // 组件渲染输出
    clonedItem.innerRender = function(text, record) {
      let fieldName = that._.get(item, 'field')
      let inlineEdit = _.get(clonedItem, 'cfExtend.inlineEdit')
      let vnodes = [];
      if(inlineEdit){
        vnodes.push(
          h(EditableCell, {
            props: {
              value: record[fieldName],
            },
            on: {
              change: (newValue) => {
                if (that._.isFunction(that.doRecordUpdate)) {
                  that.doRecordUpdate({ id: record.id, [fieldName]: newValue });
                }
              },
            }
          })
        );
      }else{
        let itemValueStr = that._.toString(that._.get(record, fieldName))
        vnodes.push(h('div',
          {
            style: {
              'margin-right': '8px',
            }
          },
          [_.get(transformOptions, itemValueStr, itemValueStr)]
        ))
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
    _.set(clonedItem, 'component', COMPO.G_INPUT)
    return {
      ...clonedItem,
    }
  }

}