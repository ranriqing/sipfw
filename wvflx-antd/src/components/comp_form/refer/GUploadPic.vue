<template>
  <div>
    <div v-if="readonly">
      <div v-for="(fileItem, index) in fileList" :key="index">
        <a-icon type="paper-clip" style="margin-right: 6px;" />
        <a target="_blank" :href="fileItem.url">{{ fileItem.name }}</a>
      </div>
    </div>
    <a-upload
      v-else
      listType="picture-card"
      :disabled="disabled"
      :fileList="fileList"
      accept=".jpg,.jpeg,.png,.gif,.bmp,.webp,image/*"
      :customRequest="customRequest"
      @preview="onPreview"
      @change="onChange">
      <div v-if="!disabled && fileList.length < _.defaultTo(_.get(item,'extend.fileLimit'), 3)">
        <a-icon class="upload-icon" type="cloud-upload" />
        <div class="ant-upload-text">上传图片</div>
      </div>
    </a-upload>
    <a-modal
      class="preview-modal"
      :footer="null"
      :visible="previewVisible"
      @cancel="onPreviewCancel">
      <img style="width: 100%" :src="previewUrl" />
    </a-modal>
  </div>
</template>

<!--
# 组件说明
  上传图片组件，仅可以上传图片类型文件，支持图片预览；
  外部数组格式 [{ fileUuid: $uuid, fileName: $fileName }];
  内部数组格式 [{ uid: $uid, name: $name, status: $status, url: $url }];
  强制限制图片文件大小，最大不能超过20M；
  可以限制图片上传数量，默认限制上传数量3个；
# 配置组件
  value: Array        // 组件数值数组
  disabled: Boolean   // 禁用状态，默认false
  extend.fileLimit: Number   // 文件数量限制，默认3
# 操作方法
  $emit('change', outFileList)
-->

<script>
  import AUpload from 'ant-design-vue/lib/upload';

  const settings = require('@/settings')
  import { uploadFile } from '@/api/api'

  export default {
    name: 'GUploadPic',
    components: {
      AUpload,
    },
    /* eslint-disable */
    props: {
      item: {
        type: Object,
        required: true
      },
      value: {
        type: Array,
        required: false
      },
      readonly: {
        type: Boolean,
        default: false
      },
      disabled: {
        type: Boolean,
        default: false
      },
    },
    data(){
      return {
        baseUrl: settings.BASE_URL,
        fileList: [],
        previewVisible: false,
        previewUrl: '',
      }
    },
    watch: {
      value: {
        immediate: true,
        handler(newValue) {
          let that = this
          // 回显初始值
          that.fileList = newValue?.map(item => {
            return {
              uid: item['fileUuid'],
              name: item['fileName'],
              status: 'done',
              url: that._resourceUrl(item['fileUuid']),
            }
          }) || []
        }
      }
    },
    methods: {
      onChange (info) {
        let that = this
        // 上传前校验
        let fileStatus = that._.get(info, 'file.status')
        if(fileStatus==='done' && !that._beforeUpload(info.file)){
          return
        }
        that.fileList = info.fileList

        if(['done', 'removed'].indexOf(fileStatus) !== -1){
          // 上传或删除文件
          that.fileList = that.fileList.map(infoItem => {
            let response = that._.get(infoItem,'response')
            if(response){
              let respItem = that._.get(response,'gmodel[0]')
              return {
                uid: respItem['fileUuid'],
                name: respItem['fileName'],
                status: 'done',
                url: that._resourceUrl(respItem['fileUuid']),
              }
            }else{
              return infoItem
            }
          })
          // 触发change事件
          let outFileList = that.fileList.map( item => {
            return {
              fileUuid: item['uid'],
              fileName: item['name'],
            }
          })
          that.$emit('change', outFileList)
        }
      },
      /** 扩展方法 */
      customRequest({onSuccess, onError, file}){
        let formData = new FormData();
        formData.set('upload_file', file)
        uploadFile(formData).then((resp)=>{
          onSuccess(resp, file)
        }).catch(err => {
          onError(err)
        })
      },
      onPreview(file){
        this.previewUrl = file.url
        this.previewVisible = true
      },
      onPreviewCancel () {
        this.previewVisible = false
      },
      /** 工具方法 */
      _resourceUrl(fileUuid){
        let that = this
        return `${that.baseUrl}/gcsStorage/downloadFile/${fileUuid}?inline=true`
      },
      _beforeUpload(file){
        let that = this
        if(!file.type){
          that.$message.warn('未能识别文件类型!')
          return false
        }
        let isImage = file.type.startsWith('image/')
        if (!isImage) {
          that.$message.warn('只能上传图片文件!')
        }
        let isLt2M = file.size / 1024 / 1024 < 20
        if (!isLt2M) {
          that.$message.warn('文件大小不能超过20MB!')
        }
        return isImage && isLt2M
      },
    },
  }
</script>


<style lang="scss" scoped>
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
  .ant-upload-select{
    .upload-icon{
      color: grey;
      font-size: 35px;
    }
    &:hover{
      .upload-icon{
        color: #4f4f4f;
      }
    }
  }

</style>

