/**
 * 使Ant Design Vue的Table组件支持表头sticky， 单元格宽度必须固定
 * https://gist.github.com/plusice/87e3e89cbba417dfff0c990723ed55b4
 *
 * 用法说明:
 * import stickyTable from '@/utilsv/antd-table-sticky'
 * Vue.directive('sticky', stickyTable)
 * <a-table v-sticky row-key="id"> ... ... </a-table>
 */
import throttle from 'lodash/throttle'
// import { throttle } from '../api/util';

let listenAction;
let container;
let stickyHeader = null;
let originEl = null;
let bindingConfig = {};

const originSelector = '.ant-table-content';
const scrollSelector = '.ant-table-scroll .ant-table-body';
const toRemoveSelector = '.ant-table-tbody,.ant-table-placeholder';


// 监听 && 取消监听
const unwatch = () => {
  container && container.removeEventListener('scroll', listenAction);
  container && container.removeEventListener('resize', resizeStickyHeader);
  originEl && originEl.querySelector(scrollSelector).removeEventListener('scroll', setScrollX);
};
const watch = () => {
  container && container.addEventListener('scroll', listenAction);
  container && container.addEventListener('resize', resizeStickyHeader);
  originEl && originEl.querySelector(scrollSelector).addEventListener('scroll', setScrollX);
};

// 读取组件配置
const getBindingConfig = (binding) => {
  const params = binding.value || {};
  const fixedTop = params.fixedTop || 0;
  const zIndex = params.zIndex || 1000;
  const bgColor = params.bgColor || '#fff';
  const { disabled } = params;
  // 父元素的id
  const { scrollContainerId } = params;
  return { fixedTop, zIndex, disabled, scrollContainerId, bgColor };
};

// 根据实际内容修改表头
const adaptStickyHeader = () => {
  if (!originEl || !stickyHeader) return;
  stickyHeader.innerHTML = originEl.innerHTML;
  // stickyHeader.querySelector(scrollSelector).style.overflowX = 'hidden';
  stickyHeader.querySelector(scrollSelector).style.setProperty('overflow-x', 'hidden', 'important');
  const tbodyList = Array.from(stickyHeader.querySelectorAll(toRemoveSelector));
  tbodyList.forEach((tbody) => {
    tbody.parentNode.removeChild(tbody);
  });
  resizeStickyHeader();
  setScrollX();
};

// 根据实际内容设置宽度
const resizeStickyHeader = throttle(() => {
  if (!originEl || !stickyHeader) return;
  stickyHeader.style.width = `${originEl.getBoundingClientRect().width}px`;
  updateHeaderColumns()
});

// 根据表格横向滚动，设置sticky表头的横向位置
const setScrollX = throttle(() => {
  if (!originEl || !stickyHeader) return;
  const stickyHeaderScroller = stickyHeader.querySelector(scrollSelector);
  const originScroller = originEl.querySelector(scrollSelector);
  stickyHeaderScroller.scrollLeft = originScroller.scrollLeft;
  updateHeaderColumns()
});

// 更新表头宽度
const updateHeaderColumns = throttle(() => {
  if (!originEl || !stickyHeader) return;
  let stickyTHead = stickyHeader.querySelector('.ant-table-thead')
  if(stickyTHead && stickyTHead.childNodes[0]){
    let stickyTHeadArr = Array.from(stickyTHead.childNodes[0].childNodes);
    stickyTHead.childNodes[0].style.display = 'flex';
    let originBody = originEl.querySelector('.ant-table-tbody')
    if(originBody && originBody.childNodes[0]){
      let bodyArr = Array.from(originBody.childNodes[0].childNodes);
      for (let i = 0; i < stickyTHeadArr.length; i++){
        if(bodyArr[i]){
          stickyTHeadArr[i].style.width = bodyArr[i].offsetWidth + 'px';
        }
      }
    }
  }
});

export default {
  bind(el, binding) {
    // 获取El信息
    originEl = el.querySelector(originSelector);
    bindingConfig = getBindingConfig(binding);
    const { disabled, fixedTop, zIndex, scrollContainerId, bgColor } = bindingConfig;
    if (disabled) return;
    container = document.getElementById(scrollContainerId) || window;
    let active = false;
    // 拷贝El组件
    stickyHeader = originEl.cloneNode(true);
    const stickyStyle = stickyHeader.style;
    stickyStyle.cssText = `position: fixed;top: ${fixedTop}px; z-index: ${zIndex}; ${stickyStyle.cssText};background-color: ${bgColor}`;
    // 定义组件方法
    const sticky = () => {
      if (active) return;
      if (!originEl || !originEl.isConnected || !stickyHeader) return;
      setScrollX();
      originEl.insertAdjacentElement('afterend', stickyHeader);
      active = true;
    };
    const reset = () => {
      if (!active) return;
      if (stickyHeader?.parentNode) {
        stickyHeader.parentNode.removeChild(stickyHeader);
      }
      active = false;
    };
    listenAction = throttle(() => {
      // 页面切换后 originEl 可能已从 DOM 中移除，getBoundingClientRect 会返回全 0
      if (!originEl || !originEl.isConnected) return;
      const rectEl = originEl;
      const rect = rectEl.getBoundingClientRect();
      const offsetTop = rect.top;
      if (offsetTop <= fixedTop) {
        return sticky();
      }
      reset();
    });
    // 启动监听器
    watch();
  },
  unbind() {
    // 移除残留的sticky header，避免切换页面后出现额外表头
    if (stickyHeader) {
      stickyHeader.parentNode?.removeChild(stickyHeader);
      stickyHeader = null;
    }
    originEl = null;
    container = null;
    bindingConfig = {};
    unwatch();
  },
  update(el, binding) {
    bindingConfig = getBindingConfig(binding);
    originEl = el.querySelector(originSelector);
    if (bindingConfig.disabled) {
      stickyHeader.parentNode?.removeChild(stickyHeader);
      unwatch();
      return;
    }
    adaptStickyHeader();
  },
};
