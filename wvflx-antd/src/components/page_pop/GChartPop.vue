<template>
  <div>
    <GPlainPop
      ref="slotPop"
      class="pop-wrapper"
      :popVisible="popVisible"
      :configObj="configObj"
      :sContext="sContext"
      debugComp="GChartPop">
      <template #default="{record, context}">
        <a-form :form="form">
          <a-form-item
            v-if="reportChart"
            :label-col="cptLabelCol"
            :wrapper-col="cptWrapperCol"
            label="开启图表">
            <a-switch :checked="chartOn" @change="chartOn=$event"/>
            <a-divider style="margin: 0;"></a-divider>
          </a-form-item>
          <a-form-item
            v-if="!reportChart && targetMetaDomain"
            :label-col="cptLabelCol"
            :wrapper-col="cptWrapperCol"
            label="数据表">
            <div>
              <a-icon type="database"/>
              {{ targetMetaDomain.title }}
            </div>
            <a-divider style="margin: 0;"></a-divider>
          </a-form-item>
          <a-form-item
            v-if="!reportChart"
            :label-col="cptLabelCol"
            :wrapper-col="cptWrapperCol"
            label="过滤条件">
            <div class="condition-wrapper">
              <div
                v-for="(filterRule, index) in filterRules"
                :key="filterRule['derivedFieldName']"
                class="condition-item">
                <a-button type="link" @click="handleFilterEdit(filterRule, index)">
                  <a-icon :type="vDftUtil.fieldIcon(filterRule, 'derivedFieldType')"/>
                  {{ filterRule.derivedTitle | Ellipsis(8) }}
                  <i style="margin: 0 8px;">
                    {{ cptOperatorObj[filterRule.ruleOperator] || '未知' }}
                  </i>
                  <span v-if="[FIELD.SELECT].indexOf(filterRule.derivedFieldType)!==-1">
                    {{ _filterSelectOption(filterRule['domainCol'], filterRule['ruleValue']) | Ellipsis(16) }}
                  </span>
                  <!--<span v-else-if="filterRule.ruleOperator === '_relative'">-->
                  <span :title="_filterDateValue(filterRule)" v-else-if="[FIELD.DATE].indexOf(filterRule.derivedFieldType)!==-1">
                    {{ (_filterDateValue(filterRule) || '未知') | Ellipsis(16) }}
                  </span>
                  <span v-else>{{ filterRule['ruleValue'] | Ellipsis(16) }}</span>
                </a-button>
                <div class="item-right">
                  <a-icon @click.stop="filterRules.splice(index, 1)" type="close" />
                </div>
              </div>
              <a-button
                icon="plus"
                type="dashed"
                :block="true"
                :disabled="false"
                @click="handleFilterAdd">
                添加条件
              </a-button>
            </div>
          </a-form-item>
          <a-form-item
            v-if="!reportChart"
            :label-col="{}"
            :wrapper-col="{}">
            <a-divider style="margin: 0;"></a-divider>
          </a-form-item>
          <a-form-item
            :label-col="cptLabelCol"
            :wrapper-col="cptWrapperCol"
            label="图表类型"
            key="chartType">
            <div
              class="chart-type-wrapper"
              v-decorator="['chartType', {
                rules: [
                  { required: true, message: '' },
                ]
              }]">
              <div
                v-for="(item, index) in cptChartTypes"
                :key="index"
                :class="['chart-item', item['type']===chartType && 'active']"
                @click="() => { _self.chartType=item['type']; form.setFieldsValue({chartType: item['type']}); }">
                <div class="chart-icon">
                  <a-icon
                    v-if="item['type'] === 'bar'"
                    style="margin-right: 4px; transform: rotate(90deg) rotateY(180deg);"
                    :type="item['icon']"/>
                  <a-icon
                    v-else
                    :type="item['icon']"/>
                </div>
                <span class="chart-title">{{ item['title'] }}</span>
              </div>
            </div>
          </a-form-item>
          <a-form-item
            :label-col="{}"
            :wrapper-col="{}">
            <a-divider style="margin: 0;"></a-divider>
          </a-form-item>
          <a-form-item
            :label-col="cptLabelCol"
            :wrapper-col="cptWrapperCol"
            label="图表标题"
            key="chartTitle">
            <div
              v-decorator="['chartTitle', {
                rules: [
                  { required: true, message: '' },
                ]
              }]">
              <a-textarea
                :rows="3"
                :value="chartTitle"
                placeholder="请输入图表标题"
                @change="(e) => { _self.chartTitle=e.target.value; form.setFieldsValue({chartTitle: e.target.value}); }">
              </a-textarea>
            </div>
          </a-form-item>
          <a-form-item
            v-if="chartType !== 'map'"
            class="chart-group-wrapper"
            :label-col="cptLabelCol"
            :wrapper-col="cptWrapperCol"
            label="图表分组"
            key="chartGroup">
            <div
              class="group-item"
              v-decorator="['chartGroup', {
                rules: cptChartGroupRules
              }]">
              <a-button type="link" @click="handleColSelect('chartGroup')">
                <template v-if="chartGroup">
                  <a-icon :type="vDftUtil.fieldIcon(chartGroup, 'type')"/>
                  {{ chartGroup.title | Ellipsis(8) }}
                </template>
                <template v-else> <a-icon type="select"/>&nbsp;分组列<span class="warning">(必须)</span> </template>
              </a-button>
              <div v-if="chartGroup" class="item-right">
                <template v-if="!reportChart">
                  <a-icon @click.stop="handleColEdit('chartGroup', chartGroup)" type="edit" />
                  <a-divider type="vertical" />
                </template>
                <a-icon @click.stop="handleColDelete('chartGroup')" type="close" />
              </div>
            </div>
          </a-form-item>
          <a-form-item
            v-if="chartType === 'map'"
            class="chart-group-wrapper"
            :label-col="cptLabelCol"
            :wrapper-col="cptWrapperCol"
            label="地址字段"
            key="chartAddrField">
            <div
              class="group-item"
              v-decorator="['chartAddrField', {
                rules: cptchartAddrFieldRules
              }]">
              <a-button type="link" @click="handleColSelect('chartAddrField')">
                <template v-if="chartAddrField">
                  <a-icon :type="vDftUtil.fieldIcon(chartAddrField, 'type')"/>
                  {{ chartAddrField.title | Ellipsis(8) }}
                </template>
                <template v-else> <a-icon type="select"/>&nbsp;地址列<span class="warning">(必须)</span> </template>
              </a-button>
              <div v-if="chartAddrField" class="item-right">
                <template v-if="!reportChart">
                  <a-icon @click.stop="handleColEdit('chartAddrField', chartAddrField)" type="edit" />
                  <a-divider type="vertical" />
                </template>
                <a-icon @click.stop="handleColDelete('chartAddrField')" type="close" />
              </div>
            </div>
          </a-form-item>
          <a-form-item
            v-if="chartType !== 'overview'"
            class="chart-group-wrapper"
            :label-col="cptLabelCol"
            :wrapper-col="cptWrapperCol"
            :label="undefined"
            key="chartSeries">
            <div
              class="group-item"
              v-decorator="['chartSeries', {
                rules: []
              }]">
              <a-button type="link" @click="handleColSelect('chartSeries')">
                <template v-if="chartSeries">
                  <a-icon :type="vDftUtil.fieldIcon(chartSeries, 'type')"/>
                  {{ chartSeries.title | Ellipsis(8) }}
                </template>
                <template v-else> <a-icon type="select"/>&nbsp;分类列<span class="text-tips">(可选)</span> </template>
              </a-button>
              <div v-if="chartSeries" class="item-right">
                <template v-if="!reportChart">
                  <a-icon @click.stop="handleColEdit('chartSeries', chartSeries)" type="edit" />
                  <a-divider type="vertical" />
                </template>
                <a-icon @click.stop="handleColDelete('chartSeries')" type="close" />
              </div>
            </div>
          </a-form-item>
          <a-form-item
            class="chart-group-wrapper"
            :label-col="cptLabelCol"
            :wrapper-col="cptWrapperCol"
            label="数值计算"
            key="chartMeasure">
            <div
              class="group-item"
              v-decorator="['chartMeasure', {
                rules: [
                  { required: true, message: '' },
                ]
              }]">
              <a-button type="link" @click="handleColSelect('chartMeasure')">
                <template v-if="chartMeasure">
                  <a-icon :type="vDftUtil.fieldIcon(chartMeasure, 'type')"/>
                  {{ chartMeasure.title | Ellipsis(8) }}
                </template>
                <template v-else> <a-icon type="select"/>&nbsp;计算列<span class="warning">(必须)</span> </template>
              </a-button>
              <div v-if="chartMeasure" class="item-right">
                <template v-if="!reportChart">
                  <a-icon @click.stop="handleColEdit('chartMeasure', chartMeasure)" type="edit" />
                  <a-divider type="vertical" />
                </template>
                <a-icon @click.stop="handleColDelete('chartMeasure')" type="close" />
              </div>
            </div>
          </a-form-item>
          <a-form-item
            v-if="chartType === 'overview'"
            :label-col="cptLabelCol"
            :wrapper-col="cptWrapperCol"
            label="[概览图] 汇总标题"
            key="overviewSumTitle">
            <div
              v-decorator="['overviewSumTitle', {
                rules: []
              }]">
              <a-input
                :value="overviewSumTitle"
                placeholder="请输入汇总标题，默认「合计」"
                @change="(e) => { _self.overviewSumTitle=e.target.value; form.setFieldsValue({overviewSumTitle: e.target.value}); }">
              </a-input>
            </div>
          </a-form-item>
          <a-form-item
            v-if="chartType === 'overview'"
            :label-col="cptLabelCol"
            :wrapper-col="cptWrapperCol"
            label="[概览图] 数值单位"
            key="overviewUnit">
            <div
              v-decorator="['overviewUnit', {
                rules: []
              }]">
              <a-input
                :value="overviewUnit"
                placeholder="请输入单位，默认「个」"
                @change="(e) => { _self.overviewUnit=e.target.value; form.setFieldsValue({overviewUnit: e.target.value}); }">
              </a-input>
            </div>
          </a-form-item>
          <a-form-item
            v-if="chartType === 'overview'"
            :label-col="cptLabelCol"
            :wrapper-col="cptWrapperCol"
            label="[概览图] 展示图片"
            key="overviewShowImage">
            <a-switch :checked="overviewShowImage" @change="overviewShowImage=$event"/>
          </a-form-item>
        </a-form>
        <a-divider></a-divider>
      </template>
    </GPlainPop>

    <!-- Ref弹窗(Lab模式) -->
    <GFormPop
      ref="refFilter"
      :configObj="filterPopConfig">
      <template slot="domainCol">
        <GDomainCols
          :echoOptionOn="true"
          :metaDomains="[targetMetaDomain]"
          :fieldSupported="function(item){
            return [
              FIELD.INTEGER, FIELD.LONG, FIELD.BIG_DECIMAL,
              FIELD.STRING, FIELD.SELECT, FIELD.DATE,
            ].indexOf(item['fieldType'])!==-1
          }"
          @change="_filterDomainColChange($event)"
          v-decorator="['domainCol', {
            rules: [
              { required: true, message: '请选择过滤列' },
            ]
          }]">
        </GDomainCols>
      </template>
    </GFormPop>
    <GFormPop
      ref="refColMeta"
      :configObj="colMetaPopConfig">
    </GFormPop>
  </div>

