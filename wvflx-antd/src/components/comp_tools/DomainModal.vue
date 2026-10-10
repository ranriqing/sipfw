<template>
  <a-modal
    title="数据表格"
    :visible="visible"
    :width="800"
    :bodyStyle="{
      'height': '500px',
      'overflow-y':'auto'
    }"
    @ok="onSubmit"
    @cancel="doClose">
    <div v-for="category in groupedCategories" :key="'cat-' + category.catId" class="category-section">
      <div class="category-header">
        <h4 class="category-title">{{ category.title }}</h4>
        <a-button v-if="category.catId" size="small" icon="plus" @click="onCategoryAdd(category.catId)">新增</a-button>
      </div>
      <a-divider />
      <draggable
        tag="div"
        class="item-container"
        v-bind="cptDragOptions"
        :list="category.records"
        @change="onDragChange($event, category)">
        <a
          v-for="(item) in category.records"
          :key="item.id"
          :class="{'domain-avatar':true, 'active':_.get(entityItem, 'id') === item.id}"
          :title="item.domainName"
          @click="entityItem = item">
          <!-- Id小于0的为系统内置Domain，内置Domain不允许编辑和删除 -->
          <a-avatar :size="65" shape="square" :style="{color: item.codeEntity?'rgba(250, 140, 22, 0.8)':undefined}" :icon="item.codeEntity?'file-sync':'file-text'" />
          <div class="avatar-title" :style="{color: item.id<0?'gray':'black'}">{{ item.title }}</div>
          <div v-if="item.id>0" class="opt-item-action opt-item-view" @click.stop="onRecordEdit(item)">
            <a-icon type="edit"/>
          </div>
          <a-popconfirm v-if="item.id>0" title="确定删除吗?" @confirm="() => doRecordDelete(item)">
            <div class="opt-item-action opt-item-delete">
              <a-icon type="delete"/>
            </div>
          </a-popconfirm>
          <div class="opt-item-action opt-item-drag drag-item-widget">
            <a-icon type="g-drag" />
          </div>
        </a>
      </draggable>
    </div>
    <template slot="footer">
      <div class="flex flex-row">
        <a-button v-if="!groupedCategories.some(function(c) { return c.catId })" type="dashed" @click="onRecordAdd">新增数据表</a-button>
        <a-button :disabled="!entityItem" type="dashed" @click="onToRoute('modelField', {domainId: entityItem.id})">【{{ entityItem?entityItem.title:'表格' }}】字段</a-button>
        <div class="item-right">
          <a-button style="margin-left: 8px" @click="doClose">关闭</a-button>
        </div>
      </div>
    </template>

    <!-- Slot弹窗 -->
    <GFormPop
      ref="slotAdd"
      :configObj="entityPopConfig.addConfig">
    </GFormPop>
    <GFormPop
      ref="slotEdit"
      :configObj="entityPopConfig.editConfig">
    </GFormPop>
  </a-modal>
</template>

<!--
# 组件说明
  数据表格Modal弹窗，展示和管理全部数据表格，按sciDomainCategory分类展示；
  id小于0为内部数据表，内部数据表不允许编辑和删除；
  支持跨分类拖拽，自动更新分类归属和排序；
  删除前校验关联页面，确认将删除数据表和多个关联页面；
# 操作方法
  $emit('popSubmit', that.entityItem)
  $emit('popCancel')
-->

