// COMPONENTS
const VARS ={
  /** 特殊组件 */
  G_NONE: 'g-none',                      // 空组件(回显数据)
  G_DYN: 'g-dyn',                        // 动态组件(占位)
  G_PANEL: 'g-panel',                    // Panel组件
  G_BLOCK: 'g-block',                    // Block组件
  G_TODO: 'g-todo',                      // 其它组件

  /** 特殊组件(显示) */
  G_NOTE: 'g-note',                      // 说明组件
  G_ALERT: 'g-alert',                    // Alert文本
  G_TEXT: 'g-text',                      // 显示文本

  /** 特殊组件(无内容) */
  G_SLOT: 'g-slot',                      // Slot组件
  G_LOCK: 'g-lock',                      // Lock组件
  G_LABEL: 'g-label',                    // Label组件

  /** 查询组件(复合) */
  Q_DYN_INTEGER: 'q-dyn-integer',        // 整数查询组件
  Q_DYN_DATE: 'q-dyn-date',              // 日期查询组件

  /** 普通组件(输入) */
  G_INPUT: 'g-input',                    // 文本组件
  G_TEXTAREA: 'g-textarea',              // 文本块组件
  G_NUMBER_INTEGER: 'g-number-integer',  // 数值整数组件
  G_NUMBER_DECIMAL: 'g-number-decimal',  // 数值精确组件
  G_PRISM_EDITOR: 'g-prism-editor',      // 代码编辑组件
  G_MAVON_EDITOR: 'g-mavon-editor',      // 富文本编辑组件
  G_TIP_TAP_EDITOR: 'g-tip-tap-editor',  // TipTap富文本编辑组件

  /** 普通组件(选择) */
  G_RATE: 'g-rate',                      // 评分数值
  G_SLIDER: 'g-slider',                  // 滑块数值
  G_ICON_SELECTOR: 'g-icon-selector',    // 图标选择
  G_SWITCH: 'g-switch',                  // Switch组件
  G_RADIO_GROUP: 'g-radio-group',        // 单选按钮组
  G_SELECT: 'g-select',                  // 选项组件
  G_MULTI_SELECT: 'g-multi-select',      // 多选组件
  G_SC_CASCADER: 'g-sc-cascader',        // 字典多层次
  G_DATE_PICKER: 'g-date-picker',        // 日期选择组件

  /** 普通组件(多值) */
  // 区间范围
  G_NUMBER_RANGE: 'g-number-range',      // 数值范围组件
  G_DATE_RANGE: 'g-date-range',          // 日期范围组件

  /** 关联组件(标准) */
  /* &&&& 简单关联 &&&& */
  G_REF_USER: 'g-ref-user',              // 用户选择组件
  G_REF_GROUP: 'g-ref-group',            // 部门选择组件
  G_REF_ADDR: 'g-ref-addr',              // 地址选择组件
  /* &&&& 上传关联 &&&& */
  G_UPLOAD_FILE: 'g-upload-file',        // 文件上传组件
  G_UPLOAD_PIC: 'g-upload-pic',          // 图片上传组件
  /* &&&& 关联关系 &&&& */
  G_ACTIVITY: 'g-activity',              // 关联活动组件
  G_ATTACHMENT: 'g-attachment',          // 关联附件组件

  /** 关联组件(特殊) */
  G_LINK_USER: 'g-link-user',            // 用户关联组件
  G_LINK_TAG: 'g-link-tag',              // 标签组件
  G_REF_ONE: 'g-ref-one',                // 关联单个
  G_REF_MANY: 'g-ref-many',              // 关联多个

  /** 零码开发 */
  // 字段编辑组件
  G_OPTIONS_EDITOR: 'g-options-editor',  // 选项编辑
  G_REF_EDITOR: 'g-ref-editor',          // 引用编辑

  /** PART组件 */
  PART_RELATIONS: 'PartRelations',

  /** TODO 需要整理 */
  G_SC_SELECT: 'g-sc-select',            // 字典单选
  G_TSELECT: 'g-tselect',                // 存在判断组件(仅用于查询)
  G_TREE_SELECT: 'g-tree-select',        // Tree下拉选项
  G_TIME_PICKER: 'g-time-picker',        // 时间选择组件
  // 关联组件
  G_REF_DOMAIN: 'g-ref-domain',          // 普通对象选择
}
export default VARS
