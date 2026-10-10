<template>
  <div>
    <GPlainPop
      ref="slotPop"
      class="pop-wrapper"
      :popVisible="popVisible"
      :configObj="configObj"
      :sContext="sContext"
      :data-cptScElements="cptScElements"
      :data-datTreeElements="cptTreeElements"
      debugComp="GScelPop">
      <template #default="{record, context}">
        <div class="draggable-cols-container">
          <!-- 多层选项 -->
          <ScelTree
            :maxDepth="2"
            :levelDrag="false"
            :allowRemove="_.get(context, 'allowRemove')"
            :allowModify="_.get(record, 'allowModify')"
            :multiLevel="_.get(record, 'multiLevel')"
            :tagStyles="popConfig.otherConfig.tagStyles"
            :itemList="datTreeElements"
            :itemStyle="{
              'border-radius': '5px',
              'border': '1px solid #e5e5e5',
              'background-color': '#f9f9fa',
              'margin-bottom': '6px',
            }"
            @addItem="onRecordAdd"
            @editItem="onRecordEdit"
            @deleteItem="doRecordDelete"
            @dragChange="onRecordChanged">
          </ScelTree>
          <!-- 非多层选项 -->
          <draggable
            tag="div"
            v-if="false"
            :list="datScElements"
            v-bind="cptDragTargetOptions"
            @change="(changeEvent) => onRecordDrag(changeEvent)">
            <div
              class="draggable-col"
              v-for="(item, index) in datScElements"
              :key="index">
              <div>
                <a-icon :title="item.id" type="g-field-collapse" class="drag drag-widget"/>
                <a-dropdown class="column-item">
                  <a-tag v-if="!item.tagStyle" class="sc-tag" style="border: none;">
                    {{ item.title }}
                  </a-tag>
                  <a-tag v-else-if="item.tagStyle==='default'" class="sc-tag" style="border: none;">
                    {{ item.title }}
                  </a-tag>
                  <a-tag v-else :color="item.tagStyle" class="sc-tag">
                    {{ item.title }}
                  </a-tag>
                  <a-menu slot="overlay" @click="(event) => onMenuClick(item, event)">
                    <a-menu-item v-for="(styleItem) in popConfig.otherConfig.tagStyles" :key="styleItem.caption">
                      <a-tag v-if="styleItem.color==='default'" style="border: none;">{{ styleItem.caption }}</a-tag>
                      <a-tag v-else :color="styleItem.color">{{ styleItem.caption }}</a-tag>
                    </a-menu-item>
                  </a-menu>
                </a-dropdown>
              </div>
              <div class="item-actions">
                <a-icon class="icon-action" type="edit" @click="onRecordEdit(item)" :title="item.id"/>
                <template v-if="_.get(context, 'allowRemove')">
                  <a-divider type="vertical" />
                  <a-popconfirm title="确定删除?" @confirm="() => doRecordDelete(item)">
                    <a-icon class="icon-action" type="close" />
                  </a-popconfirm>
                </template>
              </div>
            </div>
          </draggable>
        </div>
        <!-- 添加按钮 -->
        <a-button
          v-if="_.get(record, 'allowModify')"
          icon="plus"
          type="dashed"
          :block="true"
          @click="onRecordAdd()">
          添加选项
        </a-button>
      </template>
    </GPlainPop>

    <!-- Slot弹窗(App模式) -->
    <GFormPop
      ref="slotPopAdd"
      :configObj="popConfig.addConfig">
    </GFormPop>
    <GFormPop
      ref="slotPopEdit"
      :configObj="popConfig.editConfig">
    </GFormPop>
  </div>
</template>

