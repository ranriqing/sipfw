<template>
  <a-card :bordered="false" class="page-block tmpl-block">
    <a-table
      :rowKey="(record, index) => { return index }"
      :loading="loading"
      :scroll="{ x: true }"
      :columns="datReportColumns"
      :dataSource="reportDataSource"
      :pagination="reportPagination"
      :data-cptReportColumns="cptReportColumns"
      @change="onReportChange">
      <template slot="title">
        <!-- 图表展示 -->
        <div v-if="_.get(sExtend, 'chartConfig.chartOn')" class="chart-wrapper">
          <div class="chart-title">{{ _.get(sExtend, 'chartConfig.chartTitle') }}</div>
          <a-spin :spinning="chartLoading">
            <div ref="canvas" :style="{display: 'block'}"></div>
          </a-spin>
        </div>
        <!-- 操作按钮 -->
        <div class="report-operation-buttons">
          <div class="config-wrapper">
            <!-- 图表定义 -->
            <a-button size="small" @click="handleChartDesign()">
              <a-icon type="pie-chart"/>图表
            </a-button>
            <a-divider type="vertical" />
            <!-- 报表列 -->
            <a-button size="small" @click="vReportUtil.handleColsConfig(_self)">
              <a-icon type="setting"/>报表列
            </a-button>
          </div>
        </div>
        <!-- 拖拽表头 -->
        <div class="draggable-container draggable-report-container">
          <draggable
            tag="div"
            class="draggable-columns-container"
            :list="datReportColumns"
            v-bind="vReportUtil.cptDragTargetOptions()"
            @change="(changeEvent) => vReportUtil.handleItemDrag(_self, changeEvent)">
            <div
              class="draggable-column"
              v-for="(item, index) in datReportColumns"
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
      </template>
    </a-table>

    <!-- Ref弹窗 -->
    <GChartPop
      ref="refChart"
      :configObj="chartPopConfig">
    </GChartPop>
    <GColsPop
      ref="refCols"
      :configObj="colsPopConfig">
    </GColsPop>
    <GFormPop
      ref="refMeta"
      :configObj="metaPopConfig">
    </GFormPop>
  </a-card>
</template>