<script>
  import AAvatar from 'ant-design-vue/lib/avatar';

  import { tools } from '@/utils/util'
  import { postAction } from '@/api/api'
  import MetaDomain from '@/store/models/MetaDomain'
  import ScElement from '@/store/models/ScElement'
  import GFormPop from '@/components/page_pop/GFormPop'
  import entityPopConfig from './configs/entityPopConfig'

  export default {
    name: "DomainModal",
    components: {
      AAvatar,
      GFormPop,
    },
    data () {
      return {
        /** 配置&状态 */
        visible: false,
        entityPopConfig: entityPopConfig,
        /** 业务数据 */
        entityItem: null,    // 选中的EntityItem
      }
    },
    computed: {
      groupedCategories() {
        let that = this
        let categories = ScElement.query()
          .where(function(item) { return item.scConfigKey === 'sciDomainCategory' })
          .orderBy('seqOrder').orderBy('id').get()

        let catMap = {}
        let catOrder = []
        categories.forEach(function(cat) {
          let catId = that._.toString(cat.id)
          catMap[catId] = { catId: catId, title: cat.title, records: [] }
          catOrder.push(catId)
        })

        let domainEntities = MetaDomain.query()
          .where(function(item) { return item.visible === true })
          .orderBy('seqOrder', 'asc').orderBy('id', 'asc')
          .get()
          .map(function(item) {
            return {
              id: item.id,
              title: item.title,
              domainName: item.domainName,
              codeEntity: item.codeEntity,
              sciDomainCategory: item.sciDomainCategory,
              seqOrder: item.seqOrder,
              remark: item.remark,
              dateCreated: item.dateCreated,
              lastUpdated: item.lastUpdated,
            }
          })

        domainEntities.forEach(function(record) {
          let catId = that._.toString(record.sciDomainCategory)
          if (catMap[catId]) {
            catMap[catId].records.push(record)
          } else {
            if (!catMap['__uncategorized']) {
              catMap['__uncategorized'] = { catId: null, title: '未分类', records: [] }
              catOrder.push('__uncategorized')
            }
            catMap['__uncategorized'].records.push(record)
          }
        })

        catOrder.forEach(function(catId) {
          catMap[catId].records.sort(function(a, b) { return (a.seqOrder || 100) - (b.seqOrder || 100) })
        })

        return catOrder.map(function(catId) { return catMap[catId] })
      },
      cptDragOptions() {
        return {
          animation: 200,
          group: "domainCategory",
          ghostClass: "ghost",
          handle: '.drag-item-widget',
        };
      },
    },
    methods: {
      /** 弹窗操作 */
      showPop() {
        let that = this
        that.entityItem = null
        that.visible = true
      },
      onSubmit() {
        let that = this
        that.$emit('popSubmit', that.entityItem);
      },
      doClose () {
        this.$emit('popCancel');
        this.visible = false
      },
      /** Entity操作 */
      onRecordAdd(){
        let that = this
        let slotRef = that.$refs.slotAdd
        slotRef.showPop({domainName: that._.capitalize(tools.randomString(8))}, {},
          (record, context) => { /* submit */
            return that.doRecordSave(record, context)
          }, () => { /* cancel */ }
        )
      },
      onCategoryAdd(catId){
        let that = this
        let slotRef = that.$refs.slotAdd
        slotRef.showPop({
          domainName: that._.capitalize(tools.randomString(8)),
          sciDomainCategory: parseInt(catId)
        }, {},
          (record, context) => { /* submit */
            return that.doRecordSave(record, context)
          }, () => { /* cancel */ }
        )
      },
      doRecordSave(record){
        let that = this
        let promise = MetaDomain.$create({
          data: record
        })
        promise.then(resp => {
          resp || true
          that.$message.success('数据表创建成功');
        })
        return promise
      },
      onRecordEdit(record){
        let that = this
        if(record.id<0){
          this.$message.warn('内部数据表不允许编辑');
          return
        }
        let slotRef = that.$refs.slotEdit
        slotRef.showPop(record, {},
          (record, context) => { /* submit */
            return that.doRecordUpdate(record, context)
          }, () => { /* cancel */ }
        )
      },
      doRecordUpdate(record){
        let that = this
        if(record.id<0){
          that.$message.warn('内部数据表不允许更新');
          return
        }
        let promise = MetaDomain.$update({
          params: {
            id: record.id,
          },
          data: record
        })
        promise.then(resp => {
          resp || true
          that.$message.success('更新成功');
        })
        return promise
      },
      doRecordDelete(record){
        let that = this
        if(record.id < 0){
          that.$message.warn('内部数据表不允许删除');
          return
        }
        let promise = MetaDomain.$delete({
          params: {
            id: record.id
          }
        })
        promise.then(() => {
          that.$message.success(`删除数据表[${record.title}]成功`);
        })
      },
      onDragChange(changeEvent, category) {
        // vuedraggable 已按拖拽结果重排 category.records，直接以其当前顺序生成新的 seqOrder
        function reorderAndSave() {
          let items = category.records.map(function(r, idx) {
            return {
              id: r.id,
              seqOrder: idx + 1,
              sciDomainCategory: r.sciDomainCategory,
            }
          })

          postAction('/metaDomain/seqOrders', {items: items}).then(function(resp) {
            MetaDomain.insertOrUpdate({
              data: resp.gmodel
            })
          })
        }

        if (changeEvent.moved) {
          reorderAndSave()
        }
        if (changeEvent.added) {
          let movedItem = changeEvent.added.element
          movedItem.sciDomainCategory = category.catId ? parseInt(category.catId) : null
          MetaDomain.insertOrUpdate({ data: [movedItem] })
          reorderAndSave()
        }
      },
      /** 扩展操作 */
      onToRoute(routeName, params){
        let that = this
        // 添加query参数防止NavigationDuplicated
        that.$router.replace({ name: routeName, params: params||{}, query:{q: params['domainId'] || 0} }).then(()=>{
          that.visible = false
        })
      },
    },
  }