<script>
  import AAlert from 'ant-design-vue/lib/alert';
  import AEmpty from 'ant-design-vue/lib/empty';
  import AAvatar from 'ant-design-vue/lib/avatar';
  import ACheckbox from 'ant-design-vue/lib/checkbox';

  import { vDftUtil } from '@/utilsv/vUtil'
  import { postAction } from '@/api/api'
  import { stores } from '@/utils/util'

  import GPlainPop from '@/components/page_pop/GPlainPop'
  import GFormPop from '@/components/page_pop/GFormPop'
  import GDomainCols from '@/components/comp_form/GDomainCols'

  import appScelPopConfig from './configs/appScelPopConfig'
  import ScElement from '@/store/models/ScElement'
  import ScelTree from '@/components/comp_tools/ScelTree.vue'

  export default {
    name: "GScelPop",
    components: {
      ScelTree,
      AAlert,
      AEmpty,
      AAvatar,
      ACheckbox,
      GPlainPop,
      GFormPop,
      GDomainCols,
    },
    /* eslint-disable */
    props: {
      popVisible: {
        type: Boolean,
        default: false,
      },
      configObj: {
        type: Object,
        default: function(){
          return {}
        }
      },
      sContext: {
        type: Object,
        default: function(){
          return {}
        }
      },
    },
    data () {
      return {
        /** 配置&状态 */
        form: this.$form.createForm(this),
        loading:false,
        stores: stores,
        vDftUtil: vDftUtil,
        popConfig: appScelPopConfig,
        /** 数据部分 */
        record: {},
        context: {},
        datScElements: [],  // 镜像cptScElements
        datTreeElements: [],  // 镜像cptScElements
      }
    },
    computed: {
      cptLabelCol: function(){
        return this.configObj.labelCol || {
          /*xs: { span: 24 },
          sm: { span: 5 }*/
        }
      },
      cptWrapperCol: function(){
        return this.configObj.wrapperCol || {
          /*xs: { span: 24 },
          sm: { span: 15 }*/
        }
      },
      cptDragTargetOptions: function() {
        return {
          animation: 200,
          group: 'cols',
          ghostClass: 'ghost',
          handle: '.draggable-col',
        };
      },
      cptScElements: function(){
        let that = this
        let scelQuery = ScElement.query()
          .where(item => item.scConfigId === that.record?.id)
        let scElements = scelQuery.orderBy('seqOrder', 'asc')
          .orderBy('id', 'asc').with('relScConfig').get()
        that.datScElements = scElements.map(item => that._cloneItemFunc(that, item))
        return scElements
      },
      cptTreeElements: function(){
        let that = this
        let multiLevel = that._.get(that.record, 'multiLevel')
        let scelQuery = ScElement.query()
          .where(item => item.scConfigId === that.record?.id)
        let allElements = scelQuery.orderBy('seqOrder').orderBy('id').get()
        // 获取root节点
        let rootElements = []
        allElements.forEach(function(elem) {
          if(elem.parentId === 0){
            rootElements.push({
              id: elem['id'],
              pId: elem['parentId'],
              key: `elem${elem['id']}`,
              title: elem['title'],
              icon: 'g-field-collapse',
              tagStyle: elem['tagStyle'],
              remark: elem['description'],
            })
          }
        })
        if(multiLevel){
          that._buildElemTree(rootElements, allElements)
        }
        that.datTreeElements = rootElements
        return rootElements
      }
    },
    methods: {
      /** 弹窗操作 */
      isPage(){
        let that = this
        let slotRef = that.$refs.slotPop
        return slotRef.isPage()
      },
      showPop (record, context, submitCallback, cancelCallback) {
        let that = this
        that.form.resetFields()
        that.record = that._.pickBy(record || {}, (value, key) =>
          !that._.isNil(value) && !key.startsWith('$') && !that._.isFunction(value)
        )
        that.record = record || {}
        that.context = context || {}
        // 展示弹窗
        let slotRef = that.$refs.slotPop
        return slotRef.showPop(record, context,
          (record, context) => { /* submit */
            record || context || true
            // 返回Promise
            return submitCallback(that.record, that.context)
          }, cancelCallback)
      },
      onRecordAdd(parent){
        let that = this
        let slotRef = that.$refs.slotPopAdd
        slotRef.showPop(null, parent,
          (record, context) => { /* submit */
            let parent = context
            let parentId = parent?parent.id:0
            let scConfigId = that._.get(that.record, 'id')
            let elemArray = ScElement.query()
              .where(item => item.parentId === parentId).get()
            let maxSeqOrder = elemArray.length>0?Math.max(...elemArray.map(item => item['seqOrder'])):0
            let promise = ScElement.$create({
              data: {
                parentId: parent?parent.id:null,
                title: record.title,
                seqOrder: maxSeqOrder+1,
                dfault: false,
                tagStyle: undefined,
                description: record.description,
                scConfigId: scConfigId,
              }
            })
            return promise
          }, () => { /* cancel */ }
        )
      },
      onRecordEdit: function(record, parent){
        let that = this
        let slotRef = that.$refs.slotPopEdit
        slotRef.showPop(record, parent,
          (record, context) => { /* submit */
            context || true
            let promise = ScElement.$update({
              params: {
                id: record.id,
              },
              data: {
                title: record.title,
                description: record.description
              }
            })
            return promise
          }, () => { /* cancel */ }
        )
      },
      doRecordDelete(item){
        return ScElement.$delete({
          params: {
            id: item.id,
          }
        })
      },
      /** 扩展方法 */
      onMenuClick(elItem, event) {
        let that = this
        let styleObj = that.popConfig.otherConfig.tagStyles.find( styleItem => styleItem.caption === event.key)
        if(styleObj){
          ScElement.$update({
            params: {
              id: elItem.id,
            },
            data: {
              tagStyle: styleObj['color'],
            }
          })
        }
      },
      onRecordDrag(changeEvent){
        let that = this
        // 不能用that.datScElements，拖拽时排序已经改变
        let scelQuery = ScElement.query()
          .where(item => item.scConfigId === that.record?.id)
        let scElements = scelQuery.orderBy('seqOrder', 'asc').orderBy('id', 'asc').get()
        let mappedItems = scElements.map(item => {
          return {
            id: item.id,
            name: item.name,
            seqOrder: item.seqOrder
          }
        })
        if(changeEvent['moved']){
          let oldIndex = that._.get(changeEvent, 'moved.oldIndex')
          let newIndex = that._.get(changeEvent, 'moved.newIndex')
          let item = mappedItems[oldIndex]
          mappedItems.splice(oldIndex, 1)
          mappedItems.splice(newIndex, 0, item)
          let itemOrders = []
          mappedItems.forEach((item, index) => {
            let itemData = {id: item.id, seqOrder: index+1}
            itemOrders.push(itemData)
          })
          postAction('/scElement/seqOrders', {items: itemOrders}).then(resp => {
            let entitys = resp.gmodel
            ScElement.insertOrUpdate({
              data: entitys
            })
          })
        }
        if(changeEvent['added']){
          changeEvent || null
          // do nothing
        }
      },
      onRecordChanged(pItem, cItem, newIndex){
        // 子菜单列表
        let that = this
        let pId = that._.defaultTo(that._.get(pItem, 'id'), 0)
        let childElements = ScElement.query()
          .where(item => item.scConfigId === that.record?.id).where('parentId', pId).orderBy('seqOrder').orderBy('id').get()
        let mappedItems = childElements.map(item => {
          return {
            id: item.id,
            pId: item.parentId,
            title: item.title,
            seqOrder: item.seqOrder
          }
        })
        // 移动子元素
        let oldIndex = mappedItems.findIndex( item => item.id === cItem.id)
        if(oldIndex !== -1){
          mappedItems.splice(oldIndex, 1)
        }
        mappedItems.splice(newIndex, 0, {
          id: cItem.id,
          pId: cItem.pId,
          title: cItem.title,
          seqOrder: cItem.seqOrder
        })
        // 更新排序
        let itemOrders = []
        mappedItems.forEach((item, index) => {
          let itemData = {id: item.id, seqOrder: index+1}
          if(item.pId !== pId){ // 防止触发乐观锁
            itemData['parentId'] = pId
          }
          // ScElement.$update({ params: { id: item.id }, data: itemData })
          itemOrders.push(itemData)
        })
        postAction('/scElement/seqOrders', {items: itemOrders}).then(resp => {
          let entitys = resp.gmodel
          ScElement.insertOrUpdate({
            data: entitys
          })
        })
      },
      /** 工具方法 */
      _cloneItemFunc(that, item){
        let clonedItem = {
          id: item.id,
          title: item.title,
          seqOrder: item.seqOrder,
          dfault: item.dfault,
          tagStyle: item.tagStyle,
          description: item.description,
          parentId: item.parentId,
          remark: item.remark,
        }
        return clonedItem
      },
      _buildElemTree(elements, allElements) {
        let that = this
        elements.forEach(function(pElem) {
          let children = []
          allElements.forEach(function(elem) {
            if(elem.parentId === pElem.id){
              children.push({
                id: elem['id'],
                pId: elem['parentId'],
                key: `elem${elem['id']}`,
                title: elem['title'],
                tagStyle: elem['tagStyle'],
                remark: elem['description'],
              })
            }
          })
          // NOTE: 全部都有children字段
          pElem.children = children.length>0?children:[]
          if(children.length > 0) {
            that._buildElemTree(children, allElements)
          }
        })
      },
    }
  }
