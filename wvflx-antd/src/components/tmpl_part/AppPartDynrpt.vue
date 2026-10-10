<template>
  <a-card :bordered="false" class="page-block" :data-cptFieldItems="cptFieldItems">
    <div class="report-container">
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
  import { postAction } from '@/api/api'

  import MetaDomain from '@/store/models/MetaDomain'
  import MetaPageComponentField from '@/store/models/MetaPageComponentField'

  export default {
    name: "AppPartDynrpt",
    components: {
      ASpin,
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
    },
  }
</script>

