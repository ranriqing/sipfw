<template>
  <a-modal
    :bodyStyle="cptBodyStyle"
    :visible="visible"
    :width="cptPopWidth"
    :okText="cptSubmitText"
    @ok="onSubmit"
    @cancel="doClose">
    <div slot="title" class="flex flex-row main-between">
      <div>
        <a-popover v-if="stores.isLabState()" placement="bottomLeft" overlayClassName="lab-popover">
          <a-icon type="bug" class="bug-icon" />
          <template slot="content">
            <div class="lab-item"><div class="title">Comp</div><div class="content">GSelectUserPop</div></div>
            <div class="lab-item"><div class="title">Title</div><div class="content">选择用户</div></div>
          </template>
        </a-popover>
        选择用户({{ _.get(context, 'single')?'单个':'允许多选' }})
      </div>
      <div style="margin-right: 24px;">
        <!-- blank -->
      </div>
    </div>
    <a-form :form="form" class="pop-form" @submit.prevent="onSubmit">
      <div class="g-layout-main">
        <div class="g-layout-left" style="height: 450px; overflow-y: scroll;">
          <NestedTree
            style="padding-right: 10px;"
            size="small"
            :maxDepth="3"
            :levelDrag="false"
            :dragEnabled="false"
            :editEnabled="false"
            :itemStyle="{cursor: 'default', padding: '4px'}"
            :itemList="cptGroupTree"
            @toggleActive="onGroupSelect">
          </NestedTree>
        </div>
        <div class="g-layout-right">
          <div style="margin-bottom: 6px;">
            <a-input
              :value="keyword"
              @change="onChange"
              style="width: 100%;"
              placeholder="用户名、用户昵称...">
              <div slot="addonBefore" v-if="_.get(activeGroup, 'id')">
                {{ activeGroup.title }}
                <a-icon type="close" @click="onGroupSelect({})"/>
              </div>
              <a-icon slot="prefix" type="search" />
            </a-input>
          </div>
          <div class="table-wrapper">
            <a-table
              row-key="id"
              size="small"
              style="height: 360px;"
              :loading="loading"
              :pagination="false"
              :scroll="{ x: true, y: 360 }"
              :columns="tableColumns()"
              :dataSource="tableDataSource"
              :rowSelection="rowSelection">
              <!-- Render扩展 -->
              <template #avatarUserRender="text, record, index">
                <div @click="customRowSelect(record)">
                  <a-avatar
                    slot="avatar"
                    size="large"
                    shape="circle"
                    style="cursor: pointer; border: 1px solid #e6e9f0; filter: grayscale(40%)"
                    :src="tools.avatarUrl(record, 'avatar')"/>
                  <span :title="record.username">&nbsp;{{ record.nickname | Ellipsis(6) }}</span>
                </div>
              </template>
              <template #gcsRoleRender="text, record, index">
                <div>{{ record['gcsRoleCaption'] }}</div>
              </template>
              <template #gcsGroupRender="text, record, index">
                <div>{{ stores.levelGcsGroup(record['gcsGroupId']) }}</div>
              </template>
            </a-table>
          </div>
        </div>
      </div>
    </a-form>
    <div slot="footer" class="flex flex-row flex-wrap main-between cross-center">
      <div>
        <span class="text-tips" style="margin-right: 8px;">{{ cptSelectedUsers.length>0?'已选中':'暂无选中' }}</span>
        <div v-for="userItem in cptSelectedUsers.slice(0, maxLength)" :key="userItem.id" style="display: inline-block; margin-right: 8px;">
          <a-avatar
            size="large"
            shape="square"
            style="cursor: pointer; border: 1px solid #e6e9f0; filter: grayscale(40%)"
            :src="tools.avatarUrl(userItem, 'avatar')"/>
          <span :title="userItem.username">&nbsp;{{ userItem.nickname | Ellipsis(6) }}</span>
        </div>
        <span v-if="cptSelectedUsers.length>maxLength" class="text-tips">等共{{ cptSelectedUsers.length }}个用户</span>
      </div>
      <div class="item-right">
        <a-button :style="cptSubmitBtn?{'margin-left': '8px'}:{}" @click="doClose">取消</a-button>
        <a-button v-if="cptSubmitBtn" type="primary" :disabled="false" :loading="submitLoading" @click="onSubmit">
          确定
        </a-button>
      </div>
    </div>
  </a-modal>
