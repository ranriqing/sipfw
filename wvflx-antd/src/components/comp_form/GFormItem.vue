<template>

  <!-- @@@@@@@@@@ 特殊组件(有内容) @@@@@@@@@@ -->
  <!-- None空白组件：配合slot复合组件回显数据，见：FieldPage.slotRelation -->
  <div v-if="item.component === COMPO.G_NONE"></div>
  <!-- Panel折叠组件：用于展示Panel，标题为item.title -->
  <a-form-item
    v-else-if="item.component === COMPO.G_PANEL"
    style="margin-bottom: 0;"
    :label-col="{}"
    :wrapper-col="{}"
    :label="null"
    :labelAlign="cptLabelAlign"
    :key="item.field">
    <GPanel
      :title="item.title"
      :icon="_.get(item, 'extend.icon')"
      :collapsible="_.get(item, 'extend.collapsible')"
      @collapsePanel="$emit('collapsePanel', $event)">
    </GPanel>
  </a-form-item>
  <!-- Block区隔组件：用于展示区隔块，标题为item.title -->
  <a-form-item
    v-else-if="item.component === COMPO.G_BLOCK"
    style="margin-bottom: 0;"
    :label-col="{}"
    :wrapper-col="{}"
    :label="null"
    :labelAlign="cptLabelAlign"
    :key="item.field">
    <GBlock
      :title="item.title">
    </GBlock>
  </a-form-item>
  <!-- Note说明组件：用于展示占位符，用于表单收集 -->
  <a-form-item
    v-else-if="item.component === COMPO.G_NOTE"
    style="margin-bottom: 0;"
    :label-col="labelCol"
    :wrapper-col="wrapperCol"
    :label="item.title"
    :labelAlign="cptLabelAlign"
    :key="item.field">
    <GNote
      :placeholder="cptPlaceholder"
      v-decorator="[item.field, item.options]">
    </GNote>
  </a-form-item>
  <!-- Alert文本组件：用于展示value为Alert信息，见：组织管理 -> 重置密码 -->
  <a-form-item
    v-else-if="item.component === COMPO.G_ALERT"
    style="margin-bottom: 0;"
    :label-col="{}"
    :wrapper-col="{}"
    :label="null"
    :labelAlign="cptLabelAlign"
    :key="item.field">
    <GAlert
      :type="_.get(item.extend, 'type')"
      v-decorator="[item.field, item.options]">
    </GAlert>
  </a-form-item>

  <!-- @@@@@@@@@@ 特殊组件(无内容) @@@@@@@@@@ -->
  <!-- Slot插槽组件：定义一个插槽 -->
  <a-form-item
    v-else-if="item.component === COMPO.G_SLOT"
    :class="item.extraClass"
    :label-col="labelCol"
    :wrapper-col="wrapperCol"
    :label="item.title"
    :labelAlign="cptLabelAlign"
    :extra="item.extra"
    :key="item.field">
    <slot :name="item.field"></slot>
  </a-form-item>
  <!-- Lock锁定组件：批量编辑锁定状态 -->
  <a-form-item
    v-else-if="item.component === COMPO.G_LOCK"
    :label-col="labelCol"
    :wrapper-col="wrapperCol"
    :label="item.title"
    :labelAlign="cptLabelAlign"
    :extra="item.extra"
    :key="item.field">
    <div>
      <span>不编辑此字段</span>
    </div>
  </a-form-item>
  <!-- Label展示组件：仅展示label内容，配合其它组件构造表单，见：labPopTabPopConfig -->
  <a-form-item
    v-else-if="item.component === COMPO.G_LABEL"
    :label-col="labelCol"
    :wrapper-col="wrapperCol"
    :label="item.title"
    :labelAlign="cptLabelAlign"
    :extra="item.extra"
    :key="item.field"
    style="margin-bottom: 0;">
  </a-form-item>

  <!-- @@@@@@@@@@ 查询组件(复合) @@@@@@@@@@ -->
  <!-- TODO 需要优化 -->
  <a-form-item
    v-else-if="item.component === COMPO.Q_DYN_INTEGER"
    :label-col="labelCol"
    :wrapper-col="wrapperCol"
    :label="item.title"
    :labelAlign="cptLabelAlign"
    :key="item.field">
    <QDynInteger
      :item="item"
      :placeholder="cptPlaceholder"
      v-decorator="[item.field, item.options]">
    </QDynInteger>
  </a-form-item>
  <a-form-item
    v-else-if="item.component === COMPO.Q_DYN_DATE"
    :label-col="labelCol"
    :wrapper-col="wrapperCol"
    :label="item.title"
    :labelAlign="cptLabelAlign"
    :key="item.field">
    <QDynDate
      :item="item"
      :placeholder="cptPlaceholder"
      v-decorator="[item.field, item.options]">
    </QDynDate>
  </a-form-item>


  <!-- @@@@@@@@@@ 关联关系 @@@@@@@@@@ -->
  <!-- RefMany组件：关联多个关联关系
       通过validate-status，不让校验失败干扰子Form的校验失败展示
       validateTrigger: []，不让组件的change事件触发校验 -->
  <a-form-item
    v-else-if="item.component === COMPO.G_REF_MANY"
    :label-col="labelCol"
    :wrapper-col="wrapperCol"
    :label="item.title"
    :labelAlign="cptLabelAlign"
    :extra="item.extra"
    :key="item.field"
    validateStatus="success">
    <GRefMany
      ref="refMany"
      :item="item"
      :readonly="cptReadonly"
      :disabled="cptDisabled"
      :placeholder="cptPlaceholder"
      :record="record"
      v-decorator="[item.field, Object.assign(
        item.options || {}, {validateTrigger: [], rules: [
          { required: !!_.get(item,'extend.required'), message: `${item['title']}为必填项` },
          { validator: oneToManyDecorator, }
        ]}
      )]">
    </GRefMany>
  </a-form-item>

  <!-- @@@@@@@@@@ 普通组件(输入) @@@@@@@@@@ -->
  <a-form-item
    v-else
    :label-col="labelCol"
    :wrapper-col="wrapperCol"
    :label="item.title"
    :labelAlign="cptLabelAlign"
    :extra="item.extra"
    :key="item.field">
    <!-- debug: {{ context }} -->
    <!-- &&&& 展示组件 &&&& -->
    <!-- Value展示 -->
    <GText
      v-if="item.component === COMPO.G_TEXT"
      v-decorator="[item.field, item.options]">
    </GText>
    <!-- &&&& 输入组件 &&&& -->
    <!-- 文本输入 -->
    <GInput
      v-else-if="item.component === COMPO.G_INPUT"
      :item="item"
      :readonly="cptReadonly"
      :disabled="cptDisabled"
      :placeholder="cptPlaceholder"
      v-decorator="[item.field, item.options]">
    </GInput>
    <GTextarea
      v-else-if="item.component === COMPO.G_TEXTAREA"
      :item="item"
      :readonly="cptReadonly"
      :disabled="cptDisabled"
      :placeholder="cptPlaceholder"
      v-decorator="[item.field, item.options]">
    </GTextarea>
    <!-- 输入整数 -->
    <GNumberInteger
      v-if="item.component === COMPO.G_NUMBER_INTEGER"
      :item="item"
      :readonly="cptReadonly"
      :disabled="cptDisabled"
      :placeholder="cptPlaceholder"
      v-decorator="[item.field, item.options]">
    </GNumberInteger>
    <!-- 输入精确数值 -->
    <GNumberDecimal
      v-if="item.component === COMPO.G_NUMBER_DECIMAL"
      :item="item"
      :readonly="cptReadonly"
      :disabled="cptDisabled"
      :placeholder="cptPlaceholder"
      v-decorator="[item.field, item.options]">
    </GNumberDecimal>
    <!-- 输入代码(高亮编辑代码) -->
    <GPrismEditor
      v-else-if="item.component === COMPO.G_PRISM_EDITOR"
      :item="item"
      :readonly="cptReadonly"
      :disabled="cptDisabled"
      :placeholder="cptPlaceholder"
      v-decorator="[item.field, item.options]">
    </GPrismEditor>
    <!-- 输入富文本Markdown 
    <GMavonEditor
      v-else-if="item.component === COMPO.G_MAVON_EDITOR"
      :item="item"
      :readonly="cptReadonly"
      :disabled="cptDisabled"
      :placeholder="cptPlaceholder"
      :initValue="_.toString(_.get(record, item.field))"
      v-decorator="[item.field, item.options]">
    </GMavonEditor> -->
    <!-- 输入富文本TipTap -->
    <GTipTapEditor
      v-else-if="item.component === COMPO.G_TIP_TAP_EDITOR"
      :readonly="cptReadonly"
      :disabled="cptDisabled"
      :placeholder="cptPlaceholder"
      :mode="_.defaultTo(_.get(item, 'extend.mode'), 'simple')"
      v-decorator="[item.field, item.options]">
    </GTipTapEditor>
    <!-- 高级文本(组件较大) -->
    <!--<GTinymce
      v-else-if="item.component === 'g-richtext'"
      :item="item"
      :readonly="cptReadonly"
      :disabled="cptDisabled"
      :placeholder="cptPlaceholder"
      v-decorator="[item.field, item.options]">
    </GTinymce>-->

    <!-- @@@@@@@@@@ 普通组件(选择) @@@@@@@@@@ -->
    <!-- 选择评分 -->
    <GRate
      v-if="item.component === COMPO.G_RATE"
      :item="item"
      :readonly="cptReadonly"
      :disabled="cptDisabled"
      :placeholder="cptPlaceholder"
      v-decorator="[item.field, item.options]">
    </GRate>
    <!-- 选择滑块 -->
    <GSlider
      v-if="item.component === COMPO.G_SLIDER"
      :item="item"
      :readonly="cptReadonly"
      :disabled="cptDisabled"
      :placeholder="cptPlaceholder"
      v-decorator="[item.field, item.options]">
    </GSlider>
    <!-- 图标选择 -->
    <GIconSelector
      v-else-if="item.component === COMPO.G_ICON_SELECTOR"
      :item="item"
      :readonly="cptReadonly"
      :disabled="cptDisabled"
      :placeholder="cptPlaceholder"
      v-decorator="[item.field, item.options]">
    </GIconSelector>
    <!-- Switch组件 -->
    <GSwitch
      v-else-if="item.component === COMPO.G_SWITCH"
      :item="item"
      :readonly="cptReadonly"
      :disabled="cptDisabled"
      :placeholder="cptPlaceholder"
      v-decorator="[item.field, item.options]"
      @change="onOptionChange($event)">
    </GSwitch>
    <!-- 单选按钮组 -->
    <GRadioGroup
      v-else-if="item.component === COMPO.G_RADIO_GROUP"
      :item="item"
      :readonly="cptReadonly"
      :disabled="cptDisabled"
      :placeholder="cptPlaceholder"
      :optionsArray="extendOptionsArray()"
      v-decorator="[item.field, item.options]"
      @change="onOptionChange($event)">
    </GRadioGroup>
    <!-- 下拉选择组件 -->
    <GSelect
      v-else-if="item.component === COMPO.G_SELECT"
      :item="item"
      :readonly="cptReadonly"
      :disabled="cptDisabled"
      :placeholder="cptPlaceholder"
      :optionsArray="extendOptionsArray()"
      v-decorator="[item.field, item.options]"
      @change="onOptionChange($event)">
    </GSelect>
    <!-- 数据字典组件 -->
    <GScCascader
      v-else-if="item.component === COMPO.G_SC_CASCADER"
      :item="item"
      :readonly="cptReadonly"
      :disabled="cptDisabled"
      :placeholder="cptPlaceholder"
      v-decorator="[item.field, item.options]">
    </GScCascader>
    <!-- 日期选择 -->
    <GDatePicker
      v-else-if="item.component === COMPO.G_DATE_PICKER"
      :item="item"
      :readonly="cptReadonly"
      :disabled="cptDisabled"
      :placeholder="cptPlaceholder"
      v-decorator="[item.field, Object.assign(item.options || {}, {initialValue: extendInitialValue()})]">
    </GDatePicker>

    <!-- @@@@@@@@@@ 普通组件(多值) @@@@@@@@@@ -->
    <!-- 数值范围 -->
    <GNumberRange
      v-else-if="item.component === COMPO.G_NUMBER_RANGE"
      :item="item"
      :readonly="cptReadonly"
      :disabled="cptDisabled"
      :placeholder="cptPlaceholder"
      v-decorator="[item.field, item.options]">
    </GNumberRange>
    <!-- 日期范围 -->
    <GDateRange
      v-else-if="item.component === COMPO.G_DATE_RANGE"
      :item="item"
      :readonly="cptReadonly"
      :disabled="cptDisabled"
      :placeholder="cptPlaceholder"
      v-decorator="[item.field, Object.assign(item.options || {}, {initialValue: extendInitialValue()})]">
    </GDateRange>

    <!-- @@@@@@@@@@ 普通组件(链接) @@@@@@@@@@ -->
    <!-- 关联多个 -->
    <GLinkUser
      v-else-if="item.component === COMPO.G_LINK_USER"
      :item="item"
      :readonly="cptReadonly"
      :disabled="cptDisabled"
      :placeholder="cptPlaceholder"
      v-decorator="[item.field, Object.assign(item.options || {}, {initialValue: extendInitialValue()})]">
    </GLinkUser>
    <!-- 标签选项 -->
    <GLinkTag
      v-else-if="[COMPO.G_LINK_TAG].indexOf(item.component)!==-1"
      :item="item"
      :readonly="cptReadonly"
      :disabled="cptDisabled"
      :placeholder="cptPlaceholder"
      :indexUri="_.defaultTo(_.get(item.extend,'indexUri'), '/demo/index')"
      :queryOn="_.defaultTo(_.get(item.extend,'relQueryOn'), true)"
      :queryField="_.defaultTo(_.get(item.extend,'relQueryField'), 'demo')"
      :conditionParams="_.get(item.extend,'relQueryParams')"
      v-decorator="[item.field, item.options]"
      @change="onOptionChange($event)">
    </GLinkTag>
    <GRefOne
      v-else-if="item.component === COMPO.G_REF_ONE"
      :item="item"
      :readonly="cptReadonly"
      :disabled="cptDisabled"
      :placeholder="cptPlaceholder"
      :conditionParams="refOneConditionParams(item, context)"
      v-decorator="[item.field, decoratorOptions()]"
      @change="(event, valueObj) => onOptionChange(event, valueObj)">
    </GRefOne>


    <!-- @@@@@@@@@@ 关联组件(标准) @@@@@@@@@@ -->
    <!-- &&&& 简单关联 &&&& -->
    <!-- 相关用户 -->
    <GRefUser
      v-else-if="item.component === COMPO.G_REF_USER"
      :item="item"
      :readonly="cptReadonly"
      :disabled="cptDisabled"
      :placeholder="cptPlaceholder"
      v-decorator="[item.field, item.options]">
    </GRefUser>
    <!-- 相关部门 -->
    <GRefGroup
      v-else-if="item.component === COMPO.G_REF_GROUP"
      :item="item"
      :readonly="cptReadonly"
      :disabled="cptDisabled"
      :placeholder="cptPlaceholder"
      v-decorator="[item.field, item.options]">
    </GRefGroup>
    <!-- 相关地址 -->
    <GRefAddr
      v-else-if="item.component === COMPO.G_REF_ADDR"
      :item="item"
      :readonly="cptReadonly"
      :disabled="cptDisabled"
      :placeholder="cptPlaceholder"
      v-decorator="[item.field, item.options]">
    </GRefAddr>
    <GActivity
      v-else-if="item.component === COMPO.G_ACTIVITY"
      :item="item"
      :readonly="cptReadonly"
      :disabled="cptDisabled"
      :placeholder="cptPlaceholder"
      :record="record"
      v-decorator="[item.field, item.options]">
    </GActivity>
    <!-- &&&& 上传关联 &&&& -->
    <!-- 上传图片 -->
    <GUploadPic
      v-else-if="item.component === COMPO.G_UPLOAD_PIC"
      :item="item"
      :readonly="cptReadonly"
      :disabled="cptDisabled"
      :placeholder="cptPlaceholder"
      v-decorator="[item.field, item.options]">
    </GUploadPic>
    <!-- 上传文件 -->
    <GUploadFile
      v-else-if="item.component === COMPO.G_UPLOAD_FILE"
      :item="item"
      :readonly="cptReadonly"
      :disabled="cptDisabled"
      :placeholder="cptPlaceholder"
      v-decorator="[item.field, item.options]">
    </GUploadFile>
    <GAttachment
      v-else-if="item.component === COMPO.G_ATTACHMENT"
      :item="item"
      :readonly="cptReadonly"
      :disabled="cptDisabled"
      :placeholder="cptPlaceholder"
      :record="record"
      v-decorator="[item.field, Object.assign(
        item.options || {},
        {rules: [
          {
            required: (_.get(item,'extend.requiredFiles') || []).length>0,
            validator: validators.attachmentValidator((_.get(item,'extend.requiredFiles') || [])),
          }
        ]}
      )]">
    </GAttachment>

    <!-- @@@@@@@@@@ 零码开发 @@@@@@@@@@ -->
    <!-- &&&& 字段编辑组件 &&&& -->
    <!-- 选项编辑(可定义Label的Placeholder，添加展示Caption) -->
    <GOptionsEditor
      v-else-if="item.component === COMPO.G_OPTIONS_EDITOR"
      :labelHolder="_.get(item.extend,'labelHolder')"
      :addCaption="_.get(item.extend,'addCaption')"
      v-decorator="[item.field, item.options]">
    </GOptionsEditor>
    <GSipCodeMapEditor
      v-else-if="item.component === 'g-sip-code-map-editor'"
      :codeHolder="_.get(item.extend,'codeHolder')"
      :labelHolder="_.get(item.extend,'labelHolder')"
      :addCaption="_.get(item.extend,'addCaption')"
      v-decorator="[item.field, item.options]">
    </GSipCodeMapEditor>
    <GSipCodeTextareaEditor
      v-else-if="item.component === 'g-sip-code-textarea-editor'"
      :codeHolder="_.get(item.extend,'codeHolder')"
      :labelHolder="_.get(item.extend,'labelHolder')"
      :addCaption="_.get(item.extend,'addCaption')"
      v-decorator="[item.field, item.options]">
    </GSipCodeTextareaEditor>
    <!-- 引用编辑 -->
    <GRefEditor
      v-else-if="item.component === COMPO.G_REF_EDITOR"
      :item="item"
      :record="record"
      :relDomainName="extendRelDomainName(item)"
      :colsLimit="_.defaultTo(_.get(item.extend,'colsLimit'), 5)"
      v-decorator="[item.field, item.options]">
    </GRefEditor>

    <!-- @@@@@@@@@@ TODO 需要整理 @@@@@@@@@@ -->

    <!-- 下拉选项(可定义选项，是否展示搜索) -->
    <!-- g-tselect 查询存在组件 { 存在: '1', 不存在: '0'}  -->
    <GSelect
      v-else-if="[COMPO.G_TSELECT].indexOf(item.component)!==-1"
      v-decorator="[item.field, item.options]"
      :showSearch="_.defaultTo(_.get(item.extend,'showSearch'), false)"
      :disabled="cptDisabled"
      :allowClear="cptAllowClear"
      :placeholder="cptPlaceholder"
      :optionsArray="extendOptionsArray()"
      @change="onOptionChange($event)">
    </GSelect>
    <GMultiSelect
      v-else-if="item.component === 'g-multi-select'"
      v-decorator="[item.field, item.options]"
      :showSearch="_.defaultTo(_.get(item.extend,'showSearch'), false)"
      :disabled="cptDisabled"
      :allowClear="cptAllowClear"
      :placeholder="cptPlaceholder"
      :optionsArray="extendOptionsArray()"
      @change="onOptionChange($event)">
    </GMultiSelect>
    <!-- g-sc-select -->
    <GScSelect
      v-else-if="item.component === COMPO.G_SC_SELECT"
      v-decorator="[item.field, item.options]"
      :sContext="sContext"
      :disabled="cptDisabled"
      :placeholder="cptPlaceholder"
      :scKey="_.defaultTo(_.get(item.extend,'scKey'), 'demo')"
      @change="onOptionChange($event)">
    </GScSelect>
    <GScSelect
      v-else-if="item.component === 'g-sc-multi-select'"
      v-decorator="[item.field, item.options]"
      :sContext="sContext"
      :multiple="true"
      :disabled="cptDisabled"
      :placeholder="cptPlaceholder"
      :scKey="_.defaultTo(_.get(item.extend,'scKey'), 'demo')"
      @change="onOptionChange($event)">
    </GScSelect>
    <!-- Tree下拉选项(可定义选项) -->
    <GTreeSelect
      v-else-if="item.component === COMPO.G_TREE_SELECT"
      v-decorator="[item.field, item.options]"
      :disabled="cptDisabled"
      :allowClear="cptAllowClear"
      :placeholder="cptPlaceholder"
      :treeOptions="extendTreeOptions()"
      @change="onOptionChange($event)">
    </GTreeSelect>
    <!-- 时间选择 -->
    <a-time-picker
      v-else-if="item.component === COMPO.G_TIME_PICKER"
      :disabled="cptDisabled"
      :placeholder="cptPlaceholder"
      v-decorator="[item.field, item.options]">
    </a-time-picker>
    <!-- 普通对象(可定义单个加载路径、列表加载路径、是否开启查询、开启查询字段、限制条件对象) -->
    <GAutoComplete
      v-else-if="item.component === COMPO.G_REF_DOMAIN"
      :sContext="sContext"
      :disabled="cptDisabled"
      :vCaption="cptVCaption"
      :placeholder="cptPlaceholder"
      :showUri="_.defaultTo(_.get(item.extend,'showUri'), '/demo/show')"
      :indexUri="_.defaultTo(_.get(item.extend,'indexUri'), '/demo/index')"
      :queryOn="_.defaultTo(_.get(item.extend,'relQueryOn'), true)"
      :queryField="_.defaultTo(_.get(item.extend,'relQueryField'), 'demo')"
      :conditionParams="_.get(item.extend,'relQueryParams')"
      v-decorator="[item.field, decoratorOptions()]"
      @change="(event, valueObj) => onOptionChange(event, valueObj)">
    </GAutoComplete>

    <!-- &&&& 显示组件 &&&& -->
    <!-- 其它组件(预留Demo部分) -->
    <GTrmSubProducts
      v-else-if="item.component === 'g-trm-sub-products'"
      :disabled="cptDisabled"
      v-decorator="[item.field, item.options]">
    </GTrmSubProducts>
    <a-input
      v-else-if="item.component === COMPO.G_TODO"
      :disabled="cptDisabled"
      :placeholder="cptPlaceholder"
      v-decorator="[item.field, item.options]">
    </a-input>
  </a-form-item>
