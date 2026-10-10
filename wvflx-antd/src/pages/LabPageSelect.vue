<!--suppress CheckEmptyScriptTag -->
<template>
  <a-card v-if="stores.isLabState()" style="margin-bottom: 15px;" :bordered="false">
    <div class="flex flex-row main-between">
      <!-- 选择页面 -->
      <div style="display: inline-block;">
        <slot>暂无页面</slot>
        <a @click="handlePageSelect" :disabled="_.get($route, 'meta.temporary')" style="margin-left: 12px;">
          <a-icon type="select"/>&nbsp;选择
        </a>
        <a v-if="routePage" @click="handlePageCopy" style="margin-left: 12px;">
          <a-icon type="copy"/>&nbsp;拷贝
        </a>
        <div v-if="menuHistoryPageItems.length>0" style="display: inline-block; margin-left: 32px;">
          历史版本
          <AvatarList avatarSize="small" :maxLength="5" style="margin-left: 4px;">
            <AvatarListItem
              v-for="(item, index) in menuHistoryPageItems"
              @itemDelete="handlePageDelete(item)"
              @itemPreview="handlePagePreview(item)"
              @itemConfirm="() => {pageItem=item; handlePageConfirm(item);}"
              :key="index"
              :src="item.icon"
              :tip="`版本 - ${item.ident}`"
            />
          </AvatarList>
        </div>
      </div>
      <!-- 页面Pdent -->
      <div>
        <div v-if="_.get(routePage, 'pdent')">
          <i>{{ _.get(routePage, 'pdent') || 'None' }}</i>
          <a @click="handleEditPdent" style="margin-left: 8px;"><a-icon type="edit" /></a>
        </div>
      </div>
    </div>

    <!-- 绑定数据 -->
    <div v-if="bindEntity" style="display: inline-block; float: right;">
      <template v-if="!metaDomain">
        <a @click="handleDomainSelect"> <a-icon type="link"/> 绑定数据 </a>
      </template>
      <template v-else-if="cptPageComponentsFields.length === 0">
        <a @click="handleDomainSelect"> <a-icon type="link"/> {{ metaDomain['title'] }} </a>
      </template>
      <template v-else>
        <span><a-icon type="link"/> {{ metaDomain['title'] }} </span>
      </template>
      <template v-if="metaDomain">
        <a style="margin: 0 10px;" @click="handleToRoute('modelField', {domainId: metaDomain['id']})"> <a-icon type="bars"/> 字段 </a>
      </template>
    </div>
    <div v-else-if="entityName" style="display: inline-block; float: right;">
      <template v-if="cptPropEntityDomain">
        <a style="margin: 0 10px;" @click="handleToRoute('modelField', {domainId: cptPropEntityDomain['id']})"> <a-icon type="bars"/> 字段 </a>
      </template>
    </div>

    <!-- Ref弹窗 -->
    <GFormPop
      ref="refCopy"
      :configObj="labPageCopyConfig"
      @popSubmit="handleCopyConfirm">
      <template #slot_content="{record}">
        <a-alert
          style="margin: 10px 0 25px 0"
          message="温馨提示： 视图中无相应页面，将创建新页面到相关视图的历史页面"
          type="warning"
          show-icon>
        </a-alert>
        <div :style="{ borderBottom: '1px solid #E9E9E9' }">
          <a-checkbox :indeterminate="copyIndeterminate" :checked="copyCheckAll" @change="handleCopyCheckAll">全选</a-checkbox>
        </div>
        <div v-for="(viewItem, index) in cptOtherViews" :key="index" style="margin-left: 20px;">
          <a-checkbox :checked="_viewItemChecked(viewItem)" @change="handleCopyCheck($event, viewItem)">{{ viewItem['name'] }}</a-checkbox>
          <div style="display: inline-block; margin-left: 10px;">
            <span v-if="_viewSimilarPage(viewItem)" class="success">
              <i class="warning-disabled">将替换</i>&nbsp;&nbsp;
              相应页面({{ _viewSimilarPage(viewItem)['ident'] }})
            </span>
            <span v-else class="text-tips">
              <i class="warning-disabled">将创建</i>&nbsp;&nbsp;
              相应页面到历史页面
            </span>
          </div>
        </div>
      </template>
    </GFormPop>

    <GFormPop
      ref="refPage"
      :configObj="pageSelectConfig"
      @popSubmit="handlePageConfirm">

      <!-- 模板页面 -->
      <template #tplPages="{record}">
        <a
          v-for="(item) in cptTplPageItems"
          :key="item.key"
          :class="{'page-avatar':true, 'active':_.get(pageItem, 'key')===item.key}"
          @click="pageItem = item">
          <a-avatar :size="65" shape="square" :icon="item.icon" />
          <div class="avatar-title">{{ item.title }}</div>
        </a>
      </template>

      <!-- 系统管理 -->
      <template #sysPages="{record}">
        <a
          v-for="(item) in cptSysPageItems"
          :key="item.key"
          :class="{'page-avatar':true, 'active':_.get(pageItem, 'key') ===item.key}"
          @click="pageItem = item">
          <a-avatar :size="65" shape="square" :icon="item.icon" />
          <div class="avatar-title">{{ item.title }}</div>
          <div class="opt-item-action opt-item-view" @click.stop="handlePagePreview(item)">
            <a-icon :type="item.preview?'eye':'info-circle'"/>
          </div>
        </a>
      </template>

      <!-- 呼叫中心 -->
      <template #ccPages="{record}">
        <a
          v-for="(item) in cptCcPageItems"
          :key="item.key"
          :class="{'page-avatar':true, 'active':_.get(pageItem, 'key') ===item.key}"
          @click="pageItem = item">
          <a-avatar :size="65" shape="square" :icon="item.icon" />
          <div class="avatar-title">{{ item.title }}</div>
          <div class="opt-item-action opt-item-view" @click.stop="handlePagePreview(item)">
            <a-icon :type="item.preview?'eye':'info-circle'"/>
          </div>
        </a>
      </template>

      <!-- 实用工具 -->
      <template #otherPages="{record}">
        <a
          v-for="(item) in cptOtherPageItems"
          :key="item.key"
          :class="{'page-avatar':true, 'active':_.get(pageItem, 'key') ===item.key}"
          @click="pageItem = item">
          <a-avatar :size="65" shape="square" :icon="item.icon" />
          <div class="avatar-title">{{ item.title }}</div>
          <div class="opt-item-action opt-item-view" @click.stop="handlePagePreview(item)">
            <a-icon :type="item.preview?'eye':'info-circle'"/>
          </div>
        </a>
      </template>
    </GFormPop>
    <DomainModal
      ref="refDomain"
      :bindEntity="true"
      @popSubmit="handleDomainConfirm">
    </DomainModal>
  </a-card>
