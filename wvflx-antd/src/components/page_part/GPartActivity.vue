<template>
  <div>
    <span style="float: right;">
      <a-button size="small" type="link" @click="onRefresh"><a-icon type="reload"/>刷新</a-button>
    </span>
    <div>
      <a-button
        :class="{'op-btn': true, 'open': noteOpen}"
        size="small"
        type="link"
        @click="noteOpen=!noteOpen">
        写记录
      </a-button>
      <a-button class="op-btn" size="small" type="link" @click="onSchedule({})">
        <a-icon type="clock-circle" />
        添加日程
      </a-button>
      <div v-if="noteOpen" class="note-input">
        <a-textarea
          placeholder="输入内容..."
          :rows="3"
          v-model="noteValue"
          @focusin="3"
          @focusout="3">
        </a-textarea>
        <a-button size="small" type="primary" style="margin-top: 6px;" @click="doSaveNote"><a-icon type="check"/>保存</a-button>
      </div>
      <div
        class="flex flex-column"
        style="margin-top: 6px; width: 100%; padding: 0; row-gap: 10px; column-gap: 10px;">
        <template v-if="cptActiveSchedules.length > 0">
          <a-divider :dashed="true" style="font-size: 13px; margin: 0; ">待办日程</a-divider>
          <div
            class="activity-item"
            v-for="(activityItem) in cptActiveSchedules"
            :key="activityItem.id">
            <AvatarUser
              class="box-avatar"
              :userId="activityItem.assigneeId"
              :subIcon="activity.typeIcon(activityItem.sciScheduleType)"
              :subIconType="activity.dateStrAndClass(activityItem.dueDate)[1]"
              :showDetail="false"/>
            <div class="box-content">
              <div class="content-header">
                <!-- 截至日 -->
                <span
                  :title="activityItem.dueDate"
                  :class="['due-date', activity.dateStrAndClass(activityItem.dueDate)[1]]">
                  {{ activity.dateStrAndClass(activityItem.dueDate)[0] }}:&nbsp;
                </span>
                <!-- 标题或计划类型 -->
                <div class="activity-title">
                  <span v-if="activityItem.title">
                    "{{ activityItem.title | Ellipsis(15) }}"
                  </span>
                  <span v-else>
                    {{ _.get(stores.scElement('sciScheduleType', _.get(activityItem, 'sciScheduleType')), 'title') }}
                  </span>
                </div>
                <!-- 操作人 -->
                <span class="assignee">
                  &nbsp; <a-icon type="user"/> {{ _.get(stores.gcsUser(activityItem.actorId), 'nickname') }}
                </span>
              </div>
              <div class="content-detail">
                <!-- 调试：不展示 -->
                <template v-if="false">
                  <GScCascader
                    style="display: inline-block"
                    :readonly="true"
                    :disabled="true"
                    :value="[_.get(activityItem, 'sciScheduleType')]"
                    :item="{
                      extend: {
                        scKey: 'sciScheduleType',
                      }
                    }">
                  </GScCascader> <br />
                </template>
                <!-- 计划详情 -->
                <GTipTapEditor
                  :readonly="true"
                  :value="_.toString(activityItem.detail)">
                </GTipTapEditor>
              </div>
              <!-- 有权限：展示操作按钮 -->
              <div class="content-footer" v-if="_allowEdit(activityItem)">
                <a class="ops-btn" @click="onScheduleDone(activityItem)"><a-icon type="check" />完成</a>
                <a class="ops-btn" @click="onSchedule(activityItem)"><a-icon type="edit" />编辑</a>
                <a class="ops-btn" @click="onScheduleRemove(activityItem)"><a-icon type="close" />删除</a>
              </div>
            </div>
          </div>
        </template>
        <div v-for="(activityItem, index) in cptFinishedActivities" :key="activityItem.id">
          <a-divider v-if="activityItem['extWeekDateStr']!==_.get(cptFinishedActivities[index-1], 'extWeekDateStr')" style="font-size: 13px; margin: 0; ">
            {{ activityItem['extWeekDateStr'] }}
          </a-divider>
          <div class="activity-item">
            <!-- 活动类型：计划 -->
            <template v-if="activityItem.dueDate">
              <AvatarUser
                class="box-avatar"
                :userId="activityItem.assigneeId"
                :subIcon="undefined"
                :subIconType="activity.dateStrAndClass(activityItem.dueDate)[1]"
                :showDetail="false"/>
              <div class="box-content">
                <div class="content-header">
                  <span class="assignee">
                    <!-- 指派或办理人 -->
                    <a-icon type="user"/> {{ _.get(stores.gcsUser(activityItem.assigneeId), 'nickname') }}
                    &nbsp; {{ activityItem.finishDate | PrettyDate }}
                  </span>
                </div>
                <div class="content-detail">
                  <!-- 计划类型&计划标题 -->
                  <div>
                    <a-icon style="font-size: 14px;" :type="activity.typeIcon(activityItem.sciScheduleType)" theme="filled"/>
                    <span style="font-size: 14px; margin-left: 5px;">{{ _.get(stores.scElement('sciScheduleType', _.get(activityItem, 'sciScheduleType')), 'title') }}</span>
                    <span v-if="activityItem.title" style="margin-left: 8px; font-weight: bold;">
                      {{ activityItem.title || Ellipsis(15) }}
                    </span>
                  </div>
                  <!-- 计划详情 -->
                  <GTipTapEditor
                    :readonly="true"
                    :value="_.toString(activityItem.detail)">
                  </GTipTapEditor>
                  <!-- 完成情况 -->
                  <div v-if="activityItem.comment">
                    <span class="text-tips" style="font-weight: bold; font-size: 12px;">备注：</span>
                    <span>{{ activityItem.comment }}</span>
                  </div>
                </div>
              </div>
            </template>
            <!-- 普通记录 -->
            <template v-else>
              <AvatarUser
                class="box-avatar"
                :userId="activityItem.actorId"
                :subIcon="undefined"
                :subIconType="undefined"
                :showDetail="false"/>
              <div class="box-content">
                <div class="content-header">
                  <span class="assignee">
                    <!-- 操作人 -->
                    <a-icon type="user"/> {{ _.get(stores.gcsUser(activityItem.actorId), 'nickname') }}
                    &nbsp; {{ activityItem.finishDate | PrettyDate }}
                  </span>
                </div>
                <div class="content-detail">
                  <!-- 操作标题 -->
                  <div>
                    <span style="margin-left: 8px; font-weight: bold;">
                      {{ activityItem.title || Ellipsis(15) }}
                    </span>
                  </div>
                </div>
              </div>
            </template>
          </div>
        </div>
      </div>
    </div>
    <div v-if="tableDataSource.length===0 && activeDataSource.length===0" class="flex flex-row main-center">
      <a-spin v-if="moreLoading" style="padding: 20px 0;" />
      <a-empty v-else :imageStyle="{height: '70px'}" description="暂无活动"/>
    </div>
    <div v-else-if="loadMoreVisible">
      <a-spin :spinning="moreLoading">
        <a-button block icon="down" @click="onLoadMore">
          加载更多
        </a-button>
      </a-spin>
    </div>
  </div>
