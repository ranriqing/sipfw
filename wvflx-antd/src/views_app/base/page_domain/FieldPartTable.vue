<template>
  <div>
    <!-- 字段列表 -->
    <div class="field-list ant-list-split" :data-cptFieldItems="cptFieldItems">
      <draggable
        tag="div"
        class="item-container"
        v-bind="cptDragOptions"
        :list="tableDataSource"
        @change="onDragChange">
        <a-list-item v-for="(record, index) in tableDataSource" :key="index" class="drag-item">
          <a-list-item-meta>
            <template slot="avatar">
              <span class="drag-item-widget">
                <a-icon style="font-size: 20px;" type="menu" />
              </span>
              <a-avatar v-if="false">{{ partConfig.otherConfig.fieldTypeLetter[record['fieldType']] }}</a-avatar>
            </template>
            <template slot="title">
              <template v-if="record.id>0">
                <a @click="onRecordEdit(record)" :title="`${record.id} - ${record.fieldName}`">
                  {{ record.title }}
                </a>
                <a-tag v-if="record.codeField" color="orange" style="margin-left: 8px;"> Code </a-tag>
                <a-tag v-if="record.globalSearch" color="blue" style="margin-left: 8px;"> Search </a-tag>
                <a @click="onConstraintEdit(record)" style="margin-left: 10px;">
                  <a-icon type="tool" /> 配置
                </a>
              </template>
              <template v-else>
                <span>{{ record.title }} [内部]</span>
              </template>
            </template>
            <template slot="description">
              {{ _.get(record, 'remark') || '暂无描述' }}
            </template>
          </a-list-item-meta>
          <div class="list-content">
            <div
              class="list-content-item"
              v-for="(item, index1) in tableColumns()"
              :key="index1">
              <template v-if="item.dataIndex === 'fieldType'">
                <span>{{ item.title }}</span>
                <p>
                  {{ partConfig.otherConfig.fieldTypeOptions[record['fieldType']] }}
                  <span v-if="record['fieldType'] === FIELD.MANY_TO_ONE">
                    [多对一]<span :class="_relMetaDomain(record)?'success':'warning'">{{ _.get(_relMetaDomain(record), 'title') || '已失效' }}</span>
                  </span>
                  <span v-else-if="record['fieldType'] === FIELD.ONE_TO_MANY">
                    [一对多]<span :class="_relMetaDomain(record)?'success':'warning'">{{ _.get(_relMetaDomain(record), 'title') || '已失效' }}</span>
                  </span>
                </p>
              </template>
              <template v-else-if="item.dataIndex === 'fieldName'">
                <span>{{ item.title }}</span>
                <p>
                  <a style="margin-right: 16px;" @click="_copyText(record['fieldName'], $event)">
                    <a-icon type="copy"/>
                    {{ record['fieldName'] }}
                  </a>
                </p>
              </template>
              <template v-else-if="item.dataIndex === 'dateCreated'">
                <span>{{ item.title }}</span>
                <p>{{ record['dateCreated'] }}</p>
              </template>
              <template v-else-if="item.dataIndex === 'lastUpdated'">
                <span>{{ item.title }}</span>
                <p>{{ record['lastUpdated'] }}</p>
              </template>
              <template v-else>
                <span>{{ item.title }}</span>
                <p> record[item.dataIndex] </p>
              </template>
            </div>
          </div>
          <!-- Action定义 -->
          <div slot="actions">
            <a :disabled="record.id<0" @click="onRecordEdit(record)">编辑</a>
          </div>
          <div slot="actions">
            <a-dropdown>
              <a :disabled="record.id<0">更多<a-icon type="down"/></a>
              <a-menu slot="overlay">
                <a-menu-item>
                  <a-popconfirm title="确定删除吗?" @confirm="() => onRecordDelete(record)">
                    <a>删除</a>
                  </a-popconfirm>
                </a-menu-item>
              </a-menu>
            </a-dropdown>
          </div>
        </a-list-item>
      </draggable>
    </div>
    <!-- 添加字段 -->
    <a-row :gutter="12" style="margin-top: 10px;">
      <a-col :md="12" :sm="24" :xs="24">
        <a-button type="dashed" icon="plus" block @click="onRecordAdd">基本字段</a-button>
      </a-col>
      <a-col :md="12" :sm="24" :xs="24">
        <a-button type="dashed" icon="link" block @click="onRelationAdd">关联字段</a-button>
      </a-col>
    </a-row>
  </div>