</template>

<script>
  import AAlert from 'ant-design-vue/lib/alert';
  import AEmpty from 'ant-design-vue/lib/empty';
  import AAvatar from 'ant-design-vue/lib/avatar';
  import ACheckbox from 'ant-design-vue/lib/checkbox';

  import { FIELD, QUERY, DATE } from '@/constants'
  import { converters, stores } from '@/utils/util'
  import {vDftUtil, vReportUtil, vTableUtil} from '@/utilsv/vUtil'

  import MetaDomain from '@/store/models/MetaDomain'
  import MetaDomainField from '@/store/models/MetaDomainField'

  import GPlainPop from '@/components/page_pop/GPlainPop'
  import GFormPop from '@/components/page_pop/GFormPop'
  import GDomainCols from '@/components/comp_form/GDomainCols'

  import labChartFilterPopConfig from './configs/labChartFilterPopConfig'
  import labChartColMetaPopConfig from './configs/labChartColMetaPopConfig'
  import qDynDateConfig from '@/components/comp_form/combo/configs/qDynDateConfig'

  export default {
    name: "GChartPop",
    components: {
      AAlert,
      AEmpty,
      AAvatar,
      ACheckbox,
      GDomainCols,
      GPlainPop,
      GFormPop,
    },
    /* eslint-disable */
    props: {
      popVisible: {
        type: Boolean,
        default: false,
      },
      configObj: {
        type: Object,
        default: function(){
          return {}
        }
      },
      sContext: {
        type: Object,
        default: function(){
          return {}
        }
      },
    },
    data () {
      return {
        /** 配置&状态 */
        stores: stores,
        form: this.$form.createForm(this),  // 创建form
        loading:false,
        FIELD: FIELD,
        converters: converters,
        vDftUtil: vDftUtil,
        vTableUtil: vTableUtil,
        qDynDateConfig: qDynDateConfig,
        filterPopConfig: labChartFilterPopConfig,
        colMetaPopConfig: labChartColMetaPopConfig,
        /** 数据部分 */
        record: {},
        context: {},
        reportChart: false,
        reportColumns: [],
        targetModel: undefined,
        targetMetaDomain: undefined,
        chartOn: false,
        chartTitle: undefined,
        chartType: undefined,
        chartGroup: undefined,
        chartSeries: undefined,
        chartMeasure: undefined,
        chartAddrField: undefined,
        overviewSumTitle: undefined,
        overviewUnit: undefined,
        overviewShowImage: undefined,
        filterContext: undefined,  // [条件弹窗]作用域
        filterRules: [],
        choosedColumn: {},        // 临时使用
      }
    },
    computed: {
      cptOperatorObj: function(){
        return {
          [QUERY.EQUAL]: '等于',
          [QUERY.NOT_EQUAL]: '不等于',
          [QUERY.CONTAINS]: '包含',
          [QUERY.BEGINS_WITH]: '以开头',
          [QUERY.ENDS_WITH]: '以结尾',
          [QUERY.GREATER]: '大于',
          [QUERY.GREATER_OR_EQUAL]: '大于等于',
          [QUERY.LESS]: '小于',
          [QUERY.LESS_OR_EQUAL]: '小于等于',
          [QUERY.BETWEEN]: '之间',
          [QUERY.IS_NULL]: '为空',
          [QUERY.IS_NOT_NULL]: '不为空',
          _relative: '相对日期',
        }
      },
      cptChartTypes: function(){
        let chartTypes = [
          { type: 'line', icon: 'line-chart', title: '线型图' },
          { type: 'column', icon: 'bar-chart', title: '柱状图' },
          { type: 'bar', icon: 'bar-chart', title: '条形图' },
          { type: 'area', icon: 'area-chart', title: '面积图' },
          { type: 'pie', icon: 'pie-chart', title: '环形图' },
          { type: 'overview', icon: 'g-chart-num', title: '数量图' },
        ]
        if(!this.reportChart){
          chartTypes.push({ type: 'map', icon: 'environment', title: '中国地图' })
        }
        return chartTypes
      },
      cptChartGroupRules: function(){
        return this.chartType === 'map' ? [] : [{ required: true, message: '' }]
      },
      cptchartAddrFieldRules: function(){
        return this.chartType === 'map' ? [{ required: true, message: '请选择地址字段' }] : []
      },
      cptLabelCol: function(){
        return this.configObj.labelCol || {
          xs: { span: 24 },
          sm: { span: 5 }
        }
      },
      cptWrapperCol: function(){
        return this.configObj.wrapperCol || {
          xs: { span: 24 },
          sm: { span: 15 }
        }
      },
    },
    methods: {
      /** 弹窗操作 */
      isPage(){
        let that = this
        let slotRef = that.$refs.slotPop
        return slotRef.isPage()
      },
      showPop (record, context, submitCallback, cancelCallback) {
        let that = this
        that.form.resetFields()
        that.record = that._.pickBy(record || {}, (value, key) =>
          !that._.isNil(value) && !key.startsWith('$') && !that._.isFunction(value)
        )
        that.context = context || {}
        that.reportChart = !!that._.get(context, 'reportChart')
        if(that.reportChart){
          that.reportColumns = that._.get(context, 'reportColumns') || []  
        }
        that.targetModel = that._.get(context, 'targetModel')
        that.targetMetaDomain = MetaDomain.find(that.targetModel?.metaDomainId)
        that.chartOn = record['chartOn']
        that.chartTitle = record['chartTitle']
        that.chartType = record['chartType']
        that.chartGroup = record['chartGroup']
        that.chartSeries = record['chartSeries']
        that.chartMeasure = record['chartMeasure']
        that.chartAddrField = record['chartAddrField']
        that.overviewSumTitle = record['overviewSumTitle']
        that.overviewUnit = record['overviewUnit']
        that.overviewShowImage = record['overviewShowImage']
        that.filterRules = record['filterRules'] || [] // 不需要回显
        // 展示弹窗
        let slotRef = that.$refs.slotPop
        slotRef.showPop(record, context,
          (record, context) => { /* submit */
            record || context || true
            let valuesObj = undefined
            that.form.validateFields((errors, values) => {
              if (!errors) {
                valuesObj = values
              }
            })
            if(valuesObj){
              valuesObj['chartOn'] = that.chartOn // 不通过form设置
              valuesObj['filterRules'] = that.filterRules // 不通过form设置
              valuesObj['overviewShowImage'] = that.overviewShowImage // 不通过form设置
              return submitCallback(valuesObj, context)
            }
          }, cancelCallback)
        that.$nextTick(() => {
          that.form.setFieldsValue({
            chartTitle: that.chartTitle,
            chartType: that.chartType,
            chartGroup: that.chartGroup,
            chartSeries: that.chartSeries,
            chartMeasure: that.chartMeasure,
            chartAddrField: that.chartAddrField,
            overviewSumTitle: that.overviewSumTitle,
            overviewUnit: that.overviewUnit,
          })
        })
      },
      /** Handle操作 */
      handleFilterAdd(){
        let that = this
        that.handleFilterEdit({}, undefined)
      },
      handleFilterEdit(filterRule, ruleIndex){
        let that = this
        that.filterContext = {
          domainCol: filterRule['domainCol'],
          ruleOperator: filterRule['ruleOperator'],
        } // 外部化Context，可以从外部更改
        that.$refs.refFilter?.showPop(filterRule, that.filterContext,
          (record, context) => { /* submit */
            context || true
            let domainCol = record['domainCol']
            let ruleOperator = record['ruleOperator']
            let ruleValue = record['ruleValue']
            let dfItem = MetaDomainField.find(domainCol)
            // 日期格式化
            if([FIELD.DATE].indexOf(dfItem.fieldType)!==-1){
              ruleValue = that._formatDateValue(record)
            }
            if(dfItem && ruleOperator && !that._.isNil(ruleValue)){
              let filterRule = {
                domainCol: domainCol,
                derivedTitle: dfItem.title,
                derivedFieldName: dfItem.fieldName,
                derivedFieldType: dfItem.fieldType,
                ruleOperator: ruleOperator,
                ruleValue: ruleValue,
              }
              if(that._.isNil(ruleIndex)){
                that.filterRules.push(filterRule)
              }else{
                that.filterRules.splice(ruleIndex, 1, filterRule)
              }
              return Promise.resolve()
            }
          }, () => { /* cancel */ }
        )
      },
      handleColSelect(columnType){
        let that = this
        if(that.reportChart){
          that.handleColChoose(columnType)
          return
        }
        that.columnType = columnType
        let isMeasure = columnType === 'chartMeasure'
        let isAddrField = columnType === 'chartAddrField'
        let selectedDfItem = undefined
        that.$confirm({
          icon: 'check',
          title: isAddrField ? '请选择地址列' : '请选择图表列',
          width: '600px',
          class: 'pop-column-wrapper',
          content: function(h){
            h && true
            let vnodes = [];
            let props = {
              isChart: true,
              isMeasure: isMeasure,
              echoOptionOn: true,
              metaDomains: [that.targetMetaDomain],
              fieldSupported: isMeasure ? function(item){
                return [FIELD.INTEGER, FIELD.LONG, FIELD.BIG_DECIMAL].indexOf(item['fieldType'])!==-1
              } : isAddrField ? function(item){
                return [FIELD.ADDR].indexOf(item['fieldType'])!==-1
              } : undefined,
              placeholder: '选择列...'
            }
            const on = {
              change: (dfItemId) => {
                if('rowCount' === dfItemId){
                  selectedDfItem = {
                    title: '计数',
                    fieldName: 'rowCount',
                    fieldType: FIELD.INTEGER,
                    metaDomainFieldId: undefined,
                  }
                }else if('dateCreated' === dfItemId){
                  selectedDfItem = {
                    title: '创建时间',
                    fieldName: 'dateCreated',
                    fieldType: FIELD.DATE,
                    metaDomainFieldId: undefined,
                  }
                }else if('lastUpdated' === dfItemId){
                  selectedDfItem = {
                    title: '最近更新',
                    fieldName: 'lastUpdated',
                    fieldType: FIELD.DATE,
                    metaDomainFieldId: undefined,
                  }
                }else{
                  selectedDfItem = MetaDomainField.find(dfItemId)
                }
              },
            }
            vnodes.push(<GDomainCols {...{ props, on: on }}></GDomainCols>);
            return (<div {...{ class: 'selectable-wrapper' }}>{ vnodes }</div>)
          },
          onOk() {
            if(selectedDfItem){
              let clonedItem = vReportUtil.reportDfItem(that, selectedDfItem)
              that.$set(that.$data, columnType, clonedItem)
              that.form.setFieldsValue({[columnType]: clonedItem});
            }
          }
        });
      },
      handleColChoose(columnType){
        let that = this
        that.columnType = columnType
        let isMeasure = columnType === 'chartMeasure'
        let isAddrField = columnType === 'chartAddrField'
        that.$confirm({
          icon: 'check',
          title: isAddrField ? '请选择地址列' : '请选择图表列',
          width: '600px',
          class: 'pop-column-wrapper',
          content: function(h){
            h && true
            let vnodes = [];
            let filteredColumns = that.reportColumns.filter(item =>
              isMeasure ? [FIELD.INTEGER, FIELD.LONG, FIELD.BIG_DECIMAL].indexOf(item.type)!==-1
                : isAddrField ? [FIELD.ADDR].indexOf(item.type)!==-1
                : true
            )
            filteredColumns.forEach(item => {
              let isActive = that._.get(item, 'field') === that._.get(that.choosedColumn, 'field')
              let attrs = { class: `choose-item${isActive?' active':''}` }
              let props = { type: vDftUtil.fieldIcon(item, 'type')}
              const on = {
                click: () => {
                  that.choosedColumn = {
                    title: item.title,
                    field: item.field,
                    type: item.type,
                    metaDomainFieldId: item.metaDomainFieldId,
                  }
                },
              }
              vnodes.push(<div {...{ attrs, on: on }}>
                <b><a-icon {...{ props }} />&nbsp;{item.title}</b>
                <span>{item.type}</span>
              </div>)
            })
            return (<div {...{ class: 'choose-wrapper' }}>{vnodes}</div>)
          },
          onOk() {
            that.$set(that.$data, columnType, that.choosedColumn)
            that.form.setFieldsValue({[columnType]: that.choosedColumn});
          }
        });
      },
      handleColEdit(columnType, columnItem){
        let that = this
        that.$refs.refColMeta?.showPop(columnItem, {},
          (record, context) => { /* submit */
            context || true
            that.$set(that.$data, columnType, record)
            that.form.setFieldsValue({[columnType]: record});
            return Promise.resolve()
          }, () => { /* cancel */ }
        )
      },
      handleColDelete(columnType){
        let that = this
        that.$set(that.$data, columnType, undefined)
        that.form.setFieldsValue({[columnType]: undefined});
      },
      /** 工具方法 */
      _filterDomainColChange(value){
        let that = this
        that.filterContext['domainCol'] = value
      },
      _formatDateValue(filterRule){
        let that = this
        let ruleValue = filterRule['ruleValue']
        let ruleOperator = filterRule['ruleOperator']
        if(ruleValue){
          if(ruleOperator === '_relative'){
            return ruleValue
          }else if(QUERY.BETWEEN === ruleOperator){
            if(that._.isArray(ruleValue)){
              return [ruleValue[0]?.format(DATE.YYYYMMDD), ruleValue[1]?.format(DATE.YYYYMMDD)]
            }
            return null
          }else{
            return ruleValue.format(DATE.YYYYMMDD)
          }
        }
      },
      _filterDateValue(filterRule){
        let that = this
        let ruleValue = filterRule['ruleValue']
        let ruleOperator = filterRule['ruleOperator']
        if(ruleValue){
          if(ruleOperator === '_relative'){
            return that._.get(qDynDateConfig.relativeOptions, ruleValue) || '未知'
          }else if(QUERY.BETWEEN === ruleOperator){
            if(that._.isArray(ruleValue)){
              return that._.toString([ruleValue[0], ruleValue[1]])
            }
            return null
          }else{
            return that._.toString(ruleValue)
          }
        }
      },
      _filterSelectOption(domainCol, ruleValue){
        let that = this
        let dfItem = MetaDomainField.find(domainCol)
        let ruleValueStr = that._.toString(ruleValue)
        let transformOptions = {}
        if(dfItem){
          let extendJson = converters.parseObj(dfItem.extendJson)
          let optionsObj = extendJson['options'] || {}
          transformOptions = converters.reverseObj(optionsObj)
        }
        return that._.get(transformOptions, ruleValueStr, ruleValueStr)
      },
    }
  }
