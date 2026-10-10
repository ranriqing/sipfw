<template>
  <div ref="menu" class="slash-menu">
    <div v-if="!list.length" class="slash-menu-empty">无匹配项</div>
    <template v-else>
      <div
        v-for="(item, i) in list"
        :key="item.key"
        class="slash-menu-item"
        :class="{ 'is-selected': i === selectedIndex }"
        @click="select(i)"
        @mouseenter="selectedIndex = i">
        <span class="slash-menu-icon">{{ item.icon }}</span>
        <span class="slash-menu-title">{{ item.title }}</span>
      </div>
    </template>
  </div>
</template>

<script>
  /**
   * 斜杠菜单弹层：由 slashCommand.js 的 render() 挂载到 body，通过 props 传入 items/command/clientRect。
   * 动态值镜像到 data，避免直接改 props。
   */
  export default {
    name: 'SlashMenu',
    props: {
      items: { type: Array, default: () => [] },
      command: { type: Function, default: null },
      clientRect: { type: Function, default: null },
      query: { type: String, default: '' },
    },
    data() {
      return {
        selectedIndex: 0,
        list: this.items || [],
        cmd: this.command,
        rectFn: this.clientRect,
      }
    },
    watch: {
      list() {
        this.selectedIndex = 0
      },
    },
    mounted() {
      this.position()
    },
    updated() {
      this.position()
    },
    methods: {
      updateProps(props) {
        this.list = (props && props.items) || []
        this.cmd = (props && props.command) || null
        this.rectFn = (props && props.clientRect) || null
        this.selectedIndex = 0
      },
      select(i) {
        const item = this.list[i]
        if (item && this.cmd) {
          this.cmd(item)
        }
      },
      onKeyDown({ event }) {
        if (event.key === 'ArrowDown') {
          this.selectedIndex = this.list.length ? (this.selectedIndex + 1) % this.list.length : 0
          return true
        }
        if (event.key === 'ArrowUp') {
          this.selectedIndex = this.list.length
            ? (this.selectedIndex - 1 + this.list.length) % this.list.length
            : 0
          return true
        }
        if (event.key === 'Enter') {
          this.select(this.selectedIndex)
          return true
        }
        return false
      },
      position() {
        const el = this.$refs.menu
        if (!el || !this.rectFn) {
          return
        }
        const rect = this.rectFn()
        if (!rect) {
          return
        }
        const menuHeight = el.offsetHeight
        const menuWidth = el.offsetWidth
        const gap = 8
        const spaceBelow = window.innerHeight - rect.bottom
        let top = spaceBelow < menuHeight + gap ? rect.top - menuHeight - gap : rect.bottom + gap
        let left = rect.left
        if (left + menuWidth > window.innerWidth) {
          left = window.innerWidth - menuWidth - gap
        }
        el.style.top = `${Math.max(gap, top)}px`
        el.style.left = `${Math.max(gap, left)}px`
      },
    },
  }
</script>

<style lang="scss" scoped>
  .slash-menu {
    position: fixed;
    z-index: 3000;
    min-width: 180px;
    max-height: 320px;
    overflow-y: auto;
    padding: 4px;
    background: #fff;
    border: 1px solid #e5e6eb;
    border-radius: 8px;
    box-shadow: 0 6px 20px rgba(0, 0, 0, 0.12);
  }
  .slash-menu-empty {
    padding: 10px 12px;
    color: #999;
    font-size: 13px;
  }
  .slash-menu-item {
    display: flex;
    align-items: center;
    padding: 7px 10px;
    border-radius: 5px;
    cursor: pointer;
    font-size: 14px;
    color: #333;
    &.is-selected {
      background: #f0f5ff;
      color: #1456f0;
    }
  }
  .slash-menu-icon {
    flex-shrink: 0;
    width: 28px;
    margin-right: 8px;
    font-family: Menlo, Consolas, monospace;
    font-size: 12px;
    color: #999;
    text-align: center;
  }
  .slash-menu-title {
    flex: 1;
  }
</style>
