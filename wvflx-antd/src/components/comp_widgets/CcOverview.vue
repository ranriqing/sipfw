<template>
  <div>
    <a-row :gutter="[12, 0]">
      <a-col :md="8" :sm="24">
        <GChartCard :loading="loading" :title="tools.isApply95()?'今日呼入':'今日通话'">
          <template slot="total">
            <span class="success">{{ _.get(callCenterInfo, 'todayCalls.bridged') || 0 }}</span>
            <span style="font-size: 16px;">/ {{ _.get(callCenterInfo, 'todayCalls.total') || 0 }}</span>
          </template>
        </GChartCard>
      </a-col>
      <a-col :md="8" :sm="24">
        <GChartCard :loading="loading" title="在线分机">
          <template slot="total">
            <span class="success">{{ _.get(callCenterInfo, 'sipAccountNums.online') || 0 }}</span>
            <span style="font-size: 16px;">/ {{ _.get(callCenterInfo, 'sipAccountNums.total') || 0 }}</span>
          </template>
        </GChartCard>
      </a-col>
      <a-col :md="8" :sm="24">
        <GChartCard :loading="loading" :title="tools.isApply95()?'在线外线':'可用外线'">
          <template slot="total">
            <span class="success">{{ _.get(callCenterInfo, 'gatewayNums.online') || 0 }}</span>
            <span style="font-size: 16px;">/ {{ _.get(callCenterInfo, 'gatewayNums.total') || 0 }}</span>
          </template>
        </GChartCard>
      </a-col>
    </a-row>
  </div>
</template>

<script>
  import { tools } from '@/utils/util'
  import { postAction } from '@/api/api'
  import { converters } from '@/utils/util'

  import GChartCard from '@/components/comp_chart/GChartCard'


  export default {
    name: "CcOverview",
    components: {
      GChartCard,
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
        converters: converters,
        loading: true,
        /** 查询&数据源 */
        queryRules: [],
        callCenterInfo: {},
      };
    },
    methods: {
      /** 加载数据 */
      doQuery(queryRules){
        this.queryRules = queryRules
        this._loadData(1)
      },
      _loadData(renewChart) {
        let that = this
        renewChart || false
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
        let identifier = 'cc_info'
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
        postAction('/ccReport/chart', queryCondition).then(resp => {
          that.loading = false
          if(resp.gmodel){
            that.callCenterInfo = that._.cloneDeep(resp.gmodel)
          }
        }).catch(err => {
          that.loading = false
          if(!err.hcode){
            that.$message.error(`[${err.code}]加载失败: ${err.message}`)
          }
        })
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
</style>