</script>

<style lang="scss" scoped>
  $tmpl-color-zero6: rgba(241, 139, 98, 0.6);
  $tmpl-color-zero8: rgba(241, 139, 98, 0.8);
  $tmpl-color-zero10: rgba(241, 139, 98, 1);
  .bug-icon {
    color: $tmpl-color-zero6;
    margin-right: 6px;
  }
  .pop-wrapper{
    .ant-form-item {
      margin-bottom: 4px;
      /deep/ .ant-form-item-label {
        line-height: 26px;
      }
    }
    .condition-wrapper{
      .condition-item{
        line-height: 1.5em;
        display: flex;
        flex-direction: row;
        justify-content: flex-start;
        align-items: center;
        &:hover{
          background-color: #f9f9fa;
        }
        /deep/ .ant-btn{
          height: 26px;
        }
        .item-right{
          margin-left: auto;
        }
      }
    }
    .chart-type-wrapper{
      padding: 10px 8px;
      display: flex;
      flex-direction: row;
      justify-content: left;
      flex-wrap: wrap;
      align-items: center;
      row-gap: 10px;
      column-gap: 10px;
      .chart-item{
        width: 64px;
        height: 64px;
        color: #7d95ad;
        background-color: #f9f9f9;
        box-shadow: 0 2px 2px 0 #e0e0e0;
        border-radius: 5px;
        line-height: 1em;
        display: flex;
        flex-direction: column;
        justify-content: center;
        align-items: center;
        cursor: pointer;
        .chart-icon{
          .anticon{
            font-size: 30px;
          }
        }
        .chart-title{
          margin-top: 4px;
          line-height: 1em;
          font-size: 12px;
        }
        &.active{
          color: #ffffff;
          background-color: #1c9e73;
          box-shadow: 0 2px 2px 0 #1c9e73;
        }
      }
    }
    .chart-group-wrapper{
      .group-item{
        line-height: 1.5em;
        display: flex;
        flex-direction: row;
        justify-content: flex-start;
        align-items: center;
        &:hover{
          background-color: #f9f9fa;
        }
        /deep/ .ant-btn{
          height: 26px;
        }
        .item-right{
          margin-right: 6px;
          margin-left: auto;
        }
      }
    }
  }
</style>