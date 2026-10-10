<template>
  <div class="table-list">
    <!-- 新增评论 -->
    <a-form :form="form" @submit.prevent="onSubmit">
      <a-form-item style="margin-bottom:0;">
        <a-textarea
          placeholder="请输入评论内容"
          :rows="inputRows"
          v-decorator="['content', {
            rules: [{ required: true, message: '请输入评论内容' }]
          }]"
          @focusin="inputRows=3"
          @focusout="inputRows=3">
        </a-textarea>
      </a-form-item>
      <a-form-item>
        <a-button type="primary" size="small" :loading="submitLoading" htmlType="submit">提交</a-button>
        <a-button type="link" size="small" @click="onReset">重置</a-button>
        <span style="float: right;">
          <a-button type="link" @click="onRefresh">
            <a-icon type="reload"/> 刷新列表
          </a-button>
        </span>
      </a-form-item>
    </a-form>
    <!-- 评论列表 -->
    <a-list>
      <div v-if="tableDataSource.length===0" class="flex flex-row main-center">
        <a-spin v-if="moreLoading" style="padding: 20px 0;" />
      </div>
      <a-list-item
        v-for="(record, index0) in tableDataSource"
        :key="index0">
        <a-list-item-meta>
          <!-- 用户头像 -->
          <a-avatar slot="avatar" size="large" shape="square" :src="_avatarUrl(record.actorId)"/>
          <div slot="title">
            <span>{{ record.actorCaption }}</span>
            <span class="comment-date">{{ record.dateCreated }}</span>
          </div>
          <!-- 评论内容 -->
          <div slot="description">
            <span style="display: inline;">{{ record.content }}</span>
            <span class="ops-btn">
              <a @click="onRecordEdit(record)"><a-icon type="edit"/></a>
              <a-divider type="vertical" />
              <a-popconfirm
                v-if="stores.isSuperAdmin()"
                title="确定删除?"
                @confirm="() => doRecordDelete(record)">
                <a><a-icon type="delete" /></a>
              </a-popconfirm>
            </span>
          </div>
        </a-list-item-meta>
      </a-list-item>
      <div slot="loadMore" v-if="loadMoreVisible">
        <a-spin :spinning="moreLoading">
          <a-button block icon="down" @click="onLoadMore">
            加载更多
          </a-button>
        </a-spin>
      </div>
    </a-list>

    <!-- 弹窗部分 -->
    <GFormPop
      ref="slotEdit"
      :configObj="popConfig.editConfig">
    </GFormPop>
  </div>
</template>

<!--
# 组件说明
  为实体提供评论管理能力
# 配置组件
  configObj:{
      domainId: Long      // 实体Id
      domainName: String  // 实体Name
  }
# 操作方法
  onRefresh()
-->

<script>
  import AAvatar from 'ant-design-vue/lib/avatar';
  import AList from 'ant-design-vue/lib/list';
  import ASpin from 'ant-design-vue/lib/spin';

  const settings = require('@/settings')
  import { stores } from '@/utils/util'
  import { vTableUtil } from '@/utilsv/vUtil'
  import { postAction, patchAction, deleteAction, } from '@/api/api'
  import GFormPop from '@/components/page_pop/GFormPop'
  import popConfig from './configs/commentPopConfig'

  export default {
    name: "GPartComment",
    components: {
      AAvatar,
      AList,
      AListItem: AList.Item,
      AListItemMeta: AList.Item.Meta,
      ASpin,
      GFormPop,
    },
    props: {
      configObj: {
        type: Object,
        required: true,
      }
    },
    data () {
      return {
        /** 配置&状态 */
        stores: stores,
        baseUrl: settings.BASE_URL,
        popConfig: popConfig,
        form: this.$form.createForm(this),
        moreLoading:false,
        loadMoreVisible: false,
        submitLoading:false,
        inputRows: 3,
        /** 查询&数据源 */
        queryRules: [],
        pageNum: 1,
        pageSize: 5,
        tableDataSource: [],
      };
    },
    methods: {
      /** 加载数据 */
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
        let queryUri = `/comment/index?domainName=${that.configObj.domainName}&domainId=${that.configObj.domainId}`
        let promise = postAction(queryUri, queryCondition)
        promise.then(resp => {
          that.moreLoading = false
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
        return promise
      },
      /** 提交保存 */
      onSubmit(){
        let that = this
        that.form.validateFields((errors, values) => {
          if (!errors) {
            that.doRecordSave(values).then(() => {
              that.form.resetFields()
            })
          }
        });
      },
      onReset(){
        this.form.resetFields()
      },
      /** 执行操作 */
      doRecordSave(record){
        let that = this
        let saveUri = `/comment/save?domainName=${that.configObj.domainName}&domainId=${that.configObj.domainId}`
        that.submitLoading = true
        let promise = postAction(saveUri, record)
        promise.then(resp => {
          that.submitLoading = false
          let rstRecord = resp.gmodel
          that.tableDataSource.unshift(rstRecord)
          that.$message.success('评论成功');
        }).catch(err => {
          that.submitLoading = false
          vTableUtil.tableReqErr(that, err, '评论失败')
        })
        return promise
      },
      onRecordEdit(record) {
        let that = this
        let slotRef = that.$refs.slotEdit
        slotRef.showPop(record, {},
          (record, context) => { /* submit */
            return that.doRecordUpdate(record, context)
          }, () => { /* cancel */ }
        )
      },
      doRecordUpdate(record){
        let that = this
        let updateUri = '/comment/update'
        let promise = patchAction(updateUri, record.id, record)
        promise.then((resp) => {
          let rstRecord = resp.gmodel
          let recordIndex = that.tableDataSource.findIndex( item => item.id === rstRecord.id)
          if(recordIndex !== -1){
            that.tableDataSource.splice(recordIndex, 1, rstRecord)
          }
          that.$refs.slotEdit.doClose()
        }).catch(err => {
          vTableUtil.tableReqErr(that, err, '更新失败')
        })
        return promise
      },
      doRecordDelete(record) {
        let that = this
        let deleteUri = '/comment/delete'
        let promise = deleteAction(deleteUri, record.id, {})
        promise.then(() => {
          let recordIndex = that.tableDataSource.findIndex( item => item.id === record.id)
          if(recordIndex !== -1){
            that.tableDataSource.splice(recordIndex, 1)
          }
          that.$message.success(`删除成功`);
        }).catch(err => {
          vTableUtil.tableReqErr(that, err, '删除失败')
        })
      },
      /** 工具方法 */
      _avatarUrl(actorId){
        return `${this.baseUrl}/avatar/showAvatar?uid=${actorId}`
      },
    },
    mounted() {
      this._loadData(1)
    },
  }
</script>

<style lang="scss" scoped>
  .table-list {
    .ant-list-item-meta{
      /* 用户头像 */
      .ant-avatar-lg {
        width: 48px;
        height: 48px;
        line-height: 48px;
      }
      .comment-date {
        float: right;
        color: #c0c0c0;
      }
      /* 操作按钮 */
      /deep/ .ant-list-item-meta-description {
        word-break: break-all;
        .ops-btn{
          display: none;
          float: right;
        }
      }
      &:hover{
        .ops-btn{
          display: inline-block;
        }
      }
    }
    /* 暂无数据 */
    /deep/ .ant-list-empty-text{
      border: 1px solid #eee
    }
  }
</style>
