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
      :disabled="disabled"
      :fileList="fileList"
      :customRequest="customRequest"
      @change="onChange">
      <div v-if="!disabled && fileList.length < _.defaultTo(_.get(item,'extend.fileLimit'), 3)">
        <a-button>
          <a-icon type="upload" /> 上传文件
        </a-button>
      </div>
    </a-upload>
  </div>
</template>

<!--
# 组件说明
  上传文件组件，可以上传任意类型文件；
  外部数组格式 [{ fileUuid: $uuid, fileName: $fileName }];
  内部数组格式 [{ uid: $uid, name: $name, status: $status, url: $url }];
  可以限制文件上传数量，默认限制上传数量3个；
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
    name: 'GUploadFile',
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
      /** 工具方法 */
      _resourceUrl(fileUuid){
        let that = this
        return `${that.baseUrl}/gcsStorage/downloadFile/${fileUuid}?inline=true`
      },
      _beforeUpload(file){
        let that = this
        let isLt2M = file.size / 1024 / 1024 < 200
        if (!isLt2M) {
          that.$message.warn('文件大小不能超过200MB!')
        }
        return isLt2M
      },
    },
  }
</script>
