<template>
  <div>
    <a-card v-show="!pageVisible" :bordered="false" class="page">
      <!-- 头部部分 -->
      <div slot="title" class="flex flex-row main-between cross-center">
        <div>
          <a @click="onGoBack"><a-icon type="left"/> 返回</a>
          【{{ _.get(cptDomain, 'title', '暂无') }}】字段列表
        </div>
        <div>
          <a style="margin-right: 16px;" @click="_copyText(_.get(cptDomain, 'domainName'), $event)">
            <a-icon type="copy"/>
            {{ _.get(cptDomain, 'domainName') }}
          </a>
          <a @click="onRefresh"><a-icon type="sync"/> 刷新</a>
        </div>
      </div>

      <!-- 表格部分 -->
      <div class="page-table-wrapper">
        <GPartTable
          ref="slotTable"
          :domainId="domainId"
          :configObj="debug?partConfig.debugTableConfig:partConfig.tableConfig"
          @recordAdd="onRecordAdd"
          @recordEdit="onRecordEdit"
          @relationAdd="onRelationAdd"
          @constraintEdit="onConstraintEdit">
        </GPartTable>
      </div>
    </a-card>

    <!-- 弹窗部分 -->
    <GFormPop
      ref="slotAdd"
      :configObj="popConfig.addConfig">
    </GFormPop>
    <GFormPop
      ref="slotEdit"
      :configObj="popConfig.editConfig">
    </GFormPop>

    <!-- 扩展弹窗 -->
    <GFormPop
      ref="slotConstraint"
      :configObj="popConfig.constraintConfig">
    </GFormPop>
    <GFormPop
      ref="slotRelation"
      :configObj="popConfig.relationConfig">
      <template #relDomainOrFieldId="{record, item, form}">
        <div class="flex flex-row flex-row-reverse">
          <a-select
            :placeholder="cptMany2One?'关联表':'关联字段'"
            v-decorator="[
              'relDomainOrFieldId',
              {
                rules: [{ required: true, message: '请选择关联表' }],
              },
            ]">
            <a-select-option
              v-for="option in cptMany2One?cptAvailableDomains:cptAvailableMany2OneFields"
              :key="option['id']"
              :value="option['id']">
              {{ cptMany2One?option['title']:`${_.get(option, 'relMetaDomain.title')} (${option['title']})` }}
            </a-select-option>
          </a-select>
          <a-select
            style="width: 120px;"
            placeholder="关联类型"
            v-decorator="[
              'relType',
              {
                rules: [{ required: true, message: '关联类型' }],
              },
            ]"
            @change="(event) => changeRelType(event, form)">
            <a-select-option value="ManyToOne">
              多对一
            </a-select-option>
            <a-select-option value="OneToMany">
              一对多
            </a-select-option>
          </a-select>
        </div>
      </template>
    </GFormPop>
  </div>
</template>

