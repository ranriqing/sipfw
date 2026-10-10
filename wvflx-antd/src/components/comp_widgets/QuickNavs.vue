<template>
  <a-card :bordered="false" class="page-block quick-nav" :class="{'tmpl-block': cptIsLab}">
    <div class="chart-header">
      <span class="header-title">
        {{ cptTitle }}
      </span>
      <span v-if="cptIsLab" class="header-action">
        <a class="design" @click="handleNavDesign">
          <a-icon style="font-size: 14px;" type="bulb"/>
        </a>
      </span>
    </div>
    <div class="nav-wrapper">
      <div
        v-for="(item, index) in cptItems"
        :key="index"
        class="nav-item"
        :class="{'nav-item--hollow': item.hollow}"
        :style="{ '--nav-color': item.color || defaultColor }"
        @click="switchRoute(item.pdent)">
        <div class="nav-icon" :style="item.hollow ? {} : { backgroundColor: item.color || defaultColor }">
          <a-icon v-if="item.icon" :type="item.icon"/>
        </div>
        <div class="nav-title">{{ item.title }}</div>
      </div>
    </div>

    <!-- 配置弹窗(Lab模式) -->
    <GFormPop
      ref="refItems"
      :configObj="navPopConfig">
      <template slot="items" slot-scope="{ record }">
        <draggable :list="record.items" v-bind="cptDragOptions">
          <div
            v-for="(item, index) in record.items"
            :key="index"
            class="item-row">
            <a-icon class="drag drag-widget" type="menu"/>
            <a-button class="icon-btn" :icon="item.icon || 'select'" @click="openIconPicker(item)"></a-button>
            <a-popover trigger="click" placement="bottom">
              <template slot="content">
                <div class="color-picker">
                  <span
                    v-for="color in navColors"
                    :key="color"
                    class="color-dot"
                    :class="{'active': item.color === color}"
                    :style="{ backgroundColor: color }"
                    @click="item.color = color"></span>
                </div>
              </template>
              <a-button class="color-btn" :style="{ backgroundColor: item.color || defaultColor }"></a-button>
            </a-popover>
            <span class="hollow-flag">
              <a-switch v-model="item.hollow" size="small"/>
              <span>镂空</span>
            </span>
            <a-input v-model="item.title" placeholder="标题"/>
            <a-input v-model="item.pdent" placeholder="routePdent"/>
            <a-icon class="trash" type="delete" @click="record.items.splice(index, 1)"/>
          </div>
        </draggable>
        <a-button type="dashed" :block="true" icon="plus" @click="record.items.push({ icon: '', color: defaultColor, title: '', pdent: '', hollow: false })">
          添加入口
        </a-button>
        <GPlainPop
          ref="refIconPick"
          :configObj="iconPickPopConfig">
          <GIconSelector
            :item="{}"
            :value="iconPickItem ? iconPickItem.icon : undefined"
            @change="handleIconPick">
          </GIconSelector>
        </GPlainPop>
      </template>
    </GFormPop>
  </a-card>
</template>

