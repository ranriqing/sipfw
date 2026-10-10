<template>
  <div
    class="tmpl-block"
    :data-cptFieldItems="cptFieldItems">
    <div class="flex flex-row main-between" style="margin-bottom: 8px;">
      <a-button-group>
        <a-button v-if="_.get(sModel,'extend.addEnabled') || true" size="small" icon="plus" @click="showRelPop">{{ '新增' }}</a-button>
      </a-button-group>
      <div class="config-wrapper">
        <a-button v-if="stores.isLabState()" size="small" @click="vPopUtil.handleColsConfig(_self)">
          <a-icon type="setting"/>列定义
        </a-button>
      </div>
    </div>
    <div class="content-wrapper">
      <!-- debug: {{ _.get(sModel, 'dfExtend') }}-->
      <div
        v-for="(relRecord, index0) in tableDataSource"
        class="content-item"
        :key="relRecord.formKey || index0">
        <a-form :ref="relRecord.formKey" :form="formInstances[relRecord.formKey]" class="form-inline form-readonly" layout="horizontal">
          <a-row
            :gutter="12">
            <a-col
              v-for="(fieldItem, index1) in datFieldItems"
              :key="index1"
              :md="fieldItem.cols?fieldItem.cols:10"
              :sm="24"
              :xs="24">
              <g-form-item
                style="width: 100%;"
                v-if="fieldItem.isActived?fieldItem.isActived(relRecord, {}):true"
                :labelCol="{
                  xs: { span: 24 },
                  sm: { span: 6 }
                }"
                :wrapperCol="{
                  xs: { span: 24 },
                  sm: { span: 18 }
                }"
                :form="formInstances[relRecord.formKey]"
                :item="fieldItem"
                :record="relRecord"
                @collapsePanel="vPopUtil.collapsePanel(_self, fieldItem, $event)">
              </g-form-item>
            </a-col>
            <a-col :md="4" :sm="24" :xs="24">
              <div>
                <a v-if="_.get(sModel,'extend.editEnabled') || true" @click="showRelPop(relRecord)">
                  <a-icon class="icon-action" type="edit" />
                </a>
                <a-divider
                  type="vertical"
                  v-if="_.get(sModel,'extend.editEnabled') && _.get(sModel,'extend.deleteEnabled') || true">
                </a-divider>
                <a v-if="_.get(sModel,'extend.deleteEnabled') || true" class="item-actions">
                  <a-popconfirm title="确定删除?" @confirm="() => doRelDelete(relRecord)">
                    <a-icon class="icon-action" type="close" />
                  </a-popconfirm>
                </a>
              </div>
            </a-col>
          </a-row>
        </a-form>
      </div>
      <div v-if="tableDataSource.length===0" class="flex flex-row main-center">
        <a-spin v-if="loading" style="padding: 20px 0;" />
        <a-empty v-else :imageStyle="{height: '70px'}" description="暂无数据"/>
      </div>
      <div v-else-if="loadMoreVisible">
        <a-spin :spinning="loading">
          <a-button block icon="down" @click="_loadData">
            加载更多
          </a-button>
        </a-spin>
      </div>
    </div>
    <GColsPop
      ref="refCols"
      :configObj="colsPopConfig">
    </GColsPop>
  </div>
</template>

