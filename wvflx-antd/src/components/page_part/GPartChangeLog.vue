<template>
  <div class="change-log">
    <a-spin :spinning="loading">
      <a-timeline v-if="timeline.length > 0">
        <a-timeline-item
          v-for="(node, index) in timeline"
          :key="index"
          :color="nodeColor(node.operationType)">
          <div class="node-header">
            <a-tag :color="nodeTagColor(node.operationType)">{{ operationText(node.operationType) }}</a-tag>
            <span class="operator">{{ node.operatorName || '-' }}</span>
            <span class="date">{{ node.dateCreated }}</span>
          </div>
          <a-table
            size="small"
            :pagination="false"
            :dataSource="node.fields"
            :columns="columns"
            rowKey="fieldName">
          </a-table>
        </a-timeline-item>
      </a-timeline>
      <a-empty v-else description="暂无变更记录" />
    </a-spin>
  </div>
</template>

<script>
  import ATimeline from 'ant-design-vue/lib/timeline';
  import ATable from 'ant-design-vue/lib/table';
  import ASpin from 'ant-design-vue/lib/spin';
  import AEmpty from 'ant-design-vue/lib/empty';

  import { getAction } from '@/api/api'

  export default {
    name: "GPartChangeLog",
    components: {
      ATimeline,
      ATimelineItem: ATimeline.Item,
      ATable,
      ASpin,
      AEmpty,
    },
    props: {
      configObj: {
        type: Object,
        required: true,
      }
    },
    data () {
      return {
        loading: false,
        timeline: [],
        columns: [
          { title: '字段', dataIndex: 'fieldTitle', key: 'fieldTitle', width: 140 },
          { title: '旧值', dataIndex: 'oldValue', key: 'oldValue' },
          { title: '新值', dataIndex: 'newValue', key: 'newValue' },
        ],
      }
    },
    methods: {
      onRefresh(){
        this._loadData()
      },
      _loadData(){
        let that = this
        that.loading = true
        let promise = getAction('/vmFieldChange/list', {
          domainName: that.configObj.domainName,
          recordId: that.configObj.recordId,
        })
        promise.then(resp => {
          that.loading = false
          that.timeline = that._.get(resp, 'gmodel.entityList') || []
        }).catch(() => {
          that.loading = false
          that.timeline = []
        })
        return promise
      },
      operationText(type){
        return { CREATE: '新增', UPDATE: '修改', DELETE: '删除' }[type] || type
      },
      nodeColor(type){
        return { CREATE: 'green', UPDATE: 'blue', DELETE: 'red' }[type] || 'gray'
      },
      nodeTagColor(type){
        return { CREATE: 'green', UPDATE: 'blue', DELETE: 'red' }[type] || ''
      },
    },
    mounted() {
      this._loadData()
    },
  }
</script>

<style lang="scss" scoped>
  .change-log {
    padding: 8px 0;
    .node-header {
      margin-bottom: 6px;
      .operator { margin: 0 8px; color: #666; }
      .date { color: #c0c0c0; }
    }
  }
</style>
