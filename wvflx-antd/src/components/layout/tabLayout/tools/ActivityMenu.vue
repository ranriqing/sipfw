<template>
  <div class="header-dropdown-wrapper">
    <a-dropdown placement="bottomRight" trigger="click" @visibleChange="onVisibleChange">
      <div class="header-dropdown-link">
        <a-badge :count="badgeCount">
          <a-icon
            class="inner-icon"
            type="g-todo"/>
        </a-badge>
      </div>
      <a-menu
        slot="overlay"
        class="header-dropdown-menu"
        @click="onMenuClick">
        <!-- 日程安排 -->
        <a-menu-item-group class="schedule-group">
          <template slot="title">
            <span class="schedule-group-title flex flex-row cross-center">
              <a-icon type="calendar" class="schedule-group-icon" />
              <span>日程安排</span>
            </span>
          </template>
          <template v-for="(infoItem) in cptSchedules">
            <a-menu-item v-if="_.get(infoItem, 'referenceTitle')" :key="_.get(infoItem, 'reference')">
              <div class="flex flex-row" style="width: 290px;">
                <a-avatar
                  :size="46"
                  shape="square"
                  style="border: 1px solid #e6e9f0; margin-right: 10px; background-color: #00a2ae;">
                  {{ _.get(infoItem, 'referenceTitle', '无')[0] }}
                </a-avatar>
                <div class="flex flex-column item-flex">
                  <div class="flex flex-row main-between">
                    <div style="font-weight: bold;">{{ _.get(infoItem, 'referenceTitle') }}</div>
                    <div><a-icon type="clock-circle"/></div>
                  </div>
                  <div class="flex flex-row main-between">
                    <div>
                      <span
                        :class="{'text-tips':_.get(infoItem, 'before')===0, 'warning':_.get(infoItem, 'before')!==0}">
                        {{ _.get(infoItem, 'before') }} 逾期
                      </span>
                      <span
                        style="margin-left: 18px;"
                        :class="{'text-tips':_.get(infoItem, 'today')===0, 'success':_.get(infoItem, 'today')!==0}">
                        {{ _.get(infoItem, 'today') }} 今天
                      </span>
                    </div>
                    <div
                      :class="{'text-tips':_.get(infoItem, 'future')===0, 'primary':_.get(infoItem, 'future')!==0}">
                      {{ _.get(infoItem, 'future') }} 未来
                    </div>
                  </div>
                </div>
              </div>
            </a-menu-item>
          </template>
        </a-menu-item-group>
      </a-menu>
    </a-dropdown>
  </div>
</template>

