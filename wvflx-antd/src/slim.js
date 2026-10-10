import Vue from 'vue'
import _ from '@/utils/tiny_lodash'

import Slim from './Slim.vue'
import router from '@/router/slim'
import store from '@/store/slim'

/* 注册Antd */
import AModal from 'ant-design-vue/lib/modal';
import ARow from 'ant-design-vue/lib/row';
import ACol from 'ant-design-vue/lib/col';
import AForm from 'ant-design-vue/lib/form';
import AInput from 'ant-design-vue/lib/input';
import ASelect from 'ant-design-vue/lib/select';
import ARate from 'ant-design-vue/lib/rate';
import ADatePicker from 'ant-design-vue/lib/date-picker';
import AIcon from 'ant-design-vue/lib/icon';
import AButton from 'ant-design-vue/lib/button';
[
  AModal, ARow, ACol, AForm,
  AInput, ASelect, ARate, ADatePicker,
  AIcon, AButton,
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
Vue.prototype.$confirm = AModal.confirm;
Vue.prototype.$destroyAll = AModal.destroyAll;
// import 'ant-design-vue/dist/antd.less';
import '@/assets/scss/antd.reset.less';

/* 注册Prototype */
Vue.prototype._ = _             // Lodash plugin

/** 创建Vue实例 */
// report.html StatSize:1.86M  ParsedSize:839KB
Vue.config.productionTip = false
import '@/router/slim.guard'
let vm = new Vue({
  router,
  store,
  data(){
    return {
    }
  },
  methods:{
  },
  render: h => h(Slim)
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
