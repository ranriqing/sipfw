<template>
  <div
    class="gtip-tap-editor"
    :class="[`mode-${mode}`, { 'is-readonly': !editable }]"
    @mousemove="onEditorMouseMove"
    @mouseleave="onEditorMouseLeave">
    <template v-if="editor">
      <!-- 顶部工具栏：simple 模式 -->
      <div
        v-if="mode === 'simple' && editable"
        class="tiptap-toolbar"
        role="toolbar"
        aria-label="toolbar"
        data-variant="fixed">
        <div style="flex: 1"></div>

        <!-- 撤销 / 重做 -->
        <div role="group" class="tiptap-toolbar-group">
          <button
            type="button"
            class="tiptap-button"
            data-style="ghost"
            role="button"
            :disabled="!toolbarState.canUndo"
            data-disabled="true"
            aria-label="Undo"
            @mousedown.prevent
            @click="cmdUndo">
            <tip-tap-icon name="undo" />
          </button>
          <button
            type="button"
            class="tiptap-button"
            data-style="ghost"
            role="button"
            :disabled="!toolbarState.canRedo"
            data-disabled="true"
            aria-label="Redo"
            @mousedown.prevent
            @click="cmdRedo">
            <tip-tap-icon name="redo" />
          </button>
        </div>

        <div class="tiptap-separator" data-orientation="vertical" role="none"></div>

        <!-- 标题下拉 + 列表下拉 + 引用 + 代码块 -->
        <div role="group" class="tiptap-toolbar-group">
          <a-dropdown :trigger="['click']">
            <button type="button" class="tiptap-button" data-style="ghost" role="button" aria-label="Format text as heading">
              <tip-tap-icon name="heading" />
              <tip-tap-icon name="chevron-down" small />
            </button>
            <a-menu slot="overlay" @click="onHeadingMenu">
              <a-menu-item key="paragraph">正文</a-menu-item>
              <a-menu-item key="h1">标题 1</a-menu-item>
              <a-menu-item key="h2">标题 2</a-menu-item>
              <a-menu-item key="h3">标题 3</a-menu-item>
            </a-menu>
          </a-dropdown>
          <a-dropdown :trigger="['click']">
            <button type="button" class="tiptap-button" data-style="ghost" role="button" aria-label="List options">
              <tip-tap-icon name="list" />
              <tip-tap-icon name="chevron-down" small />
            </button>
            <a-menu slot="overlay" @click="onListMenu">
              <a-menu-item key="bullet">无序列表</a-menu-item>
              <a-menu-item key="ordered">有序列表</a-menu-item>
              <a-menu-item key="task">待办列表</a-menu-item>
            </a-menu>
          </a-dropdown>
          <button
            type="button"
            class="tiptap-button"
            data-style="ghost"
            role="button"
            :data-active-state="toolbarState.blockquote ? 'on' : 'off'"
            aria-label="Blockquote"
            aria-pressed="false"
            @mousedown.prevent
            @click="toggleBlockquote">
            <tip-tap-icon name="blockquote" />
          </button>
          <button
            type="button"
            class="tiptap-button"
            data-style="ghost"
            role="button"
            :data-active-state="toolbarState.codeBlock ? 'on' : 'off'"
            aria-label="Code Block"
            aria-pressed="false"
            @mousedown.prevent
            @click="toggleCodeBlock">
            <tip-tap-icon name="code-block" />
          </button>
        </div>

        <div class="tiptap-separator" data-orientation="vertical" role="none"></div>

        <!-- 加粗 / 斜体 / 删除线 / 行内代码 / 下划线 / 高亮 / 链接 -->
        <div role="group" class="tiptap-toolbar-group">
          <button
            type="button"
            class="tiptap-button"
            data-style="ghost"
            role="button"
            :data-active-state="toolbarState.bold ? 'on' : 'off'"
            aria-label="Bold"
            aria-pressed="false"
            @mousedown.prevent
            @click="toggleBold">
            <tip-tap-icon name="bold" />
          </button>
          <button
            type="button"
            class="tiptap-button"
            data-style="ghost"
            role="button"
            :data-active-state="toolbarState.italic ? 'on' : 'off'"
            aria-label="Italic"
            aria-pressed="false"
            @mousedown.prevent
            @click="toggleItalic">
            <tip-tap-icon name="italic" />
          </button>
          <button
            type="button"
            class="tiptap-button"
            data-style="ghost"
            role="button"
            :data-active-state="toolbarState.strike ? 'on' : 'off'"
            aria-label="Strike"
            aria-pressed="false"
            @mousedown.prevent
            @click="toggleStrike">
            <tip-tap-icon name="strike" />
          </button>
          <button
            type="button"
            class="tiptap-button"
            data-style="ghost"
            role="button"
            :data-active-state="toolbarState.code ? 'on' : 'off'"
            aria-label="Code"
            aria-pressed="false"
            @mousedown.prevent
            @click="toggleCode">
            <tip-tap-icon name="code" />
          </button>
          <button
            type="button"
            class="tiptap-button"
            data-style="ghost"
            role="button"
            :data-active-state="toolbarState.underline ? 'on' : 'off'"
            aria-label="Underline"
            aria-pressed="false"
            @mousedown.prevent
            @click="toggleUnderline">
            <tip-tap-icon name="underline" />
          </button>
          <button
            type="button"
            class="tiptap-button"
            data-style="ghost"
            role="button"
            data-appearance="default"
            :data-active-state="toolbarState.highlight ? 'on' : 'off'"
            aria-label="Highlight"
            aria-pressed="false"
            @mousedown.prevent
            @click="toggleHighlight">
            <tip-tap-icon name="highlight" />
          </button>
          <button
            type="button"
            class="tiptap-button"
            data-style="ghost"
            role="button"
            :data-active-state="toolbarState.link ? 'on' : 'off'"
            aria-label="Link"
            aria-pressed="false"
            @mousedown.prevent
            @click="toggleLink">
            <tip-tap-icon name="link" />
          </button>
        </div>

        <div class="tiptap-separator" data-orientation="vertical" role="none"></div>

        <!-- 上标 / 下标 -->
        <div role="group" class="tiptap-toolbar-group">
          <button
            type="button"
            class="tiptap-button"
            data-style="ghost"
            role="button"
            :data-active-state="toolbarState.superscript ? 'on' : 'off'"
            aria-label="Superscript"
            aria-pressed="false"
            @mousedown.prevent
            @click="toggleSuperscript">
            <tip-tap-icon name="superscript" />
          </button>
          <button
            type="button"
            class="tiptap-button"
            data-style="ghost"
            role="button"
            :data-active-state="toolbarState.subscript ? 'on' : 'off'"
            aria-label="Subscript"
            aria-pressed="false"
            @mousedown.prevent
            @click="toggleSubscript">
            <tip-tap-icon name="subscript" />
          </button>
        </div>

        <div class="tiptap-separator" data-orientation="vertical" role="none"></div>

        <!-- 对齐 -->
        <div role="group" class="tiptap-toolbar-group">
          <button
            type="button"
            class="tiptap-button"
            data-style="ghost"
            role="button"
            :data-active-state="toolbarState.alignLeft ? 'on' : 'off'"
            aria-label="Align left"
            aria-pressed="false"
            @mousedown.prevent
            @click="setAlign('left')">
            <tip-tap-icon name="align-left" />
          </button>
          <button
            type="button"
            class="tiptap-button"
            data-style="ghost"
            role="button"
            :data-active-state="toolbarState.alignCenter ? 'on' : 'off'"
            aria-label="Align center"
            aria-pressed="false"
            @mousedown.prevent
            @click="setAlign('center')">
            <tip-tap-icon name="align-center" />
          </button>
          <button
            type="button"
            class="tiptap-button"
            data-style="ghost"
            role="button"
            :data-active-state="toolbarState.alignRight ? 'on' : 'off'"
            aria-label="Align right"
            aria-pressed="false"
            @mousedown.prevent
            @click="setAlign('right')">
            <tip-tap-icon name="align-right" />
          </button>
          <button
            type="button"
            class="tiptap-button"
            data-style="ghost"
            role="button"
            :data-active-state="toolbarState.alignJustify ? 'on' : 'off'"
            aria-label="Align justify"
            aria-pressed="false"
            @mousedown.prevent
            @click="setAlign('justify')">
            <tip-tap-icon name="align-justify" />
          </button>
        </div>

        <div class="tiptap-separator" data-orientation="vertical" role="none"></div>

        <!-- 图片 -->
        <div role="group" class="tiptap-toolbar-group">
          <button type="button" class="tiptap-button" data-style="ghost" role="button" aria-label="Add image" aria-pressed="false" @mousedown.prevent @click="onToolbarImage">
            <tip-tap-icon name="image" />
            <span class="tiptap-button-text">Add</span>
          </button>
        </div>

        <div style="flex: 1"></div>
      </div>

      <!-- 气泡工具栏：notion 模式 -->
      <bubble-menu
        v-if="mode === 'notion' && editable"
        :editor="editor"
        :should-show="shouldShowBubbleMenu"
        :tippy-options="{ duration: 100, maxWidth: 560, arrow: false }">
        <div class="bubble-menu-wrap">
          <div class="bubble-menu">
            <select class="bubble-select" title="转换为" :value="currentBlockType" @change="onTurnInto">
              <option value="paragraph">正文</option>
              <option value="h1">标题 1</option>
              <option value="h2">标题 2</option>
              <option value="h3">标题 3</option>
              <option value="bulletList">无序列表</option>
              <option value="orderedList">有序列表</option>
              <option value="taskList">待办列表</option>
              <option value="blockquote">引用</option>
              <option value="codeBlock">代码块</option>
            </select>
            <span class="bubble-sep"></span>
            <button type="button" class="bubble-btn" :class="{ 'is-active': editor.isActive('bold') }" @mousedown.prevent @click="toggleBold"><b>B</b></button>
            <button type="button" class="bubble-btn bubble-btn-italic" :class="{ 'is-active': editor.isActive('italic') }" @mousedown.prevent @click="toggleItalic">I</button>
            <button type="button" class="bubble-btn" :class="{ 'is-active': editor.isActive('underline') }" @mousedown.prevent @click="toggleUnderline"><u>U</u></button>
            <button type="button" class="bubble-btn bubble-btn-strike" :class="{ 'is-active': editor.isActive('strike') }" @mousedown.prevent @click="toggleStrike">S</button>
            <button type="button" class="bubble-btn" :class="{ 'is-active': editor.isActive('code') }" @mousedown.prevent @click="toggleCode">&lt;/&gt;</button>
            <button type="button" class="bubble-btn" :class="{ 'is-active': editor.isActive('highlight') }" @mousedown.prevent @click="toggleHighlight">高亮</button>
            <span class="bubble-sep"></span>
            <button type="button" class="bubble-btn" :class="{ 'is-active': editor.isActive('link') }" @mousedown.prevent @click="toggleLink()">链接</button>
            <button type="button" class="bubble-btn" title="更多" @mousedown.prevent @click="showMore = !showMore">⋮</button>
          </div>
          <div v-if="showMore" class="bubble-more" @mousedown.prevent>
            <button type="button" class="bubble-btn" :class="{ 'is-active': editor.isActive('superscript') }" @click="toggleSuperscript">x²</button>
            <button type="button" class="bubble-btn" :class="{ 'is-active': editor.isActive('subscript') }" @click="toggleSubscript">x₂</button>
            <span class="bubble-sep"></span>
            <button type="button" class="bubble-btn" :class="{ 'is-active': editor.isActive({ textAlign: 'left' }) }" @click="setAlign('left')">左</button>
            <button type="button" class="bubble-btn" :class="{ 'is-active': editor.isActive({ textAlign: 'center' }) }" @click="setAlign('center')">中</button>
            <button type="button" class="bubble-btn" :class="{ 'is-active': editor.isActive({ textAlign: 'right' }) }" @click="setAlign('right')">右</button>
            <button type="button" class="bubble-btn" :class="{ 'is-active': editor.isActive({ textAlign: 'justify' }) }" @click="setAlign('justify')">两端</button>
            <span class="bubble-sep"></span>
            <button type="button" class="bubble-btn" @click="onLiftListItem()">⇤</button>
            <button type="button" class="bubble-btn" @click="onSinkListItem()">⇥</button>
          </div>
        </div>
      </bubble-menu>

      <!-- 图片节点气泡菜单（两种模式共用） -->
      <bubble-menu
        v-if="editable"
        :editor="editor"
        :should-show="shouldShowImageMenu"
        :tippy-options="{ duration: 100, arrow: false }">
        <div class="bubble-menu">
          <button type="button" class="bubble-btn" :class="{ 'is-active': !editor.isActive('image', { align: 'center' }) && !editor.isActive('image', { align: 'right' }) }" @mousedown.prevent @click="setImageAlign(null)">左</button>
          <button type="button" class="bubble-btn" :class="{ 'is-active': editor.isActive('image', { align: 'center' }) }" @mousedown.prevent @click="setImageAlign('center')">中</button>
          <button type="button" class="bubble-btn" :class="{ 'is-active': editor.isActive('image', { align: 'right' }) }" @mousedown.prevent @click="setImageAlign('right')">右</button>
          <span class="bubble-sep"></span>
          <button type="button" class="bubble-btn" @mousedown.prevent @click="deleteImage()">删除</button>
        </div>
      </bubble-menu>

      <!-- 表格气泡菜单（两种模式共用） -->
      <bubble-menu
        v-if="editable"
        :editor="editor"
        :should-show="shouldShowTableMenu"
        :tippy-options="{ duration: 100, arrow: false }">
        <div class="bubble-menu">
          <button type="button" class="bubble-btn" @mousedown.prevent @click="editor.chain().focus().addRowBefore().run()">行↑</button>
          <button type="button" class="bubble-btn" @mousedown.prevent @click="editor.chain().focus().addRowAfter().run()">行↓</button>
          <button type="button" class="bubble-btn" @mousedown.prevent @click="editor.chain().focus().deleteRow().run()">删行</button>
          <span class="bubble-sep"></span>
          <button type="button" class="bubble-btn" @mousedown.prevent @click="editor.chain().focus().addColumnBefore().run()">列←</button>
          <button type="button" class="bubble-btn" @mousedown.prevent @click="editor.chain().focus().addColumnAfter().run()">列→</button>
          <button type="button" class="bubble-btn" @mousedown.prevent @click="editor.chain().focus().deleteColumn().run()">删列</button>
          <span class="bubble-sep"></span>
          <button type="button" class="bubble-btn" @mousedown.prevent @click="editor.chain().focus().mergeCells().run()">合并</button>
          <button type="button" class="bubble-btn" @mousedown.prevent @click="editor.chain().focus().splitCell().run()">拆分</button>
          <button type="button" class="bubble-btn" @mousedown.prevent @click="editor.chain().focus().toggleHeaderRow().run()">表头</button>
          <span class="bubble-sep"></span>
          <button type="button" class="bubble-btn" @mousedown.prevent @click="editor.chain().focus().deleteTable().run()">删表</button>
        </div>
      </bubble-menu>

      <editor-content :editor="editor" />
    </template>

    <!-- 表格悬浮手柄 -->
    <div v-if="tableHandle && editable" class="table-handle" @mousedown.prevent>
      <div class="table-handle-group table-handle-col" :style="colGroupStyle">
        <button type="button" class="table-handle-btn" title="插入列" @click="addColumnAt">＋</button>
        <button type="button" class="table-handle-btn" title="删除列" @click="deleteColumnAt">－</button>
      </div>
      <div class="table-handle-group table-handle-row" :style="rowGroupStyle">
        <button type="button" class="table-handle-btn" title="插入行" @click="addRowAt">＋</button>
        <button type="button" class="table-handle-btn" title="删除行" @click="deleteRowAt">－</button>
      </div>
    </div>

    <!-- 图片上传隐藏输入 -->
    <input
      v-if="editable"
      ref="fileInput"
      type="file"
      accept="image/*"
      class="tiptap-file-input"
      @change="onFileChange" />
  </div>
