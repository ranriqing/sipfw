<template>
  <GChartCard :loading="loading" title="当前时间">
    <template slot="total">
      <span class="text-tips" style="font-weight:normal; font-size: 24px;">{{ currentTime }}</span>
    </template>
  </GChartCard>
</template>

<script>
  import { converters } from '@/utils/util'
  import GChartCard from '@/components/comp_chart/GChartCard'


  export default {
    name: "CurrentTime",
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
        converters: converters,
        loading: false,
        timer: undefined,
        currentTime: undefined,
      };
    },
    methods: {
      _updateTime(){
        // 获取当前时间
        let now = new Date();
        // 格式化时间为 "YYYY-MM-DD HH:mm:ss" 格式
        let year = now.getFullYear();
        let month = String(now.getMonth() + 1).padStart(2, '0');
        let day = String(now.getDate()).padStart(2, '0');
        let hours = String(now.getHours()).padStart(2, '0');
        let minutes = String(now.getMinutes()).padStart(2, '0');
        let seconds = String(now.getSeconds()).padStart(2, '0');
        // 更新当前时间数据
        this.currentTime = `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
      },
    },
    mounted () {
      let that = this
      // 更新时间
      that._updateTime();
      that.timer = setInterval(() => {
        that._updateTime();
      }, 1000);
    },
    beforeDestroy() {
      let that = this
      if (that.timer) {
        clearInterval(that.timer);
      }
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
