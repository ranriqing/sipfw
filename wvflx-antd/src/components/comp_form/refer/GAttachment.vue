<template>
  <div>
    <div v-if="readonly">
      <div v-for="(fileItem, index) in fileList" :key="index">
        <a-icon type="paper-clip" style="margin-right: 6px;" />
        <a target="_blank" :href="_fileUuidUrl(fileItem['uid'])">【{{ fileItem.type || '普通附件' }}】{{ fileItem.name | Ellipsis(30) }}</a>
      </div>
    </div>
    <div v-else>
      <a-upload
        :multiple="true"
        :disabled="disabled"
        :fileList="fileList"
        :showUploadList="false"
        :customRequest="customRequest"
        :remove="onRemove"
        @change="onChange">
        <div v-if="!disabled && fileList.length < fileLimit">
          <template v-if="cptRequiredFiles.length>0">
            <a-button
              v-for="(requiredFile, index) in cptRequiredFiles"
              size="small"
              :key="index"
              style="margin-right: 8px;"
              @click="requiredInfo=requiredFile">
              <a-icon type="upload" />
              {{ requiredFile['fileType'] }}
            </a-button>
          </template>
          <template v-else>
            <a-button size="small" @click="requiredInfo={}">
              <a-icon type="upload" />
              上传附件
            </a-button>
          </template>
        </div>
      </a-upload>
      <div
        class="flex flex-column"
        style="width: 100%; padding: 0; row-gap: 10px; column-gap: 10px;">
        <div
          class="attachment-file"
          v-for="(fileItem, index) in fileList"
          :key="index">
          <div class="box-left">
            <a-icon type="file-pdf" class="left-icon"/>
            <div class="flex flex-column main-between" style="padding-bottom: 2px;">
              <div class="file-name" :title="fileItem.name">
                <a target="_blank" :href="_fileUuidUrl(fileItem['uid'])">【{{ fileItem.type || '普通附件' }}】{{ fileItem.name | Ellipsis(30) }}</a>
              </div>
              <div>
                <span v-if="cptStartUpload">{{ fileItem['uid'] }}</span>
                <span v-else class="text-tips" style="margin-right: 8px;">{{ fileItem['actorCaption'] }}: {{ fileItem['dateCreated'] }}</span>
              </div>
            </div>
          </div>
          <div class="box-right">
            <a
              v-if="_allowRemove(fileItem)"
              class="item-action item-trash"
              @click="onRemove(fileItem)">
              <a-icon type="delete"/>
            </a>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<!--
# 组件说明
  为实体提供附件管理能力；
  附件大小不能超过20M；
# 配置组件

# 操作方法
  onRefresh()
-->