</template>

<!--
# 组件说明
  选择页面弹窗
# 配置组件
  bindEntity: Boolean           // 是否展示绑定按钮，默认false
  entityName: String            // 固定页面绑定的实体
# 操作方法
  暂无
-->

<script>
  const settings = require('@/settings')
  import AAlert from 'ant-design-vue/lib/alert';
  import ACheckbox from 'ant-design-vue/lib/checkbox';
  import AAvatar from 'ant-design-vue/lib/avatar';

  import router from '@/router'
  import { stores, tools } from '@/utils/util'
  import { postAction, ormSeqOrders } from '@/api/api'
  import { createEnhancedRouter  } from '@/utils/EnhancedRouter'
  import pageMapper from '@/pages/pageMapper'

  import MetaView from '@/store/models/MetaView'
  import MetaViewMenu from '@/store/models/MetaViewMenu'
  import MetaDomain from '@/store/models/MetaDomain'
  import MetaPage from '@/store/models/MetaPage'
  import MetaPageComponent from '@/store/models/MetaPageComponent'
  import MetaPageComponentField from '@/store/models/MetaPageComponentField'

  import DomainModal from '@/components/comp_tools/DomainModal'
  import GFormPop from '@/components/page_pop/GFormPop'
  import AvatarList from './components/AvatarList'
  const AvatarListItem = AvatarList.AvatarItem

  import labPageCopyConfig from './configs/labPageCopyConfig'
  import labPageSelectConfig from './configs/labPageSelectConfig'

  export default {
    name: "LabPageSelect",
    components: {
      AAlert,
      ACheckbox,
      AAvatar,
      DomainModal,
      GFormPop,
      AvatarList,
      AvatarListItem,
    },
    /* eslint-disable */
    props: {
      vmPage: {
        type: Object,
        required: false,
      },
      bindEntity: {
        type: Boolean,
        required: false,
        default: false,
      },
      entityName: {
        type: String,
        required: false,
        default: null,
      }
    },
    data(){
      return {
        stores: stores,
        labPageCopyConfig: labPageCopyConfig,
        pageSelectConfig: labPageSelectConfig,
        // Init
        menuId: null,
        routeName: 'initialize',
        activeView: null,
        initPageRoute: null,
        // 元信息
        routePage: null,
        metaDomain: null,
        metaMenu: null,
        menuHistoryPageItems: [],
        viewHistoryPageItems: [],
        // 动态数据
        pageItem: null,      // 选中的PageItem
        copyCheckAll: false,
        copyCheckList: [],
        copyIndeterminate: false,
      }
    },
    inject: ['closeTab'],
    computed: {
      /** 页面条目 */
      cptTplPageItems: function(){
        let pageItems = [
          {key:'TplSimple',      icon: 'layout',       title:'简单模板',        pagePath:'tpl_SimpleTmpl',     preview:false},
          /* {key:'BoardTmpl',   icon: 'dashboard',    title:'仪表模板',        pagePath:'ref_BoardTmpl',      preview:false},
          {key:'ReportTmpl',     icon: 'line-chart',   title:'普通报表',        pagePath:'ref_ReportTmpl',     preview:false},
          {key:'CrosstabTmpl',   icon: 'bar-chart',    title:'交叉报表',        pagePath:'ref_CrosstabTmpl',   preview:false},*/
        ]
        return pageItems
      },
      cptSysPageItems: function(){
        let pageItems = [
          {key:'SysGroup',       icon: 'home',         title:'组织结构',        pagePath:'ref_GroupPage',      preview:true},
          {key:'SysRole',        icon: 'crown',        title:'用户角色',        pagePath:'ref_RolePage',       preview:true},
          {key:'SysConfig',      icon: 'compass',      title:'全局配置',        pagePath:'ref_ConfigPage',     preview:true},
        ]
        return pageItems
      },
      cptCcPageItems: function(){
        let pageItems = [
          {key:'FsExten',         icon: 'phone',       title:'分机管理',        pagePath:'ref_fs_ExtenPage',         preview:true},
          {key:'FsGateway',       icon: 'gateway',     title:'外线管理',        pagePath:'ref_fs_GatewayPage',       preview:true},
          {key:'FsParam',         icon: 'control',     title:'话务参数',        pagePath:'ref_fs_FsParamPage',       preview:true},
          {key:'CcCdr',           icon: 'file-search', title:'话单列表',        pagePath:'ref_cc_CdrPage',           preview:true},
          {key:'CcBlacklist',     icon: 'exception',   title:'黑名单',          pagePath:'ref_cc_BlacklistPage',     preview:true},
          {key:'CcNumnote',       icon: 'number',      title:'功能按键',        pagePath:'ref_cc_NumNotePage',       preview:true},
          {key:'CcRptPoint',      icon: 'dot-chart',   title:'分时概览',        pagePath:'ref_cc_RptPointPage',      preview:false},
          {key:'CcRptDept',       icon: 'area-chart',  title:'部门报表',        pagePath:'tpl_cc_ReportPage',        preview:false},
          {key:'CcRptAppraisal',  icon: 'bar-chart',   title:'绩效评估',        pagePath:'tpl_cc_ReportPage',        preview:false},
        ]
        return pageItems
      },
      cptPsPageItems: function(){
        let pageItems = [
          {key:'PsPicture',       icon: 'picture',      title:'图片管理',        pagePath:'ref_ps_PicturePage',      preview:true},
        ]
        return pageItems
      },
      /* Start with Datav will open in blank tab */
      cptOtherPageItems: function(){
        let pageItems = [
          {key:'OtBlank',        icon: 'file',          title:'空白页面',     pagePath:'ref_ot_BlankPage',         preview:true},
          // {key:'OtInnerDoc',     icon: 'block',         title:'外部页面',     pagePath:'ref_ot_HrefPage',          preview:true},
        ]
        pageItems.push({
          key:'DatavSipfwPage',  icon: 'compass',     title:'SIP监控',     pagePath:'ref_ot_SipfwPage',           preview:true
        })
        return pageItems
      },
      /** 页面相关 */
      cptOtherViews: function(){   // 除当前视图外其它视图实体
        let that = this
        let viewId = that._.get(that.activeView, 'id')
        let metaViews = MetaView.query().orderBy('seqOrder', 'asc').orderBy('id', 'asc').get()
        return metaViews.filter(item => item['id'] !== viewId)
      },
      /** 实体相关 */
      cptPropEntityDomain: function(){
        return MetaDomain.query().where('domainName', this.entityName).first()
      },
      cptPageComponentsFields: function(){   // 当前路由相关的页面的，页面组件的，对应的字段
        let that = this
        let pageComponents = MetaPageComponent.query().where('metaPageId', that.routePage?.id).orderBy('id', 'desc').get()
        let pageComponentIds = pageComponents.map(item => item.id)
        return MetaPageComponentField.query().where(item => {
          return pageComponentIds.indexOf(item.metaPageComponentId)!==-1
        }).orderBy('id', 'desc').get()
      },
    },
    methods: {
      /** Page操作 */
      handlePageSelect(){
        let that = this
        that.pageItem = null
        let slotRef = that.$refs.refPage
        slotRef.showPop({}, {},
          (record, context) => { /* submit */
            return that.handlePageConfirm(record, context)
          }, () => { /* cancel */ }
        )
      },
      // 菜单绑定页面(历史页面或新页面)
      handlePageConfirm(){
        let that = this
        if(!that.pageItem){
          that.$message.warn('请先选择页面')
          return
        }
        // 获取RoutePage
        let viewId = that._.get(that.activeView, 'id')
        let menuId = that._.get(that.metaMenu, 'id')
        let menuTitle = that._.defaultTo(that.metaMenu?.title, '暂无')
        let pageIdent = that._.defaultTo(that.pageItem?.ident, tools.randomString(8))
        let pagePdent = that.pageItem.key || pageIdent
        let pageHref = that.pageItem.pageHref
        let pageComponent = that.pageItem.pagePath
        let historyRoutePage = MetaPage.query().where('metaViewId', viewId)
          .where('ident', pageIdent).orderBy('seqOrder', 'asc').orderBy('id', 'desc').first()
        if(menuId){
          MetaViewMenu.$update({
            params: {
              id: menuId
            },
            data: {
              bindHref: pageHref || null,
            }
          })
        }
        // 绑定RouteMenu
        let promise = null
        if(historyRoutePage){  // 菜单绑定历史页面
          // 页面排序
          let itemOrders = []
          let menuPages = MetaPage.query().where('metaViewId', viewId)
            .where('metaViewMenuId', menuId).orderBy('seqOrder', 'asc').orderBy('id', 'desc').get()
          let mappedItems = menuPages.map(item => {
            return {
              id: item.id,
              ident: item.ident,
              seqOrder: item.seqOrder
            }
          })
          let itemIndex = mappedItems.findIndex( item => item.id === historyRoutePage.id)
          if(itemIndex !== -1){
            mappedItems.splice(itemIndex, 1)
          }
          mappedItems.unshift({id: historyRoutePage.id, ident: historyRoutePage.ident, seqOrder: historyRoutePage.seqOrder})
          mappedItems.forEach((item, index) => {
            let itemData = {id: item.id, seqOrder: index+1}
            itemOrders.push(itemData)
          })
          // 更新排序
          ormSeqOrders('metaPage', {items: itemOrders}).then(resp => {
            let entitys = resp.gmodel
            MetaPage.insertOrUpdate({
              data: entitys
            })
          })
          // 更新页面
          promise = MetaPage.$update({
            params: {
              id: historyRoutePage.id
            },
            data: {
              title: menuTitle,
              metaViewMenuId: menuId,
              seqOrder: itemOrders[0].seqOrder, // 2个异步请求最终一致
            }
          })
        }else{      // 菜单绑定新页面
          promise = MetaPage.$create({
            data: {
              pdent: pagePdent,
              ident: pageIdent,
              title: menuTitle,
              component: pageComponent,
              seqOrder: 1,
              initialized: false,
              metaViewId: viewId,
              metaViewMenuId: menuId,
              extend: null
            }
          })
        }
        promise.then(resp =>{
          if(resp){
            // 添加动态路由
            let routePage = resp
            const routerHelpers = createEnhancedRouter(router)
            if(!routerHelpers.nameExists(routePage['ident'])){
              let pageRoute = {
                name: routePage['ident'],
                path: `/${routePage['ident']}`,
                meta: {
                  title: routePage['title'],
                },
              }
              // 兼容历史页面路径 && 支持短路径映射长路径
              let appMapperPage = that._.get(pageMapper.appMapper, routePage['component'])
              let labMapperPage = that._.get(pageMapper.labMapper, routePage['component'])
              if(appMapperPage){
                pageRoute['component'] = () => import('@/views_app/' + appMapperPage)
              }else if(labMapperPage){
                // lab 模板页不在本仓库内，忽略
              }else{
                // pageRoute['component'] = () => import('@/views_lab/' + routePage['component'])
              }
              routerHelpers.addRoutes([pageRoute],'/')
            }
            // 跳转新页面
            if (stores.isLabState() && pagePdent.startsWith('Datav')) {
              // lab 模式：Datav 页面跳 HrefPage 展示链接
              router.push({ name: 'hrefPage', query: { mId: menuId, href: `${settings.WEB_PATH}/b/${pageIdent}` } })
            } else {
              router.push({ name: pageIdent })
            }
            that.closeTab(that.initPageRoute.fullPath)
          }
        })
        return promise
      },
      // 预览指定页面
      handlePagePreview(pageItem){
        let that = this
        if(!pageItem.preview){ return }
        // 链接页面
        if(pageItem.pageHref){
          let hrefElement = document.createElement('a') // 创建元素
          hrefElement.href = pageItem.pageHref;         // 绑定路径
          hrefElement.target = '_blank';                // 新标签页
          document.body.appendChild(hrefElement);       // 添加元素
          hrefElement.click();                          // 点击下载
          document.body.removeChild(hrefElement);       // 移除元素
          return
        }
        // 历史页面
        if(pageItem.ident){
          that.$router.push({name: pageItem.ident })
          that.$refs.refPage.doClose()
          return
        }
        // 临时路由
        let routerHelpers = createEnhancedRouter(router)
        let previewRoute = `tmp${tools.randomString(8)}`

        let pageRoute = {
            name: previewRoute,
            path: `/${previewRoute}`,
            meta: {
              title: pageItem['title'],
              temporary: true,
            },
        }
        // 兼容历史页面路径 && 支持短路径映射长路径
        let appMapperPage = that._.get(pageMapper.appMapper, pageItem['pagePath'])
        let labMapperPage = that._.get(pageMapper.labMapper, pageItem['pagePath'])
        if(appMapperPage){
          pageRoute['component'] = () => import('@/views_app/' + appMapperPage)
        }else if(labMapperPage){
          // lab 模板页不在本仓库内，忽略
        }else{
          // pageRoute['component'] = () => import('@/views_lab/' + pageItem['pagePath'])
        }
        if(pageItem['key'] && pageItem['key'].startsWith('Datav')){
          // 因为是临时路由，新窗口打开无效
          routerHelpers.addRoutes([pageRoute],'/b')
        }else{
          routerHelpers.addRoutes([pageRoute],'/')
        }
        if(!routerHelpers.nameExists(previewRoute)){
          that.$message.warn('未找到要预览的页面 ......')
          return
        }
        that.$router.push({name: previewRoute })
        that.$refs.refPage.doClose()
      },
      // 删除指定页面
      handlePageDelete(pageItem, directDelete){
        let that = this
        if(directDelete){
          MetaPage.$delete({
            params: {
              id: pageItem.id,
            }
          }).then(() => {
            // 加载元信息
            that.loadMetaModels()
          })
        }else{
          that.$confirm({
            title: '确认弹窗',
            content: `确定要删除页面 [${pageItem.ident}] ?`,
            onOk() {
              MetaPage.$delete({
                params: {
                  id: pageItem.id,
                }
              }).then(() => {
                // 加载元信息
                that.loadMetaModels()
              })
            }
          });
        }
      },
      /** Copy操作 */
      handlePageCopy(){
        let that = this
        let slotRef = that.$refs.refCopy
        slotRef.showPop({}, {},
          (record, context) => { /* submit */
            return that.handleCopyConfirm(record, context)
          }, () => { /* cancel */ }
        )

      },
      // 拷贝页面到多个视图
      handleCopyConfirm(){
        let that = this
        let pageId = that.routePage.id
        if(!pageId){
          that.$message.warn('没找到要拷贝的页面')
          return
        }
        if(that.copyCheckList.length === 0){
          that.$message.warn('请先选择要拷贝到的视图!')
          return
        }
        let promise = postAction('/metaPage/copyPage', {pageId: pageId, checkViewIds: that.copyCheckList})
        promise.then(resp => {
          let metaEntitys = resp.gmodel
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
          // 拷贝成功
          that.$message.success(`拷贝成功`);
          that.$refs.refCopy.doClose()
        }).catch(err => {
          err || true
          that.$message.warn('拷贝失败');
        })
        return promise
      },
      handleCopyCheck(e, viewItem){
        let that = this
        let checked = e.target.checked
        let checkedIndex = that.copyCheckList.indexOf(viewItem['id'])
        if(checked && checkedIndex === -1){
          that.copyCheckList.push(viewItem['id'])
        }else if(!checked && checkedIndex !== -1){
          that.copyCheckList.splice(checkedIndex, 1)
        }
        // 全选状态
        if(that.copyCheckList.length>0){
          that.copyCheckAll = that.copyCheckList.length===that.cptOtherViews.length
          that.copyIndeterminate = that.copyCheckList.length<that.cptOtherViews.length
        }else{
          Object.assign(that, {
            copyCheckAll: false,
            copyIndeterminate: false,
          });
        }
      },
      handleCopyCheckAll(e){
        let that = this
        let checked = e.target.checked
        that.copyCheckList = checked?that.cptOtherViews.map(viewItem => viewItem['id']):[]
        Object.assign(that, {
          copyCheckAll: checked,
          copyIndeterminate: false,
        });
      },
      /** Domain操作 */
      handleDomainSelect(){
        this.$refs.refDomain.showPop()
      },
      handleDomainConfirm(domainItem){
        let that = this
        if(!domainItem){
          that.$message.warn('请先选择数据表格')
          return
        }
        // 页面绑定数据
        if(that.routePage){
          let promise = MetaPage.$update({
            params: {
              id: that.routePage.id
            },
            data: {
              metaDomainId: domainItem.id,
            }
          })
          promise.then(resp => {
            resp || true
            that.$refs.refDomain.doClose()
            that.loadMetaModels()
            if(that.vmPage && that.vmPage.loadMetaModels){
              that.vmPage.loadMetaModels()
            }
            that.$message.success('绑定成功');
          })
          return promise
        }
      },
      /** 扩展操作 */
      handleToRoute(routeName, params){
        this.$router.push({ name: routeName, params: params||{} })
      },
      /** 扩展方法 */
      handleEditPdent() {
        let that = this
        let pdent = that._.get(that.routePage, 'pdent')
        that.$confirm({
          title: `编辑页面标识?`,
          content: function(h) {
            h && true
            let vnodes = []
            let props = { value: pdent }
            vnodes.push(<a-input {...{ props }}
                                 placeholder="标识" onChange={e => pdent = e.target.value}
            />)
            return (<div>{vnodes}</div>)
          },
          onOk() {
            let promise = MetaPage.$update({
              params: {
                id: that.routePage.id,
              },
              data: {
                pdent: pdent
              }
            })
            promise.then(resp => {
              resp || true
              that.loadMetaModels()
              that.$message.success('标识更新成功');
            }).catch(err => {
              err || true
              that.$message.warn('标识更新失败')
            })
            return promise
          }
        });
      },
      /** Load部分 */
      loadMetaModels(){
        let that = this
        that.routePage = MetaPage.query().where('metaViewId', that.activeView?.id)
          .where('ident', that.routeName).orderBy('seqOrder', 'asc').orderBy('id', 'desc').first()
        if(that.routePage) {
          // MetaDomain
          that.metaDomain = MetaDomain.find(that.routePage.metaDomainId)
        }
        // MetaMenu
        let routeMenu = null
        if(that.menuId){
          routeMenu = MetaViewMenu.find(that.menuId)
        }else if(that.routePage){
          routeMenu = MetaViewMenu.find(that.routePage?.metaViewMenuId)
        }
        that.metaMenu = routeMenu
        // MenuHistoryPageItems
        let menuPages = MetaPage.query().where('metaViewId', that.activeView?.id)
          .where('metaViewMenuId', that.metaMenu?.id).orderBy('seqOrder', 'asc').orderBy('id', 'desc').get()
        if(menuPages.length>0){ menuPages.shift() } // 去除当前版本
        that.menuHistoryPageItems = menuPages.map(that._cloneHistoryPageItem)
        // ViewHistoryPageItems
        let viewPages = MetaPage.query().where('metaViewId', that.activeView?.id)
          .orderBy('seqOrder', 'asc').orderBy('id', 'desc').get()
        let menuToLPageId = {} // 最新版页面
        viewPages.reverse().forEach(metaPage => {
          let pageMenu = MetaViewMenu.find(metaPage['metaViewMenuId'])
          if(pageMenu){
            menuToLPageId[pageMenu.id] = metaPage['id']
          }
        })
        let menuLPageIds = Object.values(menuToLPageId)
        viewPages = viewPages.filter(item => { // 未使用的页面
          return !menuLPageIds.includes(item['id'])
        }).sort(function (a, b) { return b['id'] - a['id'] })
        that.viewHistoryPageItems = viewPages.map(that._cloneHistoryPageItem)
      },
      /** 工具方法 */
      _viewItemChecked(viewItem){
        let that = this
        return that.copyCheckList.indexOf(viewItem['id']) !== -1
      },
      _viewSimilarPage(viewItem){
        let that = this
        if(!that.routePage){ return false }
        return MetaPage.query().where('metaViewId', viewItem['id']) // Pdent相同的页面
          .where('pdent', that.routePage.pdent).orderBy('seqOrder', 'asc').orderBy('id', 'desc').first()
      },
      _cloneHistoryPageItem(item){
        let that = this
        let itemIcon = that._.get(item, 'metaViewMenuId')!==0?'file-text':'file'
        return {
          id: item.id,
          ident: item.ident,
          // --------- //
          key: item.ident,
          icon: itemIcon,
          title: item.title,
          pagePath: item.component,
          preview: true,
        }
      },
    },
    created() {
      let that = this
      that.menuId = that.$route.query.mId
      that.routeName = that.$route.name
      that.activeView = that.stores.activeView()
      that.initPageRoute = Object.assign({},that.$route)
      that.loadMetaModels()
    }
  }
</script>

<style lang="scss" scoped>
  /* 图标样式 */
  .page-avatar{
    display: inline-block;
    position: relative;
    .ant-avatar{
      margin: 10px 10px 0 10px;
    }
    .avatar-title{
      text-align: center;
      color: rgba(0, 0, 0, 0.85);
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
      bottom: 40px;
      border-radius: 0 0 5px 0;
    }
    &:hover {
      .opt-item-action{
        opacity: 1;
      }
    }
  }
</style>