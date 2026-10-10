<template>
  <div :class="`${classPrefix}-wrapper`">
    <div :class="`${classPrefix}__player`">
      <a @click="stop()" title="停止" class="btn-wrapper" >
        <i class="iconfont icon-stop"></i>
      </a>
      <a @click="pause()" title="播放" class="btn-wrapper">
        <i :class="{iconfont:true, 'icon-play': paused, 'icon-pause': !paused,}"></i>
      </a>
      <div @click="setPosition" :class="`${classPrefix}__playback-time-wrapper`" title="已播放 : 总时长">
        <div :style="progressStyle" :class="`${classPrefix}__playback-time-indicator`"></div>
        <span :class="`${classPrefix}__playback-time-current`">{{ currentTime }}</span>
        <span :class="`${classPrefix}__playback-time-separator`"></span>
        <span :class="`${classPrefix}__playback-time-total`">{{ duration }}</span>
      </div>
      <div :class="`${classPrefix}__extern-wrapper`">
        <a @click="download()" title="下载" class="btn-wrapper">
          <i class="iconfont icon-download"></i>
        </a>
        <a @click="changeLoop()" title="循环" class="btn-wrapper">
          <i :class="{iconfont:true, 'icon-loop': innerLoop, 'icon-noloop': !innerLoop,}"></i>
        </a>
        <a @click="mute()" title="静音" class="btn-wrapper">
          <i :class="{iconfont:true, 'icon-volume': !isMuted, 'icon-mute': isMuted,}"></i>
        </a>
        <a @mouseover="toggleVolume()" class="volume-toggle" title="声音">
          <input
            v-model.lazy="volumeValue"
            @change="updateVolume()"
            v-show="hideVolumeSlider"
            type="range"
            min="0"
            max="100"
            class="volume-slider"/>
        </a>
      </div>
    </div>
    <!-- 声音播放器 -->
    <audio
      ref="audiofile"
      :id="playerId"
      :loop="innerLoop"
      :src="file"
      preload="auto"
      style="display:none;">
    </audio>
  </div>
</template>

<!--
Note: "vue-audio": "~0.0.12",
https://github.com/shershen08/vuejs-sound-player
https://shershen08.github.io/vue-plugins-demo-static/index.html#/sound?ref=madewithvuejs.com
-->

<!--
# 组件说明
  播放器组件，传入音频文件，可以点击开始播放音频；
  支持播放、暂停、停止、静音、快进到指定位置、下载等功能；
# 配置组件
  file: String        // 文件全路径
  autoPlay: Boolean   // 自动播放，默认false
  loop: Boolean       // 循环播放，默认false
# 操作方法
  暂无
-->