</script>

<style lang="scss" scoped>
  $tmpl-color-zero6: rgba(241, 139, 98, 0.6);
  $tmpl-color-zero8: rgba(241, 139, 98, 0.8);
  $tmpl-color-zero10: rgba(241, 139, 98, 1);

  .bug-icon {
    color: $tmpl-color-zero6;
    margin-right: 6px;
  }
  .pop-wrapper{
    .ant-form-item {
      margin-bottom: 4px;
      /deep/ .ant-form-item-label {
        line-height: 26px;
      }
    }
    .draggable-cols-container{
      display: flex;
      flex-direction: column;
      max-height: 460px;
      padding-right: 8px;
      overflow-y: auto;
      // 暂无组件
      &:empty{
        &:before
        {
          content:"暂无组件";
          font-size: 16px;
          color: $tmpl-color-zero6;
          text-align: center;
          display: block;
        }
      }
      // 拖拽条目
      .draggable-col{
        border-radius: 5px;
        border: 1px solid #e5e5e5;
        width: 100%;
        background-color: #f9f9fa;
        line-height: 40px;
        margin-bottom: 8px;
        display: flex;
        flex-direction: row;
        justify-content: space-between;
        font-size: 14px;
        // cursor: grab;
        .sc-tag{
          min-width: 50px;
        }
        // 条目内容
        .column-item {
          display: inline-block;
          margin-left: 6px;
        }
        .drag{
          font-size: 16px;
          padding: 4px 2px 4px 6px;
          cursor: move;
        }
        // 条目操作按钮
        .item-actions{
          opacity: 0;
          margin-right: 8px;
          .icon-action {
            vertical-align: baseline;
            font-size: 14px;
            padding: 4px 0;
            transform: translateY(2px);
            transition: all .3s;
            cursor: pointer;
            color: #606266;
          }
        }
        &:hover {
          background-color: #fafafa;
          .item-actions {
            opacity: 1;
          }
        }
      }
    }
  }
</style>