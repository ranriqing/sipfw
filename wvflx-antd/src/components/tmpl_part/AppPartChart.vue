<template>
  <a-card :bordered="false" class="page-chart page-block">
    <div v-if="_.get(sExtend, 'chartType') !== 'overview'" class="chart-header">
      <span class="header-title">
        {{ sExtend['chartTitle'] || '图表标题' }}
      </span>
      <span class="header-action">
        <a @click="_loadData(1)" >
          <a-icon style="font-size: 14px;" type="reload" />
        </a>
      </span>
    </div>
    <div v-if="_.get(sExtend, 'chartType') === 'map'" class="chart-body">
      <ChinaMapPlot
        ref="refMap"
        :mapUri="_modelMapUri()"
        :chartAddrField="sExtend['chartAddrField']"
        :chartSeries="sExtend['chartSeries']">
      </ChinaMapPlot>
    </div>
    <div v-else-if="_.get(sExtend, 'chartType') === 'overview'" class="chart-body">
      <a-spin :spinning="loading">
        <OverviewPlot
          :dataSource="overviewData"
          :chartGroup="sExtend['chartGroup']"
          :chartMeasure="sExtend['chartMeasure']"
          :overviewSumTitle="sExtend['overviewSumTitle']"
          :overviewUnit="sExtend['overviewUnit']"
          :overviewShowImage="sExtend['overviewShowImage']"
          :overviewImageMap="sExtend['overviewImageMap']">
        </OverviewPlot>
      </a-spin>
      <div v-if="!_modelChartUri()" class="overlay">
        <span class="text-tips">图表暂无配置</span>
      </div>
    </div>
    <div v-else :class="{'chart-body':true, 'empty-chart': !_modelChartUri()}">
      <a-spin :spinning="loading">
        <div ref="canvas" :style="{display: 'block'}"></div>
      </a-spin>
      <div v-if="!_modelChartUri()" class="overlay">
        <span class="text-tips">图表暂无配置</span>
      </div>
    </div>
  </a-card>
</template>

<!--
# 组件说明
  Lab图表组件
# 配置组件
  sModel: Object        // 当前组件SModel
  sContext: Object      // 上下文sContext
# 操作方法
  doQuery(queryRules)
-->

