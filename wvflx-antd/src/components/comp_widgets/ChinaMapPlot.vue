<template>
  <div class="flex flex-row main-center">
    <div class="china-map flex" :class="cptIsMobile ? 'flex-column' : 'flex-row'">
      <div class="left">
        <a-button @click="upsideGeoChart">{{ adName }}</a-button>
      </div>
      <div class="center flex flex-column cross-center">
        <!-- 展示数量汇总 -->
        <div v-if="cptLevel>0" class="num-wrapper" :style="{top: cptLevel===1?'80px':'0px'}">
          <div>数量 <span class="num">{{ _.sumBy(dataArray, 'rowCount') }}</span></div>
          <div class="switch-wrapper">
            <a-switch :checked="showLabel" size="small" @change="showLabel=$event"/>
            <span class="switch-label">展示数量</span>
          </div>
        </div>
        <!-- 用于渲染图表的容器 -->
        <div ref="slotChart" :style="cptChartSize"></div>
      </div>
      <div class="right flex flex-column cross-start" :style="cptIsMobile ? 'width: 100%' : 'width: 300px'">
        <div class="flex flex-row">
          <span>
            <a-checkbox :indeterminate="checkIndeterminate" :checked="checkAll" @change="onCheckAll">全选</a-checkbox>
          </span>
        </div>
        <div class="flex flex-row main-start flex-wrap" style="margin-top: 10px;">
          <template>
            <a-checkable-tag
              v-for="tag in cptTypeTags"
              style="margin-bottom: 4px; line-height: 16px; padding: 0 4px; font-size: 12px;"
              :key="tag.value"
              :checked="typeSelected.indexOf(tag.value) !== -1"
              @change="checked => onTypeChange(tag, checked)">
              {{ tag.name }}
            </a-checkable-tag>
          </template>
        </div>
        <div class="rank-content">
          <div
            v-for="(gItem, gIndex) in groupedArray.slice(0, 10)"
            :key="gIndex"
            class="sort-item flex flex-row main-between cross-center">
            <div class="flex flex-row cross-center">
              <div :class="['item-num', gIndex<3 && 'active']">
                {{ gIndex+1 }}
              </div>
              <div>
                {{ gItem['caption'] }}
              </div>
            </div>
            <div>
              {{ _.get(gItem, 'value[2]') }}
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
  import * as echarts from 'echarts'
  import mapConfig from './configs/mapConfig'
  import funcConfig from './configs/funcConfig'
  import chinaJson from './assets/china.json'

  import { postAction, publicAsset } from '@/api/api'
  import { converters, stores } from '@/utils/util'

  import ARadio from "ant-design-vue/lib/radio"
  import ACheckbox from 'ant-design-vue/lib/checkbox';

  import ScElement from '@/store/models/ScElement'
  import MetaDomainField from "@/store/models/MetaDomainField";

  // https://blog.csdn.net/m0_52539553/article/details/138074255
  export default {
    name: 'ChinaMapPlot',
    components: {
      ARadio,
      ARadioGroup: ARadio.Group,
      ARadioButton: ARadio.Button,
      ACheckbox,
    },
    props: {
      mapUri: {
        type: String,
        required: true
      },
      chartAddrField: {
        type: Object,
        required: true
      },
      chartSeries: {
        type: Object,
        required: false,
        default: function(){
          return {}
        }
      },
    },
    data() {
      return {
        /** 配置&状态 */
        stores: stores,
        converters: converters,
        loading: true,
        geoPublicUrl: 'https://geo.datav.aliyun.com/areas_v3/bound/',
        optionMap: mapConfig.optionMap,
        /** 统计条件 */
        checkAll: false,
        checkIndeterminate: false,
        typeSelected: [],
        /** 查询&数据源 */
        queryRules: [],
        dataSource: [],
        dataArray: [],
        groupedArray: [],
        showLabel: true,
        chartMap: undefined,
        alladcode: '',    // 行政区划编码
        historyData: [],  // 缓存各层级
        filterCode: undefined,
        groupField: undefined,
        parentAdItem: undefined,
        adName: '全国',    // 当前层中文name
        chartTimer: undefined
      }
    },
    watch: {
      showLabel(val){
        if(this.chartMap){
          this.chartMap.setOption({
            series: [{ label: { show: val } }]
          })
        }
      },
      cptIsMobile(){
        // 设备模式切换后,等DOM尺寸更新完成再重绘
        this.$nextTick(() => {
          if(!this.chartMap){
            return
          }
          this.chartMap.resize()
          // 重新渲染以应用visualMap尺寸调整
          let upData = this.historyData[this.historyData.length-1]
          if(upData){
            this.formatGeoChart(upData.geoJson, upData.name)
          }
        })
      }
    },
    computed: {
      cptLevel: function(){
        return this.historyData.length
      },
      cptIsMobile: function(){
        return stores.isMobile()
      },
      cptChartSize: function(){
        return this.cptIsMobile
          ? { width: '100%', height: '320px' }
          : { width: '900px', height: '500px' }
      },
      cptTypeTags: function(){
        let that = this
        let metaDomainFieldId = that._.get(that.chartSeries, 'metaDomainFieldId')
        let metaDomainField = MetaDomainField.find(metaDomainFieldId)
        let dfExtend = converters.parseObj(metaDomainField?.extendJson)
        let scKey = that._.get(dfExtend, 'scKey')
        let scelArray = ScElement.query()
          .where(item => item.scConfigKey === scKey)
          .orderBy('seqOrder').orderBy('id').get()
        return scelArray.filter(item =>{
          return that.dataSource.find(record => record['seriesType'] === item['id'] )
        }).map(item => {
          return {
            value: item['id'],
            name: item['title'],
          }
        })
      }
    },
    methods: {
      /** 统计分类 */
      onStatisticChange(e){
        let that = this
        e.target.value || true
        let upData = that.historyData[that.historyData.length-1]
        that.formatGeoChart(upData.geoJson, upData.name)
      },
      /** 勾选设备类型 */
      onCheckAll(e){
        let that = this
        let checked = e.target.checked
        that.checkAll = checked
        that.checkIndeterminate = false
        that.typeSelected = checked?that.cptTypeTags.map(item => item.value ):[]
        let upData = that.historyData[that.historyData.length-1]
        that.formatGeoChart(upData.geoJson, upData.name)
      },
      onTypeChange(tag, checked){
        let that = this
        let itemIndex = that.typeSelected.findIndex( value => value === tag.value)
        if(checked){
          if(itemIndex === -1){
            that.typeSelected.push(tag.value)
          }
        }else{
          if(itemIndex !== -1){
            that.typeSelected.splice(itemIndex, 1)
          }
        }
        that.checkAll = that.cptTypeTags.length === that.typeSelected.length
        that.checkIndeterminate = that.typeSelected.length>0 && !that.checkAll
        let upData = that.historyData[that.historyData.length-1]
        that.formatGeoChart(upData.geoJson, upData.name)
      },
      /** 加载数据 */
      /*doQuery1(queryRules){
        let that = this
        that.queryRules = queryRules
        that._loadData(1)
      },*/
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
        queryCondition.rules.push({
          field: '_extend_chartAddrField',
          value: that._.get(that.chartAddrField, 'field', 'addr'),
          operator: 'equal',
        })
        queryCondition.rules.push({
          field: '_extend_chartSeries',
          value: that._.get(that.chartSeries, 'field'),
          operator: 'equal',
        })
        // 加载数据
        that.loading = true
        postAction(that.mapUri, queryCondition).then(resp => {
          that.loading = false
          if(resp.gmodel){
            that.dataSource = resp.gmodel
            this.initChart()
          }
        }).catch(err => {
          that.loading = false
          if(!err.hcode){
            that.$message.error(`[${err.code}]加载失败: ${err.message}`)
          }
        })
      },
      async initChart() {
        let that = this
        // 获取地图name对应的adcode行政区划编码
        let allJson = await publicAsset('/maps/all.json')
        this.alladcode = allJson.data
        // 获取中国地图json, 南海诸岛小窗化
        let chinaGeoJson = await publicAsset('/maps/100000_full.json')
        let formatChinaGeoJson = funcConfig.formatJson(chinaGeoJson.data)
        // 鼠标移入点位的弹窗
        let rich = {
          b: {
            color: '#fff',
            backgroundColor: {
              image: require('./assets/rich-bg.png')
            },
            padding: [10, 20],
            fontSize: 14,
            align: 'center'
          }
        }
        // 初始地图展示
        that.optionMap.series[0].emphasis.label.rich = rich
        that.optionMap.series[0].label.rich = rich
        that.historyData = [] // 支持上钻下钻
        // 勾选全部设备
        that.checkAll = true
        that.checkIndeterminate = false
        that.typeSelected = that.cptTypeTags.map(item => item.value )
        that.initGeoEcharts(formatChinaGeoJson, 'china')
      },
      initGeoEcharts(geoJson, name, isUpside) {
        let that = this
        that.formatGeoChart(geoJson, name)
        that.chartMap.off('click')
        // 存储当前下钻的层级和地图json信息
        if(!isUpside){
          that.historyData.push({ geoJson, name })
        }
        // 地图绑定点击事件
        that.chartMap.on('click', (params) => {
          // 控制最多下钻层级控制
          if(that.historyData.length>=3){
            return
          }
          // 包含未知caption的点，则不允许下钻
          if(that._.get(params, 'data.caption','').indexOf('未知')!==-1){
            return
          }
          // 获取中文名对应的行政区划编码,调接口用的
          let clickRegionCode = that.alladcode.filter((areaJson) => areaJson.name === params.name)[0].adcode
          // 没有区县的地级市,东莞,中山,儋州,三沙,嘉峪关, 这5个市比较特殊,没有下一级区县,
          // 在地图json数据中,中国,省,市,地图json的接口是 '行政区划+_full.json'的,而最后一级区县的地图json接口是 '行政区划+.json'.
          // 由于这5个地级市没有区县,所以他们就是最后一级,所以他们接口参数格式是'行政区划+.json'
          let adcodeArr = ['460400', '460300', '441900', '442000', '620200']
          let regJson = `${clickRegionCode}_full.json`
          // 区县或者没有区县的地级市,行政区划最后两位不是00的就是最后一级,adcodeArr除外
          if (clickRegionCode.toString().slice(-2) !== '00' || adcodeArr.includes(clickRegionCode.toString())) {
            regJson = `${clickRegionCode}.json`
          }
          publicAsset('/maps/' + regJson).then((regionGeoJson) => {
            that.initGeoEcharts(regionGeoJson.data, params.name)
          }).catch(err => {
            console.log(that.geoPublicUrl + regJson)
            console.err(err)
            that.historyData = []
            that.initGeoEcharts(chinaJson, 'china')
          })
        })
      },
      formatGeoChart(geoJson, name) {
        let that = this
        // 页面上回钻按钮的name
        that.adName = name === 'china' ? '全国' : name
        // 注册3D地图
        echarts.registerMap(name, geoJson)
        // 下钻的时候地图大小设置,不设置会有部分地图超出屏幕
        that.optionMap.geo.forEach((v) => {
          v.map = name
          // 手机端画布较窄,全国地图用170%会被裁切,回退到100%以完整展示
          v.layoutSize = name === 'china' ? (that.cptIsMobile ? '110%' : '150%') : '100%'
        })
        let demoData = [];
        for (let i = 0; i < 100; i++) {
          let lng = Math.random() * (135 - 73) + 73;
          let lat = Math.random() * (53 - 18) + 18;
          let value = Math.floor(Math.random() * 100);
          demoData.push({value: [lng, lat, value], name:'idx-'+i});
        }
        // 过滤出当前地图的点位,过滤掉不属于改地图区域的点位
        let filterCode = undefined
        let groupField = undefined
        let groupedArray = []
        let parentAdItem = this.alladcode.find(item => item['adcode'] == 100000)
        // 国家按省份分组
        if(name === 'china'){
          filterCode = undefined
          groupField = 'provinceCode'
          that.optionMap.series[0].symbolSize = function (data) {
            return [data[2]/8, data[2]/8]; // 根据数量设置点的大小
          }
        }else{
          let adItem = that.alladcode.find((item) => item['name'] === name)
          // 省份按城市分组
          if(adItem.adcode.toString().endsWith('0000')){
            filterCode = adItem.adcode
            groupField = 'cityCode'
            parentAdItem = adItem
            // 城市按区域分组
          }else if(adItem.adcode.toString().endsWith('00')){
            filterCode = adItem.adcode
            groupField = 'areaCode'
            parentAdItem = adItem
            // 区域按区域分组
          }else{
            filterCode = adItem.adcode
            groupField = 'areaCode'
            parentAdItem = adItem
          }
        }

        // 用于导出
        that.filterCode = filterCode
        that.groupField = groupField
        that.parentAdItem = parentAdItem

        let maxNum = 3;
        if(groupField){
          let dataArray = that.dataSource
          if(filterCode){
            if(groupField === 'cityCode'){
              dataArray = that.dataSource.filter(item => item['provinceCode'] == filterCode)
            }else if(groupField === 'areaCode'){
              dataArray = that.dataSource.filter(item => item['cityCode'] == filterCode)
            }
          }
          dataArray = dataArray.filter(item => that.typeSelected.indexOf(item['seriesType'])!==-1 )
          that.dataArray = dataArray
          let adGroups = that._.groupBy(dataArray, groupField)
          for (let groupCode in adGroups) {
            let adItem = that.alladcode.find(item => item['adcode'] == groupCode)
            let adCount = that._.sumBy(adGroups[groupCode], 'rowCount')
            if(adItem){
              let itemName = adItem['adcode'] === 100000?'中国':adItem['name']
              groupedArray.push({value: [adItem['lng'], adItem['lat'], adCount], name: itemName==='中国'?'北京':itemName, caption: itemName})
            }else if(parentAdItem){
              let itemName = parentAdItem['adcode'] === 100000?'中国':parentAdItem['name']
              groupedArray.push({value: [parentAdItem['lng'], parentAdItem['lat'], adCount], name: itemName==='中国'?'北京':itemName, caption: `${itemName}|未知`})
            }
            if(adCount>maxNum){
              maxNum = adCount
            }
          }
        }
        that.optionMap.series[0].symbolSize = function (data) {
          let pointSize = (35/maxNum)*data[2]
          return [pointSize, pointSize]; // 根据数量设置点的大小
        }
        /*let polygonData = this.dataArray.filter((v) => {
          return funcConfig.isPointInMultiPolygon([v.value[0], v.value[1]], geoJson.features)
        })*/
        that.groupedArray = groupedArray.sort((s1, s2) => that._.get(s2, 'value[2]') - that._.get(s1, 'value[2]'))
        that.optionMap.series[0].data = that.groupedArray
        that.optionMap.series[0].label.show = that.showLabel
        that.optionMap.visualMap.min = 0
        that.optionMap.visualMap.max = maxNum
        // 手机模式下缩小visualMap,减少占用空间
        if (that.cptIsMobile) {
          that.optionMap.visualMap.itemWidth = 10
          that.optionMap.visualMap.itemHeight = 70
          that.optionMap.visualMap.textStyle.fontSize = 12
        } else {
          that.optionMap.visualMap.itemWidth = 20
          that.optionMap.visualMap.itemHeight = 140
          that.optionMap.visualMap.textStyle.fontSize = 12
        }
        // 点位颜色高亮,初始进入第一个高亮
        // this.highLightPoint(currentData, 0)
        that.chartMap.clear()
        that.chartMap.setOption(that.optionMap)
        // 循环轮播每个点位
        if (that.chartTimer) {
          clearInterval(that.chartTimer)
          that.chartTimer = null
        }
      },
      // 返回上一级
      upsideGeoChart() {
        let that = this
        // 如果是最顶层就返回,没得回钻了
        if (that.historyData.length <= 1) return
        // 回钻一层, historyData移出一层
        that.historyData.pop()
        let upData = that.historyData[that.historyData.length-1]
        that.initGeoEcharts(upData.geoJson, upData.name, true)
      },
      // 格式化点位飞线数据
      flyLines(currentData) {
        let dataLines = []
        let center = [120.537612, 31.276282] // 飞线图终点的地位经纬度
        currentData.forEach((v) => {
          let lonLat = [v.value[0], v.value[1]]
          dataLines.push({
            coords: [lonLat, center] // 高版本的飞线数据组
          })
        })
        return dataLines
      },
      // 点位颜色高亮
      highLightPoint(currentData, index) {
        currentData.forEach((v, key) => {
          let flag = key === index ? 'aqua' : 'orange'
          v.itemStyle = { color: flag, shadowColor: flag }
        })
        this.optionMap.series[0].label.formatter = (e) => {
          return e.name === currentData[index].name ? `{b|${e.name}}` : ''
        }
      },
      // 窗口尺寸变化时重绘图表
      _windowResize() {
        if (this.chartMap) {
          this.chartMap.resize()
        }
      },
    },
    mounted () {
      let slotRef = this.$refs.slotChart
      this.chartMap = echarts.init(slotRef)
      this._loadData(1)
      window.addEventListener('resize', this._windowResize)
    },
    beforeDestroy () {
      window.removeEventListener('resize', this._windowResize)
      if (this.chartTimer) {
        clearInterval(this.chartTimer)
        this.chartTimer = null
      }
      if (this.chartMap) {
        this.chartMap.dispose()
        this.chartMap = null
      }
    },
  }
