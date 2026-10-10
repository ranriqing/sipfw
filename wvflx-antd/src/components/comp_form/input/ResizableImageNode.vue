<template>
  <div
    class="resizable-image-node"
    data-node-view-wrapper=""
    :data-align="node.attrs.align"
    :class="{ 'is-selected': selected }">
    <div class="resizable-image-inner">
      <img
        :src="node.attrs.src"
        :alt="node.attrs.alt"
        :style="imgStyle"
        draggable="false" />
      <span
        v-if="selected"
        class="resize-handle"
        title="拖拽调整大小"
        @mousedown.prevent="onResizeStart"></span>
    </div>
  </div>
</template>

<script>
  /**
   * 可拖拽调整大小的图片 NodeView。
   * 作为 root 的 div 必须带 data-node-view-wrapper，供 @tiptap/vue-2 的 VueNodeView 识别。
   */
  export default {
    name: 'ResizableImageNode',
    props: {
      node: { type: Object, required: true },
      selected: { type: Boolean, default: false },
      updateAttributes: { type: Function, required: true },
    },
    computed: {
      imgStyle() {
        const width = this.node.attrs.width
        return width ? { width: `${width}px` } : {}
      },
    },
    methods: {
      onResizeStart(e) {
        const startX = e.clientX
        const img = this.$el.querySelector('img')
        const startWidth = this.node.attrs.width || (img ? img.offsetWidth : 100)
        const onMove = (ev) => {
          const width = Math.max(50, Math.round(startWidth + (ev.clientX - startX)))
          this.updateAttributes({ width })
        }
        const onUp = () => {
          document.removeEventListener('mousemove', onMove)
          document.removeEventListener('mouseup', onUp)
        }
        document.addEventListener('mousemove', onMove)
        document.addEventListener('mouseup', onUp)
      },
    },
  }
</script>

<style lang="scss" scoped>
  .resizable-image-node {
    &[data-align='center'] { text-align: center; }
    &[data-align='right'] { text-align: right; }
    &.is-selected .resizable-image-inner { outline: 2px solid #1456f0; border-radius: 6px; }
  }
  .resizable-image-inner {
    position: relative;
    display: inline-block;
    max-width: 100%;
    line-height: 0;
  }
  img {
    max-width: 100%;
    height: auto;
    border-radius: 6px;
  }
  .resize-handle {
    position: absolute;
    right: -6px;
    bottom: -6px;
    width: 12px;
    height: 12px;
    background: #fff;
    border: 2px solid #1456f0;
    border-radius: 50%;
    cursor: nwse-resize;
    z-index: 2;
  }
</style>
