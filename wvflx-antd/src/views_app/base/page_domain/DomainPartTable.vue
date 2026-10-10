<template>
  <div>
    <a-table
      row-key="id"
      :loading="loading"
      :scroll="{ x: true }"
      :columns="tableColumns()"
      :dataSource="tableDataSource"
      :pagination="tablePagination"
      @change="onTableChange">
      <!-- Title部分 -->
      <template slot="title">
        <div class="flex flex-row">
          <a-button-group>
            <a-button type="primary" icon="plus" @click="onRecordAdd">新增</a-button>
          </a-button-group>
          <a-pagination
            class="item-right"
            v-bind="tablePagination"
            @change="_paginationChange"
            @showSizeChange="_paginationShowSizeChange">
          </a-pagination>
        </div>
      </template>
      <!-- Render定义 -->
      <template #linkRender="text, record, index">
        <a @click="onRecordEdit(record)">
          <span :title="record.id">{{ text }}</span>
          <a-tag v-if="record.codeEntity" color="orange" style="margin-left: 6px;"> Code </a-tag>
        </a>
      </template>
      <template #actionRender="text, record, index">
        <span>
          <a @click="switchRoute('modelField', {domainId: record.id})">字段</a>
          <a-divider type="vertical" />
          <a-dropdown>
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
  </div>
</template>

<!-- 操作MetaDomain，需要定义GPartTable -->
<script>
  import { tools } from '@/utils/util'
  import { vTableUtil } from '@/utilsv/vUtil'

  import MetaDomain from '@/store/models/MetaDomain'

  export default {
    name: "DomainPartTable",
    components: {
    },
    props: {
      configObj: {
        type: Object,
        required: true,
      }
    },
    data () {
      return {
        /** 配置&&状态 */
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
      };
    },
    inject: ['switchRoute'],
    methods: {
      /** Col字段 */
      tableColumns(){
        let that = this
        let tColumns = that.configObj.tableColumns()
        let cloneItemFunc = vTableUtil.tableCloneItem
        return tColumns.map(item => cloneItemFunc(that, item, that.$createElement))
      },
      /** 查询数据 */
      doQuery(queryRules){
        let that = this
        that.queryRules = queryRules
        let entities = MetaDomain.all()
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
        // 查询条件
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
        // 加载数据
        that.loading = true
        let query = MetaDomain.query().where(item => {
          // 条件查询
          let isMatched = true
          if(queryCondition.rules.length>0){
            isMatched = false
            queryCondition.rules.forEach(rule => {
              let field = rule.field
              let value = rule.value
              if(item[field] && item[field].match(value)){
                isMatched = true
              }
            })
          }
          return isMatched
        })
        let totalCount = query.count()
        let sort = that.tableSorter.field;
        let order = "descend" === that.tableSorter.order ? "desc" : "asc" // 默认asc
        let offset = (queryCondition.pageNum - 1) * queryCondition.pageSize
        let entityList = query.orderBy(sort?sort:'id', order?order:'desc')
          .offset(offset).limit(queryCondition.pageSize).get()
        // 包装结果
        that.loading = false
        that.tablePagination.total = totalCount
        that.tableDataSource = entityList
      },
      /** 执行操作 */
      onRecordAdd(){
        this.$emit('recordAdd');
      },
      doRecordSave(record){
        let that = this
        record['domainName'] = tools.randomString(8)
        let promise = MetaDomain.$create({
          data: record
        })
        promise.then(resp => {
          that.tableDataSource.unshift(resp)
          that.$message.success('保存成功');
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
        let promise = MetaDomain.$update({
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
          that.$message.success('更新成功');
        }).catch(err => {
          err || true
          // vTableUtil.tableReqErr(that, err, '更新失败')
        })
        return promise
      },
      doRecordDelete(record) {
        let that = this
        let promise = MetaDomain.$delete({
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
    },
  }
</script>
