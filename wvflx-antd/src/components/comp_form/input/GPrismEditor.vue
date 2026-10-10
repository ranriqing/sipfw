<template>
  <div v-if="readonly">
    {{ _.toString(value) }}
  </div>
  <prism-editor
    v-else
    class="prism-editor"
    :value="cptValue"
    :disabled="disabled"
    :highlight="highlighter"
    :line-numbers="lineNumbers"
    :tabSize="4"
    @input="onChange">
  </prism-editor>
</template>

<!--
# 组件说明
  代码编辑组件，用于代码编辑；
# 配置组件
  value: String              // 展示的内容
# 操作方法
  $emit('change', value)
-->

<script>
  import { PrismEditor } from "vue-prism-editor";
  import "vue-prism-editor/dist/prismeditor.min.css"; // import the styles somewhere

  // import highlighting library (you can use any library you want just return html string)
  import { highlight, languages } from "prismjs/components/prism-core";
  import 'prismjs/components/prism-clike';
  import "prismjs/components/prism-groovy";
  // import 'prismjs/components/prism-javascript';
  import "prismjs/themes/prism-tomorrow.css"; // import syntax highlighting styles


  export default {
    name: 'GPrismEditor',
    components: {
      PrismEditor
    },
    /* eslint-disable */
    props: {
      item: {
        type: Object,
        required: true
      },
      value: {
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
      placeholder: {
        type: String,
        default: ''
      },
    },
    data () {
      return {
        lineNumbers: true
      }
    },
    computed: {
      cptValue: function(){
        let that = this
        if(that._.isNil(that.value)){
          return ''
        }else{
          return that._.toString(that.value)
        }
      }
    },
    methods: {
      highlighter(code) {
        return highlight(code, languages.groovy); //returns html
      },
      onChange (value) {
        this.$emit('change', value)
      },
    },
  }
</script>

<style lang="scss" scoped>
  .prism-editor {
    background: #2d2d2d;
    color: #ccc;
    font-size: 14px;
    line-height: 1.5;
    padding: 5px;
    min-height: 200px;
    border-radius: 4px;
  }
  // optional
  .prism-editor__textarea:focus {
    outline: none;
  }
</style>
