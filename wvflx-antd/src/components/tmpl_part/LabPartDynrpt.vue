<template>
  <a-card :bordered="false" class="page-block tmpl-block" :data-cptFieldItems="cptFieldItems">
    <div class="report-container">
      <div class="page-dynrpt-wrapper">
        <!-- 操作按钮 -->
        <div class="report-operation-buttons">
          <div class="config-wrapper">
            <!-- 报表列 -->
            <a-button size="small" @click="vReportUtil.handleColsConfig(_self)">
              <a-icon type="setting"/>动报表
            </a-button>
          </div>
        </div>
        <!-- 拖拽表头 -->
        <div class="full-container">
          <div class="conr-wrapper-container">
            <div class="draggable-container">
              {{ (sExtend['reportTitle'] || '暂无标题') | Ellipsis(10) }}
            </div>
          </div>
          <div class="cols-wrapper-container">
            <div class="draggable-container draggable-report-container">
              <draggable
                tag="div"
                class="draggable-columns-container"
                :list="datColItems"
                v-bind="cptDragRowTargetOptions"
                @change="(changeEvent) => handleItemDrag(changeEvent, 'dynrptCol')">
                <div
                  class="draggable-column"
                  v-for="(item, index) in datColItems"
                  :key="index">
                  <a-icon :type="vReportUtil.domainFieldIcon(item, 'type')" class="drag drag-widget"/>
                  <span class="column-item">{{ item.title }}</span>
                  <span class="item-actions">
                    <a-icon class="icon-action" type="edit" @click="vReportUtil.handleItemEdit(_self, item)"/>
                    <a-divider type="vertical" />
                    <a-popconfirm title="确定删除?" @confirm="() => vReportUtil.handleItemDelete(_self, item)">
                      <a-icon class="icon-action" type="delete" />
                    </a-popconfirm>
                  </span>
                </div>
              </draggable>
            </div>
          </div>
        </div>
        <div class="full-container">
          <div class="rows-wrapper-container">
            <div class="draggable-container draggable-report-container">
              <div
                v-if="!sExtend['crossOn']"
                class="draggable-columns-container text-tips">
                已关闭
              </div>
              <draggable
                v-else
                tag="div"
                class="draggable-columns-container"
                :list="datRowItems"
                v-bind="cptDragColTargetOptions"
                @change="(changeEvent) => handleItemDrag(changeEvent, 'dynrptRow')">
                <div
                  class="draggable-column"
                  v-for="(item, index) in datRowItems"
                  :key="index">
                  <a-icon :type="vReportUtil.domainFieldIcon(item, 'type')" class="drag drag-widget"/>
                  <span class="column-item">{{ item.title }}</span>
                  <span class="item-actions">
                    <a-icon class="icon-action" type="edit" @click="vReportUtil.handleItemEdit(_self, item)"/>
                    <a-divider type="vertical" />
                    <a-popconfirm title="确定删除?" @confirm="() => vReportUtil.handleItemDelete(_self, item)">
                      <a-icon class="icon-action" type="delete" />
                    </a-popconfirm>
                  </span>
                </div>
              </draggable>
            </div>
          </div>
          <div class="measures-wrapper-container">
            <div class="draggable-container draggable-report-container">
              <draggable
                tag="div"
                class="draggable-columns-container"
                :list="datMeasureItems"
                v-bind="cptDragMeasureTargetOptions"
                @change="(changeEvent) => handleItemDrag(changeEvent, 'dynrptMeasure')">
                <div
                  class="draggable-column"
                  v-for="(item, index) in datMeasureItems"
                  :key="index">
                  <a-icon :type="vReportUtil.domainFieldIcon(item, 'type')" class="drag drag-widget"/>
                  <span class="column-item">{{ item.title }}</span>
                  <span class="item-actions">
                    <a-icon class="icon-action" type="edit" @click="vReportUtil.handleItemEdit(_self, item)"/>
                    <a-divider type="vertical" />
                    <a-popconfirm title="确定删除?" @confirm="() => vReportUtil.handleItemDelete(_self, item)">
                      <a-icon class="icon-action" type="delete" />
                    </a-popconfirm>
                  </span>
                </div>
              </draggable>
            </div>
          </div>
        </div>
      </div>
      <div class="page-dynrpt-wrapper">
        <a-spin :spinning="loading">
          <div
            style="min-height: 150px;"
            class="ant-table ant-table-bordered">
            <!-- 表格内容 -->
            <div
              v-if="reportDataSource.length > 0"
              class="ant-table-content">
              <div class="ant-table-scroll">
                <div class="ant-table-body">
                  <table>
                    <tbody class="ant-table-tbody">
                      <tr
                        v-for="(dataRow, dIndex) in reportDataSource"
                        :key="dIndex">
                        <td
                          v-for="(cell, index) in dataRow"
                          :key="index"
                          :class="cell.class"
                          :rowspan="cell.rowspan"
                          :colspan="cell.colspan">
                          {{ cell.content }}
                        </td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
            <div v-else>
              <a-empty description="暂无数据"/>
            </div>
          </div>
        </a-spin>
      </div>
    </div>

    <!-- Ref弹窗 -->
    <GDynrptPop
      ref="refCols"
      :configObj="colsPopConfig">
    </GDynrptPop>
    <GFormPop
      ref="refMeta"
      :configObj="metaPopConfig">
    </GFormPop>
  </a-card>