</template>

<!--
# 组件说明
  为实体提供活动管理能力；
# 配置组件
  configObj:{
      recordId: Long      // 实体Id
      domainName: String  // 实体Name
  }
# 操作方法
  onRefresh()
-->

<script>
  import ATabs from 'ant-design-vue/lib/tabs';
  import AUpload from 'ant-design-vue/lib/upload';
  import AEmpty from 'ant-design-vue/lib/empty';
  import AAvatar from 'ant-design-vue/lib/avatar';

  const settings = require('@/settings')
  import {vActivityUtil, vTableUtil} from '@/utilsv/vUtil'
  import { postAction, deleteAction } from '@/api/api'
  import { stores, tools, activity } from "@/utils/util";
  import GScCascader from "@/components/comp_form/select/GScCascader.vue";
  import DATE from "@/constants/consts-date";
  import GTipTapEditor from "@/components/comp_form/input/GTipTapEditor.vue";

  export default {
    name: 'GPartActivity',
    components: {
      GTipTapEditor,
      GScCascader,
      AUpload,
      AEmpty,
      AAvatar,
      ATabs,
      ATabPane: ATabs.TabPane,
    },
    props: {
      configObj: {
        type: Object,
        required: true,
      },
    },
    data(){
      return {
        /** 配置&状态 */
        DATE: DATE,
        baseUrl: settings.BASE_URL,
        stores: stores,
        tools: tools,
        activity: activity,
        moreLoading: false,
        loadMoreVisible: false,
        /** 记录活动 */
        noteOpen: false,
        noteValue: undefined,
        /** 活动列表 */
        queryRules: [],
        pageNum: 1,
        pageSize: 10,
        activeDataSource: [],
        tableDataSource: [],
      }
    },
    computed: {
      cptActiveSchedules: function(){
        let that = this
        let activeSchedules = that.activeDataSource.filter(record => record.isActive && record.dueDate)
        return that._.sortBy(activeSchedules, ['dueDate', 'id'])
      },
      cptFinishedActivities: function(){
        let that = this
        let activeFinishArray = that.activeDataSource.filter(record => !(record.isActive && record.dueDate))
        let finishedSchedules = that._.cloneDeep(that.tableDataSource)
        finishedSchedules.push(...that._.cloneDeep(activeFinishArray))
        return that._.orderBy(finishedSchedules, ['finishDate', 'dateCreated', 'id'], ['desc', 'desc', 'desc'])
      },
    },
    methods: {
      /** 查询数据 */
      onRefresh(){
        this._loadData(1)
      },
      onLoadMore(){
        this._loadData()
      },
      _loadData(arg) {
        let that = this
        let reqPageNum
        if (arg === 1) {
          that.pageNum = 1;
          reqPageNum = 1
          that.tableDataSource=[]
        }else{
          reqPageNum = that.pageNum+1
        }
        /* 查询条件 */
        let queryCondition = {
          pageNum: reqPageNum,
          pageSize: that.pageSize,
          condition: 'and',
          rules: [],
          orderBy: null,
        }
        that.queryRules.forEach(rule => {
          queryCondition.rules.push(rule)
        })
        // 加载数据
        that.moreLoading = true
        let queryUri = `/gcsActivity/index?domainName=${that.configObj.domainName}&domainId=${that.configObj.recordId}`
        postAction(queryUri, queryCondition).then(resp => {
          that.moreLoading = false
          that.activeDataSource = that._.get(resp, 'gmodel.activeList')
          let entityList = that._.get(resp, 'gmodel.entityList')
          if(entityList && entityList.length>0){
            that.pageNum = reqPageNum // 页码赋值(失败不增加页码)
            that.tableDataSource = that.tableDataSource.concat(entityList)
          }
          that.loadMoreVisible = entityList?.length === that.pageSize
        }).catch(err => {
          that.moreLoading = false
          vTableUtil.tableReqErr(that, err, '加载失败')
        })
      },
      /** 执行操作 */
      doSaveNote(){
        let that = this
        let saveUri = `/gcsActivity/saveNote?domainName=${that.configObj.domainName}&domainId=${that.configObj.recordId}`
        let promise = postAction(saveUri, {title: that.noteValue})
        promise.then(resp => {
          let respRecord = resp.gmodel
          that.noteOpen = false
          that.noteValue = undefined
          that.tableDataSource.unshift(respRecord)
          that.$message.success(`记录成功`);
        }).catch(err => {
          vTableUtil.tableReqErr(that, err, `记录失败`)
        })
        return promise
      },
      onSchedule(record){
        let that = this
        let _title = undefined
        if(that._.get(record, 'id')){
          _title = '编辑日程'
        }
        vActivityUtil.showScheduleModal(
          that, record, {
            domainName: that.configObj.domainName,
            recordId: that.configObj.recordId,
            _title: _title,
          },
          (respRecord, context) => {
            context || true
            if(record.id){ // 更新
              let activity = that.activeDataSource.findIndex( item => item.id === respRecord.id)
              if(activity !== -1){
                that.activeDataSource.splice(activity, 1, respRecord)
              }
            }else{ // 保存
              that.activeDataSource.unshift(respRecord)
            }
          }, () => {
            /* cancel */
          }
        )
      },
      onScheduleDone(record){
        let that = this
        vActivityUtil.showFinishModal(
          that, record, {},
          (respRecord, context) => {
            context || true
            let activity = that.activeDataSource.findIndex( item => item.id === respRecord.id)
            if(activity !== -1){
              that.activeDataSource.splice(activity, 1, respRecord)
            }
          }, () => {
            /* cancel */
          }
        )
      },
      onScheduleRemove(record){
        let that = this
        return new Promise((resolve, reject) => {
          that.$confirm({
            title: '确认',
            content: `确定删除该日程？`,
            onOk() {
              let deleteUri = "/gcsActivity/delete"
              deleteAction(deleteUri, record.id, {}).then(() => {
                let activityIndex = that.activeDataSource.findIndex( activityItem => activityItem.id === record.id)
                if(activityIndex !== -1){
                  that.activeDataSource.splice(activityIndex, 1)
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
      /** 工具方法 */
      _allowEdit(item){
        let that = this
        if(that.disabled){
          return false
        }
        let allow = false
        if(that._.get(stores.currentUser(), 'id') === item.actorId){
          allow = true  // 我创建的活动允许
        }
        return allow
      },
    },
    mounted() {
      this._loadData(1)
    },
  }
</script>

<style lang="scss" scoped>
.op-btn{
  border-radius: 0;
  &.open{
    border: 1px solid #dee2e6;
    background-color: #f2f4f5;
    border-bottom: transparent;
    transform: translateY(1px);
  }
}
.note-input{
  padding: 8px 12px 12px 12px;
  background-color: #f2f4f5;
  border: 1px solid #dee2e6;
  margin-top: -1px;
}
.activity-item {
  display: flex;
  flex-direction: row;
  padding: 8px 4px;
  // background: linear-gradient(180deg, rgba(92, 94, 98, 0.05) 0, rgba(92, 94, 98, 0) 50%);
  border-bottom: 1px solid rgba(92, 94, 98, 0.1);
  .box-avatar{
    margin-right: 10px;
  }
  .box-content{
    flex: 1;
    display: flex;
    flex-direction: column;
    .content-header{
      .due-date{
        font-weight: bold;
      }
      .activity-title{
        font-weight: bold;
        display: inline-block;
      }
      .assignee{
        font-size: 12px;
        color: #909399;
      }
    }
    .content-detail{

    }
    .content-footer{
      margin-top: 6px;
      .ops-btn{
        font-size: 12px;
        display: inline-block;
        margin-right: 12px;
        color: #c0c4cc;
        font-weight: bold;
        &:hover{
          color: #606266;
        }
        .anticon{
          margin-right: 4px;
          font-weight: 700;
        }
      }
    }
  }
}
.empty-text{
  padding: 10px;
  border: 1px solid #eee;
}
</style>
