<template>
  <div v-if="cptGcsUser">
    <div class="avatar-user flex flex-row">
      <div style="position: relative">
        <a-avatar
          :size="size"
          shape="circle"
          style="cursor: pointer; border: 1px solid #e6e9f0; filter: grayscale(5%)"
          @click="onViewAvatar(cptGcsUser)"
          :src="cptGcsUser['wxHeadImgUrl'] || tools.avatarUrl(cptGcsUser, 'avatar')">
        </a-avatar>
        <div v-if="subIcon" :class="['sub-icon', subIconType]">
          <a-icon style="font-size: 14px; color: #fff;" :type="subIcon" theme="filled"/>
        </div>
      </div>
      <div v-if="showDetail" style="margin-left: 8px;">
        <div>{{ cptGcsUser['nickname'] }}</div>
        <div class="text-tips" style="font-style: italic;">{{ cptGcsUser['mobile'] }}</div>
      </div>
    </div>
    <a-modal :visible="avatarVisible" :footer="null" @cancel="avatarVisible=false">
      <img alt="avatar" style="width: 100%" :src="avatarUrl" />
    </a-modal>
  </div>
</template>

<script>
  import AAvatar from 'ant-design-vue/lib/avatar';

  import { tools } from '@/utils/util'
  import GcsUser from '@/store/models/GcsUser'

  export default {
    name: 'AvatarUser',
    components: {
      AAvatar,
    },
    props: {
      userId: {
        type: Number,
        required: true,
      },
      size: {
        type: Number,
        required: false,
        default: function(){
          return 40
        }
      },
      subIcon: {
        type: String,
        required: false,
        default: undefined
      },
      subIconType: {
        type: String,
        required: false,
        default: 'success-dark'
      },
      showDetail: {
        type: Boolean,
        default: true,
      },
    },
    data () {
      return {
        /** 配置&状态 */
        tools: tools,
        /** 头像Modal */
        avatarVisible: false,
        avatarUrl: ''
      }
    },
    computed: {
      cptGcsUser: function() {
        let that = this
        return GcsUser.find(that.userId)
      }
    },
    methods: {
      onViewAvatar(cptGcsUser){
        this.avatarUrl = cptGcsUser['wxHeadImgUrl'] || tools.avatarUrl(cptGcsUser, 'avatar')
        this.avatarVisible = true
      },
    }
  }
</script>

<style lang="scss" scoped>
  .avatar-user{
    line-height: 20px;
    .sub-icon{
      display: flex;
      flex-direction: row;
      align-items: center;
      justify-content: center;
      // 位置信息
      position: absolute;
      bottom: -5px;
      right: -5px;
      border-radius: 50%;
      width: 24px;
      height: 24px;
      border: 2px solid #fff;
      background-color: #18b566;
      &.info, &.info-dark{
        background-color: #82848a;
      }
      &.success, &.success-dark{
        background-color: #18b566;
      }
      &.warning, &.warning-dark{
        background-color: #f29100;
      }
      &.error, &.error-dark{
        background-color: #dd6161;
      }

    }
  }
</style>