</template>

<script>
  import Clipboard from 'clipboard'
  import AList from 'ant-design-vue/lib/list';
  import AAvatar from 'ant-design-vue/lib/avatar';

  import { postAction } from '@/api/api'
  import { FIELD, DATE } from '@/constants'
  import {converters, domains} from '@/utils/util'
  import { vTableUtil } from '@/utilsv/vUtil'

  import MetaDomain from '@/store/models/MetaDomain'
  import MetaDomainField from '@/store/models/MetaDomainField'
  import MetaPageComponentField from '@/store/models/MetaPageComponentField'

  import partConfig from './configs/fieldPartConfig'

  export default {
    name: "FieldPartTable",
    components: {
      AAvatar,
      AList,
      AListItem: AList.Item,
      AListItemMeta: AList.Item.Meta,
    },
    props: {
      domainId: {
        type: Number,
        default: 0
      },
      configObj: {
        type: Object,
        required: true,
      }
    },
    data () {
      return {
        /** 配置&&状态 */
        FIELD: FIELD,
        loading:false,
        partConfig: partConfig,
        /** 查询&数据源 */
        queryRules: [],
        tableSorter: {},
        tableDataSource: [],
      };
    },
    computed: {
      cptDragOptions() {
        return {
          animation: 200,
          group: "entityField",
          ghostClass: "ghost",
          handle: '.drag-item-widget',
        };
      },
      cptFieldItems: function(){
        let that = this
        let domainFields = domains.metaDomainFields(that.domainId, undefined)
        that.tableDataSource = domainFields.map(that._cloneItem)
        return domainFields
      }
    },
    methods: {
      /** Col字段 */
      tableColumns(){
        let that = this
        let tColumns = that.configObj.tableColumns()
        let cloneItemFunc = vTableUtil.tableCloneItem
        return tColumns.map(item => cloneItemFunc(that, item, that.$createElement))
      },
      /** 执行操作 */
      onRecordAdd(){
        this.$emit('recordAdd');
      },
      doRecordSave(record){
        let that = this
        record['metaDomainId'] = that.domainId
        // 默认值
        if(record['fieldType'] === FIELD.SEQUENCE){
          record['extendJson'] = converters.encodeObj({
            seqFormat: '%1$sCHN%2$05d',
          })
        }else if(record['fieldType'] === FIELD.SELECT){
          record['extendJson'] = converters.encodeObj({
            options: {"选项1":1, "选项2":2},
          })
        }else if(record['fieldType'] === FIELD.SC_EL){
          record['extendJson'] = converters.encodeObj({
            depth:1,
            scKey:'sciMultiLevel'
          })
        }else if(record['fieldType'] === FIELD.DATE){
          record['extendJson'] = converters.encodeObj({
            format: DATE.YYYYMMDD
          })
        }
        let promise = MetaDomainField.$create({
          data: record,
        })
        promise.then(resp => {
          resp || true
          that.$message.success('保存成功');
        })
        return promise
      },
      onRecordEdit(record){
        this.$emit('recordEdit', record)
      },
      doRecordUpdate(record){ 
        let that = this
        let promise = MetaDomainField.$update({
          params: {
            id: record.id,
          },
          data: record
        })
        promise.then(resp => {
          resp || true
          that.$message.success('更新成功');
        })
        return promise
      },
      onRecordDelete(record){
        let that = this
        let metaFields = MetaPageComponentField.query().where('metaDomainFieldId', record?.id)
          .orderBy('seqOrder', 'asc').orderBy('id', 'desc').get()
        if(metaFields.length>0){
          that.$confirm({
            title: '确认弹窗',
            content: `删除字段【${record.title}】同时删除【${metaFields.length}】个相关组件?`,
            onOk() {
              let allPromise = []
              metaFields.forEach(metaField => {
                let promise = MetaPageComponentField.$delete({
                  params: {
                    id: metaField.id,
                  }
                })
                allPromise.push(promise)
              })
              Promise.all(allPromise).then(() => {
                that.doRecordDelete(record)
              })
            }
          });
        }else{
          that.doRecordDelete(record)
        }
      },
      doRecordDelete(record) {
        let that = this
        let promise = MetaDomainField.$delete({
          params: {
            id: record.id
          }
        })
        promise.then(() => {
          that.$message.success(`删除[${record.title}]成功`);
        })
        return promise
      },
      /** 扩展操作 */
      onRelationAdd(){
        this.$emit('relationAdd')
      },
      onConstraintEdit(record){
        this.$emit('constraintEdit', record)
      },
      onDragChange(changeEvent) {
        let that = this
        let domainFields = MetaDomainField.query()
          .where('metaDomainId', that.domainId).where('visible', true)
          .orderBy('seqOrder', 'asc').orderBy('id', 'asc').get()
        let mappedItems = domainFields.map(item => {
          return {
            id: item.id,
            title: item.title,
            seqOrder: item.seqOrder
          }
        })
        if(changeEvent['moved']){
          let oldIndex = that._.get(changeEvent, 'moved.oldIndex', null)
          let newIndex = that._.get(changeEvent, 'moved.newIndex', null)
          let item = mappedItems[oldIndex]
          mappedItems.splice(oldIndex, 1)
          mappedItems.splice(newIndex, 0, item)
          let itemOrders = []
          mappedItems.forEach((item, index) => {
            let itemData = {id: item.id, seqOrder: index+1}
            itemOrders.push(itemData)
          })
          postAction('/metaDomainField/seqOrders', {items: itemOrders}).then(resp => {
            let entitys = resp.gmodel
            MetaDomainField.insertOrUpdate({
              data: entitys
            })
          })
        }
      },
      /** 工具方法 */
      _copyText(copyText, e){
        let that = this
        let target = e.target
        if(e.target.tagName === 'svg'){
          target = e.target.parentElement
        }
        let clipboard = new Clipboard(target, {
          text: () => copyText
        })
        clipboard.on('success', () => {
          that.$message.success(`拷贝成功`);
          clipboard.destroy()
        })
        clipboard.on('error', () => {
          clipboard.destroy()
        })
        clipboard.onClick(e)
      },
      _relMetaDomain(fieldRecord){
        let that = this
        let domainId = that._.get(fieldRecord, 'extendJson.relDomainId')
        return MetaDomain.find(domainId)
      },
      _cloneItem(item){
        let itemObj = {
          id: item.id,
          title: item.title,
          fieldName: item.fieldName,
          fieldType: item.fieldType,
          codeField: item.codeField,
          seqOrder: item.seqOrder,
          globalSearch: item.globalSearch,
          remark: item.remark,
          dateCreated: item.dateCreated,
          lastUpdated: item.lastUpdated,
          metaDomainId: item.metaDomainId,
          extendJson: converters.parseObj(item.extendJson),
          constraintJson: converters.parseObj(item.constraintJson),
        }
        // 查询One2Many关联
        if(item.fieldType === FIELD.MANY_TO_ONE){
          let [metaDomain, metaDomainField, relMetaDomain, relMetaDomainField] = domains.many2OneFieldWithRelInfo(item.id)
          metaDomain || metaDomainField || relMetaDomain || true
          // 补充inverseJson
          if(relMetaDomainField){
            let extendJson = converters.parseObj(relMetaDomainField['extendJson'])
            itemObj['inverseJson'] = {
              inverseRelOn: true,                                           // 启用关联
              relDomainFieldType: extendJson['relDomainFieldType'],         // 对端->当前端: 多个(many)|单个(one)
            }
          }
        }
        return itemObj
      },
    },
  }
</script>

<style lang="scss" scoped>
  .field-list {
    .item-container {
      /* 拖拽Ghost */
      .ghost{
        border-top: 1px dashed #1890ff;
      }
      /* Item元素 */
      .drag-item-widget {
        cursor: move;
        margin-right: 10px;
        vertical-align: middle;
        color: rgba(0, 0, 0, 0.45);
      }
      &:empty{
        &:before{
          min-height: 100px;
          vertical-align: center;
          content:"暂无字段数据";
          font-size: 16px;
          color: #bbbbbb;
          text-align: center;
          display: block;
        }
      }
    }
    /deep/ .ant-list-item-meta {
      flex: 2;
    }
    .list-content{
      flex: 3;
      .list-content-item {
        display: inline-block;
        vertical-align: middle;
        font-size: 14px;
        min-width: 100px;
        margin-left: 45px;
        span {
          line-height: 20px;
        }
        p {
          color: rgba(0, 0, 0, .45);
          margin-top: 4px;
          margin-bottom: 0;
          line-height: 22px;
        }
      }
    }
  }
</style>
