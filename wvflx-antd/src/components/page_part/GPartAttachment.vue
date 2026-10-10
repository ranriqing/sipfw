<template>
  <div>
    <span style="float: right;">
      <a-button type="link" @click="onRefresh"><a-icon type="reload"/>刷新</a-button>
    </span>
    <a-upload
      :fileList="fileList"
      :showUploadList="false"
      :customRequest="customRequest"
      :remove="onRemove"
      @change="onChange">
      <a-button size="small"> <a-icon type="upload" />上传附件</a-button>
      <div
        @click.stop=""
        class="flex flex-column"
        style="width: 100%; padding: 10px 0; row-gap: 10px; column-gap: 10px;">
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
                <span class="text-tips" style="margin-right: 8px;">{{ fileItem['actorCaption'] }}: {{ fileItem['dateCreated'] }}</span>
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
    </a-upload>
    <div
      v-if="fileList.length===0"
      class="empty-text">
      <a-empty description="暂无附件"/>
    </div>
  </div>
</template>

<!--
# 组件说明
  为实体提供附件管理能力；
  附件大小不能超过20M；
# 配置组件
  configObj:{
      recordId: Long      // 实体Id
      domainName: String  // 实体Name
  }
# 操作方法
  onRefresh()
-->

<script>
  import AUpload from 'ant-design-vue/lib/upload';
  import AEmpty from 'ant-design-vue/lib/empty';

  const settings = require('@/settings')
  import { vTableUtil } from '@/utilsv/vUtil'
  import { postAction, deleteAction, formAction } from '@/api/api'
  import {stores} from "@/utils/util";

  export default {
    name: 'GPartAttachment',
    components: {
      AUpload,
      AEmpty,
    },
    props: {
      configObj: {
        type: Object,
        required: true,
      },
      /** 扩展Props */
      requiredFiles: {
        type: Array,
        default: function(){
          return []
        }
      },
    },
    data(){
      return {
        /** 配置&状态 */
        baseUrl: settings.BASE_URL,
        loading:false,
        /** 文件列表 */
        fileList: [],
        loadedFileIds: [],
      }
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
        let queryUri = `/gcsAttachment/index?domainName=${that.configObj.domainName}&domainId=${that.configObj.recordId}`
        postAction(queryUri, queryCondition).then(resp => {
          that.loading = false
          if(resp.gmodel && resp.gmodel.entityList){
            that.fileList = resp.gmodel.entityList.map(item => {
              return {
                id: item['id'],
                uid: item['fileUuid'],
                name: item['fileName'],
                status: 'done',
                thumbUrl: "data:text/plain;base64,ICAwCTAxLjAwCiAgMQkyMS8xMC8yMDE5CiAgMgkxMTowMgogIDMJMTY1CiAgNAkgODIKICA1CTAKICA2CSBCKwogIDcJCiAgOAkKICA5CSAyNjYKIDEwCTExOjU4CiAxMQkgNDkKIDEyCTE0NQogMTMJNDIKIDE0CSAKIDE1CSAyMzY2CiAxNgkgMjM2NgogMTcJICAgCiAxOAkgCiAxOQkgCiAyMAkgCiAyMQkgCiAyMgkgCiAyMwkgICAKIDI0CSAgCiAzMwkzMDAwCiAzNAkgIDUKIDI1CSAKIDI2CQ==",
                url: that._fileUuidUrl(item['fileUuid']),
                type: item['type'],
                actorId: item['actorId'],
                actorCaption: item['actorCaption'],
                dateCreated: item['dateCreated'],
              }
            })
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
          that.fileList = that.fileList.map(infoItem => {
            let response = that._.get(infoItem,'response')
            if(response){
              let respItem = that._.get(response,'gmodel')
              return {
                id: respItem['id'],
                uid: respItem['fileUuid'],
                name: respItem['fileName'],
                status: 'done',
                thumbUrl: "data:text/plain;base64,ICAwCTAxLjAwCiAgMQkyMS8xMC8yMDE5CiAgMgkxMTowMgogIDMJMTY1CiAgNAkgODIKICA1CTAKICA2CSBCKwogIDcJCiAgOAkKICA5CSAyNjYKIDEwCTExOjU4CiAxMQkgNDkKIDEyCTE0NQogMTMJNDIKIDE0CSAKIDE1CSAyMzY2CiAxNgkgMjM2NgogMTcJICAgCiAxOAkgCiAxOQkgCiAyMAkgCiAyMQkgCiAyMgkgCiAyMwkgICAKIDI0CSAgCiAzMwkzMDAwCiAzNAkgIDUKIDI1CSAKIDI2CQ==",
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
        let uploadUrl = `/gcsAttachment/upload?domainName=${that.configObj.domainName}&domainId=${that.configObj.recordId}`
        formAction(uploadUrl, formData).then(resp=>{
          onSuccess(resp, file)
        }).catch(err => {
          onError(err)
        })
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
    mounted() {
      this._loadData()
    },
  }
</script>

<style lang="scss" scoped>
  /deep/ .ant-upload{
    display: block;
    width: 100%;
  }
  .attachment-file {
    display: flex;
    flex-direction: row;
    justify-content: space-between;
    border: 1px solid #d9d9d9;
    border-radius: 5px;
    padding: 8px;
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
  .empty-text{
    padding: 10px;
    border: 1px solid #eee;
  }
</style>
