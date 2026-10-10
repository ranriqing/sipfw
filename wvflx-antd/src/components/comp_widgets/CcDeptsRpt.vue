<template>
  <a-card :bordered="false">
    <div class="chart-header">
      <span class="header-title">
        <a-icon type="line-chart" />
        {{ _.get(item, 'extend.title') || '部门日报' }}
      </span>
      <span class="header-action">
        <a @click="_loadData(1)" >
          <a-icon style="font-size: 14px;" type="reload" />
        </a>
      </span>
    </div>
    <div class="chart-body">
      <a-spin :spinning="loading">
        <div class="flex flex-row cross-start">
          <div class="body-right">
            <div ref="canvas"></div>
          </div>
        </div>
      </a-spin>
    </div>
  </a-card>
</template>

<script>
  import { stores, tools } from '@/utils/util'
  import { postAction } from '@/api/api'
  import { converters } from '@/utils/util'
  import { Column } from '@antv/g2plot';
  const DataSet = require('@antv/data-set')

  export default {
    name: "CcDeptRpt",
    components: {
    },
    props: {
      item: {
        type: Object,
        required: true,
      }
    },
    data() {
      return {
        /** 配置&状态 */
        stores: stores,
        converters: converters,
        loading: true,
        /** 查询&数据源 */
        queryRules: [],
        g2plot: null,
        plotData: [],
        todayCallInfo: {}
      }
    },
    methods: {
      /** 加载数据 */
      doQuery(queryRules){
        this.queryRules = queryRules
        this._loadData(1)
      },
      _loadData(renewChart) {
        let that = this
        // 查询条件
        let queryCondition = {
          paginate: false,
          condition: 'and',
          rules: [],
        }
        that.queryRules.forEach(rule => {
          queryCondition.rules.push(rule)
        })
        // 扩展条件
        let identifier = 'cc_depts_rpt'
        if(identifier){
          queryCondition.rules.push({
            field: '_extend_identifier',
            value: identifier,
            operator: 'equal',
          })
        }
        // 加载数据
        let block = true
        if(block){
          that.loading = false
          return
        }
        that.loading = true
        if(renewChart){
          if(that.g2plot){
            that.g2plot.destroy()
            that.g2plot = null
          }
        }
        postAction('/ccReport/chart', queryCondition).then(resp => {
          that.loading = false
          if(resp.gmodel){
            let cGmodel = that._.cloneDeep(resp.gmodel)
            that.plotData = that._transformData(cGmodel['groupedResult'])
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
        that.g2plot = new Column(
          that.$refs.canvas,
          {
            data: that.plotData,
            padding: 'auto',
            isGroup: true,
            xField: 'label',
            yField: 'value',
            seriesField: 'type',
            smooth: true,
            label: {
              position: 'middle', // 'top', 'middle', 'bottom'
              layout: [
                { type: 'interval-adjust-position' },
                { type: 'interval-hide-overlap' },
                { type: 'adjust-color' },
              ],
            },
            legend: {
              visible: true,
              position: 'bottom',
            },
            color: ['#5AD8A6','#5B8FF9','#5D7092','#F6BD16','#6F5EF9'],
          }
        )
        that.g2plot.render()
      },
      _transformData(sourceData){
        let dv = new DataSet.View().source(sourceData);
        let fields = []
        let firstObj = sourceData[0]
        if(firstObj){
          this._.forIn(firstObj, (value, key)=>{
            if(key !== 'label'){
              fields.push(key)
            }
          })
        }
        dv.transform({  // 展开
          type: 'fold',
          fields: fields,
          key: 'type',
          value: 'value',
        });
        return dv.rows;
      },
    },
    activated() {
      tools.sleep(500).then(()=>{
        this._loadData()
      })
    },
    mounted () {
      tools.sleep(500).then(()=>{
        this._loadData()
      })
    },
  }
</script>

<style lang="scss" scoped>
  .chart-header {
    position: relative;
    overflow: hidden;
    width: 100%;
    font-size: 14px;
    line-height: 22px;
    margin-bottom: 15px;
    .header-action {
      cursor: pointer;
      position: absolute;
      top: 0;
      right: 0;
    }
  }
  .chart-body{
    .body-left{
      padding: 15px;
      width: 300px;
      flex: 0 0 auto;
      &.mobile{
        width: 100%;
        flex: 1 1 auto;
      }
    }
    .body-right{
      padding: 15px;
      flex: 1 1 auto;
    }
  }
</style>