<!--
# 组件说明
  Lab报表组件
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
  import { Line, Area, Column, Bar, Pie } from '@antv/g2plot';

  import MetaDomain from '@/store/models/MetaDomain'
  import MetaPageComponent from '@/store/models/MetaPageComponent'
  import MetaPageComponentField from '@/store/models/MetaPageComponentField'

  import GColsPop from '@/components/page_pop/GColsPop'
  import GFormPop from '@/components/page_pop/GFormPop'
  import GChartPop from '@/components/page_pop/GChartPop'

  import labPartReportChartPopConfig from './configs/labPartReportChartPopConfig'
  import labPartReportColsPopConfig from './configs/labPartReportColsPopConfig'
  import labPartReportMetaPopConfig from './configs/labPartReportMetaPopConfig'

  export default {
    name: "LabPartReport",
    components: {
      ASpin,
      GChartPop,
      GColsPop,
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
        chartPopConfig: labPartReportChartPopConfig,
        colsPopConfig: labPartReportColsPopConfig,
        metaPopConfig: labPartReportMetaPopConfig,
        loading:false,
        chartLoading:false,
        datReportColumns: [],
        /** 查询&数据源 */
        queryRules: [],
        reportSorter: {},
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
      cptReportColumns: function(){
        let that = this
        let metaFields = MetaPageComponentField.query().where('metaPageComponentId', that.sModel?.id)
          .orderBy('seqOrder', 'asc').orderBy('id', 'asc').with('relMetaDomainField').get()
        let cloneItemFunc = vReportUtil.reportCloneItem
        that.datReportColumns = metaFields.map(item => cloneItemFunc(that, item, that.$createElement))
        return metaFields
      },
    },
    methods: {
      /** Record操作 */
      doQuery(queryRules){
        this.queryRules = queryRules
        this._loadData(1)
        this._loadChartData(true)
      },
      onReportChange(pagination, filters, sorter) {
        this.reportPagination = pagination
        this.reportSorter = sorter
        this._loadData()
      },
      _loadData(arg) {
        let that = this
        if (arg === 1) {
          that.reportPagination.current = 1;
        }
        let reportUri = that._modelUri('reportUri')
        if(!reportUri){
          that.reportDataSource = that.defaultDataSource
          that.$message.warn('暂无查询调用路径')
          return
        }
        // 报表Columns
        let reportColumns = []
        that.datReportColumns.forEach(item => {
          let resolvedColumn = that._resolveReportColumn(item)
          if(resolvedColumn){
            reportColumns.push(resolvedColumn)
          }
        })
        // 查询条件
        let queryCondition = {
          pageNum: that.reportPagination.current,
          pageSize: that.reportPagination.pageSize,
          orderBy: null,
          condition: 'and',
          rules: [{
            field: '_extend_columns',
            value: reportColumns,
            operator: QUERY.EQUAL,
          }]
        }
        that.queryRules.forEach(rule => {
          queryCondition.rules.push(rule)
        })
        // 排序参数
        if (Object.keys(that.reportSorter).length > 0) {
          let sort = that.reportSorter.field;
          let order = that.reportSorter.order;
          if(sort && order){
            order ="ascend" === that.reportSorter.order ? "asc" : "desc"
            queryCondition.orderBy = `${sort}-${order}`
          }
        }
        // 默认排序
        if(that._.isNil(queryCondition.orderBy)){
          if(reportColumns.length>0){
            queryCondition.orderBy = `${reportColumns[0]['field']}-asc`
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
      _loadChartData(renewChart) {
        let that = this
        let reportUri = that._modelUri('reportUri')
        if(!reportUri){
          return
        }
        // 图表Columns
        let chartReportColumns = []
        let chartConfig = that.sExtend['chartConfig'] || {}
        let chartOn = chartConfig['chartOn']
        if(!chartOn){
          return
        }
        // let chartTitle = chartConfig['chartTitle']
        // let chartType = chartConfig['chartType']
        let chartGroup = that._resolveChartColumn(chartConfig['chartGroup'])
        let chartSeries = that._resolveChartColumn(chartConfig['chartSeries'])
        let chartMeasure = that._resolveChartColumn(chartConfig['chartMeasure'])
        if(chartGroup){ chartReportColumns.push(chartGroup) }
        if(chartSeries){ chartReportColumns.push(chartSeries) }
        if(chartMeasure){ chartReportColumns.push(chartMeasure) }
        // 查询条件
        let queryCondition = {
          paginate: false,
          orderBy: null,
          condition: 'and',
          rules: [{
            field: '_extend_columns',
            value: chartReportColumns,
            operator: QUERY.EQUAL,
          }]
        }
        that.queryRules.forEach(rule => {
          queryCondition.rules.push(rule)
        })
        // 排序参数
        if(chartReportColumns.length>0){
          queryCondition.orderBy = `${chartReportColumns[0]['field']}-asc`
        }
        // 加载数据
        if(renewChart){
          if(that.g2plot){
            that.g2plot.destroy()
            that.g2plot = null
          }
        }
        that.chartLoading = true
        postAction(reportUri, queryCondition).then(resp => {
          that.chartLoading = false
          if(resp.gmodel && resp.gmodel){
            let chartDataSource = resp.gmodel
            that.plotData = that._transformData(chartDataSource)
            if(!that.g2plot){
              that._initG2plot()
            }else{
              that.g2plot.changeData(that.plotData)
            }
          }
        }).catch(err => {
          that.chartLoading = false
          vReportUtil.reportReqErr(that, err, '加载失败')
        })
      },
      /** 设计图表 */
      handleChartDesign(){
        let that = this
        // 报表Columns
        let reportColumns = []
        that.datReportColumns.forEach(item => {
          let resolvedColumn = that._resolveReportColumn(item)
          if(resolvedColumn){
            reportColumns.push(resolvedColumn)
          }
        })
        // 展示设计弹窗
        let extendObj = converters.parseObj(that.sModel?.extend)
        let chartConfig = extendObj['chartConfig'] || {}
        let slotRef = that.$refs.refChart
        slotRef.showPop({
            chartOn: chartConfig['chartOn'],
            chartTitle: chartConfig['chartTitle'],
            chartType: chartConfig['chartType'],
            chartGroup: chartConfig['chartGroup'],
            chartSeries: chartConfig['chartSeries'],
            chartMeasure: chartConfig['chartMeasure'],
          }, {
            reportChart: true,
            reportColumns: reportColumns,
            targetModel: that.sModel,
          },
          (record, context) => { /* submit */
            context || true
            if(that.sModel){
              extendObj['chartConfig'] = {
                chartOn: record['chartOn'],
                chartTitle: record['chartTitle'],
                chartType: record['chartType'],
                chartGroup: record['chartGroup'],
                chartSeries: record['chartSeries'],
                chartMeasure: record['chartMeasure'],
              }
              let promise = MetaPageComponent.$update({
                params: {
                  id: that.sModel?.id,
                },
                data: {
                  extend: converters.encodeObj(extendObj)
                }
              })
              promise.then(() => {
                // 加载元信息
                that.$emit('metaModel');
                that.$nextTick(() => {
                  that._loadChartData(true)
                })
              })
              return promise
            }
          }, () => { /* cancel */
            /* do nothing */
          }
        )
      },
      /** 工具方法 */
      _modelUri(uriType){
        let that = this
        let modelMetaDomainId = that.sModel?.metaDomainId
        let uriTypePath = uriType==='reportUri'?'report':'none'
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
      _queryCondition(){
        let that = this
        let queryCondition = {
          orderBy: null,
          condition: 'and',
          rules: [],
        }
        that.queryRules.forEach(rule => {
          queryCondition.rules.push(rule)
        })
        return queryCondition
      },
      _initG2plot(){
        let that = this
        if(!that.$refs.canvas){
          return
        }
        // 读取配置
        let chartConfig = that.sExtend['chartConfig'] || {}
        let chartType = chartConfig['chartType']
        let chartGroup = that._resolveChartColumn(chartConfig['chartGroup']) || {field: 'groupField', title: '分组'}
        let chartSeries = that._resolveChartColumn(chartConfig['chartSeries'])
        let chartMeasure = that._resolveChartColumn(chartConfig['chartMeasure']) || {field: 'measureField', title: '计算'}
        // Meta对象
        let metaObj = {}
        metaObj[chartGroup.field] = { alias: chartGroup.title, }
        metaObj[chartMeasure.field] = { alias: chartMeasure.title, }
        if(chartSeries){
          metaObj[chartSeries.field] = { alias: chartSeries.title, }
        }
        // 配置图表
        let chartMap = { area: Area, column: Column, bar: Bar, line: Line, pie: Pie }
        let chartTypeClass = chartMap[chartType] || Line
        let optionsObj = {
          xField: chartGroup.field,
          yField: chartMeasure.field,
          seriesField: chartSeries?.field,
          isGroup: true,
        }
        if(chartType === 'pie'){
          optionsObj = {
            radius: 1,
            innerRadius: 0.6,
            angleField: chartMeasure.field,
            colorField: chartGroup.field,
            label: {
              formatter:(datum)=>{ return `${datum[chartGroup.field]}: ${(datum['percent']*100).toFixed(2)}%` }
            },
            statistic: {
              title: false,
              content: false,
            },
          }
        }else if(chartType === 'bar'){
          optionsObj = {
            xField: chartMeasure.field,
            yField: chartGroup.field,
            seriesField: chartSeries?.field,
            isGroup: true,
          }
        }
        // 生成图表
        that.g2plot = new chartTypeClass(
          that.$refs.canvas,
          Object.assign({
            data: that.plotData,
            padding: [60, 50, 60, 50],
            /*xField: 'xField',
            yField: 'yField',
            seriesField: 'seriesField',
            isGroup: true, */
            smooth: true,
            meta: metaObj,
            legend: {
              visible: true,
              position: 'bottom',
            },
          }, optionsObj)
        )
        that.g2plot.render()
      },
      _transformData(sourceData){
        let that = this
        sourceData.forEach(item => {
          that._.forIn(item, (value, key)=>{
            if(that._.isNil(value)){
              item[key] = 'none'
            }
          })
        })
        return sourceData
      },
      _resolveChartColumn(chartColumn){
        let that = this
        let reportColumn = that.datReportColumns.find(item => item['field'] === chartColumn?.field )
        return that._resolveReportColumn(reportColumn)
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

<style lang="scss" scoped>
  .chart-wrapper{
    margin-bottom: 18px;
    .chart-title{
      font-weight: bold;
      font-size: 15px;
      margin-bottom: 14px;
    }
  }
</style>