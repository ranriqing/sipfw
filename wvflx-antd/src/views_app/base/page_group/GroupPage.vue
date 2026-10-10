<template>
  <div>
    <LabPageSelect>组织结构</LabPageSelect>
    <a-card v-show="!pageVisible" :bordered="false" class="page">
      <div class="g-layout-main">
        <div class="g-layout-left">
          <!-- 公司管理 -->
          <a @click="onDeptSelect({})" >
            <a-icon :type="_.defaultTo(_.get(cptRootGroup, 'icon'), 'home')" />
            {{ _.defaultTo(_.get(cptRootGroup, 'name'), '公司总部') }}
          </a>
          <template v-if="cptRootGroup && cptAdmin" >
            <a-divider type="vertical" />
            <a @click="onDeptEdit(cptRootGroup, null)" >
              <a-icon type="edit" />
            </a>
          </template>
          <!-- 部门管理 -->
          <template v-if="cptAdmin" >
            <a style="float: right; margin-right: 15px;" @click="onDeptAdd(null)">
              <a-icon type="plus" /> 顶级部门
            </a>
          </template>
          <a-divider style="width: 95%; min-width: 95%;"/>
          <NestedTree
            style="padding-right: 10px;"
            :maxDepth="3"
            :levelDrag="true"
            :dragEnabled="cptAdmin"
            :itemStyle="{cursor: 'default'}"
            :itemList="cptGroupTree"
            @addItem="onDeptAdd"
            @editItem="onDeptEdit"
            @deleteItem="onDeptDelete"
            @dragChange="onDeptChanged"
            @toggleActive="onDeptSelect">
          </NestedTree>
        </div>
        <!-- 用户列表 -->
        <div class="g-layout-right">
          <!-- 查询部分 -->
          <div class="page-query-wrapper">
            <GPartQuery
              ref="slotQuery"
              :configObj="userPartConfig.queryConfig"
              @doSubmit="doQuery"
              @queryReset="onQueryReset">
              <template #slot_keyword="{item}">
                <a-input
                  :value="keyword"
                  @change="onChange"
                  style="width: 100%;"
                  placeholder="用户名、用户昵称...">
                  <div slot="addonBefore" v-if="_.get(activeGroup, 'id')">
                    {{ activeGroup.title || '暂无' }}
                    <a-icon type="close" @click="onDeptSelect({})"/>
                  </div>
                  <a-icon slot="prefix" type="search" />
                </a-input>
              </template>
            </GPartQuery>
          </div>
          <!-- 表格部分 -->
          <div class="page-table-wrapper">
            <GPartTable
              ref="slotTable"
              :configObj="userPartConfig.tableConfig"
              @ccQuickAdd="onCcQuickAdd"
              @recordAdd="onUserAdd"
              @recordEdit="onUserEdit"
              @resetEdit="onResetEdit"
              @recordBatch="onUserBatch">
            </GPartTable>
          </div>
        </div>
      </div>
    </a-card>

    <!-- 部门弹窗 -->
    <GFormPop
      ref="slotDeptAdd"
      :popVisible="popDeptAddVisible"
      :configObj="deptPopConfig.addConfig">
    </GFormPop>
    <GFormPop
      ref="slotDeptEdit"
      :popVisible="popDeptEditVisible"
      :configObj="deptPopConfig.editConfig">
    </GFormPop>
    
    <!-- 用户弹窗 -->
    <GFormPop
      ref="slotCcQuickAdd"
      :popVisible="popUserCcQuickAddVisible"
      :configObj="userPopConfig.ccQuickAddConfig">
    </GFormPop>
    <GFormPop
      ref="slotUserAdd"
      :popVisible="popUserAddVisible"
      :configObj="userPopConfig.addConfig">
    </GFormPop>
    <GFormPop
      ref="slotUserEdit"
      :popVisible="popUserEditVisible"
      :configObj="userPopConfig.editConfig">
    </GFormPop>
    <GFormPop
      ref="slotUserReset"
      :popVisible="popUserResetVisible"
      :configObj="userPopConfig.resetConfig">
    </GFormPop>
    <GBatchPop
      ref="slotUserBatch"
      :popVisible="popUserBatchVisible"
      :configObj="userPopConfig.batchConfig">
    </GBatchPop>
  </div>
</template>

