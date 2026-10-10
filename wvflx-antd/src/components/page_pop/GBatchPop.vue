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
  <component
    v-if="cptVisible"
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
    :class="cptTypePage&&'page'">
    <template v-if="cptTypePage" slot="title">
      <a @click="doClose"><a-icon type="left-circle"/></a>
      {{ cptTitle }}
    </template>
    <div v-else class="header-title" slot="title">
      <a-popover v-if="stores.isLabState()" placement="bottomLeft" overlayClassName="lab-popover">
        <a-icon type="bug" class="bug-icon" />
        <template slot="content">
          <div class="lab-item"><div class="title">Comp</div><div class="content">GBatchPop</div></div>
          <div class="lab-item"><div class="title">Title</div><div class="content">{{ configObj.title || '-' }}</div></div>
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
    <a-form :form="form" :class="cptVertical?'pop-vform':'pop-form'" @submit.prevent="onSubmit">
      <a-alert
        v-if="context['batchNum']"
        style="margin: 0 0 15px 0"
        :message="`温馨提示： 批量编辑 [${ context['batchNum'] }] 条记录`"
        type="warning"
        show-icon>
      </a-alert>
      <a-row :gutter="12">
        <a-col
          v-for="(item, index) in cptFieldItems"
          :key="index"
          :md="item.cols?item.cols:24"
          :sm="24"
          :xs="24"
          v-show="!_.defaultTo(_.get(collapsedState, item.panelIdent), false)">
          <div :class="{flex:true, 'flex-row':!cptVertical, 'flex-column':cptVertical, 'cross-center': !cptVertical}">
            <a-switch
              v-if="item.component!==COMPO.G_PANEL"
              size="small"
              style="width: 36px; margin-right: 6px;"
              :checked="!!itemLockState[item.field]"
              @change="onItemLock(item, $event)">
              <a-icon type="check" slot="checkedChildren" />
              <a-icon type="close" slot="unCheckedChildren" />
            </a-switch>
            <g-form-item
              style="flex: 1;"
              v-if="item.isActived?item.isActived(record, context):true"
              :labelCol="cptLabelCol"
              :wrapperCol="cptWrapperCol"
              :form="form"
              :item="item"
              :record="record"
              :context="context"
              :sContext="sContext"
              @collapsePanel="vPopUtil.collapsePanel(_self, item, $event)">
              <template
                v-if="item.component===COMPO.G_SLOT"
                :slot="item.field">
                <slot :name="item.field" :record="record" :item="item" :form="form"></slot>
              </template>
            </g-form-item>
          </div>
        </a-col>
      </a-row>
      <template v-if="cptTypeDrawer || cptTypePage">
        <a-form-item>
          <div class="flex flex-row">
            <a-button v-if="cptSubmitBtn" :loading="submitLoading" style="margin-right: 8px" type="primary" htmlType="submit">
              {{ cptSubmitText }}
            </a-button>
            <a-button style="margin-right: 8px" type="danger" :loading="deleteLoading" @click="onBatchDelete">删除</a-button>
            <a-button v-if="cptCancelBtn" @click="doClose">{{ cptCancelText }}</a-button>
          </div>
        </a-form-item>
      </template>
    </a-form>
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
  </component>
</template>


<!--
# 组件说明
  批量弹窗
# 配置组件
  configObj:{
      title: 弹窗标题
      formFields: [{ // 支持数组或formFieldsFunc(record, context)
        ... GFormItem
        component: String                  // 可为g-slot
        isActived: Function
        getValue:  Function
        setValue:  Function
        extend: {
          如：format: DATE.YYYYMMDD
        }
      }]
      submitBtn: Boolean类型，配置是否展示提交按钮
      submitText: 提交按钮展示的文本
      cancelBtn: Boolean类型，配置是否展示取消按钮
      cancelText: 取消按钮展示的文本
      popWidth: 弹窗宽度，默认700
      bodyStyle: 同a-modal的bodyStyle
      layout: Form条目展示方式，vertical为垂直模式，默认为水平模式
      labelCol: 仅水平模式有效，g-form-item的labelCol，同antd的labelCol
      wrapperCol: 仅水平模式有效，g-form-item的wrapperCol，同antd的wrapperCol
      initValues: Function初始化默认值
  }
  popVisible: Boolean
  sContext: Object
