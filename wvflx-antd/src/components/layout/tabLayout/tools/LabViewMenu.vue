<template>
  <div class="header-dropdown-wrapper">
    <!-- 下拉菜单 -->
    <a-dropdown v-model="menuVisible">
      <a class="header-dropdown-link">
        <a-icon :type="_.get(cptActiveView, 'icon', 'edit')"></a-icon>
        <span>{{ _.get(cptActiveView, 'name', '暂无视图') }}</span>
      </a>
      <a-menu 
        slot="overlay" 
        class="header-dropdown-menu" 
        @click="onMenuClick">
        <a-menu-item v-for="item in cptViews" :key="item.id">
          <span :style="{color: item.type===1?'#039267':'#87d068'}">
            <a-icon :type="item.icon" /> {{ item.name }}
          </span>
          <div class="ops-btn">
            <a-icon type="edit" @click.stop="onRecordEdit(item)"/>
            <a-divider type="vertical" />
            <a-icon type="copy" @click.stop="onRecordCopy(item)"/>
            <a-divider type="vertical" />
            <a-icon type="close" @click.stop="doRecordDelete(item)"/>
          </div>
        </a-menu-item>
        <a-menu-divider/>
        <a-menu-item key="add">
          <a-icon type="plus"/>
          <span>新增视图</span>
        </a-menu-item>
      </a-menu>
    </a-dropdown>

    <!-- 弹窗部分 -->
    <GFormPop
      ref="slotAdd"
      :configObj="popConfig.addConfig">
    </GFormPop>
    <GFormPop
      ref="slotEdit"
      :configObj="popConfig.editConfig">
    </GFormPop>
  </div>
</template>

<!--
# 组件说明
  视图管理菜单，新增、编辑、拷贝、删除视图等功能；
  从MetaView中读取数据，视图默认不包含Page页面；
  支持异步拷贝视图，快速创建拷贝版本视图；
# 配置组件
  暂无
# 操作方法
  暂无
-->