</script>

<style lang="scss" scoped>
.china-map{
  position: relative;
  border: 1px solid #e6e9f0;
  border-radius: 8px;
  padding: 10px;
  // background-color: #0aaeb3;
  .left{
    position: absolute;
    left: 10px;
    top: 10px;
    z-index: 10;
  }
  .center{
    .num-wrapper{
      position: absolute;
      z-index: 10;
      top: 100px;
      font-size: 15px;
      color: #909399;
      font-weight: bold;
      .num{
        color: #303133;
        font-size: 22px;
        font-weight: bold;
        text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.5);
      }
      .switch-wrapper{
        margin-top: 6px;
        display: flex;
        align-items: center;
        gap: 6px;
        .switch-label{
          font-size: 12px;
          font-weight: normal;
          color: #909399;
        }
      }
    }
  }
  .right{
    /deep/ .ant-tag-checkable-checked{
      background-color: #0aaeb3;
    }
    .rank-content{
      width: 100%;
      margin-top: 12px;
      font-size: 16px;
      .sort-item{
        line-height: 34px;
        padding: 0 6px;
        &:hover {
          background: #ecedef;
          box-shadow: 0 2px 8px 0 rgba(17, 31, 44, 0.08);
        }
        .item-num{
          border-radius: 20px;
          display: inline-block;
          font-size: 14px;
          font-weight: 600;
          margin-right: 24px;
          height: 22px;
          width: 22px;
          line-height: 22px;
          text-align: center;
          background-color: #f0f2f5;
          color: rgba(0,0,0,.45);
          &.active{
            background-color: #314659;
            color: #fff;
          }
        }
      }

    }
  }
}
// 手机/平板模式下缩小地图,让图表自适应容器宽度
@media (max-width: 1087.99px) {
  .china-map{
    width: 100%;
    .center{
      .num-wrapper{
        // 移出绝对定位,改为正常流,让数量汇总显示在地图上方而非盖住地图
        position: static;
        margin: 8px 0;
      }
    }
    .right{
      margin-top: 12px;
    }
  }
}
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
