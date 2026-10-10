<template>
  <div>
    <div style="padding: 0 0 5px 0;">
      <div
        class="user-wrapper"
        v-for="(userItem, index) in selectedUsers.slice(0, cptShowLimit)"
        :key="index">
        <AvatarUser
          class="box-avatar"
          :userId="userItem.id"
          :showDetail="true"/>
        <div v-if="!(readonly || disabled)" class="remove warning" @click="removeUser(userItem)"><a-icon type="close-circle"/></div>
      </div>
      <span v-if="selectedUsers.length>cptShowLimit" style="margin-left: 8px;" class="text-tips">等共{{ selectedUsers.length }}个用户</span>
      <a v-if="!(readonly || disabled)" class="domain-avatar" @click="selectUser">
        <AAvatar class="avatar-plus" size="large" shape="circle" icon="plus"/>
      </a>
    </div>
  </div>
</template>

<!--
# 组件说明
  选择用户组件，选择多个关联用户
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
  import { tools } from '@/utils/util'

  import AvatarUser from "@/components/comp_tools/AvatarUser.vue";
  import { vPopUtil } from '@/utilsv/vUtil'
  import AAvatar from 'ant-design-vue/lib/avatar';

  export default {
    name: 'GLinkUser',
    components: {
      AAvatar,
      AvatarUser,
    },
    /* eslint-disable */
    props: {
      item: {
        type: Object,
        required: true
      },
      value: {
        type: [Array],
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
        default: '请选择用户'
      },
    },
    data () {
      return {
        /** 配置&状态 */
        tools: tools,
        selectedUsers: [],
        showLimit: 3,
      }
    },
    watch: {
      // [Watch] 回显记录
      value: {
        immediate: true,
        handler(newValue) {
          let that = this
          if (!newValue) {
            that.selectedUsers = []
          } else if (that._.isArray(newValue)) {
            let noneNil = newValue.find(item => !that._.isNil(item))
            that.selectedUsers = noneNil?newValue:[]
          }
        }
      }
    },
    computed: {
      ...mapGetters({
        currentUser: GETTERS.GET_USER_CURRENT_USER,
      }),
      cptShowLimit: function(){
        let that = this
        return that._.defaultTo(that._.get(that.item,'extend.showLimit'), that.showLimit)
      },
    },
    methods: {
      onChange (value) {
        this.$emit('change', value)
      },
      /** 扩展方法 */
      selectUser(){
        let that = this
        vPopUtil.selectTransferUser(
          that, {}, {multiple: true},
          (record, context) => {
            /* submit */
            context || true
            let noRepeatUsers = (record || []).filter(user =>
              that.selectedUsers.findIndex( item => item.username === user.username) === -1
            )
            that.selectedUsers.push(...noRepeatUsers)
            that.onChange(that.selectedUsers)
            return Promise.resolve()
          }, () => {
            /* cancel */
          })
      },
      removeUser(user){
        let that = this
        let recordIndex = that.selectedUsers.findIndex( item => item.username === user.username)
        if(recordIndex !== -1){
          that.selectedUsers.splice(recordIndex, 1)
          that.onChange(that.selectedUsers)
        }
      },
    },
  }
</script>

<style lang="scss" scoped>
.user-wrapper{
  display: inline-block;
  margin-left: 8px;
  position: relative;
  padding-right: 15px;
  .remove{
    cursor: pointer;
    opacity: 0;
    position: absolute;
    top: 2px;
    right: 4px;
    line-height: 1;
  }
  &:hover {
    // background-color: #fafafa;
    .remove {
      opacity: 1;
    }
  }
}

.avatar-plus{
  display: inline-flex !important;
  align-items: center;
  justify-content: center;
  color: #ccc;
  background-color: transparent;
  border: 2px dashed #ccc;
  margin-left: 10px;
  &:hover{
    color: #ddd;
    border: 2px dashed #ddd;
  }
}
</style>