<script>
  import AUpload from 'ant-design-vue/lib/upload';
  import AEmpty from 'ant-design-vue/lib/empty';

  const settings = require('@/settings')
  import { stores } from '@/utils/util'
  import { vTableUtil } from '@/utilsv/vUtil'
  import { postAction, deleteAction, formAction, uploadFile } from '@/api/api'
  import { OTHER } from '@/constants'

  export default {
    name: 'GAttachment',
    components: {
      AUpload,
      AEmpty,
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
      placeholder: {
        type: String,
        default: ''
      },
      /** 扩展Props */
      record: {
        type: Object,
        required: false
      },
      requiredFiles: {
        type: Array,
        default: function(){
          return []
        }
      },
      fileLimit: {
        type: Number,
        default: 10
      },
    },
    watch: {
      record: {
        immediate: true,
        handler(newValue, oldValue) {
          let that = this
          oldValue || true
          if(that._.get(newValue, 'id')){
            that._loadData(1)
          }else{
            that.fileList = []
          }
        }
      },
      value: function() {
        let that = this
        // 回显初始值
        if(that.cptStartUpload){
          that.fileList = that.value?.map( item => {
            return {
              uid: item['fileUuid'],
              name: item['fileName'],
              type: item['fileType'],
              status: 'done',
              url: that._fileUuidUrl(item['fileUuid']),
            }
          }) || []
        }
      }
    },
    data(){
      return {
        /** 配置&状态 */
        baseUrl: settings.BASE_URL,
        loading:false,
        stores: stores,
        /** 文件列表 */
        requiredInfo: {},
        fileList: [],
        loadedFileIds: [],
      }
    },
    computed: {
      cptStartUpload: function(){
        let that = this
        let domainId = that._.get(that.record, 'id')
        return !domainId
      },
      cptMetaDomainName: function(){
        let that = this
        return that._.get(that.item,`extend.${OTHER.META_DOMAIN_NAME}`)
      },
      cptRequiredFiles() {
        let that = this
        let rstRequiredFiles = []
        that.requiredFiles.forEach(item => {
          let fileType = item['fileType']
          let fileNum = item['fileNum'] || 1
          let typeFileList = that.fileList.filter(item => item['type'] === fileType)
          if(typeFileList.length >= fileNum){
            // do nothing
          }else{
            rstRequiredFiles.push(item)
          }
        })
        return rstRequiredFiles
      },
    },
    methods: {
      /** 查询数据 */
      onRefresh(){
        this._loadData()
      },
      _loadData() {
        let that = this
        /* 查询条件 */
        let queryCondition = {
          pageNum: 1,
          pageSize: 30,
          orderBy: null,
          condition: 'and',
          rules: []
        }
        // 加载数据
        that.loading = true
        let domainId = that._.get(that.record, 'id')
        if(!domainId || !that.cptMetaDomainName){
          that.fileList = []
          return
        }
        let queryUri = `/gcsAttachment/index?domainName=${that.cptMetaDomainName}&domainId=${domainId}`
        postAction(queryUri, queryCondition).then(resp => {
          that.loading = false
          if(resp.gmodel && resp.gmodel.entityList){
            that.fileList = resp.gmodel.entityList.map(item => {
              return {
                id: item['id'],
                uid: item['fileUuid'],
                name: item['fileName'],
                status: 'done',
                url: that._fileUuidUrl(item['fileUuid']),
                type: item['type'],
                actorId: item['actorId'],
                actorCaption: item['actorCaption'],
                dateCreated: item['dateCreated'],
              }
            })
            that.triggerChange()
            that.loadedFileIds = that.fileList.map(item => item.id)
          }
        }).catch(err => {
          that.loading = false
          vTableUtil.tableReqErr(that, err, '加载失败')
        })
      },
      /** 执行操作 */
      onChange (info) {
        let that = this
        // 上传前校验
        if(!that._beforeUpload(info.file)){
          return
        }
        that.fileList = info.fileList
        let fileStatus = that._.get(info, 'file.status')
        if(['done', 'removed'].indexOf(fileStatus) !== -1){
          if(that.cptStartUpload){
            // 上传或删除文件
            that.fileList = that.fileList.map(infoItem => {
              let response = that._.get(infoItem,'response')
              if(response){
                let respItem = that._.get(response,'gmodel[0]')
                return {
                  uid: respItem['fileUuid'],
                  name: respItem['fileName'],
                  status: 'done',
                  url: that._fileUuidUrl(respItem['fileUuid']),
                  type: respItem['uploadType'],
                }
              }else{
                return infoItem
              }
            })
          }else{
            that.fileList = that.fileList.map(infoItem => {
              let response = that._.get(infoItem,'response')
              if(response){
                let respItem = that._.get(response,'gmodel')
                return {
                  id: respItem['id'],
                  uid: respItem['fileUuid'],
                  name: respItem['fileName'],
                  status: 'done',
                  url: that._fileUuidUrl(respItem['fileUuid']),
                  type: respItem['type'],
                  actorId: respItem['actorId'],
                  actorCaption: respItem['actorCaption'],
                  dateCreated: respItem['dateCreated'],
                }
              }else{
                return infoItem
              }
            })
          }
          that.triggerChange()
        }
      },
      triggerChange(){
        let that = this
        /* 触发change */
        let outFileList = that.fileList.map( item => {
          return {
            fileUuid: item['uid'],
            fileName: item['name'],
            fileType: item['type'],
          }
        })
        that.$emit('change', outFileList)
      },
      onRemove(item){
        let that = this
        return new Promise((resolve, reject) => {
          that.$confirm({
            title: '确认弹窗',
            content: `确定要删除附件 [${item.name}] ?`,
            onOk() {
              let deleteUri = "/gcsAttachment/delete"
              deleteAction(deleteUri, item.id, {}).then(() => {
                let fileIndex = that.fileList.findIndex( fileItem => fileItem.id === item.id)
                if(fileIndex !== -1){
                  that.fileList.splice(fileIndex, 1)
                }
                resolve(true)
              }).catch(err => {
                vTableUtil.tableReqErr(that, err, '删除失败')
                reject(false)
              })
            }
          });
        })
      },
      customRequest({onSuccess, onError, file}){
        let that = this
        let formData = new FormData();
        formData.set('upload_file', file)
        let uploadType = that._.get(that.requiredInfo, 'fileType')
        if(uploadType){
          formData.set('upload_type', uploadType)
        }
        /** 上传附件【作为表单项】 */
        if(that.cptStartUpload){
          uploadFile(formData).then((resp)=>{
            onSuccess(resp, file)
          }).catch(err => {
            onError(err)
          })
        /** 上传附件【作为组件】  */
        }else{
          let domainId = that._.get(that.record, 'id')
          let uploadUrl = `/gcsAttachment/upload?domainName=${that.cptMetaDomainName}&domainId=${domainId}`
          formAction(uploadUrl, formData).then(resp=>{
            onSuccess(resp, file)
          }).catch(err => {
            onError(err)
          })
        }
      },
      /** 工具方法 */
      _fileUuidUrl(fileUuid){
        let that = this
        return `${that.baseUrl}/gcsStorage/downloadFile/${fileUuid}?inline=true`
      },
      _allowRemove(item){
        let that = this
        if(that.disabled){
          return false
        }
        let allow = false
        if(that.loadedFileIds.indexOf(item.id)===-1){
          allow = true  // 新上传文件允许删除
        } else if(that._.get(stores.currentUser(), 'id') === item.actorId){
          let requiredFile = that.requiredFiles.find(requiredItem => requiredItem.fileType === that._.get(item, 'type'))
          if(!that._.get(item, 'type') || that._.get(requiredFile, 'allowRemove')){
            allow = true // 属于我的允许删除
          }
        }
        return allow
      },
      _beforeUpload(file){
        if(!file.type){
          return true
        }
        let that = this
        let isLt2M = file.size / 1024 / 1024 < 20
        if (!isLt2M) {
          that.$message.error('文件大小不能超过20MB!')
        }
        return isLt2M
      },
    },
  }
</script>

<style lang="scss" scoped>
  /deep/ .ant-upload{
    // display: block;
    // width: 100%;
  }
  .attachment-file {
    display: flex;
    flex-direction: row;
    justify-content: space-between;
    border: 1px solid #d9d9d9;
    border-radius: 5px;
    padding: 8px;
    line-height: 20px;
    .box-left{
      display: flex;
      flex-direction: row;
      justify-content: flex-start;
      .left-icon{
        font-size: 38px;
        color: #d0d0d0;
      }
      .file-name{
        font-size: 14px;
        // font-weight: bold;
      }
    }
    .box-right{
      display: flex;
      flex-direction: row;
      justify-content: center;
      align-items: center;
    }

  }
  /deep/ .ant-upload-list-item{
    height: 26px;
    margin-top: 4px;
    .ant-upload-list-item-info{
      // padding-top: 5px;
      // padding-bottom: 5px;
      span {
        display: flex;
        flex-direction: row;
        align-items: center;
        > i{
          position: unset;
        }
        .ant-upload-list-item-name {
          padding-left: 10px;
        }
      }
    }
  }
</style>
