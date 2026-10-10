<template>
  <a-card :bordered="false" class="page-block">
    <a-table
      row-key="id"
      :loading="loading"
      :scroll="{ x: true }"
      :columns="tableColumns()"
      :dataSource="tableDataSource"
      :pagination="tablePagination"
      :rowSelection="selectionOn?rowSelection:null"
      @change="onTableChange">
      <!-- Title部分 -->
      <template slot="title">
        <div class="table-operation-buttons">
          <a-button-group>
            <template v-if="vTableUtil.tableEnableState(_self, sExtend, [OTHER.ADD_ENABLED])">
              <a-button type="primary" icon="plus" @click="onRecordAdd">{{ sExtend[OTHER.ADD_CAPTION] || '新增' }}</a-button>
            </template>
          </a-button-group>
          <div v-if="vTableUtil.tableEnableState(_self, sExtend, [OTHER.BATCH_ENABLED], false)" style="margin-left: 10px">
            <span>
              <a-checkbox :checked="selectionOn" @change="_toggleSelection($event)"></a-checkbox>
              {{ selectionOn?'勾选':'条件' }}选中<span class="text-green font-bold">{{ cptTotal }}</span> 条，
            </span>
            <a-button-group>
              <a-button :disabled="cptTotal===0" size="small" @click="onRecordBatch({batchNum: cptTotal})">
                <a-icon type="g-batch" style="margin-right: 4px;"></a-icon> 批量弹窗
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
      </template>
      <!-- 扩展Render -->
      <template v-for="(column) in slotTableColumns()" :slot="column['component']" slot-scope="text, record, index">
        <slot :name="column['component']" :text="text" :record="record" :index="index"></slot>
      </template>
      <!-- 通用Render -->
      <template #linkRender="text, record, index">
        <a @click="onRecordEdit(record)">
          <span :title="record.id">{{ text }}</span>
        </a>
      </template>
      <template #actionRender="text, record, index">
        <span>
          <a v-if="vTableUtil.tableEnableState(_self, sExtend, [OTHER.EDIT_ENABLED])" @click="onRecordEdit(record)">{{ sExtend[OTHER.EDIT_CAPTION] || '编辑' }}</a>
          <a-divider
            type="vertical"
            v-if="vTableUtil.tableEnableState(_self, sExtend, [OTHER.EDIT_ENABLED, OTHER.DELETE_ENABLED])">
          </a-divider>
          <a-dropdown v-if="vTableUtil.tableEnableState(_self, sExtend, [OTHER.DELETE_ENABLED])">
            <a class="ant-dropdown-link">
              更多 <a-icon type="down" />
            </a>
            <a-menu slot="overlay">
              <a-menu-item>
                <a-popconfirm title="确定删除吗?" @confirm="() => doRecordDelete(record)">
                  <a>删除</a>
                </a-popconfirm>
              </a-menu-item>
            </a-menu>
          </a-dropdown>
        </span>
      </template>
    </a-table>
  </a-card>
</template>

<!--
# 组件说明
  通用表格组件
# 配置组件
  configObj:{
    queryUri: 查询Uri
    saveUri: 保存Uri
    updateUri: 更新Uri
    deleteUri: 删除Uri
    tableColumns: Function(){
      return [{
        title: String,
        field: String,
        type: String,
        component: String,
        placeholder: String,
      }]
    }
  }
  sContext: Object
# 操作方法
  $emit('recordAdd')
  $emit('recordEdit', record)
  Promise doRecordSave(record)
  Promise doRecordUpdate(record)
  Promise doRecordDelete(record)
-->