</template>

<script>
  import { Editor, EditorContent, BubbleMenu, VueNodeViewRenderer } from '@tiptap/vue-2'
  import StarterKit from '@tiptap/starter-kit'
  import Image from '@tiptap/extension-image'
  import Table from '@tiptap/extension-table'
  import TableRow from '@tiptap/extension-table-row'
  import TableCell from '@tiptap/extension-table-cell'
  import TableHeader from '@tiptap/extension-table-header'
  import TaskList from '@tiptap/extension-task-list'
  import TaskItem from '@tiptap/extension-task-item'
  import Highlight from '@tiptap/extension-highlight'
  import Underline from '@tiptap/extension-underline'
  import Superscript from '@tiptap/extension-superscript'
  import Subscript from '@tiptap/extension-subscript'
  import TextAlign from '@tiptap/extension-text-align'
  import Link from '@tiptap/extension-link'
  import Placeholder from '@tiptap/extension-placeholder'
  import settings from '@/settings'
  import { uploadFile } from '@/api/api'
  import { createSlashCommand } from './slashCommand'
  import ResizableImageNode from './ResizableImageNode.vue'
  import { TextSelection } from '@tiptap/pm/state'
  import 'tippy.js/dist/tippy.css'

  // 扩展 Image，增加 align/width 属性，并用自定义 NodeView 支持拖拽调整大小
  const AlignableImage = Image.extend({
    addAttributes() {
      return {
        ...this.parent?.(),
        align: { default: null },
        width: { default: null },
      }
    },
    addNodeView() {
      return VueNodeViewRenderer(ResizableImageNode)
    },
  })

  // 工具栏 SVG 图标路径（与 tiptap.dev 固定工具栏一致）
  const ICONS = {
    undo: { evenodd: true, paths: ['M9.70711 3.70711C10.0976 3.31658 10.0976 2.68342 9.70711 2.29289C9.31658 1.90237 8.68342 1.90237 8.29289 2.29289L3.29289 7.29289C2.90237 7.68342 2.90237 8.31658 3.29289 8.70711L8.29289 13.7071C8.68342 14.0976 9.31658 14.0976 9.70711 13.7071C10.0976 13.3166 10.0976 12.6834 9.70711 12.2929L6.41421 9H14.5C15.0909 9 15.6761 9.1164 16.2221 9.34254C16.768 9.56869 17.2641 9.90016 17.682 10.318C18.0998 10.7359 18.4313 11.232 18.6575 11.7779C18.8836 12.3239 19 12.9091 19 13.5C19 14.0909 18.8836 14.6761 18.6575 15.2221C18.4313 15.768 18.0998 16.2641 17.682 16.682C17.2641 17.0998 16.768 17.4313 16.2221 17.6575C15.6761 17.8836 15.0909 18 14.5 18H11C10.4477 18 10 18.4477 10 19C10 19.5523 10.4477 20 11 20H14.5C15.3536 20 16.1988 19.8319 16.9874 19.5052C17.7761 19.1786 18.4926 18.6998 19.0962 18.0962C19.6998 17.4926 20.1786 16.7761 20.5052 15.9874C20.8319 15.1988 21 14.3536 21 13.5C21 12.6464 20.8319 11.8012 20.5052 11.0126C20.1786 10.2239 19.6998 9.50739 19.0962 8.90381C18.4926 8.30022 17.7761 7.82144 16.9874 7.49478C16.1988 7.16813 15.3536 7 14.5 7H6.41421L9.70711 3.70711Z'] },
    redo: { evenodd: true, paths: ['M15.7071 2.29289C15.3166 1.90237 14.6834 1.90237 14.2929 2.29289C13.9024 2.68342 13.9024 3.31658 14.2929 3.70711L17.5858 7H9.5C7.77609 7 6.12279 7.68482 4.90381 8.90381C3.68482 10.1228 3 11.7761 3 13.5C3 14.3536 3.16813 15.1988 3.49478 15.9874C3.82144 16.7761 4.30023 17.4926 4.90381 18.0962C6.12279 19.3152 7.77609 20 9.5 20H13C13.5523 20 14 19.5523 14 19C14 18.4477 13.5523 18 13 18H9.5C8.30653 18 7.16193 17.5259 6.31802 16.682C5.90016 16.2641 5.56869 15.768 5.34254 15.2221C5.1164 14.6761 5 14.0909 5 13.5C5 12.3065 5.47411 11.1619 6.31802 10.318C7.16193 9.47411 8.30653 9 9.5 9H17.5858L14.2929 12.2929C13.9024 12.6834 13.9024 13.3166 14.2929 13.7071C14.6834 14.0976 15.3166 14.0976 15.7071 13.7071L20.7071 8.70711C21.0976 8.31658 21.0976 7.68342 20.7071 7.29289L15.7071 2.29289Z'] },
    heading: { evenodd: false, paths: ['M6 3C6.55228 3 7 3.44772 7 4V11H17V4C17 3.44772 17.4477 3 18 3C18.5523 3 19 3.44772 19 4V20C19 20.5523 18.5523 21 18 21C17.4477 21 17 20.5523 17 20V13H7V20C7 20.5523 6.55228 21 6 21C5.44772 21 5 20.5523 5 20V4C5 3.44772 5.44772 3 6 3Z'] },
    'chevron-down': { evenodd: true, paths: ['M5.29289 8.29289C5.68342 7.90237 6.31658 7.90237 6.70711 8.29289L12 13.5858L17.2929 8.29289C17.6834 7.90237 18.3166 7.90237 18.7071 8.29289C19.0976 8.68342 19.0976 9.31658 18.7071 9.70711L12.7071 15.7071C12.3166 16.0976 11.6834 16.0976 11.2929 15.7071L5.29289 9.70711C4.90237 9.31658 4.90237 8.68342 5.29289 8.29289Z'] },
    list: { evenodd: true, paths: ['M7 6C7 5.44772 7.44772 5 8 5H21C21.5523 5 22 5.44772 22 6C22 6.55228 21.5523 7 21 7H8C7.44772 7 7 6.55228 7 6Z', 'M7 12C7 11.4477 7.44772 11 8 11H21C21.5523 11 22 11.4477 22 12C22 12.5523 21.5523 13 21 13H8C7.44772 13 7 12.5523 7 12Z', 'M7 18C7 17.4477 7.44772 17 8 17H21C21.5523 17 22 17.4477 22 18C22 18.5523 21.5523 19 21 19H8C7.44772 19 7 18.5523 7 18Z', 'M2 6C2 5.44772 2.44772 5 3 5H3.01C3.56228 5 4.01 5.44772 4.01 6C4.01 6.55228 3.56228 7 3.01 7H3C2.44772 7 2 6.55228 2 6Z', 'M2 12C2 11.4477 2.44772 11 3 11H3.01C3.56228 11 4.01 11.4477 4.01 12C4.01 12.5523 3.56228 13 3.01 13H3C2.44772 13 2 12.5523 2 12Z', 'M2 18C2 17.4477 2.44772 17 3 17H3.01C3.56228 17 4.01 17.4477 4.01 18C4.01 18.5523 3.56228 19 3.01 19H3C2.44772 19 2 18.5523 2 18Z'] },
    blockquote: { evenodd: true, paths: ['M8 6C8 5.44772 8.44772 5 9 5H16C16.5523 5 17 5.44772 17 6C17 6.55228 16.5523 7 16 7H9C8.44772 7 8 6.55228 8 6Z', 'M4 3C4.55228 3 5 3.44772 5 4L5 20C5 20.5523 4.55229 21 4 21C3.44772 21 3 20.5523 3 20L3 4C3 3.44772 3.44772 3 4 3Z', 'M8 12C8 11.4477 8.44772 11 9 11H20C20.5523 11 21 11.4477 21 12C21 12.5523 20.5523 13 20 13H9C8.44772 13 8 12.5523 8 12Z', 'M8 18C8 17.4477 8.44772 17 9 17H16C16.5523 17 17 17.4477 17 18C17 18.5523 16.5523 19 16 19H9C8.44772 19 8 18.5523 8 18Z'] },
    'code-block': { evenodd: true, paths: ['M6.70711 2.29289C7.09763 2.68342 7.09763 3.31658 6.70711 3.70711L4.41421 6L6.70711 8.29289C7.09763 8.68342 7.09763 9.31658 6.70711 9.70711C6.31658 10.0976 5.68342 10.0976 5.29289 9.70711L2.29289 6.70711C1.90237 6.31658 1.90237 5.68342 2.29289 5.29289L5.29289 2.29289C5.68342 1.90237 6.31658 1.90237 6.70711 2.29289Z', 'M10.2929 2.29289C10.6834 1.90237 11.3166 1.90237 11.7071 2.29289L14.7071 5.29289C15.0976 5.68342 15.0976 6.31658 14.7071 6.70711L11.7071 9.70711C11.3166 10.0976 10.6834 10.0976 10.2929 9.70711C9.90237 9.31658 9.90237 8.68342 10.2929 8.29289L12.5858 6L10.2929 3.70711C9.90237 3.31658 9.90237 2.68342 10.2929 2.29289Z', 'M17 4C17 3.44772 17.4477 3 18 3H19C20.6569 3 22 4.34315 22 6V18C22 19.6569 20.6569 21 19 21H5C3.34315 21 2 19.6569 2 18V12C2 11.4477 2.44772 11 3 11C3.55228 11 4 11.4477 4 12V18C4 18.5523 4.44772 19 5 19H19C19.5523 19 20 18.5523 20 18V6C20 5.44772 19.5523 5 19 5H18C17.4477 5 17 4.55228 17 4Z'] },
    bold: { evenodd: true, paths: ['M6 2.5C5.17157 2.5 4.5 3.17157 4.5 4V20C4.5 20.8284 5.17157 21.5 6 21.5H15C16.4587 21.5 17.8576 20.9205 18.8891 19.8891C19.9205 18.8576 20.5 17.4587 20.5 16C20.5 14.5413 19.9205 13.1424 18.8891 12.1109C18.6781 11.9 18.4518 11.7079 18.2128 11.5359C19.041 10.5492 19.5 9.29829 19.5 8C19.5 6.54131 18.9205 5.14236 17.8891 4.11091C16.8576 3.07946 15.4587 2.5 14 2.5H6ZM14 10.5C14.663 10.5 15.2989 10.2366 15.7678 9.76777C16.2366 9.29893 16.5 8.66304 16.5 8C16.5 7.33696 16.2366 6.70107 15.7678 6.23223C15.2989 5.76339 14.663 5.5 14 5.5H7.5V10.5H14ZM7.5 18.5V13.5H15C15.663 13.5 16.2989 13.7634 16.7678 14.2322C17.2366 14.7011 17.5 15.337 17.5 16C17.5 16.663 17.2366 17.2989 16.7678 17.7678C16.2989 18.2366 15.663 18.5 15 18.5H7.5Z'] },
    italic: { evenodd: false, paths: ['M15.0222 3H19C19.5523 3 20 3.44772 20 4C20 4.55228 19.5523 5 19 5H15.693L10.443 19H14C14.5523 19 15 19.4477 15 20C15 20.5523 14.5523 21 14 21H9.02418C9.00802 21.0004 8.99181 21.0004 8.97557 21H5C4.44772 21 4 20.5523 4 20C4 19.4477 4.44772 19 5 19H8.30704L13.557 5H10C9.44772 5 9 4.55228 9 4C9 3.44772 9.44772 3 10 3H14.9782C14.9928 2.99968 15.0075 2.99967 15.0222 3Z'] },
    strike: { evenodd: false, paths: ['M9.00039 3H16.0001C16.5524 3 17.0001 3.44772 17.0001 4C17.0001 4.55229 16.5524 5 16.0001 5H9.00011C8.68006 4.99983 8.36412 5.07648 8.07983 5.22349C7.79555 5.37051 7.55069 5.5836 7.36585 5.84487C7.181 6.10614 7.06155 6.40796 7.01754 6.72497C6.97352 7.04198 7.00623 7.36492 7.11292 7.66667C7.29701 8.18737 7.02414 8.75872 6.50344 8.94281C5.98274 9.1269 5.4114 8.85403 5.2273 8.33333C5.01393 7.72984 4.94851 7.08396 5.03654 6.44994C5.12456 5.81592 5.36346 5.21229 5.73316 4.68974C6.10285 4.1672 6.59256 3.74101 7.16113 3.44698C7.72955 3.15303 8.36047 2.99975 9.00039 3Z', 'M18 13H20C20.5523 13 21 12.5523 21 12C21 11.4477 20.5523 11 20 11H4C3.44772 11 3 11.4477 3 12C3 12.5523 3.44772 13 4 13H14C14.7956 13 15.5587 13.3161 16.1213 13.8787C16.6839 14.4413 17 15.2044 17 16C17 16.7956 16.6839 17.5587 16.1213 18.1213C15.5587 18.6839 14.7956 19 14 19H6C5.44772 19 5 19.4477 5 20C5 20.5523 5.44772 21 6 21H14C15.3261 21 16.5979 20.4732 17.5355 19.5355C18.4732 18.5979 19 17.3261 19 16C19 14.9119 18.6453 13.8604 18 13Z'] },
    code: { evenodd: false, paths: ['M15.4545 4.2983C15.6192 3.77115 15.3254 3.21028 14.7983 3.04554C14.2712 2.88081 13.7103 3.1746 13.5455 3.70175L8.54554 19.7017C8.38081 20.2289 8.6746 20.7898 9.20175 20.9545C9.72889 21.1192 10.2898 20.8254 10.4545 20.2983L15.4545 4.2983Z', 'M6.70711 7.29289C7.09763 7.68342 7.09763 8.31658 6.70711 8.70711L3.41421 12L6.70711 15.2929C7.09763 15.6834 7.09763 16.3166 6.70711 16.7071C6.31658 17.0976 5.68342 17.0976 5.29289 16.7071L1.29289 12.7071C0.902369 12.3166 0.902369 11.6834 1.29289 11.2929L5.29289 7.29289C5.68342 6.90237 6.31658 6.90237 6.70711 7.29289Z', 'M17.2929 7.29289C17.6834 6.90237 18.3166 6.90237 18.7071 7.29289L22.7071 11.2929C23.0976 11.6834 23.0976 12.3166 22.7071 12.7071L18.7071 16.7071C18.3166 17.0976 17.6834 17.0976 17.2929 16.7071C16.9024 16.3166 16.9024 15.6834 17.2929 15.2929L20.5858 12L17.2929 8.70711C16.9024 8.31658 16.9024 7.68342 17.2929 7.29289Z'] },
    underline: { evenodd: true, paths: ['M7 4C7 3.44772 6.55228 3 6 3C5.44772 3 5 3.44772 5 4V10C5 11.8565 5.7375 13.637 7.05025 14.9497C8.36301 16.2625 10.1435 17 12 17C13.8565 17 15.637 16.2625 16.9497 14.9497C18.2625 13.637 19 11.8565 19 10V4C19 3.44772 18.5523 3 18 3C17.4477 3 17 3.44772 17 4V10C17 11.3261 16.4732 12.5979 15.5355 13.5355C14.5979 14.4732 13.3261 15 12 15C10.6739 15 9.40215 14.4732 8.46447 13.5355C7.52678 12.5979 7 11.3261 7 10V4ZM4 19C3.44772 19 3 19.4477 3 20C3 20.5523 3.44772 21 4 21H20C20.5523 21 21 20.5523 21 20C21 19.4477 20.5523 19 20 19H4Z'] },
    highlight: { evenodd: true, paths: ['M14.7072 4.70711C15.0977 4.31658 15.0977 3.68342 14.7072 3.29289C14.3167 2.90237 13.6835 2.90237 13.293 3.29289L8.69294 7.89286L8.68594 7.9C8.13626 8.46079 7.82837 9.21474 7.82837 10C7.82837 10.2306 7.85491 10.4584 7.90631 10.6795L2.29289 16.2929C2.10536 16.4804 2 16.7348 2 17V20C2 20.5523 2.44772 21 3 21H12C12.2652 21 12.5196 20.8946 12.7071 20.7071L15.3205 18.0937C15.5416 18.1452 15.7695 18.1717 16.0001 18.1717C16.7853 18.1717 17.5393 17.8639 18.1001 17.3142L22.7072 12.7071C23.0977 12.3166 23.0977 11.6834 22.7072 11.2929C22.3167 10.9024 21.6835 10.9024 21.293 11.2929L16.6971 15.8887C16.5105 16.0702 16.2605 16.1717 16.0001 16.1717C15.7397 16.1717 15.4897 16.0702 15.303 15.8887L10.1113 10.697C9.92992 10.5104 9.82837 10.2604 9.82837 10C9.82837 9.73963 9.92992 9.48958 10.1113 9.30297L14.7072 4.70711ZM13.5858 17L9.00004 12.4142L4 17.4142V19H11.5858L13.5858 17Z'] },
    link: { evenodd: false, paths: ['M16.9958 1.06669C15.4226 1.05302 13.907 1.65779 12.7753 2.75074L12.765 2.76086L11.045 4.47086C10.6534 4.86024 10.6515 5.49341 11.0409 5.88507C11.4303 6.27673 12.0634 6.27858 12.4551 5.88919L14.1697 4.18456C14.9236 3.45893 15.9319 3.05752 16.9784 3.06662C18.0272 3.07573 19.0304 3.49641 19.772 4.23804C20.5137 4.97967 20.9344 5.98292 20.9435 7.03171C20.9526 8.07776 20.5515 9.08563 19.8265 9.83941L16.833 12.8329C16.4274 13.2386 15.9393 13.5524 15.4019 13.7529C14.8645 13.9533 14.2903 14.0359 13.7181 13.9949C13.146 13.9539 12.5894 13.7904 12.0861 13.5154C11.5827 13.2404 11.1444 12.8604 10.8008 12.401C10.47 11.9588 9.84333 11.8685 9.40108 12.1993C8.95883 12.5301 8.86849 13.1568 9.1993 13.599C9.71464 14.288 10.3721 14.858 11.1272 15.2705C11.8822 15.683 12.7171 15.9283 13.5753 15.9898C14.4334 16.0513 15.2948 15.9274 16.1009 15.6267C16.907 15.326 17.639 14.8555 18.2473 14.247L21.2472 11.2471L21.2593 11.2347C22.3523 10.1031 22.9571 8.58751 22.9434 7.01433C22.9297 5.44115 22.2987 3.93628 21.1863 2.82383C20.0738 1.71138 18.5689 1.08036 16.9958 1.06669Z', 'M10.4247 8.0102C9.56657 7.94874 8.70522 8.07256 7.89911 8.37326C7.09305 8.67395 6.36096 9.14458 5.75272 9.753L2.75285 12.7529L2.74067 12.7653C1.64772 13.8969 1.04295 15.4125 1.05662 16.9857C1.07029 18.5589 1.70131 20.0637 2.81376 21.1762C3.9262 22.2886 5.43108 22.9196 7.00426 22.9333C8.57744 22.947 10.0931 22.3422 11.2247 21.2493L11.2371 21.2371L12.9471 19.5271C13.3376 19.1366 13.3376 18.5034 12.9471 18.1129C12.5565 17.7223 11.9234 17.7223 11.5328 18.1129L9.82932 19.8164C9.07555 20.5414 8.06768 20.9425 7.02164 20.9334C5.97285 20.9243 4.9696 20.5036 4.22797 19.762C3.48634 19.0203 3.06566 18.0171 3.05655 16.9683C3.04746 15.9222 3.44851 14.9144 4.17355 14.1606L7.16719 11.167C7.5727 10.7613 8.06071 10.4476 8.59811 10.2471C9.13552 10.0467 9.70976 9.96412 10.2819 10.0051C10.854 10.0461 11.4106 10.2096 11.9139 10.4846C12.4173 10.7596 12.8556 11.1397 13.1992 11.599C13.53 12.0412 14.1567 12.1316 14.5989 11.8007C15.0412 11.4699 15.1315 10.8433 14.8007 10.401C14.2854 9.71205 13.6279 9.14198 12.8729 8.72948C12.1178 8.31697 11.2829 8.07166 10.4247 8.0102Z'] },
    superscript: { evenodd: true, paths: ['M12.7071 7.29289C13.0976 7.68342 13.0976 8.31658 12.7071 8.70711L4.70711 16.7071C4.31658 17.0976 3.68342 17.0976 3.29289 16.7071C2.90237 16.3166 2.90237 15.6834 3.29289 15.2929L11.2929 7.29289C11.6834 6.90237 12.3166 6.90237 12.7071 7.29289Z', 'M3.29289 7.29289C3.68342 6.90237 4.31658 6.90237 4.70711 7.29289L12.7071 15.2929C13.0976 15.6834 13.0976 16.3166 12.7071 16.7071C12.3166 17.0976 11.6834 17.0976 11.2929 16.7071L3.29289 8.70711C2.90237 8.31658 2.90237 7.68342 3.29289 7.29289Z', 'M17.405 1.40657C18.0246 1.05456 18.7463 0.92634 19.4492 1.04344C20.1521 1.16054 20.7933 1.51583 21.2652 2.0497L21.2697 2.05469L21.2696 2.05471C21.7431 2.5975 22 3.28922 22 4.00203C22 5.08579 21.3952 5.84326 20.7727 6.34289C20.1966 6.80531 19.4941 7.13675 18.9941 7.37261C18.9714 7.38332 18.9491 7.39383 18.9273 7.40415C18.4487 7.63034 18.2814 7.78152 18.1927 7.91844C18.1778 7.94155 18.1625 7.96834 18.1473 8.00003H21C21.5523 8.00003 22 8.44774 22 9.00003C22 9.55231 21.5523 10 21 10H17C16.4477 10 16 9.55231 16 9.00003C16 8.17007 16.1183 7.44255 16.5138 6.83161C16.9107 6.21854 17.4934 5.86971 18.0728 5.59591C18.6281 5.33347 19.1376 5.09075 19.5208 4.78316C19.8838 4.49179 20 4.25026 20 4.00203C20 3.77192 19.9178 3.54865 19.7646 3.37182C19.5968 3.18324 19.3696 3.05774 19.1205 3.01625C18.8705 2.97459 18.6137 3.02017 18.3933 3.14533C18.1762 3.26898 18.0191 3.45826 17.9406 3.67557C17.7531 4.19504 17.18 4.46414 16.6605 4.27662C16.141 4.0891 15.8719 3.51596 16.0594 2.99649C16.303 2.3219 16.7817 1.76125 17.4045 1.40689L17.405 1.40657Z'] },
    subscript: { evenodd: true, paths: ['M3.29289 7.29289C3.68342 6.90237 4.31658 6.90237 4.70711 7.29289L12.7071 15.2929C13.0976 15.6834 13.0976 16.3166 12.7071 16.7071C12.3166 17.0976 11.6834 17.0976 11.2929 16.7071L3.29289 8.70711C2.90237 8.31658 2.90237 7.68342 3.29289 7.29289Z', 'M12.7071 7.29289C13.0976 7.68342 13.0976 8.31658 12.7071 8.70711L4.70711 16.7071C4.31658 17.0976 3.68342 17.0976 3.29289 16.7071C2.90237 16.3166 2.90237 15.6834 3.29289 15.2929L11.2929 7.29289C11.6834 6.90237 12.3166 6.90237 12.7071 7.29289Z', 'M17.4079 14.3995C18.0284 14.0487 18.7506 13.9217 19.4536 14.0397C20.1566 14.1578 20.7977 14.5138 21.2696 15.0481L21.2779 15.0574L21.2778 15.0575C21.7439 15.5988 22 16.2903 22 17C22 18.0823 21.3962 18.8401 20.7744 19.3404C20.194 19.8073 19.4858 20.141 18.9828 20.378C18.9638 20.387 18.9451 20.3958 18.9266 20.4045C18.4473 20.6306 18.2804 20.7817 18.1922 20.918C18.1773 20.9412 18.1619 20.9681 18.1467 21H21C21.5523 21 22 21.4477 22 22C22 22.5523 21.5523 23 21 23H17C16.4477 23 16 22.5523 16 22C16 21.1708 16.1176 20.4431 16.5128 19.832C16.9096 19.2184 17.4928 18.8695 18.0734 18.5956C18.6279 18.334 19.138 18.0901 19.5207 17.7821C19.8838 17.49 20 17.2477 20 17C20 16.7718 19.9176 16.5452 19.7663 16.3672C19.5983 16.1792 19.3712 16.0539 19.1224 16.0121C18.8722 15.9701 18.6152 16.015 18.3942 16.1394C18.1794 16.2628 18.0205 16.4549 17.9422 16.675C17.7572 17.1954 17.1854 17.4673 16.665 17.2822C16.1446 17.0972 15.8728 16.5254 16.0578 16.005C16.2993 15.3259 16.7797 14.7584 17.4039 14.4018L17.4079 14.3995L17.4079 14.3995Z'] },
    'align-left': { evenodd: true, paths: ['M2 6C2 5.44772 2.44772 5 3 5H21C21.5523 5 22 5.44772 22 6C22 6.55228 21.5523 7 21 7H3C2.44772 7 2 6.55228 2 6Z', 'M2 12C2 11.4477 2.44772 11 3 11H15C15.5523 11 16 11.4477 16 12C16 12.5523 15.5523 13 15 13H3C2.44772 13 2 12.5523 2 12Z', 'M2 18C2 17.4477 2.44772 17 3 17H17C17.5523 17 18 17.4477 18 18C18 18.5523 17.5523 19 17 19H3C2.44772 19 2 18.5523 2 18Z'] },
    'align-center': { evenodd: true, paths: ['M2 6C2 5.44772 2.44772 5 3 5H21C21.5523 5 22 5.44772 22 6C22 6.55228 21.5523 7 21 7H3C2.44772 7 2 6.55228 2 6Z', 'M6 12C6 11.4477 6.44772 11 7 11H17C17.5523 11 18 11.4477 18 12C18 12.5523 17.5523 13 17 13H7C6.44772 13 6 12.5523 6 12Z', 'M4 18C4 17.4477 4.44772 17 5 17H19C19.5523 17 20 17.4477 20 18C20 18.5523 19.5523 19 19 19H5C4.44772 19 4 18.5523 4 18Z'] },
    'align-right': { evenodd: true, paths: ['M2 6C2 5.44772 2.44772 5 3 5H21C21.5523 5 22 5.44772 22 6C22 6.55228 21.5523 7 21 7H3C2.44772 7 2 6.55228 2 6Z', 'M8 12C8 11.4477 8.44772 11 9 11H21C21.5523 11 22 11.4477 22 12C22 12.5523 21.5523 13 21 13H9C8.44772 13 8 12.5523 8 12Z', 'M6 18C6 17.4477 6.44772 17 7 17H21C21.5523 17 22 17.4477 22 18C22 18.5523 21.5523 19 21 19H7C6.44772 19 6 18.5523 6 18Z'] },
    'align-justify': { evenodd: true, paths: ['M2 6C2 5.44772 2.44772 5 3 5H21C21.5523 5 22 5.44772 22 6C22 6.55228 21.5523 7 21 7H3C2.44772 7 2 6.55228 2 6Z', 'M2 12C2 11.4477 2.44772 11 3 11H21C21.5523 11 22 11.4477 22 12C22 12.5523 21.5523 13 21 13H3C2.44772 13 2 12.5523 2 12Z', 'M2 18C2 17.4477 2.44772 17 3 17H21C21.5523 17 22 17.4477 22 18C22 18.5523 21.5523 19 21 19H3C2.44772 19 2 18.5523 2 18Z'] },
    image: { evenodd: true, paths: ['M20 2C20 1.44772 19.5523 1 19 1C18.4477 1 18 1.44772 18 2V4H16C15.4477 4 15 4.44772 15 5C15 5.55228 15.4477 6 16 6H18V8C18 8.55228 18.4477 9 19 9C19.5523 9 20 8.55228 20 8V6H22C22.5523 6 23 5.55228 23 5C23 4.44772 22.5523 4 22 4H20V2ZM5 4C4.73478 4 4.48043 4.10536 4.29289 4.29289C4.10536 4.48043 4 4.73478 4 5V19C4 19.2652 4.10536 19.5196 4.29289 19.7071C4.48043 19.8946 4.73478 20 5 20H5.58579L14.379 11.2068C14.9416 10.6444 15.7045 10.3284 16.5 10.3284C17.2955 10.3284 18.0584 10.6444 18.621 11.2068L20 12.5858V12C20 11.4477 20.4477 11 21 11C21.5523 11 22 11.4477 22 12V14.998C22 14.9994 22 15.0007 22 15.002V19C22 19.7957 21.6839 20.5587 21.1213 21.1213C20.5587 21.6839 19.7957 22 19 22H6.00219C6.00073 22 5.99927 22 5.99781 22H5C4.20435 22 3.44129 21.6839 2.87868 21.1213C2.31607 20.5587 2 19.7957 2 19V5C2 4.20435 2.31607 3.44129 2.87868 2.87868C3.44129 2.31607 4.20435 2 5 2H12C12.5523 2 13 2.44772 13 3C13 3.55228 12.5523 4 12 4H5ZM8.41422 20H19C19.2652 20 19.5196 19.8946 19.7071 19.7071C19.8946 19.5196 20 19.2652 20 19V15.4142L17.207 12.6212C17.0195 12.4338 16.7651 12.3284 16.5 12.3284C16.2349 12.3284 15.9806 12.4337 15.7931 12.6211L8.41422 20ZM6.87868 6.87868C7.44129 6.31607 8.20435 6 9 6C9.79565 6 10.5587 6.31607 11.1213 6.87868C11.6839 7.44129 12 8.20435 12 9C12 9.79565 11.6839 10.5587 11.1213 11.1213C10.5587 11.6839 9.79565 12 9 12C8.20435 12 7.44129 11.6839 6.87868 11.1213C6.31607 10.5587 6 9.79565 6 9C6 8.20435 6.31607 7.44129 6.87868 6.87868ZM9 8C8.73478 8 8.48043 8.10536 8.29289 8.29289C8.10536 8.48043 8 8.73478 8 9C8 9.26522 8.10536 9.51957 8.29289 9.70711C8.48043 9.89464 8.73478 10 9 10C9.26522 10 9.51957 9.89464 9.70711 9.70711C9.89464 9.51957 10 9.26522 10 9C10 8.73478 9.89464 8.48043 9.70711 8.29289C9.51957 8.10536 9.26522 8 9 8Z'] },
  }

  // 轻量功能组件：按 name 渲染工具栏 SVG 图标
  const TipTapIcon = {
    functional: true,
    props: {
      name: { type: String, required: true },
      small: { type: Boolean, default: false },
    },
    render(h, ctx) {
      const icon = ICONS[ctx.props.name]
      if (!icon) return null
      const paths = icon.paths.map(d => {
        const attrs = { d, fill: 'currentColor' }
        if (icon.evenodd) {
          attrs['fill-rule'] = 'evenodd'
          attrs['clip-rule'] = 'evenodd'
        }
        return h('path', { attrs })
      })
      return h('svg', {
        attrs: { width: 24, height: 24, viewBox: '0 0 24 24', fill: 'currentColor', xmlns: 'http://www.w3.org/2000/svg' },
        class: ctx.props.small ? 'tiptap-button-dropdown-small' : 'tiptap-button-icon',
      }, paths)
    },
  }

  export default {
    name: 'GTipTapEditor',
    components: {
      EditorContent,
      BubbleMenu,
      TipTapIcon,
    },
    props: {
      value: { type: String, default: '' },
      readonly: { type: Boolean, default: false },
      disabled: { type: Boolean, default: false },
      placeholder: { type: String, default: '输入 / 唤起菜单…' },
      /* 工具栏形态：simple = 固定顶部工具栏；notion = 选中气泡工具栏 */
      mode: { type: String, default: 'notion' },
    },
    data() {
      return {
        editor: null,
        internalChange: false,
        pendingImageRange: null,
        showMore: false,
        selectionTick: 0,
        tableHandle: null,
      }
    },
    computed: {
      editable() {
        return !this.readonly && !this.disabled
      },
      currentBlockType() {
        this.selectionTick
        const e = this.editor
        if (!e) return 'paragraph'
        if (e.isActive('heading', { level: 1 })) return 'h1'
        if (e.isActive('heading', { level: 2 })) return 'h2'
        if (e.isActive('heading', { level: 3 })) return 'h3'
        if (e.isActive('bulletList')) return 'bulletList'
        if (e.isActive('orderedList')) return 'orderedList'
        if (e.isActive('taskList')) return 'taskList'
        if (e.isActive('blockquote')) return 'blockquote'
        if (e.isActive('codeBlock')) return 'codeBlock'
        return 'paragraph'
      },
      /* 顶部工具栏按钮激活态（引用 selectionTick 建立响应式） */
      toolbarState() {
        this.selectionTick
        const e = this.editor
        if (!e) return {}
        return {
          canUndo: e.can().undo(),
          canRedo: e.can().redo(),
          blockquote: e.isActive('blockquote'),
          codeBlock: e.isActive('codeBlock'),
          bold: e.isActive('bold'),
          italic: e.isActive('italic'),
          strike: e.isActive('strike'),
          code: e.isActive('code'),
          underline: e.isActive('underline'),
          highlight: e.isActive('highlight'),
          link: e.isActive('link'),
          superscript: e.isActive('superscript'),
          subscript: e.isActive('subscript'),
          alignLeft: e.isActive({ textAlign: 'left' }),
          alignCenter: e.isActive({ textAlign: 'center' }),
          alignRight: e.isActive({ textAlign: 'right' }),
          alignJustify: e.isActive({ textAlign: 'justify' }),
        }
      },
      colGroupStyle() {
        const h = this.tableHandle
        if (!h) return {}
        return { left: `${h.colLeft}px`, top: `${h.tableTop - 4}px`, transform: 'translate(-50%, -100%)' }
      },
      rowGroupStyle() {
        const h = this.tableHandle
        if (!h) return {}
        return { left: `${h.tableLeft - 4}px`, top: `${h.rowTop}px`, transform: 'translate(-100%, -50%)' }
      },
    },
    watch: {
      value(newVal) {
        if (!this.editor || this.internalChange) return
        if (newVal === JSON.stringify(this.editor.getJSON())) return
        this.editor.commands.setContent(this.parseValue(newVal), false)
      },
    },
    mounted() {
      const editable = !this.readonly && !this.disabled
      this.editor = new Editor({
        content: this.parseValue(this.value),
        editable,
        extensions: [
          StarterKit.configure({ heading: { levels: [1, 2, 3, 4] } }),
          AlignableImage.configure({ inline: false }),
          Table.configure({ resizable: true }),
          TableRow,
          TableHeader,
          TableCell,
          TaskList,
          TaskItem.configure({ nested: true }),
          Highlight,
          Underline,
          Superscript,
          Subscript,
          TextAlign.configure({ types: ['heading', 'paragraph'] }),
          Link.configure({
            openOnClick: !editable,
            autolink: true,
            linkOnPaste: true,
            HTMLAttributes: { rel: 'noopener noreferrer nofollow', target: '_blank' },
          }),
          Placeholder.configure({ placeholder: this.placeholder }),
          createSlashCommand({ onPickImage: this.onPickImage }),
        ],
        editorProps: {
          handlePaste: (view, event) => {
            const items = event.clipboardData && event.clipboardData.items
            if (items) {
              for (const item of items) {
                if (item.type && item.type.startsWith('image/')) {
                  event.preventDefault()
                  this.uploadImage(item.getAsFile())
                  return true
                }
              }
            }
            return false
          },
          handleDrop: (view, event) => {
            const files = event.dataTransfer && event.dataTransfer.files
            if (files && files.length) {
              const images = Array.from(files).filter(f => f.type && f.type.startsWith('image/'))
              if (images.length) {
                event.preventDefault()
                images.forEach(file => this.uploadImage(file))
                return true
              }
            }
            return false
          },
        },
        onUpdate: () => {
          this.internalChange = true
          const json = JSON.stringify(this.editor.getJSON())
          this.$emit('input', json)
          this.$emit('change', json)
          this.$nextTick(() => { this.internalChange = false })
        },
      })

      // 每次事务（含选区变化）递增 selectionTick，驱动工具栏激活态刷新
      this.editor.on('transaction', () => { this.selectionTick++ })
    },
    beforeDestroy() {
      this.cancelClearHandle()
      if (this.editor) {
        this.editor.destroy()
        this.editor = null
      }
    },
    methods: {
      parseValue(str) {
        if (!str) return ''
        try {
          const obj = JSON.parse(str)
          return obj && obj.type === 'doc' ? obj : ''
        } catch (e) {
          return ''
        }
      },
      /* ---------- 顶部工具栏命令 ---------- */
      cmdUndo() { this.editor.chain().focus().undo().run() },
      cmdRedo() { this.editor.chain().focus().redo().run() },
      setParagraph() { this.editor.chain().focus().setParagraph().run() },
      setHeading(level) { this.editor.chain().focus().setHeading({ level }).run() },
      toggleBulletList() { this.editor.chain().focus().toggleBulletList().run() },
      toggleOrderedList() { this.editor.chain().focus().toggleOrderedList().run() },
      toggleTaskList() { this.editor.chain().focus().toggleTaskList().run() },
      toggleBlockquote() { this.editor.chain().focus().toggleBlockquote().run() },
      toggleCodeBlock() { this.editor.chain().focus().toggleCodeBlock().run() },
      toggleBold() { this.editor.chain().focus().toggleBold().run() },
      toggleItalic() { this.editor.chain().focus().toggleItalic().run() },
      toggleStrike() { this.editor.chain().focus().toggleStrike().run() },
      toggleCode() { this.editor.chain().focus().toggleCode().run() },
      toggleUnderline() { this.editor.chain().focus().toggleUnderline().run() },
      toggleHighlight() { this.editor.chain().focus().toggleHighlight().run() },
      toggleSuperscript() { this.editor.chain().focus().toggleSuperscript().run() },
      toggleSubscript() { this.editor.chain().focus().toggleSubscript().run() },
      setAlign(align) { this.editor.chain().focus().setTextAlign(align).run() },
      onHeadingMenu({ key }) {
        if (key === 'paragraph') this.setParagraph()
        else if (key === 'h1') this.setHeading(1)
        else if (key === 'h2') this.setHeading(2)
        else if (key === 'h3') this.setHeading(3)
      },
      onListMenu({ key }) {
        if (key === 'bullet') this.toggleBulletList()
        else if (key === 'ordered') this.toggleOrderedList()
        else if (key === 'task') this.toggleTaskList()
      },
      onToolbarImage() {
        if (this.$refs.fileInput) {
          this.$refs.fileInput.value = ''
          this.$refs.fileInput.click()
        }
      },
      toggleLink() {
        if (!this.editor) return
        const prev = this.editor.getAttributes('link').href
        if (prev) {
          this.editor.chain().focus().unsetLink().run()
          return
        }
        const url = window.prompt('链接地址', 'https://')
        if (url === null) return
        const href = (url || '').trim()
        if (!href) {
          this.editor.chain().focus().unsetLink().run()
          return
        }
        this.editor.chain().focus().setLink({ href }).run()
      },
      /* ---------- 气泡菜单 ---------- */
      shouldShowBubbleMenu({ editor }) {
        if (!editor || editor.state.selection.empty) return false
        if (editor.isActive('codeBlock')) return false
        if (editor.state.selection.node) return false
        return true
      },
      shouldShowImageMenu({ editor }) {
        if (!editor) return false
        const sel = editor.state.selection
        return !!(sel && sel.node && sel.node.type.name === 'image')
      },
      shouldShowTableMenu({ editor }) {
        if (!editor) return false
        return false && editor.isActive('table') && editor.state.selection.empty
      },
      setImageAlign(align) {
        if (!this.editor) return
        this.editor.chain().focus().updateAttributes('image', { align }).run()
      },
      deleteImage() {
        if (!this.editor) return
        this.editor.chain().focus().deleteSelection().run()
      },
      onSinkListItem() {
        if (!this.editor) return
        const type = this.editor.isActive('taskItem') ? 'taskItem' : 'listItem'
        this.editor.chain().focus().sinkListItem(type).run()
      },
      onLiftListItem() {
        if (!this.editor) return
        const type = this.editor.isActive('taskItem') ? 'taskItem' : 'listItem'
        this.editor.chain().focus().liftListItem(type).run()
      },
      onTurnInto(event) {
        if (!this.editor) return
        const value = event.target.value
        const chain = this.editor.chain().focus()
        switch (value) {
          case 'paragraph': chain.setParagraph().run(); break
          case 'h1': chain.setHeading({ level: 1 }).run(); break
          case 'h2': chain.setHeading({ level: 2 }).run(); break
          case 'h3': chain.setHeading({ level: 3 }).run(); break
          case 'bulletList': chain.toggleBulletList().run(); break
          case 'orderedList': chain.toggleOrderedList().run(); break
          case 'taskList': chain.toggleTaskList().run(); break
          case 'blockquote': chain.toggleBlockquote().run(); break
          case 'codeBlock': chain.toggleCodeBlock().run(); break
        }
      },
      /* ---------- 图片 ---------- */
      onPickImage({ editor, range }) {
        editor || true
        this.pendingImageRange = range
        if (this.$refs.fileInput) {
          this.$refs.fileInput.value = ''
          this.$refs.fileInput.click()
        }
      },
      onFileChange(e) {
        const file = e.target && e.target.files && e.target.files[0]
        if (file) this.uploadImage(file)
      },
      uploadImage(file) {
        if (!file || !this._beforeUpload(file)) return
        const formData = new FormData()
        formData.set('upload_file', file)
        uploadFile(formData).then(resp => {
          const gmodel = resp && resp.gmodel
          const fileUuid = gmodel && gmodel[0] && gmodel[0].fileUuid
          if (fileUuid) {
            this.insertImage(`${settings.BASE_URL}/gcsStorage/downloadFile/${fileUuid}`)
          }
        }).catch(() => {})
      },
      insertImage(url) {
        if (!this.editor) return
        const range = this.pendingImageRange
        this.pendingImageRange = null
        if (range) {
          this.editor.chain().focus().deleteRange(range).setImage({ src: url }).run()
        } else {
          this.editor.chain().focus().setImage({ src: url }).run()
        }
      },
      _beforeUpload(file) {
        if (!file.type) return true
        const isImage = file.type.startsWith('image/')
        if (!isImage) this.$message.warn('只能上传图片文件!')
        const isLt2M = file.size / 1024 / 1024 < 2
        if (!isLt2M) this.$message.warn('文件大小不能超过2MB!')
        return isImage && isLt2M
      },
      /* ---------- 表格悬浮手柄 ---------- */
      onEditorMouseMove(e) {
        if (!this.editor || !this.editable) return
        const target = e.target
        if (target && target.closest && target.closest('.table-handle')) {
          this.cancelClearHandle()
          return
        }
        const cell = target && target.closest ? target.closest('td, th') : null
        if (!cell || !this.editor.view.dom.contains(cell)) {
          this.scheduleClearHandle()
          return
        }
        this.cancelClearHandle()
        this.updateTableHandle(cell)
      },
      onEditorMouseLeave() {
        this.scheduleClearHandle()
      },
      updateTableHandle(cell) {
        const table = cell.closest('table')
        if (!table) {
          this.tableHandle = null
          return
        }
        const editorRect = this.$el.getBoundingClientRect()
        const cellRect = cell.getBoundingClientRect()
        const tableRect = table.getBoundingClientRect()
        this.tableHandle = {
          cell,
          colLeft: cellRect.left - editorRect.left + cellRect.width / 2,
          rowTop: cellRect.top - editorRect.top + cellRect.height / 2,
          tableTop: tableRect.top - editorRect.top,
          tableLeft: tableRect.left - editorRect.left,
        }
      },
      scheduleClearHandle() {
        if (this._clearTimer) clearTimeout(this._clearTimer)
        this._clearTimer = setTimeout(() => { this.tableHandle = null }, 180)
      },
      cancelClearHandle() {
        if (this._clearTimer) {
          clearTimeout(this._clearTimer)
          this._clearTimer = null
        }
      },
      selectCell(cell) {
        const pos = this.editor.view.posAtDOM(cell, 0)
        const $pos = this.editor.state.doc.resolve(pos)
        this.editor.view.dispatch(this.editor.state.tr.setSelection(TextSelection.near($pos)))
      },
      addRowAt() {
        const h = this.tableHandle
        if (!h) return
        this.selectCell(h.cell)
        this.editor.chain().focus().addRowAfter().run()
        this.tableHandle = null
      },
      addColumnAt() {
        const h = this.tableHandle
        if (!h) return
        this.selectCell(h.cell)
        this.editor.chain().focus().addColumnAfter().run()
        this.tableHandle = null
      },
      deleteRowAt() {
        const h = this.tableHandle
        if (!h) return
        this.selectCell(h.cell)
        this.editor.chain().focus().deleteRow().run()
        this.tableHandle = null
      },
      deleteColumnAt() {
        const h = this.tableHandle
        if (!h) return
        this.selectCell(h.cell)
        this.editor.chain().focus().deleteColumn().run()
        this.tableHandle = null
      },
    },
  }