<script>
  import { stores } from '@/utils/util'
  import { mapGetters } from "vuex";
  import { GETTERS } from '@/constants'
  import { putAction } from '@/api/api'

  import GcsGroup from '@/store/models/GcsGroup'

  import LabPageSelect from '@/pages/LabPageSelect'
  import NestedTree from '@/components/comp_tools/NestedTree'
  import GPartQuery from '@/components/page_part/GPartQuery'
  import GPartTable from './GroupPartTable'
  import GFormPop from '@/components/page_pop/GFormPop'
  import GBatchPop from '@/components/page_pop/GBatchPop'

  import userPartConfig from './configs/userPartConfig'
  import userPopConfig from './configs/userPopConfig'
  import deptPopConfig from './configs/deptPopConfig'
  import _ from "lodash";

  export default {
    name: "GroupPage",
    components: {
      LabPageSelect,
      GPartQuery,
      GPartTable,
      GFormPop,
      GBatchPop,
      NestedTree,
    },
    data(){
      return {
        // 用户配置
        stores: stores,
        userPartConfig: userPartConfig,
        userPopConfig: userPopConfig,
        deptPopConfig: deptPopConfig,
        popDeptAddVisible: false,
        popDeptEditVisible: false,
        popUserAddVisible: false,
        popUserCcQuickAddVisible: false,
        popUserEditVisible: false,
        popUserResetVisible: false,
        popUserBatchVisible: false,
        groupQueryRules: [], // 查询规则
        keyword: undefined,
        activeGroup: {}
      }
    },
    computed: {
      ...mapGetters({
        currentUser: GETTERS.GET_USER_CURRENT_USER,
      }),
      pageVisible: function() {
        return this.popDeptAddVisible || this.popDeptEditVisible
          || this.popUserAddVisible || this.popUserCcQuickAddVisible
          || this.popUserResetVisible || this.popUserEditVisible || this.popUserBatchVisible
      },
      cptAdmin: function(){
        return stores.isSuperAdmin()
      },
      cptRootGroup: function(){
        return GcsGroup.find(-1)
      },
      cptGroupTree: function() {
        let allGroups = GcsGroup.query().orderBy('sequence', 'asc').orderBy('id', 'asc').get()
        let rootGroup = allGroups.find(item => item.id === -1)
        let rootTree = stores._buildGroupTree(rootGroup, allGroups)
        return rootTree?.children || []
      }
    },
    methods:{
      /** 部门操作 */
      onDeptSelect(item){
        let that = this
        that.activeGroup = item
        that.$refs.slotTable.doGroupQuery(item?.id)
      },
      onDeptAdd(pItem){
        let that = this
        let pGroup = GcsGroup.find(pItem?.id)
        let context = {id: pGroup?.id, title: pGroup?.name}
        let slotRef = that.$refs.slotDeptAdd
        that.popDeptAddVisible = slotRef.isPage()
        slotRef.showPop({}, context,
          (record, context) => { /* submit */
            let promise = GcsGroup.$create({
              data: {
                parentId: that._.defaultTo(that._.get(context, 'id'), -1),
                name: record.name,
                icon: record.icon,
                sequence: record.sequence,
                remark: record.remark,
              }
            })
            promise.then(() => that.popDeptAddVisible = false)
            return promise
          }, () => { /* cancel */
            that.popDeptAddVisible = false
          }
        )
      },
      onDeptEdit(item, pItem){
        let that = this
        let dept = GcsGroup.find(item.id)
        let slotRef = that.$refs.slotDeptEdit
        that.popDeptEditVisible = slotRef.isPage()
        slotRef.showPop(dept, pItem,
          (record, context) => { /* submit */
            context || true
            let that = this
            if(record.id === -1){
              record.parentId = null
            }
            let promise = GcsGroup.$update({
              params: {
                id: record.id
              },
              data: record
            })
            promise.then(() => that.popDeptEditVisible = false)
            return promise
          }, () => { /* cancel */
            that.popDeptEditVisible = false
          }
        )
      },
      onDeptDelete(item){
        GcsGroup.$delete({
          params: {
            id: item.id
          }
        })
      },
      onDeptChanged(pItem, cItem, newIndex){
        // 子部门列表
        let pId = this._.defaultTo(this._.get(pItem, 'id'), -1)
        let childGroups = GcsGroup.query().where('parentId', pId).orderBy('sequence').orderBy('id').get()
        let mappedGroups = childGroups.map(item => {
          return {
            id: item.id,
            pId: item.pId,
            title: item.title,
            sequence: item.sequence
          }
        })
        // 移动子元素
        let oldIndex = mappedGroups.findIndex( item => item.id === cItem.id)
        if(oldIndex !== -1){
          mappedGroups.splice(oldIndex, 1)
        }
        mappedGroups.splice(newIndex, 0, {
          id: cItem.id,
          pId: cItem.pId,
          title: cItem.title,
          sequence: cItem.sequence
        })
        // 更新排序
        mappedGroups.forEach((item, index) => {
          let itemData = {sequence: index+1}
          if(item.pId !== pId){
            itemData['parentId'] = pId
          }
          GcsGroup.$update({
            params: {
              id: item.id
            },
            data: itemData
          })
        })
      },
      /** User操作 */
      onChange: _.debounce(function(e){
        let that = this
        that.keyword = (e.target.value || '').trim().toLowerCase()
        // that.doQuery()
      }, 500),
      onQueryReset(){
        let that = this
        that.keyword = undefined
        that.doQuery()
      },
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
        this.$refs.slotTable.doQuery(that.queryRules)
      },
      onCcQuickAdd(){
        let that = this
        let slotRef = that.$refs.slotCcQuickAdd
        that.popUserCcQuickAddVisible = slotRef.isPage()
        slotRef.showPop({}, {},
          (record, context) => { /* submit */
            let slotTable = that.$refs.slotTable
            let promise = slotTable.doCcQuickSave(record, context)
            promise.then(() => that.popUserCcQuickAddVisible = false)
            return promise
          }, () => { /* cancel */
            that.popUserCcQuickAddVisible = false
          }
        )
      },
      onUserAdd(){
        let that = this
        let slotRef = that.$refs.slotUserAdd
        that.popUserAddVisible = slotRef.isPage()
        slotRef.showPop({}, {},
          (record, context) => { /* submit */
            let slotTable = that.$refs.slotTable
            let promise = slotTable.doRecordSave(record, context)
            promise.then(() => that.popUserAddVisible = false)
            return promise
          }, () => { /* cancel */
            that.popUserAddVisible = false
          }
        )
      },
      onUserEdit(record) {
        let that = this
        let slotRef = that.$refs.slotUserEdit
        that.popUserEditVisible = slotRef.isPage()
        /* 95申请 */
        if(record.remark && record.remark.indexOf('临时呼出', 1)!==-1){
          record['callPermission'] = true
        }else{
          record['callPermission'] = false
        }
        slotRef.showPop(record, {title: record.username},
          (record, context) => { /* submit */
            let slotTable = that.$refs.slotTable
            let promise = slotTable.doRecordUpdate(record, context)
            promise.then(() => that.popUserEditVisible = false)
            return promise
          }, () => { /* cancel */
            that.popUserEditVisible = false
          }
        )
      },
      onResetEdit(record) {
        let that = this
        let slotRef = that.$refs.slotUserReset
        that.popUserResetVisible = slotRef.isPage()
        slotRef.showPop(record, {title: record.username},
          (record, context) => { /* submit */
            context || true
            // 密码与备注统一走 updatePassword 接口：后端 hash 密码并写入 remark
            let promise = putAction('/gcsUser/updatePassword', null, {
              userId: record.id,
              newPassword: record.newPassword,
              remark: record.remark,
            })
            promise.then(() => that.popUserResetVisible = false)
            return promise
          }, () => { /* cancel */
            that.popUserResetVisible = false
          }
        )
      },
      onUserBatch(context){
        let that = this
        let slotRef = that.$refs.slotUserBatch
        that.popUserBatchVisible = slotRef.isPage()
        slotRef.showPop({}, context,
          (record, context) => { /* submit */
            let slotTable = that.$refs.slotTable
            let promise = slotTable.doRecordBatch(record, context)
            promise.then(() => that.popUserBatchVisible = false)
            return promise
          }, () => { /* cancel */
            that.popUserBatchVisible = false
          }
        )
      },
    },
    mounted() {
      let slotQuery = this.$refs.slotQuery
      slotQuery?slotQuery.onSubmit():this.doQuery([])
    },
  }
</script>


<style lang="scss" scoped>
  /* 布局样式 */
  .g-layout-main {
    width: 100%;
    display: flex;
    height: 100%;
    overflow: auto;
    .g-layout-left {
      border-right: 1px solid #e8e8e8;
      width: 300px;
    }
    .g-layout-right {
      flex: 1 1;
      padding: 8px 15px;
    }
  }

  /* Tree样式 */
  .tree-wrapper {
    /deep/ .ant-menu-item-group-title {
      position: relative;
      &:hover {
        .btn {
          display: block;
        }
      }
    }
    /deep/ .ant-menu-item, /deep/ .ant-menu-submenu-title {
      &:hover {
        .btn {
          display: block;
        }
      }
    }
    /deep/ .btn {
      display: none;
      position: absolute;
      top: 0;
      right: 10px;
      height: 40px;
      line-height: 40px;
      z-index: 1050;
      &:hover {
        /* transform: scale(1.2); */
        transition: 0.5s all;
      }
    }
    /deep/ .ant-menu-submenu-arrow{
      display: none;
    }
    /deep/ .ant-menu-submenu-title .btn{
      right: 40px;
    }
  }
</style>