<script>
  import ABadge from 'ant-design-vue/lib/badge';
  import AAvatar from 'ant-design-vue/lib/avatar';

  import { stores, tools } from '@/utils/util'
  import { mapGetters } from "vuex";
  import { GETTERS } from '@/constants'
  import {postAction} from "@/api/api";
  import {vTableUtil} from "@/utilsv/vUtil";

  export default {
    name: "ActivityMenu",
    components:{
      ABadge,
      AAvatar,
    },
    data() {
      return {
        /** 配置&状态 */
        tools: tools,
        stores: stores,
        summaryLoading: false,
        summaryList: [],
        taskSummary: { total: 0, today: 0, within10: 0, over10: 0 },
      }
    },
    computed: {
      ...mapGetters({
        isDesktop: GETTERS.CPT_APP_DEVICE_DESKTOP,
        currentUser: GETTERS.GET_USER_CURRENT_USER,
      }),
      cptSchedules: function(){
        let that = this
        let beforeOrTodaySchedules = that.summaryList || that.summaryList.filter(infoItem =>
          that._.get(infoItem, 'before')>0 || that._.get(infoItem, 'today')>0
        )
        return that._.orderBy(beforeOrTodaySchedules, ['before', 'today'], ['desc', 'desc'])
      },
      beforeCount: function(){
        return this._.sumBy(this.summaryList, 'before')
      },
      todayCount: function(){
        return this._.sumBy(this.summaryList, 'today')
      },
      badgeCount: function(){
        return this._.get(this.taskSummary, 'total', 0) + this.beforeCount + this.todayCount
      },
    },
    inject: ['switchRoute', 'wsSubscribe', 'wsUnSubscribe'],
    methods: {
      onMenuClick({ key }) {
        let that = this
        let reference = key;
        that.switchRoute(reference)
      },
      doQuery() {
        let that = this
        // 加载日程汇总
        that.summaryLoading = true
        let queryUri = `/gcsActivity/summarySchedule`
        let promise = postAction(queryUri, {})
        promise.then(resp => {
          that.summaryLoading = false
          that.summaryList = that._.get(resp, 'gmodel') || []
        }).catch(err => {
          err || true
          that.summaryLoading = false
          vTableUtil.tableReqErr(that, err, '加载失败')
        })
        return promise
      },
      onVisibleChange(visible) {
        if (visible) {
          this.doQuery()
        }
      },
      /** 工具方法 */
      wsCallbackFunc(summaryList) {
        let that = this
        that.summaryList = summaryList || []
      },
    },
    mounted() {
      let that = this
      that.doQuery()
      // 订阅路径Ws
      let subPath = stores.wsUserPath('/summarySchedule')
      that.wsSubscribe(subPath, that.wsCallbackFunc)
    },
    beforeDestroy(){
      let that = this
      // 销毁路径Ws
      let subPath = stores.wsUserPath('/summarySchedule')
      that.wsUnSubscribe(subPath)
    },
  }
</script>

<style lang="scss" scoped>
  /* Profile内容 */
  .header-dropdown-wrapper {
    height: 60px;
    display: flex;
    flex-direction: row;
    justify-content: center;
    align-items: center;
    // 展示部分
    .header-dropdown-link {
      height: 32px;
      width: 32px;
      cursor: pointer;
      padding: 0 !important;
      transition: all 0.18s ease;
      display: flex;
      flex-direction: row;
      justify-content: center;
      align-items: center;
      border-radius: 6px;
      &:hover {
        background: rgba(255, 255, 255, 0.15);
      }
      .inner-icon{
        font-size: 24px;
        transition: all 0.18s ease;
      }
      &:hover .inner-icon{
        color: #ffffff;
      }
      .ant-badge{
        /deep/ .ant-badge-multiple-words {
          padding: 0 6px;
        }
        /deep/ .ant-badge-count{
          height: 16px;
          line-height: 16px;
          min-width: 16px;
        }
      }
    }

  }
  .header-dropdown-menu{
    /deep/ .ant-dropdown-menu-item{
      &:hover{
        background-color: #f0f0f0;
      }
    }
    .task-group{
      /deep/ .ant-menu-item-group-title{
        padding: 8px 12px 4px;
      }
      .task-group-title{
        color: rgba(0, 0, 0, 0.45);
        font-size: 13px;
      }
      .task-group-icon{
        margin-right: 6px;
        color: #f5222d;
      }
    }
    .task-summary{
      .task-summary-inner{
        width: 290px;
      }
      .task-summary-avatar{
        display: flex;
        align-items: center;
        justify-content: center;
        width: 40px;
        height: 40px;
        border-radius: 8px;
        margin-right: 12px;
        flex-shrink: 0;
        background: rgba(3, 146, 103, 0.1);
        color: #039267;
        font-size: 20px;
      }
      .task-buckets{
        flex: 1;
        .task-bucket{
          margin-right: 12px;
          font-size: 13px;
          b{
            font-size: 22px;
            margin-right: 2px;
          }
        }
      }
      .task-summary-arrow{
        font-size: 12px;
        color: rgba(0, 0, 0, 0.45);
      }
    }
    .schedule-group{
      /deep/ .ant-menu-item-group-title{
        padding: 8px 12px 4px;
      }
      .schedule-group-title{
        color: rgba(0, 0, 0, 0.45);
        font-size: 13px;
      }
      .schedule-group-icon{
        margin-right: 6px;
        color: #00a2ae;
      }
    }
  }
</style>