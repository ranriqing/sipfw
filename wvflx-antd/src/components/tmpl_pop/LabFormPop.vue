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
        <a-popover v-if="stores.isLabState()" placement="bottomLeft" overlayClassName="lab-popover">
          <a-icon type="bug" class="bug-icon" />
          <div slot="title" class="flex flex-row main-between">
            <span>{{ _.get(sModel, 'type', '') }}({{ _.get(sModel, 'title', '') }})</span>
            <a @click="vPageUtil.handlePartCopy(_self, sModel, {})"><a-icon type="copy"/></a>
          </div>
          <template slot="content">
            <div class="lab-item">
              <div class="title">Ident</div>
              <div class="content"><a @click="copyText(_.get(sModel, 'ident'), $event)">{{ _.get(sModel, 'ident') }}</a></div>
            </div>
            <div class="lab-item">
              <div class="title">Type</div>
              <div class="content">{{ _.get(sModel, 'type') }}</div>
            </div>
            <div class="lab-item">
              <div class="title">Comp</div>
              <div class="content">{{ _.get(sModel, 'component') }}</div>
            </div>
          </template>
        </a-popover>
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
          <a-form :form="form" :class="['tmpl-block', cptVertical?'pop-vform':'pop-form']" @submit.prevent="onSubmit">
            <!-- 拖拽目的(Lab模式) -->
            <draggable
              tag="a-row"
              class="draggable-container"
              :list="datFieldItems"
              v-bind="vPopUtil.cptDragTargetOptions()"
              @change="(changeEvent) => vPopUtil.handleItemDrag(_self, changeEvent)">
              <a-col
                v-for="(item, index) in datFieldItems"
                :key="item.id || index"
                :md="_.get(item, 'extend.cols') || 24"
                :sm="24"
                :xs="24"
                v-show="!_.defaultTo(_.get(collapsedState, item.panelIdent), false)"
                :class="{'drag-item': true, active: activeItem.id === item.id}"
                @click="vPopUtil.handleItemSelect(_self, item, $event)">
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
                <!-- Item操作(Lab模式) -->
                <template v-if="activeItem.id === item.id">
                  <div class="drag-item-action">
                    <a-popover v-if="stores.isLabState()" :title="'FIELD(' + item.title + ')'" placement="bottomLeft" overlayClassName="lab-popover">
                      <a-icon type="bug" class="bug-icon" />
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
                          <div class="content">{{ item.component }}</div>
                        </div>
                      </template>
                    </a-popover>
                    <a-divider type="vertical" style="margin: 0 4px;" />
                    <a-icon type="edit" @click="vPopUtil.handleItemEdit(_self, item)"/>
                    <a-divider type="vertical" style="margin: 0 4px;" />
                    <a-popconfirm title="确定删除字段?" @confirm="() => vPopUtil.handleItemDelete(_self, item)">
                      <a-icon type="delete" />
                    </a-popconfirm>
                  </div>
                  <div class="drag-item-widget">
                    <a-icon type="g-drag" />
                  </div>
                </template>
              </a-col>
            </draggable>
            <div v-if="cptTypeModal" class="pop-operation-buttons">
              <div class="config-wrapper">
                <a-button size="small" @click="handleSaveDefaultValues">
                  <a-icon type="save"/>保存默认值
                </a-button>
                <a-button size="small" @click="vPopUtil.handleColsConfig(_self)">
                  <a-icon type="setting"/>弹窗列
                </a-button>
              </div>
            </div>

            <!-- 操作按钮(App模式) -->
            <!-- <template v-if="cptTypeDrawer || cptTypePage">
              <a-divider></a-divider>
              <a-form-item
                :label-col="{}"
                :wrapper-col="{}">
                <div class="pop-operation-buttons">
                  <a-button v-if="cptSubmitBtn && _editEnabled()" :loading="submitLoading" style="margin-right: 8px" type="primary" htmlType="submit">
                    {{ cptSubmitText }}
                  </a-button>
                  <a-button v-if="cptCancelBtn" @click="doClose">{{ cptCancelText }}</a-button>
                  <div class="config-wrapper">
                    &lt;!&ndash; 弹窗列 &ndash;&gt;
                    <a-button size="small" @click="vPopUtil.handleColsConfig(_self)">
                      <a-icon type="setting"/>弹窗列
                    </a-button>
                  </div>
                </div>
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
            <LabPartRelations
              v-else-if="COMPO.PART_RELATIONS===item.component && record.id"
              :key="item.id"
              :sModel="item"
              :record="record">
            </LabPartRelations>
          </template>
        </a-tab-pane>

        <!-- 配置Tab(Lab模式) -->
        <div v-if="cptMultitab" slot="tabBarExtraContent" style="display: inline-block; margin-right: 8px;">
          <!-- 非基本，可以添加 -->
          <template v-if="activeTabKey!=='tab_00'">
            <!-- 非基本，可以编辑 -->
            <a @click="handleTabEdit"><a-icon type="edit"/></a>
            <a-divider type="vertical" />
            <!-- 非首个，可以往左 -->
            <template v-if="`tab_${_.get(cptTabItems[0], 'id')}`!==activeTabKey">
              <a @click="handleTabMove('left')"><a-icon type="left"/></a>
              <a-divider type="vertical" />
            </template>
            <!-- 非尾个，可以往右 -->
            <template v-if="`tab_${_.get(cptTabItems[cptTabItems.length-1], 'id')}`!==activeTabKey">
              <a @click="handleTabMove('right')"><a-icon type="right"/></a>
              <a-divider type="vertical" />
            </template>
            <!-- 非基本，可以删除 -->
            <a-popconfirm title="确定删除吗?" @confirm="handleTabDelete">
              <a><a-icon type="close"/></a>
            </a-popconfirm>
            <a-divider type="vertical" />
          </template>
          <!-- 全部，可以添加 -->
          <a @click="handleTabAdd"><a-icon type="plus"/></a>
        </div>
      </component>

      <!-- 操作按钮(App模式) -->
      <template v-if="cptTypeModal" slot="footer">
        <a-button v-if="cptCancelBtn" style="margin-right: 8px" @click="doClose">{{ cptCancelText }}</a-button>
        <a-button v-if="cptSubmitBtn && _editEnabled()" :loading="submitLoading" type="primary" @click="onSubmit">
          {{ cptSubmitText }}
        </a-button>
      </template>
      <div v-if="(cptTypeDrawer || cptTypePage) && activeTabKey==='tab_00'">
        <div class="pop-bottom-wrapper pop-operation-buttons">
          <a-button v-if="cptSubmitBtn && _editEnabled()" :loading="submitLoading" type="primary" @click="onSubmit">
            {{ cptSubmitText }}
          </a-button>
          <a-button v-if="cptCancelBtn" @click="doClose" :style="{ marginLeft: '8px' }">{{ cptCancelText }}</a-button>
          <div class="config-wrapper">
            <a-button size="small" @click="handleSaveDefaultValues">
              <a-icon type="save"/>保存默认值
            </a-button>
            <a-button size="small" @click="vPopUtil.handleColsConfig(_self)">
              <a-icon type="setting"/>弹窗列
            </a-button>
          </div>
        </div>
      </div>

      <!-- Ref弹窗(Lab模式) -->
      <GFormPop
        ref="refTabAdd"
        :configObj="tabPopConfig.addConfig">
      </GFormPop>
      <GFormPop
        ref="refTabEdit"
        :configObj="tabPopConfig.editConfig">
      </GFormPop>
      <GColsPop
        ref="refCols"
        :configObj="colsPopConfig">
      </GColsPop>
      <GFormPop
        ref="refMeta"
        :configObj="metaPopConfig">
      </GFormPop>
    </component>
  </div>
