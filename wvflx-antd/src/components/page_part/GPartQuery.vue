<template>
  <a-card :bordered="false" class="page-block">
    <a-form
      layout="inline"
      :form="form"
      @submit.prevent="onSubmit">
      <a-row :gutter="24">
        <a-col
          v-for="(item, index) in configObj.queryFields"
          :key="index"
          :md="_.get(item, 'extend.cols') || 8"
          :sm="24"
          :xs="24">
          <g-form-item :item="item" :form="form">
            <template
              v-if="item.component===COMPO.G_SLOT"
              :slot="item.field">
              <slot :name="item.field" :item="item" :form="form"></slot>
            </template>
          </g-form-item>
        </a-col>
        <a-col :md="4" :sm="24" :xs="24">
          <span class="query-submit-buttons">
            <a-button type="primary" html-type="submit">查询</a-button>
            <a-button class="reset-btn" @click="onQueryReset">重置</a-button>
          </span>
        </a-col>
      </a-row>
    </a-form>
  </a-card>
</template>

<!--
# 组件说明
  通用查询组件
# 配置组件
  configObj:{
      queryFields: [{
        ... GFormItem
        component: String                  // 可为g-slot
        extend: {
          cols: Number                     // 默认为 8
        }
      }]
  }
  sContext: Object
# 操作方法
  $emit('doSubmit', that.queryRules)
  $emit('queryReset')
-->

<script>
  import { COMPO } from '@/constants'
  import { vQueryUtil } from '@/utilsv/vUtil'

  export default {
    name: "GPartQuery",
    components:{
    },
    props: {
      configObj: {
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
      return {
        /** 配置&状态 */
        COMPO: COMPO,
        form: this.$form.createForm(this),
        queryRules: [],    // 查询规则
      };
    },
    methods: {
      /** 提交和重置 */
      onSubmit() {
        let that = this
        that.form.validateFields((errors, values) => {
          if (!errors) {
            that.queryRules = vQueryUtil.queryRules(values, that._findItem)
            that.$emit('doSubmit', that.queryRules);
          }
        })
      },
      onQueryReset() {
        this.form.resetFields()
        this.$emit('queryReset');
        this.onSubmit();
      },
      /** 工具方法 */
      _findItem(fieldName){
        let that = this
        return that.configObj.queryFields?.find(item => item.field === fieldName)
      },
    },
  }
</script>