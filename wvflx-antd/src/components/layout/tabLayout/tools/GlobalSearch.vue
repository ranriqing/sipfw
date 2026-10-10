<template>
  <a-modal
    :visible="visible"
    :title="null"
    :footer="null"
    :closable="false"
    :maskClosable="true"
    :keyboard="true"
    :width="720"
    :bodyStyle="{ padding: '16px' }"
    wrapClassName="global-search-modal"
    @cancel="onClose">
    <div class="global-search">
      <!-- 长搜索条 -->
      <a-input
        ref="searchInput"
        class="search-bar"
        size="large"
        v-model="keyword"
        placeholder="搜索…"
        allowClear
        @input="onKeywordInput">
        <a-icon slot="prefix" type="search" />
      </a-input>

      <!-- 标签过滤器 -->
      <a-select
        class="tag-filter"
        mode="multiple"
        optionFilterProp="children"
        :showSearch="true"
        :allowClear="true"
        :maxTagCount="5"
        placeholder="按标签过滤"
        :value="selectedTags"
        @change="onTagChange"
        @dropdownVisibleChange="onTagSelectToggle">
        <a-select-option v-for="tag in tagOptions" :key="tag" :value="tag">{{ tag }}</a-select-option>
      </a-select>

      <!-- 结果区 -->
      <div class="result-area">
        <a-spin :spinning="loading">
          <div v-if="state === 'empty'" class="result-hint">输入关键词或选择标签开始搜索</div>
          <div v-else-if="state === 'none'" class="result-hint">无结果</div>
          <div v-else-if="state === 'error'" class="result-hint">搜索出错，请重试</div>
          <template v-else-if="groups.length">
            <div v-for="group in groups" :key="group.domainName" class="result-group">
              <div class="group-header">
                <span class="group-title">{{ group.domainTitle || group.domainName }}</span>
                <span class="group-total">{{ group.total }}</span>
                <a class="group-more" @click.stop="onViewAll(group)">查看全部 ›</a>
              </div>
              <div v-for="item in group.items" :key="item.id" class="result-item" :class="{ highlighted: isHighlighted(group, item) }" @click="onOpenItem(group, item)">
                <span class="item-id">#{{ item.id }}</span>
                <span class="item-values">
                  <template v-if="item.values && item.values.length">
                    <span v-for="(v, i) in item.values" :key="i">
                      <span v-if="i > 0" class="value-sep"> · </span>
                      <span v-html="highlight(v)"></span>
                    </span>
                  </template>
                  <span v-else class="item-tags">
                    <a-tag v-for="t in item.tags" :key="t" color="blue">{{ t }}</a-tag>
                  </span>
                </span>
              </div>
            </div>
          </template>
        </a-spin>
      </div>

      <!-- 快捷键提示 -->
      <div class="search-footer">
        <span class="shortcut-item"><kbd>↑</kbd><kbd>↓</kbd> 切换</span>
        <span class="shortcut-item"><kbd>↵</kbd> 选择</span>
        <span class="shortcut-item"><kbd>Esc</kbd> 关闭</span>
      </div>
    </div>
  </a-modal>
</template>

