<template>
  <div>
    <LabPageSelect>用户角色</LabPageSelect>
    <div v-show="!pageVisible" :bordered="false" class="page">
      <a-row :gutter="[12, 12]">
        <!-- 查询部分 -->
        <a-col :md="24" class="page-query-wrapper">
          <GPartQuery
            ref="slotQuery"
            :configObj="partConfig.queryConfig"
            @doSubmit="doQuery">
          </GPartQuery>
        </a-col>

        <!-- 表格部分 -->
        <a-col :md="24" class="page-table-wrapper">
          <GPartTable
            ref="slotTable"
            :configObj="partConfig.tableConfig"
            @recordAdd="onRecordAdd"
            @recordEdit="onRecordEdit">
            <template #viewRender="{text, record, index}">
              <span>{{ record['viewCaption'] }}</span>
            </template>
            <template #typeRender="{text, record, index}">
              <a-tag :color="text===1?'blue':'green'">
                {{ text===1?'限制角色':'普通角色' }}
              </a-tag>
            </template>
          </GPartTable>
        </a-col>
      </a-row>
    </div>

    <!-- 弹窗部分 -->
    <GFormPop
      ref="slotAdd"
      :popVisible="popAddVisible"
      :configObj="popConfig.addConfig">
    </GFormPop>
    <GFormPop
      ref="slotEdit"
      :popVisible="popEditVisible"
      :configObj="popConfig.editConfig">
      <template #slot_permission="{record, item}">
        <div v-if="record.id === -1">
          <a-alert :message="'超级管理员 拥有全部数据权限'" type="success"></a-alert>
        </div>
        <div v-else class="tmpl-block" :data-cptDynPermissionItems="cptDynPermissionItems">
          <div>
            <!-- 系统内置权限（固定写在配置中） -->
            <!-- <div style="margin-bottom: 10px;">
              <GPanel title="系统内置权限" :collapsible="false" />
            </div> -->
            <a-row :gutter="[16, 0]" style="margin-bottom: 12px;">
              <a-col
                class="item-group"
                v-for="(permission, index) in fixedViewPermissions"
                :key="`sys_${index}`"
                :md="_.get(permission, 'cols') || 24"
                :sm="24"
                :xs="24">
                <div class="item-label" :title="permission.name">
                  <a-icon :type="permission.icon" />
                  {{ permission.title }}
                </div>
                <div class="item-value">
                  <a-select
                    v-if="permission.field"
                    v-model="permission.field"
                    style="width: 150px;"
                    placeholder="权限字段">
                    <a-select-option
                      v-for="(option, oIndex) in converters.objToOptions(permission.fieldOptions)"
                      :key="oIndex"
                      :value="option.value">
                      {{ option.label }}
                    </a-select-option>
                  </a-select>
                  <a-select
                    v-model="permission.permission"
                    placeholder="数据范围">
                    <a-select-option
                      v-for="(option, oIndex) in converters.objToOptions(permission.options)"
                      :key="oIndex"
                      :value="option.value">
                      {{ option.label }}
                    </a-select-option>
                  </a-select>
                </div>
              </a-col>
            </a-row>

            <!-- <div style="margin-bottom: 10px;">
              <GPanel title="自定义权限" :collapsible="false" />
            </div> -->

            <div v-if="stores.isLabState()" class="config-wrapper" style="line-height: 20px;">
              <a @click="handlePermDef('panel')">分组</a>
              <a-divider type="vertical" style="margin: 0 10px;"/>
              <a @click="handlePermDef('select')">选择</a>
              <a-divider type="vertical" style="margin: 0 10px;"/>
              <a @click="handlePermDef('check')">勾选</a>
            </div>

            <draggable
              v-if="stores.isLabState()"
              tag="a-row"
              :componentData="vQueryUtil.cptComponentData()"
              class="draggable-container"
              :list="datDynPermissionItems"
              v-bind="cptPermDragTargetOptions"
              @change="handlePermDrag">
              <a-col
                v-for="(perm, index) in datDynPermissionItems"
                :key="perm.id || index"
                :md="perm.type === 'select' ? (_.get(perm, 'cfExtend.cols') || 12) : perm.type === 'check' ? (_.get(perm, 'cfExtend.cols') || 24) : 24"
                :sm="24"
                :xs="24">

                <!-- panel：分组展示 -->
                <div v-if="perm.type === 'panel'" class="perm-panel">
                  <GPanel :title="perm.title" :icon="perm.icon" :collapsible="false" />
                  <div class="perm-panel-actions config-wrapper">
                    <a-icon type="edit" @click="handlePermDef('panel', perm)"/>
                    <a-divider type="vertical" />
                    <a-popconfirm title="确定删除?" @confirm="() => doDeletePermDef(perm)">
                      <a-icon type="delete" />
                    </a-popconfirm>
                    <a-divider type="vertical" />
                    <a-icon class="drag-item-widget" type="g-drag" />
                  </div>
                </div>

                <!-- select：选择权限 -->
                <div v-else-if="perm.type === 'select'" class="item-group" style="border-bottom:none;">
                  <div class="item-label" :title="perm.title">
                    <a-icon v-if="false" :type="perm.icon" />
                    {{ perm.title }}
                  </div>
                  <div class="item-value">
                    <a-select
                      size="medium"
                      style="min-width: 60px;"
                      v-model="dynPermissionValues[perm.permission]"
                      placeholder="--">
                      <a-select-option
                        v-for="(option, oIndex) in converters.objToOptions(_.get(perm, 'cfExtend.options', {}))"
                        :key="oIndex"
                        :value="option.value">
                        {{ option.label }}
                      </a-select-option>
                    </a-select>
                    <span class="perm-actions config-wrapper">
                      <a-icon type="edit" @click="handlePermDef('select', perm)"/>
                      <a-divider type="vertical" />
                      <a-popconfirm title="确定删除?" @confirm="() => doDeletePermDef(perm)">
                        <a-icon type="delete" />
                      </a-popconfirm>
                      <a-divider type="vertical" />
                      <a-icon class="drag-item-widget" type="g-drag" />
                    </span>
                  </div>
                </div>

                <!-- check：勾选权限 -->
                <div v-else-if="perm.type === 'check'" class="perm-check-item">
                  <a-checkbox
                    :checked="_permCheckChecked(perm)"
                    @change="onPermCheck($event, perm)">
                    {{ perm.title }}
                  </a-checkbox>
                  <span class="perm-actions config-wrapper">
                    <a-icon type="edit" @click="handlePermDef('check', perm)"/>
                    <a-divider type="vertical" />
                    <a-popconfirm title="确定删除?" @confirm="() => doDeletePermDef(perm)">
                      <a-icon type="delete" />
                    </a-popconfirm>
                    <a-divider type="vertical" />
                    <a-icon class="drag-item-widget" type="g-drag" />
                  </span>
                </div>
              </a-col>
            </draggable>

            <!-- 非Lab：不提供拖拽/维护配置项，仅按 MetaPermission 定义展示 -->
            <a-row v-else :gutter="[16, 12]">
              <a-col
                v-for="(perm, index) in datDynPermissionItems"
                :key="perm.id || index"
                :md="perm.type === 'select' ? (_.get(perm, 'cfExtend.cols') || 12) : perm.type === 'check' ? (_.get(perm, 'cfExtend.cols') || 24) : 24"
                :sm="24"
                :xs="24">

                <div v-if="perm.type === 'panel'" class="perm-panel">
                  <GPanel :title="perm.title" :icon="perm.icon" :collapsible="false" />
                </div>

                <div v-else-if="perm.type === 'select'" class="item-group" style="border-bottom:none;">
                  <div class="item-label" :title="perm.title">
                    {{ perm.title }}
                  </div>
                  <div class="item-value">
                    <a-select
                      size="medium"
                      style="min-width: 60px;"
                      v-model="dynPermissionValues[perm.permission]"
                      placeholder="--">
                      <a-select-option
                        v-for="(option, oIndex) in converters.objToOptions(_.get(perm, 'cfExtend.options', {}))"
                        :key="oIndex"
                        :value="option.value">
                        {{ option.label }}
                      </a-select-option>
                    </a-select>
                  </div>
                </div>

                <div v-else-if="perm.type === 'check'" class="perm-check-item">
                  <a-checkbox
                    :checked="_permCheckChecked(perm)"
                    @change="onPermCheck($event, perm)">
                    {{ perm.title }}
                  </a-checkbox>
                </div>
              </a-col>
            </a-row>


          </div>
        </div>
      </template>
    </GFormPop>

    <!-- 元属性弹窗 -->
    <GFormPop
      ref="refMeta"
      :configObj="permMetaPopConfig">
    </GFormPop>
  </div>
