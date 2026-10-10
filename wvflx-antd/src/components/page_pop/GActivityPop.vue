<template>
  <a-modal
    :bodyStyle="cptBodyStyle"
    :visible="visible"
    :width="cptPopWidth"
    :okText="cptSubmitText"
    @ok="onSubmit"
    @cancel="doClose">
    <div slot="title" class="flex flex-row main-between">
      <div>
        <a-popover v-if="stores.isLabState()" placement="bottomLeft" overlayClassName="lab-popover">
          <a-icon type="bug" class="bug-icon" />
          <template slot="content">
            <div class="lab-item"><div class="title">Comp</div><div class="content">GActivityPop</div></div>
            <div class="lab-item"><div class="title">Title</div><div class="content">{{ configObj.title || '-' }}</div></div>
          </template>
        </a-popover>
        {{ cptTitle }}
      </div>
      <div style="margin-right: 24px;">
        <!-- blank -->
      </div>
    </div>
    <a-form :form="form" class="pop-form" labelAlign="left" @submit.prevent="onSubmit">
      <a-row :gutter="12">
        <a-col
          v-for="(item, index) in cptFieldItems"
          :key="index"
          :md="item.cols?item.cols:24"
          :sm="24"
          :xs="24"
          v-show="!_.defaultTo(_.get(collapsedState, item.panelIdent), false)">
          <g-form-item
            v-if="item.isActived?item.isActived(record, context):true"
            :labelCol="cptLabelCol"
            :wrapperCol="cptWrapperCol"
            :form="form"
            :item="item.funcItem?item.funcItem(item, record, context):item"
            :record="record"
            :context="context"
            :sContext="sContext"
            @collapsePanel="vPopUtil.collapsePanel(_self, item, $event)">
            <template
              v-if="item.component==='g-slot'"
              :slot="item.field">
              <slot :name="item.field" :record="record" :item="item" :form="form"></slot>
            </template>
          </g-form-item>
        </a-col>
        <a-col v-if="false" :sm="24" :xs="24">
          <mavon-editor
            ref="md"
            defaultOpen="preview"
            :boxShadow="false"
            :toolbarsFlag="mavonEditable"
            :editable="mavonEditable"
            :subfield="mavonEditable"
            :externalLink="mavonExternalLink"
            :previewBackground="mavonEditable?'#fbfbfb':'#ffffff'"
            :value="undefined"
            @change="editorChange">
          </mavon-editor>
        </a-col>
      </a-row>
    </a-form>
    <div slot="footer" class="flex flex-row flex-wrap main-between cross-center">
      <div v-if="false">
        <a @click="selectRelationUsers">
          <a-icon type="select"/>
          &nbsp;相关人员
        </a>
        <div
          class="user-wrapper"
          v-for="(relationUser, index) in mixRelationUsers().slice(0, userShowLimit)"
          :key="index">
          <AAvatar
            size="large"
            shape="circle"
            style="cursor: pointer; border: 1px solid #e6e9f0; filter: grayscale(40%);"
            :src="tools.avatarUrl(relationUser, 'avatar')"/>
          <span :title="relationUser.nickname">&nbsp;{{ relationUser.nickname | Ellipsis(12) }}</span>
          <div class="remove warning" @click="removeRelationUser(relationUser)"><a-icon type="close-circle"/></div>
        </div>
        <span v-if="mixRelationUsers().length>userShowLimit" style="margin-left: 8px;" class="text-tips">等共{{ mixRelationUsers().length }}个用户</span>
      </div>
      <div class="item-right">
        <a-button v-if="cptSubmitBtn" type="primary" :disabled="false" :loading="loadingObj['submit']" @click="onSubmit(0, 'submit')">
          {{ record.id?'更新':'保存' }}
        </a-button>
        <a-button :disabled="false" :loading="loadingObj['doneOnly']" @click="onSubmit(1, 'doneOnly')">
          完成
        </a-button>
        <a-button :disabled="false" :loading="loadingObj['doneAndCreate']" @click="onSubmit(2, 'doneAndCreate')">
          完成并新建
        </a-button>
        <a-button :style="cptSubmitBtn?{'margin-left': '8px'}:{}" @click="doClose">取消</a-button>
      </div>
    </div>
  </a-modal>
</template>