<!-- 此文件和LabPartRelations完全相同，解决AppFormPop展示后LabFormPop不显示问题 -->
<script>
  import AUpload from 'ant-design-vue/lib/upload';
  import AEmpty from 'ant-design-vue/lib/empty';

  import {COMPO, FIELD, OTHER, QUERY, DATE} from "@/constants";
  import {tools, stores, converters} from '@/utils/util'
  import {vPopUtil, vTableUtil, vPageUtil} from '@/utilsv/vUtil'
  import { postAction, putAction, deleteAction } from '@/api/api'

  import GColsPop from "@/components/page_pop/GColsPop.vue";
  import MetaDomain from "@/store/models/MetaDomain";
  import MetaPageComponent from "@/store/models/MetaPageComponent";
  import MetaPageComponentField from "@/store/models/MetaPageComponentField";

  import AppFormPop from '@/components/tmpl_pop/AppFormPop'
  import LabFormPop from '@/components/tmpl_pop/LabFormPop'

  import labPopColsPopConfig from "@/components/tmpl_pop/configs/labPopColsPopConfig";

  export default {
    name: 'LabPartRelations',
    components: {
      GColsPop,
      AUpload,
      AEmpty,
      AppFormPop,
      LabFormPop,
    },
    /* eslint-disable */
    props: {
      sModel: {
        type: Object,
        required: true
      },
      /** 扩展Props */
      record: {
        type: Object,
        required: false
      },
    },
    watch: {
      record: {
        immediate: true,
        handler(newValue, oldValue) {
          let that = this
          oldValue || true
          if(that._.get(newValue, 'id')){
            that._loadData(1)
          }else{
            that.tableDataSource = []
            that.tablePagination.total = 0
            that._refreshFormInstances()
          }
        }
      },
    },
    data() {
      return {
        /** 配置&状态 */
        stores: stores,
        vPopUtil: vPopUtil,
        colsPopConfig: labPopColsPopConfig,
        loading:false,
        loadMoreVisible: false,
        datFieldItems: [],
        formInstances: {},
        /** 查询&数据源 */
        tablePagination: {
          current: 1,
          pageSize: 10,
          pageSizeOptions: ['30', '50', '100'],
          showTotal: (total, range) => {
            return range[0] + "-" + range[1] + " 共" + total + "条"
          },
          showQuickJumper: true,
          showSizeChanger: true,
          total: 0
        },
        tableDataSource: [],
        defaultDataSource: [],
      }
    },
    computed: {
      cptRelDomainName: function () {
        let that = this
        return that._.defaultTo(that._.get(that.sModel, `extend.${OTHER.REL_DOMAIN_NAME}`), true)
      },
      cptRelDomainFieldName: function () {
        let that = this
        return that._.defaultTo(that._.get(that.sModel, `extend.${OTHER.REL_DOMAIN_FIELD_NAME}`), true)
      },
      cptFieldItems: function(){
        let that = this
        let metaFields = MetaPageComponentField.query().where('metaPageComponentId', that.sModel?.id)
          .orderBy('seqOrder', 'asc').orderBy('id', 'asc').with('relMetaDomainField').get()
        let cloneItemFunc = vPopUtil.popCloneItem
        let panelItem = null
        // 特殊处理，读取表格配置，是否允许编辑
        that.datFieldItems = metaFields.map(item => {
          let clonedItem = cloneItemFunc(that, item)
          if(item['component']===COMPO.G_PANEL){
            clonedItem['pnlIdent'] = item['id']
            panelItem = item
          }else if(panelItem){
            clonedItem['panelIdent'] = panelItem['id']
          }
          clonedItem['options'] = {}
          that._.set(clonedItem, 'extend.readonly', true)
          that._.set(clonedItem, 'extend.labelAlign', 'left')
          return clonedItem
        })
        return metaFields
      },
    },
    methods: {
      _loadData(arg) {
        let that = this
        if (arg === 1) {
          that.tablePagination.current = 1;
        }else{
          that.tablePagination.current = that.tablePagination.current + 1;
        }
        // 参数校验
        let recordId = that._.get(that.record, 'id')
        if (!(that.cptRelDomainName && that.cptRelDomainFieldName && recordId)) {
          return
        }
        // 查询条件
        let queryCondition = {
          pageNum: that.tablePagination.current,
          pageSize: that.tablePagination.pageSize,
          orderBy: 'id-desc',
          condition: 'and',
          rules: [{
            field: that.cptRelDomainFieldName + FIELD.SUFFIX_ID,
            value: recordId,
            operator: QUERY.EQUAL,
          }],
        }
        // 加载数据
        that.loading = true
        let indexFullUri = that._domainUri('index')
        postAction(indexFullUri, queryCondition).then(resp => {
          that.loading = false
          if (resp.gmodel && resp.gmodel.entityList) {
            let entityList = resp.gmodel.entityList
            that.tableDataSource = that.tableDataSource.concat(entityList)
            that.tablePagination.total = resp.gmodel.totalCount
            that._refreshFormInstances()
            // 回显form数据
            that.tableDataSource.map(record => {
              let formKey = record.formKey;
              let formInstance = that.formInstances[formKey];
              let values = {}
              that.datFieldItems.forEach( item => {
                if([COMPO.G_PANEL, COMPO.G_SLOT, COMPO.G_LOCK].indexOf(item.component)!==-1){ return }
                let fieldValue = item.getValue?item.getValue(record, that.context):
                  that._.isNil(record[item.field])?values[item.field]:record[item.field]
                fieldValue = that._.isNil(fieldValue)?undefined:fieldValue
                if(COMPO.G_DATE_PICKER === item.component){
                  let dateFormat = that._.get(item, 'extend.format') || DATE.YYYYMMDD
                  values[item.field] = fieldValue?that.$moment(fieldValue, dateFormat):null
                }else if(COMPO.G_TIME_PICKER === item.component){
                  values[item.field] = fieldValue?that.$moment(fieldValue, 'HH:mm:ss'):null
                }else{
                  values[item.field] = fieldValue
                }
              })
              that._.forIn(values, (value, key)=>{ formInstance.getFieldDecorator(key, {}) })
              formInstance.setFieldsValue(values)
            })
            that.loadMoreVisible = entityList.length === that.tablePagination.pageSize
          }
        }).catch(err => {
          that.loading = false
          that.tableDataSource = that.defaultDataSource
          vTableUtil.tableReqErr(that, err, '加载失败')
        })
      },
      /** 弹窗操作 */
      async showRelPop(relRecord){
        let that = this
        relRecord = relRecord || {}
        let isUpdate = !!relRecord.id
        // 加载元信息
        let popType = isUpdate?OTHER.FIELD_POP_EDIT:OTHER.FIELD_POP_ADD
        let fieldItem = that.sModel
        let fieldIdent = `${fieldItem.id}_${fieldItem.field}`
        let fieldComponent = MetaPageComponent.query().where('ident', fieldIdent)
          .where('type', popType).orderBy('id', 'desc').first()
        let relMetaDomain = MetaDomain.query().where('domainName', that.cptRelDomainName).first()
        if(!fieldComponent){
          await MetaPageComponent.$create({
            data: {
              title: `字段组件_${fieldItem.title}`,
              pIdent: 'comp_field',
              ident: fieldIdent,
              type: popType,
              component: 'FormPop',
              metaDomainId: relMetaDomain?.id,
            }
          }).then(() =>{
            fieldComponent = MetaPageComponent.query().where('ident', fieldIdent)
              .where('type', popType).orderBy('id', 'desc').first()
          })
        }
        // 动态创建组件
        let dynComp = that.$dynamicModal({
          sModel: fieldComponent,
          configObj: vPageUtil.popConfig(that, {extend: converters.parseObj(fieldComponent?.extend)}, {
            title: `${isUpdate?'编辑':'新增'} - ${that.sModel?.title}`, multiTab: false, submitText: isUpdate?'更新':'保存'
          }, {modal: true}),
        }, stores.isLabState()?LabFormPop:AppFormPop)
        dynComp.showPop(relRecord,  that._.get(that.sModel, 'mainContext') || {},
          (relRecord, relContext) => { /* submit */
            relContext || true
            relRecord[that.cptRelDomainFieldName + FIELD.SUFFIX_ID] = that._.get(that.record, 'id')
            let promise
            if(isUpdate){
              promise = putAction(that._domainUri('update'), relRecord.id, relRecord)
              promise.then(resp => {
                let rstRecord = resp.gmodel
                let recordIndex = that.tableDataSource.findIndex( item => item.id === rstRecord.id)
                if(recordIndex !== -1){
                  that.tableDataSource.splice(recordIndex, 1, rstRecord)
                  that._refreshFormInstances()
                  that._showRecord(rstRecord)
                }
                that.$message.success('更新成功');
              }).catch(err => {
                vTableUtil.tableReqErr(that, err, '更新失败')
              })
            }else{
              promise = postAction(that._domainUri('save'), relRecord)
              promise.then(resp => {
                let rstRecord = resp.gmodel
                that.tableDataSource.unshift(rstRecord)
                that._refreshFormInstances()
                that._showRecord(rstRecord)
                that.$message.success('保存成功');
              }).catch(err => {
                vTableUtil.tableReqErr(that, err, '保存失败')
              })
            }
            return promise
          }, () => {
            /* cancel */
          }
        )
      },
      doRelDelete(relRecord){
        let that = this
        let promise = deleteAction(that._domainUri('delete'), relRecord.id, {})
        promise.then(() => {
          let recordIndex = that.tableDataSource.findIndex( item => item.id === relRecord.id)
          if(recordIndex !== -1){
            that.tableDataSource.splice(recordIndex, 1)
          }
          that.$message.success('删除成功');
        }).catch(err => {
          vTableUtil.tableReqErr(that, err, '删除失败')
        })
      },
      /** 工具方法 */
      _domainUri(suffix){
        let that = this
        let relDomainName = that._.defaultTo(that._.get(that.sModel,`extend.${OTHER.REL_DOMAIN_NAME}`), 'demo')
        return `/vm/${tools.toLowerCamel(relDomainName)}/${suffix}`
      },
      _showRecord(record){
        let that = this
        let formKey = record.formKey;
        let formInstance = that.formInstances[formKey];
        let values = {}
        that.datFieldItems.forEach( item => {
          if([COMPO.G_PANEL, COMPO.G_SLOT, COMPO.G_LOCK].indexOf(item.component)!==-1){ return }
          let fieldValue = item.getValue?item.getValue(record, that.context):
            that._.isNil(record[item.field])?values[item.field]:record[item.field]
          fieldValue = that._.isNil(fieldValue)?undefined:fieldValue
          if(COMPO.G_DATE_PICKER === item.component){
            let dateFormat = that._.get(item, 'extend.format') || DATE.YYYYMMDD
            values[item.field] = fieldValue?that.$moment(fieldValue, dateFormat):null
          }else if(COMPO.G_TIME_PICKER === item.component){
            values[item.field] = fieldValue?that.$moment(fieldValue, 'HH:mm:ss'):null
          }else{
            values[item.field] = fieldValue
          }
        })
        that._.forIn(values, (value, key)=>{ formInstance.getFieldDecorator(key, {}) })
        formInstance.setFieldsValue(values)
      },
      _refreshFormInstances(){
        let that = this
        // 创建不存在formInstance
        that.tableDataSource.forEach(record => {
          if(!record.formKey){
            record.formKey = record.id?`ref${record.id}`:tools.randomString(5);
          }
          if (!that.formInstances[record.formKey]) {
            that.formInstances[record.formKey] = that.$form.createForm(that, {
              name: record.formKey,
              onValuesChange: undefined,
            });
          }
        });
        // 移除无用formInstance
        Object.keys(that.formInstances).forEach(formKey => {
          let instanceRecord = that.tableDataSource.find(record => record.formKey===formKey)
          if (!instanceRecord) {
            delete that.formInstances[formKey];
          }
        });
      }
    },
  }
</script>

<style lang="scss" scoped>
.content-wrapper {
  // align-items: stretch;
  .content-item {
    margin-bottom: 8px;
    height: 100%;
    padding: 10px 16px;
    background-color: #f9f9f9;
    box-shadow: 0 1px 1px 0 #e0e0e0;
    border-radius: 5px;
    line-height: 1.5em;
    &:hover{
      background-color: rgba(236, 245, 255, 0.5);
      .item-title{
        .item-actions {
          opacity: 1;
        }
      }
    }
    &.add-item{
      display: flex;
      flex-direction: row;
      justify-content: center;
      align-items: center;
      color: #909399;
      padding: 15px 0;
    }
    .item-title{
      font-size: 14px;
      font-weight: bold;
      .anticon{
        font-size: 13px;
        margin-right: 4px;
      }
      .item-actions{
        position: absolute;
        top: 10px;
        right: 10px;
        opacity: 0;
        .icon-action {
          vertical-align: baseline;
          font-size: 14px;
          padding: 4px 0;
          transform: translateY(2px);
          transition: all .3s;
          cursor: pointer;
        }
      }
    }
    .item-content{
      .label{
        margin-right: 6px;
        font-style: italic;
      }
      .content{

      }
    }
  }
}
</style>