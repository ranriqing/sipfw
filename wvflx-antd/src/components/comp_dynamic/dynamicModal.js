import _extends from 'babel-runtime/helpers/extends';
import Vue from 'vue';
import { destroyFns } from 'ant-design-vue/lib/modal/Modal';
import Base from 'ant-design-vue/lib/base';
import Omit from 'omit.js';
import AConfigProvider from 'ant-design-vue/lib/config-provider';
import zhCN from 'ant-design-vue/lib/locale-provider/zh_CN';

import GFormPop from '@/components/page_pop/GFormPop'
import { tools } from '@/utils/util'

export default function dynamicModal(props, DynamicModal) {
  DynamicModal = DynamicModal || GFormPop;
  var config = _extends({
    title: '动态弹窗',
    formFields: [],
    submitBtn: true,
    cancelBtn: true,
  }, props)
  /** 创建DOM元素 */
  var div = document.createElement('div');
  var el = document.createElement('div');
  div.appendChild(el);
  document.body.appendChild(div);
  let modalKey = `modal-${tools.randomUuid()}`

  /** 定义弹窗Config、弹窗实例、弹窗Props */
  var currentConfig = _extends({}, Omit(config, ['parentContext']), { close: close, visible: true });
  var dialogInstance = null;
  var dialogProps = { props: {} };

  /** 关闭方法：调用destroy销毁弹窗 */
  function close() {
    destroy.apply(undefined, arguments);
  }

  /** 更新方法：更新弹窗配置 */
  function update(newConfig) {
    currentConfig = _extends({}, currentConfig, newConfig);
    dialogProps.props = currentConfig;
  }

  /** 销毁方法：销毁实例 */
  function destroy() {
    // 毁Vue实例并从DOM中移除元素
    if (dialogInstance && div.parentNode) {
      dialogInstance.$destroy();
      dialogInstance = null;
      div.parentNode.removeChild(div);
    }

    // 收集传递的参数
    for (var _len = arguments.length, args = Array(_len), _key = 0; _key < _len; _key++) {
      args[_key] = arguments[_key];
    }

    // 检查是否触发取消回调
    var triggerCancel = args.some(function (param) {
      return param && param.triggerCancel;
    });
    if (config.onCancel && triggerCancel) {
      config.onCancel.apply(config, args);
    }

    // 从全局销毁函数列表中移除当前关闭函数
    for (var i = 0; i < destroyFns.length; i++) {
      var fn = destroyFns[i];
      if (fn === close) {
        destroyFns.splice(i, 1);
        break;
      }
    }
  }

  /** 渲染方法：按照Props创建新的Vue实例 */
  function render(props) {
    dialogProps.props = props;
    var V = Base.Vue || Vue;
    return new V({
      el: el,
      parent: config.parentContext,
      data: function data() {
        return { popDialogProps: dialogProps };
      },
      render: function render() {
        var h = arguments[0];
        // 先解构，避免报错，原因不详
        var cdProps = _extends({}, this.popDialogProps);
        let scopedSlots = {}
        if(typeof props.content === 'function'){
          scopedSlots.default = (slotProps) => [
            props.content(h, slotProps)
          ]
        }
        if(typeof props.footer === 'function'){
          scopedSlots.footer = (slotProps) => [
            props.footer(h, slotProps)
          ]
        }
        cdProps.key = modalKey;
        cdProps.scopedSlots = scopedSlots
        cdProps.ref = 'dynamicModalRef'
        return h(AConfigProvider, { props: { locale: zhCN } }, [h(DynamicModal, cdProps)]);
      }
    });
  }

  /** 执行渲染：返回方法列表 */
  dialogInstance = render(currentConfig);
  // 将关闭函数添加到全局销毁函数列表
  destroyFns.push(close);

  // 返回控制对话框的方法
  return new Proxy({
    destroy: close,
    update: update
  }, {
    get(target, prop) {
      // 如果属性在target中已定义，直接返回
      if (target.hasOwnProperty(prop)) {
        return target[prop];
      } else if (dialogInstance && dialogInstance.$refs.dynamicModalRef) {
        return dialogInstance.$refs.dynamicModalRef[prop]
      }
      return undefined;
    },
    set(target, prop, value) {
      // 如果属性在target中已定义，直接设置
      if (target.hasOwnProperty(prop)) {
        target[prop] = value;
      } else if (dialogInstance && dialogInstance.$refs.dynamicModalRef) {
        dialogInstance.$refs.dynamicModalRef[prop] = value;
      }
      return true;
    }
  });
}