import _ from '@/utils/tiny_lodash'
import {COMPO, FIELD, QUERY} from '@/constants'
import {converters} from "@/utils/util";
import AbstractField from './AbstractField';
import ADropdown from 'ant-design-vue/lib/dropdown';
import GScCascader from '@/components/comp_form/select/GScCascader'
import ScElement from '@/store/models/ScElement'

export default class ScElField extends AbstractField {

  static getSupportedTypes() {
    return [FIELD.SC_EL];
  }

  fieldIcon(){
    return 'g-field-select'
  }

  querySupported(){
    return true
  }

  queryCloneItem(that, domainName, item) {
    let clonedItem = super.queryCloneItem(that, domainName, item)
    let scKey = _.get(clonedItem, 'dfExtend.scKey')
    _.set(clonedItem, 'component', COMPO.G_SC_CASCADER)
    _.set(clonedItem, 'extend.operator', QUERY.EQUAL)
    _.set(clonedItem, 'extend.scKey', scKey)
    clonedItem.rulesGenerator = function(valueArray){
      let ruleArray = []
      if(_.isArray(valueArray) && valueArray.length > 0){
        for(let i=0; i<valueArray.length; i++) {
          let repeatSub = i === 0 ? "" : "Sub".repeat(i)
          let ruleItem = {
            field: clonedItem['field'] + repeatSub,
            value: valueArray[i],
            operator: QUERY.EQUAL,
          }
          ruleArray.push(ruleItem)
        }
      }
      return ruleArray
    }
    return {
      ...clonedItem,
    }
  }

  tableSupported(){
    return true
  }

  tableCloneItem(that, domainName, item, h){
    let clonedItem = super.tableCloneItem(that, domainName, item, h)
    let scKey = _.defaultTo(_.get(clonedItem, 'dfExtend.scKey'), 'none')
    let depth = _.defaultTo(_.get(clonedItem, 'dfExtend.depth'), 1)
    // 组件渲染输出
    clonedItem.innerRender = function(text, record) {
      let fieldName = that._.get(item, 'field')
      let vnodes = [];
      // 显示组件
      vnodes.push(h(GScCascader, {
        props: {
          table: true,
          readonly: true,
          disabled: true,
          value: record[fieldName],
          item: {
            extend: {
              scKey: scKey,
              depth: depth,
            }
          }
        }
      }))
      // 更改组件
      let inlineEdit = _.get(clonedItem, 'cfExtend.inlineEdit')
      if(inlineEdit){
        let doRecordUpdate = undefined
        if(that._.isFunction(that.doRecordUpdate)){
          doRecordUpdate = that.doRecordUpdate
        }
        if(doRecordUpdate){
          // 1. 查询并构建树形数据（保持原有逻辑）
          let allElements = ScElement.query()
            .where(item => item.scConfigKey === scKey)
            .orderBy('seqOrder').orderBy('id').get();
          // 构建树形结构（包含 children 字段）
          let buildTreeData = (allElements) => {
            let treeMap = new Map();
            let rootNodes = [];
            // 初始化节点并映射 ID
            allElements.forEach(elem => {
              treeMap.set(elem.id, {
                id: elem.id,
                pId: elem.parentId,
                key: `elem${elem.id}`,
                title: elem.title,
                tagStyle: elem.tagStyle,
                remark: elem.description,
                value: elem.id,
                label: elem.title,
                children: [],
                parent: null // 存储父节点引用，用于回溯路径
              });
            });
            // 组装父子节点
            allElements.forEach(elem => {
              let node = treeMap.get(elem.id);
              if (elem.parentId === 0) {
                rootNodes.push(node);
              } else {
                let parentNode = treeMap.get(elem.parentId);
                if (parentNode) {
                  parentNode.children.push(node);
                  node.parent = parentNode; // 记录父节点
                }
              }
            });
            return rootNodes;
          };
          // 生成节点的完整路径（ID数组 + 标签拼接）
          let generateNodePath = (node) => {
            const path = []; // ID数组
            const pathLabel = []; // 名称数组
            let currentNode = node;
            while (currentNode) {
              path.unshift(currentNode.id);
              pathLabel.unshift(currentNode.label);
              currentNode = currentNode.parent;
            }
            return {
              path: path,
              pathLabel: pathLabel.join('/')
            };
          };

          // 2. 递归渲染菜单节点
          let treeData = buildTreeData(allElements);
          let renderMenuNode = (node) => {
            if (node.children && node.children.length > 0) {
              return h('a-sub-menu', { key: node.key }, [
                h('span', { slot: 'title' }, node.label),
                node.children.map(child => renderMenuNode(child))
              ]);
            } else { // 叶子节点：渲染 MenuItem（保留原有交互）
              let { path, pathLabel } = generateNodePath(node);
              return h('a-menu-item', { key: node.key }, [
                h('a-popconfirm', {
                  props: { title: `确定改为[${pathLabel}]?` },
                  on: { confirm: () => doRecordUpdate({ id: record.id, [fieldName]: path }) }
                }, [
                  h('a', node.label)
                ])
              ]);
            }
          };

          vnodes.push(
            h(ADropdown, [
              h('a', { class: 'ant-dropdown-link' }, [
                '更改',
                h('a-icon', { props: { type: 'down' } })
              ]),
              h('template', { slot: 'overlay' }, [
                h('a-menu', treeData.map(node => renderMenuNode(node)))
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
    let dfItem = item['relMetaDomainField']
    let dfExtend = converters.parseObj(dfItem?.extendJson)
    let clonedItem = super.popCloneItem(that, domainName, item)
    _.set(clonedItem, 'component', COMPO.G_SC_CASCADER)
    _.set(clonedItem, 'extend.depth', dfExtend.depth)
    _.set(clonedItem, 'extend.scKey', dfExtend.scKey)
    return {
      ...clonedItem,
    }
  }

}