<script>
  import { tools } from '@/utils/util'

  let baseVolumeValue = 75
  let prefixCls = 'vue-sound'

  export default {
    name: 'AudioPlayer',
    props: {
      file: {
        type: String,
        default: null
      },
      autoPlay: {
        type: Boolean,
        default: false
      },
      loop: {
        type: Boolean,
        default: false
      }
    },
    computed: {
      duration: function () {
        return this.audio ? this.convertTimeHHMMSS(this.totalDuration) : ''
      },
      playerId: function () {
        return 'player-' + this.uuid
      },
      classPrefix: function () {
        return prefixCls
      }
    },
    data () {
      return {
        isMuted: false,
        loaded: false,
        playing: false,
        paused: true,
        progressStyle: '',
        currentTime: '00:00',
        uuid: '0',
        innerLoop: undefined,
        audio: undefined,
        totalDuration: 0,
        hideVolumeSlider: false,
        volumeValue: baseVolumeValue
      }
    },
    methods: {
      /** 基本操作 */
      play: function () {
        if (this.playing && !this.paused) return
        this.paused = false
        this.audio.play()
        this.playing = true
      },
      pause: function () {
        this.paused = !this.paused;
        (this.paused) ? this.audio.pause() : this.audio.play()
      },
      stop: function () {
        this.playing = false
        this.paused = true
        this.audio.pause()
        this.audio.currentTime = 0
      },
      setPosition: function name (e) {
        let tag = e.target
        if (this.paused) return
        if (e.target.tagName === 'SPAN') {
          return
        }
        let pos = tag.getBoundingClientRect()
        let  seekPos = (e.clientX - pos.left) / pos.width
        this.audio.currentTime = parseInt(this.audio.duration * seekPos)
      },
      /** 扩展操作 */
      download: function () {
        this.stop()
        window.open(this.file, 'download')
      },
      changeLoop: function () {
        this.innerLoop = !this.innerLoop
      },
      mute: function () {
        this.isMuted = !this.isMuted
        this.audio.muted = this.isMuted
        this.volumeValue = this.isMuted ? 0 : 75
      },
      /** 声音调节 */
      updateVolume: function () {
        this.hideVolumeSlider = false
        this.audio.volume = this.volumeValue / 100
        if (this.volumeValue / 100 > 0) {
          this.isMuted = this.audio.muted = false
        }

        if (this.volumeValue / 100 === 0) {
          this.isMuted = this.audio.muted = true
        }
      },
      toggleVolume: function () {
        this.hideVolumeSlider = true
      },
      /** 组件事件 */
      _handleLoaded: function () {
        if (this.audio.readyState >= 2) {
          if (this.autoPlay) this.play()
          this.loaded = true
          this.totalDuration = parseInt(this.audio.duration)
        } else {
          throw new Error('Failed to load sound file')
        }
      },
      _handlePlayingUI: function (e) {
        e || true
        let currTime = parseInt(this.audio.currentTime)
        let percentage = parseInt((currTime / this.totalDuration) * 100)
        this.progressStyle = `width:${percentage}%;`
        this.currentTime = this.convertTimeHHMMSS(currTime)
      },
      _handlePlayPause: function (e) {
        if (e.type === 'pause' && this.playing === false) {
          this.progressStyle = `width:0%;`
          this.currentTime = '00:00'
          this.paused = true
        }
      },
      /** 组件初始化 */
      init: function () {
        this.audio.addEventListener('timeupdate', this._handlePlayingUI)
        this.audio.addEventListener('loadeddata', this._handleLoaded)
        this.audio.addEventListener('pause', this._handlePlayPause)
        this.audio.addEventListener('play', this._handlePlayPause)
      },
      getAudio: function () {
        return this.$el.querySelectorAll('audio')[0]
      },
      convertTimeHHMMSS: function(val){
        let hhmmss = new Date(val * 1000).toISOString().substr(11, 8)
        return (hhmmss.indexOf('00:') === 0) ? hhmmss.substr(3) : hhmmss
      }
    },
    mounted: function () {
      this.uuid = tools.randomUuid()
      this.audio = this.getAudio()
      this.innerLoop = this.loop
      this.init()
    },
    beforeDestroy: function () {
      /** 组件销毁 */
      this.audio.removeEventListener('timeupdate', this._handlePlayingUI)
      this.audio.removeEventListener('loadeddata', this._handleLoaded)
      this.audio.removeEventListener('pause', this._handlePlayPause)
      this.audio.removeEventListener('play', this._handlePlayPause)
    }
  }
</script>

<style lang="scss" scoped>
  $prefixCls: "vue-sound";
  .#{$prefixCls}-wrapper {
    min-width: 500px;
    border: 2px solid #f3f4f6;
    border-radius: 5px;
    padding: 4px;
    .btn-wrapper{
      line-height: 1;
      display: flex;
      flex-direction: row;
      justify-content: center;
      align-items: center;
    }
    .#{$prefixCls}__player {
      display: flex;
      flex-direction: row;
      align-items: center;
      height: 96px;
      line-height: 36px;
      a {
        vertical-align: middle;
        margin-right: 5px;
        position: relative;
        display: inline-block;
        white-space: nowrap;
        background-image: none;
        -webkit-box-shadow: 0 2px 0 rgba(0, 0, 0, 0.015);
        box-shadow: 0 2px 0 rgba(0, 0, 0, 0.015);
        cursor: pointer;
        transition: all 0.3s cubic-bezier(0.645, 0.045, 0.355, 1);
        height: 25px;
        padding: 4px;
        font-size: 14px;
        border-radius: 4px;
        color: rgba(0, 0, 0, 0.65);
        background-color: #fff;
        border: 1px solid #d9d9d9;
        &:hover{
          color: #40a9ff;
          border-color: #40a9ff;
        }
      }
      .#{$prefixCls}__extern-wrapper {
        display: inline-block;
        margin-left: 10px;
        > a {
          margin-right: 5px;
          vertical-align: 0;
        }
        .volume-toggle {
          border: none;
          position: relative;
          cursor: default;
          &:hover{
            color: rgba(0, 0, 0, 0.65);
          }
          .volume-slider {
            cursor: pointer;
            position: absolute;
            top: 0;
            width: 80px;
            left: 14px;
            transform: translateX(-16px) translateY(8px) rotate(270deg);
            &[style] {
              display: block !important;
            }
          }
        }
      }
      .#{$prefixCls}__playback-time-wrapper {
        position: relative;
        width: 250px;
        display: inline-block;
        background: #e0dcd7;
        height: 6px;
        cursor: pointer;
        font-size: 14px;
        vertical-align: middle;
        border-radius: 3px;
        .#{$prefixCls}__playback-time-separator::after {
          content:' / ';
        }
        .#{$prefixCls}__playback-time-total {
        }
        .#{$prefixCls}__playback-time-indicator {
          border-radius: 3px;
          background: #5099ff;
          position: absolute;
          top: 0;
          left: 0;
          bottom: 0;
        }
      }
    }
  }
</style>