<script>
  import ACheckbox from 'ant-design-vue/lib/checkbox';

  import { OTHER } from '@/constants'
  import { vTableUtil } from '@/utilsv/vUtil'
  import {deleteAction, postAction, putAction} from '@/api/api'

  export default {
    name: "GPartTable",
    components: {
      ACheckbox,
    },
    props: {
      configObj: {
        type: Object,
        required: true,
      },
      sContext: {
        type: Object,
        required: false,
        default: function(){
          return {}
        }
      }
    },
    data () {
      let that = this
      return {
        /** 配置&&状态 */
        OTHER: OTHER,
        vTableUtil: vTableUtil,
        loading:false,
        /** 查询&数据源 */
        queryRules: [],
        tableSorter: {},
        tablePagination: {
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
        rowSelection: {
          onChange: (selectedRowKeys, selectedRows) => {
            selectedRowKeys || selectedRows || true
            that.selectedRowKeys = selectedRowKeys
          },
        },
      };
    },
    computed: {
      sExtend: function() {
        return this.configObj?.extend
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
      /** Col字段 */
      tableColumns(){
        let that = this
        let tColumns = that.configObj.tableColumns()
        let cloneItemFunc = vTableUtil.tableCloneItem
        return tColumns.map(item => cloneItemFunc(that, item, that.$createElement))
      },
      slotTableColumns(){
        let that = this
        return that.tableColumns().filter(item => {
          let slotRender = that._.get(item, 'scopedSlots.customRender')
          if(slotRender && ['linkRender', 'actionRender'].indexOf(slotRender)===-1 ){
            return item
          }
        })
      },
      /** 查询数据 */
      doQuery(queryRules){
        let that = this
        that.queryRules = queryRules
        that._loadData(1)
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
        // 查询条件
        let queryCondition = {
          pageNum: that.tablePagination.current,
          pageSize: that.tablePagination.pageSize,
          condition: 'and',
          rules: [],
          orderBy: that.sExtend[OTHER.ORDER_BY] || null,
        }
        // 查询参数
        that.queryRules.forEach(rule => {
          queryCondition.rules.push(rule)
        })
        // 排序参数
        if (Object.keys(that.tableSorter).length > 0) {
          let sort = that.tableSorter.field;
          let order = that.tableSorter.order;
          if(sort && order){
            order ="ascend" === that.tableSorter.order ? "asc" : "desc"
            queryCondition.orderBy = `${sort}-${order}`
          }
        }
        // 加载数据
        that.loading = true
        let queryUri = that.configObj.queryUri
        postAction(queryUri, queryCondition).then(resp => {
          that.loading = false
          if(resp.gmodel && resp.gmodel.entityList){
            that.tableDataSource = resp.gmodel.entityList
            that.tablePagination.total = resp.gmodel.totalCount
          }
        }).catch(err => {
          that.loading = false
          vTableUtil.tableReqErr(that, err, '加载失败')
        })
      },
      /** 执行操作 */
      onRecordAdd(){
        this.$emit('recordAdd');
      },
      doRecordSave(record){
        let that = this
        let subjectNote = that.sExtend[OTHER.SUBJECT_NOTE]
        let subjectField = that.sExtend[OTHER.SUBJECT_FIELD]
        let subjectFieldValue = subjectField?`(${record[subjectField]})`:''
        let saveUri = that.configObj.saveUri
        let promise = postAction(saveUri, record)
        promise.then(resp => {
          let rstRecord = resp.gmodel
          that.tableDataSource.unshift(rstRecord)
          that.$message.success(`保存${subjectNote}${subjectField?subjectFieldValue:''}成功`);
        }).catch(err => {
          vTableUtil.tableReqErr(that, err, `保存${subjectNote}${subjectField?subjectFieldValue:''}失败`)
        })
        return promise
      },
      onRecordEdit(record){
        this.$emit('recordEdit', record)
      },
      doRecordUpdate(record){
        let that = this
        let subjectNote = that.sExtend[OTHER.SUBJECT_NOTE]
        let subjectField = that.sExtend[OTHER.SUBJECT_FIELD]
        let subjectFieldValue = subjectField?`(${record[subjectField]})`:''
        let updateUri = that.configObj.updateUri
        let promise = putAction(updateUri, record.id, record)
        promise.then(resp => {
          let rstRecord = resp.gmodel
          let recordIndex = that.tableDataSource.findIndex( item => item.id === rstRecord.id)
          if(recordIndex !== -1){
            that.tableDataSource.splice(recordIndex, 1, rstRecord)
          }
          that.$message.success(`更新${subjectNote}${subjectField?subjectFieldValue:''}成功`);
        }).catch(err => {
          vTableUtil.tableReqErr(that, err, `更新${subjectNote}${subjectField?subjectFieldValue:''}失败`)
        })
        return promise
      },
      doRecordDelete(record) {
        let that = this
        let subjectNote = that.sExtend[OTHER.SUBJECT_NOTE]
        let subjectField = that.sExtend[OTHER.SUBJECT_FIELD]
        let subjectFieldValue = subjectField?`(${record[subjectField]})`:''
        let deleteUri = that.configObj.deleteUri
        let promise = deleteAction(deleteUri, record.id, {})
        promise.then(() => {
          let recordIndex = that.tableDataSource.findIndex( item => item.id === record.id)
          if(recordIndex !== -1){
            that.tableDataSource.splice(recordIndex, 1)
          }
          that.$message.success(`删除${subjectNote}${subjectField?subjectFieldValue:''}成功`);
        }).catch(err => {
          vTableUtil.tableReqErr(that, err, `删除${subjectNote}${subjectField?subjectFieldValue:''}失败`)
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
        let batchUri = that.configObj.batchUri
        let promise = postAction(batchUri, record)
        promise.then(resp => {
          let rstModel = resp.gmodel
          let modifiedAmount = rstModel['modifiedAmount']
          let batchOpStr = (record['batchDelete'] === true)?'删除':'编辑'
          that.$message.success(`成功${batchOpStr}${modifiedAmount}条数据`);
          // 加载数据(列表变化)
          that.selectionOn = false
          that.selectedRowKeys = []
          that._loadData()
        }).catch(err => {
          vTableUtil.tableReqErr(that, err, '批量编辑失败')
        })
        return promise
      },
      /** 工具方法 */
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
      _queryCondition(){
        let that = this
        let queryCondition = {
          condition: 'and',
          rules: [],
          orderBy: null,
        }
        that.queryRules.forEach(rule => {
          queryCondition.rules.push(rule)
        })
        return queryCondition
      },
      _toggleSelection(e){
        let that = this
        let checked = e.target.checked
        that.selectionOn = checked
        if(!checked){
          that.selectedRowKeys = []
        }
      },
    },
  }
</script>