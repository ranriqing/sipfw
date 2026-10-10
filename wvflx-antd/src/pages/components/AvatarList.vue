<!--
# 组件说明
  页面版本列表
# 配置组件
  prefixCls: String             // Class前缀字符串
  avatarSize: [String, Number]  // 图标大小，数值或字符串 large | small | mini | default
  maxLength: Number             // 最多展示版本数量
  itemsStyle: Object            // 条目样式
# 操作方法
  暂无
-->

<script>
  import Avatar from 'ant-design-vue/es/avatar'
  import AvatarItem from './AvatarItem'

  export default {
    AvatarItem: AvatarItem,
    name: 'AvatarList',
    components: {
      Avatar,
      AvatarItem
    },
    props: {
      prefixCls: {
        type: String,
        default: 'ant-pro-avatar-list'
      },
      avatarSize: {
        type: [String, Number],
        default: 'default' // large | small | mini | default
      },
      maxLength: {
        type: Number,
        default: 0
      },
      itemsStyle: {
        type: Object,
        default: () => {
          return { color: '#f56a00', backgroundColor: '#fde3cf' }
        }
      }
    },
    data() {
      return {}
    },
    methods: {
      getItems(items) {
        let that = this
        let classString = { [`${that.prefixCls}-item`]: true, [`${that.avatarSize}`]: true }
        let itemsLength = items.length
        if (that.maxLength < itemsLength) {
          items = items.slice(0, that.maxLength)
          items.push((<Avatar size={`${that.avatarSize}`} style={ that.itemsStyle }>{`+${itemsLength - that.maxLength}`}</Avatar>))
        }
        return items.map(item => (<li class={ classString }>{ item }</li>))
      }
    },
    render() {
      let that = this
      let { prefixCls, avatarSize } = that.$props
      let classString = {[`${prefixCls}`]: true, [`${avatarSize}`]: true }
      let avatarItems = that.$slots.default
      let avatarItemsDom = avatarItems && avatarItems.length ? <ul class={`${prefixCls}-items`}>{ that.getItems(avatarItems) }</ul> : null
      return (<div class={ classString }> { avatarItemsDom } </div>)
    }
  }
</script>

<style lang="scss" scoped>
  // @import "~ant-design-vue/lib/style/themes/default.less";
  $font-size-base: 14px;
  $avatar-size-base: 32px;
  $avatar-size-lg: 40px;
  $avatar-size-sm: 24px;

  // The prefix to use on all css classes from ant-pro.
  $ant-pro-prefix             : ant-pro;
  $ant-global-header-zindex   : 105;

  .ant-pro-avatar-list{
    display: inline-block;

    ul {
      list-style: none;
      display: inline-block;
      padding: 0;
      margin: 0 0 0 8px;
      font-size: 0;
    }
  }

  .ant-pro-avatar-list-item{
    display: inline-block;
    font-size: $font-size-base;
    margin-left: -8px;
    width: $avatar-size-base;
    height: $avatar-size-base;

    :global {
      .ant-avatar {
        border: 1px solid #fff;
        cursor: pointer;
      }
    }

    &.large {
      width: $avatar-size-lg;
      height: $avatar-size-lg;
    }

    &.small {
      width: $avatar-size-sm;
      height: $avatar-size-sm;
    }

    &.mini {
      width: 20px;
      height: 20px;

      :global {
        .ant-avatar {
          width: 20px;
          height: 20px;
          line-height: 20px;

          .ant-avatar-string {
            font-size: 12px;
            line-height: 18px;
          }
        }
      }
    }
  }
</style>