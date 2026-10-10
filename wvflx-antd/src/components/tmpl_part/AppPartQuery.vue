<template>
  <a-card :bordered="false" class="page-block">
    <a-form
      layout="inline"
      :form="form"
      @submit.prevent="onSubmit">
      <a-row :gutter="24">
        <a-col
          v-for="(item, index) in cptFieldItems"
          :key="index"
          :md="_.get(item, 'extend.cols') || 8"
          :sm="24"
          :xs="24">
          <g-form-item :item="item"></g-form-item>
        </a-col>
        <a-col :md="8" :sm="24" :xs="24">
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
  App查询组件
# 配置组件
  sModel: Object        // 当前组件SModel
  sContext: Object      // 上下文sContext
# 操作方法
  onQueryReset()
  $emit('doSubmit', that.queryRules);
-->

<script>
  import { vQueryUtil } from '@/utilsv/vUtil'

  import MetaPageComponentField from '@/store/models/MetaPageComponentField'

  export default {
    name: "AppPartQuery",
    components:{
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
      return {
        /** 配置&状态 */
        form: this.$form.createForm(this),
        queryRules: [],    // 查询规则
      };
    },
    computed: {
      cptFieldItems: function(){
        let that = this
        let queryFields = MetaPageComponentField.query().where('metaPageComponentId', that.sModel?.id)
          .orderBy('seqOrder', 'asc').orderBy('id', 'asc').with('relMetaDomainField').get()
        let cloneItemFunc = vQueryUtil.queryCloneItem
        return queryFields.map(item => cloneItemFunc(that, item))
      },
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
        });
      },
      onQueryReset() {
        this.form.resetFields()
        this.$emit('queryReset');
        this.onSubmit();
      },
      /** 工具方法 */
      _findItem(fieldName){
        let that = this
        return that.cptFieldItems?.find(item => item.field===fieldName);
      },
    },
  }
</script>