<script>
  import { tools } from '@/utils/util'
  import { ormCopyView, ormSeqOrders } from '@/api/api'
  import { mapActions, mapGetters } from 'vuex'
  import { ACTIONS, GETTERS } from '@/constants'

  import MetaView from '@/store/models/MetaView'
  import MetaViewMenu from '@/store/models/MetaViewMenu'
  import MetaPage from '@/store/models/MetaPage'
  import MetaPageComponent from '@/store/models/MetaPageComponent'
  import MetaPageComponentField from '@/store/models/MetaPageComponentField'

  import GFormPop from '@/components/page_pop/GFormPop'
  import popConfig from './labViewMenu/popConfig'

  export default {
    name: "LabViewMenu",
    components:{
      GFormPop,
    },
    computed:{
      ...mapGetters({
        activeView: GETTERS.GET_META_ACTIVE_VIEW,
      }),
      cptViews: function(){
        return MetaView.query().orderBy('seqOrder', 'asc').orderBy('id', 'asc').get()
      },
      cptActiveView: function(){
        let that = this
        let viewId = that._.get(that.activeView, 'id')
        return MetaView.find(viewId)
      },
    },
    data () {
      return {
        menuVisible: false,
        popConfig: popConfig,
        copyViewName: '',
      }
    },
    methods: {
      ...mapActions({
        setActiveView: ACTIONS.SET_META_ACTIVE_VIEW,
      }),
      onMenuClick({ key }){
        let that = this
        if(key === 'add') {
          that.onRecordAdd()
        }else{
          let selectedView = MetaView.find(key)
          that.setActiveView(selectedView).then(()=>{
            // 改为 TabLayout 中 watch 观察 activeView
            // that.$router.push({ name: 'root' }).catch(err => { err || true });
          })
        }
        that.menuVisible = false
      },
      /* 模板操作 */
      onRecordAdd(record){
        let that = this
        let slotRef = that.$refs.slotAdd
        slotRef.showPop(record, {},
          (record, context) => { /* submit */
            context || true
            let promise = MetaView.$create({
              data: record
            })
            promise.then((resp) => {
              that.setActiveView(resp).then(()=>{
                // 改为 TabLayout 中 watch 观察 activeView
                // that.$router.push({ name: 'root' }).catch(err => { err || true });
              })
            })
            return promise
          }, () => { /* cancel */ }
        )
      },
      onRecordEdit(record) {
        let that = this
        that.menuVisible = false
        let slotRef = that.$refs.slotEdit
        slotRef.showPop(record, {},
          (record, context) => { /* submit */
            context || true
            let promise = MetaView.$update({
              params: {
                id: record.id
              },
              data: record
            })
            promise.then(() => {
              // 置顶视图
              if(record.top){
                let activeViews = MetaView.query().orderBy('seqOrder', 'asc').orderBy('id', 'asc').get()
                let mappedItems = activeViews.map(item => {
                  return {
                    id: item.id,
                    seqOrder: item.seqOrder
                  }
                })
                let itemIndex = mappedItems.findIndex( item => item.id === record.id)
                if(itemIndex !== -1){
                  let itemsArray = mappedItems.splice(itemIndex, 1)
                  mappedItems.unshift(itemsArray[0])
                }
                let itemOrders = []
                mappedItems.forEach((item, index) => {
                  let itemData = {id: item.id, seqOrder: index+1}
                  itemOrders.push(itemData)
                })
                ormSeqOrders('metaView', {items: itemOrders}).then(resp => {
                  let entitys = resp.gmodel
                  MetaView.insertOrUpdate({
                    data: entitys
                  })
                })
              }
            })
            return promise
          }, () => { /* cancel */
            that.popEditVisible = false
          }
        )
      },
      onRecordCopy(item){
        let that = this
        that.menuVisible = false
        that.copyViewName = null
        that.$confirm({
          title: `确定拷贝视图 ${item.name}?`,
          content: <a-input
            placeholder="新视图名称(默认随机)"
            onChange={ e => that.copyViewName = e.target.value }
          />,
          onOk() {
            that.doRecordCopy(item)
          }
        });
      },
      async doRecordCopy(item) {
        let that = this
        that.$message.success(`[${item.name}]拷贝中，请稍后 ...`);
        await ormCopyView({
          viewId: item.id,
          copyViewName: that.copyViewName || `拷贝${tools.randomString(5)}`,
        }).then(resp => {
          let metaEntitys = resp.gmodel
          /* 视图信息 */
          MetaView.insertOrUpdate({
            data: that._.get(metaEntitys, 'metaView', [])
          })
          MetaViewMenu.insertOrUpdate({
            data: that._.get(metaEntitys, 'metaViewMenu', [])
          })
          /* 页面信息 */
          MetaPage.insertOrUpdate({
            data: that._.get(metaEntitys, 'metaPage', [])
          })
          MetaPageComponent.insertOrUpdate({
            data: that._.get(metaEntitys, 'metaPageComponent', [])
          })
          MetaPageComponentField.insertOrUpdate({
            data: that._.get(metaEntitys, 'metaPageComponentField', [])
          })
          that.setActiveView(metaEntitys['metaView'][0]).then(()=>{
            // 改为 TabLayout 中 watch 观察 activeView
            // that.$router.push({ name: 'root' }).catch(err => { err || true });
          })
          that.$message.success(`[${item.name}]拷贝完成`);
        }).catch(err => {
          err || true
          that.$message.warn(`[${item.name}]拷贝失败`);
        })
      },
      doRecordDelete(item){
        let that = this
        that.menuVisible = false
        that.$confirm({
          title: `确定删除视图 ${item.name}?`,
          content: '请谨慎操作，点击确定按钮删除视图',
          onOk() {
            that._doRecordDelete(item)
          }
        });
      },
      _doRecordDelete(item){
        let that = this
        let activeId = that._.get(that.activeView, 'id')
        MetaView.$delete({
          params: {
            id: item.id
          }
        }).then(resp => {
          // NOTE: 激活首个模板
          if(`${activeId}` === `${resp.id}`){
            let templates = MetaView.all()
            that.setActiveView(templates[0] || {}).then(()=>{
              // 改为 TabLayout 中 watch 观察 activeView
              // that.$router.push({ name: 'root' }).catch(err => { err || true });
            })
          }
        });
        that.$message.success(`成功删除视图[${item.name}]`);
      },
    },
  }
</script>

<style lang="scss" scoped>
  /* 菜单样式修正 */
  .header-dropdown-wrapper {
    display: inline-block;
    height: 100%;
    .header-dropdown-link {
      padding: 10px 14px;
      height: 100%;
      .anticon{
        margin-right: 6px;
      }
      &:hover {
        background: rgba(255, 255, 255, 0.3);
      }
    }
  }
  .header-dropdown-menu {
    color: #fff;
    padding: 4px 0;
    .ant-dropdown-menu-item {
      width: 190px;
      position: relative;
      // 操作按钮
      .ops-btn {
        display: none;
        float: right;
        color: gray;
        padding-right: 5px;
        position: absolute;
        right: 5px;
        .anticon {
          cursor: pointer;
          font-size: 14px;
          /*padding: 4px;*/
          &:hover{
            color: #1f1f1f;
          }
        }
      }
    }
    .ant-dropdown-menu-item-active{
      .ops-btn{
        display: inline-block;
      }
    }
  }
</style>