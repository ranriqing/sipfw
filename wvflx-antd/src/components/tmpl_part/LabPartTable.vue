<template>
  <a-card :bordered="false" class="page-block tmpl-block">
    <a-table
      row-key="id"
      :loading="loading"
      :scroll="{ x: true }"
      :columns="datTableColumns"
      :dataSource="tableDataSource"
      :pagination="tablePagination"
      :rowSelection="selectionOn?rowSelection:null"
      :rowClassName="tableRowClassName"
      :data-cptTableColumns="cptTableColumns"
      @change="onTableChange">
      <template slot="title">
        <!-- 操作按钮 -->
        <div class="table-operation-buttons">
          <a-button-group>
            <template v-if="vTableUtil.tableEnableState(_self, sExtend, [OTHER.ADD_ENABLED])">
              <a-button type="primary" icon="plus" @click="onRecordAdd">{{ sExtend[OTHER.ADD_CAPTION] || '新增' }}</a-button>
            </template>
            <template v-if="vTableUtil.tableEnableState(_self, sExtend, [OTHER.EDIT_ENABLED])">
              <a-button type="dashed" icon="edit" @click="onRecordEdit({id: undefined})">{{ sExtend[OTHER.EDIT_CAPTION] || '编辑' }}</a-button>
            </template>
            <template v-if="vTableUtil.tableEnableState(_self, sExtend, [OTHER.IMPORT_ENABLED])">
              <a-upload
                :multiple="false"
                :showUploadList="false"
                :beforeUpload="_beforeUpload"
                :customRequest="customRequest">
                <a-button icon="cloud-upload">导入</a-button>
              </a-upload>
              <a-button icon="download" @click="onDownloadTemplate">模板</a-button>
            </template>
            <a-dropdown :disabled="false">
              <a-menu slot="overlay">
                <a-menu-item-group>
                  <template slot="title">数据导出</template>
                  <a-menu-item :disabled="exportLoading" @click="onExportExcel()">
                    <a-icon type="download" />导出Excel
                  </a-menu-item>
                </a-menu-item-group>
              </a-menu>
              <a-button> 更多 <a-icon type="down" /> </a-button>
            </a-dropdown>
          </a-button-group>
          <div v-if="vTableUtil.tableEnableState(_self, sExtend, [OTHER.BATCH_ENABLED])" style="margin-left: 10px">
            <span>
              <a-checkbox :checked="selectionOn" @change="_toggleSelection($event)"></a-checkbox>
              {{ selectionOn?'勾选':'条件' }}选中<span class="text-green font-bold">{{ cptTotal }}</span> 条，
            </span>
            <a-button-group>
              <a-button :disabled="false" size="small" @click="onRecordBatch({batchNum: cptTotal})">
                <a-icon type="g-batch" style="margin-right: 4px;"></a-icon> 批量弹窗
              </a-button>
            </a-button-group>
          </div>
          <div class="config-wrapper">
            <!-- 操作项 -->
            <a-button size="small" @click="vTableUtil.handleOpsConfig(_self, sModel)">
              <a-icon type="control"/>操作项
            </a-button>
            <a-divider type="vertical" />
            <!-- 表格列 -->
            <a-button size="small" @click="vTableUtil.handleColsConfig(_self)">
              <a-icon type="setting"/>表格列
            </a-button>
          </div>
        </div>
        <!-- 拖拽表头 -->
        <div class="draggable-container draggable-table-container">
          <draggable
            tag="div"
            class="draggable-columns-container"
            :list="datTableColumns"
            v-bind="vTableUtil.cptDragTargetOptions()"
            @change="(changeEvent) => vTableUtil.handleItemDrag(_self, changeEvent)">
            <div
              class="draggable-column"
              v-for="(item, index) in datTableColumns"
              :key="index">
              <a-icon :type="vTableUtil.domainFieldIcon(item, 'type')" class="drag drag-widget"/>
              <span class="column-item">{{ item.title }}</span>
              <span class="item-actions">
                <a-popover :title="'COL(' + item.title + ')'" placement="bottomLeft" overlayClassName="lab-popover">
                  <a-icon class="icon-action" type="bug" />
                  <template slot="content">
                    <div class="lab-item">
                      <div class="title">Ident</div>
                      <div class="content"> <a @click="copyText(_.get(item, 'field'), $event)">{{ item.field }}</a> </div>
                    </div>
                    <div class="lab-item">
                      <div class="title">Type</div>
                      <div class="content">{{ item.type }}</div>
                    </div>
                    <div class="lab-item">
                      <div class="title">Comp</div>
                      <div class="content">{{ item.innerRender?'innerRender':item.customRender?'customRender':item.component }}</div>
                    </div>
                  </template>
                </a-popover>
                <a-divider type="vertical" />
                <a-icon class="icon-action" type="edit" @click="vTableUtil.handleItemEdit(_self, item)"/>
                <a-divider type="vertical" />
                <a-popconfirm title="确定删除?" @confirm="() => vTableUtil.handleItemDelete(_self, item)">
                  <a-icon class="icon-action" type="delete" />
                </a-popconfirm>
              </span>
            </div>
          </draggable>
        </div>
      </template>
      <!-- Render定义 -->
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

    <!-- Ref弹窗 -->
    <GColsPop
      ref="refCols"
      :configObj="colsPopConfig">
    </GColsPop>
    <GFormPop
      ref="refMeta"
      :configObj="metaPopConfig">
    </GFormPop>
    <GFormPop
      ref="refOperation"
      :configObj="opsPopConfig">
    </GFormPop>

    <!-- 导入导出进度条 -->
    <a-card
      v-if="stores.isDesktop() && cptProgressInfos.length > 0"
      class="progress-card"
      :bodyStyle="{
        'width': '460px',
        'max-height': '250px',
        'overflow-y':'auto'
      }">
      <div
        class="flex flex-row card-item"
        v-for="(infoItem, index) in cptProgressInfos"
        :key="index">
        <a-avatar
          slot="avatar"
          shape="square"
          :size="60"
          :icon="'file-zip'">
        </a-avatar>
        <div class="flex flex-column item-content">
          <div class="title">
            [{{ infoItem.type==='import'?'导入':infoItem.type==='export'?'导出':'None' }}]
            &nbsp;{{ infoItem.identifier }}
          </div>
          <a-progress
            class="progress"
            :percent="_.get(infoItem, 'percent', 0.00)"
            :status="_.get(infoItem, 'status', 'active')">
          </a-progress>
        </div>
      </div>
    </a-card>
  </a-card>
