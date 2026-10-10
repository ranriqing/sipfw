<template>
  <div class="caller-card" :class="{ 'has-qa-alert': item.qaFailCount > 0 }">
    <div class="caller-header">
      <div class="caller-name-wrap">
        <a-icon type="setting" class="caller-setting-icon" @click="onSettingClick" />
        <div class="caller-name">{{ item.displayName || '未知主叫' }}</div>
      </div>
      <div class="caller-right">
        <div class="caller-total">
          {{ reportWindowMinutes }}Min通话
          <span class="num">{{ item.totalCalls | SipPrettyInteger }}</span>
        </div>
        <a-popover
          v-show="item.qaFailCount > 0"
          trigger="click"
          :visible="qaFailPopVisible"
          placement="bottomRight"
          @visibleChange="onQaFailPopVisible">
          <template slot="content">
            <div class="qa-fail-pop">
              <div class="qa-fail-pop-title">
                <span>质检不通过 ({{ item.qaFailCount }})</span>
                <a class="qa-fail-clear" @click="clearAllQaFails">清空</a>
              </div>
              <div class="qa-fail-list">
                <div
                  v-for="(rec, idx) in qaFailRecords"
                  :key="idx"
                  class="qa-fail-row">
                  <div class="qa-fail-row-hd">
                    <span class="qa-fail-index">#{{ idx + 1 }}</span>
                    <div class="qa-fail-numbers">
                      <div>入: {{ rec.inboundCaller }}→{{ rec.inboundCallee }}</div>
                      <div>出: {{ rec.outboundCaller }}→{{ rec.outboundCallee }}</div>
                    </div>
                    <a-icon type="delete" class="qa-fail-del" @click="deleteQaFail(idx)" />
                  </div>
                  <div class="qa-fail-url">
                    <a :href="rec.recordUrl" target="_blank">{{ rec.recordUrl }}</a>
                  </div>
                  <div class="qa-fail-text" :title="rec.transcript">{{ rec.transcript | Ellipsis(80) }}</div>
                  <div class="qa-fail-summary">总结：{{ rec.summary }}</div>
                </div>
              </div>
            </div>
          </template>
          <span class="qa-fail-badge" :title="'质检不通过 ' + item.qaFailCount + ' 条'">
            {{ qaFailBadge }}
          </span>
        </a-popover>
      </div>
    </div>

    <div class="section-card handling">
      <div class="handling-wrapper">
        <div class="handling-total" :title="`处理中 ${handlingTotal | SipPrettyInteger}`">
          <div class="total-label">处理中</div>
          <div class="total-num">{{ handlingTotal | SipPrettyInteger }}</div>
        </div>
        <div class="handling-bar-panel">
          <div class="bar-percent-row">
            <div class="percent-item answered" :title="`通话中 ${handlingAnswered | SipPrettyInteger}`">
              通话中 {{ handlingBars[0].percent }}
            </div>
            <div class="percent-item connected" :title="`已触达 ${handlingConnected | SipPrettyInteger}`">
              已触达 {{ handlingBars[1].percent }}
            </div>
            <div class="percent-item trying" :title="`尝试中 ${handlingTrying | SipPrettyInteger}`">
              尝试中 {{ handlingBars[2].percent }}
            </div>
          </div>
          <div class="bar-wrapper">
            <div
              class="bar-segment answered"
              :style="{ width: handlingBars[0].width + '%' }"
              :title="`通话中 ${handlingAnswered | SipPrettyInteger} / ${handlingBars[0].percent}`">
            </div>
            <div
              class="bar-segment connected"
              :style="{ width: handlingBars[1].width + '%' }"
              :title="`已触达 ${handlingConnected | SipPrettyInteger} / ${handlingBars[1].percent}`">
            </div>
            <div
              class="bar-segment trying"
              :style="{ width: handlingBars[2].width + '%' }"
              :title="`尝试中 ${handlingTrying | SipPrettyInteger} / ${handlingBars[2].percent}`">
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="section-card processed">
      <div class="section-title">
        <span class="processed-title-wrap">
          <template v-if="specialHitRows.length>0">
            <a-switch
              size="small"
              :checked="ratioTypeEnabledSwitch"
              checkedChildren="去"
              unCheckedChildren="全"
              @change="onRatioTypeEnabledChange" />
            <span class="special-hit-summary">{{ specialHitSummaryPercent }}</span>
            <span class="special-hit-wrap" :class="{ disabled: !ratioTypeEnabled }">
              <span
                v-for="(specialItem, idx) in specialHitRows"
                :key="'title_special_'+idx"
                class="meta-item blacklist"
                :title="`${specialItem.label} ${specialItem.value | SipPrettyInteger} / ${specialItem.percent}`">
                {{ specialItem.label }} {{ specialItem.percent }}
              </span>
            </span>
          </template>
          <span v-else>暂未定义SIP码映射</span>
        </span>
      </div>
      <div class="processed-top-row">
        <div class="answer-rate-box">
          <div class="answer-rate-label">应答率</div>
          <div class="answer-rate-num">{{ processedAnsweredRate }}</div>
        </div>
        <div class="processed-meta">
          <div class="answer-num-wrap">
            <span class="answer-num">{{ processedAnswered | SipPrettyInteger }}</span>
            <span class="answer-total" :title="`分母 ${ratioBaseTotal | SipPrettyInteger}`">
              / {{ ratioBaseTotal | SipPrettyInteger }}
            </span>
          </div>
          <div>
            <span
              v-if="processedReinvite>0"
              class="meta-item">
              重发 {{ processedReinvite | SipPrettyInteger }}
            </span>
            <span
              class="meta-item connect-rate"
              :title="`触达率（${ratioTypeEnabled ? '去特殊' : '全量'}）`">
              触达率 {{ processedConnectRate }}
            </span>
          </div>
        </div>
      </div>

      <div class="processed-bars">
        <div v-for="(bar, idx) in processedBars" :key="'pb_'+idx" class="processed-bar-row">
          <div class="bar-label">{{ bar.label }}</div>
          <div class="bar-track" :title="`${bar.label} ${bar.value | SipPrettyInteger} / ${ratioTypeEnabled ? '去特殊' : '全量'} ${bar.percentNoSpecial}`">
            <div class="bar-fill" :class="bar.className" :style="{ width: bar.width + '%' }"></div>
          </div>
          <div class="bar-value">
            <div :title="`${bar.value} / ${ratioTypeEnabled ? '去特殊' : '全量'} ${bar.percentNoSpecial}`">
              {{ bar.value | SipPrettyInteger }} / {{ bar.percentNoSpecial }}
            </div>
          </div>
        </div>
      </div>

      <div class="prefix-grid">
        <div class="prefix-column">
          <div class="prefix-title">[C] 触达 top5</div>
          <div class="prefix-list">
            <span v-if="connsipTop.length===0" class="prefix-empty">暂无</span>
            <div
              class="prefix-row"
              v-for="(reason, rIdx) in connsipTop"
              :key="'c'+rIdx"
              :title="`${ reason.alias } ${ reason.value } / ${ reason.percent }`">
              <div class="prefix-progress answered" :style="{ width: reason.prefixPercent }"></div>
              <span class="prefix-name">{{ reason.alias }}</span>
              <span class="prefix-right">{{ reason.value | SipPrettyInteger }} / {{ reason.percent }}</span>
            </div>
          </div>
        </div>
        <div class="prefix-column">
          <div class="prefix-title">[O] 未触达 top5</div>
          <div class="prefix-list">
            <span v-if="othersipTop.length===0" class="prefix-empty">暂无</span>
            <div
              class="prefix-row"
              v-for="(reason, idx) in othersipTop"
              :key="'o'+idx"
              :title="`${ reason.alias } ${ reason.value } / ${ reason.percent }`">
              <div class="prefix-progress others" :style="{ width: reason.prefixPercent }"></div>
              <span class="prefix-name">{{ reason.alias }}</span>
              <span class="prefix-right">{{ reason.value | SipPrettyInteger }} / {{ reason.percent }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
    <GFormPop
      ref="definitionFormPop"
      :popVisible="false"
      :configObj="definitionPopConfig">
      <template slot="footerLeft" slot-scope="{ record }">
        <div v-if="record && record.id" class="definition-delete-wrap">
          <a-button
            type="danger"
            ghost
            @click="onDeleteDefinition(record)">
            删除
          </a-button>
        </div>
      </template>
    </GFormPop>
  </div>
</template>

<script>
  import { COMPO } from '@/constants'
  import GFormPop from '@/components/page_pop/GFormPop'
  import { postAction, putAction, deleteAction, getAction } from '@/api/api'
  export default {
    name: 'CallerStatsItem',
    components: {
      GFormPop,
    },
    props: {
      item: {
        type: Object,
        required: true
      },
      defaultCallDirection: {
        type: String,
        default: 'INBOUND'
      },
      rptMillis: {
        type: Number,
        default: 5 * 60 * 1000
      }
    },
    data() {
      return {
        ratioTypeEnabledSwitch: false,
        qaFailPopVisible: false,
        qaFailRecords: [],
        definitionPopConfig: {
          modal: true,
          title: '新增分组定义',
          submitText: '保存',
          formFields: [
            {
              title: '号码前缀',
              field: 'prefix',
              component: COMPO.G_INPUT,
              placeholder: '请输入号码前缀',
              options: {
                rules: [{ required: true, message: '请输入号码前缀' }]
              },
            },
            {
              title: '展示名称',
              field: 'displayName',
              component: COMPO.G_INPUT,
              placeholder: '请输入展示名称',
              options: {
                rules: [{ required: true, message: '请输入展示名称' }]
              },
            },
            {
              title: '呼叫方向',
              field: 'callDirection',
              component: COMPO.G_SELECT,
              options: {
                rules: [{ required: true, message: '请选择呼叫方向' }]
              },
              extend: {
                options: () => ({
                  INBOUND: 'INBOUND',
                  OUTBOUND: 'OUTBOUND',
                })
              }
            },
            {
              title: '比例类型',
              field: 'ratioTypeEnabled',
              component: COMPO.G_SWITCH,
            },
            {
              title: 'SIP码映射',
              field: 'specialSipCodeJson',
              component: 'g-sip-code-map-editor',
              extend: {
                codeHolder: 'SIP_CODE',
                labelHolder: '显示名',
                addCaption: '添加映射',
              }
            },
            {
              title: '开启质检',
              field: 'qaEnabled',
              component: COMPO.G_SWITCH,
            },
            {
              title: '质检话术',
              field: 'qaScriptJson',
              component: 'g-sip-code-textarea-editor',
              extend: {
                codeHolder: '话术业务',
                labelHolder: '话术内容',
                addCaption: '添加话术',
              }
            },
          ]
        }
      }
    },
    computed: {
      qaFailBadge() {
        let n = parseInt(this.item.qaFailCount) || 0
        return n > 9 ? '9+' : String(n)
      },
      reportWindowMinutes() {
        let millis = parseInt(this.rptMillis) || 0
        if (millis <= 0) return 5
        let minutes = millis / (60 * 1000)
        if (Number.isInteger(minutes)) return minutes
        return parseFloat(minutes.toFixed(1))
      },
      handlingTotal() { return parseInt(this._.get(this.item, 'handling', 0)) || 0 },
      handlingConnected() { return parseInt(this._.get(this.item, 'connected', 0)) || 0 },
      handlingTrying() { return parseInt(this._.get(this.item, 'trying', 0)) || 0 },
      handlingAnswered() {
        // 按 formatter 的处理语义，handling 下 answered/connected/trying 互斥，优先用差值兜底。
        let answered = this.handlingTotal - this.handlingConnected - this.handlingTrying
        return answered > 0 ? answered : 0
      },
      handlingBars() {
        let total = this.handlingTotal
        let calc = (num) => {
          if (!total || total <= 0) return { width: 0, percent: '0.00%' }
          return {
            width: num / total * 100,
            percent: this._percent(num, total)
          }
        }
        return [calc(this.handlingAnswered), calc(this.handlingConnected), calc(this.handlingTrying)]
      },
      processedTotal() { return parseInt(this._.get(this.item, 'processed', 0)) || 0 },
      sourceRatioTypeEnabled() { return !!this._.get(this.item, 'ratioTypeEnabled', false) },
      ratioTypeEnabled() { return this.ratioTypeEnabledSwitch },
      dimensions() { return this._.get(this.item, 'hangupTypeCount', {}) || {} },
      specialSipCodeMap() {
        let source = this._.get(this.item, 'specialSipCodeJson', {})
        if (!source) return {}
        if (typeof source === 'string') {
          try {
            source = JSON.parse(source)
          } catch (e) {
            return {}
          }
        }
        if (typeof source !== 'object') return {}
        return Object.keys(source).reduce((ret, code) => {
          let codeKey = String(code || '').trim()
          let label = String(source[code] || '').trim()
          if (codeKey && label) {
            ret[codeKey] = label
          }
          return ret
        }, {})
      },
      specialHitCountMap() {
        let rows = {}
        Object.keys(this.dimensions).forEach(key => {
          let code = this._extractSipCode(key)
          if (!code || !this.specialSipCodeMap[code]) return
          let label = this.specialSipCodeMap[code]
          rows[label] = (rows[label] || 0) + this._read(key)
        })
        return rows
      },
      specialHitTotal() {
        return Object.keys(this.specialHitCountMap).reduce((sum, key) => sum + this.specialHitCountMap[key], 0)
      },
      ratioBaseTotal() {
        if (!this.ratioTypeEnabled) {
          return this.processedTotal
        }
        let value = this.processedTotal - this.specialHitTotal
        return value > 0 ? value : 0
      },
      specialHitRows() {
        return Object.keys(this.specialHitCountMap).map(label => ({
          label: label,
          value: this.specialHitCountMap[label],
          percent: this._percentOne(this.specialHitCountMap[label], this.processedTotal),
        })).filter(item => item.value > 0)
      },
      specialHitSummaryPercent() {
        return this._percentOne(this.specialHitTotal, this.processedTotal)
      },
      processedAnswered() { return this._read('answered') + this._read('conv20m') },
      processedReinvite() { return this._read('reinvite') },
      processedConnected() { return this._sumPrefix('connsip') },
      processedOthers() { return this._sumPrefix('othersip') },
      processedCoreTotal() { return this.processedAnswered + this.processedConnected + this.processedOthers },
      processedReachable() { return this.processedAnswered + this.processedConnected },
      processedAnsweredRate() {
        return this._percent(this.processedAnswered, this.ratioBaseTotal)
      },
      processedConnectRate() {
        return this._percent(this.processedReachable, this.ratioBaseTotal)
      },
      processedBars() {
        let total = this.processedCoreTotal
        let calc = (num) => {
          if (!total || total <= 0) return { width: 0, percent: '0.00%' }
          return {
            width: num / total * 100,
            percent: this._percent(num, total)
          }
        }
        let answeredBar = calc(this.processedAnswered)
        let connectedBar = calc(this.processedConnected)
        let othersBar = calc(this.processedOthers)
        return [
          {
            label: '应答',
            value: this.processedAnswered,
            width: answeredBar.width,
            percentNoSpecial: this._percent(this.processedAnswered, this.ratioBaseTotal),
            className: 'answered'
          },
          {
            label: '触达挂断',
            value: this.processedConnected,
            width: connectedBar.width,
            percentNoSpecial: this._percent(this.processedConnected, this.ratioBaseTotal),
            className: 'connected'
          },
          {
            label: '未触达挂断',
            value: this.processedOthers,
            width: othersBar.width,
            percentNoSpecial: this._percent(this.processedOthers, this.ratioBaseTotal),
            className: 'others'
          },
        ]
      },
      connsipTop() {
        return this._prefixTop('connsip', '[C]', 5)
      },
      othersipTop() {
        return this._prefixTop('othersip', '[O]', 5)
      },
    },
    watch: {
      sourceRatioTypeEnabled: {
        immediate: true,
        handler(value) {
          this.ratioTypeEnabledSwitch = !!value
        }
      }
    },
    methods: {
      onQaFailPopVisible(visible) {
        if (visible) {
          this.loadQaFails()
        }
        this.qaFailPopVisible = visible
      },
      loadQaFails() {
        let caller = this._.get(this.item, 'prefix', '')
        let direction = this._.get(this.item, 'callDirection', 'INBOUND')
        let ip = ''
        getAction('/sipPrefixDefinition/qaFails', { caller, direction, ip }).then(resp => {
          this.qaFailRecords = this._.get(resp, 'gmodel.records', []) || []
        }).catch(() => {
          this.qaFailRecords = []
        })
      },
      deleteQaFail(index) {
        let caller = this._.get(this.item, 'prefix', '')
        let direction = this._.get(this.item, 'callDirection', 'INBOUND')
        let ip = ''
        deleteAction('/sipPrefixDefinition/qaFails', null, { caller, direction, ip, index }).then(() => {
          this.qaFailRecords.splice(index, 1)
          this.$set(this.item, 'qaFailCount', Math.max(0, (this.item.qaFailCount || 0) - 1))
          this.$message.success('已删除')
        }).catch(() => {
          this.$message.error('删除失败')
        })
      },
      clearAllQaFails() {
        let caller = this._.get(this.item, 'prefix', '')
        let direction = this._.get(this.item, 'callDirection', 'INBOUND')
        let ip = ''
        deleteAction('/sipPrefixDefinition/qaFails/all', null, { caller, direction, ip }).then(() => {
          this.qaFailRecords = []
          this.$set(this.item, 'qaFailCount', 0)
          this.$message.success('已清空')
        }).catch(() => {
          this.$message.error('清空失败')
        })
      },
      onSettingClick() {
        this.openDefinitionModal()
      },
      onRatioTypeEnabledChange(checked) {
        let nextValue = !!checked
        this.ratioTypeEnabledSwitch = nextValue
        let record = this._buildDefinitionRecord({ ratioTypeEnabled: nextValue })
        this.doSaveDefinition(record).catch(() => {
          this.ratioTypeEnabledSwitch = this.sourceRatioTypeEnabled
        })
      },
      openDefinitionModal(extraForm = {}) {
        let record = this._buildDefinitionRecord(extraForm)
        let isUpdate = !!record.id
        this.$set(this.definitionPopConfig, 'title', isUpdate ? '更新分组定义' : '新增分组定义')
        this.$set(this.definitionPopConfig, 'submitText', isUpdate ? '更新' : '保存')
        this.$refs.definitionFormPop.showPop(
          record,
          {},
          (popRecord) => this.doSaveDefinition(popRecord),
          () => {}
        )
      },
      onDeleteDefinition(popRecord) {
        let id = this._.get(popRecord, 'id', null)
        if (!id) return
        this.$confirm({
          title: '确认删除该分组定义？',
          content: '删除后将按默认分组规则统计。',
          onOk: () => {
            return deleteAction('/sipPrefixDefinition/delete', id, {}).then(() => {
              this.$set(this.item, 'definitionId', undefined)
              this.$set(this.item, 'callDirection', this.defaultCallDirection)
              this.$set(this.item, 'ratioTypeEnabled', false)
              this.$set(this.item, 'specialSipCodeJson', {})
              this.$set(this.item, 'qaEnabled', false)
              this.$set(this.item, 'qaScriptJson', {})
              this.ratioTypeEnabledSwitch = false
              this.$refs.definitionFormPop.doClose()
              this.$message.success('分组定义已删除')
            }).catch(err => {
              this.$message.error('分组定义删除失败')
              return Promise.reject(err)
            })
          }
        })
      },
      doSaveDefinition(popRecord) {
        let prefix = String(popRecord.prefix || '').trim()
        let displayName = String(popRecord.displayName || '').trim()
        let specialSipCodeMap = this._normalizeSpecialSipCodeMap(popRecord.specialSipCodeJson)
        let jsonText = this._safeSpecialJsonText(specialSipCodeMap)
        if (!prefix || !displayName) {
          this.$message.warning('前缀和展示名不能为空')
          return Promise.reject(new Error('invalid form'))
        }
        if (!jsonText) {
          this.$message.warning('specialSipCodeJson 不是合法 JSON')
          return Promise.reject(new Error('invalid json'))
        }
        let qaScriptMap = this._normalizeSpecialSipCodeMap(popRecord.qaScriptJson)
        let qaScriptJson = this._safeSpecialJsonText(qaScriptMap)
        if (!qaScriptJson) {
          this.$message.warning('qaScriptJson 不是合法 JSON')
          return Promise.reject(new Error('invalid json'))
        }
        let payload = {
          prefix: prefix,
          displayName: displayName,
          callDirection: popRecord.callDirection,
          ratioTypeEnabled: !!popRecord.ratioTypeEnabled,
          specialSipCodeJson: jsonText,
          qaEnabled: !!popRecord.qaEnabled,
          qaScriptJson: qaScriptJson,
        }
        let promise = popRecord.id
          ? putAction('/sipPrefixDefinition/update', popRecord.id, payload)
          : postAction('/sipPrefixDefinition/save', payload)
        return promise.then(resp => {
          let saved = this._.get(resp, 'gmodel', {}) || {}
          this.$set(this.item, 'definitionId', this._.get(saved, 'id', popRecord.id))
          this.$set(this.item, 'prefix', payload.prefix)
          this.$set(this.item, 'displayName', payload.displayName)
          this.$set(this.item, 'callDirection', payload.callDirection)
          this.$set(this.item, 'ratioTypeEnabled', payload.ratioTypeEnabled)
          this.$set(this.item, 'specialSipCodeJson', specialSipCodeMap)
          this.$set(this.item, 'qaEnabled', payload.qaEnabled)
          this.$set(this.item, 'qaScriptJson', qaScriptMap)
          this.ratioTypeEnabledSwitch = payload.ratioTypeEnabled
          this.$message.success('定义更新成功')
        }).catch(err => {
          this.$message.error('定义更新失败')
          return Promise.reject(err)
        })
      },
      _safeSpecialJsonText(source) {
        try {
          if (source === null || source === undefined || source === '') return '{}'
          if (typeof source === 'string') {
            let parsed = JSON.parse(source)
            return JSON.stringify(parsed)
          }
          if (typeof source === 'object') {
            return JSON.stringify(source)
          }
          return ''
        } catch (e) {
          return ''
        }
      },
      _normalizeSpecialSipCodeMap(source) {
        let normalized = {}
        if (!source) return normalized
        if (typeof source === 'string') {
          try {
            source = JSON.parse(source)
          } catch (e) {
            return normalized
          }
        }
        if (typeof source !== 'object') return normalized
        Object.keys(source).forEach(code => {
          let codeKey = String(code || '').trim()
          let label = String(source[code] || '').trim()
          if (codeKey && label) {
            normalized[codeKey] = label
          }
        })
        return normalized
      },
      _buildDefinitionRecord(extraForm = {}) {
        let specialSipCodeJson = this._normalizeSpecialSipCodeMap(this._.get(this.item, 'specialSipCodeJson', {}))
        let qaScriptJson = this._normalizeSpecialSipCodeMap(this._.get(this.item, 'qaScriptJson', {}))
        let id = this._.get(this.item, 'definitionId', this._.get(this.item, 'id', undefined))
        let hasDefinition = !!id
        return {
          id: id,
          prefix: this._.get(this.item, 'prefix'),
          displayName: this._.get(this.item, 'displayName'),
          callDirection: hasDefinition ? (this._.get(this.item, 'callDirection') || this.defaultCallDirection) : this.defaultCallDirection,
          ratioTypeEnabled: this.sourceRatioTypeEnabled,
          qaEnabled: !!this._.get(this.item, 'qaEnabled', false),
          specialSipCodeJson: specialSipCodeJson,
          qaScriptJson: qaScriptJson,
          ...extraForm,
        }
      },
      _read(key) {
        return parseInt(this.dimensions[key] || 0) || 0
      },
      _sumPrefix(prefix) {
        return Object.keys(this.dimensions)
          .filter(key => {
            if (!key.startsWith(prefix)) return false
            if (this._read(key) <= 0) return false
            let sipCode = this._extractSipCode(key)
            return !this.specialSipCodeMap[sipCode]
          }).reduce((sum, key) => {
            if (key.startsWith(prefix)) return sum + this._read(key)
            return sum
          }, 0)
      },
      _prefixTop(prefix, alias, maxNum) {
        let rows = Object.keys(this.dimensions)
          .filter(key => {
            if (!key.startsWith(prefix)) return false
            if (this._read(key) <= 0) return false
            let sipCode = this._extractSipCode(key)
            return !this.specialSipCodeMap[sipCode]
          })
          .map(key => ({
            key: key,
            value: this._read(key),
          }))
        let prefixTotal = rows.reduce((sum, row) => sum + row.value, 0)
        if (prefixTotal <= 0) return []
        return rows
          .map(row => {
            let value = row.value
            return {
              alias: alias + row.key.substring(prefix.length),
              value: value,
              percent: this._percent(value, this.ratioBaseTotal),
              prefixPercent: this._percent(value, prefixTotal),
            }
          })
          .sort((s1, s2) => s2.value - s1.value)
          .slice(0, maxNum)
      },
      _extractSipCode(key) {
        if (!key) return ''
        if (key.startsWith('connsip')) return key.replace('connsip', '')
        if (key.startsWith('othersip')) return key.replace('othersip', '')
        return ''
      },
      _percent(num, den) {
        if (!den || den <= 0) return '0.00%'
        return (num / den * 100).toFixed(2) + '%'
      },
      _percentOne(num, den) {
        if (!den || den <= 0) return '0.0%'
        return (num / den * 100).toFixed(1) + '%'
      },
    }
  }
</script>

<style lang="scss" scoped>
  .caller-card{
    width: 370px;
    border-radius: 10px;
    background: linear-gradient(180deg, #ffffff 0%, #f9fcff 100%);
    border: 1px solid #dbe7f3;
    box-shadow: 0 3px 12px rgba(0,0,0,0.06);
    padding: 12px;
    display: flex;
    flex-direction: column;
    row-gap: 10px;
    transition: border-color 0.3s, box-shadow 0.3s;
    &.has-qa-alert{
      border-color: #ff4d4f;
      box-shadow: 0 3px 12px rgba(255, 77, 79, 0.15);
    }
  }
  .caller-header{
    display: flex;
    flex-direction: row;
    justify-content: space-between;
    align-items: baseline;
    .caller-name-wrap{
      display: flex;
      flex-direction: row;
      align-items: center;
      min-width: 0;
      gap: 6px;
      .caller-setting-icon{
        font-size: 13px;
        color: #7f8b98;
        flex: 0 0 auto;
        cursor: pointer;
      }
    }
    .caller-name{
      font-weight: bold;
      font-size: 16px;
      max-width: 200px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
    .caller-right{
      display: flex;
      flex-direction: row;
      align-items: center;
      flex-wrap: nowrap;
      flex: 0 0 auto;
    }
    .caller-total{
      color: #888;
      white-space: nowrap;
      .num{
        color: #222;
        font-weight: bold;
        margin-left: 4px;
      }
    }
  }
  .section-card{
    border-radius: 8px;
    border: 1px solid #e8eef5;
    background: #fff;
    padding: 8px 10px;
    .section-title{
      display: flex;
      flex-direction: row;
      justify-content: space-between;
      align-items: center;
      font-size: 12px;
      color: #7f8b98;
      margin-bottom: 6px;
      .processed-title-wrap{
        display: inline-flex;
        flex-direction: row;
        align-items: center;
        gap: 6px;
        width: 100%;
        .special-hit-wrap{
          margin-left: auto;
          display: inline-flex;
          flex-direction: row;
          align-items: center;
          justify-content: flex-end;
          gap: 6px;
          flex-wrap: wrap;
          &.disabled{
            filter: grayscale(100%);
            opacity: 0.75;
          }
        }
        .special-hit-summary{
          color: #6b7c90;
          font-weight: 600;
          white-space: nowrap;
        }
      }
    }
    .line-item{
      display: flex;
      justify-content: space-between;
      border-bottom: 1px dashed #edf2f7;
      padding: 3px 0;
      font-size: 12px;
      &:last-child{ border-bottom: none; }
    }
    .prefix-title{
      font-size: 12px;
      color: #667788;
      margin-top: 8px;
      margin-bottom: 4px;
    }
    .prefix-list{
      display: flex;
      flex-direction: column;
      gap: 6px;
      width: 100%;
    }
    .prefix-empty{
      color: #aaa;
      font-size: 12px;
    }
    .prefix-tag{
      padding: 2px 8px;
      font-size: 12px;
      border-radius: 12px;
      background: #f3f8ff;
      border: 1px solid #d9e7fb;
      color: #4a5f78;
    }
  }
  .section-card.handling{
    border-left: 3px solid #3ea0ff;
    .handling-wrapper{
      display: flex;
      flex-direction: row;
      align-items: center;
      gap: 10px;
    }
    .handling-total{
      flex: 0 0 74px;
      min-width: 74px;
      text-align: center;
      border-right: 1px dashed #e5ecf5;
      padding-right: 8px;
      .total-label{
        font-size: 11px;
        color: #7f8b98;
      }
      .total-num{
        // margin-top: 2px;
        font-size: 24px;
        line-height: 1.1;
        font-weight: bold;
        color: #2a3f55;
      }
    }
    .handling-bar-panel{
      flex: 1 1 auto;
      min-width: 0;
      .bar-percent-row{
        display: flex;
        flex-direction: row;
        justify-content: space-between;
        gap: 8px;
        margin-bottom: 6px;
      }
      .percent-item{
        font-size: 12px;
        white-space: nowrap;
        &.answered{ color: #2f9e44; }
        &.connected{ color: #1c7ed6; }
        &.trying{ color: #e67700; }
      }
      .bar-wrapper{
        width: 100%;
        height: 16px;
        border-radius: 10px;
        overflow: hidden;
        background: #f2f5f9;
        display: flex;
        flex-direction: row;
      }
      .bar-segment{
        height: 100%;
        transition: width .2s ease-in-out;
        &.answered{ background: rgba(47, 158, 68, 0.75); }
        &.connected{ background: rgba(28, 126, 214, 0.75); }
        &.trying{ background: rgba(230, 119, 0, 0.75); }
      }
    }
  }
  .section-card.processed{
    border-left: 3px solid #6ec26e;
    .blacklist{
      color: #ad2e24;
      background: rgba(245, 108, 108, 0.12);
      border: 1px solid rgba(245, 108, 108, 0.25);
      border-radius: 10px;
      padding: 2px 8px;
    }
    .processed-top-row{
      display: flex;
      flex-direction: row;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 8px;
      gap: 8px;
    }
    .answer-rate-box{
      flex: 0 0 108px;
      min-width: 108px;
      border-radius: 8px;
      border: 1px solid rgba(47, 158, 68, 0.3);
      background: linear-gradient(180deg, rgba(47, 158, 68, 0.08) 0%, rgba(47, 158, 68, 0.03) 100%);
      padding: 6px 8px;
      text-align: center;
      .answer-rate-label{
        font-size: 11px;
        color: #5f7085;
      }
      .answer-rate-num{
        margin-top: 2px;
        font-size: 24px;
        line-height: 1.1;
        font-weight: 800;
        color: #2f9e44;
      }
    }
    .processed-meta{
      flex: 1 1 auto;
      min-width: 0;
      display: flex;
      flex-direction: column;
      gap: 8px;
      align-items: flex-end;
      flex-wrap: wrap;
      justify-content: flex-start;
      .answer-num{
        font-size: 18px;
        line-height: 1.1;
        font-weight: 800;
        color: #2f9e44;
      }
      .answer-num-wrap{
        display: inline-flex;
        flex-direction: row;
        align-items: baseline;
        gap: 4px;
      }
      .answer-total{
        font-size: 18px;
        color: #5f7085;
        font-weight: 600;
      }
      .meta-item{
        font-size: 12px;
        color: #748196;
        white-space: nowrap;
      }
      .connect-rate{
        font-weight: 700;
        color: #1c7ed6;
      }
    }
    .processed-bars{
      display: flex;
      flex-direction: column;
      gap: 8px;
      margin-bottom: 6px;
    }
    .processed-bar-row{
      display: grid;
      grid-template-columns: 66px 1fr auto;
      align-items: center;
      column-gap: 8px;
      .bar-label{
        font-size: 12px;
        color: #54657a;
      }
      .bar-track{
        position: relative;
        width: 100%;
        height: 14px;
        border-radius: 10px;
        overflow: hidden;
        background: #f1f5f9;
      }
      .bar-fill{
        height: 100%;
        transition: width .2s ease-in-out;
        &.answered{ background: rgba(47, 158, 68, 0.78); }
        &.connected{ background: rgba(28, 126, 214, 0.78); }
        &.others{ background: rgba(230, 119, 0, 0.78); }
      }
      .bar-value{
        font-size: 12px;
        color: #5f7085;
        text-align: right;
        .sub-rate{
          font-size: 11px;
          color: #8a98aa;
          white-space: nowrap;
        }
      }
    }
    .prefix-grid{
      display: grid;
      grid-template-columns: 1fr 1fr;
      column-gap: 10px;
      margin-top: 6px;
    }
    .prefix-column{
      min-width: 0;
      display: flex;
      flex-direction: column;
      &:first-child{
        padding-right: 0;
      }
      &:last-child{
        padding-left: 9px;
        border-left: 1px solid rgba(140, 154, 176, 0.32);
      }
    }
    .prefix-row{
      width: 100%;
      position: relative;
      display: flex;
      flex-direction: row;
      align-items: center;
      justify-content: space-between;
      gap: 8px;
      padding: 3px 8px;
      border: 1px solid #d9e7fb;
      border-radius: 6px;
      background: #f7fbff;
      font-size: 12px;
      color: #4a5f78;
      overflow: hidden;
    }
    .prefix-progress{
      position: absolute;
      left: 0;
      top: 0;
      bottom: 0;
      min-width: 1px;
      pointer-events: none;
      &.answered{
        background: rgba(28, 126, 214, 0.12);
      }
      &.others{
        background: rgba(230, 119, 0, 0.10);
      }
    }
    .prefix-name{
      position: relative;
      z-index: 1;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
    .prefix-right{
      position: relative;
      z-index: 1;
      flex: 0 0 auto;
      white-space: nowrap;
      color: #5f7085;
    }
  }
  .definition-form{
    display: none;
  }
  .definition-delete-wrap{
    margin-top: 4px;
    display: flex;
    justify-content: flex-start;
  }

  .qa-fail-badge{
    display: inline-flex;
    align-items: center;
    justify-content: center;
    min-width: 22px;
    height: 22px;
    padding: 0 6px;
    border-radius: 11px;
    background: #ff4d4f;
    color: #fff;
    font-size: 12px;
    font-weight: bold;
    cursor: pointer;
    margin-left: 8px;
    flex: 0 0 auto;
    &:hover{
      background: #ff7875;
    }
  }

  .qa-fail-pop{
    max-width: 420px;
    .qa-fail-pop-title{
      display: flex;
      flex-direction: row;
      justify-content: space-between;
      align-items: center;
      font-weight: bold;
      font-size: 14px;
      margin-bottom: 10px;
      padding-bottom: 8px;
      border-bottom: 1px solid #e8e8e8;
      .qa-fail-clear{
        font-size: 12px;
        font-weight: normal;
        color: #ff4d4f;
        cursor: pointer;
        &:hover{
          color: #ff7875;
        }
      }
    }
    .qa-fail-list{
      max-height: 300px;
      overflow-y: auto;
    }
    .qa-fail-row{
      margin-bottom: 12px;
      padding-bottom: 12px;
      border-bottom: 1px dashed #f0f0f0;
      font-size: 12px;
      &:last-child{
        margin-bottom: 0;
        padding-bottom: 0;
        border-bottom: none;
      }
      .qa-fail-row-hd{
        display: flex;
        align-items: center;
        gap: 6px;
        margin-bottom: 4px;
        .qa-fail-index{
          color: #ff4d4f;
          font-weight: bold;
        }
        .qa-fail-numbers{
          color: #555;
          flex: 1 1 auto;
        }
        .qa-fail-del{
          color: #999;
          cursor: pointer;
          &:hover{
            color: #ff4d4f;
          }
        }
      }
      .qa-fail-url{
        margin-bottom: 4px;
        a{
          color: #1890ff;
          word-break: break-all;
        }
      }
      .qa-fail-text{
        color: #666;
        margin-bottom: 4px;
        line-height: 1.5;
        max-height: 36px;
        overflow: hidden;
      }
      .qa-fail-summary{
        color: #333;
        line-height: 1.5;
      }
    }
  }
</style>
