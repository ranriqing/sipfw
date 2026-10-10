<template>
  <div style="height: 100vh;">
    <div class="dataScreen-container">
      <div class="dataScreen" ref="dataScreenRef">
        <a-layout class="layout">
          <a-layout-header>
            <div class="header">
              <div class="header-left">
                <div class="header-title flex flex-row cross-center">
                  <img src="~@/assets/sip_logo.png" width="20" alt="logo" style="margin-right: 10px;">
                  <div v-if="!wsConnected" style="margin-right: 8px;">
                    <a-icon type="loading" />
                  </div>
                  <div>{{ 'SIP 实时监控' | Ellipsis(28) }}</div>
                </div>
              </div>
              <div class="header-right">
                <div class="header-kpis" v-if="stores.isDesktop()">
                  <div class="kpi-item" :title="`处理中 ${summary.handling | SipPrettyInteger}`">
                    <div class="kpi-label">处理中</div>
                    <div class="kpi-value primary-dark">{{ summary.handling | SipPrettyInteger }}</div>
                  </div>
                  <div class="kpi-item" :title="`应答率 ${summary.totalAnswerRate}`">
                    <div class="kpi-label">应答率</div>
                    <div class="kpi-value success-dark">{{ summary.totalAnswerRate }}</div>
                  </div>
                  <div class="kpi-item" :title="`总应答 ${summary.totalAnswered | SipPrettyInteger}`">
                    <div class="kpi-label">总应答</div>
                    <div class="kpi-value">{{ summary.totalAnswered | SipPrettyInteger }}</div>
                  </div>
                  <div class="kpi-item" :title="`处理量 ${summary.processed | SipPrettyInteger}`">
                    <div class="kpi-label">处理量</div>
                    <div class="kpi-value">{{ summary.processed | SipPrettyInteger }}</div>
                  </div>
                </div>
                <div class="right-btns">
                  <a-icon
                    class="fullscreen-icon"
                    :type="fullscreen.isFullscreen ? 'fullscreen-exit' : 'fullscreen'"
                    @click="toggleFullscreen">
                  </a-icon>
                </div>
              </div>
            </div>
          </a-layout-header>
          <a-layout-content
            class="layout-content">
            <div v-if="loading" style="flex: 1;" class="flex flex-row main-center cross-center">
              <a-spin size="large"></a-spin>
            </div>
            <template v-else>
              <div class="inbound-title">
                呼入主叫分组
                <span class="sub-title">共 {{ inboundCallerList.length }} 条（按处理中通话排序）</span>
              </div>
              <div v-if="inboundCallerList.length===0" class="flex flex-row main-center cross-center" style="height: 120px;">
                <a-empty description="暂无数据"/>
              </div>
              <div v-else class="inbound-grid">
                <CallerStatsItem
                  v-for="(item, index) in inboundCallerList"
                  :key="'in_' + item.caller + '_' + index"
                  :item="item"
                  default-call-direction="INBOUND"
                  :rpt-millis="rptMillis"/>
              </div>

              <!--
              <div class="inbound-title">
                呼入IP分组
                <span class="sub-title">共 {{ inboundIpList.length }} 条（按处理中通话排序）</span>
              </div>
              <div v-if="inboundIpList.length===0" class="flex flex-row main-center cross-center" style="height: 120px;">
                <a-empty description="暂无数据"/>
              </div>
              <div v-else class="inbound-grid">
                <CallerStatsItem
                  v-for="(item, index) in inboundIpList"
                  :key="'in_ip_' + item.caller + '_' + index"
                  :item="item"
                  :rpt-millis="rptMillis"/>
              </div>
              -->

              <div class="inbound-title">
                呼出主叫分组
                <span class="sub-title">共 {{ outboundCallerList.length }} 条（按处理中通话排序）</span>
              </div>
              <div v-if="outboundCallerList.length===0" class="flex flex-row main-center cross-center" style="height: 120px;">
                <a-empty description="暂无数据"/>
              </div>
              <div v-else class="inbound-grid">
                <CallerStatsItem
                  v-for="(item, index) in outboundCallerList"
                  :key="'out_' + item.caller + '_' + index"
                  :item="item"
                  default-call-direction="OUTBOUND"
                  :rpt-millis="rptMillis"/>
              </div>

              <!--
              <div class="inbound-title">
                呼出IP分组
                <span class="sub-title">共 {{ outboundIpList.length }} 条（按处理中通话排序）</span>
              </div>
              <div v-if="outboundIpList.length===0" class="flex flex-row main-center cross-center" style="height: 120px;">
                <a-empty description="暂无数据"/>
              </div>
              <div v-else class="inbound-grid">
                <CallerStatsItem
                  v-for="(item, index) in outboundIpList"
                  :key="'out_ip_' + item.caller + '_' + index"
                  :item="item"
                  :rpt-millis="rptMillis"/>
              </div>-->
            </template>
          </a-layout-content>
          <MainFooter />
        </a-layout>
      </div>
    </div>
  </div>
