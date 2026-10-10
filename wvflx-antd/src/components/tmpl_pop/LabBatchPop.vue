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
      <!-- 基本信息(App模式) -->
      <div>
        <a-form :form="form" :class="['tmpl-block', cptVertical?'pop-vform':'pop-form']" @submit.prevent="onSubmit">
          <a-alert
            v-if="context['batchNum']"
            style="margin: 0 0 15px 0"
            :message="`温馨提示： 批量编辑 [${ context['batchNum'] }] 条记录`"
            type="warning"
            show-icon>
          </a-alert>
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
              <div :class="{flex:true, 'flex-row':!cptVertical, 'flex-column':cptVertical, 'cross-center': !cptVertical}">
                <a-switch
                  v-if="item.component!==COMPO.G_PANEL"
                  size="small"
                  style="width: 36px; margin-right: 6px;"
                  :checked="!!itemLockState[item.id]"
                  @change="onItemLock(item, $event)">
                  <a-icon type="check" slot="checkedChildren" />
                  <a-icon type="close" slot="unCheckedChildren" />
                </a-switch>
                <g-form-item
                  style="flex: 1;"
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
              </div>
            </a-col>
          </draggable>
          <div v-if="cptTypeModal" class="pop-operation-buttons">
            <div class="config-wrapper">
              <!-- 弹窗列 -->
              <a-button size="small" @click="vPopUtil.handleColsConfig(_self)">
                <a-icon type="setting"/>弹窗列
              </a-button>
            </div>
          </div>

          <!-- 操作按钮(App模式) -->
          <template v-if="cptTypeDrawer || cptTypePage">
            <a-divider></a-divider>
            <a-form-item
              :label-col="{}"
              :wrapper-col="{}">
              <div class="pop-operation-buttons">
                <a-button :loading="submitLoading" style="margin-right: 8px" type="primary" htmlType="submit">
                  {{ cptSubmitText }}
                </a-button>
                <a-button style="margin-right: 8px" type="danger" :loading="deleteLoading" @click="onBatchDelete">删除</a-button>
                <a-button v-if="cptCancelBtn" @click="doClose">{{ cptCancelText }}</a-button>
                <div class="config-wrapper">
                  <!-- 弹窗列 -->
                  <a-button size="small" @click="vPopUtil.handleColsConfig(_self)">
                    <a-icon type="setting"/>弹窗列
                  </a-button>
                </div>
              </div>
            </a-form-item>
          </template>
        </a-form>
      </div>

      <!-- 操作按钮(App模式) -->
      <template v-if="cptTypeModal" slot="footer">
        <div class="flex flex-row">
          <a-button type="danger" @click="onBatchDelete">批量删除</a-button>
          <div class="item-right">
            <a-button v-if="cptCancelBtn" style="margin-right: 8px" @click="doClose">{{ cptCancelText }}</a-button>
            <a-button v-if="cptSubmitBtn" :loading="submitLoading" type="primary" @click="onSubmit">
              {{ cptSubmitText }}
            </a-button>
          </div>
        </div>
      </template>

      <!-- Ref弹窗(Lab模式) -->
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
  Lab批量弹窗
# 配置组件
  sModel: Object        // 当前组件SModel
  sContext: Object      // 上下文sContext
  popVisible: Boolean   // 弹窗是否可见
  configObj: Object     // 配置对象： page, modal, layout, popWidth, bodyStyle, labelCol, wrapperCol,
                        //          title, submitBtn, submitText, cancelBtn, cancelText, initValues
# 操作方法
  isPage()
  showPop (record, context, submitCallback, cancelCallback)
-->