</script>

<style lang="scss" scoped>
  .gtip-tap-editor {
    position: relative;
    // border: 1px solid #e5e6eb;
    // border-radius: 8px;
    overflow: hidden;

    .tiptap-file-input { display: none; }

    .table-handle {
      position: absolute;
      top: 0;
      left: 0;
      width: 0;
      height: 0;
      pointer-events: none;
      z-index: 20;
    }
    .table-handle-group {
      position: absolute;
      display: flex;
      gap: 3px;
      pointer-events: auto;
    }
    .table-handle-btn {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 18px;
      height: 18px;
      padding: 0;
      border: 1px solid #d6d9de;
      border-radius: 50%;
      background: #fff;
      color: #4e5969;
      font-size: 14px;
      cursor: pointer;
      box-shadow: 0 1px 4px rgba(0, 0, 0, 0.12);
      &:hover { background: #e8f0ff; color: #1456f0; border-color: #1456f0; }
    }
  }

  /* ---------- 顶部工具栏（simple 模式） ---------- */
  .tiptap-toolbar {
    display: flex;
    align-items: center;
    gap: 4px;
    padding: 6px 8px;
    border-bottom: 1px solid #e5e6eb;
    background: #fafbfc;
    flex-wrap: wrap;
  }
  .tiptap-toolbar-group {
    display: flex;
    align-items: center;
    gap: 2px;
  }
  .tiptap-separator {
    flex-shrink: 0;
    width: 1px;
    height: 20px;
    margin: 0 4px;
    background: #e5e6eb;
  }
  .tiptap-button {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    height: 30px;
    padding: 0 6px;
    border: none;
    border-radius: 6px;
    background: transparent;
    color: #4e5969;
    cursor: pointer;
    transition: background-color 0.15s ease, color 0.15s ease;

    &:hover { background: #eef1f6; color: #1f2329; }
    &:disabled { opacity: 0.4; cursor: not-allowed; }
    &[data-active-state='on'] { background: #e8f0ff; color: #1456f0; }
  }
  .tiptap-button-icon {
    width: 16px;
    height: 16px;
    flex-shrink: 0;
  }
  .tiptap-button-dropdown-small {
    width: 12px;
    height: 12px;
    flex-shrink: 0;
    opacity: 0.6;
  }
  .tiptap-button-text {
    font-size: 13px;
  }
</style>

<style lang="scss">
  // ProseMirror 内容基础样式（非 scoped，作用于 EditorContent 内部 DOM）
  .gtip-tap-editor {
    .ProseMirror {
      min-height: 320px;
      // padding: 16px 20px;
      outline: none;
      font-size: 15px;
      line-height: 1.7;
      color: #1f2329;
      word-break: break-word;

      > * + * { margin-top: 8px; }

      p { margin-bottom: 0; }

      h1 { font-size: 28px; font-weight: 700; line-height: 1.2; margin: 4px 0 10px; }
      h2 { font-size: 23px; font-weight: 700; line-height: 1.2; margin: 3px 0 8px; }
      h3 { font-size: 19px; font-weight: 600; line-height: 1.3; margin: 3px 0 8px; }
      h4 { font-size: 16px; font-weight: 600; line-height: 1.3; margin: 2px 0 6px; }

      ul, ol { padding-left: 22px; }
      ul { list-style: disc; }
      ol { list-style: decimal; }

      blockquote {
        margin: 8px 0;
        padding: 4px 14px;
        border-left: 3px solid #d6d9de;
        color: #6b7280;
        background: #f7f8fa;
      }

      pre {
        margin: 8px 0;
        padding: 12px 14px;
        border-radius: 6px;
        background: #1f2329;
        color: #e6e6e6;
        overflow-x: auto;
        code { background: none; color: inherit; font-family: Menlo, Consolas, monospace; font-size: 13px; }
      }
      code {
        padding: 1px 5px;
        border-radius: 4px;
        background: #f2f3f5;
        color: #d6336c;
        font-family: Menlo, Consolas, monospace;
        font-size: 13px;
      }

      mark { background: #fff3bf; color: inherit; }

      a { color: #1456f0; text-decoration: none; &:hover { text-decoration: underline; } }

      img {
        max-width: 100%;
        height: auto;
        border-radius: 6px;
      }

      hr {
        border: none;
        border-top: 1px solid #e5e6eb;
        margin: 16px 0;
      }

      table {
        border-collapse: collapse;
        margin: 8px 0;
        width: 100%;
        th, td {
          border: 1px solid #e5e6eb;
          padding: 6px 10px;
          text-align: left;
        }
        th { background: #f7f8fa; font-weight: 600; }
      }

      ul[data-type='taskList'] {
        list-style: none;
        padding-left: 4px;
        li {
          display: flex;
          align-items: flex-start;
          > label { margin-right: 8px; user-select: none; }
          > div { flex: 1; }
        }
      }

      p.is-editor-empty:first-child::before {
        content: attr(data-placeholder);
        float: left;
        height: 0;
        color: #adb5bd;
        pointer-events: none;
      }
    }
  }
  // 只读态去掉编辑态边框/最小高度/内边距
  .gtip-tap-editor.is-readonly {
    border-color: transparent;
    .ProseMirror { min-height: 0; padding: 0; }
  }

  // 选中文本浮动气泡菜单（tippy 渲染到 body，故用非 scoped 样式）
  .tippy-box { background: transparent; }
  .tippy-content { padding: 0; }
  .bubble-menu-wrap { position: relative; }
  .bubble-menu {
    display: flex;
    align-items: center;
    gap: 2px;
    padding: 4px;
    background: #fff;
    border: 1px solid #e5e6eb;
    border-radius: 8px;
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
  }
  .bubble-more {
    position: absolute;
    bottom: calc(100% + 4px);
    right: 0;
    display: flex;
    align-items: center;
    gap: 2px;
    padding: 4px;
    background: #fff;
    border: 1px solid #e5e6eb;
    border-radius: 8px;
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
  }
  .bubble-btn {
    min-width: 28px;
    height: 28px;
    padding: 0 6px;
    border: none;
    border-radius: 5px;
    background: transparent;
    color: #4e5969;
    font-size: 13px;
    cursor: pointer;
    &:hover { background: #eef1f6; }
    &.is-active { background: #e8f0ff; color: #1456f0; }
  }
  .bubble-btn-italic { font-style: italic; }
  .bubble-btn-strike { text-decoration: line-through; }
  .bubble-sep {
    width: 1px;
    height: 16px;
    margin: 0 4px;
    background: #e5e6eb;
  }
  .bubble-select {
    height: 28px;
    padding: 0 6px;
    border: none;
    border-radius: 5px;
    background: transparent;
    color: #4e5969;
    font-size: 13px;
    cursor: pointer;
    outline: none;
    &:hover { background: #eef1f6; }
    option { color: #333; background: #fff; }
  }
</style>