<script>
  import { postAction } from '@/api/api'
  import { tools } from '@/utils/util'
  import MetaDomain from '@/store/models/MetaDomain'
  import MetaDomainField from '@/store/models/MetaDomainField'
  import MetaPage from '@/store/models/MetaPage'

  export default {
    name: 'GlobalSearch',
    data() {
      return {
        visible: false,
        keyword: '',
        selectedTags: [],
        tagOptions: [],
        domainList: [],
        groups: [],
        loading: false,
        state: 'empty',   // empty | loading | result | none | error
        seq: 0,
        debounceTimer: null,
        highlightIndex: -1,
        tagSelectOpen: false,
      }
    },
    computed: {
      // 扁平化所有结果项，供键盘导航使用
      flatResults() {
        let flat = []
        ;(this.groups || []).forEach(group => {
          (group.items || []).forEach(item => {
            flat.push({ group, item })
          })
        })
        return flat
      },
    },
    methods: {
      open() {
        this.visible = true
        this.$nextTick(() => {
          if (this.$refs.searchInput) {
            this.$refs.searchInput.focus()
          }
        })
        if (!this.domainList.length) {
          this.domainList = this.computeSearchableDomains()
        }
        if (!this.tagOptions.length) {
          this.loadTagOptions()
        }
      },
      onClose() {
        clearTimeout(this.debounceTimer)
        this.seq++
        this.visible = false
        this.keyword = ''
        this.selectedTags = []
        this.groups = []
        this.state = 'empty'
        this.highlightIndex = -1
      },
      onKeydown(event) {
        if (!this.visible) return
        if (event.key === 'Escape') {
          this.onClose()
        } else if (this.tagSelectOpen) {
          return
        } else if (event.key === 'ArrowDown') {
          event.preventDefault()
          this.moveHighlight(1)
        } else if (event.key === 'ArrowUp') {
          event.preventDefault()
          this.moveHighlight(-1)
        } else if (event.key === 'Enter') {
          event.preventDefault()
          this.openHighlighted()
        }
      },
      moveHighlight(delta) {
        const total = this.flatResults.length
        if (total === 0) {
          this.highlightIndex = -1
          return
        }
        if (this.highlightIndex < 0) {
          this.highlightIndex = delta > 0 ? 0 : total - 1
        } else {
          this.highlightIndex = (this.highlightIndex + delta + total) % total
        }
        this.scrollHighlightIntoView()
      },
      openHighlighted() {
        const entry = this.flatResults[this.highlightIndex]
        if (entry) {
          this.onOpenItem(entry.group, entry.item)
        }
      },
      scrollHighlightIntoView() {
        this.$nextTick(() => {
          const el = document.querySelector('.global-search-modal .result-item.highlighted')
          if (el && el.scrollIntoView) {
            el.scrollIntoView({ block: 'nearest' })
          }
        })
      },
      isHighlighted(group, item) {
        const entry = this.flatResults[this.highlightIndex]
        return !!entry && entry.group === group && entry.item === item
      },
      onTagSelectToggle(open) {
        this.tagSelectOpen = open
      },
      onKeywordInput() {
        clearTimeout(this.debounceTimer)
        this.debounceTimer = setTimeout(() => this.doSearch(), 300)
      },
      onTagChange(value) {
        this.selectedTags = value || []
        clearTimeout(this.debounceTimer)
        this.doSearch()
      },
      doSearch() {
        let that = this
        let hasKeyword = !!(that.keyword && that.keyword.trim().length >= 2)
        let hasTags = !!(that.selectedTags && that.selectedTags.length)
        if (!hasKeyword && !hasTags) {
          that.groups = []
          that.state = 'empty'
          that.loading = false
          that.highlightIndex = -1
          return
        }
        that.loading = true
        that.state = 'loading'
        let seq = ++that.seq
        postAction('/globalSearch', {
          keyword: hasKeyword ? that.keyword.trim() : undefined,
          tagNames: hasTags ? that.selectedTags : undefined,
          domainNames: that.domainList.map(d => d.domainName),
        }).then(resp => {
          if (seq !== that.seq) return
          that.loading = false
          that.groups = that._.get(resp, 'gmodel.groups') || []
          that.state = that.groups.length ? 'result' : 'none'
          that.highlightIndex = -1
        }).catch(err => {
          err || true
          if (seq !== that.seq) return
          that.loading = false
          that.state = 'error'
        })
      },
      computeSearchableDomains() {
        let fields = MetaDomainField.query().where('globalSearch', true).get()
        let domainIds = [...new Set(fields.map(f => f.metaDomainId).filter(id => id))]
        let result = []
        domainIds.forEach(domainId => {
          let domain = MetaDomain.find(domainId)
          if (!domain) return
          // 参考 GcsActivityService 的 reference：domainName 首字母小写 → MetaPage.pdent
          let reference = tools.toLowerCamel(domain.domainName)
          let page = MetaPage.query().where('pdent', reference)
            .orderBy('seqOrder', 'asc').orderBy('id', 'desc').first()
          if (!page || !page.ident) return
          result.push({
            domainName: domain.domainName,
            domainTitle: domain.title,
            pageIdent: page.ident,
          })
        })
        return result
      },
      loadTagOptions() {
        let that = this
        postAction('/gcsTag/names', { condition: 'and', rules: [] }).then(resp => {
          that.tagOptions = that._.get(resp, 'gmodel') || []
        }).catch(err => {
          err || true
          that.tagOptions = []
        })
      },
      onOpenItem(group, item) {
        let domain = this.domainList.find(d => d.domainName === group.domainName)
        if (!domain) return
        this.$router.push({ name: domain.pageIdent, query: { gsearchId: item.id } })
        this.onClose()
      },
      onViewAll(group) {
        let domain = this.domainList.find(d => d.domainName === group.domainName)
        if (!domain) return
        let query = {}
        if (this.keyword && this.keyword.trim().length >= 2) {
          query.gsearch = this.keyword.trim()
        }
        if (this.selectedTags && this.selectedTags.length) {
          query.gsearchTag = this.selectedTags.join(',')
        }
        this.$router.push({ name: domain.pageIdent, query })
        this.onClose()
      },
      escapeHtml(str) {
        return String(str).replace(/[&<>"']/g, c => ({
          '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;',
        }[c]))
      },
      highlight(value) {
        let safeValue = this.escapeHtml(value)
        if (!this.keyword) return safeValue
        let safeKeyword = this.escapeHtml(this.keyword.trim())
        if (!safeKeyword) return safeValue
        let re = new RegExp(safeKeyword.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'), 'ig')
        return safeValue.replace(re, m => `<mark>${m}</mark>`)
      },
    },
    mounted() {
      window.addEventListener('keydown', this.onKeydown)
    },
    beforeDestroy() {
      window.removeEventListener('keydown', this.onKeydown)
    },
  }
