const settings = require('@/settings')
import Vue from 'vue'
import moment from "moment"
import _ from '@/utils/tiny_lodash'
import { Decimal } from 'decimal.js'
import avue from '@/utils/avue'
import Clipboard from "clipboard";

import App from './App.vue'
import router from '@/router'
import store from '@/store'
import { resolveRouteName } from '@/utils/util_routes'

import Stomp from 'stompjs'
import SockJS from  'sockjs-client'


import Element from 'element-ui'
import 'element-ui/lib/theme-chalk/index.css'
Vue.use(Element, {
  size: 'medium' // set element-ui default size
})

/* 注册Antd */
import Draggable from 'vuedraggable'
import AModal from 'ant-design-vue/lib/modal';
import ADrawer from 'ant-design-vue/lib/drawer';
import ACard from 'ant-design-vue/lib/card';
import ATable from 'ant-design-vue/lib/table';
import APagination from 'ant-design-vue/lib/pagination';
import ARow from 'ant-design-vue/lib/row';
import ACol from 'ant-design-vue/lib/col';
import ASpin from 'ant-design-vue/lib/spin';
import ATag from 'ant-design-vue/lib/tag';
import ABadge from 'ant-design-vue/lib/badge';
import AIcon from 'ant-design-vue/lib/icon';
import AButton from 'ant-design-vue/lib/button';
import ACheckbox from 'ant-design-vue/lib/checkbox';
import ATooltip from 'ant-design-vue/lib/tooltip';
import APopover from 'ant-design-vue/lib/popover';
import ADropdown from 'ant-design-vue/lib/dropdown';
import AMenu from 'ant-design-vue/lib/menu';
import ADivider from 'ant-design-vue/lib/divider';
import APopconfirm from 'ant-design-vue/lib/popconfirm';
import AConfigProvider from 'ant-design-vue/lib/config-provider';
import AForm from 'ant-design-vue/lib/form';
import AInput from 'ant-design-vue/lib/input';
import ASelect from 'ant-design-vue/lib/select';
import ASwitch from 'ant-design-vue/lib/switch';
import AInputNumber from 'ant-design-vue/lib/input-number';
import ARate from 'ant-design-vue/lib/rate';
import ADatePicker from 'ant-design-vue/lib/date-picker';
import ATimePicker from 'ant-design-vue/lib/time-picker';
import GFormItem from '@/components/comp_form/GFormItem';
import GPanel from '@/components/comp_form/special/GPanel';
import ScElem from '@/components/comp_tools/ScElem';
import AvatarUser from '@/components/comp_tools/AvatarUser';
Vue.component('GFormItem', GFormItem);
Vue.component('ScElem', ScElem);
Vue.component('AvatarUser', AvatarUser);
Vue.component('GPanel', GPanel);

[
  Draggable, AModal, ADrawer, ACard,
  ATable, APagination,
  ARow, ACol, ASpin, AIcon, ATag, ABadge, AButton, ACheckbox,
  ATooltip, APopover,
  ADropdown, AMenu, ADivider,
  APopconfirm, AConfigProvider, AForm,
  AInput, ASelect, ASwitch, AInputNumber,
  ARate, ADatePicker, ATimePicker,
].map(function(component){
  Vue.use(component);
})
import message from 'ant-design-vue/lib/message';
import notification from 'ant-design-vue/lib/notification';
Vue.prototype.$message = message;
Vue.prototype.$notification = notification;
Vue.prototype.$info = AModal.info;
Vue.prototype.$success = AModal.success;
Vue.prototype.$error = AModal.error;
Vue.prototype.$warning = AModal.warning;
Vue.prototype.$confirm = function(config) {
  return AModal.confirm(Object.assign({
    okText: '确定',
    cancelText: '取消',
  }, config))
}
Vue.prototype.$destroyAll = AModal.destroyAll;
// import 'ant-design-vue/dist/antd.less';
import '@/assets/scss/antd.reset.less';

import dynamicModal from '@/components/comp_dynamic/dynamicModal';
Vue.prototype.$dynamicModal = dynamicModal;

/* 注册Event */
import emitter from "@/utils/eventbus.js";
Vue.prototype.$event = emitter;

/* 注册MavonEditor */
// https://github.com/hinesboy/mavonEditor
import mavonEditor from 'mavon-editor'
import 'mavon-editor/dist/css/index.css'
Vue.use(mavonEditor)

/* 注册VueSignaturePad */
import VueSignaturePad from "vue-signature-pad"
Vue.use(VueSignaturePad);

/* 注册IconFont */
let IconFont = AIcon.createFromIconfontCN({scriptUrl: null,})
Vue.component('IconFont', IconFont) // 为iconfont.js注册组件

/* 注册Prototype */
Vue.prototype._ = _             // Lodash plugin
Vue.prototype.$avue = avue      // Avue plugin
Vue.prototype.$moment = moment  // Moment plugin
Vue.prototype.Decimal = Decimal // Decimal plugin