</template>

<!--
# 组件说明
  Form内不同类型组件整合，支持不同类型的Form组件；
# 配置组件(item字段)
  title: String                  // 字段展示名， eg： 姓名
  extra: String                  // 字段扩展信息， eg： 姓名不能为特殊字符
  field: String                  // 字段Field， eg： name
  placeholder: String            // 字段占位符， eg： 请输入姓名
  component: String              // 具体组件， eg： g-input
  options: Object {              // 同 https://www.antdv.com/components/form/ 中 getFieldDecorator(id, options)
    normalize                    // options 展示内容调整
    initialValue                 // options 初始值
    rules: object[]              // options 校验规则
  }
  labelCol: Object               // Label布局， eg： {span: 3, offset: 12}
  wrapperCol: Object             // 组件布局， eg： {span: 3, offset: 12}
  extend: Object {
    disabled: String             // 'true'字符串，则禁用
    allowClear: Boolean          // 可清空组件，是否允许情况，默认true
    options: Object              // 选择组件的options选项
  }
  change: Function(record, context, value){  // Change选项改变
    // 回调函数体
  }
# 操作方法
  $emit('change', value)
# 特殊组件：
  g-panel 无label
  g-slot 根据field，动态创建slot
  g-note 根据placeHolder，展示固定文本