</script>

<style lang="scss" scoped>
  .global-search {
    .search-bar {
      width: 100%;
      margin-bottom: 12px;
    }
    .tag-filter {
      width: 100%;
      margin-bottom: 12px;
    }
    .result-area {
      max-height: 420px;
      overflow-y: auto;
    }
    .result-hint {
      color: #999;
      text-align: center;
      padding: 32px 0;
    }
    .result-group {
      margin-bottom: 12px;
      .group-header {
        display: flex;
        align-items: center;
        padding: 4px 0;
        border-bottom: 1px solid #f0f0f0;
        .group-title {
          font-weight: 600;
          color: #333;
        }
        .group-total {
          margin-left: 6px;
          color: #999;
          font-size: 12px;
        }
        .group-more {
          margin-left: auto;
          font-size: 12px;
        }
      }
      .result-item {
        display: flex;
        align-items: center;
        padding: 8px 4px;
        cursor: pointer;
        border-bottom: 1px solid #fafafa;
        &:hover {
          background: #f5f5f5;
        }
        &.highlighted {
          background: #e6f7ff;
        }
        .item-id {
          color: #bbb;
          font-size: 12px;
          margin-right: 10px;
          white-space: nowrap;
        }
        .item-values {
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
          /deep/ mark {
            background: #ffe58f;
            padding: 0;
          }
          .value-sep {
            color: #ddd;
          }
          .item-tags {
            display: inline-flex;
            gap: 4px;
          }
        }
      }
    }
    .search-footer {
      display: flex;
      align-items: center;
      gap: 16px;
      padding-top: 12px;
      margin-top: 12px;
      border-top: 1px solid #f0f0f0;
      color: #999;
      font-size: 12px;
      .shortcut-item {
        display: inline-flex;
        align-items: center;
        gap: 4px;
        kbd {
          display: inline-block;
          min-width: 20px;
          padding: 1px 6px;
          border: 1px solid #d9d9d9;
          border-bottom-width: 2px;
          border-radius: 4px;
          background: #fafafa;
          color: #595959;
          font-size: 12px;
          font-family: inherit;
          line-height: 1.5;
          text-align: center;
        }
      }
    }
  }
</style>