/** 创建Vue实例 */
import { MUTATIONS, STORAGE } from '@/constants'
import '@/router/router.guard'
import '@/utils/filters'
Vue.config.productionTip = false  // 启动时的生产提示
let labState = avue.lsGet(STORAGE.LAB_STATE)
let authToken = avue.lsGet(STORAGE.AUTH_TOKEN)
let cachePool = avue.lsGet(STORAGE.CACHE_POOL)
store.commit(MUTATIONS.SET_APP_LAB_STATE, labState)      // 同步Storage
store.commit(MUTATIONS.SET_USER_AUTH_TOKEN, authToken)   // 同步Storage
store.commit(MUTATIONS.SET_APP_CACHE_POOL, cachePool)    // 同步Storage
let vm = new Vue({
  router,
  store,
  data(){
    return {
      stompClient: null,
      stompSubscribers: {},
      stompSubscriptions: {} // 已经注册
    }
  },
  provide: function () {
    let that = this
    return {
      switchRoute: that.switchRoute,
      copyText: that.copyText,
      wsSend: that.wsSend,
      wsSubscribe: that.wsSubscribe,
      wsUnSubscribe: that.wsUnSubscribe,
    }
  },
  methods:{
    switchRoute(target, params){
      let name = resolveRouteName(target)
      if(name){
        this.$router.push({ name: name, params: params||{} })
      }else{
        this.$message.warn('页面不存在，请联系管理员')
      }
    },
    copyText(textStr, e){
      let that = this
      let target = e.target
      if(e.target.tagName === 'svg'){
        target = e.target.parentElement
      }
      let clipboard = new Clipboard(target, {
        text: () => textStr
      })
      clipboard.on('success', () => {
        that.$message.success(`已拷贝：${textStr}`);
        clipboard.destroy()
      })
      clipboard.on('error', () => {
        clipboard.destroy()
      })
      clipboard.onClick(e)
    },
    wsDemoFunc(messageJson){
      console.log("wsDemoFunc:", messageJson)
    },
    wsHelloFunc(messageJson){
      console.log("wsHelloFunc:", messageJson)
    },
    wsSend(){
      let that = this
      if(that.stompClient && that.stompClient['connected']){
        that.stompClient.send("/ws/hello", {}, JSON.stringify({name:'chenhb', age: 30}));
      }
    },
    wsSubscribe(subPath, subFunc){
      let that = this
      if(subPath && that._.isFunction(subFunc)){
        // 保存Subscriber
        that.wsUnSubscribe(subPath) // 确保不重复
        that.stompSubscribers[subPath] = subFunc
        // 注册Subscriber
        if(that.stompClient && that.stompClient['connected']){
          console.log('wsSubscribe - ', subPath)
          that.stompSubscriptions[subPath] = that.stompClient.subscribe(subPath, function(message) {
            let messageJson = JSON.parse(message.body);
            subFunc(messageJson)
          });
        }
      }
    },
    wsUnSubscribe(subPath){
      let that = this
      if(subPath){
        let subscription = that.stompSubscriptions[subPath]
        if(subscription && that._.isFunction(subscription.unsubscribe)){
          console.log('wsUnSubscribe - ', subPath)
          subscription.unsubscribe()
        }
        delete that.stompSubscriptions[subPath]
        delete that.stompSubscribers[subPath]
      }
    },
    initWebSocket() {
      let that = this
      that.wsSubscribe('/topic/demo', that.wsDemoFunc)
      that.wsSubscribe('/topic/hello', that.wsHelloFunc)
      let socket = new SockJS(settings.BASE_URL+'/stomp',null, {
        timeout: 5000,
      });
      that.stompClient = Stomp.over(socket);
      that.stompClient.debug = function(str) {
        str || true
        // console.log("-----------", str)
      };
      that.stompClient.connect({},
        function() {
          store.commit(MUTATIONS.SET_APP_WS_CONNECTED, true)
          Object.keys(that.stompSubscribers).forEach( subPath => {
            let subFunc = that.stompSubscribers[subPath]
            if(that._.isFunction(subFunc)){
              that.wsSubscribe(subPath, subFunc)
            }
          })
        },
        function() {
          store.commit(MUTATIONS.SET_APP_WS_CONNECTED, false)
          setTimeout(that.initWebSocket, 10000); // reconnect after 10s
        }
      );
    },
    destroyWebSocket(){
      if (this.stompClient) {
        this.stompClient.disconnect();
      }
    }
  },
  mounted() {
    let that = this
    that.initWebSocket();
  },
  beforeDestroy(){
    let that = this
    that.destroyWebSocket();
  },
  render: h => h(App)
}).$mount('#app')

/** 调试用变量 */
window.Vue = Vue
window.vm = vm;
if (console && !console.trace) {
  // 输出函数调用路径
  console.trace = function() {
    let i = 0;
    let fun = arguments.callee;
    do {
      fun = fun['arguments'].callee.caller;
      console.log(++i + ': ' + fun);
    } while (fun);
  }
}
