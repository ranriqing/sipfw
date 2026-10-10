<template>
  <div class="gc-canvas" @click="refreshCode">
    <img :width="contentWidth" :height="contentHeight" class="canvas-img" :src="`${baseUrl}/auth/captcha.jpg?uuid=${uuidStr}`">
    <!--<canvas ref="canvas" :width="contentWidth" :height="contentHeight" />-->
  </div>
</template>

<!--
# 组件说明
  验证码组件，动态生成验证码；
  可以配置验证码长度、最小字体、最大字体、内容宽度、内容高度；
# 配置组件
  length: Number        // 验证码长度，默认4
  fontSizeMin: Number   // 最小文字，默认20
  fontSizeMax: Number   // 最大文字，默认45
  contentWidth: Number  // 验证码块宽度，默认136
  contentHeight: Number // 验证码块高度，默认38
# 操作方法
  暂无
-->

<script>
import settings from '@/settings'
import { tools } from '@/utils/util'

export default {
  name: 'GraphicCode',
  props: {
    // 验证码长度
    uuidStr:{
      type: String,
      required: true
    },
    // 验证码长度
    length:{
      type: Number,
      default: 4
    },
    // 字体大小
    fontSizeMin: {
      type: Number,
      default: 20
    },
    fontSizeMax: {
      type: Number,
      default: 45
    },
    // 内容宽高
    contentWidth: {
      type: Number,
      default:136
    },
    contentHeight: {
      type: Number,
      default: 38
    },
  },
  data(){
    return {
      tools: tools,
      baseUrl: settings.BASE_URL,
      randomCode: ''
    }
  },
  methods: {
    refreshCode(){
      // this.drawPic()
      this.$emit("change", tools.randomUuid())
    },
    drawPic () {
      let that = this
      that._randomCode()
      let canvas = that.$refs.canvas
      let ctx = canvas.getContext('2d')
      ctx.textBaseline = 'bottom'
      // 填充背景色
      ctx.fillStyle = that._randomColor(180, 240)
      ctx.fillRect(0, 0, that.contentWidth, that.contentHeight)
      // 书写验证码
      for (let i = 0; i < that.randomCode.length; i++) {
        that._drawText(ctx, that.randomCode[i], i)
      }
      // 干扰线和干扰点
      that._drawLine(ctx)
      that._drawDot(ctx)
      // 成功生成图片
      that.$emit("change",that.randomCode)
    },
    /** 工具方法 */
    _randomCode(){
      let random = ''
      // 移除 { I l i o O }
      let str = "QWERTYUPLKJHGFDSAZXCVBNMqwertyupkjhgfdsazxcvbnm1234567890"
      for(let i = 0; i < this.length; i++) {
        let index = Math.floor(Math.random()*57);
        random += str[index];
      }
      this.randomCode = random
    },
    _randomNum (min, max) {
      return Math.floor(Math.random() * (max - min) + min)
    },
    _randomColor (min, max) {
      let r = this._randomNum(min, max)
      let g = this._randomNum(min, max)
      let b = this._randomNum(min, max)
      return 'rgb(' + r + ',' + g + ',' + b + ')'
    },
    _drawText (ctx, txt, i) {
      ctx.fillStyle = this._randomColor(50, 160)
      let fontSize = this._randomNum(this.fontSizeMin, this.fontSizeMax)
      ctx.font = fontSize + 'px SimHei'
      let padding = 10;
      let offset = (this.contentWidth-40)/(this.randomCode.length-1)
      let x=padding;
      if(i>0){ x = padding+(i*offset) }
      let y = this._randomNum(this.fontSizeMax, this.contentHeight - 5)
      if(fontSize>40){ y=40 }
      let deg = this._randomNum(-10,10)
      // 修改坐标原点和旋转角度
      ctx.translate(x, y)
      ctx.rotate(deg * Math.PI / 180)
      ctx.fillText(txt, 0, 0)
      // 恢复坐标原点和旋转角度
      ctx.rotate(-deg * Math.PI / 180)
      ctx.translate(-x, -y)
    },
    _drawLine (ctx) {
      for (let i = 0; i <1; i++) {
        ctx.strokeStyle = this._randomColor(40, 180)
        ctx.beginPath()
        ctx.moveTo(this._randomNum(0, this.contentWidth), this._randomNum(0, this.contentHeight))
        ctx.lineTo(this._randomNum(0, this.contentWidth), this._randomNum(0, this.contentHeight))
        ctx.stroke()
      }
    },
    _drawDot (ctx) {
      for (let i = 0; i < 100; i++) {
        ctx.fillStyle = this._randomColor(0, 255)
        ctx.beginPath()
        ctx.arc(this._randomNum(0, this.contentWidth), this._randomNum(0, this.contentHeight), 1, 0, 2 * Math.PI)
        ctx.fill()
      }
    },
  },
  mounted () {
    this.refreshCode()
  },
}
</script>
