<template>
  <a-card :bordered="false" class="page-block tmpl-block" :bodyStyle="{padding: 0, overflow: 'hidden'}">
    <mavon-editor
      ref="md"
      defaultOpen="preview"
      :boxShadow="false"
      :toolbarsFlag="mavonEditable"
      :editable="mavonEditable"
      :subfield="mavonEditable"
      :externalLink="mavonExternalLink"
      :previewBackground="mavonEditable?'#fbfbfb':'#ffffff'"
      :value="sExtend['markdown']"
      @imgAdd="handleImageAdd"
      @save="handleMavonSave">
    </mavon-editor>
    <a-row v-if="!mavonEditable" :gutter="24">
      <a-col :md="24" :sm="24" :xs="24">
        <div class="mdown-bottom">
          <div class="config-wrapper">
            <a-button size="small" @click="handleMavonEdit">
              <a-icon type="edit"/>内容
            </a-button>
          </div>
        </div>
      </a-col>
    </a-row>
  </a-card>
</template>

<!--
# 组件说明
  https://github.com/hinesboy/mavonEditor
  https://github.com/hinesboy/mavonEditor/blob/master/doc/cn/no-cnd.md
  Lab组件MarkDown
# 配置组件
  sModel: Object        // 当前组件SModel
  sContext: Object      // 上下文sContext
# 操作方法
  $emit('metaModel')    // 重新加载Model
-->

<script>
  const settings = require('@/settings')
  import { converters } from '@/utils/util'
  import { uploadFile } from '@/api/api'

  import MetaPageComponent from '@/store/models/MetaPageComponent'

  export default {
    name: "LabPartMdown",
    components: {
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
      }
    },
    data () {
      let webPath = settings.WEB_PATH
      return {
        /** 配置&状态 */
        baseUrl: settings.BASE_URL,
        mavonEditable: false,
        // https://github.com/hinesboy/mavonEditor/blob/master/doc/cn/no-cnd.md
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
        }
      }
    },
    computed: {
      sExtend: function(){
        return converters.parseObj(this.sModel?.extend)
      },
    },
    methods: {
      /** 执行操作 */
      handleMavonEdit(){
        this.mavonEditable = true
      },
      handleImageAdd(pos, file){
        let that = this
        // 上传前校验
        if(!that._beforeUpload(file)){
          return
        }
        let formData = new FormData();
        formData.set('upload_file', file)
        uploadFile(formData).then((resp)=>{
          // https://github.com/hinesboy/mavonEditor/blob/master/doc/cn/upload-images.md
          let fileUuid = that._.get(resp, 'gmodel[0].fileUuid')
          if(fileUuid){
            that.$refs.md.$img2Url(pos, `${that.baseUrl}/gcsStorage/downloadFile/${fileUuid}`)
          }
        }).catch(err => {
          // onError(err)
        })

      },
      handleMavonSave(value, render){
        render || true
        let that = this
        let extendObj = that._.cloneDeep(that.sExtend)
        extendObj['markdown'] = value
        let promise = MetaPageComponent.$update({
          params: {
            id: that.sModel?.id,
          },
          data: {
            extend : converters.encodeObj(extendObj)
          }
        })
        promise.then(() => {
          that.mavonEditable = false
          that.$emit('metaModel');
        }).catch(err => {
          err || true
          that.mavonEditable = true
        })
        return promise
      },
      /** 工具方法 */
      _beforeUpload(file){
        if(!file.type){
          return true
        }
        let that = this
        let isImage = file.type.startsWith('image/')
        if (!isImage) {
          that.$message.warn('只能上传图片文件!')
        }
        let isLt2M = file.size / 1024 / 1024 < 2
        if (!isLt2M) {
          that.$message.warn('文件大小不能超过2MB!')
        }
        return isImage && isLt2M
      },
    },
  }
</script>
