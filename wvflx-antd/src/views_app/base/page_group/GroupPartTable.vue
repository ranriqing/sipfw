<template>
  <div class="table-list">
    <a-list size="large" :pagination="tablePagination">
      <div slot="header">
        <div class="flex flex-row flex-wrap cross-center" style="row-gap: 6px;">
          <a-button-group>
            <template v-if="tools.isCallCenter()">
              <a-button type="primary" icon="thunderbolt" @click="onCcQuickAdd">快捷</a-button>
            </template>
            <a-button icon="plus" @click="onRecordAdd">新增</a-button>
          </a-button-group>
          <div v-if="tools.isCallCenter()" style="margin-left: 10px">
            <span>
              <a-checkbox :checked="selectionOn" @change="_toggleSelection($event)">勾选</a-checkbox>
              <template v-if="selectionOn">
                选中<span class="text-green font-bold">{{ cptTotal }}</span> 条，
              </template>
            </span>
            <a-button-group v-if="selectionOn">
              <a-button :disabled="cptTotal===0" size="small" @click="onRecordBatch({batchNum: cptTotal})">
                <a-icon type="g-batch" style="margin-right: 4px;"></a-icon> 批量编辑
              </a-button>
            </a-button-group>
          </div>
          <a-pagination
            class="item-right"
            v-bind="tablePagination"
            @change="_paginationChange"
            @showSizeChange="_paginationShowSizeChange">
          </a-pagination>
        </div>
      </div>
      <!-- 用户列表 -->
      <a-list-item
        v-for="(record, index0) in tableDataSource"
        :class="['list-item', !record.isActive && 'gray']"
        :key="index0">
        <!-- 用户头像 -->
        <a-list-item-meta>
          <template slot="description">
            <a-icon type="user" /> {{ record.username }} <br/>
            <a-icon type="phone" /> {{ record.mobile }}
          </template>
          <div class="avatar-wrapper" slot="avatar">
            <a-checkbox
              v-if="selectionOn"
              :checked="_rowChecked(record)"
              style="margin-right: 8px;"
              @change="_onSelectRow($event, record)">
            </a-checkbox>
            <a-avatar
              size="large"
              shape="square"
              style="cursor: pointer;"
              @click="onViewAvatar(record.avatar)"
              :src="record['wxHeadImgUrl'] || tools.avatarUrl(record, 'avatar')">
            </a-avatar>
          </div>
          <template slot="title">
            <a-icon v-if="record['wxUserId']" style="color: #19be6b;" type="wechat" />
            <a :disabled="record.id <0 && !cptAdmin" @click="onRecordEdit(record)">
              {{ record.nickname }} <span v-if="!record.isActive">(禁用)</span>
            </a>
          </template>
        </a-list-item-meta>
        <!-- 其它信息 -->
        <div class="list-content">
          <div
            class="list-content-item"
            v-for="(item, index1) in tableColumns()"
            :key="index1">
            <template v-if="item.field === 'ccJobNum'">
              <span>{{ item.title }}</span>
              <p>
                <a-icon type="customer-service" /> {{ record.ccJobNum || '暂无' }}
              </p>
              <p>
                <a-icon type="gateway" />
                {{ userPartConfig.otherConfig.strategyOptions[record['gatewayStrategy']] || '同分机' }}
                ({{ record['strategyPattern'] || '同分机' }})
              </p>
            </template>
            <template v-else-if="item.field === 'gcsRole'">
              <span>{{ item.title }}</span>
              <p> <a-icon type="crown" /> {{ record['gcsRoleCaption'] }}</p>
              <p> <a-icon type="apartment" /> {{ stores.levelGcsGroup(record['gcsGroupId']) }}</p>
            </template>
            <template v-else-if="item.field === 'dateCreated'">
              <span>{{ item.title }}</span>
              <p>{{ record['dateCreated'] }}</p>
              <p>{{ record['lastUpdated'] }}</p>
            </template>
            <template v-else>
              <span>{{ item.title }}</span>
              <p>{{ record[item.field] }}</p>
            </template>
          </div>
        </div>
        <!-- Action定义 -->
        <!-- 说明： record.id <0 && !cptAdmin 内置用户记录，当前登录的不是超级管理员 -->
        <div slot="actions">
          <a @click="onResetEdit(record)" :disabled="record.id <0 && !cptAdmin">密码</a>
        </div>
        <div slot="actions">
          <a-dropdown>
            <a :disabled="record.id<0 && !cptAdmin">更多<a-icon type="down"/></a>
            <a-menu slot="overlay">
              <a-menu-item>
                <a @click="onRecordEdit(record)">编辑</a>
              </a-menu-item>
              <a-menu-item v-if="record.id>0 && cptAdmin">
                <a @click="onSwitchTo(record)">切换</a>
              </a-menu-item>
              <!-- <a-menu-item v-if="record['wxUserId']">
                <a @click="onUnbindWechat(record)">解绑</a>
              </a-menu-item>-->
              <!-- <a-menu-item v-else-if="tools.isApply95()">
                <a @click="onBindWechat(record)">绑定</a>
              </a-menu-item> -->
              <a-menu-item v-if="record.id>0">
                <a-popconfirm title="确定删除吗?" @confirm="() => doRecordDelete(record)">
                  <a>删除</a>
                </a-popconfirm>
              </a-menu-item>
            </a-menu>
          </a-dropdown>
        </div>
      </a-list-item>
    </a-list>

    <!-- 弹窗部分 -->
    <a-modal :visible="avatarVisible" :footer="null" @cancel="avatarVisible=false">
      <img alt="avatar" style="width: 100%" :src="avatarUrl" />
    </a-modal>
  </div>
