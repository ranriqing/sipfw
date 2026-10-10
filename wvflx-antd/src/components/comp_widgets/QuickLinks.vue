<template>
  <a-card :bordered="false" class="page-block quick-links" :class="{'tmpl-block': cptIsLab}">
    <div class="chart-header">
      <span class="header-title">
        {{ cptTitle }}
      </span>
      <span v-if="cptIsLab" class="header-action">
        <a class="design" @click="handleLinksDesign">
          <a-icon style="font-size: 14px;" type="bulb"/>
        </a>
      </span>
    </div>
    <div class="links-wrapper">
      <a-button
        v-for="(link, index) in cptLinks"
        :key="index"
        :ghost="true"
        type="primary"
        :href="link.url"
        target="_blank"
        :icon="link.icon || undefined">
        {{ link.title }}
      </a-button>
    </div>

    <!-- 配置弹窗(Lab模式) -->
    <GFormPop
      ref="refLinks"
      :configObj="linksPopConfig">
      <template slot="links" slot-scope="{ record }">
        <draggable :list="record.links" v-bind="cptDragOptions">
          <div
            v-for="(link, index) in record.links"
            :key="index"
            class="link-item">
            <a-icon class="drag drag-widget" type="menu"/>
            <a-button class="icon-btn" :icon="link.icon || 'select'" @click="openIconPicker(link)"></a-button>
            <a-input v-model="link.title" placeholder="标题"/>
            <a-input v-model="link.url" placeholder="https://..."/>
            <a-icon class="trash" type="delete" @click="record.links.splice(index, 1)"/>
          </div>
        </draggable>
        <a-button type="dashed" :block="true" icon="plus" @click="record.links.push({ title: '', url: '' })">
          添加链接
        </a-button>
        <GPlainPop
          ref="refIconPick"
          :configObj="iconPickPopConfig">
          <GIconSelector
            :item="{}"
            :value="iconPickLink ? iconPickLink.icon : undefined"
            @change="handleIconPick">
          </GIconSelector>
        </GPlainPop>
      </template>
    </GFormPop>
  </a-card>
</template>

<!--
# 组件说明
  外部链接Widget组件，展示标题+一排外部链接按钮;
  Lab模式下可通过灯泡按钮配置卡片标题与链接列表(标题/URL/图标, 拖拽排序/新增/删除);
# 配置组件
  sModel: Object        // 当前组件SModel(MetaPageComponent), 配置存于 sModel.extend
  sContext: Object      // 上下文sContext
# 操作方法
  暂无
-->

<script>
  import { converters, stores } from '@/utils/util'

  import GFormPop from '@/components/page_pop/GFormPop'
  import GPlainPop from '@/components/page_pop/GPlainPop'
  import GIconSelector from '@/components/comp_form/select/GIconSelector'
  import MetaPageComponent from '@/store/models/MetaPageComponent'

  import linksPopConfig from './configs/linksPopConfig'

  export default {
    name: "QuickLinks",
    components: {
      GFormPop,
      GPlainPop,
      GIconSelector,
    },
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
    data() {
      return {
        /** 配置&状态 */
        converters: converters,
        stores: stores,
        linksPopConfig: linksPopConfig,
        /** 图标选择弹窗 */
        iconPickPopConfig: {
          title: '选择图标',
          modal: true,
          popWidth: 520,
          submitBtn: false,
          cancelBtn: true,
        },
        iconPickLink: undefined,
      };
    },
    computed: {
      sExtend: function() {
        return converters.parseObj(this.sModel?.extend)
      },
      cptIsLab: function() {
        return stores.isLabState()
      },
      cptTitle: function() {
        return this.sExtend['title'] || '外部链接'
      },
      cptLinks: function() {
        return this.sExtend['links'] || []
      },
      cptDragOptions: function() {
        return {
          animation: 200,
          group: 'links',
          ghostClass: 'ghost',
          handle: '.drag-widget',
        };
      },
    },
    methods: {
      /** 图标选择 */
      openIconPicker(link) {
        let that = this
        that.iconPickLink = link
        let slotRef = that.$refs.refIconPick
        slotRef.showPop({}, {}, undefined, undefined)
      },
      handleIconPick(iconType) {
        let that = this
        if (that.iconPickLink) {
          that.$set(that.iconPickLink, 'icon', iconType)
        }
        let slotRef = that.$refs.refIconPick
        slotRef.doClose()
      },
      /** 设计外部链接 */
      handleLinksDesign() {
        let that = this
        let slotRef = that.$refs.refLinks
        slotRef.showPop({
            title: that.sExtend['title'],
            links: that._.cloneDeep(that.cptLinks),
          }, { targetModel: that.sModel },
          (record, context) => { /* submit */
            context || true
            let links = (record.links || [])
              .filter(link => link.title && link.url)
              .map(link => ({
                title: that._.trim(link.title),
                url: that._.trim(link.url),
                icon: link.icon || undefined,
              }))
            let extend = that._.cloneDeep(that.sExtend)
            extend.title = record.title
            extend.links = links
            let promise = MetaPageComponent.$update({
              params: {
                id: that.sModel?.id,
              },
              data: {
                extend : converters.encodeObj(extend),
              }
            })
            promise.then(() => {
              // 加载元信息
              that.$emit('metaModel');
            })
            return promise
          }, () => { /* cancel */
            /* do nothing */
          }
        )
      },
    },
  }
</script>

<style lang="scss" scoped>
  @import '~@/assets/scss/theme.scss';
  .quick-links{
    .chart-header{
      position: relative;
      overflow: hidden;
      width: 100%;
      font-size: 14px;
      line-height: 22px;
      margin-bottom: 12px;
      .header-title{
        color: #909090;
        // font-weight: bold;
      }
      .header-action{
        cursor: pointer;
        position: absolute;
        top: 0;
        right: 0;
        .design{
          .anticon{
            color: $tmpl-color-zero8;
            &:hover{
              color: $tmpl-color-zero10;
            }
          }
        }
      }
    }
    .links-wrapper{
      display: flex;
      flex-direction: row;
      flex-wrap: wrap;
      justify-content: flex-start;
      align-items: center;
      row-gap: 10px;
      column-gap: 10px;
    }
  }
  // 配置弹窗内链接编辑行
  .link-item{
    display: flex;
    flex-direction: row;
    justify-content: flex-start;
    align-items: center;
    column-gap: 6px;
    padding: 4px 0;
    .drag{
      font-size: 15px;
      cursor: move;
    }
    .icon-btn{
      flex: 0 0 auto;
      width: 24px;
      height: 24px;
      padding: 0;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      .anticon{
        font-size: 14px;
        line-height: 1;
      }
    }
    .ant-input{
      flex: 1 1 auto;
      min-width: 0;
    }
    .trash{
      font-size: 15px;
      cursor: pointer;
      color: rgba(0, 0, 0, .45);
      &:hover{
        color: #1f1f1f;
      }
    }
  }
</style>
