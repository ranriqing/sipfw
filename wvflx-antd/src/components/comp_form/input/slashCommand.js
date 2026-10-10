import { Extension } from '@tiptap/core'
import Suggestion from '@tiptap/suggestion'
import Vue from 'vue'
import SlashMenu from './SlashMenu.vue'

/**
 * 斜杠菜单条目。command 接收 { editor, range }，负责删除已输入的 "/查询" 并执行对应块命令。
 */
export const slashItems = [
  { key: 'h1', icon: 'H1', title: '标题 1', command: ({ editor, range }) => editor.chain().focus().deleteRange(range).setNode('heading', { level: 1 }).run() },
  { key: 'h2', icon: 'H2', title: '标题 2', command: ({ editor, range }) => editor.chain().focus().deleteRange(range).setNode('heading', { level: 2 }).run() },
  { key: 'h3', icon: 'H3', title: '标题 3', command: ({ editor, range }) => editor.chain().focus().deleteRange(range).setNode('heading', { level: 3 }).run() },
  { key: 'h4', icon: 'H4', title: '标题 4', command: ({ editor, range }) => editor.chain().focus().deleteRange(range).setNode('heading', { level: 4 }).run() },
  { key: 'bullet', icon: '•', title: '无序列表', command: ({ editor, range }) => editor.chain().focus().deleteRange(range).toggleBulletList().run() },
  { key: 'ordered', icon: '1.', title: '有序列表', command: ({ editor, range }) => editor.chain().focus().deleteRange(range).toggleOrderedList().run() },
  { key: 'task', icon: '☐', title: '待办列表', command: ({ editor, range }) => editor.chain().focus().deleteRange(range).toggleTaskList().run() },
  { key: 'quote', icon: '❝', title: '引用', command: ({ editor, range }) => editor.chain().focus().deleteRange(range).toggleBlockquote().run() },
  { key: 'code', icon: '</>', title: '代码块', command: ({ editor, range }) => editor.chain().focus().deleteRange(range).toggleCodeBlock().run() },
  { key: 'table', icon: '⊞', title: '表格', command: ({ editor, range }) => editor.chain().focus().deleteRange(range).insertTable({ rows: 3, cols: 3, withHeaderRow: true }).run() },
  { key: 'hr', icon: '—', title: '分割线', command: ({ editor, range }) => editor.chain().focus().deleteRange(range).setHorizontalRule().run() },
  { key: 'image', icon: '图', title: '图片', command: null },
]

/**
 * 创建斜杠菜单扩展。
 * @param {{ onPickImage?: Function }} options - onPickImage 在选中「图片」条目时回调，参数 { editor, range }。
 */
export function createSlashCommand(options = {}) {
  return Extension.create({
    name: 'slashCommand',

    addOptions() {
      return {
        suggestion: {
          char: '/',
          startOfLine: true,
          command: ({ editor, range, props }) => {
            if (!props) {
              return
            }
            if (props.key === 'image') {
              if (options.onPickImage) {
                options.onPickImage({ editor, range })
              }
            } else if (typeof props.command === 'function') {
              props.command({ editor, range })
            }
          },
          items: ({ query }) => {
            const q = (query || '').toLowerCase()
            if (!q) {
              return slashItems
            }
            return slashItems.filter(
              item => item.title.toLowerCase().includes(q) || item.key.toLowerCase().includes(q),
            )
          },
          render: () => {
            let component = null
            let element = null

            return {
              onStart(props) {
                component = new (Vue.extend(SlashMenu))({ propsData: props })
                component.$mount()
                element = component.$el
                document.body.appendChild(element)
              },
              onUpdate(props) {
                if (component) {
                  component.updateProps(props)
                }
              },
              onKeyDown(props) {
                if (props.event.key === 'Escape') {
                  // 删除已输入的 "/查询"，使建议失活并触发 onExit 清理
                  props.view.dispatch(props.view.state.tr.deleteRange(props.range.from, props.range.to))
                  return true
                }
                return component ? component.onKeyDown(props) : false
              },
              onExit() {
                if (component) {
                  component.$destroy()
                  component = null
                }
                if (element && element.parentNode) {
                  element.parentNode.removeChild(element)
                  element = null
                }
              },
            }
          },
        },
      }
    },

    addProseMirrorPlugins() {
      return [
        Suggestion({
          editor: this.editor,
          ...this.options.suggestion,
        }),
      ]
    },
  })
}