# 操作方法
  showPop (record, context, submitCallback, cancelCallback){ ... ... }
-->

<script>
  import AAlert from 'ant-design-vue/lib/alert';

  import { COMPO, DATE } from '@/constants'
  import { stores } from '@/utils/util'
  import { vPopUtil } from '@/utilsv/vUtil'

  export default {
    name: "GBatchPop",
    components: {
      AAlert,
    },
    props: {
      popVisible: { 
        type: Boolean,
        default: false,
      },
      configObj: {
        type: Object,
        default: function(){
          return {}
        }
      },
      sContext: {
        type: Object,
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
        form: this.$form.createForm(this),
        visible: false,
        fullScreen: false,
        submitLoading:false,
        deleteLoading:false,
        collapsedState: {},
        record: {},
        context: {},
        submitCallback: undefined,
        cancelCallback: undefined,
        /** 扩展数据 */
        itemLockState: {},
      }
    },
    computed: {
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
      cptFieldItems: function(){
        let that = this
        let formFields = that.configObj.formFields || []
        if(that._.isFunction(formFields)){
          let formFieldsFunc = formFields
          formFields = formFieldsFunc(that.record, that.context)
        }
        formFields = that._.cloneDeep(formFields)
        let panelItem = null
        formFields.forEach(item => {
          if(item['component']===COMPO.G_PANEL){
            item['pnlIdent'] = item['field']
            panelItem = item
            return
          }else if(panelItem){
            item['panelIdent'] = panelItem['field']
          }
          let itemEnabled = that.itemLockState[item.field]
          item['component'] = itemEnabled?item.component:COMPO.G_LOCK
        })
        return formFields
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
        that.record = that._.pickBy(record || {}, (value, key) =>
          !that._.isNil(value) && !key.startsWith('$') && !that._.isFunction(value)
        )
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
            let actived = item.isActived?item.isActived(that.record, that.context):true
            if(actived){
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
            }
          })
          // Warning: Register field before render
          that._.forIn(values, (value, key)=>{ that.form.getFieldDecorator(key, {}) })
          that.form.setFieldsValue(values)
        })
      },
      onSubmit () {
        let that = this
        that.form.validateFields((errors, values) => {
          if (!errors) {
            that.cptFieldItems.forEach( item => {
              if([COMPO.G_PANEL, COMPO.G_SLOT, COMPO.G_LOCK].indexOf(item.component)!==-1){ return }
              let actived = item.isActived?item.isActived(that.record, that.context):true
              if(actived){
                let fieldValue = values[item.field]
                fieldValue = that._.isNil(fieldValue)?undefined:fieldValue
                if(item.setValue) {
                  item.setValue(that.record, fieldValue)
                }else if([COMPO.G_INPUT, COMPO.G_TEXTAREA].indexOf(item.component) !== -1) {
                  that.record[item.field] = that._.isEmpty(fieldValue)?null:that._.trim(fieldValue)
                }else if(COMPO.G_DATE_PICKER === item.component){
                  let dateFormat = that._.get(item, 'extend.format') || DATE.YYYYMMDD
                  that.record[item.field] = fieldValue?fieldValue.format(dateFormat):null
                }else if(COMPO.G_TIME_PICKER === item.component){
                  that.record[item.field] = fieldValue?fieldValue.format('HH:mm:ss'):null
                }else{
                  that.record[item.field] = fieldValue
                }
              }
            })
            if(that.submitCallback){
              let promise = that.submitCallback(that.record, that.context)
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
        this.$set(this.itemLockState, item.field, checked)
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
</style>
