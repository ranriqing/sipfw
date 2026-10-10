<template>
  <a-card :bordered="false" class="page-chart page-block">
    <div class="chart-header">
      <span class="header-title">
        {{ sExtend['chartTitle'] || '图表标题' }}
      </span>
      <span class="header-action">
        <a @click="_loadData(1)" >
          <a-icon style="font-size: 14px;" type="reload" />
        </a>
      </span>
    </div>
    <div :class="{'chart-body':true, 'empty-chart': !_modelChartUri()}">
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
  Demo图表组件
# 配置组件
  sModel: Object        // 当前组件SModel
  sContext: Object      // 上下文sContext
# 操作方法
  doQuery(queryRules)
-->

<script>
  import ASpin from 'ant-design-vue/lib/spin';

  import { QUERY } from '@/constants'
  import { postAction, reportFakeData } from '@/api/api'
  import { tools, converters } from '@/utils/util'
  import { Line, Area, Column, Bar, Pie } from '@antv/g2plot';

  export default {
    name: "ZzDemoChart",
    components: {
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
        /** 图表&字段 */
        chartType: 'area', // area, column, bar, line, pie
        chartUri: '/zzDemo/chartData' && undefined,
        chartIdentifier: undefined,
        chartGroup: {
          field: 'name',
          title: '日期',
        },
        chartMeasure: {
          field: 'value',
          title: '数量',
        },
        chartSeries: {
          field: 'series',
          title: '分类',
        } && undefined,
        /** 查询&数据源 */
        queryRules: [],
        g2plot: null,
        plotData: [],
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
        this.queryRules = queryRules || []
        this._loadData(1)
      },
      _loadData(renewChart) {
        let that = this
        // 图表Columns
        let chartColumns = []
        if(that.chartGroup){ chartColumns.push(that.chartGroup) }
        if(that.chartMeasure){ chartColumns.push(that.chartMeasure) }
        if(that.chartSeries){ chartColumns.push(that.chartSeries) }
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
        // 扩展条件
        let identifier = that.chartIdentifier
        if(identifier){
          queryCondition.rules.push({
            field: '_extend_identifier',
            value: identifier,
            operator: 'equal',
          })
        }
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
        let realPostAction = that.chartUri?postAction:reportFakeData
        realPostAction(that.chartUri, queryCondition).then(resp => {
          that.loading = false
          if(resp.gmodel && resp.gmodel.entityList){
            let chartDataSource = resp.gmodel.entityList
            that.plotData = that._transformData(chartDataSource)
            if(!that.g2plot){
              that._initG2plot()
            }else{
              that.g2plot.changeData(that.plotData)
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
        // 图表配置
        let chartGroup = that.chartGroup
        let chartMeasure = that.chartMeasure
        let chartSeries = that.characterSet
        // Meta对象
        let metaObj = {}
        metaObj[chartGroup.field] = { alias: chartGroup.title, }
        metaObj[chartMeasure.field] = { alias: chartMeasure.title, }
        if(chartSeries){
          metaObj[chartSeries.field] = { alias: chartSeries.title, }
        }
        // 配置图表
        let chartMap = { area: Area, column: Column, bar: Bar, line: Line, pie: Pie }
        let chartTypeClass = chartMap[that.chartType] || Line
        let optionsObj = {
          xField: chartGroup.field,
          yField: chartMeasure.field,
          seriesField: chartSeries?.field,
          isGroup: true,
        }
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
            theme: {
              styleSheet: {
                brandColor: '#33a865',
                paletteQualitative10: [
                  '#216d41', '#2a8b53',
                  '#33a865', '#3cc677',
                  '#6dd499', '#b9d5b2',
                  '#84b29e', '#568f8b',
                  '#326b77', '#1b485e',
                  /*'#0E8E89', '#FF6B3B',
                  '#FFC100', '#9FB40F',
                  '#76523B', '#DAD5B5',
                  '#F383A2', '#247FEA',
                  '#626681', '#E19348',*/
                ]
              },
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
    },
    mounted () {
      tools.sleep(500).then(()=>{
        this._loadData()
      })
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
