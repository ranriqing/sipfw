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
          <div class="lab-item"><div class="title">Comp</div><div class="content">{{ debugComp }}</div></div>
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
    <div class="pop-content">
      <slot :record="record" :context="context" :fullScreen="fullScreen"></slot>
    </div>
    <div v-if="cptTypeDrawer || cptTypePage" style="margin-top: 8px;">
      <a-button v-if="cptSubmitBtn" :loading="submitLoading" style="margin-right: 8px" type="primary" @click="onSubmit">
        {{ cptSubmitText }}
      </a-button>
      <a-button v-if="cptCancelBtn" @click="doClose">{{ cptCancelText }}</a-button>
    </div>
    <template v-if="cptTypeModal" slot="footer">
      <slot v-if="footer" name="footer" :record="record" :context="context"></slot>
      <template v-else>
        <i></i> <!-- 占位组件 -->
        <a-button v-if="cptCancelBtn || true" style="margin-right: 8px" @click="doClose">{{ cptCancelText }}</a-button>
        <a-button v-if="cptSubmitBtn" :loading="submitLoading" type="primary" @click="onSubmit">
          {{ cptSubmitText }}
        </a-button>
      </template>
    </template>
  </component>
</template>


<!--
# 组件说明
  表单弹窗
# 配置组件
  configObj:{
      title: 弹窗标题
      submitBtn: Boolean类型，配置是否展示提交按钮
      submitText: 提交按钮展示的文本
      cancelBtn: Boolean类型，配置是否展示取消按钮
      cancelText: 取消按钮展示的文本
      popWidth: 弹窗宽度，默认700
      bodyStyle: 同a-modal的bodyStyle
  }
  popVisible: Boolean
  sContext: Object
# 操作方法
  showPop (record, context, submitCallback, cancelCallback){ ... ... }
-->

<script>
import { stores } from '@/utils/util'
import { vPopUtil } from '@/utilsv/vUtil'

export default {
  name: "GPlainPop",
  components: {
  },
  props: {
    close: {
      type: Function,
      required: false,
      default: undefined,
    },
    popVisible: {
      type: Boolean,
      default: false,
    },
    submitEnabled: {
      type: Boolean,
      default: true,
    },
    configObj: {
      type: Object,
      default: function() {
        return {}
      }
    },
    sContext: {
      type: Object,
      default: function() {
        return {}
      }
    },
    /** 扩展插槽内容 */
    footer: {
      type: Function,
      required: false,
      default: undefined,
    },
    content: {
      type: Function,
      required: false,
      default: undefined,
    },
    debugComp: {
      type: String,
      default: 'GPlainPop',
    },
  },
  data() {
    return {
      /** 配置&状态 */
      stores: stores,
      vPopUtil: vPopUtil,
      visible: false,
      fullScreen: false,
      submitLoading: false,
      collapsedState: {},
      record: {},
      context: {},
      submitCallback: undefined,
      cancelCallback: undefined,
      contentComponent: undefined
    }
  },
  computed: {
    /** Cpt选项 */
    cptVisible: function() {
      let that = this
      if (that.cptTypePage) {
        return that.popVisible
      } else {
        return true
      }
    },
    cptType: function() {
      let that = this
      let isMobile = stores.isMobile()
      let isPage = that.configObj['page']
      let isModal = that.configObj['modal']
      return isPage ? 'ACard' : isModal ? 'AModal' : isMobile ? 'AModal' : 'ADrawer'
    },
    cptTypePage: function() {
      return this.cptType === 'ACard'
    },
    cptTypeModal: function() {
      return this.cptType === 'AModal'
    },
    cptTypeDrawer: function() {
      return this.cptType === 'ADrawer'
    },
    cptTitle: function() {
      let that = this
      return that._.get(that.context, '_title') || that.configObj.title || '简单弹窗'
    },
    cptDialogStyle: function() {
      let that = this
      let dialogStyle = {}
      if (that.cptTypeModal && that.fullScreen) {
        dialogStyle = {
          top: 0,
          paddingBottom: 0,
          maxWidth: '100vw',
        }
      }
      return dialogStyle
    },
    cptBodyStyle: function() {
      let that = this
      let bodyStyle = that.configObj.bodyStyle || {}
      if (that.cptTypeModal && that.fullScreen) {
        bodyStyle = Object.assign(bodyStyle, {
          height: 'calc(100vh - 55px - 53px)',
          overflowY: 'auto',
        })
      }
      return bodyStyle
    },
    cptSubmitBtn: function() {
      return this._.defaultTo(this.configObj.submitBtn, true)
    },
    cptSubmitText: function() {
      return this._.defaultTo(this.configObj.submitText, '确认')
    },
    cptCancelBtn: function() {
      return this._.defaultTo(this.configObj.cancelBtn, true)
    },
    cptCancelText: function() {
      return this._.defaultTo(this.configObj.cancelText, '取消')
    },
    cptPopWidth: function() {
      let that = this
      let popWidth = that._.defaultTo(this.configObj.popWidth, 700)
      if (that.fullScreen) {
        if (that.cptTypeModal) {
          popWidth = '100%'
        } else if (that.cptTypeDrawer) {
          popWidth = '100%'
        }
      }
      return popWidth
    },
  },
  methods: {
    /** 弹窗操作 */
    isPage() {
      return this.cptTypePage
    },
    showPop(record, context, submitCallback, cancelCallback) {
      let that = this
      that.record = that._.pickBy(record || {}, (value, key) =>
        !that._.isNil(value) && !key.startsWith('$') && !that._.isFunction(value)
      )
      that.context = context || {}
      that.submitCallback = submitCallback
      that.cancelCallback = cancelCallback
      that.visible = true
    },
    onSubmit(submitRecord, submitContext) {
      let that = this
      if (that.submitCallback) {
        let sRecord = Object.assign(that.record, submitRecord || {})
        let sContext = Object.assign(that.context, submitContext || {})
        let promise = that.submitCallback(sRecord, sContext)
        if (promise && promise.then) {
          that.submitLoading = true
          promise.then(() => {
            that.submitLoading = false
            that.doClose()
          }).catch(() => {
            that.submitLoading = false
          })
        }
        return promise
      }
    },
    doClose() {
      let that = this
      that.record = {}
      that.context = {}
      if (that.cancelCallback) {
        that.cancelCallback()
      }
      that.visible = false
      // 回调$destroy
      if (that.close) {
        that.close()
      }
    },
    toggleFullScreen() {
      let that = this
      that.fullScreen = !that.fullScreen
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
  .pop-content {
    flex: 1;
    min-height: 0;
  }
</style>
