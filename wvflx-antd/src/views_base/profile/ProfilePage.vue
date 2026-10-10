<template>
  <div>
    <a-card v-show="!pageVisible" :bordered="false" class="page">
      <a-row :gutter="12">
        <!-- 左侧Form -->
        <a-col :md="12" :sm="24" :xs="24">
          <a-form :form="form" layout="vertical" @submit.prevent="onSubmit">
            <a-row :gutter="12">
              <a-col
                v-for="(item, index) in cptFieldItems"
                :key="index"
                :md="item.cols?item.cols:24"
                :sm="24"
                :xs="24">
                <g-form-item
                  v-if="item.isActived?item.isActived({}, context):true"
                  :item="item"
                  :context="context">
                  <template
                    v-if="item.component===COMPO.G_SLOT"
                    :slot="item.field">
                    <a-alert message="" type="success">
                      <template slot="description">
                        <span> {{ currentUser.username }} ({{ currentUser.ccJobNum||'暂无' }}) </span>
                        <a-button-group style="float: right;">
                          <a-button size="small" type="link" icon="smile" @click="onAvatarEdit">头像</a-button>
                          <a-divider type="vertical" />
                          <a-button size="small" type="link" icon="edit" @click="onResetEdit">密码</a-button>
                        </a-button-group>
                      </template>
                    </a-alert>
                  </template>
                </g-form-item>
              </a-col>
            </a-row>
            <a-form-item>
              <a-button type="primary" htmlType="submit">提交</a-button>
              <a-button style="margin-left: 8px" @click="onGoBack">返回</a-button>
            </a-form-item>
          </a-form>
        </a-col>
        <!-- 右侧头像 -->
        <a-col v-if="isDesktop" :md="12" :sm="24" :xs="24">
          <div class="ant-upload-preview" @click="onAvatarEdit" >
            <div class="mask">
              <a-icon type="cloud-upload-o" />
            </div>
            <img :src="cptAvatarUrl"/>
          </div>
        </a-col>
      </a-row>
    </a-card>

    <!-- 弹窗部分 -->
    <GAvatarPop
      ref="slotAvatar"
      :popVisible="popAvatarVisible"
      :configObj="popConfig.avatarConfig">>
    </GAvatarPop>
    <GFormPop
      ref="slotReset"
      :popVisible="popResetVisible"
      :configObj="popConfig.resetConfig">
    </GFormPop>
  </div>
</template>