<script>
  import AAlert from 'ant-design-vue/lib/alert';
  import ADrawer from 'ant-design-vue/lib/drawer';

  import GColsPop from '@/components/page_pop/GColsPop'
  import GFormPop from '@/components/page_pop/GFormPop'

  import MetaDomain from '@/store/models/MetaDomain'
  import MetaPageComponentField from '@/store/models/MetaPageComponentField'

  import { COMPO, DATE } from '@/constants'
  import { vPopUtil, vPageUtil } from '@/utilsv/vUtil'
  import { stores, tools } from '@/utils/util'

  import labPopColsPopConfig from './configs/labPopColsPopConfig'
  import labPopMetaPopConfig from './configs/labPopMetaPopConfig'

  export default {
    name: "LabBatchPop",
    components: {
      AAlert,
      ADrawer,
      GColsPop,
      GFormPop,
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
        stores: stores,
        vPopUtil: vPopUtil,
        vPageUtil: vPageUtil,
        colsPopConfig: labPopColsPopConfig,
        metaPopConfig: labPopMetaPopConfig,
        configObj: {},
        form: this.$form.createForm(this),
        visible: false,
        fullScreen: false,
        submitLoading:false,
        deleteLoading:false,
        activeItem: {},
        collapsedState: {},
        /** 数据部分 */
        record: {},
        context: {},
        datFieldItems: [],
        itemLockState: {},
      }
    },
    computed: {
      sIdent: function(){
        return this._.defaultTo(this.sModel?.ident, '0_0_0')
      },
      cptFieldItems: function(){
        let that = this
        let metaFields = MetaPageComponentField.query().where('metaPageComponentId', that.sModel?.id)
          .orderBy('seqOrder', 'asc').orderBy('id', 'asc').with('relMetaDomainField').get()
        let cloneItemFunc = vPopUtil.popCloneItem
        let panelItem = null
        that.datFieldItems = metaFields.map(item => {
          let clonedItem = cloneItemFunc(that, item)
          if(item['component']===COMPO.G_PANEL){
            clonedItem['pnlIdent'] = item['id']
            panelItem = item
            return clonedItem
          }else if(panelItem){
            clonedItem['panelIdent'] = panelItem['id']
          }
          let required = that._.get(clonedItem, 'extend.required')
          clonedItem['options'] = {
            rules: [{ required: !!required, message: `${item['title']}为必填项` }]
          }
          let itemEnabled = that.itemLockState[item.id]
          clonedItem['component'] = itemEnabled?clonedItem['component']:COMPO.G_LOCK
          return clonedItem
        })
        return metaFields
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
        return that._.get(that.context, '_title') || that.configObj.title || '批量弹窗'
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
        if(that.cptTypeModal && that.fullScreen){
          bodyStyle = Object.assign(bodyStyle, {
            height: 'calc(100vh - 55px - 53px)',
            overflowY: 'auto',
          })
        }
        return bodyStyle
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
          if(that.configObj.initValues){
            values = that.configObj.initValues()
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
            let record = {}
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
      onBatchDelete(){
        let that = this
        that.$confirm({
          title: '确认操作',
          content: function(h) {
            h && true
            let vNodes = [];
            let attrs1 = { class: 'warning' }
            let attrs2 = { class: 'warning font-bold font-lg' }
            vNodes.push(<div>操作<span {...{ attrs: attrs1 }}>不可恢复</span>，请谨慎操作!</div>)
            vNodes.push(<div>确定删除<span {...{ attrs: attrs2 }}>&nbsp;{ that.context['batchNum'] }&nbsp;</span>条记录？</div>)
            return (
              <span>
                {vNodes}
              </span>
            )
          },
          onOk() {
            if(that.submitCallback){
              let promise = that.submitCallback({batchDelete: true}, that.context)
              if(promise && promise.then){
                that.deleteLoading = true
                promise.then(() => {
                  that.deleteLoading = false
                  that.doClose()
                }).catch(() => {
                  that.deleteLoading = false
                })
              }
            }
          },
          onCancel() {},
        });
      },
      doClose () {
        let that = this
        that.record = {}
        that.context = {}
        that.itemLockState = {}
        if(that.cancelCallback){
          that.cancelCallback()
        }
        that.visible = false
      },
      toggleFullScreen(){
        let that = this
        that.fullScreen = !that.fullScreen
      },
      /** 扩展操作 */
      onItemLock(item, checked){
        this.$set(this.itemLockState, item.id, checked)
      },
      /** 工具方法 */
      _fieldMetaDomain(item){
        return MetaDomain.find(this._.get(item, 'extend.relDomainId'))
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