</template>

<!--
# 组件说明
  Lab动态报表
# 配置组件
  sModel: Object        // 当前组件SModel
  sContext: Object      // 上下文sContext
# 操作方法
  $emit('metaModel')    // 重新加载Model
  doQuery(queryRules)
-->

<script>
  import ASpin from 'ant-design-vue/lib/spin';

  const settings = require('@/settings')
  import { OTHER, QUERY, FIELD } from '@/constants'
  import { converters } from '@/utils/util'
  import { vReportUtil } from '@/utilsv/vUtil'
  import { postAction, ormSeqOrders } from '@/api/api'

  import MetaDomain from '@/store/models/MetaDomain'
  import MetaPageComponentField from '@/store/models/MetaPageComponentField'

  import GDynrptPop from '@/components/page_pop/GDynrptPop'
  import GFormPop from '@/components/page_pop/GFormPop'

  import labPartReportColsPopConfig from './configs/labPartReportColsPopConfig'
  import labPartDynrptMetaPopConfig from './configs/labPartDynrptMetaPopConfig'

  export default {
    name: "LabPartDynrpt",
    components: {
      ASpin,
      GDynrptPop,
      GFormPop,
    },
    /* eslint-disable */
    props: {
      sModel: {
        type: Object,
        required: true,
      },
      sContext: {
        type: Object,
        required: false,
        default: function(){
          return {}
        }
      }
    },
    data () {
      return {
        /** 配置&状态 */
        OTHER: OTHER,
        baseUrl: settings.BASE_URL,
        vReportUtil: vReportUtil,
        colsPopConfig: labPartReportColsPopConfig,
        metaPopConfig: labPartDynrptMetaPopConfig,
        loading:false,
        datColItems: [],       // 镜像cptFieldItems
        datRowItems: [],       // 镜像cptFieldItems
        datMeasureItems: [],   // 镜像cptFieldItems
        /** 查询&数据源 */
        queryRules: [],
        reportPagination: {
          current: 1,
          pageSize: 30,
          pageSizeOptions: ['30', '50', '100'],
          showTotal: (total, range) => {
            return range[0] + "-" + range[1] + " 共" + total + "条"
          },
          showQuickJumper: true,
          showSizeChanger: true,
          total: 0
        },
        reportDataSource: [],
        defaultDataSource: [],
      }
    },
    computed: {
      sExtend: function(){
        return converters.parseObj(this.sModel?.extend)
      },
      cptFieldItems: function(){
        let that = this
        let metaFields = MetaPageComponentField.query().where('metaPageComponentId', that.sModel?.id)
          .orderBy('seqOrder', 'asc').orderBy('id', 'asc').with('relMetaDomainField').get()
        let cloneItemFunc = vReportUtil.reportCloneItem
        let clonedMetaFields = metaFields.map(item => cloneItemFunc(that, item))
        that.datColItems = clonedMetaFields.filter(item => that._.get(item, 'extend.targetGroup')==='dynrptCol')
        that.datRowItems = clonedMetaFields.filter(item => that._.get(item, 'extend.targetGroup')==='dynrptRow')
        that.datMeasureItems = clonedMetaFields.filter(item => that._.get(item, 'extend.targetGroup')==='dynrptMeasure')
        return metaFields
      },
      cptDragRowTargetOptions: function() {
        return {
          animation: 200,
          group: 'row',
          ghostClass: 'ghost',
          handle: '.drag-widget',
        };
      },
      cptDragColTargetOptions: function() {
        return {
          animation: 200,
          group: 'col',
          ghostClass: 'ghost',
          handle: '.drag-widget',
        };
      },
      cptDragMeasureTargetOptions: function() {
        return {
          animation: 200,
          group: 'measure',
          ghostClass: 'ghost',
          handle: '.drag-widget',
        };
      },
    },
    methods: {
      /** Record操作 */
      doQuery(queryRules){
        this.queryRules = queryRules
        this._loadData(1)
      },
      _loadData(arg) {
        let that = this
        if (arg === 1) {
          that.reportPagination.current = 1;
        }
        let crossOn = that.sExtend['crossOn']
        let reportTitle = that.sExtend['reportTitle'] || '暂无标题'
        let reportUri = that._modelUri(crossOn?'reportCrosstab':'reportColumn')
        if(!reportUri){
          that.reportDataSource = that.defaultDataSource
          that.$message.warn('暂无查询调用路径')
          return
        }
        // 报表Columns
        let columnItems = []
        let rowItems = []
        let measureItems = []
        that.datColItems.forEach(item => {
          let resolvedColumn = that._resolveReportColumn(item)
          if(resolvedColumn){
            columnItems.push(resolvedColumn)
          }
        })
        that.datRowItems.forEach(item => {
          let resolvedColumn = that._resolveReportColumn(item)
          if(resolvedColumn){
            rowItems.push(resolvedColumn)
          }
        })
        that.datMeasureItems.forEach(item => {
          let resolvedColumn = that._resolveReportColumn(item)
          if(resolvedColumn){
            measureItems.push(resolvedColumn)
          }
        })
        // 查询条件
        let queryCondition = {
          pageNum: that.reportPagination.current,
          pageSize: that.reportPagination.pageSize,
          orderBy: null,
          condition: 'and',
          rules: []
        }
        if(!crossOn){
          let queryColumns = that._.cloneDeep(columnItems)
          queryColumns.push(...measureItems)
          queryCondition.rules.push({
            field: '_extend_columns',
            value: queryColumns,
            operator: QUERY.EQUAL,
          })
        }else{
          queryCondition.rules.push(
          {
            field: '_extend_reportTitle',
            value: reportTitle,
            operator: QUERY.EQUAL,
          },
          {
            field: '_extend_rows',
            value: rowItems,
            operator: QUERY.EQUAL,
          },
          {
            field: '_extend_columns',
            value: columnItems,
            operator: QUERY.EQUAL,
          },
          {
            field: '_extend_measures',
            value: measureItems,
            operator: QUERY.EQUAL,
          })
        }
        that.queryRules.forEach(rule => {
          queryCondition.rules.push(rule)
        })
        // 默认排序
        if(that._.isNil(queryCondition.orderBy)){
          if(rowItems.length>0){
            queryCondition.orderBy = `${rowItems[0]['field']}-asc`
          }
        }
        // 加载数据
        that.loading = true
        postAction(reportUri, queryCondition).then(resp => {
          that.loading = false
          if(resp.gmodel){
            that.reportDataSource = resp.gmodel
            that.reportPagination.total = resp.gmodel.length
          }

        }).catch(err => {
          that.loading = false
          that.reportDataSource = that.defaultDataSource
          vReportUtil.reportReqErr(that, err, '加载失败')
        })
      },
      /** Item操作 */
      handleItemDrag(changeEvent, targetGroup){
        let that = this
        // 不能用that.datFieldItems，拖拽时排序已经改变
        let metaFields = MetaPageComponentField.query().where('metaPageComponentId', that.sModel?.id)
          .orderBy('seqOrder', 'asc').orderBy('id', 'asc').get()
        let cloneItemFunc = vReportUtil.reportCloneItem
        let groupMetaFields = metaFields.map(item => cloneItemFunc(that, item)).filter(item => that._.get(item, 'extend.targetGroup')===targetGroup)
        let mappedItems = groupMetaFields.map(item => {
          return {
            id: item.id,
            title: item.title,
            seqOrder: item.seqOrder
          }
        })
        if(changeEvent['moved']){
          let oldIndex = that._.get(changeEvent, 'moved.oldIndex')
          let newIndex = that._.get(changeEvent, 'moved.newIndex')
          let item = mappedItems[oldIndex]
          mappedItems.splice(oldIndex, 1)
          mappedItems.splice(newIndex, 0, item)
          that._updateItemOrder(mappedItems)
        }
        if(changeEvent['added']){
          changeEvent || null
          // do nothing
        }
      },
      /** 工具方法 */
      _modelUri(uriType){
        let that = this
        let modelMetaDomainId = that.sModel?.metaDomainId
        let uriTypePath = uriType==='reportColumn'?'column':uriType==='reportCrosstab'?'crosstab':'none'
        if(modelMetaDomainId===0) { // 自定义组件
          let uriPrefix = that._.get(that.sExtend, 'uriPrefix')
          if(uriPrefix){
            uriPrefix = uriPrefix.startsWith('/')?uriPrefix.substring(1):uriPrefix
          }
          return (uriPrefix && uriTypePath)?`/${uriPrefix}/${uriTypePath}`:undefined
        }else{                    // 数据表组件
          let metaDomain = MetaDomain.find(modelMetaDomainId)
          return (metaDomain && uriTypePath)?`/rpt/${metaDomain.domainName}/${uriTypePath}`:undefined
        }
      },
      _resolveReportColumn(reportColumn){
        let that = this
        if(!reportColumn){ return }
        let format = that._.get(reportColumn, 'cfExtend.format')
        let projection = that._.get(reportColumn, 'cfExtend.projection')
        let resolvedColumn = {
          title: reportColumn.title,
          field: reportColumn.field,
          type: reportColumn.type,
          metaDomainFieldId: reportColumn.metaDomainFieldId,
          projection: projection,
        }
        if([FIELD.DATE].indexOf(reportColumn.type) !== -1){
          resolvedColumn['format'] = format
        }
        return resolvedColumn
      },
      _updateItemOrder(mappedItems){
        let itemOrders = []
        mappedItems.forEach((item, index) => {
          let itemData = {id: item.id, seqOrder: index+1}
          itemOrders.push(itemData)
        })
        ormSeqOrders('metaPageComponentField', {items: itemOrders}).then(resp => {
          let entitys = resp.gmodel
          MetaPageComponentField.insertOrUpdate({
            data: entitys
          })
        })
      },
    },
  }
</script>


<style lang="scss" scoped>
  .full-container{
    width: 100%;
    height: 100%;
    display: flex;
    .conr-wrapper-container,
    .cols-wrapper-container,
    .rows-wrapper-container,
    .measures-wrapper-container {
      display: flex;
      min-height: 30px;
    }
    .cols-wrapper-container,
    .measures-wrapper-container{
      flex: 1;
    }
    .conr-wrapper-container,
    .rows-wrapper-container{
      width: 165px;
      margin-right: 10px;
    }
    .conr-wrapper-container{
      .draggable-container{
        display: flex;
        flex-direction: row;
        justify-content: center;
        align-items: center;
        font-weight: bold;
      }
    }
    .rows-wrapper-container{
      .draggable-container{
        .draggable-columns-container{
          display: flex;
          flex-direction: column;
          justify-content: center;
          align-items: center;
        }
      }
    }
    .measures-wrapper-container{
      .draggable-container{
        .draggable-columns-container{
          align-items: center;
        }
      }
    }
  }
</style>