<template>
  <div class="editor-wrapper">
    <mavon-editor
      ref="md"
      style="padding: 0;"
      defaultOpen="preview"
      :boxShadow="false"
      :toolbarsFlag="cptMavonEditable"
      :editable="cptMavonEditable"
      :subfield="cptMavonEditable"
      :externalLink="mavonExternalLink"
      :previewBackground="cptMavonEditable?'#fbfbfb':'transparent'"
      :value="initValue"
      @imgAdd="onImageAdd"
      @change="onEditorChange">
    </mavon-editor>
  </div>
</template>

<!--
# 组件说明
  https://github.com/hinesboy/mavonEditor
  https://github.com/hinesboy/mavonEditor/blob/master/doc/cn/no-cnd.md
  富文本Mavon组件
# 操作方法

-->

<script>
  import settings from "@/settings";
  import {uploadFile} from "@/api/api";

  export default {
    name: 'GMavonEditor',
    components: {
    },
    /* eslint-disable */
    props: {
      value: {
        type: String,
        required: false,
        default: '',
      },
      initValue: {
        type: String,
        required: false
      },
      readonly: {
        type: Boolean,
        default: false
      },
      disabled: {
        type: Boolean,
        default: false
      },
    },
    data () {
      let webPath = settings.WEB_PATH
      return {
        /** 配置&状态 */
        baseUrl: settings.BASE_URL,
        mavonEditable: true,
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
      cptMavonEditable: function () {
        let that = this
        return !that.readonly && !that.disabled && that.mavonEditable
      },
    },
    methods: {
      /** 执行操作 */
      onImageAdd(pos, file){
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
      onEditorChange(val, render){
        let that = this
        render || true
        that.$emit('change', val)
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