<script>
  import AAvatar from 'ant-design-vue/lib/avatar';

  import { tools, stores } from '@/utils/util'
  import { vPopUtil } from '@/utilsv/vUtil'

  import { COMPO, DATE } from '@/constants'
  import settings from "@/settings";

  export default {
    name: "GActivityPop",
    components: {
      AAvatar,
    },
    props: {
      configObj: {
        type: Object,
        required: true,
      },
      sContext: {
        type: Object,
        default: function(){
          return {}
        }
      },
    },
    data () {
      let that = this
      let webPath = settings.WEB_PATH
      return {
        /** 配置&状态 */
        tools: tools,
        stores: stores,
        vPopUtil: vPopUtil,
        form: that.$form.createForm(this),
        visible: false,
        loading:false,
        loadingObj: {},
        collapsedState: {},
        record: {},
        context: {},
        submitCallback: undefined,
        cancelCallback: undefined,
        /** Mavon配置 */
        // https://github.com/hinesboy/mavonEditor/blob/master/doc/cn/no-cnd.md
        mavonEditable: true,
        mavonExternalLink: {
          markdown_css: function() {
            return `${webPath}/mavon/markdown/github-markdown.min.css`;
          },
          hljs_js: function() {
            return `${webPath}/mavon/highlightjs/highlight.min.js`;
          } && false,
          hljs_css: function(css) {
            return `${webPath}/mavon/highlightjs/styles/${css}.min.css`;
          } && false,
          hljs_lang: function(lang) {
            return `${webPath}/mavon/highlightjs/languages/${lang}.min.js`;
          } && false,
          katex_css: function() {
            return `${webPath}/mavon/katex/katex.min.css`;
          } && false,
          katex_js: function() {
            return `${webPath}/mavon/katex/katex.min.js`;
          } && false,
        },
        mavonDetail: undefined,
        /** 扩展属性 */
        userShowLimit: 3,
        relationUsers: [],
      }
    },
    computed: {
      /** Cpt选项 */
      cptTitle: function(){
        let that = this
        return that._.get(that.context, '_title') || that.configObj.title || '日程'
      },
      cptBodyStyle: function(){
        return this.configObj.bodyStyle || {}
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
        })
        return formFields
      },
      cptSubmitBtn: function(){
        return this._.defaultTo(this.configObj.submitBtn, true)
      },
      cptSubmitText: function(){
        return this._.defaultTo(this.configObj.submitText, '确认')
      },
      cptPopWidth: function(){
        return this._.defaultTo(this.configObj.popWidth, 700)
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
            if([COMPO.G_PANEL, COMPO.G_LOCK].indexOf(item.component)!==-1){ return }
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
      onSubmit (_opType, _opIdent) {
        let that = this
        let loadingStr = _opIdent
        that.form.validateFields((errors, values) => {
          if (!errors) {
            that.cptFieldItems.forEach( item => {
              if([COMPO.G_PANEL, COMPO.G_LOCK].indexOf(item.component)!==-1){ return }
              let actived = item.isActived?item.isActived(that.record, that.context):true
              if(actived){
                let fieldValue = values[item.field]
                fieldValue = that._.isNil(fieldValue)?undefined:fieldValue
                if(item.setValue) {
                  item.setValue(that.record, fieldValue)
                }else if([COMPO.G_INPUT, COMPO.G_TEXTAREA].indexOf(item.component) !== -1) {
                  that.record[item.field] = that._.isEmpty(fieldValue)?'':that._.trim(fieldValue)
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
            // 【扩展】富文本输入 && 关联用户
            // that._.set(that.record, 'detail', that.mavonDetail)
            that._.set(that.record, '_opType', _opType)
            that._.set(that.record, 'relationUsers', that.mixRelationUsers().map(item => item.id))
            if(that.submitCallback){
              that.loadingObj[loadingStr] = true
              let promise = that.submitCallback(that.record, that.context)
              if(promise && promise.then){
                promise.then(() => {
                  that.loadingObj[loadingStr] = false
                  that.doClose()
                }).catch(() => {
                  that.loadingObj[loadingStr] = false
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
      /** 扩展操作 */
      editorChange(val, render){
        let that = this
        that.mavonDetail = render
      },
      selectRelationUsers(){
        let that = this
        vPopUtil.selectTransferUser(
          that, {}, {multiple: true},
          (record, context) => {
            /* submit */
            context || true
            let noRepeatUsers = (record || []).filter(user =>
              that.relationUsers.findIndex( item => item.username === user.username) === -1
            )
            that.relationUsers.push(...noRepeatUsers)
            return Promise.resolve()
          }, () => {
            /* cancel */
          })
      },
      mixRelationUsers(){
        let that = this
        let mixUsers = []
        mixUsers.push(...that.relationUsers);
        return mixUsers
      },
      removeRelationUser(user){
        let that = this
        let recordIndex = that.relationUsers.findIndex( item => item.username === user.username)
        if(recordIndex !== -1){
          that.relationUsers.splice(recordIndex, 1)
        }
      }
    },
  }
</script>

<style lang="scss" scoped>
  $tmpl-color-zero6: rgba(241, 139, 98, 0.6);

  .bug-icon {
    color: $tmpl-color-zero6;
    margin-right: 6px;
  }
  .user-wrapper{
    display: inline-block;
    margin-left: 8px;
    position: relative;
    padding-right: 15px;
    .remove{
      cursor: pointer;
      opacity: 0;
      position: absolute;
      top: -4px;
      right: -2px;
    }
    &:hover {
      // background-color: #fafafa;
      .remove {
        opacity: 1;
      }
    }
  }
</style>

