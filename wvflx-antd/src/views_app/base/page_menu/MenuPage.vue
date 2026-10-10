<template>
  <div>
    <div v-show="!pageVisible" :bordered="false" class="page menu-page">
      <a-card :bordered="false" class="page-block">
        <a-layout>
          <a-layout-content
            style="background-color: #fff; padding: 10px;">
            <!-- 表格部分 -->
            <div class="page-table-wrapper">
              <!-- 添加菜单 -->
              <div v-if="activeView">
                <a @click="onRecordAdd(null)">
                  <a-icon type="plus" />
                  添加菜单
                </a>
                <a-divider/>
              </div>
              <NestedTree
                :maxDepth="3"
                :levelDrag="false"
                :itemList="cptViewMenus"
                @addItem="onRecordAdd"
                @editItem="onRecordEdit"
                @deleteItem="doRecordDelete"
                @dragChange="onRecordChanged"
                @toggleActive="onToggleActive">
              </NestedTree>
            </div>
          </a-layout-content>
          <a-layout-sider
            v-if="stores.isDesktop()"
            :width="300"
            theme="light"
            style="margin-left: 10px; background-color: #fff; padding: 10px;">
            <!-- 说明部分 -->
            <div>
              <h3>菜单说明</h3>
              <p>可以拖拽菜单完成排序</p>
              <p>菜单操作仅作用在当前视图</p>
              <a-divider/>
              <h3>当前选中</h3>
              <p>{{ _.get(activeItem, 'title', '未选中') }}</p>
              <p v-if="_.get(activeItem, 'bindHref')">
                <a :href="_.get(activeItem, 'bindHref')" target="_blank">链接地址</a>
              </p>
              <a-divider/>
              <!--<p>菜单JSON</p>
              <pre>{{ JSON.stringify(activeItem, null, 2) }}</pre>-->
            </div>
          </a-layout-sider>
        </a-layout>
      </a-card>
    </div>

    <!-- 弹窗部分 -->
    <GFormPop
      ref="slotAdd"
      :popVisible="popAddVisible"
      :configObj="popConfig.addConfig">
    </GFormPop>
    <GFormPop
      ref="slotEdit"
      :popVisible="popEditVisible"
      :configObj="popConfig.editConfig">
    </GFormPop>
  </div>
</template>

