<template>
  <div class="overview-wrapper">
    <div class="main-card">
      <div class="main-title">{{ overviewSumTitle }}</div>
      <div class="main-value">{{ mainTotal }}<span class="unit">{{ overviewUnit }}</span></div>
    </div>
    <a-divider class="overview-divider" type="vertical" />
    <div class="sub-wrapper">
      <div
        v-for="(row, index) in dataSource"
        :key="index"
        class="sub-card">
        <div class="sub-title">{{ row[groupField] }}</div>
        <div class="sub-body">
          <a-upload
            v-if="overviewShowImage && cptIsLab"
            :showUploadList="false"
            accept="image/*"
            :customRequest="(opts) => handleImageUpload(row, opts)">
            <img class="sub-image" :src="_subImageUrl(row)" />
          </a-upload>
          <img v-else-if="overviewShowImage" class="sub-image" :src="_subImageUrl(row)" />
          <div class="sub-value">{{ row[measureField] }}<span class="unit">{{ overviewUnit }}</span></div>
        </div>
      </div>
    </div>
  </div>
</template>

<!--
# 组件说明
  概览展示组件(PartChart 的 overview 类型),渲染左侧「合计」大数字 + 右侧分类子卡片;
  纯展示组件,数据由 PartChart 查询后传入,不做数据请求;
  overviewShowImage 开启时,子卡片数字前展示图片(无配置显示 none_pic.png);
  Lab 模式下点击图片上传替换,并通过 changeImage 事件交给父组件保存;
# 配置组件
  dataSource: Array      // 聚合后的行数组 [{ [groupField]: 分类显示名, [measureField]: 数值 }, ...]
  chartGroup: Object     // 子卡片分类维度(取 field 作标签键)
  chartMeasure: Object   // 聚合数值(取 field 作数值键)
  overviewSumTitle: String  // 主数字标签,默认「合计」
  overviewUnit: String      // 单位,默认「个」
  overviewShowImage: Boolean // 是否展示图片,默认 false
  overviewImageMap: Object   // 分类显示名 -> fileUuid
# 操作方法
  $emit('changeImage', { categoryName, fileUuid })
-->

<script>
  import AUpload from 'ant-design-vue/lib/upload';

  import { stores } from '@/utils/util'
  import { uploadFile } from '@/api/api'

  const settings = require('@/settings')
  const nonePic = require('@/assets/images/none_pic.png')

  export default {
    name: "OverviewPlot",
    components: {
      AUpload,
    },
    props: {
      dataSource: {
        type: Array,
        required: true,
      },
      chartGroup: {
        type: Object,
        required: true,
      },
      chartMeasure: {
        type: Object,
        required: true,
      },
      overviewSumTitle: {
        type: String,
        required: false,
        default: '合计',
      },
      overviewUnit: {
        type: String,
        required: false,
        default: '个',
      },
      overviewShowImage: {
        type: Boolean,
        required: false,
        default: false,
      },
      overviewImageMap: {
        type: Object,
        required: false,
        default: function(){
          return {}
        }
      },
    },
    data() {
      return {
        baseUrl: settings.BASE_URL,
        nonePic: nonePic,
      };
    },
    computed: {
      cptIsLab: function() {
        return stores.isLabState()
      },
      groupField: function() {
        return this._.get(this.chartGroup, 'field')
      },
      measureField: function() {
        return this._.get(this.chartMeasure, 'field')
      },
      mainTotal: function() {
        return this._.sumBy(this.dataSource, this.measureField)
      },
    },
    methods: {
      /** 子卡片图片URL */
      _subImageUrl(row) {
        let that = this
        let fileUuid = that._.get(that.overviewImageMap, row[that.groupField])
        if (fileUuid) {
          return `${that.baseUrl}/gcsStorage/downloadFile/${fileUuid}?inline=true`
        }
        return that.nonePic
      },
      /** 上传图片(仅Lab模式),成功后交给父组件保存 */
      handleImageUpload(row, { onSuccess, onError, file }) {
        let that = this
        let formData = new FormData()
        formData.set('upload_file', file)
        uploadFile(formData).then(resp => {
          onSuccess(resp, file)
          let fileUuid = that._.get(resp, 'gmodel[0].fileUuid')
          if (fileUuid) {
            that.$emit('changeImage', { categoryName: row[that.groupField], fileUuid })
          }
        }).catch(err => onError(err))
      },
    },
  }
</script>

<style lang="scss" scoped>
  .overview-wrapper{
    display: flex;
    flex-direction: row;
    justify-content: flex-start;
    align-items: center;
    flex-wrap: wrap;
    .main-card{
      .main-title{
        color: rgba(0, 0, 0, .45);
        font-size: 16px;
        line-height: 22px;
      }
      .main-value{
        color: #000;
        font-size: 40px;
        font-weight: 500;
        margin-left: 20px;
        line-height: 1.2;
        .unit{
          font-size: 16px;
          font-weight: normal;
          margin-left: 8px;
        }
      }
    }
    .overview-divider{
      height: 40px;
      width: 2px;
      margin: 0 26px;
    }
    .sub-wrapper{
      display: flex;
      flex-direction: row;
      justify-content: flex-start;
      align-items: center;
      flex-wrap: wrap;
      .sub-card{
        margin-left: 40px;
        &:first-child{
          margin-left: 0;
        }
        .sub-title{
          color: rgba(0, 0, 0, .45);
          font-size: 16px;
          line-height: 22px;
        }
        .sub-body{
          display: flex;
          flex-direction: row;
          justify-content: flex-start;
          align-items: center;
          .sub-image{
            height: 42px;
            object-fit: contain;
            margin-right: 20px;
            cursor: pointer;
          }
          .sub-value{
            color: #000;
            font-size: 30px;
            margin-top: 4px;
            line-height: 1.2;
            .unit{
              font-size: 16px;
              margin-left: 8px;
            }
          }
        }
      }
    }
  }
</style>