<script>
  import Clipboard from 'clipboard'
  import {FIELD, OTHER} from '@/constants'
  import { tools, domains, converters } from '@/utils/util'
  import { vPageUtil } from '@/utilsv/vUtil'

  import MetaDomain from '@/store/models/MetaDomain'
  import MetaDomainField from '@/store/models/MetaDomainField'

  import GPartQuery from '@/components/page_part/GPartQuery'
  import GPartTable from './FieldPartTable'
  import GFormPop from '@/components/page_pop/GFormPop'

  import partConfig from './configs/fieldPartConfig'
  import popConfig from './configs/fieldPopConfig'

  export default {
    name: "FieldPage",
    components: {
      GPartQuery,
      GPartTable,
      GFormPop,
    },
    data(){
      return {
        debug: false,
        FIELD: FIELD,
        vPageUtil: vPageUtil,
        partConfig: partConfig,
        popConfig: popConfig,
        popAddVisible: false,
        popEditVisible: false,
        popConstraintVisible: false,
        popRelationVisible: false,
        /** 扩展数据 */
        relType: FIELD.MANY_TO_ONE,
        domainId: null,
      }
    },
    computed: {
      pageVisible: function() {
        return this.popAddVisible || this.popEditVisible || this.popConstraintVisible || this.popRelationVisible
      },
      cptDomain: function(){
        return MetaDomain.find(this.domainId)
      },
      cptAvailableDomains: function(){
        return MetaDomain.query().where('visible', true)
          .orderBy('order').orderBy('id').get()
      },
      cptAvailableMany2OneFields: function(){
        let that = this
        return domains.many2OneFields(undefined, that.domainId)
      },
      cptMany2One: function(){
        return this.relType===FIELD.MANY_TO_ONE
      }
    },
    methods:{
      /** 弹窗操作 */
      onRecordAdd(){
        let that = this
        let slotRef = that.$refs.slotAdd
        that.popAddVisible = slotRef.isPage()
        slotRef.showPop({'fieldName': tools.randomString(8)}, {},
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
      onRelationAdd() {
        let that = this
        let slotRef = that.$refs.slotRelation
        that.popRelationVisible = slotRef.isPage()
        slotRef.showPop({fieldName: tools.randomString(8), relType: that.relType }, {},
          (record, context) => { /* submit */
            record['fieldType'] = record['relType']
            if(record['relType'] === FIELD.MANY_TO_ONE){
              /** 多对一：记录关联domainId、domainName */
              let relMetaDomain = MetaDomain.find(record['relDomainOrFieldId'])
              if(!relMetaDomain){ return }
              record['extendJson'] = JSON.stringify({
                [OTHER.REL_DOMAIN_ID]: relMetaDomain['id'],                     // 关联的参数1
                [OTHER.REL_DOMAIN_NAME]: relMetaDomain['domainName'],           // 关联的参数2
              })
            }else{
              /** 一对多：记录关联domainId、domainName、fieldId、fieldName */
              let relMetaDomainField = MetaDomainField.find(record['relDomainOrFieldId'])
              let relMetaDomain = MetaDomain.find(relMetaDomainField?.metaDomainId)
              if(!relMetaDomainField || !relMetaDomain){ return }
              record['extendJson'] = JSON.stringify({
                [OTHER.REL_DOMAIN_ID]: relMetaDomain['id'],                     // 关联的参数1
                [OTHER.REL_DOMAIN_NAME]: relMetaDomain['domainName'],           // 关联的参数2
                [OTHER.REL_DOMAIN_FIELD_ID]: relMetaDomainField['id'],          // 关联的参数3
                [OTHER.REL_DOMAIN_FIELD_NAME]: relMetaDomainField['fieldName'], // 关联的参数4
              })
            }
            let slotTable = that.$refs.slotTable
            let promise = slotTable.doRecordSave(record, context)
            promise.then(() => that.popRelationVisible = false)
            return promise
          }, () => { /* cancel */
            that.popRelationVisible = false
          }
        )
      },
      onConstraintEdit(record) {
        let that = this
        let slotRef = that.$refs.slotConstraint
        that.popConstraintVisible = slotRef.isPage()
        slotRef.showPop(record, {},
          (record, context) => { /* submit */
            context || true
            // 更新字段约束配置
            let promise = MetaDomainField.$update({
              params: {
                id: record.id
              },
              data: {
                extendJson: converters.encodeObj(record.extendJson),
                constraintJson: converters.encodeObj(record.constraintJson)
              }
            })
            promise.then(() => {
              that.popConstraintVisible = false
            })
            return promise
          }, () => { /* cancel */
            that.popConstraintVisible = false
          }
        )
      },
      /** 扩展操作 */
      changeRelType(relType, form){
        this.relType = relType
        form.setFieldsValue({
          relType: relType,
          relDomainOrFieldId: undefined
        })
      },
      onGoBack(){
        this.$router.go(-1)
      },
      onRefresh(){
        this.$message.success('刷新成功');
      },
      /** 工具方法 */
      _copyText(copyText, e){
        let that = this
        let target = e.target
        if(e.target.tagName === 'svg'){
          target = e.target.parentElement
        }
        let clipboard = new Clipboard(target, {
          text: () => copyText
        })
        clipboard.on('success', () => {
          that.$message.success(`拷贝成功`);
          clipboard.destroy()
        })
        clipboard.on('error', () => {
          clipboard.destroy()
        })
        clipboard.onClick(e)
      },
      _routeEntity(){
        this.domainId = this.$route.params.domainId
      }
    },
    activated(){
      this._routeEntity()
    },
    mounted() {
      this._routeEntity()
    },
    created() {
      this.debug = this.$route.query.debug
    },
  }
</script>