<script>
  import ASpin from 'ant-design-vue/lib/spin';

  import { QUERY, FIELD, DATE } from '@/constants'
  import { postAction, reportFakeData } from '@/api/api'
  import { converters } from '@/utils/util'
  import { Line, Area, Column, Bar, Pie } from '@antv/g2plot';

  import MetaDomain from '@/store/models/MetaDomain'
  import ChinaMapPlot from "@/components/comp_widgets/ChinaMapPlot.vue";
  import OverviewPlot from "@/components/comp_widgets/OverviewPlot.vue";

  export default {
    name: "AppPartChart",
    components: {
      ChinaMapPlot,
      OverviewPlot,
      ASpin,
    },
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
    data() {
      return {
        /** 配置&状态 */
        converters: converters,
        loading: false,
        /** 查询&数据源 */
        queryRules: [],
        g2plot: null,
        plotData: [],
        overviewData: [],
      };
    },
    computed: {
      sExtend: function() {
        return converters.parseObj(this.sModel?.extend)
      },
    },
    methods: {
      /** 加载数据 */
      doQuery(queryRules){
        this.queryRules = queryRules
        this._loadData(1)
      },
      _loadData(renewChart) {
        let that = this
        let chartType = that._.get(that.sExtend, 'chartType')
        // 加载地图
        if(chartType === 'map'){
          let refMap = that.$refs['refMap']
          if(that._.isFunction(refMap?._loadData)){
            refMap?._loadData(true)
          }
          return;
        }
        // 图表Columns
        let chartColumns = []
        let chartGroup = that.sExtend['chartGroup']
        let chartSeries = that.sExtend['chartSeries']
        let chartMeasure = that.sExtend['chartMeasure']
        if(chartGroup){ chartColumns.push(chartGroup) }
        if(chartSeries && chartType !== 'overview'){ chartColumns.push(chartSeries) }
        if(chartMeasure){ chartColumns.push(chartMeasure) }
        // 查询条件
        let queryCondition = {
          paginate: false,
          pageNum: undefined,
          pageSize: undefined,
          orderBy: null,
          condition: 'and',
          rules: [{
            field: '_extend_columns',
            value: chartColumns,
            operator: QUERY.EQUAL,
          }]
        }
        that.queryRules.forEach(rule => {
          queryCondition.rules.push(rule)
        })
        let filterRules = that.sExtend['filterRules'] || []
        filterRules.forEach(filterRule => {
          let rule = that._resolveFilterRule(filterRule)
          if(rule){ queryCondition.rules.push(rule) }
        })
        // 排序参数
        if(chartColumns.length>0){
          queryCondition.orderBy = `${chartColumns[0]['field']}-asc`
        }
        // 加载数据
        that.loading = true
        if(renewChart){
          if(that.g2plot){
            that.g2plot.destroy()
            that.g2plot = null
          }
        }
        let chartUri = that._modelChartUri()
        let realPostAction = chartUri?postAction:reportFakeData
        realPostAction(chartUri, queryCondition).then(resp => {
          that.loading = false
          if(resp.gmodel){
            let chartDataSource = resp.gmodel
            if(chartType === 'overview'){
              that.overviewData = chartDataSource
            }else{
              that.plotData = that._transformData(chartDataSource)
              if(!that.g2plot){
                that._initG2plot()
              }else{
                that.g2plot.changeData(that.plotData)
              }
            }
          }
        }).catch(err => {
          that.loading = false
          if(!err.hcode){
            that.$message.error(`[${err.code}]加载失败: ${err.message}`)
          }
        })
      },
      /** 工具方法 */
      _initG2plot(){
        let that = this
        if(!that.$refs.canvas){
          return
        }
        // 读取配置
        let chartType = that.sExtend['chartType']
        let chartGroup = that.sExtend['chartGroup'] || {field: 'groupField', title: '分组'}
        let chartSeries = that.sExtend['chartSeries']
        let chartMeasure = that.sExtend['chartMeasure'] || {field: 'measureField', title: '计算'}
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
      _resolveFilterRule(filterRule){
        let that = this
        let ruleField = filterRule['derivedFieldName']
        let ruleFieldType = filterRule['derivedFieldType']
        let ruleValue = filterRule['ruleValue']
        let ruleOperator = filterRule['ruleOperator']
        // 日期条件处理
        if(ruleFieldType === FIELD.DATE){
          if(ruleOperator === '_relative'){
            // 参考 GChartPop.cptRelativeObj
            if(['recent7', 'recent15', 'recent30'].indexOf(ruleValue) !== -1){
              ruleOperator = QUERY.GREATER
              if(ruleValue === 'recent7'){
                ruleValue = that.$moment().subtract(6, 'days').startOf('day').format(DATE.YYYYMMDD)
              }else if(ruleValue === 'recent15'){
                ruleValue = that.$moment().subtract(14, 'days').startOf('day').format(DATE.YYYYMMDD)
              }else if(ruleValue === 'recent30'){
                ruleValue = that.$moment().subtract(29, 'days').startOf('day').format(DATE.YYYYMMDD)
              }
            }else if(['currentDay', 'currentWeek', 'currentMonth', 'currentYear'].indexOf(ruleValue) !== -1){
              ruleOperator = QUERY.BETWEEN
              if(ruleValue === 'currentDay'){
                ruleValue = [that.$moment().startOf('day').format(DATE.YYYYMMDD), that.$moment().endOf('day').format(DATE.YYYYMMDD)]
              }else if(ruleValue === 'currentWeek'){
                ruleValue = [that.$moment().startOf('week').format(DATE.YYYYMMDD), that.$moment().endOf('week').format(DATE.YYYYMMDD)]
              }else if(ruleValue === 'currentMonth'){
                ruleValue = [that.$moment().startOf('month').format(DATE.YYYYMMDD), that.$moment().endOf('month').format(DATE.YYYYMMDD)]
              }else if(ruleValue === 'currentYear'){
                ruleValue = [that.$moment().startOf('year').format(DATE.YYYYMMDD), that.$moment().endOf('year').format(DATE.YYYYMMDD)]
              }
            }
          }
        }
        if(ruleField && ruleOperator && !that._.isNil(ruleValue)){
          return {
            field: ruleField,
            value: ruleValue,
            operator: ruleOperator,
          }
        }
      },
      _modelChartUri(){
        let that = this
        let modelMetaDomainId = that.sModel?.metaDomainId
        if(modelMetaDomainId===0) { // 自定义组件
          let uriPrefix = that._.get(that.sExtend, 'uriPrefix')
          if(uriPrefix){
            uriPrefix = uriPrefix.startsWith('/')?uriPrefix.substring(1):uriPrefix
          }
          return (uriPrefix)?`/${uriPrefix}/report`:undefined
        }else{                    // 数据表组件
          let metaDomain = MetaDomain.find(modelMetaDomainId)
          return (metaDomain)?`/rpt/${metaDomain.domainName}/report`:undefined
        }
      },
      _modelMapUri(){
        let that = this
        let modelMetaDomainId = that.sModel?.metaDomainId
        if(modelMetaDomainId===0) { // 自定义组件
          let uriPrefix = that._.get(that.sExtend, 'uriPrefix')
          if(uriPrefix){
            uriPrefix = uriPrefix.startsWith('/')?uriPrefix.substring(1):uriPrefix
          }
          return (uriPrefix)?`/${uriPrefix}/chinaMap`:undefined
        }else{
          let metaDomain = MetaDomain.find(modelMetaDomainId)
          return (metaDomain)?`/rpt/${metaDomain.domainName}/chinaMap`:undefined
        }
      },
    },
  }
</script>

<style lang="scss" scoped>
  .page-chart{
    .chart-body{
      margin-top: 6px;
    }
    .empty-chart{
      position: relative;
      min-height: 400px;
      .overlay{
        position: absolute;
        left: 0;
        top: 0;
        width: 100%;
        height: 100%;
        z-index: 20;
        border-radius: 5px;
        background-color: rgba(160, 207, 255,.1);
        display: flex;
        flex-direction: row;
        justify-content: center;
        align-items: center;
      }
    }
  }
</style>
