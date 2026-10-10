<template>
  <a-card :bordered="false" class="page-block" >
    <a-list
      row-key="id"
      :loading="loading"
      :dataSource="tableDataSource"
      :pagination="false"
      :renderItem="_renderItem"
      itemLayout="horizontal"
      :data-cptTableColumns="cptTableColumns">
      <template slot="header">
        <div class="table-operation-buttons">
          <a-button-group>
            <template v-if="vTableUtil.tableEnableState(_self, sExtend, [OTHER.ADD_ENABLED])">
              <a-button type="primary" icon="plus" @click="onRecordAdd">{{ sExtend[OTHER.ADD_CAPTION] || '新增' }}</a-button>
            </template>
          </a-button-group>
          <div class="item-right">共{{ _.get(tablePagination, 'total', 0) }}条</div>
        </div>
      </template>
      <!-- Render定义 -->
      <!-- 改为使用_renderItem
      <a-list-item slot="renderItem" slot-scope="record, index">
        <a-list-item-meta></a-list-item-meta>
        <div class="list-information"></div>
        <div slot="actions"></div>
      </a-list-item>-->
      <div
        v-if="loadMore"
        slot="loadMore"
        :style="{ textAlign: 'center', marginTop: '12px', height: '32px', lineHeight: '32px' }">
        <a-button :loading="loading" @click="_loadData()">
          加载更多 ...
        </a-button>
      </div>

    </a-list>
  </a-card>
</template>

<!--
# 组件说明
  App表格组件
# 配置组件
  sModel: Object        // 当前组件SModel
  sContext: Object      // 上下文sContext
  注：表格字段不包含['RefInverse']
# 操作方法
  doQuery(queryRules)
  $emit('recordAdd')
  doRecordSave(record)
  $emit('recordEdit', record)
  doRecordUpdate(record)
-->

