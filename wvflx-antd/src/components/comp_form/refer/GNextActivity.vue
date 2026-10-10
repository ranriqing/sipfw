<template>
  <div>
    <div v-if="readonly">
      <template v-if="cptNextActivity">
        <div class="flex flex-row">
          <a-icon
            style="font-size: 20px; margin-right: 6px;"
            :class="activity.dateStrAndClass(cptNextActivity.dueDate)[1]"
            :type="activity.typeIcon(cptNextActivity.sciScheduleType)"
            theme="filled"/>
          <div class="activity-title">
            <span v-if="cptNextActivity.title">
              {{ cptNextActivity.title | Ellipsis(15) }}
            </span>
            <span v-else>
              {{ _.get(stores.scElement('sciScheduleType', _.get(cptNextActivity, 'sciScheduleType')), 'title') }}
            </span>
          </div>
        </div>
        <!-- 截至日 -->
        <div>
          截止：
          <span
            :title="cptNextActivity.dueDate"
            :class="['due-date', activity.dateStrAndClass(cptNextActivity.dueDate)[1]]">
            {{ activity.dateStrAndClass(cptNextActivity.dueDate)[0] }}
          </span>
        </div>

      </template>
    </div>
    <div v-else>
      None
    </div>
  </div>
</template>

<script>
  const settings = require('@/settings')
  import { stores, activity } from '@/utils/util'

  export default {
    name: 'GNextActivity',
    components: {
    },
    /* eslint-disable */
    props: {
      item: {
        type: Object,
        required: true
      },
      value: {
        type: Array,
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
      /** 扩展Props */
      record: {
        type: Object,
        required: false
      },
    },
    data(){
      return {
        /** 配置&状态 */
        baseUrl: settings.BASE_URL,
        loading:false,
        stores: stores,
        activity: activity,
      }
    },
    computed: {
      cptNextActivity: function () {
        let that = this
        return that._.get(that.record, 'nextActivity')
      },
    },
    methods: {
      /** 工具方法 */
    },
  }
</script>

<style lang="scss" scoped>

</style>
