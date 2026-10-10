<template>
  <div>
    <draggable v-bind="cptDragOptions" :list="productArray" @change="onChange()">
      <div
        class="item-group"
        v-for="(product, oIndex) in productArray"
        :key="oIndex" >
        <!-- 拖拽Icon -->
        <a-icon class="drag drag-widget" type="menu" style="font-size: 20px;"/>
        <!-- 输入组件 -->
        <div class="center flex flex-column text-tips" style="line-height: 28px; margin-left: 16px;">
          <div>
            <div class="text-tips" style="font-style: italic;">
              <b class="text-content" style="font-style: normal;">{{ product['productSeqNum'] }}</b>
              <a-icon :title="product['productDescription']" style="margin: 0 2px 0 8px;" type="file-text"/>
              {{ product['productDescription']| Ellipsis(12) }}
            </div>
          </div>
          <div>
            <a-input-number
              size="small"
              style="width: 60px"
              :disabled="disabled"
              v-model="product.productNum"
              @change="onChange"
              placeholder="数量">
            </a-input-number>
            <span class="text-tips" style="font-size: 12px;">&nbsp;&nbsp;套</span>
            <template>
              <a-input
                size="small"
                style="width: 80px; margin-left: 28px;"
                :disabled="disabled"
                v-model="product.productLot"
                @change="onChange"
                placeholder="批号">
              </a-input>
              <span class="text-tips" style="font-size: 12px;">&nbsp;&nbsp;LOT</span>
            </template>
            <template>
              <a-input-number
                size="small"
                style="width: 80px; margin-left: 28px;"
                :disabled="disabled"
                v-model="product.productPrice"
                @change="onChange"
                placeholder="价格">
              </a-input-number>
              <span class="text-tips" style="font-size: 12px;">&nbsp;&nbsp;元/套</span>
            </template>
          </div>
        </div>
        <!-- 删除Icon -->
        <a-icon
          v-if="!disabled"
          class="trash"
          type="delete"
          @click="onProductDelete(product, oIndex)">
        </a-icon>
      </div>
    </draggable>
    <div v-if="!disabled" class="flex flex-row">
      <GTrmComplete
        style="width: 260px;"
        placeholder="选择产品..."
        showUri="/trmProduct/show"
        indexUri="/trmProduct/index"
        :valueOn="false"
        :queryOn="true"
        queryField="seqNum"
        @change="onProductAdd">
      </GTrmComplete>
    </div>
  </div>
</template>

<!--
# 组件说明
  下拉选项配置组件
# 配置组件
# 操作方法
  $emit('change', objOptions)
-->

<script>
  import { getAction } from '@/api/api'
  import GTrmComplete from './GTrmComplete.vue'

  export default {
    components: {
      GTrmComplete
    },
    /* eslint-disable */
    props: {
      value: { 
        type: Array,
        required: false
      },
      disabled: {
        type: Boolean,
        default: false
      },
      /** 扩展Props */
      priceActive: {
        type: Boolean,
        default: true
      },
    },
    watch: {
      value: function() {
        let that = this
        if (that.value) {
          that.productArray = that._.cloneDeep(that.value) || []
        } else {
          that.productArray = []
        }
      },
    },
    data () {
      return {
        productArray: [] // 此处用model绑定，不用计算属性
      }
    },
    computed: {
      cptDragOptions: function() {
        return {
          animation: 200,
          group: 'products',
          ghostClass: 'ghost',
          handle: this.disabled?'.none':'.drag-widget',
        };
      },
    },
    methods: {
      onChange() {
        let that = this
        let validProductArray = that.productArray.filter(product =>{
          return product.productId && product.productNum
        })
        this.$emit('change', validProductArray)
      },
      /** 扩展方法 */
      onProductAdd(productId){
        let that = this
        getAction(`/trmProduct/show/${productId}`).then(resp => {
          let item = resp.gmodel
          if(item){
            let productExists = that.productArray.findIndex( product => product.productId === item.id)!==-1
            if(!productExists){
              that.productArray.push({
                productId: item['id'],
                productSeqNum: item['seqNum'],
                productDescription: item['description'],
                productNum: 1,
                productLot: '',
                productPrice: undefined,
              })
              that.onChange()
            }else{
              that.$message.warn('请勿重复选择产品');
            }
          }
        })
      },
      onProductDelete(option, oIndex){
        this.productArray.splice(oIndex, 1)
        this.onChange()
      },
    },
  }
</script>

<style lang="scss" scoped>
  /* 选项编辑样式 */
  .item-group{
    display: flex;
    align-items: center;
    border-bottom: 1px solid #e5e5e5;
    /deep/ input, .center {
      flex: auto;
      padding: 0 6px;
      border: 0;
      background-color: transparent;
      transition: all .3s;
      outline: unset;
      box-shadow: unset;
    }
    .drag{
      font-size: 15px;
      cursor: move;
    }
    .trash {
      font-size: 15px;
      &:hover{
        color: #1f1f1f;
        cursor: pointer;
      }
    }
  }
</style>