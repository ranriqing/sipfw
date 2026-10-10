<template>
  <div>
    <a-card v-show="!pageVisible" :bordered="false" class="page">
      <!-- 查询部分 -->
      <div class="page-query-wrapper">
        <GPartQuery
          ref="slotQuery"
          :configObj="partConfig.queryConfig"
          @doSubmit="doQuery">
        </GPartQuery>
      </div>

      <!-- 表格部分 -->
      <div class="page-table-wrapper">
        <GPartTable
          ref="slotTable"
          :configObj="partConfig.tableConfig"
          @recordAdd="onRecordAdd"
          @recordEdit="onRecordEdit">
        </GPartTable>
      </div>
    </a-card>

    <!-- 弹窗部分 -->
    <GFormPop
      ref="slotAdd"
      :popVisible="popAddVisible"
      :configObj="popConfig.addConfig">
    </GFormPop>
    <GFormPop
      ref="slotEdit"
      :popVisible="popEditVisible"
      :configObj="popConfig.editConfig">
    </GFormPop>
  </div>
</template>

<script>
  import GPartQuery from '@/components/page_part/GPartQuery'
  import GPartTable from './DomainPartTable'
  import GFormPop from '@/components/page_pop/GFormPop'

  import partConfig from './configs/partConfig'
  import popConfig from './configs/popConfig'

  export default {
    name: "DomainPage",
    components: {
      GPartQuery,
      GPartTable,
      GFormPop,
    },
    data(){
      return {
        /** 配置&状态 */
        partConfig: partConfig,
        popConfig: popConfig,
        popAddVisible: false,
        popEditVisible: false,
      }
    },
    computed: {
      pageVisible: function() {
        return this.popAddVisible || this.popEditVisible
      },
    },
    methods:{
      /** 查询部分 */
      doQuery(queryRules){
        this.$refs.slotTable?.doQuery(queryRules)
      },
      /** 弹窗操作 */
      onRecordAdd(record){
        let that = this
        let slotRef = that.$refs.slotAdd
        that.popAddVisible = slotRef.isPage()
        slotRef.showPop(record, {},
          (record, context) => { /* submit */
            let slotTable = that.$refs.slotTable
            let promise = slotTable.doRecordSave(record, context)
            promise.then(() => that.popAddVisible = false)
            return promise
          }, () => { /* cancel */
            that.popAddVisible = false
          }
        )
      },
      onRecordEdit(record) {
        let that = this
        let slotRef = that.$refs.slotEdit
        that.popEditVisible = slotRef.isPage()
        slotRef.showPop(record, {},
          (record, context) => { /* submit */
            let slotTable = that.$refs.slotTable
            let promise = slotTable.doRecordUpdate(record, context)
            promise.then(() => that.popEditVisible = false)
            return promise
          }, () => { /* cancel */
            that.popEditVisible = false
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
