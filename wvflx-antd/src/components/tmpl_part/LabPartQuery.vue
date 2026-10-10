<template>
  <a-card :bordered="false" class="page-block tmpl-block" :data-cptFieldItems="cptFieldItems">
    <a-form
      layout="inline"
      :form="form"
      @submit.prevent="onSubmit">
      <!-- 拖拽目的 -->
      <draggable
        tag="a-row"
        :componentData="vQueryUtil.cptComponentData()"
        class="draggable-container"
        :list="datFieldItems"
        v-bind="vQueryUtil.cptDragTargetOptions()"
        @change="(changeEvent) => vQueryUtil.handleItemDrag(_self, changeEvent)">
        <a-col
          v-for="(item, index) in datFieldItems"
          :key="index"
          :md="_.get(item, 'extend.cols') || 8"
          :sm="24"
          :xs="24"
          :class="{'drag-item': true, active: activeItem.id === item.id}"
          @click.native.stop="vQueryUtil.handleItemSelect(_self, item, $event)">
          <!-- Item组件 -->
          <g-form-item :item="item"></g-form-item>
          <!-- Item操作 -->
          <template v-if="activeItem.id === item.id">
            <div class="drag-item-action">
              <a-popover :title="'FIELD(' + item.title + ')'" placement="bottomLeft" overlayClassName="lab-popover">
                <a-icon type="bug" />
                <template slot="content">
                  <div class="lab-item">
                    <div class="title">Ident</div>
                    <div class="content"> <a @click="copyText(_.get(item, 'field'), $event)">{{ item.field }}</a> </div>
                  </div>
                  <div class="lab-item">
                    <div class="title">Type</div>
                    <div class="content">{{ item.type }}</div>
                  </div>
                  <div class="lab-item">
                    <div class="title">Comp</div>
                    <div class="content">{{ item.component }}</div>
                  </div>
                </template>
              </a-popover>
              <a-divider type="vertical" style="margin: 0 4px;" />
              <a-icon type="edit" @click="vQueryUtil.handleItemEdit(_self, item)"/>
              <a-divider type="vertical" style="margin: 0 4px;" />
              <a-popconfirm title="确定删除字段?" @confirm="() => vQueryUtil.handleItemDelete(_self, item)">
                <a-icon type="delete" />
              </a-popconfirm>
            </div>
            <div class="drag-item-widget">
              <a-icon type="g-drag" />
            </div>
          </template>
        </a-col>
      </draggable>
      <a-row :gutter="24">
        <a-col :md="24" :sm="24" :xs="24">
          <div class="query-bottom">
            <span class="query-submit-buttons">
              <a-button type="primary" html-type="submit">查询</a-button>
              <a-button class="reset-btn" @click="onQueryReset">重置</a-button>
            </span>
            <div class="config-wrapper">
              <a-button size="small" @click="vQueryUtil.handleColsConfig(_self)">
                <a-icon type="setting"/>查询列
              </a-button>
            </div>
          </div>
        </a-col>
      </a-row>
    </a-form>

    <!-- 弹窗部分  -->
    <GColsPop
      ref="refCols"
      :configObj="colsPopConfig">
    </GColsPop>
    <GFormPop
      ref="refMeta"
      :configObj="metaPopConfig">
    </GFormPop>
  </a-card>
</template>

<!--
# 组件说明
  Lab查询组件
# 配置组件
  sModel: Object        // 当前组件SModel
  sContext: Object      // 上下文sContext
  注：查询字段不包含['File', 'Picture', 'RefInverse']
# 操作方法
  onQueryReset()
  $emit('doSubmit', that.queryRules);
-->

<script>
  import ACheckbox from 'ant-design-vue/lib/checkbox';

  import { vQueryUtil } from '@/utilsv/vUtil'

  import MetaPageComponentField from '@/store/models/MetaPageComponentField'

  import GColsPop from '@/components/page_pop/GColsPop'
  import GFormPop from '@/components/page_pop/GFormPop'

  import labPartQueryColsPopConfig from './configs/labPartQueryColsPopConfig'
  import labPartQueryMetaPopConfig from './configs/labPartQueryMetaPopConfig'

  export default {
    name: "LabPartQuery",
    components:{
      ACheckbox,
      GColsPop,
      GFormPop,
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
      },
    },
    data () {
      return {
        /** 配置&状态 */
        form: this.$form.createForm(this),
        vQueryUtil: vQueryUtil,
        colsPopConfig: labPartQueryColsPopConfig,
        metaPopConfig: labPartQueryMetaPopConfig,
        popVisible: false,
        /** 数据部分 */
        activeItem: {},
        queryRules: [],
        datFieldItems: [],  // 镜像cptFieldItems
      };
    },
    computed: {
      cptFieldItems: function(){
        let that = this
        let queryFields = MetaPageComponentField.query().where('metaPageComponentId', that.sModel?.id)
          .orderBy('seqOrder', 'asc').orderBy('id', 'asc').with('relMetaDomainField').get()
        let cloneItemFunc = vQueryUtil.queryCloneItem
        that.datFieldItems = queryFields.map(item => cloneItemFunc(that, item))
        return queryFields
      },
    },
    inject: ['copyText'],
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
        this.onSubmit();
      },
      /** 工具方法 */
      _itemChecked(fieldItem){
        let that = this
        let itemIndex = that.datFieldItems.findIndex(item => item.field===fieldItem.fieldName)
        return itemIndex!==-1
      },
      _findItem(fieldName){
        let that = this
        return that.datFieldItems.find(item => item.field===fieldName);
      },
    },
  }
</script>