</template>

<!--
# 组件说明
  Lab表格组件
# 配置组件
  sModel: Object        // 当前组件SModel
  sContext: Object      // 上下文sContext
  注：表格字段不包含['RefInverse']
# 操作方法
  $emit('metaModel')    // 重新加载Model
  doQuery(queryRules)
  $emit('recordAdd')
  doRecordSave(record)
  $emit('recordEdit', record)
  doRecordUpdate(record)
-->

<script>
  import AUpload from 'ant-design-vue/lib/upload';
  import ACheckbox from 'ant-design-vue/lib/checkbox';

  const settings = require('@/settings')
  import { OTHER, QUERY, FIELD } from '@/constants'
  import {stores, converters, tools} from '@/utils/util'
  import { vTableUtil } from '@/utilsv/vUtil'
  import { postAction, putAction, deleteAction, formAction, downloadAction } from '@/api/api'

  import MetaDomain from '@/store/models/MetaDomain'
  import MetaPageComponentField from '@/store/models/MetaPageComponentField'

  import GColsPop from '@/components/page_pop/GColsPop'
  import GFormPop from '@/components/page_pop/GFormPop'

  import labPartTableColsPopConfig from './configs/labPartTableColsPopConfig'
  import labPartTableMetaPopConfig from './configs/labPartTableMetaPopConfig'
  import labPartTableOpsPopConfig from './configs/labPartTableOpsPopConfig'

  export default {
    name: "LabPartTable",
    components: {
      AUpload,
      ACheckbox,
      GColsPop,
      GFormPop,
    },
    /* eslint-disable */
    props: {
      sModel: {
        type: Object,
        required: true,
      },
      sPart: {
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
        /** 配置&状态 */
        OTHER: OTHER,
        baseUrl: settings.BASE_URL,
        vTableUtil: vTableUtil,
        stores: stores,
        colsPopConfig: labPartTableColsPopConfig,
        metaPopConfig: labPartTableMetaPopConfig,
        opsPopConfig: labPartTableOpsPopConfig,
        loading:false,
        importLoading: false,
        importIdentifier: undefined,
        exportLoading: false,
        exportIdentifier: undefined,
        popVisible: false,
        datTableColumns: [],
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
        defaultDataSource: [],
        /** 勾选记录 */
        selectionOn: false,
        selectedRowKeys: [],
        rowSelection: {
          onChange: (selectedRowKeys, selectedRows) => {
            selectedRowKeys || selectedRows || true
            that.selectedRowKeys = selectedRowKeys
          },
        },
        /** 扩展部分 */
        progressInfos: [],
        vmDomainInfo: {},
        customRequest: ({onSuccess, file}) => {
          let that = this
          let formData = new FormData()
          that.importIdentifier = that.$moment().format('YYYYMMDD') + '_' + tools.randomString(10)
          formData.set('uploadFile', file)
          formData.set('importIdentifier', that.importIdentifier)
          let importUri = that._modelUri('importUri')
          if(!importUri){
            that.$message.warn('暂无导入调用路径')
            return
          }
          that.importLoading = true
          formAction(importUri, formData).then(resp=>{
            that.importLoading = false
            onSuccess(resp, file)
            that.$message.success(`数据导入中 ...`);
          }).catch(err => {
            err || true
            that.importLoading = false
            that.$message.warn('数据导入失败')
          })
        }
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
        that.datTableColumns = metaFields.map(item => cloneItemFunc(that, item, that.$createElement))
        return metaFields
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
      cptProgressInfos: function(){
        let that = this
        let progressArray = []
        if(that.importLoading){
          progressArray.push(that.progressInfos.find(item => item.identifier === that.importIdentifier))
        }
        if(that.exportLoading){
          progressArray.push(that.progressInfos.find(item => item.identifier === that.exportIdentifier))
        }
        return progressArray.filter(Boolean); // 过滤掉undefined
      },
    },
    inject: ['wsSubscribe', 'wsUnSubscribe', 'copyText'],
    methods: {
      /** Record操作 */
      doQuery(queryRules){
        this.queryRules = queryRules
        this._loadData(1)
      },
      onTableChange(pagination, filters, sorter) {
        this.tablePagination = pagination
        this.tableSorter = sorter
        this._loadData()
      },
      _loadData(arg, download) {
        let that = this
        if (arg === 1) {
          that.tablePagination.current = 1;
        }
        let queryUri = that._modelUri('queryUri')
        if(!queryUri){
          that.tableDataSource = that.defaultDataSource
          that.$message.warn('暂无查询调用路径')
          return
        }
        // 查询条件
        let queryCondition = {
          pageNum: that.tablePagination.current,
          pageSize: that.tablePagination.pageSize,
          orderBy: null,
          condition: 'and',
          rules: []
        }
        that.queryRules.forEach(rule => {
          queryCondition.rules.push(rule)
        })
        // 执行动态代码
        let queryRulesCode = that._.get(that.sPart, 'extend.queryRulesCode')
        if(queryRulesCode){
          let executeCode = new Function('queryCondition', 'QUERY', queryRulesCode);
          executeCode(queryCondition, QUERY);
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
        if(download){
          let fileName = `数据列表.xlsx`
          that.exportLoading = true
          that.exportIdentifier = that.$moment().format('YYYYMMDD') + '_' + tools.randomString(10)
          let exportUri = that._modelUri('exportUri')
          queryCondition.rules.push({ field: '_extend_identifier', value: that.exportIdentifier, operator: "equal" })
          downloadAction(exportUri, queryCondition).then(resp => {
            that.exportLoading = false
            tools.triggerBlobDownload(resp, fileName)
          }).catch(err=>{
            that.exportLoading = false
            vTableUtil.tableReqErr(that, err, '导出失败')
          })
        }else{
          // 关联字段批量加载：收集 MANY_TO_ONE / ONE_TO_MANY 字段名
          let enrichFields = that.datTableColumns
            .filter(col => [FIELD.MANY_TO_ONE, FIELD.ONE_TO_MANY].indexOf(col.type) !== -1)
            .map(col => col.field)
          if (enrichFields.length > 0) {
            queryCondition.enrichFields = enrichFields
          }
          // 加载数据
          that.loading = true
          postAction(queryUri, queryCondition).then(resp => {
            that.loading = false
            if(resp.gmodel && resp.gmodel.entityList){
              that.tableDataSource = resp.gmodel.entityList
              that.tablePagination.total = resp.gmodel.totalCount
            }
          }).catch(err => {
            that.loading = false
            that.tableDataSource = that.defaultDataSource
            vTableUtil.tableReqErr(that, err, '加载失败')
          })
        }
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
      doRecordOps(uriPath, record){
        let that = this
        let opsUri = that._modelUri('opsUri', uriPath)
        if(!opsUri){
          that.$message.warn('暂无操作调用路径')
          return
        }
        let promise = postAction(opsUri, record)
        promise.then((resp) => {
          let rstRecord = resp.gmodel
          let recordIndex = that.tableDataSource.findIndex( item => item.id === rstRecord.id)
          if(recordIndex !== -1){
            that.tableDataSource.splice(recordIndex, 1, rstRecord)
          }
          that.$message.success('操作成功');
        }).catch(err => {
          vTableUtil.tableReqErr(that, err, '操作失败')
        })
        return promise
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
        let batchUri = that._modelUri('batchUri')
        if(!batchUri){
          that.$message.warn('暂无批量调用路径')
          return
        }
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
      onExportExcel(){
        let that = this
        that.$confirm({
          title: '确认弹窗',
          content: `确定导出当前条件下数据?`,
          onOk() {
            that._loadData(null, true)
          }
        });
      },
      onDownloadTemplate(){
        let that = this
        let templateUri = that._modelUri('templateUri')
        if(!templateUri){
          that.$message.warn('暂无模板调用路径')
          return
        }
        tools.authorizedDownload(templateUri, {}, '导入模板.xlsx')
      },
      tableRowClassName(record, index){
        return this.datTableColumns
          .map(col => col.rowClassName)
          .filter(Boolean)
          .map(fn => fn(record, index))
          .filter(Boolean)
          .join(' ')
      },
      /** 工具方法 */
      _modelUri(uriType, uriPath){
        let that = this
        let modelMetaDomainId = that.sModel?.metaDomainId
        let uriTypePath = uriType==='queryUri'?'index':
                          uriType==='saveUri'?'save':
                          uriType==='updateUri'?'update':
                          uriType==='deleteUri'?'delete':
                          uriType==='batchUri'?'batch':
                          uriType==='importUri'?'importExcel':
                          uriType==='exportUri'?'exportExcel':
                          uriType==='templateUri'?'downloadTemplate':
                          uriType==='excelUri'?'downloadExcel':
                          uriType==='opsUri'?uriPath:
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
      _beforeUpload(file){
        let that = this
        let isExcel = file.name.endsWith('.xlsx') || file.name.endsWith('.xls');
        let isLt2M = file.size / 1024 / 1024 < 5
        if (!isExcel) {
          this.$message.warn('只可以上传Excel文件!');
          return false;
        }else if (!isLt2M) {
          that.$message.error('文件大小不能超过5MB!')
          return false;
        }
        return true;
      },
      _queryCondition(){
        let that = this
        let queryCondition = {
          orderBy: null,
          condition: 'and',
          rules: [],
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
      _updateHeaderWidth(){
        // https://github.com/ant-design/ant-design/issues/7603
        let head = document.querySelector('.ant-table-thead');
        if(head && head.childNodes[0]){
          let headArr = Array.from(head.childNodes[0].childNodes);
          head.childNodes[0].style.display = 'flex';
          let body = document.querySelector('.ant-table-tbody');
          if(body && body.childNodes[0]){
            let bodyArr = Array.from(body.childNodes[0].childNodes);
            for (let i = 0; i < headArr.length; i++){
              headArr[i].style.width = bodyArr[i].offsetWidth + 'px';
            }
          }
        }
      },
      wsDomainFunc(messageJson){
        let that = this
        let progressIndex = that.progressInfos.findIndex( item => item.identifier === messageJson.identifier)
        if(progressIndex !== -1){
          that.progressInfos.splice(progressIndex, 1, messageJson)
        }else if(messageJson.percent < 50){
          that.progressInfos.push(messageJson)
        }
        if(that._.get(messageJson, 'extendMap.finished')){
          that.$success({
            title: `数据导入完成`,
            onOk() {
              that.progressInfos.splice(progressIndex, 1)
              that._loadData(1)
            },
            content: function(h){
              h && true
              let amountInfo = messageJson['extendMap']
              let vNodes = []
              if(amountInfo){
                [
                  {field:'successAmount', color:'green', title:'成功'},
                  {field:'failedAmount', color:'red', title:'失败'},
                ].forEach(item => {
                  if(amountInfo[item.field]){
                    let props = { color: item.color }
                    vNodes.push(
                      <a-tag {...{ props }}>{item.title} {amountInfo[item.field]}条</a-tag>
                    )
                  }
                })
              }
              if(amountInfo['totalAmount']){
                vNodes.push(<a-divider type="vertical" />)
                vNodes.push(<a-tag color="blue">总数{ amountInfo['totalAmount'] }条</a-tag>)
              }
              let excelUri = that._modelUri('excelUri')
              if(amountInfo['localFilename'] && excelUri){
                vNodes.push([<br/>, <br/>, <span>结果文件:</span>])
                vNodes.push(<a onClick={ () => tools.authorizedDownload(excelUri, {}, amountInfo['localFilename']) }> { amountInfo['localFilename'] }</a>)
              }
              return (
                <div>
                  {vNodes}
                </div>
              )
            },
          });
        }
      },
    },
    mounted() {
      let that = this
      // window.addEventListener('resize', that._updateHeaderWidth);
      // 订阅数据导入Ws
      let metaDomain = MetaDomain.find(that.sModel?.metaDomainId)
      if(metaDomain) {
        let subPathDomain = stores.wsTopicPath(metaDomain.domainName)
        that.wsSubscribe(subPathDomain, that.wsDomainFunc)
      }
    },
    beforeDestroy(){
      let that = this
      // window.removeEventListener('resize', that._updateHeaderWidth)
      // 销毁数据导入Ws
      let metaDomain = MetaDomain.find(that.sModel?.metaDomainId)
      if(metaDomain) {
        let subPathDomain = stores.wsTopicPath(metaDomain.domainName)
        that.wsUnSubscribe(subPathDomain)
      }
    },
  }
</script>

<style lang="scss" scoped>
.progress-card{
  position: fixed;
  bottom: 30px;
  right: 30px;
  box-shadow: 0 4px 6px #909399;
  .card-item{
    margin-bottom: 15px;
    background-color: #f3f4f6;
    border-radius: 5px;
    &:last-of-type{
      margin-bottom: 0;
    }
    .item-content{
      margin-left: 15px;
      .title{
        padding: 6px 0 2px 0;
        font-weight: bold;
      }
      .progress{
        width: 330px;
      }
    }
  }
}
</style>
