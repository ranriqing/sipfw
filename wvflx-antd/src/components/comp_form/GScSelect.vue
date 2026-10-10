<template>
  <div class="sc-select">
    <a-select
      style="width: 100%;"
      optionFilterProp="children"
      :disabled="disabled"
      :mode="multiple?'multiple':'default'"
      :allowClear="allowClear"
      :showSearch="true"
      :placeholder="placeholder"
      :value="cptValue"
      @change="onChange($event)">
      <template v-if="_.get(cptScConfig, 'freeCreation')" slot="notFoundContent">
        {{ keyword?`无符合'${keyword}'的选项`:'暂无选项' }}
        <a style="float: right" @click="onAddOption">添加</a>
      </template>
      <a-select-option v-for="(item) in cptScOptions" :key="item.value" class="sc-select-item">
        {{ item.title }} - {{ item.value }}
        <a-tag v-if="!item.tagStyle" class="sc-tag" style="border: none;">
          {{ item.title }}
        </a-tag>
        <a-tag v-else-if="item.tagStyle==='default'" class="sc-tag" style="border: none;">
          {{ item.title }}
        </a-tag>
        <a-tag v-else :color="item.tagStyle" class="sc-tag">
          {{ item.title }}
        </a-tag>
      </a-select-option>
    </a-select>
  </div>
</template>

<script>
import { tools } from '@/utils/util'

import ScConfig from '@/store/models/ScConfig'
import ScElement from '@/store/models/ScElement'

export default {
  name: 'GScSelect',
  components: {
  },
  /* eslint-disable */
  props: {
    sContext: {
      type: Object,
      default: function() {
        return {}
      }
    },
    value: {
      type: [String, Number],
      required: false
    },
    disabled: {
      type: Boolean,
      default: false
    },
    multiple: {
      type: Boolean,
      default: false
    },
    allowClear: {
      type: Boolean,
      default: true
    },
    placeholder: {
      type: String,
      default: '请选择'
    },
    /** 扩展Props */
    scKey: {
      type: String,
      required: true
    },
  },
  data() {
    return {
      toAddOption: null,
      keyword: null,
    }
  },
  computed: {
    cptValue: function() {
      let that = this
      if (that._.isNil(that.value)) {
        return undefined
      }else if(that.multiple){
        return that.value
      } else {
        return that._.toString(that.value) === '' ? undefined : that._.toString(that.value)
      }
    },
    cptScConfig: function() {
      return ScConfig.query().where('key', this.scKey).first()
    },
    cptScOptions: function() {
      let that = this
      let scElements = ScElement.query()
        .where(item => item.scConfigKey === that.scKey)
        .orderBy('seqOrder').orderBy('id').get()
      // 获取root节点
      let rootElements = []
      scElements.forEach(function(item) {
        if (item.parentId === 0) {
          rootElements.push({
            id: item['id'],
            pId: item['parentId'],
            key: `elem${item['id']}`,
            title: item['title'],
            value: that._.toString(item['id']),
            tagStyle: item['tagStyle'],
            remark: item['description'],
          })
        }
      })
      return _.uniqBy(rootElements, 'value');
    }
  },
  methods: {
    onChange(value) {
      this.$emit('change', value)
    },
    /** 扩展方法 */
    onAddOption() {
      let that = this
      that.$confirm({
        title: `确定添加选项?`,
        content: function(h) {
          h && true
          let vnodes = []
          let initOptionValue = that._.trim(that.keyword)
          that.toAddOption = initOptionValue
          let props = { value: initOptionValue }
          vnodes.push(<a-input {...{ props }}
            placeholder="选项值" onChange={e => that.toAddOption = e.target.value}
          />)
          return (<div>{vnodes}</div>)
        },
        onOk() {
          let scElements = ScElement.query()
            .where(item => item.scConfigKey === that.scKey).get()
          let maxSeqOrder = scElements.length>0?Math.max(...scElements.map(item => item['seqOrder'])):0
          let promise = ScElement.$create({
            data: {
              title: that._.trim(that.toAddOption),
              seqOrder: maxSeqOrder+1,
              dfault: false,
              tagStyle: undefined,
              description: undefined,
              scConfigId: that.cptScConfig?.id
            }
          })
          promise.then(resp => {
            that.onChange(that._.toString(resp['id']))
            that.$message.success('选项添加成功');
          }).catch(err => {
            err || true
            that.$message.warn('选项添加失败')
          })
          return promise
        }
      });
    },
  },
  mounted() {
    let that = this
    tools.sleep(1000).then(() => {
      // 不存在则创建
      if (that.scKey && !that.cptScConfig) {
        ScConfig.$create({
          data: { key: that.scKey, }
        })
      }
    })
  }
}
</script>

<style lang="scss" scoped>
.sc-select{
  /deep/ .ant-select{
    .ant-select-selection-selected-value{
      font-size: 0;
      margin-top: 4px;
    }
  }
}
</style>