<script>
  import AAlert from 'ant-design-vue/lib/alert';

  import GFormPop from '@/components/page_pop/GFormPop'
  import GAvatarPop from '@/components/page_pop/GAvatarPop'

  import { GETTERS, ACTIONS, COMPO } from '@/constants'
  import { mapGetters, mapActions } from "vuex";
  import { currentUser, updateProfile } from '@/api/api'
  import { tools } from '@/utils/util'
  import partConfig from './configs/partConfig'
  import popConfig from './configs/popConfig'

  export default {
    name: "ProfilePage",
    components: {
      AAlert,
      GFormPop,
      GAvatarPop,
    },
    data(){
      return {
        /** 配置&状态 */
        COMPO: COMPO,
        partConfig: partConfig,
        popConfig: popConfig,
        popAvatarVisible: false,
        popResetVisible: false,
        form: this.$form.createForm(this),
        context: {},
      }
    },
    computed: {
      ...mapGetters({
        isMobile: GETTERS.CPT_APP_DEVICE_MOBILE,
        isDesktop: GETTERS.CPT_APP_DEVICE_DESKTOP,
        currentUser: GETTERS.GET_USER_CURRENT_USER,
      }),
      pageVisible: function() {
        return this.popAvatarVisible || this.popResetVisible
      },
      cptFieldItems: function() {
        return this.partConfig.formFields || []
      },
      cptAvatarName: function(){
        let that = this
        return that._.get(that.currentUser, 'avatar', 'avatar-default.png')
      },
      cptAvatarUrl: function(){
        let that = this
        return tools.avatarUrl(that.cptAvatarName)
      },
      cptProfileUser: function(){
        let that = this
        if(that.currentUser){
          that.context.title = that.currentUser.username
          return {
            id: that.currentUser.id,
            username: that.currentUser.username,
            nickname: that.currentUser.nickname,
            mobile: that.currentUser.mobile,
            ccJobNum: that.currentUser.ccJobNum,
            ccJobPwd: that.currentUser.ccJobPwd,
            remark: that.currentUser.remark,
          }
        }else{
          that.context.title = null
          return {}
        }
      }
    },
    watch: {
      cptProfileUser(val) {
        let that = this
        if(val){
          that.form.setFieldsValue({
            nickname: val.nickname,
            mobile: val.mobile,
            ccJobPwd: val.ccJobPwd,
            remark: val.remark,
          })
        }
      }
    },
    methods:{
      ...mapActions({
        setCurrentUser: ACTIONS.SET_USER_CURRENT_USER,
      }),
      /** 基本操作 */
      onSubmit() {
        let that = this
        that.form.validateFields((errors, values) => {
          if (!errors) {
            let record = Object.assign({id: that.cptProfileUser.id}, values)
            that._updateProfile(record).then( () => {
              that.$message.success('更新资料成功')
            })
          }
        });
      },
      onGoBack(){
        this.$router.go(-1)
      },
      /** 弹窗操作 */
      onAvatarEdit(){
        let that = this
        let slotRef = that.$refs.slotAvatar
        that.popAvatarVisible = slotRef.isPage()
        slotRef.showPop({}, {_avatarUrl: that.cptAvatarUrl, _avatarName: that.cptAvatarName},
          (record, context) => { /* submit */
            let promise = that._updateProfile({avatar: context['_avatarName']})
            promise.then(() =>{
              that.$message.success('更新头像成功')
              that.popAvatarVisible = false
            })
            return promise
          }, () => { /* cancel */
            that.popAvatarVisible = false
          }
        )
      },
      onResetEdit(){
        let that = this
        let slotRef = that.$refs.slotReset
        that.popResetVisible = slotRef.isPage()
        slotRef.showPop(that.cptProfileUser, {},
          (record, context) => { /* submit */
            context || true
            let newPassword = record['password1']
            if(newPassword === record['password2']){
              let promise = that._updateProfile({ password: newPassword })
              promise.then(() => {
                that.$message.success('密码更改成功')
                that.popResetVisible = false
              })
              return promise
            }else{
              that.$message.warn('两次输入密码不同')
            }

          }, () => { /* cancel */
            that.popResetVisible = false
          }
        )
      },
      /** 工具方法 */
      _updateProfile(record){
        let that = this
        let promise = updateProfile(record)
        promise.then(()=>{
          currentUser({}).then(resp => {
            that.setCurrentUser(resp.gmodel)
          })
        })
        return promise
      }
    },
    mounted() {
      let that = this
      that.form.setFieldsValue({
        nickname: that.cptProfileUser.nickname,
        mobile: that.cptProfileUser.mobile,
        ccJobPwd: that.cptProfileUser.ccJobPwd,
        remark: that.cptProfileUser.remark,
      })
    },
  }
</script>

<style lang="scss" scoped>
  /* 头像展示 */
  .ant-upload-preview {
    position: relative;
    margin: 20px;
    width: 180px;
    border-radius: 50%;
    box-shadow: 0 0 4px #ccc;
    // Image、Mask组件
    img, .mask {
      width: 100%;
      height: 100%;
      border-radius: 50%;
      overflow: hidden;
    }
    // Mask独有样式
    .mask {
      opacity: 0;
      position: absolute;
      background: rgba(0,0,0,0.4);
      cursor: pointer;
      transition: opacity 0.4s;
      &:hover {
        opacity: 1;
      }
      i {
        font-size: 4rem;
        position: absolute;
        top: 50%;
        left: 50%;
        margin-left: -2rem;
        margin-top: -2rem;
        color: #d6d6d6;
      }
    }
  }
</style>
