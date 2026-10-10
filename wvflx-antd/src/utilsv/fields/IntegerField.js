import AbstractField from './AbstractField';
import {COMPO, FIELD, QUERY} from '@/constants'
import _ from "@/utils/tiny_lodash";
import EditableCell from '@/components/comp_tools/EditableCell.vue'

export default class IntegerField extends AbstractField {

  static getSupportedTypes() {
    return [FIELD.INTEGER];
  }

  fieldIcon(){
    return 'g-field-number'
  }

  querySupported(){
    return true
  }

  queryCloneItem(that, domainName, item) {
    let clonedItem = super.queryCloneItem(that, domainName, item)
    _.set(clonedItem, 'component', COMPO.Q_DYN_INTEGER)
    _.set(clonedItem, 'extend.operator', QUERY.BETWEEN)
    return {
      ...clonedItem,
    }
  }

  tableSupported(){
    return true
  }

  tableCloneItem(that, domainName, item, h){
    let clonedItem = super.tableCloneItem(that, domainName, item, h)
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
              type: 'Integer',
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
        vnodes.push(h('div',
          {
            style: {
              'margin-right': '8px',
            }
          },
          [record[fieldName]]
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
    _.set(clonedItem, 'component', COMPO.G_NUMBER_INTEGER)
    return {
      ...clonedItem,
    }
  }

}