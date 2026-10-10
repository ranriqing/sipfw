<template>
  <div class="autherized-img">
    <img
      :src="thumbUrl"
      class="autherized-img-thumb"
      @click="onPreview" />
    <a-modal
      class="preview-modal"
      :footer="null"
      :width="previewWidth"
      :visible="previewVisible"
      @cancel="onPreviewCancel">
      <img
        style="max-width: 100%; max-height: 70vh;"
        :src="previewUrl"
        @load="onPreviewImageLoad" />
    </a-modal>
  </div>
</template>

<!-- 带上Token请求图片，支持点击放大查看原图 -->
<!-- 参考: https://blog.csdn.net/budai_bb/article/details/124022594 -->
<script>
  const settings = require('@/settings')
  import store from '@/store'
  import { GETTERS } from '@/constants'

  export default {
    name: "AuthImg",
    props: {
      imageSrc: {
        type: String,
        default: null
      },
    },
    data() {
      return {
        thumbUrl: '',
        previewVisible: false,
        previewUrl: '',
        previewWidth: 520,
      }
    },
    methods: {
      /** 带鉴权加载图片，返回 blob URL */
      _fetchBlob(url) {
        return new Promise((resolve, reject) => {
          let request = new XMLHttpRequest();
          request.responseType = 'blob';
          request.open('GET', url, true);
          // 扩展AccessToken
          let accessToken = store.getters[GETTERS.CPT_USER_ACCESS_TOKEN]
          let currentUsername = store.getters[GETTERS.CPT_USER_CURRENT_USERNAME]
          if (accessToken) {
            request.setRequestHeader('Authorization', accessToken)
            request.setRequestHeader('X-Gcs-Username', `${settings.BASE_URL}/${ currentUsername || 'none'}`)
          }
          request.onreadystatechange = () => {
            if (request.readyState === XMLHttpRequest.DONE) {
              if (request.status === 200) {
                resolve(URL.createObjectURL(request.response));
              } else {
                reject(new Error('图片加载失败'));
              }
            }
          };
          request.send(null);
        })
      },
      init() {
        let that = this
        if (!that.imageSrc) return
        that._fetchBlob(that.imageSrc).then(blobUrl => {
          that.thumbUrl = blobUrl
        }).catch(() => {})
      },
      onPreview() {
        let that = this
        if (!that.imageSrc) return
        // 先展示缩略图，原图加载完成后替换
        that.previewUrl = that.thumbUrl
        that.previewVisible = true
        let originalUrl = that.imageSrc.indexOf('?') !== -1
          ? `${that.imageSrc}&original=true`
          : `${that.imageSrc}?original=true`
        that._fetchBlob(originalUrl).then(blobUrl => {
          that.previewUrl = blobUrl
        }).catch(() => {})
      },
      onPreviewImageLoad(e) {
        let that = this
        let naturalWidth = e.target.naturalWidth
        if (naturalWidth) {
          let maxWidth = Math.floor(window.innerWidth * 0.9)
          that.previewWidth = Math.max(320, Math.min(naturalWidth + 48, maxWidth))
        }
      },
      onPreviewCancel() {
        this.previewVisible = false
      },
    },
    mounted: function () {
      this.init()
    }
  };
</script>

<style lang="scss" scoped>
  .autherized-img {
    display: inline-block;
    .autherized-img-thumb {
      display: block;
      width: 100%;
      cursor: zoom-in;
    }
    .preview-modal{
      /deep/ .ant-modal-close-x{
        opacity: 0.8;
        &:hover{
          opacity: 1;
          .ant-modal-close-icon{
            transform: scale(1.2);
            color: #1f1f1f;
          }
        }
      }
    }
  }
</style>