</template>

<script>
  import { mapGetters } from "vuex";
  import { GETTERS, GLOBAL } from '@/constants'
  import { stores } from '@/utils/util'
  import { api as fullscreen } from 'vue-fullscreen'
  import { postAction } from '@/api/api'

  import ALayout from 'ant-design-vue/lib/layout';
  import MainFooter from '@/components/layout/tabLayout/MainFooter'
  import AEmpty from 'ant-design-vue/lib/empty';
  import CallerStatsItem from './components/CallerStatsItem'

  export default {
    name: 'SipfwPage',
    components: {
      ALayout,
      ALayoutHeader: ALayout.Header,
      MainFooter,
      AEmpty,
      CallerStatsItem,
    },
    data(){
      return {
        GLOBAL: GLOBAL,
        stores: stores,
        fullscreen: fullscreen,
        loading: false,
        // 动态数据
        wsCounter: 0,
        intervalTimer: undefined,
        currentTime: undefined,
        exception: false,
        rptMillis: 5 * 60 * 1000,
        inboundCallerList: [],
        outboundCallerList: [],
        inboundIpList: [],
        outboundIpList: [],
        summary: {
          handling: 0,
          handlingAnswered: 0,
          handlingConnected: 0,
          handlingTrying: 0,
          handlingAnsweredRate: '0.00%',
          handlingConnectedRate: '0.00%',
          handlingTryingRate: '0.00%',
          totalAnswered: 0,
          totalAnswerRate: '0.00%',
          processed: 0,
          processedRows: [],
          connsipTop: [],
          othersipTop: [],
        },
      }
    },
    computed: {
      ...mapGetters({
        /** App state */
        wsConnected: GETTERS.GET_APP_WS_CONNECTED,
      }),
    },
    inject: ['wsSubscribe', 'wsUnSubscribe'],
    methods: {
      toggleFullscreen(){
        fullscreen.toggle()
      },
      wsSipfwFunc(messageJson){
        let that = this
        if(that.loading){
          that.loading = false
        }
        let inboundCallerGroup = that._.get(messageJson, 'inboundCallerGroup', {}) || {}
        let outboundCallerGroup = that._.get(messageJson, 'outboundCallerGroup', {}) || {}
        let inboundIpGroup = that._.get(messageJson, 'inboundIpGroup', {}) || {}
        let outboundIpGroup = that._.get(messageJson, 'outboundIpGroup', {}) || {}
        let globalSpecialSipMap = that._normalizeSpecialSipMap(that._.get(messageJson, 'specialSipCodeJson', {}))
        let inboundCallerList = that._buildGroupList(inboundCallerGroup, globalSpecialSipMap)
        let outboundCallerList = that._buildGroupList(outboundCallerGroup, globalSpecialSipMap)
        let inboundIpList = that._buildGroupList(inboundIpGroup, globalSpecialSipMap)
        let outboundIpList = that._buildGroupList(outboundIpGroup, globalSpecialSipMap)
        let rptMillis = parseInt(that._.get(messageJson, 'rptMillis', that.rptMillis)) || that.rptMillis
        that.exception = messageJson['exception']
        if(!that.exception){
          that.rptMillis = rptMillis
          that.inboundCallerList = inboundCallerList
          that.outboundCallerList = outboundCallerList
          that.inboundIpList = inboundIpList
          that.outboundIpList = outboundIpList
          that.summary = that._buildSummary(messageJson)
        }
        that.wsCounter = that.wsCounter + 1
        // en-lang
        if(that.wsCounter%10 === 0){
          postAction('/application/clientInteractive', {alive: true}).then(resp => {
            resp || true
            // let item = resp.gmodel
            // console.log('Interactive alive')
          })
        }
      },
      /** 工具方法 */
      // 计算缩放比例
      _screenScale(width = 1920, height = 1080) {
        let ww = window.innerWidth / width
        let wh = window.innerHeight / height
        return ww < wh ? ww : wh
      },
      // 窗口大小改变
      _windowResize() {
        let that = this
        let dataScreenRef = that.$refs.dataScreenRef
        if (dataScreenRef) {
          dataScreenRef.style.transform = `scale(${that._screenScale()}) translate(-50%, -50%)`
        }
      },
      _buildSummary(messageJson) {
        let dimensions = this._.get(messageJson, 'dimensionCount', {}) || {}
        let handling = parseInt(this._.get(messageJson, 'handling', 0))
        let handlingAnswered = parseInt(this._.get(messageJson, 'handlingAnswered', 0))
        let handlingConnected = parseInt(this._.get(messageJson, 'handlingConnected', 0))
        let handlingTrying = parseInt(this._.get(messageJson, 'handlingTrying', 0))
        let processed = parseInt(this._.get(messageJson, 'processed', 0))
        let answered = parseInt(dimensions.answered || 0) + parseInt(dimensions.conv20m || 0)
        let reinvite = parseInt(dimensions.reinvite || 0)
        let blacklist = parseInt(dimensions.blacklist || 0)
        let connected = this._sumPrefix(dimensions, 'connsip')
        let others = this._sumPrefix(dimensions, 'othersip')
        let totalAnswered = handlingAnswered + answered
        let totalAnswerBase = handling + processed

        return {
          handling,
          handlingAnswered,
          handlingConnected,
          handlingTrying,
          handlingAnsweredRate: this._percent(handlingAnswered, handling),
          handlingConnectedRate: this._percent(handlingConnected, handling),
          handlingTryingRate: this._percent(handlingTrying, handling),
          totalAnswered: totalAnswered,
          totalAnswerRate: this._percent(totalAnswered, totalAnswerBase),
          processed,
          processedRows: [
            { label: '应答', value: answered, percent: this._percent(answered, processed) },
            { label: '重邀请', value: reinvite, percent: this._percent(reinvite, processed) },
            { label: '黑名单', value: blacklist, percent: this._percent(blacklist, processed) },
            { label: '触达挂断', value: connected, percent: this._percent(connected, processed) },
            { label: '未触达挂断', value: others, percent: this._percent(others, processed) },
          ].filter(item => item.value > 0),
          connsipTop: this._prefixTopN(dimensions, 'connsip', 3, processed),
          othersipTop: this._prefixTopN(dimensions, 'othersip', 5, processed),
        }
      },
      _buildGroupList(groupMap, globalSpecialSipMap) {
        return Object.keys(groupMap).map(groupKey => {
          let stats = groupMap[groupKey] || {}
          let handling = parseInt(stats.handling || 0)
          let processed = parseInt(stats.processed || 0)
          let specialSipCodeJson = this._normalizeSpecialSipMap(this._.get(stats, 'specialSipCodeJson', globalSpecialSipMap))
          return {
            definitionId: this._.get(stats, 'definitionId', undefined),
            prefix: this._.get(stats, 'prefix') || groupKey,
            displayName: this._.get(stats, 'displayName') || groupKey,
            callDirection: this._.get(stats, 'callDirection'),
            handling: handling,
            processed: processed,
            answered: parseInt(stats.answered || 0),
            connected: parseInt(stats.connected || 0),
            trying: parseInt(stats.trying || 0),
            hangupTypeCount: stats.hangupTypeCount || {},
            specialSipCodeJson: specialSipCodeJson,
            ratioTypeEnabled: !!this._.get(stats, 'ratioTypeEnabled', false),
            qaEnabled: !!this._.get(stats, 'qaEnabled', false),
            qaScriptJson: this._normalizeSpecialSipMap(this._.get(stats, 'qaScriptJson', {})),
            qaFailCount: parseInt(this._.get(stats, 'qaFailCount', 0)),
            totalCalls: handling + processed,
          }
        }).sort((s1, s2) => {
          // 调试代码
          const TOP_PREFIX = 'none';
          const s1IsTop = s1.prefix === TOP_PREFIX;
          const s2IsTop = s2.prefix === TOP_PREFIX;
          if (s1IsTop && !s2IsTop) return -1; // s1 置顶
          if (!s1IsTop && s2IsTop) return 1;  // s2 置顶
          // 正常排序
          if (s2.handling !== s1.handling) return s2.handling - s1.handling
          if (s2.processed !== s1.processed) return s2.processed - s1.processed
          return s2.totalCalls - s1.totalCalls
        })
      },
      _sumPrefix(dimensions, prefix) {
        return Object.keys(dimensions).reduce((sum, key) => {
          if (key.startsWith(prefix)) {
            return sum + parseInt(dimensions[key] || 0)
          }
          return sum
        }, 0)
      },
      _normalizeSpecialSipMap(source) {
        if (!source) return {}
        if (typeof source === 'string') {
          try {
            let parsed = JSON.parse(source)
            return this._normalizeSpecialSipMap(parsed)
          } catch (e) {
            return {}
          }
        }
        if (typeof source !== 'object') return {}
        return Object.keys(source).reduce((ret, code) => {
          let codeKey = String(code || '').trim()
          let displayName = String(source[code] || '').trim()
          if (codeKey && displayName) {
            ret[codeKey] = displayName
          }
          return ret
        }, {})
      },
      _percentNumber(percentText) {
        return parseFloat(String(percentText || '0').replace('%', '')) || 0
      },
      _prefixTopN(dimensions, prefix, maxNum, processed) {
        return Object.keys(dimensions)
          .filter(key => key.startsWith(prefix) && parseInt(dimensions[key] || 0) > 0)
          .map(key => ({
            key: key,
            value: parseInt(dimensions[key] || 0),
            percent: this._percent(parseInt(dimensions[key] || 0), processed),
          }))
          .sort((s1, s2) => s2.value - s1.value)
          .slice(0, maxNum)
      },
      _percent(num, den) {
        if (!den || den <= 0) return '0.00%'
        return (num / den * 100).toFixed(2) + '%'
      },
    },
    mounted() {
      let that = this
      // 初始盒子缩放
      let dataScreenRef = that.$refs.dataScreenRef
      if (dataScreenRef) {
        dataScreenRef.style.transform = `scale(${that._screenScale()}) translate(-50%, -50%)`
        dataScreenRef.style.width = `1920px`
        dataScreenRef.style.height = `1080px`
      }
      // 创建时间Timer
      that.intervalTimer = setInterval(() => {
        that.currentTime = that.$moment().format('YYYY年MM月DD HH:mm:ss')
      }, 1000)
      // 绑定浏览器改变大小
      window.addEventListener('resize', that._windowResize)
      // 定期发送存活消息
      /*postAction('/application/clientInteractive', {mounted: true}).then(resp => {
        resp.gmodel || true
        console.log('Interactive mounted')
      })*/
      // 订阅Ws消息
      that.loading = true
      let subPath = '/topic/sipfw'
      that.wsSubscribe(subPath, that.wsSipfwFunc)
    },
    beforeDestroy() {
      let that = this
      // 清理时间Timer
      if (that.intervalTimer) {
        clearInterval(that.intervalTimer)
      }
      // 移除浏览器改变大小
      window.removeEventListener('resize', that._windowResize)
      // 取消订阅Ws消息
      let subPath = '/topic/sipfw'
      that.wsUnSubscribe(subPath)
    },
  }
