<template>
  <div class="tinymce-editor">
    <TinymceEditor
      v-model="realValue"
      :init="initOptions"
      :disabled="disabled"
      @onClick="onClick">
    </TinymceEditor>
  </div>
</template>

<!--
# 组件说明(组件较大，谨慎引入)
  富文本输入组件，组件比较大，打包后1M大小(Uglify后，非压缩)；
  支持输入、撤销、段落、加粗、倾斜、颜色、背景色、对齐、列表、图片、表格等功能；
# 配置组件
  value: String             // 当前选中条目
  disabled: Boolean         // 禁用状态，默认false
  plugins: [String, Array]  // 插件列表
  toolbar: [String, Array]  // 工具栏列表
# 操作方法
  $emit('onClick', e, tinymce)
  $emit('change', newValue)
-->

<script>
  const settings = require('@/settings')
  import tinymce from 'tinymce/tinymce'
  import TinymceEditor from '@tinymce/tinymce-vue'
  import 'tinymce/themes/silver/theme'
  import 'tinymce/plugins/image'       // 插入上传图片插件
  import 'tinymce/plugins/media'       // 插入视频插件
  import 'tinymce/plugins/table'       // 插入表格插件
  import 'tinymce/plugins/lists'       // 列表插件
  import 'tinymce/plugins/contextmenu' // 右键菜单
  import 'tinymce/plugins/colorpicker' // 取色器
  import 'tinymce/plugins/textcolor'   // 文本颜色
  import 'tinymce/plugins/wordcount'   // 字数统计插件
  import { uploadFile } from '@/api/api'

  export default {
    name: 'GTinymce',
    components: {
      TinymceEditor
    },
    /* eslint-disable */
    props: {
      value: {
        type: String,
        required: false
      },
      disabled: {
        type: Boolean,
        default: false
      },
      /** 扩展Props */
      plugins: {
        type: [String, Array],
        default: 'lists image table textcolor wordcount contextmenu'
      },
      toolbar: {
        type: [String, Array],
        default: 'undo redo |  formatselect | bold italic forecolor backcolor | alignleft aligncenter alignright alignjustify | bullist numlist outdent indent | lists image table | removeformat'
      }
    },
    watch: {
      value (newValue) {
        let that = this
        that.realValue = that._.toString(newValue)===''?undefined:that._.toString(newValue)
      },
      realValue (newValue) {
        this.$emit('change', newValue)
      }
    },
    data () {
      let that = this
      let webPath = settings.WEB_PATH
      webPath = webPath.endsWith('/')?webPath.slice(0, -1):webPath
      return {
        baseUrl: settings.BASE_URL,
        realValue: that.value,
        initOptions: {
          language: 'zh_CN',
          language_url: `${webPath}/tinymce/langs/zh_CN.js`,
          skin_url: `${webPath}/tinymce/skins/lightgray`,
          content_css: `${webPath}/tinymce/skins/lightgray/content.min.css`,
          height: 300,
          plugins: that.plugins,
          toolbar: that.toolbar,
          branding: false,
          menubar: false,
          images_upload_handler: (blobInfo, success, failure) => {
            /* Base64上传图片
            let img = 'data:image/jpeg;base64,' + blobInfo.base64()
            success(img)*/
            // Blob上传图片
            let formData = new FormData();
            formData.set('upload_image', blobInfo.blob())
            uploadFile(formData).then((resp)=>{
              let respItem = that._.get(resp,'gmodel[0]')
              success(`${that.baseUrl}/gcsStorage/downloadFile/${respItem['fileUuid']}`)
            }).catch(err => {
              err || true
              failure('图片上传失败')
            })
          }
        },
      }
    },
    methods: {
      onClick (e) {
        this.$emit('onClick', e, tinymce)
      },
      clear () {
        this.realValue = ''
      },
    },
    mounted () {
      tinymce.init({})
    },
  }
</script>