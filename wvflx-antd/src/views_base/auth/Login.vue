<template>
  <div class="main">
    <a-form :form="form" class="user-layout-login">
      <a-tabs
        :active-key="activeTab"
        :tab-bar-style="{ textAlign: 'center', borderBottom: 'unset' }"
        @change="onTabClick">
        <!-- 账号密码登陆 -->
        <a-tab-pane key="tab1" tab="账号密码登陆">
          <a-form-item>
            <a-input
              size="large"
              type="text"
              v-decorator="['username', itemOptions.username]"
              placeholder="请输入帐户名">
              <a-icon slot="prefix" type="user" :style="{ color: 'rgba(0,0,0,.25)' }" />
            </a-input>
          </a-form-item>
          <a-form-item>
            <a-input
              size="large"
              type="password"
              autocomplete="false"
              v-decorator="['password', itemOptions.password]"
              placeholder="请输入密码">
              <a-icon slot="prefix" type="lock" :style="{ color: 'rgba(0,0,0,.25)' }" />
            </a-input>
          </a-form-item>
          <a-row :gutter="0">
            <a-col :span="14">
              <a-form-item>
                <a-input
                  size="large"
                  type="text"
                  v-decorator="['inputCode', itemOptions.inputCode]"
                  @change="onInputCodeChange"
                  placeholder="请输入验证码">
                  <a-icon slot="prefix" type="smile" :style="{ color: 'rgba(0,0,0,.25)' }" />
                </a-input>
              </a-form-item>
            </a-col>
            <a-col :span="10">
              <GraphicCode :uuidStr="uuidStr" @change="codeChange" style="float: right" />
            </a-col>
          </a-row>
        </a-tab-pane>

        <!-- 手机验证码登陆 -->
        <a-tab-pane key="tab2" tab="手机号登陆">
          <a-form-item>
            <a-input
              size="large"
              type="text"
              v-decorator="['mobile',itemOptions.mobile]"
              placeholder="手机号">
              <a-icon slot="prefix" type="mobile" :style="{ color: 'rgba(0,0,0,.25)' }" />
            </a-input>
          </a-form-item>
          <a-row :gutter="16">
            <a-col class="gutter-row" :span="16">
              <a-form-item>
                <a-input
                  size="large"
                  type="text"
                  v-decorator="['captcha', itemOptions.captcha]"
                  placeholder="请输入验证码" >
                  <a-icon slot="prefix" type="mail" :style="{ color: 'rgba(0,0,0,.25)' }" />
                </a-input>
              </a-form-item>
            </a-col>
            <a-col class="gutter-row" :span="8">
              <a-button
                class="sms-code-btn"
                tabindex="-1"
                :disabled="state.smsSendDisabled"
                @click.stop.prevent="sendSmsCode"
                v-text="!state.smsSendDisabled && '获取验证码' || (state.time+' s')">
              </a-button>
            </a-col>
          </a-row>
        </a-tab-pane>
      </a-tabs>

      <!-- 通用登录组件 -->
      <a-form-item>
        <a-checkbox v-model="rememberMe"> 自动登陆 </a-checkbox>
      </a-form-item>
      <a-form-item style="margin-top:24px">
        <a-button
          size="large"
          type="primary"
          html-type="submit"
          class="login-button"
          :loading="loginLoading"
          :disabled="loginLoading"
          @click.stop.prevent="onSubmit" >
          确定
        </a-button>
      </a-form-item>
    </a-form>
  </div>
</template>

