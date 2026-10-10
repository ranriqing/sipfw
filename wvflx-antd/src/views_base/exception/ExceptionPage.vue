<template>
  <div :class="{'mobile': isMobile }">
    <div class="exception">
      <!-- 显示图片 -->
      <div class="img">
        <img v-if="type==='403'" src="~@/assets/images/hcode_403.svg">
        <img v-else-if="type==='404'" src="~@/assets/images/hcode_404.svg">
        <img v-else src="~@/assets/images/hcode_500.svg">
      </div>
      <!-- 展示内容 -->
      <div class="content">
        <!--<h1>{{ config[type].title }}</h1>
        <div class="desc">
          {{ config[type].desc }}
        </div>-->
        <!--<div class="action">
          <a-button type="primary" @click="onToDashboard">
            返回首页({{ countDownTime }}s)
          </a-button>
        </div>-->
      </div>
    </div>
  </div>
</template>

<script>
  import { mapGetters } from "vuex";
  import { GETTERS } from '@/constants'

  import types from './type'

  export default {
    name: "Exception",
    props: {
      type: {
        type: String,
        default: '404'
      },
    },
    data() {
      return {
        config: types,
        countInterval: null,
        countDownTime: 0,
      }
    },
    computed: {
      ...mapGetters({
        isMobile: GETTERS.CPT_APP_DEVICE_MOBILE,
      }),
    },
    methods: {
      onToDashboard() {
        let that = this
        window.clearInterval(this.countInterval);
        let currentRouteName = that.$route.name
        if(/^\d\d\d$/.test(currentRouteName)){
          that.$router.push({ name: 'root' }).catch(err => { err || true });
        }
      },
      doCountDown(){
        let that = this
        that.countDownTime = 5
        that.countInterval = window.setInterval(() => {
          if (that.countDownTime-- <= 1) {
            that.onToDashboard()
          }
        }, 1000);
      }
    },
    mounted() {
      this.doCountDown()
    },
    activated() {
      this.doCountDown()
    },
  }
</script>

<style lang="scss" scoped>
  /* 异常样式 */
  .exception {
    min-height: 500px;
    height: 80%;
    align-items: center;
    text-align: center;
    padding-top: 150px;
    .img {
      display: inline-block;
      padding-right: 52px;
      zoom: 1;
      img {
        height: 360px;
        max-width: 430px;
      }
    }
    .content {
      display: inline-block;
      flex: auto;
      h1 {
        color: #434e59;
        font-size: 72px;
        font-weight: 600;
        line-height: 72px;
        margin-bottom: 24px;
      }
      .desc {
        color: rgba(0, 0, 0, .45);
        font-size: 20px;
        line-height: 28px;
        margin-bottom: 16px;
      }
    }
  }
  /* 手机样式 */
  .mobile {
    .exception {
      .img {
        padding-right: unset;
        img {
          height: 40%;
          max-width: 80%;
        }
      }
    }
  }
</style>