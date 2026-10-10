<template>
  <!-- 属性说明
  is:         modal  drawer  card
  title:      modal  drawer  card
  bodyStyle:  modal  drawer  card
  visible:    modal  drawer
  width:      modal  drawer
  bordered:   card
  @ok:        modal
  @cancel:    modal
  @close:     drawer
  -->
  <!-- 添加div解决Modal转Page的bug -->
  <div v-if="cptVisible">
    <component
      :is="cptType"
      :title="undefined"
      :dialogStyle="cptDialogStyle"
      :bodyStyle="cptBodyStyle"
      :visible="visible"
      :width="cptPopWidth"
      :closable="false"
      @ok="onSubmit"
      @cancel="doClose"
      @close="doClose"
      :bordered="false"
      :class="cptTypePage&&'page'"
      :data-cptFieldItems="cptFieldItems">
      <template v-if="cptTypePage" slot="title">
        <a @click="doClose"><a-icon type="left-circle"/></a>
        {{ cptTitle }}
      </template>
      <div v-else class="header-title" slot="title">
        {{ cptTitle }}
        <div class="item-right">
          <a-icon
            class="icon-action"
            style="margin-right: 10px;"
            :type="fullScreen?'g-fullscreen-exit':'g-fullscreen'"
            @click="toggleFullScreen" />
          <a-icon class="icon-action" type="close" @click="doClose" />
        </div>
      </div>
      <component :is="cptMultitab?'a-tabs':'div'" :tabBarGutter="2" v-model="activeTabKey">
        <!-- 基本信息(App模式) -->
        <component :is="cptMultitab?'a-tab-pane':'div'" key="tab_00">
          <span v-if="cptMultitab" slot="tab">
            <a-icon type="bars"/>基本
          </span>
          <a-form :form="form" :class="cptVertical?'pop-vform':'pop-form'" @submit.prevent="onSubmit">
            <transition-group name="collapse" tag="a-row">
              <a-col
                v-for="item in cptFieldItems"
                :key="item.id || index"
                :md="_.get(item, 'extend.cols') || 24"
                :sm="24"
                :xs="24"
                v-show="!_.defaultTo(_.get(collapsedState, item.panelIdent), false)"
                :class="{'drag-item': true}">
                <!-- Item组件(App模式) -->
                <g-form-item
                  v-if="item.isActived?item.isActived(record, context):true"
                  :labelCol="cptLabelCol"
                  :wrapperCol="cptWrapperCol"
                  :item="item"
                  :record="record"
                  :context="context"
                  :sContext="sContext"
                  @collapsePanel="vPopUtil.collapsePanel(_self, item, $event)">
                </g-form-item>
              </a-col>
            </transition-group>
            <!-- 操作按钮(App模式) -->
            <!-- <template v-if="cptTypeDrawer || cptTypePage">
              <a-form-item :wrapper-col="cptWrapperCol" class="btn-form-item">
                <span slot="label"></span>
                <a-button v-if="cptSubmitBtn && _editEnabled()" :loading="submitLoading" style="margin-right: 8px" type="primary" htmlType="submit">
                  {{ cptSubmitText }}
                </a-button>
                <a-button v-if="cptCancelBtn" @click="doClose">{{ cptCancelText }}</a-button>
              </a-form-item>
            </template>-->
          </a-form>
        </component>

        <!-- 附件&评论&关联(App模式) -->
        <a-tab-pane
          v-if="cptMultitab"
          v-for="(item) in cptTabItems"
          :key="`tab_${item.id}`">
          <!-- Tab头部 -->
          <span slot="tab">
            <a-icon :type="_tabItemIcon(item)"/> {{ item.title }}
          </span>
          <!-- Tab内容(App模式) -->
          <template v-if="`tab_${item.id}`===activeTabKey && record.id">
            <component
              v-if="['GPartComment', 'GPartAttachment', 'GPartActivity', 'GPartChangeLog'].indexOf(item.component)>-1"
              :key="record.id"
              :is="item.component"
              :configObj="{
                recordId: record.id,
                domainId: _.get(sDomain, 'id'),
                domainName: tools.toLowerCamel(_.get(sDomain, 'domainName')),
              }">
            </component>
            <AppPartRelations
              v-else-if="COMPO.PART_RELATIONS===item.component && record.id"
              :key="item.id"
              :sModel="item"
              :record="record">
            </AppPartRelations>
          </template>
        </a-tab-pane>
      </component>

      <!-- 操作按钮(App模式) -->
      <template v-if="cptTypeModal" slot="footer">
        <a-button v-if="cptCancelBtn" style="margin-right: 8px" @click="doClose">{{ cptCancelText }}</a-button>
        <a-button v-if="cptSubmitBtn && _editEnabled()" :loading="submitLoading" type="primary" @click="onSubmit">
          {{ cptSubmitText }}
        </a-button>
      </template>
      <div
        v-if="(cptTypeDrawer || cptTypePage) && activeTabKey==='tab_00'"
        class="pop-bottom-wrapper">
        <a-button v-if="cptSubmitBtn && _editEnabled()" :loading="submitLoading" type="primary" @click="onSubmit">
          {{ cptSubmitText }}
        </a-button>
        <a-button v-if="cptCancelBtn" @click="doClose" :style="{ marginLeft: '8px' }">{{ cptCancelText }}</a-button>
      </div>
    </component>
  </div>
