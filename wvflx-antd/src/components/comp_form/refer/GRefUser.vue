<template>
  <div>
    <div v-if="readonly">
      <AvatarUser :userId="cptValue" />
    </div>
    <a-select
      v-else
      style="width: 100%;"
      optionFilterProp="children"
      :showSearch="true"
      :disabled="disabled"
      :allowClear="_.defaultTo(_.get(item,'extend.allowClear'), true)"
      :placeholder="placeholder"
      :value="cptValue"
      :options="optionsArray()"
      :dropdownRender="dropdownRender"
      @change="onChange($event)">
    </a-select>
  </div>
</template>

<!--
# 组件说明
  选择用户组件，下拉选择用户并展示；
  为提高兼容性，组件会将选项值转为字符串；
  数据从元信息中加载，超管展示全部用户，其它用户只展示本子部用户；
# 配置组件
  value: [String, Number]   // 选中条目
  disabled: Boolean         // 禁用状态，默认false
  placeholder: String       // 占位字符串
  allowClear: Boolean       // 允许为空，默认true
# 操作方法
  $emit('change', value)
-->

<script>
  import { mapGetters } from "vuex";
  import { GETTERS } from '@/constants'
  import { stores, converters } from '@/utils/util'
  import { vPopUtil } from '@/utilsv/vUtil'

  import GcsUser from '@/store/models/GcsUser'
  import GcsGroup from '@/store/models/GcsGroup'
  import AvatarUser from "@/components/comp_tools/AvatarUser.vue";

  export default {
    name: 'GRefUser',
    components: {
      AvatarUser
    },
    /* eslint-disable */
    props: {
      item: {
        type: Object,
        required: true
      },
      value: {
        type: [String, Number],
        required: false
      },
      readonly: {
        type: Boolean,
        default: false
      },
      disabled: {
        type: Boolean,
        default: false
      },
      placeholder: {
        type: String,
        default: '请选择相关用户'
      },
    },
    computed: {
      ...mapGetters({
        currentUser: GETTERS.GET_USER_CURRENT_USER,
      }),
      cptValue: function() {
        let that = this
        return that._.toString(that.value)===''?undefined:that._.toString(that.value)
      },
      cptAdmin: function() {
        return stores.isSuperAdmin()
      },
    },
    methods: {
      onChange (value) {
        this.$emit('change', value)
      },
      selectUser(){
        let that = this
        vPopUtil.selectTransferUser(
          that, {}, { single: true },
          (selectedUsers) => {
            if (selectedUsers && selectedUsers.length > 0) {
              that.onChange(that._.toString(selectedUsers[0].id))
            }
            return Promise.resolve()
          }, () => {
            /* cancel */
          })
      },
      dropdownRender(menu) {
        if (this.disabled) return menu
        const h = this.$createElement
        return h('div', [
          menu,
          h('a-divider', { style: { margin: '4px 0' } }),
          h('div', {
            style: { padding: '4px 8px', cursor: 'pointer', textAlign: 'center', color: '#1890ff' },
            on: { click: this.selectUser }
          }, [
            h('a-icon', { attrs: { type: 'select' }, style: { marginRight: '4px' } }),
            '弹窗选择用户'
          ])
        ])
      },
      /** 扩展方法 */
      optionsArray(){
        let that = this
        let optionsObj = {}
        let allItems = []
        if(!that.cptAdmin){
          let groupIds = []
          let queryGroupId = that.currentUser.groupId
          let queryGroup = GcsGroup.find(queryGroupId)
          if(queryGroup){
            let allGroups = GcsGroup.query().orderBy('id').get()
            let subGroups = that._recurseSubGroups(queryGroup, allGroups, true)
            groupIds = subGroups.map(item => item.id)
          }
          allItems = GcsUser.query().where(item => {
            let groupId = groupIds.find( groupId => groupId === item.gcsGroupId )
            return !!groupId
          }).with('relGcsGroup').orderBy('id', 'asc').get()
        }else{
          allItems = GcsUser.query().with('relGcsGroup').orderBy('id', 'asc').get()
        }
        allItems.forEach( item => {
          optionsObj[`${item.nickname}(${that._.defaultTo(that._.get(item, 'relGcsGroup.name'), '暂无')})`] = that._.toString(item.id)
        })
        return converters.objToOptions(optionsObj)
      },
      _recurseSubGroups(group, allGroups, includeSelf){
        let that = this
        let subGroups = allGroups.filter(item => item.parentId === group.id)
        includeSelf && subGroups.push(group)
        subGroups.forEach(subGroup => {
          let children = that._recurseSubGroups(subGroup, allGroups, false)
          subGroups.push(...children)
        })
        return subGroups
      }
    },
  }
</script>
