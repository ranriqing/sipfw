<template>
  <div class="header-dropdown-wrapper">
    <a-dropdown>
      <span class="header-dropdown-link" :class="{ 'header-dropdown-link-mobile': !stores.isDesktop() }">
        <a-avatar class="avatar" :src="tools.avatarUrl(currentUser, 'avatar')"/>
        <span v-if="stores.isDesktop()">{{ _.get(currentUser, 'nickname', 'UNKNOWN') }}</span>
      </span>
      <a-menu
        slot="overlay"
        class="header-dropdown-menu"
        @click="onMenuClick">
        <a-menu-item key="profile">
          <a-icon type="user"/>
          <span>个人资料</span>
        </a-menu-item>
        <a-menu-item v-if="supportLab && stores.isDesktop() && stores.isSuperAdmin() && (!tools.isApply95() || queryLab)" key="lab">
          <a-icon :type="stores.isStoreLab()?'layout':'bulb'"/>
          <span>{{ stores.isStoreLab()?'普通模式':'零码开发' }}</span>
        </a-menu-item>
        <a-menu-item key="logout">
          <a-icon type="logout"/>
          <span>退出登录</span>
        </a-menu-item>
      </a-menu>
    </a-dropdown>
  </div>
</template>

<!--
# 组件说明
  个人资料、退出登录等功能；
  退出系统登录将清除用户Token、标记为非Lab模式；
  退出登录将直接登出到登录页，防止被SuperToken二次登录；
# 配置组件
  暂无
# 操作方法
  暂无
-->

<script>
  import AAvatar from 'ant-design-vue/lib/avatar';

  const settings = require('@/settings')
  import { logout } from '@/api/api'
  import { stores, tools } from '@/utils/util'
  import { mapGetters, mapActions } from "vuex";
  import { GETTERS, ACTIONS, STORAGE } from '@/constants'

  export default {
    name: "ProfileMenu",
    components:{
      AAvatar,
    },
    props: {
      // 是否支持Lab模式（零码开发模式）
      supportLab: {
        type: Boolean,
        default: true,
      },
    },
    data() {
      return {
        tools: tools,
        stores: stores,
        queryLab: undefined,
      }
    },
    computed: {
      ...mapGetters({
        isDesktop: GETTERS.CPT_APP_DEVICE_DESKTOP,
        currentUser: GETTERS.GET_USER_CURRENT_USER,
      }),
    },
    methods: {
      ...mapActions({
        setAuthToken: ACTIONS.SET_USER_AUTH_TOKEN,
      }),
      onMenuClick({ key }){
        let that = this
        if(key === 'profile'){
          that.$router.push({ name: 'userProfile' })
        }else if(key === 'lab'){
          that.$emit('toggleStoreLab');
        }else if(key === 'logout'){
          let webPath = settings.WEB_PATH
          that.$confirm({
            title: '提示',
            content: '真的要注销登录吗 ?',
            onOk() {
              return logout({}).then(() => {
                that.setAuthToken(null)
                that.$avue.lsSet(STORAGE.LAB_STATE, 'app', 1)    // 1 days
                window.location.href=`${webPath}/auth/login`;   // 到登录页，防止被app的token登录
              }).catch(err => {
                that.$message.error({
                  title: '登出错误',
                  description: err.message
                })
              })
            }
          });
        }
      },
    },
    created() {
      this.queryLab = this.$route.query.lab
    },
  }
</script>

<style lang="scss" scoped>
  /* Profile内容 */
  .header-dropdown-wrapper {
    height: 60px;
    display: flex;
    flex-direction: row;
    justify-content: center;
    align-items: center;
    // 展示部分
    .header-dropdown-link {
      display: inline-block;
      line-height: 60px;
      text-align: center;
      cursor: pointer;
      padding: 0 15px 0 8px;
      transition: all .3s;
      &:hover {
        background: rgba(255, 255, 255, 0.3);
      }
      .avatar {
        margin: 0 10px 0 5px;
        color: #1890ff;
        background: hsla(0, 0%, 100%, .85);
        vertical-align: middle;
        .icon {
          font-size: 16px;
          padding: 4px;
        }
      }
    }
    .header-dropdown-link-mobile {
      padding-right: 0;
    }
  }
</style>