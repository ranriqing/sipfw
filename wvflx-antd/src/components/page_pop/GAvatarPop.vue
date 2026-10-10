<template>
  <div>
    <GPlainPop
      ref="slotAvatar"
      :popVisible="popVisible"
      :configObj="configObj"
      :sContext="sContext"
      debugComp="GAvatarPop">
      <template #default="{record, context}">
        <div>
          <a-row>
            <!-- Logo裁切 -->
            <a-col :md="12" :xs="24" :style="{height: '350px'}">
              <vue-cropper
                ref="cropper"
                :info="true"
                :img="cropperOptions.img"
                :autoCrop="cropperOptions.autoCrop"
                :outputType="cropperOptions.outputType"
                :autoCropWidth="cropperOptions.autoCropWidth"
                :autoCropHeight="cropperOptions.autoCropHeight"
                :fixedBox="cropperOptions.fixedBox"
                @realTime="_handleRealTime">
              </vue-cropper>
            </a-col>
            <!-- Logo预览 -->
            <a-col :md="12" :xs="24" :style="{height: '350px'}">
              <div class="flex flex-column cross-center main-center" style="height: 100%; background-color: #f3f4f6;">
                <div class="avatar-upload-preview">
                  <img :src="previews.url" :style="previews.img"/>
                </div>
                <div class="avatar-upload-btn">
                  <a-upload
                    name="file"
                    :multiple="false"
                    :showUploadList="false"
                    :beforeUpload="_beforeUpload">
                    <a-button> <a-icon type="upload"/>更换Logo </a-button>
                  </a-upload>
                </div>
              </div>
            </a-col>
          </a-row>
        </div>
      </template>
    </GPlainPop>
  </div>
</template>

<!--
Note:
https://blog.csdn.net/qq_30632003/article/details/79639346
https://www.cnblogs.com/rakich/p/9814965.html
-->

<script>
  import AUpload from 'ant-design-vue/lib/upload';

  import GPlainPop from './GPlainPop'
  import { VueCropper } from 'vue-cropper'

  import { uploadAvatar } from '@/api/api'
  import { stores } from '@/utils/util'

  export default {
    name: "GAvatarPop",
    components: {
      AUpload,
      GPlainPop,
      VueCropper,
    },
    props: {
      popVisible: {
        type: Boolean,
        default: false,
      },
      configObj: {
        type: Object,
        default: function(){
          return {}
        }
      },
      sContext: {
        type: Object,
        default: function(){
          return {}
        }
      },
    },
    data () {
      return {
        /** 配置&状态 */
        stores: stores,
        cropperOptions: {
          img: null,
          autoCrop: true,
          fixedBox: false,
          outputType: 'png', /* Png可以解决透明自动转黑色背景问题 */
          autoCropWidth: 200,
          autoCropHeight: 200,
        },
        previews: {},
      }
    },
    methods: {
      /** 弹窗操作 */
      isPage(){
        let that = this
        let slotRef = that.$refs.slotAvatar
        return slotRef.isPage()
      },
      showPop (record, context, submitCallback, cancelCallback) {
        let that = this
        let slotRef = that.$refs.slotAvatar
        that.cropperOptions.img = context['_avatarUrl']
        that.avatarFileName = context['_avatarName']
        return slotRef.showPop(record, context,
          (record, context) => { /* submit */
          return new Promise((resolve, reject) => {
            // 上传图片
            let formData = new FormData();
            that.$refs.cropper.getCropBlob(data => {
              formData.append('uploadFile', data, that.avatarFileName);
              uploadAvatar(formData).then((resp)=>{
                let avatarName = resp.gmodel
                context['_avatarName'] = avatarName
                let promise0 = submitCallback(record, context)
                promise0.then(resp0 => {
                  resolve(resp0)
                }).catch(err0 => {
                  reject(err0)
                })
              }).catch(err => {
                that.$message.warn('上传Logo失败')
                reject(err)
              })
            })
          })
        }, cancelCallback)
      },
      /** 工具方法 */
      _beforeUpload(file){
        let that = this
        let reader = new FileReader();
        reader.onload =(e) => {
          let data;
          if (typeof e.target.result === 'object') {
            data = window.URL.createObjectURL(new Blob([e.target.result]))
          }else {
            data = e.target.result
          }
          that.cropperOptions.img = data
          that.avatarFileName = file.name;
        }
        reader.readAsArrayBuffer(file);
        return false
      },
      _handleRealTime (data) {
        this.previews = data
      }
    }
  }
</script>

<style lang="less" scoped>
  .bug-icon {
    color: rgba(241, 139, 98, 0.6);
    margin-right: 6px;
  }
  /* Logo预览 */
  .avatar-upload-preview {
    width: 200px;
    height: 200px;
    border-radius: 50%;
    box-shadow: 0 0 4px #ccc;
    overflow: hidden;
    img {
      width: 100%;
      height: 100%;
    }
  }
  /* 更换Logo */
  .avatar-upload-btn{
    margin-top: 20px;
  }
</style>
