<template>
  <div>
    <div v-if="readonly">
      <div
        class="flex flex-column"
        style="width: 100%; padding: 0; row-gap: 10px; column-gap: 10px;">
        <div
          class="activity-item"
          v-for="(activityItem) in cptActiveSchedules"
          :key="activityItem.id">
          <div class="box-left">
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
                {{ $moment(activityItem.dueDate).format(DATE.YYYYMMDD) }}
                <div style="margin-left: 10px;">{{ activityItem.detail | Ellipsis(15) }}</div>
              </div>
            </div>
          </div>
          <div class="box-right">
          </div>
        </div>
      </div>
    </div>
    <div v-else>
      <a-button v-if="!disabled" size="small" @click="onSchedule({})">
        <a-icon type="clock-circle" />
        添加日程
      </a-button>
      <div
        class="flex flex-column"
        style="width: 100%; padding: 0; row-gap: 10px; column-gap: 10px;">
        <div
          class="activity-item"
          v-for="(activityItem) in cptActiveSchedules"
          :key="activityItem.id">
          <div class="box-left">
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
                <!-- 计划详情 -->
                {{ $moment(activityItem.dueDate).format(DATE.YYYYMMDD) }}
                <div style="margin-left: 10px;">{{ activityItem.detail | Ellipsis(15) }}</div>
              </div>
            </div>
          </div>
          <div class="box-right">
            <div v-if="_allowEdit(activityItem)">
              <template v-if="!cptStartActivity">
                <a
                  class="item-action"
                  @click="onSchedule(activityItem)">
                  <a-icon type="edit"/>
                </a>
                <a-divider type="vertical" />
              </template>
              <a
                class="item-action"
                @click="onRemove(activityItem)">
                <a-icon type="delete"/>
              </a>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<!--
# 组件说明
  为实体提供活动管理能力；
# 配置组件

-->

<script>
  import AUpload from 'ant-design-vue/lib/upload';
  import AEmpty from 'ant-design-vue/lib/empty';
  import GScCascader from '@/components/comp_form/select/GScCascader.vue'

  const settings = require('@/settings')
  import { stores, activity } from '@/utils/util'
  import { vTableUtil, vActivityUtil } from '@/utilsv/vUtil'
  import { deleteAction, postAction } from '@/api/api'
  import { OTHER } from '@/constants'
  import DATE from '@/constants/consts-date'

  export default {
    name: 'GActivity',
    components: {
      GScCascader,
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
            that.activityList = []
          }
        }
      },
      value: function() {
        let that = this
        if(that.cptStartActivity){
          // 初始为空
        }else{
          // 动态加载
        }
      }
    },
    data(){
      return {
        /** 配置&状态 */
        DATE: DATE,
        baseUrl: settings.BASE_URL,
        loading:false,
        stores: stores,
        activity: activity,
        /** 活动列表 */
        activityList: [],
        loadedActivityIds: [],
      }
    },
    computed: {
      cptStartActivity: function(){
        let that = this
        let domainId = that._.get(that.record, 'id')
        return !domainId
      },
      cptMetaDomainName: function(){
        let that = this
        return that._.get(that.item,`extend.${OTHER.META_DOMAIN_NAME}`)
      },
      cptActiveSchedules: function () {
        let that = this
        let activeSchedules = that.activityList.filter(record => record.isActive && record.dueDate)
        return that._.sortBy(activeSchedules, ['dueDate', 'id'])
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
          pageSize: 10,
          orderBy: null,
          condition: 'and',
          rules: []
        }
        // 加载数据
        that.loading = true
        let domainId = that._.get(that.record, 'id')
        if(!domainId || !that.cptMetaDomainName){
          that.activityList = []
          return
        }
        let queryUri = `/gcsActivity/index?domainName=${that.cptMetaDomainName}&domainId=${domainId}`
        postAction(queryUri, queryCondition).then(resp => {
          that.loading = false
          if(resp.gmodel && resp.gmodel.activeList){
            that.activityList = resp.gmodel.activeList
            that.loadedActivityIds = that.activityList.map(item => item.id)
            that.$emit('change', that.activityList) // 不写会导致：编辑必填无法保存
          }
        }).catch(err => {
          that.loading = false
          vTableUtil.tableReqErr(that, err, '加载失败')
        })
      },
      /** 执行操作 */
      onSchedule(record){
        let that = this
        let _title = undefined
        if(that._.get(record, 'id')){
          _title = '编辑日程'
        }
        vActivityUtil.showScheduleModal(
          that, record, {
            domainName: that.cptMetaDomainName,
            recordId: that._.get(that.record, 'id'),
            _title: _title,
          },
          (respRecord, context) => {
            context || true
            if(record.id){ // 更新
              let activity = that.activityList.findIndex( item => item.id === respRecord.id)
              if(activity !== -1){
                that.activityList.splice(activity, 1, respRecord)
              }
            }else{ // 保存
              that.activityList.unshift(respRecord)
            }
          }, () => {
            /* cancel */
          }
        )
      },
      onRemove(item){
        let that = this
        return new Promise((resolve, reject) => {
          that.$confirm({
            title: '确认',
            content: `确定删除该日程？`,
            onOk() {
              let deleteUri = "/gcsActivity/delete"
              deleteAction(deleteUri, item.id, {}).then(() => {
                let activityIndex = that.activityList.findIndex( activityItem => activityItem.id === item.id)
                if(activityIndex !== -1){
                  that.activityList.splice(activityIndex, 1)
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
        if(that.loadedActivityIds.indexOf(item.id)===-1){
          allow = true  // 新添加的活动允许
        } else if(that._.get(stores.currentUser(), 'id') === item.actorId){
          allow = true  // 我创建的活动允许
        }
        return allow
      },
    },
  }
</script>

<style lang="scss" scoped>
  .activity-item {
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
          display: flex;
          flex-direction: row;
        }
      }
    }
    .box-right{
      display: flex;
      flex-direction: row;
      justify-content: center;
      align-items: center;
    }
  }
</style>