</template>

<!--
# 组件说明
  App表单弹窗
# 配置组件
  sModel: Object        // 当前组件SModel
  sContext: Object      // 上下文sContext
  popVisible: Boolean   // 弹窗是否可见
  configObj: Object     // 配置对象： page, modal, layout, popWidth, bodyStyle, labelCol, wrapperCol,
                        //          title, submitBtn, submitText, cancelBtn, cancelText, multiTab, initValues
# 操作方法
  isPage()
  showPop (record, context, submitCallback, cancelCallback)
-->

<script>
  import ADrawer from 'ant-design-vue/lib/drawer';
  import ATabs from 'ant-design-vue/lib/tabs';

  import GPartComment from '@/components/page_part/GPartComment'
  import GPartAttachment from '@/components/page_part/GPartAttachment'
  import GPartActivity from '@/components/page_part/GPartActivity'
  import AppPartRelations from '@/components/tmpl_part/AppPartRelations'
  import GPartChangeLog from '@/components/page_part/GPartChangeLog'

  import MetaDomain from '@/store/models/MetaDomain'
  import MetaPageComponent from '@/store/models/MetaPageComponent'
  import MetaPageComponentField from '@/store/models/MetaPageComponentField'

  import {COMPO, FIELD, OTHER, DATE} from '@/constants'
  import { vPopUtil } from '@/utilsv/vUtil'
  import { stores, tools, converters } from '@/utils/util'

  export default {
    name: "AppFormPop",
    components: {
      ADrawer,
      ATabs,
      ATabPane: ATabs.TabPane,
      GPartComment,
      GPartAttachment,
      GPartActivity,
      AppPartRelations,
      GPartChangeLog,
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
      },
      popVisible: {
        type: Boolean,
        default: false,
      },
      configObj: {
        type: Object,
        required: false,
        default: function(){
          return {}
        }
      },
    },
    data () {
      return {
        /** 配置&状态 */
        COMPO: COMPO,
        tools: tools,
        vPopUtil: vPopUtil,
        configObj: {},
        form: this.$form.createForm(this),
        visible: false,
        fullScreen: false,
        submitLoading:false,
        collapsedState: {},
        activeTabKey: 'tab_00',
        /** 数据部分 */
        record: {},
        context: {},
      }
    },
    computed: {
      sIdent: function(){
        return this._.defaultTo(this.sModel?.ident, '0_0_0')
      },
      sDomain: function(){
        return MetaDomain.find(this.sModel?.metaDomainId)
      },
      cptFieldItems: function(){
        let that = this
        let metaFields = MetaPageComponentField.query().where('metaPageComponentId', that.sModel?.id)
          .orderBy('seqOrder', 'asc').orderBy('id', 'asc').with('relMetaDomainField').get()
        let cloneItemFunc = vPopUtil.popCloneItem
        let panelItem = null
        // 特殊处理，读取表格配置，是否允许编辑
        let editEnabled = that._.defaultTo(that._.get(that.context, OTHER.EDIT_ENABLED), true)
        return metaFields.map(item => {
          let clonedItem = cloneItemFunc(that, item)
          clonedItem['change'] = (record, context, value, form, valueObj) => {
            that._fieldItemChange(clonedItem, value, valueObj)
          }
          if(item['component']===COMPO.G_PANEL){
            clonedItem['pnlIdent'] = item['id']
            panelItem = item
          }else if(panelItem){
            clonedItem['panelIdent'] = panelItem['id']
          }
          let required = that._.get(clonedItem, 'extend.required')
          clonedItem['options'] = {
            rules: [{ required: !!required, message: `${item['title']}为必填项` }]
          }
          if(!editEnabled){
            that._.set(clonedItem, 'extend.readonly', true)
          }
          return clonedItem
        })
      },
      cptTabItems: function(){
        let that = this
        let tabItems = MetaPageComponent.query().where('pIdent', this.sIdent)
          .where('type', OTHER.PART_TAB).orderBy('seqOrder', 'asc').orderBy('id', 'asc').get()
        return tabItems.map(item => {
          let cfExtend = converters.parseObj(item['extend'])
          let domainIdKey = tools.toLowerCamel(that._.get(that.sDomain, 'domainName') + FIELD.SUFFIX_ID)
          let mainContext = {
            [domainIdKey]: that.record.id
          }
          let dfExtend = {}
          let dfToExtend = {}
          return {
            id: item.id,
            title: item.title,
            ident: item.ident,
            type: item.type,
            component: item.component,
            seqOrder: item.seqOrder,
            extend: Object.assign(dfToExtend, cfExtend, dfExtend),
            // ----------- //
            metaDomainId: item.metaDomainId,
            cfExtend: cfExtend,
            dfExtend: dfExtend,
            mainContext: mainContext,
          }
        })
      },
      /** Cpt选项 */
      cptVisible: function(){
        let that = this
        if(that.cptTypePage){
          return that.popVisible
        }else{
          return true
        }
      },
      cptType: function(){
        let that = this
        let isMobile = stores.isMobile()
        let isPage = that.configObj['page']
        let isModal = that.configObj['modal']
        let popWidth = that.configObj['popWidth']
        let isNarrowDrawer = popWidth && popWidth<=380
        return isPage?'ACard':isModal?'AModal':(isMobile && !isNarrowDrawer)?'AModal':'ADrawer'
      },
      cptTypePage: function(){
        return this.cptType === 'ACard'
      },
      cptTypeModal: function(){
        return this.cptType === 'AModal'
      },
      cptTypeDrawer: function(){
        return this.cptType === 'ADrawer'
      },
      cptTitle: function(){
        let that = this
        return that._.get(that.context, '_title') || that.configObj.title || '表单弹窗'
      },
      cptDialogStyle: function(){
        let that = this
        let dialogStyle = {}
        if(that.cptTypeModal && that.fullScreen){
          dialogStyle = {
            top: 0,
            paddingBottom: 0,
            maxWidth: '100vw',
          }
        }
        return dialogStyle
      },
      cptBodyStyle: function(){
        let that = this
        let bodyStyle = that.configObj.bodyStyle || {}
        if(that.cptTypeModal){
          if(that.fullScreen){
            bodyStyle = Object.assign(bodyStyle, {
              height: 'calc(100vh - 55px - 53px)',
              overflowY: 'auto',
            })
          }else{
            bodyStyle = Object.assign(bodyStyle, {
              maxHeight: 'calc(100vh - 55px - 53px - 130px)',
              overflowY: 'auto',
            })
          }
        }else if(that.cptTypeDrawer && that.activeTabKey==='tab_00'){
          bodyStyle = Object.assign(bodyStyle, {
            height: 'calc(100vh - 55px - 50px)',
            overflowY: 'auto',
          })
        }
        return bodyStyle
      },
      cptMultitab: function(){
        let that = this
        let configMultiTab = that._.defaultTo(that.configObj.multiTab, false)
        return configMultiTab && that.cptTabItems.length>0
      },
      cptSubmitBtn: function(){
        return this._.defaultTo(this.configObj.submitBtn, true)
      },
      cptSubmitText: function(){
        return this._.defaultTo(this.configObj.submitText, '确认')
      },
      cptCancelBtn: function(){
        return this._.defaultTo(this.configObj.cancelBtn, true)
      },
      cptCancelText: function(){
        return this._.defaultTo(this.configObj.cancelText, '取消')
      },
      cptPopWidth: function(){
        let that = this
        let popWidth = that._.defaultTo(this.configObj.popWidth, 700)
        if(that.fullScreen){
          if(that.cptTypeModal){
            popWidth = '100%'
          }else if(that.cptTypeDrawer){
            popWidth = '100%'
          }
        }
        return popWidth
      },
      cptVertical: function(){
        return this.configObj.layout === 'vertical'
      },
      cptLabelCol: function(){
        return this.configObj.labelCol || this.cptVertical?{}:{
          xs: { span: 24 },
          sm: { span: 5 }
        }
      },
      cptWrapperCol: function(){
        return this.configObj.wrapperCol || this.cptVertical?{}:{
          xs: { span: 24 },
          sm: { span: 15 }
        }
      },
    },
    methods: {
      /** 弹窗操作 */
      isPage(){
        return this.cptTypePage
      },
      showPop (record, context, submitCallback, cancelCallback) {
        let that = this
        that.form.resetFields()
        that.record = tools.flattenObj(record)
        that.context = context || {}
        that.submitCallback = submitCallback
        that.cancelCallback = cancelCallback
        that.visible = true
        that.$nextTick(() => {
          let values = {}
          if(that.configObj.initValues){
            values = that.configObj.initValues()
          }
          that.cptFieldItems.forEach( item => {
            if([COMPO.G_PANEL, COMPO.G_SLOT, COMPO.G_LOCK].indexOf(item.component)!==-1){ return }
            let fieldValue = item.getValue?item.getValue(that.record, that.context):
              that._.isNil(that.record[item.field])?values[item.field]:that.record[item.field]
            fieldValue = that._.isNil(fieldValue)?undefined:fieldValue
            if(COMPO.G_DATE_PICKER === item.component){
              let dateFormat = that._.get(item, 'extend.format') || DATE.YYYYMMDD
              values[item.field] = fieldValue?that.$moment(fieldValue, dateFormat):null
            }else if(COMPO.G_TIME_PICKER === item.component){
              values[item.field] = fieldValue?that.$moment(fieldValue, 'HH:mm:ss'):null
            }else{
              values[item.field] = fieldValue
            }
          })
          // Warning: Register field before render
          that._.forIn(values, (value, key)=>{ that.form.getFieldDecorator(key, {}) })
          that.form.setFieldsValue(values)
        })
      },
      onSubmit () {
        this.form.validateFields((errors, values) => {
          let that = this
          if (!errors) {
            let recordId = that._.get(that, 'record.id')
            let record = {id: recordId}
            that.cptFieldItems.forEach(item => {
              if([COMPO.G_PANEL, COMPO.G_SLOT, COMPO.G_LOCK].indexOf(item.component)!==-1){ return }
              let fieldValue = values[item.field]
              fieldValue = that._.isNil(fieldValue)?undefined:fieldValue
              if(item.setValue) {
                item.setValue(record, fieldValue)
              }else if([COMPO.G_INPUT, COMPO.G_TEXTAREA].indexOf(item.component) !== -1) {
                record[item.field] = that._.isEmpty(fieldValue)?null:that._.trim(fieldValue)
              }else if(COMPO.G_DATE_PICKER === item.component){
                let dateFormat = that._.get(item, 'extend.format') || DATE.YYYYMMDD
                record[item.field] = fieldValue?fieldValue.format(dateFormat):null
              }else if(COMPO.G_TIME_PICKER === item.component){
                record[item.field] = fieldValue?fieldValue.format('HH:mm:ss'):null
              }else{
                record[item.field] = fieldValue
              }
            })
            let activitiesField = FIELD.V_ACTIVITIES.toLowerCase()
            if(recordId && values[activitiesField]){
              record[activitiesField] = values[activitiesField]
            }
            let attachmentsField = FIELD.V_ATTACHMENTS.toLowerCase()
            if(recordId && values[attachmentsField]){
              record[attachmentsField] = values[attachmentsField]
            }
            if(that.submitCallback){
              let promise = that.submitCallback(record, that.context)
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
      },
      toggleFullScreen(){
        let that = this
        that.fullScreen = !that.fullScreen
      },
      /** 工具方法 */
      _fieldItemChange(item, value, valueObj){
        let that = this
        if(item.type === FIELD.MANY_TO_ONE){
          let domainIdKey = tools.toLowerCamel(that._.get(item, 'dfExtend.relDomainName') + FIELD.SUFFIX_ID)
          that.$set(that.context, domainIdKey, value)
        }
      },
      _tabItemIcon(item){
        let that = this
        let iconMapper = {
          'GPartComment': 'file-text',
          'GPartAttachment': 'paper-clip',
          'GPartActivity': 'clock-circle',
          'GPartChangeLog': 'history',
        }
        return that._.get(iconMapper, item.component, 'link')
      },
      _editEnabled(){
        let that = this
        return that._.defaultTo(that._.get(that.context, OTHER.EDIT_ENABLED), true)
      },
    },
  }
</script>

<style lang="scss" scoped>
  .drag-item{
  }
</style>