-->


<script>
  import {COMPO, FIELD, OTHER} from '@/constants'
  import { tools, domains, converters, validators } from '@/utils/util'
  import GPanel from './special/GPanel'
  import GBlock from './special/GBlock'
  import GNote from './special/GNote'
  import GAlert from './special/GAlert'
  import GText from './special/GText'
  // G_SLOT、G_LOCK、G_LABEL
  import QDynDate from './combo/QDynDate'
  import QDynInteger from './combo/QDynInteger'
  import GInput from './input/GInput'
  import GTextarea from './input/GTextarea'
  import GNumberInteger from './input/GNumberInteger'
  import GNumberDecimal from './input/GNumberDecimal'
  import GPrismEditor from './input/GPrismEditor'
  // import GMavonEditor from "./input/GMavonEditor";
  import GTipTapEditor from "./input/GTipTapEditor";
  import GRate from './select/GRate'
  import GSlider from './select/GSlider'
  import GIconSelector from './select/GIconSelector'
  import GSwitch from './select/GSwitch'
  import GRadioGroup from './select/GRadioGroup'
  import GSelect from './select/GSelect'
  import GMultiSelect from './select/GMultiSelect'
  import GScCascader from './select/GScCascader'
  import GDatePicker from './select/GDatePicker'
  import GNumberRange from './multi/GNumberRange'
  import GDateRange from './multi/GDateRange'
  import GLinkUser from './link/GLinkUser'
  import GLinkTag from './link/GLinkTag'
  import GRefUser from './refer/GRefUser'
  import GRefGroup from './refer/GRefGroup'
  import GRefAddr from './refer/GRefAddr'
  import GUploadPic from './refer/GUploadPic'
  import GUploadFile from './refer/GUploadFile'
  import GRefOne from './refer/GRefOne'
  import GRefMany from './refer/GRefMany'
  import GActivity from './refer/GActivity'
  import GAttachment from './refer/GAttachment'
  import GOptionsEditor from './zlab/GOptionsEditor'
  import GSipCodeMapEditor from './zlab/GSipCodeMapEditor'
  import GSipCodeTextareaEditor from './zlab/GSipCodeTextareaEditor'
  import GRefEditor from './zlab/GRefEditor'

  import GScSelect from './GScSelect'
  import GTreeSelect from './GTreeSelect'
  import GAutoComplete from './GAutoComplete'
  import GTrmSubProducts from './GTrmSubProducts'


  export default {
    name: "GFormItem",
    components: {
      /** 特殊组件 */
      GPanel,
      GBlock,
      GNote,
      GAlert,
      GText,
      /** 查询组件(复合) */
      QDynDate,
      QDynInteger,
      /** 普通组件(输入) */
      GInput,
      GTextarea,
      GNumberInteger,
      GNumberDecimal,
      GPrismEditor,
      GTipTapEditor,
      // GTinymce: import('./input/GTinymce'),
      /** 普通组件(选择) */
      GRate,
      GSlider,
      GIconSelector,
      GSwitch,
      GRadioGroup,
      GSelect,
      GMultiSelect,
      GScCascader,
      GDatePicker,
      /** 普通组件(多值) */
      GNumberRange,
      GDateRange,
      GLinkUser,
      GLinkTag,
      /** 关联组件(标准) */
      GRefUser,
      GRefGroup,
      GRefAddr,
      GUploadPic,
      GUploadFile,
      GRefOne,
      GRefMany,
      GActivity,
      GAttachment,
      /** 零码开发 */
      GRefEditor,
      GOptionsEditor,
      GSipCodeMapEditor,
      GSipCodeTextareaEditor,

      GScSelect,
      GTreeSelect,
      GAutoComplete,

      GTrmSubProducts,

    },
    props: {
      form: {
        type: Object,
        required: true
      },
      item: {
        type: Object,
        required: true
      },
      /* 记录信息 */
      record: {
        type: Object,
        default: function(){
          return {}
        }
      },
      context: {
        type: Object,
        default: function(){
          return {}
        }
      },
      /* Super信息 */
      sContext: {
        type: Object,
        required: false,
        default: function(){
          return {}
        }
      },
      /* 样式信息 */
      labelCol: {
        type: Object,
        default: function(){
          return {}
        }
      },
      wrapperCol: {
        type: Object,
        default: function(){
          return {}
        }
      },
    },
    data(){
      return {
        COMPO: COMPO,
        converters: converters,
        validators: validators,
      }
    },
    computed: {
      cptDisabled: function(){
        let that = this
        let disabledStr = that._.toString(that._.get(that.item,'extend.disabled'))
        return disabledStr === 'true'
      },
      cptReadonly: function(){
        let that = this
        let readonlyStr = that._.toString(that._.get(that.item,'extend.readonly'))
        return readonlyStr === 'true'
      },
      cptLabelAlign: function(){
        let that = this
        return that._.get(that.item,'extend.labelAlign')
      },
      cptVCaption: function(){
        let that = this
        let valueCaption = that._.toString(that._.get(that.item,'extend.vCaption'))
        return valueCaption === 'true'
      },
      cptAllowClear: function(){
        let that = this
        return that._.defaultTo(that._.get(that.item,'extend.allowClear'), true)
      },
      cptPlaceholder: function(){
        let that = this
        let itemPlaceholder = that._.get(that.item, 'placeholder') || that.item.title
        return tools.templateString(itemPlaceholder, that.record)
      },
    },
    methods: {
      oneToManyDecorator(rule, value, callback){
        let that = this
        let refMany = that.$refs['refMany']
        if(that._.isFunction(refMany?.fieldValidator)){
          refMany?.fieldValidator(rule, value, callback)
        }else{
          callback()
        }
      },
      refOneConditionParams(item, context){
        let that = this
        let relDomainId = that._.get(item, 'dfExtend.relDomainId')
        let manyToOneFields = domains.metaDomainFields(relDomainId, FIELD.MANY_TO_ONE)
        let conditionParams = {}
        manyToOneFields.forEach(field => {
          let dfExtend = converters.parseObj(field['extendJson'])
          let domainIdKey = tools.toLowerCamel(that._.get(dfExtend, OTHER.REL_DOMAIN_NAME) + FIELD.SUFFIX_ID)
          if(that._.has(context, domainIdKey)){
            conditionParams[field['fieldName'] + FIELD.SUFFIX_ID] = that._.get(context, domainIdKey) || 0
          }
        })
        return conditionParams
      },
      decoratorOptions() {
        let that = this
        if(that._.isFunction(that.item.options)){
          let optionsFunc = that.item.options
          return optionsFunc(that.record, that.context)
        }else{
          return that.item.options
        }
      },
      extendRelDomainName(item){
        let that = this
        let relDomainNameFunc = that._.get(item.extend, 'relDomainNameFunc') || null
        if(that._.isFunction(relDomainNameFunc)){
          return relDomainNameFunc(that.record, that.context)
        }else{
          return that._.get(item.extend,'relDomainName')
        }
      },
      // 选项数组
      extendOptionsArray() {
        let that = this
        // [{label, value, color?}] or {label: value}
        let optionsObj = that._.get(that.item,'extend.options') || {}
        if(that._.isFunction(optionsObj)){
          let optionsFunc = optionsObj
          let result = optionsFunc(that.record, that.context)
          return Array.isArray(result) ? result : converters.objToOptions(result)
        }else if(Array.isArray(optionsObj)){
          return optionsObj
        }else{
          return converters.objToOptions(optionsObj)
        }
      },
      // 树选项数组
      extendTreeOptions() {
        let that = this
        let treeOptions = that._.get(that.item.extend,'options') || {}
        if(that._.isFunction(treeOptions)){
          let optionsFunc = treeOptions
          return optionsFunc(that.record, that.context)
        }else{
          return treeOptions
        }
      },
      // 改变Record或Context(影响其它组件)
      onOptionChange(value, valueObj){
        let that = this
        let changeFunc = that.item.change
        if(that._.isFunction(changeFunc)){
          changeFunc(that.record, that.context, value, that.form, valueObj)
        }
      },
      // 默认值选项
      extendInitialValue(initValue) {
        let that = this
        let initValueObj = that._.get(that.item.extend, 'initValue') || initValue
        if(that._.isFunction(initValueObj)){
          let initValueFunc = initValueObj
          return initValueFunc(that.record, that.context)
        }else{
          // g-date-range 类型处理
          if(that.item.component === COMPO.G_DATE_RANGE){
            if(initValueObj === 'day'){
              initValueObj = [that.$moment().startOf('day'), that.$moment().endOf('day')]
            }else if(initValueObj === 'week'){
              initValueObj = [that.$moment().startOf('week'), that.$moment().endOf('week')]
            }else if(initValueObj === 'month'){
              initValueObj = [that.$moment().startOf('month'), that.$moment().endOf('month')]
            }else if(initValueObj === 'year'){
              initValueObj = [that.$moment().startOf('year'), that.$moment().endOf('year')]
            }
          } else if(that.item.component === COMPO.G_DATE_PICKER){
            if(initValueObj === 'now'){
              initValueObj = that.$moment()
            }else if(initValueObj === 'birthday'){
              initValueObj = that.$moment('1980-01-01', 'YYYY-MM-DD')
            }
          }
          return initValueObj
        }
      },
    }
  }
</script>