<script>
  import ALayout from "ant-design-vue/lib/layout"

  import GFormPop from '@/components/page_pop/GFormPop'
  import NestedTree from '@/components/comp_tools/NestedTree'

  import MetaViewMenu from '@/store/models/MetaViewMenu'

  import { mapGetters } from "vuex"
  import { GETTERS } from '@/constants'
  import { stores } from '@/utils/util'
  import { ormSeqOrders } from '@/api/api'
  import popConfig from './configs/popConfig'

  export default {
    name: "MenuPage",
    components: {
      ALayout: ALayout,
      ALayoutSider: ALayout.Sider,
      ALayoutContent: ALayout.Content,
      NestedTree,
      GFormPop,
    },
    data(){
      return {
        stores: stores,
        popConfig: popConfig,
        popAddVisible: false,
        popEditVisible: false,
        activeItem: {},
      }
    },
    computed:{
      ...mapGetters({
        activeView: GETTERS.GET_META_ACTIVE_VIEW,
      }),
      pageVisible: function() {
        return this.popAddVisible || this.popEditVisible
      },
      cptViewMenus: function(){
        let that = this
        let viewId = that._.get(that.activeView, 'id')
        let allMenus = MetaViewMenu.query().where('metaViewId', viewId).orderBy('seqOrder').orderBy('id').get()
        // 获取root节点
        let rootMenus = []
        allMenus.forEach(function(menu) {
          if(menu.parentId === 0){
            rootMenus.push({
              id: menu['id'],
              pId: menu['parentId'],
              key: `menu${menu['id']}`,
              title: menu['title'],
              icon: menu['icon'],
              bindHref: menu['bindHref'],
              remark: menu['remark'],
            })
          }
        })
        that._buildMenuTree(rootMenus, allMenus)
        return rootMenus
      }
    },
    methods: {
      /** 弹窗操作 */
      onRecordAdd(parent){
        let that = this
        let slotRef = that.$refs.slotAdd
        that.popAddVisible = slotRef.isPage()
        slotRef.showPop(null, parent,
          (record, context) => { /* submit */
            let parent = context
            let viewId = that._.get(that.activeView, 'id')
            let pId = parent ? parent.id : 0
            let siblingMenus = MetaViewMenu.query().where('metaViewId', viewId).where('parentId', pId).get()
            let maxSeqOrder = siblingMenus.length? Math.max(...siblingMenus.map(m => m.seqOrder != null ? m.seqOrder : 0)) : 1
            let promise = MetaViewMenu.$create({
              data: {
                parentId: parent ? parent.id : null,
                title: record.title,
                icon: record.icon?record.icon:'',
                seqOrder: maxSeqOrder + 1,
                bindHref: '',
                metaViewId: viewId
              }
            })
            promise.then(() => that.popAddVisible = false)
            return promise
          }, () => { /* cancel */
            that.popAddVisible = false
          }
        )
      },
      onRecordEdit(record, parent) {
        let that = this
        let slotRef = that.$refs.slotEdit
        that.popEditVisible = slotRef.isPage()
        slotRef.showPop(record, parent,
          (record, context) => { /* submit */
            context || true
            let promise = MetaViewMenu.$update({
              params: {
                id: record.id
              },
              data: {
                title: record.title,
                icon: record.icon,
                remark: record.remark,
              }
            })
            promise.then(() => that.popEditVisible = false)
            return promise
          }, () => { /* cancel */
            that.popEditVisible = false
          }
        )
      },
      doRecordDelete(item){
        MetaViewMenu.$delete({
          params: {
            id: item.id
          }
        })
      },
      /** 扩展方法 */
      onToggleActive(item){
        this.activeItem = item
      },
      onRecordChanged(pItem, cItem, newIndex){
        // 子菜单列表
        let that = this
        let pId = that._.defaultTo(that._.get(pItem, 'id'), 0)
        let viewId = that._.get(that.activeView, 'id')
        let childMenus = MetaViewMenu.query().where('metaViewId', viewId).where('parentId', pId).orderBy('seqOrder').orderBy('id').get()
        let mappedMenus = childMenus.map(item => {
          return {
            id: item.id,
            pId: item.parentId,
            title: item.title,
            seqOrder: item.seqOrder
          }
        })
        // 移动子元素
        let oldIndex = mappedMenus.findIndex( item => item.id === cItem.id)
        if(oldIndex !== -1){
          mappedMenus.splice(oldIndex, 1)
        }
        mappedMenus.splice(newIndex, 0, {
          id: cItem.id,
          pId: cItem.pId,
          title: cItem.title,
          seqOrder: cItem.seqOrder
        })
        // 更新排序
        let itemOrders = []
        mappedMenus.forEach((item, index) => {
          let itemData = {id: item.id, seqOrder: index+1}
          if(item.pId !== pId){ // 防止触发乐观锁
            itemData['parentId'] = pId
          }
          // MetaViewMenu.$update({ params: { id: item.id }, data: itemData })
          itemOrders.push(itemData)
        })
        ormSeqOrders('metaViewMenu', {items: itemOrders}).then(resp => {
          let entitys = resp.gmodel
          MetaViewMenu.insertOrUpdate({
            data: entitys
          })
        })
      },
      /** 工具方法 */
      _buildMenuTree(menus, allMenus) {
        let that = this
        menus.forEach(function(pMenu) {
          let children = []
          allMenus.forEach(function(menu) {
            if(menu.parentId === pMenu.id){
              children.push({
                id: menu['id'],
                pId: menu['parentId'],
                key: `menu${menu['id']}`,
                title: menu['title'],
                icon: menu['icon'],
                bindHref: menu['bindHref'],
                remark: menu['remark'],
              })
            }
          })
          // NOTE: 全部都有children字段
          pMenu.children = children.length>0?children:[]
          if(children.length > 0) {
            that._buildMenuTree(children, allMenus)
          }
        })
      },
    }
  }
</script>


<style lang="scss" scoped>
  .menu-page{
    background-color: #f3f4f6;
    /deep/ .ant-card-body{
      padding: 0;
    }
  }
</style>