</template>

<!--
# 组件说明
  Lab表单弹窗
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

  import GColsPop from '@/components/page_pop/GColsPop'
  import GFormPop from '@/components/page_pop/GFormPop'
  import GPartComment from '@/components/page_part/GPartComment'
  import GPartAttachment from '@/components/page_part/GPartAttachment'
  import GPartActivity from '@/components/page_part/GPartActivity'
  import LabPartRelations from '@/components/tmpl_part/LabPartRelations'
  import GPartChangeLog from '@/components/page_part/GPartChangeLog'

  import MetaDomain from '@/store/models/MetaDomain'
  import MetaPageComponent from '@/store/models/MetaPageComponent'
  import MetaPageComponentField from '@/store/models/MetaPageComponentField'

  import {COMPO, FIELD, OTHER, DATE} from '@/constants'
  import { ormSeqOrders } from '@/api/api'
  import { vPopUtil, vPageUtil } from '@/utilsv/vUtil'
  import { stores, tools, converters } from '@/utils/util'

  import labPopColsPopConfig from './configs/labPopColsPopConfig'
  import labPopTabPopConfig from './configs/labPopTabPopConfig'
  import labPopMetaPopConfig from './configs/labPopMetaPopConfig'

  export default {
    name: "LabFormPop",
    components: {
      ADrawer,
      ATabs,
      ATabPane: ATabs.TabPane,
      GColsPop,
      GFormPop,
      GPartComment,
      GPartAttachment,
      GPartActivity,
      LabPartRelations,
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
        stores: stores,
        vPopUtil: vPopUtil,
        vPageUtil: vPageUtil,
        colsPopConfig: labPopColsPopConfig,
        tabPopConfig: labPopTabPopConfig,
        metaPopConfig: labPopMetaPopConfig,
        configObj: {},
        form: this.$form.createForm(this),
        visible: false,
        fullScreen: false,
        submitLoading:false,
        activeItem: {},
        collapsedState: {},
        activeTabKey: 'tab_00',
        /** 数据部分 */
        record: {},
        context: {},
        datFieldItems: [],
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
        that.datFieldItems = metaFields.map(item => {
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
        return metaFields
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
            realItem: item,
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
        return configMultiTab // && that.cptTabItems.length>0
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
    inject: ['copyText'],
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
          let cfExtend = converters.parseObj(that.sModel.extend) || {}
          if(cfExtend.defaultValues){
            values = { ...cfExtend.defaultValues }
          }
          if(that.configObj.initValues){
            that._.assign(values, that.configObj.initValues())
          }
          that.datFieldItems.forEach( item => {
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
            that.datFieldItems.forEach(item => {
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
      /** Handle操作 */
      handleTabAdd(){
        let that = this
        let slotRef = that.$refs.refTabAdd
        slotRef.showPop({}, {
            recordId: that.record.id,
            domainId: _.get(that.sDomain, 'id'),
            domainName: tools.toLowerCamel(_.get(that.sDomain, 'domainName')),
          },
          (item, context) => { /* submit */
            return that.handleTabSave(item, context)
          }, () => { /* cancel */ }
        )
      },
      handleTabSave(item){
        let that = this
        return MetaPageComponent.$create({
          data: {
            title: item.title,
            pIdent: that.sIdent,
            ident: `${that.sIdent}_PTAB${tools.randomString(6)}`,
            type: 'PART_TAB',
            component: item.component,
            seqOrder: item.seqOrder,
            metaDomainId: item.metaDomainId,
            extend: converters.encodeObj(item.extend),
          }
        })
      },
      handleTabEdit(){
        let that = this
        let item = that.cptTabItems.find(tabItem => {
          return `tab_${tabItem['id']}`===that.activeTabKey
        })
        if(item){
          let slotRef = that.$refs.refTabEdit
          slotRef.showPop(item, {},
            (item, context) => { /* submit */
              return that.handleTabUpdate(item, context)
            }, () => { /* cancel */ }
          )
        }
      },
      handleTabUpdate(item){
        return MetaPageComponent.$update({
          params: {
            id: item.id,
          },
          data: {
            title: item.title,
            remark: item.remark,
            extend: converters.encodeObj(item.cfExtend),
          }
        })
      },
      handleTabMove(direction){
        let that = this
        // 获取Item
        let mappedItems = that.cptTabItems.map(item => {
          return {
            id: item.id,
            seqOrder: item.seqOrder
          }
        })
        let moveIndex = mappedItems.findIndex(tabItem => {
          return `tab_${tabItem['id']}`===that.activeTabKey
        })
        // 移动Item
        if(moveIndex>=0){
          let moveItem = mappedItems[moveIndex]
          if(direction === 'left' && moveIndex>0){
            mappedItems.splice(moveIndex, 1)
            mappedItems.splice(moveIndex-1, 0, moveItem)
          }else if(direction === 'right'){
            mappedItems.splice(moveIndex, 1)
            mappedItems.splice(moveIndex+1, 0, moveItem)
          }
          let itemOrders = []
          mappedItems.forEach((item, index) => {
            let itemData = {id: item.id, seqOrder: index+1}
            itemOrders.push(itemData)
          })
          ormSeqOrders('metaPageComponent', {items: itemOrders}).then(resp => {
            let entitys = resp.gmodel
            MetaPageComponent.insertOrUpdate({
              data: entitys
            })
          })
        }
      },
      handleTabDelete(){
        let that = this
        let item = that.cptTabItems.find(tabItem => {
          return `tab_${tabItem['id']}`===that.activeTabKey
        })
        if(item){
          MetaPageComponent.$delete({
            params: {
              id: item.id
            }
          }).then(()=>{
            that.activeTabKey = 'tab_00'
          })
        }
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
      handleSaveDefaultValues(){
        let that = this
        let values = that.form.getFieldsValue()
        let defaultValues = {}
        that.datFieldItems.forEach(item => {
          if([COMPO.G_PANEL, COMPO.G_SLOT, COMPO.G_LOCK].indexOf(item.component) !== -1){ return }
          let fieldValue = values[item.field]
          if(!that._.isNil(fieldValue)){
            if(COMPO.G_DATE_PICKER === item.component){
              let format = that._.get(item, 'extend.format') || DATE.YYYYMMDD
              defaultValues[item.field] = fieldValue.format(format)
            }else if(COMPO.G_TIME_PICKER === item.component){
              defaultValues[item.field] = fieldValue.format('HH:mm:ss')
            }else{
              defaultValues[item.field] = fieldValue
            }
          }
        })
        let cfExtend = converters.parseObj(that.sModel.extend) || {}
        cfExtend.defaultValues = defaultValues
        MetaPageComponent.$update({
          params: { id: that.sModel.id },
          data: { extend: converters.encodeObj(cfExtend) }
        }).then(() => {
          that.sModel.extend = converters.encodeObj(cfExtend)
          that.$message.success('默认值已保存')
        }).catch(() => {
          that.$message.error('保存失败')
        })
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
  .drag-item{
    padding: 5px 0;
  }
</style>
