<template>
  <a-card :bordered="false" class="page-block" :bodyStyle="{padding: 0, overflow: 'hidden'}">
    <mavon-editor
      ref="md"
      defaultOpen="preview"
      :boxShadow="false"
      :toolbarsFlag="mavonEditable"
      :editable="mavonEditable"
      :subfield="mavonEditable"
      :externalLink="mavonExternalLink"
      :previewBackground="mavonEditable?'#fbfbfb':'#ffffff'"
      :value="sExtend['markdown']">
    </mavon-editor>
  </a-card>
</template>

<!--
# 组件说明
  https://github.com/hinesboy/mavonEditor
  https://github.com/hinesboy/mavonEditor/blob/master/doc/cn/no-cnd.md
  App组件MarkDown
# 配置组件
  sModel: Object        // 当前组件SModel
  sContext: Object      // 上下文sContext
# 操作方法
  $emit('metaModel')    // 重新加载Model
-->

<script>
  const settings = require('@/settings')
  import { converters } from '@/utils/util'

  export default {
    name: "AppPartMdown",
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
    },
  }
</script>