<script>
  import ATabs from 'ant-design-vue/lib/tabs';
  import ACheckbox from 'ant-design-vue/lib/checkbox';

  import GraphicCode from '@/components/comp_tools/GraphicCode'
  import { mapActions } from 'vuex'
  import { ACTIONS, STORAGE } from '@/constants'
  import { sendSmsCode, login } from '@/api/api'
  import settings from '@/settings'
  import { tools } from '@/utils/util'

  export default {
    components: {
      ATabs,
      ATabPane: ATabs.TabPane,
      ACheckbox,
      GraphicCode,
    },
    data () {
      let that = this
      return {
        /** 基础变量 */
        baseUrl: settings.BASE_URL,
        form: that.$form.createForm(this),  // 创建form
        activeTab: "tab1",                  // 激活的tab
        loginLoading: false,                // 正在登录
        rememberMe: true,                   // 记住登录
        /** Tab1 变量 */
        uuidStr: tools.randomUuid(),
        verifyCode:"",
        /** Tab2 变量 */
        state: {
          time: 60,
          smsSendDisabled: false,
        },
        itemOptions:{
          /** 用户名密码登录 */
          username:{
            initialValue: '', // chenhb 开发用
            rules: [
              { required: true, message: '请输入用户名!'},
            ]
          },
          password:{
            initialValue: '', // chenhb 开发用
            rules:[
              { required: true, message: '请输入密码!'}
            ]
          },
          inputCode:{
            rules: [
              { required: true, message: '请输入验证码!'},
            ]
          },
          /** 手机验证码登录 */
          mobile:{
            rules: [
              { validator:
                  function(rule,value,callback){
                    if (!value || new RegExp(/^1([38][0-9]|4[579]|5[0-3,5-9]|6[6]|7[0135678]|9[89])\d{8}$/).test(value)){
                      callback();
                    }else{
                      callback("您的手机号码格式不正确!");
                    }
                  }
              }
            ]
          },
          captcha:{
            rules: [
              { required: true, message: '请输入验证码!'}
            ]
          },
        },
      }
    },
    methods: {
      ...mapActions({
        setAuthToken: ACTIONS.SET_USER_AUTH_TOKEN,
      }),
      // 激活指定Tab
      onTabClick (activeKey) {
        let that = this
        if(activeKey !== 'tab1'){
          that.$message.warn('功能尚未开放...')
        }else{
          that.activeTab = activeKey
        }
      },
      onSubmit () {
        let that = this
        let loginParams = { rememberMe: that.rememberMe }
        if (that.activeTab === 'tab1') {
          // 用户名密码登录
          that.form.validateFields([ 'username', 'password', 'inputCode' ], { force: true }, (err, values) => {
            if(!err) {
              loginParams.identifier = values.username
              loginParams.password = values.password
              loginParams.uuid = that.uuidStr
              loginParams.captcha = that.verifyCode
              that.loginLoading = true
              login(loginParams).then(resp=>{
                that._loginSuccess(resp.gmodel)
              }).catch(err => {
                that._loginFailed(err);
              })
            }
          })
        } else {
          // 使用手机号登陆
          that.form.validateFields([ 'mobile', 'captcha' ], { force: true }, (err, values) => {
            if (!err) {
              loginParams = Object.assign(loginParams, values)
              that.loginLoading = true
              login(loginParams).then((resp) => {
                that._loginSuccess(resp.gmodel)
              }).catch(err => {
                that._loginFailed(err);
              })
            }
          })
        }
      },
      /** Tab1方法 */
      // 生成验证码
      codeChange(uuidStr){
        this.uuidStr = uuidStr
        this.verifyCode = ''
      },
      // 输入验证码
      onInputCodeChange(e){
        let inputCode = e.target.value
        this.verifyCode = inputCode?inputCode.toLowerCase():null
      },
      /** Tab2方法 */
      // 发送短信验证码
      sendSmsCode () {
        let that = this
        that.form.validateFields([ 'mobile' ], { force: true }, (err, values) => {
          if(!err) {
            // 验证码倒计时
            this.state.smsSendDisabled = true;
            let interval = window.setInterval(() => {
              if (that.state.time-- <= 0) {
                that.state.time = 60;
                that.state.smsSendDisabled = false;
                window.clearInterval(interval);
              }
            }, 1000);
            // 发送验证码
            let hideSendMsg = that.$message.loading('验证码发送中..', 0);
            sendSmsCode({ mobile: values.mobile }).then(resp => {
              setTimeout(hideSendMsg, 2500);
              this.$notification[ 'success' ]({
                message: '提示',
                description: '验证码获取成功，您的验证码为：' + resp.gmodel.smsCode,
                duration: 8
              })
            }).catch(err => {
              setTimeout(hideSendMsg, 1);
              clearInterval(interval);
              that.state.time = 60;
              that.state.smsSendDisabled = false;
              that._loginFailed(err);
            });
          }
        });
      },
      /** 工具方法 */
      _loginSuccess(gmodel) {
        this.loginLoading = false
        this.setAuthToken(gmodel)
        this.$avue.lsSet(STORAGE.LAB_STATE, 'app', 1)    // 1 days
        this.$router.push({ name: 'root' })
        this.$message.success('欢迎回来');
      },
      _loginFailed(err) {
        this.loginLoading = false;
        this.codeChange(tools.randomUuid());
        this.$notification[ 'error' ]({
          message: '登录失败',
          description: ((err.response || {}).data || {}).message || err.message || '请求出现错误，请稍后再试',
          duration: 4,
        })
      },
    }
  }
</script>

<style lang="scss" scoped>
  /* 用户登录定义 */
  .user-layout-login {
    label {
      font-size: 14px;
    }
    .forge-password {
      font-size: 14px;
    }
    button.login-button {
      padding: 0 15px;
      font-size: 16px;
      height: 40px;
      width: 100%;
    }

    /* 验证码登录 */
    .sms-code-btn {
      display: block;
      width: 100%;
      height: 40px;
    }
  }
</style>