<script>
  import AAvatar from 'ant-design-vue/lib/avatar';
  import AList from 'ant-design-vue/lib/list';

  const settings = require('@/settings')
  import { OTHER } from '@/constants'
  import { converters, tools } from '@/utils/util'
  import { vTableUtil } from '@/utilsv/vUtil'
  import { postAction, putAction, deleteAction } from '@/api/api'

  import MetaDomain from '@/store/models/MetaDomain'
  import MetaPageComponentField from '@/store/models/MetaPageComponentField'

  export default {
    name: "AppPartList",
    components: {
      AAvatar,
      AList,
      AListItem: AList.Item,
      AListItemMeta: AList.Item.Meta,
    },
    /* eslint-disable */
    props: {
      sModel: {
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
      return {
        /** 配置&状态 */
        OTHER: OTHER,
        tools: tools,
        baseUrl: settings.BASE_URL,
        vTableUtil: vTableUtil,
        loading:false,
        loadMore:false,
        datTitleColumn: undefined,
        datDescriptionColumn: undefined,
        datInformationColumns: [],
        /** 查询&数据源 */
        queryRules: [],
        tableSorter: {},
        tablePagination: {
          current: 1,
          pageSize: 10,
          pageSizeOptions: ['10', '30', '50', '100'],
          total: 0
        },
        tableDataSource: [],
        defaultDataSource: [],
      }
    },
    computed: {
      sExtend: function(){
        return converters.parseObj(this.sModel?.extend)
      },
      cptTableColumns: function(){
        let that = this
        let metaFields = MetaPageComponentField.query().where('metaPageComponentId', that.sModel?.id)
          .orderBy('seqOrder', 'asc').orderBy('id', 'asc').with('relMetaDomainField').get()
        let cloneItemFunc = vTableUtil.tableCloneItem
        let datTableColumns = metaFields.map(item => cloneItemFunc(that, item, that.$createElement))
        let datListColumns = datTableColumns.filter(item =>
          !!that._.get(item, 'extend.mobile')
          || item['component'] === 'linkRender'
        )
        // titleColumn 为 linkRender 或者首个 mobile 字段，或者手工非 actionRender 列
        that.datTitleColumn = datListColumns.find(item => item['component'] === 'linkRender' )
        if(!that.datTitleColumn) { that.datTitleColumn = datListColumns[0] }
        if(!that.datTitleColumn){ // 使用datTableColumns修正
          that.datTitleColumn = datTableColumns.find(item => item['component'] !== 'actionRender' )
        }
        // descriptionColumn 为不是 titleColumn 的首个非 actionRender 列
        that.datDescriptionColumn = datListColumns.find(item =>
          item['field'] !== that.datTitleColumn['field']
        )
        // informationColumns 为不是 titleColumn 也不是 descriptionColumn 的列
        that.datInformationColumns = datListColumns.filter(item =>
          item['field'] !== that.datTitleColumn['field']
          && item['field'] !== that.datDescriptionColumn['field']
        )
        return metaFields
      },
    },
    inject: ['wsSubscribe', 'wsUnSubscribe'],
    methods: {
      /** Record操作 */
      doQuery(queryRules){
        this.queryRules = queryRules
        this._loadData(1)
      },
      _loadData(arg) {
        let that = this
        let reqPageNum
        if (arg === 1) {
          reqPageNum = 1
          that.tablePagination.current = 1;
        }else{
          reqPageNum = that.tablePagination.current + 1
        }
        let queryUri = that._modelUri('queryUri')
        if(!queryUri){
          that.tableDataSource = that.defaultDataSource
          that.$message.warn('暂无查询调用路径')
          return
        }
        // 查询条件
        let queryCondition = {
          pageNum: reqPageNum,
          pageSize: that.tablePagination.pageSize,
          orderBy: null,
          condition: 'and',
          rules: []
        }
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
        postAction(queryUri, queryCondition).then(resp => {
          that.loading = false
          let entityList = that._.get(resp, 'gmodel.entityList')
          if(entityList){
            that.tableDataSource = (arg===1)?entityList:that.tableDataSource.concat(entityList)
            if(entityList.length>0){
              that.tablePagination.current = reqPageNum // 页码赋值(失败不增加页码)
            }
            that.tablePagination.total = that._.get(resp, 'gmodel.totalCount')
            that.loadMore = (entityList.length !== 0) && (that.tableDataSource.length < that.tablePagination.total)
          }
        }).catch(err => {
          that.loading = false
          if (arg === 1) {
            that.tableDataSource = that.defaultDataSource
          }
          vTableUtil.tableReqErr(that, err, '加载失败')
        })
      },
      /** 执行操作 */
      onRecordAdd(){
        this.$emit('recordAdd');
      },
      doRecordSave(record){
        let that = this
        let saveUri = that._modelUri('saveUri')
        if(!saveUri){
          that.tableDataSource = that.defaultDataSource
          that.$message.warn('暂无保存调用路径')
          return
        }
        let promise = postAction(saveUri, record)
        promise.then(resp => {
          let rstRecord = resp.gmodel
          that.tableDataSource.unshift(rstRecord)
          that.$message.success('保存成功');
        }).catch(err => {
          vTableUtil.tableReqErr(that, err, '保存失败')
        })
        return promise
      },
      onRecordEdit(record){
        this.$emit('recordEdit', record)
      },
      doRecordUpdate(record){
        let that = this
        let updateUri = that._modelUri('updateUri')
        if(!updateUri){
          that.tableDataSource = that.defaultDataSource
          that.$message.warn('暂无更新调用路径')
          return
        }
        let promise = putAction(updateUri, record.id, record)
        promise.then((resp) => {
          let rstRecord = resp.gmodel
          let recordIndex = that.tableDataSource.findIndex( item => item.id === rstRecord.id)
          if(recordIndex !== -1){
            that.tableDataSource.splice(recordIndex, 1, rstRecord)
          }
          that.$message.success('更新成功');
        }).catch(err => {
          vTableUtil.tableReqErr(that, err, '更新失败')
        })
        return promise
      },
      doRecordDelete(record){
        let that = this
        let deleteUri = that._modelUri('deleteUri')
        if(!deleteUri){
          that.tableDataSource = that.defaultDataSource
          that.$message.warn('暂无删除调用路径')
          return
        }
        let promise = deleteAction(deleteUri, record.id, {})
        promise.then(() => {
          let recordIndex = that.tableDataSource.findIndex( item => item.id === record.id)
          if(recordIndex !== -1){
            that.tableDataSource.splice(recordIndex, 1)
          }
          that.$message.success('删除成功');
        }).catch(err => {
          vTableUtil.tableReqErr(that, err, '删除失败')
        })
      },
      /** 工具方法 */
      _renderItem(record, index){
        index || true
        let that = this
        // Title 部分
        let titleNodes = [];
        if(that.datTitleColumn){
          const on = {
            click: () => {
              that.onRecordEdit(record)
            },
          }
          titleNodes.push(<a slot="title"  {...{ on: on }}>
            { record[that.datTitleColumn.field] }
          </a>)
        }
        // Description 部分
        let descriptionNodes = [];
        if(that.datDescriptionColumn){
          let columnNode = undefined
          let recordText = record[that.datDescriptionColumn.field]
          let customRender = that.datDescriptionColumn.customRender
          if(customRender && that._.isFunction(customRender)){
            columnNode = customRender(recordText, record)
          }else{
            columnNode = record
          }
          descriptionNodes.push(<div slot="description">
            { columnNode }
          </div>)
        }
        // Information 部分
        let informationNodes = [];
        that.datInformationColumns.forEach(informationColumn => {
          let columnNode = undefined
          let recordText = record[informationColumn.field]
          let customRender = informationColumn.customRender
          if(customRender && that._.isFunction(customRender)){
            columnNode = customRender(recordText, record)
          }else{
            columnNode = record
          }
          informationNodes.push(<div {...{ attrs: {class: 'list-information-item'} }}>
            { columnNode }
          </div>)
        })
        // 操作部分
        let actionNodes = [];
        if(vTableUtil.tableEnableState(that, that.sExtend, [OTHER.EDIT_ENABLED])){
          actionNodes.push(<a slot="title"  {...{ on: {
              click: () => { that.onRecordEdit(record) },
            } }}>
            { that.sExtend[OTHER.EDIT_CAPTION] || '编辑' }
          </a>)
        }
        if(vTableUtil.tableEnableState(that, that.sExtend, [OTHER.EDIT_ENABLED, OTHER.DELETE_ENABLED])){
          actionNodes.push(<a-divider {...{ props: { type: 'vertical' } }} />)
        }
        if(vTableUtil.tableEnableState(that, that.sExtend, [OTHER.DELETE_ENABLED])){
          actionNodes.push(<a-dropdown>
            <a {...{ attrs: {class: 'ant-dropdown-link'} }}>
              更多 <a-icon {...{ props: {type: 'down'} }} />
            </a>
            <a-menu slot="overlay">
              <a-menu-item>
                <a-popconfirm {...{ attrs: {title: '确定删除吗?'}, on: {
                    confirm: () => { that.doRecordDelete(record) },
                  } }}>
                <a>删除</a>
              </a-popconfirm>
              </a-menu-item>
            </a-menu>
          </a-dropdown>)
        }
        return (<a-list-item>
          <a-list-item-meta>
            { titleNodes }
            { descriptionNodes }
          </a-list-item-meta>
          <div {...{ attrs: {class: 'list-information'} }}>
            { informationNodes }
          </div>
          <div slot="actions">
            { actionNodes }
          </div>
        </a-list-item>)
      },
      _modelUri(uriType){
        let that = this
        let modelMetaDomainId = that.sModel?.metaDomainId
        let uriTypePath = uriType==='queryUri'?'index':
                          uriType==='saveUri'?'save':
                          uriType==='updateUri'?'update':
                          uriType==='deleteUri'?'delete':
                          'none'
        if(modelMetaDomainId===0) { // 自定义组件
          let uriPrefix = that._.get(that.sExtend, 'uriPrefix')
          if(uriPrefix){
            uriPrefix = uriPrefix.startsWith('/')?uriPrefix.substring(1):uriPrefix
          }
          return (uriPrefix && uriTypePath)?`/${uriPrefix}/${uriTypePath}`:undefined
        }else{                    // 数据表组件
          let metaDomain = MetaDomain.find(modelMetaDomainId)
          return (metaDomain && uriTypePath)?`/vm/${tools.toLowerCamel(metaDomain.domainName)}/${uriTypePath}`:undefined
        }
      },
    },
  }
</script>

<style lang="scss" scoped>
  /*.list-content {
    flex: 1;
    display: flex;
    justify-content: flex-start;
    flex-wrap: wrap;
    .list-content-item {
      display: inline-block;
      vertical-align: middle;
    }
  }*/
</style>