<!--
# 组件说明
  快捷导航Widget组件，展示标题+若干内部页面快捷入口(色块图标+文字);
  Lab模式下可通过灯泡按钮配置卡片标题与入口列表(图标/背景色/标题/routePdent, 拖拽排序/新增/删除);
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

  import navPopConfig from './configs/quickNavPopConfig'

  const NAV_COLORS = [
    '#16baaa', '#1e9fff', '#a233c6', '#ff5722', 
    '#f75496', '#5ecc3e', '#fe9555', '#fec34f'
  ]

  export default {
    name: "QuickNavs",
    components: {
      GFormPop,
      GPlainPop,
      GIconSelector,
    },
    inject: ['switchRoute'],
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
        navPopConfig: navPopConfig,
        /** 背景色固定配色 */
        navColors: NAV_COLORS,
        defaultColor: NAV_COLORS[0],
        /** 图标选择弹窗 */
        iconPickPopConfig: {
          title: '选择图标',
          modal: true,
          popWidth: 520,
          submitBtn: false,
          cancelBtn: true,
        },
        iconPickItem: undefined,
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
        return this.sExtend['title'] || '快捷导航'
      },
      cptItems: function() {
        return this.sExtend['items'] || []
      },
      cptDragOptions: function() {
        return {
          animation: 200,
          group: 'navItems',
          ghostClass: 'ghost',
          handle: '.drag-widget',
        };
      },
    },
    methods: {
      /** 图标选择 */
      openIconPicker(item) {
        let that = this
        that.iconPickItem = item
        let slotRef = that.$refs.refIconPick
        slotRef.showPop({}, {}, undefined, undefined)
      },
      handleIconPick(iconType) {
        let that = this
        if (that.iconPickItem) {
          that.$set(that.iconPickItem, 'icon', iconType)
        }
        let slotRef = that.$refs.refIconPick
        slotRef.doClose()
      },
      /** 设计快捷导航 */
      handleNavDesign() {
        let that = this
        let slotRef = that.$refs.refItems
        slotRef.showPop({
            title: that.sExtend['title'],
            items: that._.cloneDeep(that.cptItems),
          }, { targetModel: that.sModel },
          (record, context) => { /* submit */
            context || true
            let items = (record.items || [])
              .filter(item => item.title && item.pdent)
              .map(item => ({
                title: that._.trim(item.title),
                pdent: that._.trim(item.pdent),
                icon: item.icon || undefined,
                color: item.color || that.defaultColor,
                hollow: !!item.hollow,
              }))
            let extend = that._.cloneDeep(that.sExtend)
            extend.title = record.title
            extend.items = items
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
  .quick-nav{
    .chart-header{
      position: relative;
      overflow: hidden;
      width: 100%;
      font-size: 14px;
      line-height: 22px;
      margin-bottom: 12px;
      .header-title{
        color: #909090;
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
    .nav-wrapper{
      display: flex;
      flex-direction: row;
      flex-wrap: wrap;
      justify-content: flex-start;
      align-items: stretch;
      row-gap: 12px;
      column-gap: 12px;
    }
    .nav-item{
      flex: 0 0 auto;
      width: 120px;
      padding: 12px 8px;
      display: flex;
      flex-direction: column;
      align-items: center;
      row-gap: 8px;
      background-color: #f5f5f5;
      border-radius: 4px;
      cursor: pointer;
      transition: transform .2s, box-shadow .2s;
      &:hover{
        transform: translateY(-2px);
        box-shadow: 2px 4px 4px rgba(0, 0, 0, .1);
      }
      .nav-icon{
        width: 52px;
        height: 52px;
        border-radius: 8px;
        display: flex;
        align-items: center;
        justify-content: center;
        .anticon{
          font-size: 28px;
          color: #ffffff;
        }
      }
      .nav-title{
        max-width: 100%;
        font-size: 13px;
        color: #333333;
        text-align: center;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
        transition: color .2s;
      }
      &:hover .nav-title{
        color: var(--nav-color, $primary-color);
      }
      &.nav-item--hollow{
        .nav-icon{
          background-color: transparent;
          .anticon{
            font-size: 46px;
            color: var(--nav-color, $primary-color);
          }
        }
        .nav-title{
          color: var(--nav-color, $primary-color);
        }
      }
    }
  }
  // 配置弹窗内入口编辑行
  .item-row{
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
      width: 28px;
      height: 28px;
      padding: 0;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      .anticon{
        font-size: 14px;
        line-height: 1;
      }
    }
    .color-btn{
      flex: 0 0 auto;
      width: 28px;
      height: 28px;
      padding: 0;
      border-radius: 4px;
      border: 1px solid rgba(0, 0, 0, .15);
    }
    .hollow-flag{
      flex: 0 0 auto;
      display: inline-flex;
      align-items: center;
      column-gap: 4px;
      font-size: 12px;
      color: rgba(0, 0, 0, .65);
      white-space: nowrap;
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
  // 背景色选择器
  .color-picker{
    display: flex;
    flex-direction: row;
    flex-wrap: wrap;
    gap: 8px;
    max-width: 176px;
    .color-dot{
      width: 22px;
      height: 22px;
      border-radius: 50%;
      cursor: pointer;
      border: 1px solid rgba(0, 0, 0, .12);
      box-sizing: border-box;
      transition: transform .15s;
      &:hover{
        transform: scale(1.15);
      }
      &.active{
        box-shadow: 0 0 0 2px #ffffff, 0 0 0 3px #333333;
      }
    }
  }
</style>