</template>

<script>
  import AAlert from 'ant-design-vue/lib/alert';
  import ACheckbox from 'ant-design-vue/lib/checkbox';

  import LabPageSelect from '@/pages/LabPageSelect'
  import GPartQuery from '@/components/page_part/GPartQuery'
  import GPartTable from '@/components/page_part/GPartTable'
  import GFormPop from '@/components/page_pop/GFormPop'
  import GPanel from '@/components/comp_form/special/GPanel'

  import MetaPermission from '@/store/models/MetaPermission'

  import { OTHER } from '@/constants'
  import { converters, stores } from '@/utils/util'
  import { ormSeqOrders } from '@/api/api'
  import { vQueryUtil, vPageUtil } from '@/utilsv/vUtil'
  import partConfig from './configs/partConfig'
  import popConfig from './configs/popConfig'
  import permMetaPopConfig from './configs/permMetaPopConfig'

  export default {
    name: "RolePage",
    components: {
      AAlert,
      ACheckbox,
      LabPageSelect,
      GPartQuery,
      GPartTable,
      GFormPop,
      GPanel,
    },
    data(){
      return {
        converters: converters,
        stores: stores,
        vPageUtil: vPageUtil,
        vQueryUtil: vQueryUtil,
        partConfig: partConfig,
        popConfig: popConfig,
        permMetaPopConfig: permMetaPopConfig,
        popAddVisible: false,
        popEditVisible: false,
        // 编辑: Record的权限
        fixedViewPermissions: [],
        datDynPermissionItems: [],   // 元权限配置
        dynPermissionValues: {}, // 角色权限值
      }
    },
    computed: {
      pageVisible: function() {
        return this.popAddVisible || this.popEditVisible
      },
      // 用于把MetaPermission变换存储到data中
      cptDynPermissionItems: function () {
        let that = this
        let metaPermissions = MetaPermission.query().orderBy('seqOrder', 'asc').orderBy('id', 'asc').get()
        that.datDynPermissionItems = metaPermissions.map(item => {
          let cfExtend = converters.parseObj(item.extend) || {}
          return {
            id: item.id,
            title: item.title,
            icon: item.icon,
            seqOrder: item.seqOrder,
            type: item.type,
            permission: item.permission,
            gcsRoleId: item.gcsRoleId,
            cfExtend: cfExtend,
            extend: item.extend,
          }
        })
        return metaPermissions
      },
      cptFixedPermissions: function(){
        let fixedPermissions = []
        // 基础权限
        fixedPermissions.push({
          icon: 'apartment',
          title: '部 门',
          domainName: 'gcsGroup',
          options: {
            本人:OTHER.PEM_SELF,
            本部门:OTHER.PEM_DEPT,
            子部门:OTHER.PEM_SUB_DEPT,
            本子部:OTHER.PEM_DEPT_AND_SUB_DEPT,
            全部:OTHER.PEM_ALL
          },
          permission: OTHER.PEM_DEPT, // 默认权限字符串
          advanced: false,
          cols: 12,
        })
        fixedPermissions.push({
          icon: 'user',
          title: '用 户',
          domainName: 'gcsUser',
          permission: OTHER.PEM_SELF,
          advanced: false,
          cols: 12,
        })
        return fixedPermissions
      },
      cptPermDragTargetOptions: function () {
        return {
          animation: 200,
          group: 'metaPermission',
          ghostClass: 'ghost',
          handle: '.drag-item-widget',
        }
      },
    },
    methods:{
      handlePermDrag(changeEvent){
        let that = this
        if(!changeEvent || !changeEvent.moved){
          return
        }
        let itemOrders = []
        that.datDynPermissionItems.forEach((item, index) => {
          if(item && item.id){
            itemOrders.push({ id: item.id, seqOrder: index + 1 })
          }
        })
        ormSeqOrders('metaPermission', { items: itemOrders }).then(resp => {
          let entitys = resp.gmodel || []
          if(entitys && entitys.length > 0){
            MetaPermission.insertOrUpdate({ data: entitys })
          }
        })
      },
      /** 查询部分 */
      doQuery(queryRules){
        this.$refs.slotTable.doQuery(queryRules)
      },
      /** 弹窗操作 */
      onRecordAdd(record){
        let that = this
        let slotRef = that.$refs.slotAdd
        that.popAddVisible = slotRef.isPage()
        slotRef.showPop(record, {},
          (record, context) => { /* submit */
            let slotTable = that.$refs.slotTable
            let promise = slotTable.doRecordSave(record, context)
            promise.then(() => that.popAddVisible = false)
            return promise
          }, () => { /* cancel */
            that.popAddVisible = false
          }
        )
      },
      onRecordEdit(record) {
        let that = this
        if (record.id !== -1) {
          let recordPermissions = that._.defaultTo(record['permissions'], [])
          /** ------ 1、为动态权限提取权限值（配置值|默认值） ------ */
          let dynPermValues = {}
          // 动态权限默认值（默认未勾选）
          that.datDynPermissionItems.forEach(def => {
            if (def.type === 'panel') return
            let key = def.permission
            if(!key){ return }
            dynPermValues[key] = def.type === 'check' ? 'notAllow' : undefined
          })
          // 写入加载的真实权限值
          recordPermissions.forEach(p => {
            let key = that._.get(p, 'permission')
            let value = that._.get(p, 'value')
            if (!key) return
            let def = that.datDynPermissionItems.find(d => d.permission === key)
            if (!def) return
            dynPermValues[key] = value
          })
          that.dynPermissionValues = dynPermValues
      
          /** ------ 2、为固定权限变换为展示格式（同步提取回显权限） ------ */
          let fixedViewPermissions = []
          that.cptFixedPermissions.forEach(item => {
            // 查找DomainName相关的权限记录
            let domainRecordPermission = recordPermissions.find(recordPermission => recordPermission.permission === item.domainName)
            let dftPermissionStr = that._.defaultTo(that._.get(item, 'permission'), OTHER.PEM_SELF)
            // 当前这个条目的真实权限字符串（配置值|默认值）
            let permissionStr = that._.defaultTo(that._.get(domainRecordPermission, 'value'), dftPermissionStr)
            let rolePermission = {
              id: domainRecordPermission?.id,
              icon: that._.get(item, 'icon') || 'tag',
              title: item.title,
              name: item.domainName,
              options: that._.get(item, 'options') || {
                本人:OTHER.PEM_SELF,
                本部门:OTHER.PEM_DEPT,
                子部门:OTHER.PEM_SUB_DEPT,
                本子部:OTHER.PEM_DEPT_AND_SUB_DEPT,
                全部:OTHER.PEM_ALL
              },
              permission: permissionStr,
              cols: that._.get(item, 'cols'),
              advanced: that._.get(item, 'advanced') || false
            }
            fixedViewPermissions.push(rolePermission)
          })
          that.fixedViewPermissions = fixedViewPermissions
        }
        let slotRef = that.$refs.slotEdit
        that.popEditVisible = slotRef.isPage()
        slotRef.showPop(record, {},
          (record, context) => { /* submit */
            if(record.id !== -1){
              /** ------ 3、提取不在固定和动态权限中的记录 ------ */
              let existingPermissions = that._.defaultTo(record['permissions'], [])
              let fixedPermissionKeys = that._.defaultTo(that.cptFixedPermissions, []).map(item => item.domainName)
              let dynPermissionKeys = that.datDynPermissionItems.filter(def => def && def.type !== 'panel').map(def => def.permission).filter(key => !!key)
              let preservedPermissions = existingPermissions.filter(p => {
                let key = that._.get(p, 'permission')
                return dynPermissionKeys.indexOf(key) === -1 && fixedPermissionKeys.indexOf(key) === -1
              })
              preservedPermissions = []  // 强制为空，不留存旧权限

              /** ------ 4、固定权限对齐到权限记录 ------ */
              let fixedPermissions = that.fixedViewPermissions.map(item => {
                return { id: item.id, permission: item.name, value: item.permission }
              })

              /** ------ 5、动态权限对齐到权限记录 ------ */
              let dynPermissions = []
              that.datDynPermissionItems.forEach(def => {
                if (!def || def.type === 'panel') return
                let key = def.permission
                if (!key) return
                if (def.type === 'check') {
                  let val = that._.defaultTo(that.dynPermissionValues[key], 'notAllow')
                  dynPermissions.push({ permission: key, value: val })
                } else {
                  let val = that.dynPermissionValues[key]
                  dynPermissions.push({ permission: key, value: that._.isNil(val)?'notAllow' : val })
                }
              })

              /** ------ 6、最终汇总固定和动态权限到权限记录 ------ */
              record['permissions'] = preservedPermissions.concat(fixedPermissions).concat(dynPermissions)
            }
            let slotTable = that.$refs.slotTable
            let promise = slotTable.doRecordUpdate(record, context)
            promise.then(() => that.popEditVisible = false)
            return promise
          }, () => { /* cancel */
            that.popEditVisible = false
          }
        )
      },
      /** 其它操作 */
      doAdvancePermission(){
        this.$message.warn(`功能暂未开放 ...`);
      },
      /** Lab：维护 MetaPermission 定义 */
      handlePermDef(type, perm){
        let that = this
        let record = {
          id: perm?.id,
          title: perm?.title || '',
          icon: perm?.icon || undefined,
          type: perm?.type || type || 'panel',
          permission: perm?.permission,
          cfExtend: perm?.cfExtend || {}
        }
        if(!perm && type==='select'){
          that._.set(record, 'cfExtend.options', {
            本人:OTHER.PEM_SELF,
            本部门:OTHER.PEM_DEPT,
            子部门:OTHER.PEM_SUB_DEPT,
            本子部:OTHER.PEM_DEPT_AND_SUB_DEPT,
            全部:OTHER.PEM_ALL
          })
        }
        let slotRef = that.$refs.refMeta
        slotRef.showPop(record, { _title: perm ? '编辑权限配置' : '新增权限配置' },
          (subRecord) => {
            let cfExtend = {}
            let cols = that._.get(subRecord, 'cfExtend.cols')
            if(!that._.isNil(cols)){
              that._.set(cfExtend, 'cols', cols)
            }
            if(subRecord.type !== 'panel'){
              // select 才需要 options；check 不需要 options（每个权限一个勾选项）
              if(subRecord.type === 'select'){
                that._.set(cfExtend, 'options', that._.get(subRecord, 'cfExtend.options') || {})
              }
            }
            let payload = {
              title: subRecord.title,
              icon: subRecord.icon,
              type: subRecord.type,
              permission: subRecord.permission,
              extend: JSON.stringify(cfExtend),
            }
            let promise
            if(subRecord.id){
              promise = MetaPermission.$update({ params: { id: subRecord.id }, data: payload })
            }else{
              promise = MetaPermission.$create({ data: payload })
            }
            return promise.then(resp => {
              let entity = resp?.gmodel || resp
              if(entity){
                MetaPermission.insertOrUpdate({ data: Array.isArray(entity) ? entity : [entity] })
              }
            })
          },
          () => {}
        )
      },
      doDeletePermDef(perm){
        return MetaPermission.$delete({ params: { id: perm.id } }).then(() => {
          MetaPermission.delete(perm.id)
        })
      },
      /** 单个权限勾选 */
      _permCheckChecked(perm){
        let key = perm.permission
        return this._.get(this.dynPermissionValues, key) === 'allow'
      },
      onPermCheck(e, perm){
        let key = perm.permission
        if(!key){ return }
        let checked = e.target.checked === true
        this.$set(this.dynPermissionValues, key, checked ? 'allow' : 'notAllow')
      }
    },
    mounted() {
      let slotQuery = this.$refs.slotQuery
      slotQuery?slotQuery.onSubmit():this.doQuery([])
      stores.isPermitted("abc")
    },
  }
</script>

<style lang="scss" scoped>
  .item-group {
    display: flex;
    flex-direction: row;
    margin-bottom: 0;
    // border-bottom: 1px solid #e5e5e5;
    .item-label{
      line-height: 30px;
      display: inline-block;
      width: 80px;
      flex: 0 0 80px;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }
    .item-value{
      flex: 1;
      display: flex;
      flex-direction: row;
    }
  }

  .perm-panel{
    position: relative;
  }
  .perm-panel-actions{
    position: absolute;
    right: 0;
    top: 0;
    line-height: 25px;
    background: #fff;
    padding-left: 10px;
  }
  .perm-actions{
    margin-left: 10px;
    display: inline-flex;
    align-items: center;
  }
  .perm-check-item{
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding-left: 20px;
    line-height: 30px;
  }
  .drag-item-widget{
    cursor: move;
  }
</style>