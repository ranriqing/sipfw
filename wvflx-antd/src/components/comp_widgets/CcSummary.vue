<template>
  <a-card :bordered="false">
    <div class="chart-header">
      <span class="header-title">
        <a-icon type="line-chart" />
        {{ _.get(item, 'extend.title') || '我的通话' }}
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
          <div :class="['body-left', stores.isMobile() && 'mobile']">
            <div style="margin: 20px 0;">
              <span>今日通话</span>
              <span style="margin-left: 8px; font-size: 16px;" class="warning">{{ _.get(todayCallInfo, 'dateStr') }}</span>
            </div>
            <a-divider/>
            <div style="margin: 15px 0;">
              {{ tools.isApply95()?'接听数量':'通话数量' }}
            </div>
            <div class="flex flex-row main-start" style="margin: 15px 0;">
              <div style="margin-right: 30px;">
                <a-progress
                  :percent="_bridgeRate()"
                  strokeColor="#52c41a"
                  type="dashboard"
                  status="normal"
                  :width="60">
                </a-progress>
              </div>
              <div class="flex flex-column cross-center">
                <span class="text-tips" style="margin-bottom: 4px; font-size: 14px;">接通/{{ tools.isApply95()?'总数':'呼叫' }}</span>
                <div style="font-size: 20px; ">
                  <span style="color: #5AD8A6">{{ _.get(todayCallInfo, 'bridged') || 0 }}</span>
                  /<span style="color: #5B8FF9">{{ _.get(todayCallInfo, 'total') || 0 }}</span>
                </div>
              </div>
            </div>
            <a-divider/>
            <div style="margin: 15px 0;">
              通话时长
            </div>
            <div class="flex flex-row main-start" style="margin: 15px 0;">
              <div style="margin-right: 30px;">
                <a-progress
                  :percent="_talkRate()"
                  strokeColor="#52c41a"
                  type="dashboard"
                  status="normal"
                  :width="60">
                </a-progress>
              </div>
              <div class="flex flex-column cross-center" style="font-size: 14px;">
                <div>
                  <span class="text-tips">通话</span>
                  <span style="color: #5AD8A6; margin-left: 8px;">{{ _formatDuring(_.get(todayCallInfo, 'bridgesec')) }}</span>
                </div>
                <div style="margin-top: 8px;">
                  <span class="text-tips">持续</span>
                  <span style="color: #5B8FF9; margin-left: 8px;">{{ _formatDuring(_.get(todayCallInfo, 'duration')) }}</span>
                </div>
              </div>
            </div>
          </div>
          <div v-if="stores.isDesktop()" class="body-right">
            <div ref="canvas"></div>
          </div>
        </div>
      </a-spin>
    </div>
  </a-card>
</template>

<script>
  import ALayout from "ant-design-vue/lib/layout"
  import AProgress from "ant-design-vue/lib/progress"

  import { stores, tools } from '@/utils/util'
  import { postAction } from '@/api/api'
  import { converters } from '@/utils/util'
  import { Line } from '@antv/g2plot';
  const DataSet = require('@antv/data-set')

  export default {
    name: "CcSummary",
    components: {
      ALayout: ALayout,
      ALayoutSider: ALayout.Sider,
      ALayoutContent: ALayout.Content,
      AProgress,
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
        tools: tools,
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
        let identifier = 'cc_duration'
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
            that.todayCallInfo = cGmodel['todayCallInfo']
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
        that.g2plot = new Line(
          that.$refs.canvas,
          {
            data: that.plotData,
            padding: 'auto',
            xField: 'label',
            yField: 'value',
            seriesField: 'type',
            smooth: true,
            tooltip: {
              formatter:(datum)=>{
                let type = datum['type'] || ''
                if(type.includes('时长')){
                  return { name: type, value: that._formatDuring(datum['value']) };
                }else{
                  return { name: type, value: datum['value'] };
                }
              }
            },
            legend: {
              visible: true,
              position: 'bottom',
            },
            // @antv_g2@4.1.14@@antv\g2\lib\theme\style-sheet\light.js
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
      _formatDuring(second) {
        second = second || 0
        let hours = parseInt(second / (60 * 60))
        let minutes = parseInt((second % (60 * 60)) /60)
        let seconds = second % 60
        return `${hours<10?'0'+hours:hours}:${minutes<10?'0'+minutes:minutes}:${seconds<10?'0'+seconds:seconds}`
      },
      _bridgeRate(){
        let that = this
        let bridgeCount = that.todayCallInfo['bridged'] || 0
        let totalCount = that.todayCallInfo['total'] || 0
        let percent = totalCount>0?that.Decimal.div(bridgeCount, totalCount).toDP(2).toNumber():0
        return that.Decimal.mul(percent, 100).toDP(0)
      },
      _talkRate(){
        let that = this
        let bridgeCount = that.todayCallInfo['bridgesec'] || 0
        let totalCount = that.todayCallInfo['duration'] || 0
        let percent = totalCount>0?that.Decimal.div(bridgeCount, totalCount).toDP(2).toNumber():0
        return that.Decimal.mul(percent, 100).toDP(0)
      }
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