</script>

<style lang="scss" scoped>
  /* 分类区域 */
  .category-section {
    margin-bottom: 16px;
  }
  .category-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 4px;
  }
  .category-title {
    font-size: 15px;
    font-weight: 600;
    color: rgba(0, 0, 0, 0.55);
    margin-bottom: 0;
  }
  /* 图标样式 */
  .item-container {
    min-height: 60px;
    &:empty:before {
      content: "拖拽数据表至此分类";
      font-size: 13px;
      color: #bfbfbf;
      text-align: center;
      display: block;
      padding: 20px;
      border: 1px dashed #d9d9d9;
      border-radius: 4px;
    }
  }
  .domain-avatar{
    display: inline-block;
    position: relative;
    .ant-avatar{
      margin: 10px 10px 0 10px;
    }
    .avatar-title{
      text-align: center;
      color: rgba(0, 0, 0, 0.85);
    }
    // 新增样式
    .avatar-plus{
      display: inline-flex !important;
      align-items: center;
      justify-content: center;
      color: #ccc;
      background-color: transparent;
      border: 2px solid #ccc;
      &:hover{
        color: #ddd;
        border: 2px solid #ddd;
      }
    }
    // 激活样式
    &.active{
      .ant-avatar{
        background-color:#87d068;
      }
      .avatar-title{
        color:#87d068;
      }
    }
    // 操作按钮
    .opt-item-action{
      position: absolute;
      height: 22px;
      line-height: 22px;
      z-index: 100;
      opacity: 0;
      i.anticon{
        font-size: 16px;
        font-weight: bold;
        color: #eee;
        margin: 0 3px;
        cursor: pointer;
      }
      &:hover{
        i.anticon{
          transform: scale(1.1);
          color: #fff;
        }
      }
    }
    .opt-item-view{
      right: 10px;
      top: 10px;
      border-radius: 0 5px 0 0;
    }
    .opt-item-delete{
      right: 10px;
      bottom: 20px;
      border-radius: 0 0 5px 0;
    }
    .opt-item-drag{
      left: 10px;
      top: 10px;
      border-radius: 0 5px 0 0;
    }
    &:hover {
      .opt-item-action{
        opacity: 1;
      }
    }
  }
  /* Ghost样式 */
  .ghost {
    opacity: 0.4;
  }
</style>