</script>

<style lang="scss" scoped>
  @import "./screen-page";
  $height: 56px;
  .layout {
    min-height: 100vh;
    .ant-layout-header {
      height: $height;
      line-height: $height;
      padding: 0;
    }
    .header {
      height: $height;
      background-color: #fff;
      padding: 0 16px;
      box-shadow: 0 1px 4px rgba(0, 24, 41, 0.08);
      transition: background 300ms;
      display: flex;
      flex-direction: row;
      justify-content: space-between;
      align-items: center;
      /*&:hover{

      }*/
      .header-right{
        .right-btns{
          margin-left: 16px;
          display: inline-block;
        }
      }
    }
    .header-left{
      flex: 1 1 auto;
      min-width: 0;
      display: flex;
      align-items: center;
      .header-title{
        max-width: 100%;
        overflow: hidden;
        white-space: nowrap;
      }
    }
    .header-right{
      height: $height;
      flex: 0 0 auto;
      line-height: 1;
      display: flex;
      flex-direction: row;
      justify-content: flex-end;
      align-items: center;
      gap: 8px;
      .header-kpis{
        display: flex;
        flex-direction: row;
        gap: 6px;
        .kpi-item{
          min-width: 78px;
          border: 1px solid #e6edf5;
          border-radius: 8px;
          background: #fafcff;
          padding: 6px 8px;
          text-align: center;
          .kpi-label{
            font-size: 12px;
            color: #7f8b98;
            white-space: nowrap;
          }
          .kpi-value{
            margin-top: 1px;
            font-size: 16px;
            line-height: 1.1;
            font-weight: 700;
            // color: #1f2d3d;
            white-space: nowrap;
          }
        }
      }
      .right-btns{
        display: none;
      }
      .fullscreen-icon{
        font-size: 16px;
        margin-right: 4px;
      }
    }
  }
  .layout-content{
    padding: 10px 8px;
    flex: 1;
    display: flex;
    flex-direction: column;
    justify-content: flex-start;
  }
  .inbound-title{
    font-weight: bold;
    margin-bottom: 10px;
    margin-top: 14px;
    .sub-title{
      margin-left: 8px;
      font-weight: normal;
      color: #666;
      font-size: 12px;
    }
  }
  .inbound-grid{
    display: flex;
    flex-direction: row;
    flex-wrap: wrap;
    gap: 10px;
  }
</style>
