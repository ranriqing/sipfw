<template>
  <div class="page">
    <LabPageSelect>外部页面</LabPageSelect>
    <a-card :bordered="false">
      <!-- <iframe :src="metaMenu.bindHref" frameborder="0" class="full-iframe"></iframe> -->
      <a-empty class="page-empty-wrapper">
        <template slot="description">
          <a-button v-if="pageHref" type="link" @click="handleToHref">
            {{ pageHref }}
          </a-button>
          <span v-else>外部页面</span>
        </template>
      </a-empty>
    </a-card>
  </div>
</template>

<script>
  import AEmpty from 'ant-design-vue/lib/empty';

  import { mapGetters } from "vuex"
  import { GETTERS } from '@/constants'

  import MetaViewMenu from '@/store/models/MetaViewMenu'
  import MetaPage from '@/store/models/MetaPage'

  import LabPageSelect from '@/pages/LabPageSelect'

  export default {
    name: 'HrefPage',
    components: {
      AEmpty,
      LabPageSelect,
    },
    data () {
      return {
        initialized: false,
        // 元信息
        metaMenu: null,
      };
    },
    computed:{
      ...mapGetters({
        activeView: GETTERS.GET_META_ACTIVE_VIEW,
      }),
      // 展示链接：优先使用路由携带的 href(如 Datav 页面)，否则取菜单绑定的 bindHref
      pageHref: function(){
        let that = this
        return that.$route.query.href || (that.metaMenu && that.metaMenu.bindHref)
      },
    },
    methods: {
      handleToHref(){
        let that = this
        let pageHref = that.pageHref
        if (!pageHref) {
          return
        }
        // 链接页面
        let hrefElement = document.createElement('a') // 创建元素
        hrefElement.href = pageHref;                  // 绑定路径
        hrefElement.target = '_blank';                // 新标签页
        document.body.appendChild(hrefElement);       // 添加元素
        hrefElement.click();                          // 点击下载
        document.body.removeChild(hrefElement);       // 移除元素
      }
    },
    created() {
      let that = this
      let menuId = that.$route.query.mId
      let routeName = that.$route.name
      let routePage = MetaPage.query().where('metaViewId', that.activeView?.id)
        .where('ident', routeName).orderBy('seqOrder', 'asc').orderBy('id', 'desc').first()
      // MetaMenu
      let routeMenu = null
      if(menuId){
        routeMenu = MetaViewMenu.find(menuId)
      }else if(routePage){
        routeMenu = MetaViewMenu.find(routePage?.metaViewMenuId)
      }
      that.metaMenu = routeMenu
    }
  }
</script>

<style lang="scss" scoped>
  .full-iframe {
    width: 100%;
    min-height: 800px;
  }
</style>