</template>

<script>
  import ARadio from 'ant-design-vue/lib/radio';
  import AAvatar from 'ant-design-vue/lib/avatar';

  import { tools, stores } from '@/utils/util'
  import { vTableUtil } from '@/utilsv/vUtil'

  import GcsUser from '@/store/models/GcsUser'
  import popConfig from './configs/appSelectUserPopConfig'
  import _ from 'lodash'
  import NestedTree from '@/components/comp_tools/NestedTree.vue'
  import GcsGroup from '@/store/models/GcsGroup'

  export default {
    name: "GSelectUserPop",
    components: {
      NestedTree,
      AAvatar,
      ARadio,
      ARadioGroup: ARadio.Group,
    },
    props: {
      close: {
        type: Function,
        required: false,
        default: undefined,
      },
    },
    data () {
      let that = this
      return {
        configObj: popConfig.selectUserConfig,
        /** 配置&状态 */
        tools: tools,
        stores: stores,
        form: this.$form.createForm(this),
        visible: false,
        loading:false,
        submitLoading:false,
        activeGroup: {},
        maxLength: 3,
        record: {},
        context: {},
        /** 【扩展】查询&数据源 */
        keyword: undefined,
        queryRules: [],
        tableSorter: {},
        tableDataSource: [],
        rowSelection: {
          selectedRowKeys: [],
          onChange: (selectedRowKeys) => {
            that.rowSelection.selectedRowKeys = selectedRowKeys
          },
        }
      }
    },
    computed: {
      /** Cpt选项 */
      cptBodyStyle: function(){
        return this.configObj.bodyStyle || {}
      },
      cptSubmitBtn: function(){
        return this._.defaultTo(this.configObj.submitBtn, true)
      },
      cptSubmitText: function(){
        return this._.defaultTo(this.configObj.submitText, '确认')
      },
      cptPopWidth: function(){
        return this._.defaultTo(this.configObj.popWidth, 700)
      },
      cptGroupTree: function() {
        let allGroups = GcsGroup.query().orderBy('sequence', 'asc').orderBy('id', 'asc').get()
        let rootGroup = allGroups.find(item => item.id === -1)
        let rootTree = stores._buildGroupTree(rootGroup, allGroups)
        return rootTree?.children || []
      },
      cptSelectedUsers: function(){
        let that = this
        let selectedKeys = that.rowSelection.selectedRowKeys
        let selectedUsers = []
        selectedKeys.forEach(userId => {
          let gcsUser = GcsUser.find(userId)
          if(gcsUser){
            selectedUsers.push(gcsUser)
          }
        })
        return selectedUsers
      }
    },
    methods: {
      customRowSelect(record){
        let that = this
        let rowKeys = that.rowSelection.selectedRowKeys
        if(rowKeys && !that._.get(that.context, 'single')){
          let recordIndex = rowKeys.findIndex( id => id === record.id);
          if(recordIndex === -1){
            rowKeys.push(record.id)
          }else{
            rowKeys.splice(recordIndex, 1)
          }
        }else{
          that.rowSelection.selectedRowKeys = [record.id]
        }
      },
      /** 弹窗操作 */
      showPop (record, context, submitCallback, cancelCallback) {
        let that = this
        that.form.resetFields()
        that.record = that._.pickBy(record || {}, (value, key) =>
          !that._.isNil(value) && !key.startsWith('$') && !that._.isFunction(value)
        )
        that.context = context || {}
        that.submitCallback = submitCallback
        that.cancelCallback = cancelCallback
        that.rowSelection.type = that._.get(context, 'single')?'radio':'checkbox'
        that.visible = true
        that.$nextTick(() => {
          let values = {}
          that.form.setFieldsValue(values)
        })
        /** 【扩展】加载用户 */
        // that.tableDataSource = []
        that.rowSelection.selectedRowKeys = []
        that.doQuery()
      },
      onSubmit () {
        let that = this
        if(that.cptSelectedUsers.length === 0){
          that.$message.warn('请先选择用户!')
          return
        }
        that.form.validateFields((errors, values) => {
          if (!errors) {
            values || true
            // 【扩展】返回结果
            let selectedUsers = that.cptSelectedUsers.map(user => {
              return {
                id: user.id,
                nickname: user.nickname,
                username: user.username,
                avatar: user.avatar
              }
            })
            if(that.submitCallback){
              let promise = that.submitCallback(selectedUsers, that.context)
              if(promise && promise.then){
                that.submitLoading = true
                promise.then(() => {
                  that.submitLoading = false
                  that.doClose()
                }).catch(() => {
                  that.submitLoading = false
                })
              }
            }
          }
        });
      },
      doClose () {
        let that = this
        that.record = {}
        that.context = {}
        if(that.cancelCallback){
          that.cancelCallback()
        }
        that.visible = false
        // 回调$destroy
        if(that.close){
          that.close()
        }
      },
      /** 扩展操作 */
      onGroupSelect(activeGroup){
        let that = this
        that.activeGroup = activeGroup
        that.doQuery()
      },
      /** 【扩展】Col字段 */
      tableColumns(){
        let that = this
        let tColumns = that.configObj.tableConfig.tableColumns()
        let cloneItemFunc = vTableUtil.tableCloneItem
        return tColumns.map(item => cloneItemFunc(that, item, that.$createElement))
      },
      /** 【扩展】查询数据 */
      onChange: _.debounce(function(e){
        let that = this
        that.keyword = (e.target.value || '').trim().toLowerCase()
        that.doQuery()
      }, 500),
      doQuery(){
        let that = this
        if(that.keyword){
          that.queryRules = [
            {field:"username", value:that.keyword, operator:"contains"},
            {field:"nickname", value:that.keyword, operator:"contains"}
          ]
        }else{
          that.queryRules = []
        }
        let entities = GcsUser.all()
        tools.sleep(entities.length===0?500:0).then(()=>{
          that._loadData(1)
        })
      },
      _loadData() {
        let that = this
        /* 查询条件 */
        let queryCondition = {
          paginate: false,
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
        let queryGroup = GcsGroup.find(that._.get(that.activeGroup, 'id'))
        if(queryGroup){
          let allGroups = GcsGroup.query().orderBy('id').get()
          let subGroups = stores._recurseSubGroups(queryGroup, allGroups, true)
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
              if(item[field] && that._.toString(item[field]).toLowerCase().match(value)){
                isMatched = true
              }
            })
          }
          return isMatched
        })
        // let totalCount = query.count()
        let sort = that.tableSorter.field;
        let order = "ascend" === that.tableSorter.order ? "asc" : "desc"
        let entityList = query.orderBy(sort?sort:'id', order?order:'desc').get()
        // 包装结果
        that.loading = false
        that.tableDataSource = entityList
      },
    },
  }
</script>

<style lang="scss" scoped>
  $tmpl-color-zero6: rgba(241, 139, 98, 0.6);

  .bug-icon {
    color: $tmpl-color-zero6;
    margin-right: 6px;
  }
  .table-wrapper{
    height: 360px;
  }
  /* 布局样式 */
  .g-layout-main {
    width: 100%;
    display: flex;
    height: 100%;
    overflow: auto;
    .g-layout-left {
      border-right: 1px solid #e8e8e8;
      width: 200px;
    }
    .g-layout-right {
      flex: 1 1;
      padding: 8px 15px;
      /deep/ .ant-radio{
        transform: scale(1.3);
      }
      /deep/ .ant-checkbox{
        transform: scale(1.3);
      }
    }
  }
</style>