</template>

<script>
  const settings = require('@/settings')
  import AAvatar from 'ant-design-vue/lib/avatar';
  import AList from 'ant-design-vue/lib/list';
  import ACheckbox from 'ant-design-vue/lib/checkbox';

  import { tools, stores } from '@/utils/util'
  import { vTableUtil } from '@/utilsv/vUtil'
  import { mapGetters } from "vuex";
  import { GETTERS, STORAGE } from '@/constants'
  import { postAction } from '@/api/api'

  import GcsUser from '@/store/models/GcsUser'
  import GcsGroup from '@/store/models/GcsGroup'

  import userPartConfig from './configs/userPartConfig'

  export default {
    name: "GroupPartTable",
    components: {
      AAvatar,
      AList,
      ACheckbox,
      AListItem: AList.Item,
      AListItemMeta: AList.Item.Meta,
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
        loading:false,
        tools: tools,
        stores: stores,
        baseUrl: settings.BASE_URL,
        userPartConfig: userPartConfig,
        /** 查询&数据源 */
        queryGroupId: null,
        queryRules: [],
        tableSorter: {},
        tablePagination: {
          onChange: (page, pageSize) => {
            pageSize || true
            this.tablePagination.current = page
            this.onTableChange(this.tablePagination, null, this.tableSorter)
          },
          onShowSizeChange: (current, size) => {
            this.tablePagination.current = current
            this.tablePagination.pageSize = size
            this.onTableChange(this.tablePagination, null, this.tableSorter)
          },
          current: 1,
          pageSize: 30,
          pageSizeOptions: ['30', '50', '100'],
          showTotal: (total, range) => {
            return range[0] + "-" + range[1] + " 共" + total + "条"
          },
          showQuickJumper: true,
          showSizeChanger: true,
          total: 0
        },
        tableDataSource: [],
        /** 勾选记录 */
        selectionOn: false,
        selectedRowKeys: [],
        /** 头像Modal */
        avatarVisible: false,
        avatarUrl: ''
      };
    },
    computed: {
      ...mapGetters({
        currentUser: GETTERS.GET_USER_CURRENT_USER,
        userAuthToken: GETTERS.GET_USER_AUTH_TOKEN,
        webRootUrl: GETTERS.GET_APP_WEBROOT_URL,
      }),
      cptAdmin: function() {
        return stores.isSuperAdmin()
      },
      cptTotal: function(){
        let that = this
        let totalCount = 0
        if(that.selectionOn){
          totalCount = that.selectedRowKeys.length
        }else{
          totalCount = that.tablePagination.total
        }
        return totalCount
      },
    },
    methods: {
      onViewAvatar(avatar){
        this.avatarUrl = tools.avatarUrl(avatar)
        this.avatarVisible = true
      },
      /** Col字段 */
      tableColumns(){
        let that = this
        let tColumns = that.configObj.tableColumns()
        let cloneItemFunc = vTableUtil.tableCloneItem
        return tColumns.map(item => cloneItemFunc(that, item, that.$createElement))
      },
      /** 查询数据 */
      doGroupQuery(groupId){
        this.queryGroupId = groupId
        this.doQuery(this.queryRules)
      },
      doQuery(queryRules){  /* 外部调用 */
        let that = this
        that.queryRules = queryRules
        let entities = GcsUser.all()
        tools.sleep(entities.length===0?500:0).then(()=>{
          that._loadData(1)
        })
      },
      onTableChange(pagination, filters, sorter) {
        this.tablePagination = pagination
        this.tableSorter = sorter
        this._loadData()
      },
      _loadData(arg) {
        let that = this
        if (arg === 1) {
          that.tablePagination.current = 1;
        }
        /* 查询条件 */
        let queryCondition = {
          pageNum: that.tablePagination.current,
          pageSize: that.tablePagination.pageSize,
          orderBy: null,
          condition: 'and',
          rules: []
        }
        // 排序参数
        if (Object.keys(that.tableSorter).length > 0) {
          let sort = that.tableSorter.field;
          let order = that.tableSorter.order;
          if(sort && order){
            order ="ascend" === that.tableSorter.order ? "asc" : "desc"
            queryCondition.orderBy = `${sort}-${order}`
          }
        }
        // 查询参数
        that.queryRules.forEach(rule => {
          if(rule['rules']){
            queryCondition.rules.push(...rule['rules'])
          }else{
            queryCondition.rules.push(rule)
          }
        })
        // 扩展组参数
        let groupIds = []
        let queryGroup = GcsGroup.find(that.queryGroupId)
        if(queryGroup){
          let allGroups = GcsGroup.query().orderBy('id').get()
          let subGroups = that._recurseSubGroups(queryGroup, allGroups, true)
          groupIds = subGroups.map(item => item.id)
        }
        // 加载数据
        that.loading = true
        let query = GcsUser.query().where(item => {
          // 扩展组查询
          if(groupIds.length > 0){
            let groupId = groupIds.find( groupId => item.gcsGroupId === groupId)
            if(!groupId){
              return false
            }
          }
          // 条件查询
          let isMatched = true
          if(queryCondition.rules.length>0){
            isMatched = false
            queryCondition.rules.forEach((rule)=>{
              let field = rule.field
              let value = rule.value
              if(item[field] && item[field].match(new RegExp(value, 'i'))) {
                isMatched = true
              }
            })
          }
          return isMatched
        })
        let totalCount = query.count()
        let sort = that.tableSorter.field;
        let order = "ascend" === that.tableSorter.order ? "asc" : "desc"
        let offset = (queryCondition.pageNum - 1) * queryCondition.pageSize
        let entityList = query.orderBy(sort?sort:'id', order?order:'desc')
          .offset(offset).limit(queryCondition.pageSize).get()
        // 包装结果
        that.loading = false
        that.tablePagination.total = totalCount
        that.tableDataSource = entityList
      },
      /** 执行操作 */
      onCcQuickAdd(){
        this.$emit('ccQuickAdd');
      },
      doCcQuickSave(record){
        let that = this
        // 开坐席账号（gcs_user + cc_sip_user）属于呼叫中心，实现在 /ccSipUser/batchSave；
        // 响应 gmodel 仍是用户实体数组，直接进 Vuex-ORM
        let promise = postAction('/ccSipUser/batchSave', record)
        promise.then(resp => {
          let batchEntitys = resp.gmodel
          GcsUser.insertOrUpdate({
            data: batchEntitys
          })
          that._loadData()
          that.$message.success(`快捷添加成功`);
        }).catch(err => {
          vTableUtil.tableReqErr(that, err, '快捷添加失败')
        })
        return promise
      },
      onRecordAdd(){
        this.$emit('recordAdd');
      },
      doRecordSave(record){
        let that = this
        let promise = GcsUser.$create({
          data: record
        })
        promise.then(resp => {
          that.tableDataSource.unshift(resp)
          that.$message.success(`保存成功`);
        }).catch(err => {
          err || true
          // vTableUtil.tableReqErr(that, err, '保存失败')
        })
        return promise
      },
      onRecordEdit(record){
        this.$emit('recordEdit', record)
      },
      doRecordUpdate(record){
        let that = this
        let promise = GcsUser.$update({
          params: {
            id: record.id,
          },
          data: record
        })
        promise.then(resp => {
          let recordIndex = that.tableDataSource.findIndex( item => item.id === resp.id)
          if(recordIndex !== -1){
            that.tableDataSource.splice(recordIndex, 1, resp)
          }
          that.$message.success(`更新成功`);
        }).catch(err => {
          err || true
          // vTableUtil.tableReqErr(that, err, '更新失败')
        })
        return promise
      },
      onRecordBatch(context){
        this.$emit('recordBatch', context);
      },
      doRecordBatch(record){
        let that = this
        let updateFields = Object.keys(record)
        record = that._.cloneDeep(record)
        if(that.selectionOn){
          record['opKeys'] = that.selectedRowKeys  // (选中记录)
        }else{
          record['queryCondition'] = that._queryCondition()
        }
        if(record['batchDelete']){
          record['batchDelete'] = true
        }else{
          record['fieldNames'] = updateFields
        }
        // 批量编辑
        let promise = postAction('/gcsUser/batch', record)
        promise.then(resp => {
          let rstModel = resp.gmodel
          let batchAmount = rstModel['batchAmount']
          that.$message.success(`成功操作${batchAmount}条数据`);
          // 加载数据(列表变化)
          that.selectionOn = false
          that.selectedRowKeys = []
          if(record['batchDelete']){
            GcsUser.deleteAll()
          }
          GcsUser.$fetch().then(resp1 =>{
            resp1 || true
            that._loadData()
          })
        }).catch(err => {
          vTableUtil.tableReqErr(that, err, '批量操作失败')
        })
        return promise
      },
      doRecordDelete(record) {
        let that = this
        let promise = GcsUser.$delete({
          params: {
            id: record.id
          }
        })
        promise.then(() => {
          let recordIndex = that.tableDataSource.findIndex( item => item.id === record.id)
          if(recordIndex !== -1){
            that.tableDataSource.splice(recordIndex, 1)
          }
          that.$message.success(`删除${record.id}成功`);
        }).catch(err => {
          err || true
          // vTableUtil.tableReqErr(that, err, `删除${record.id}失败`)
        })
      },
      onResetEdit(record){
        this.$emit('resetEdit', record)
      },
      onBindWechat(record){
        let that = this
        // 加载二维码
        let promise = postAction('/gcsUser/genBindCode', {userId: record['id']}).then(resp => {
          let gmodel = resp.gmodel || {}
          let bindUrl = `${that.webRootUrl.replace(/(wv\w+)/, "mvapp")}/pages/virtual/?vPath=wechatBind&bindCode=${gmodel['bindCode']}`
          console.log(bindUrl)
          let wechatQrcodeUrl = `${that.baseUrl}/qrcode/generate?content=${encodeURIComponent(bindUrl)}`
          that.$info({
            title: `绑定微信`,
            content: function(h){
              h && true
              const props = {}
              let attrs = {
                style:'width: 300px; height: 300px;',
                src: wechatQrcodeUrl,
              }
              return (
                <div>
                  <img {... { props, attrs } } />
                  <div {... { props, attrs: {style: 'display: flex; flex-direction: column; align-items: center;'} } }>
                    <span {... { props, attrs: {style: 'font-weight: bold;'} } }>微信扫码，绑定用户 {record['nickname']}</span>
                    <span>10分钟内有效</span>
                  </div>
                </div>
              )
            },
            okText: '确认',
            onOk() {
              // 刷新用户信息
              GcsUser.$update({
                params: {
                  id: record.id,
                },
                data: record
              }).then(resp => {
                let recordIndex = that.tableDataSource.findIndex( item => item.id === resp.id)
                if(recordIndex !== -1){
                  that.tableDataSource.splice(recordIndex, 1, resp)
                }
              })
            },
          });
        })
        return promise
      },
      onUnbindWechat(record){
        let that = this
        that.$confirm({
          title: '确认弹窗',
          content: `确定将用户 ${record['nickname']} 解绑微信？`,
          okText: '解绑',
          onOk() {
            // 刷新用户信息
            GcsUser.$update({
              params: {
                id: record.id,
              },
              data: {
                id: record.id,
                wxOpenId: null,
              }
            }).then(resp => {
              let recordIndex = that.tableDataSource.findIndex( item => item.id === resp.id)
              if(recordIndex !== -1){
                that.tableDataSource.splice(recordIndex, 1, resp)
              }
            })
          }
        });
      },
      onSwitchTo(record){
        let that = this
        let reqParams = {webPath: that.userAuthToken['origin'], toUsername: record['username']}
        let promise = postAction('/auth/switchToken', reqParams)
        promise.then(resp => {
          that.$avue.lsSet(STORAGE.AUTH_TOKEN, resp.gmodel, 30)
          window.location.href = settings.WEB_PATH
        }).catch(err => {
          that.$message.warn(`切换失败：${err['message']}`)
        })
        return promise
      },
      /** 工具方法 */
      _recurseSubGroups(group, allGroups, includeSelf){
        let that = this
        let subGroups = allGroups.filter(item => item.parentId === group.id)
        subGroups.forEach(subGroup => {
          let children = that._recurseSubGroups(subGroup, allGroups, false)
          subGroups.push(...children)
        })
        includeSelf && subGroups.unshift(group)
        return subGroups
      },
      _queryCondition(){
        let that = this
        let queryCondition = {
          orderBy: null,
          condition: 'and',
          rules: [],
        }
        that.queryRules.forEach(rule => {
          if(rule['rules']){
            queryCondition.rules.push(...rule['rules'])
          }else{
            queryCondition.rules.push(rule)
          }
        })
        return queryCondition
      },
      _paginationChange: function(page, pageSize){
        let that = this
        that.tablePagination.current =  page
        that.tablePagination.pageSize =  pageSize
        that.onTableChange(that.tablePagination, null, that.tableSorter)
      },
      _paginationShowSizeChange: function(current, size){
        let that = this
        that.tablePagination.current =  current
        that.tablePagination.pageSize =  size
        that.onTableChange(that.tablePagination, null, that.tableSorter)
      },
      _toggleSelection(e){
        let checked = e.target.checked
        this.selectionOn = checked
        if(!checked){
          this.selectedRowKeys = []
        }
      },
      _onSelectRow(e, record){
        let that = this
        let checked = e.target.checked
        let checkedIndex = that.selectedRowKeys.findIndex(id => id === record['id'])
        if(!checked && checkedIndex !== -1){
          that.selectedRowKeys.splice(checkedIndex, 1)
        }else if(checked && checkedIndex === -1){
          that.selectedRowKeys.push(record['id'])
        }
      },
      _rowChecked(record){
        let that = this
        let checkedIndex = that.selectedRowKeys.findIndex(id => id === record['id'])
        return checkedIndex !== -1
      }
    },
  }
</script>

<style lang="scss" scoped>
  .table-list {
    .avatar-wrapper{
      display: flex;
      flex-direction: row;
      align-items: center;
    }
    .ant-avatar-lg {
      width: 48px;
      height: 48px;
      line-height: 48px;
    }
    .list-item{
      &.gray{
        filter: grayscale(100%);
      }
    }
    /deep/ .ant-list-item-meta {
      flex: 2;
    }
    .list-content{
      flex: 3;
      .list-content-item {
        display: inline-block;
        vertical-align: middle;
        font-size: 14px;
        min-width: 100px;
        margin-left: 45px;
        span {
          line-height: 20px;
        }
        p {
          color: rgba(0, 0, 0, .45);
          margin-top: 4px;
          margin-bottom: 0;
          line-height: 22px;
        }
      }
    }
  }
</style>
