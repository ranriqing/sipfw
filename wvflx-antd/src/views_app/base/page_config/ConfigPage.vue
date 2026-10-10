<template>
  <div>
    <LabPageSelect>全局配置</LabPageSelect>
    <a-card v-show="!pageVisible" :bordered="false" class="page">
      <a-tabs defaultActiveKey="baseTab" :tabPosition="isMobile?'top':'left'" style="min-height: 360px;">
        <!-- 扩展部分 -->
        <!--
        <div slot="tabBarExtraContent" style="display: inline-block; margin-right: 8px; padding-top: 10px;">
          <span style="margin-left: 8px;"><a-icon type="plus" style="margin-right: 4px;"/>选项组</span>
        </div>
        -->

        <!-- 基本配置Tab -->
        <a-tab-pane tab="基本配置" key="baseTab">
          <a-list
            itemLayout="horizontal"
            :dataSource="cptBasicData">
            <template #renderItem="item, index">
              <a-list-item :key="index">
                <div v-if="item.key==='logo'" style="margin-right: 20px;">
                  <div class="ant-upload-preview" @click="onLogoEdit" >
                    <div class="mask">
                      <a-icon type="cloud-upload-o" />
                    </div>
                    <img :src="_logoUrl()"/>
                  </div>
                </div>
                <template v-if="_.get(item, 'model.key') === GLOBAL.API_SECRET">
                  <a-list-item-meta>
                    <a slot="title" @click="onApiConfigEdit(item.model, item.title)">{{ item.title }}</a>
                    <span slot="description">
                      <span>{{ item.description }}</span>
                      <span> : </span>
                      <span>{{ _.get(item, 'model.value') }}</span>
                    </span>
                  </a-list-item-meta>
                  <a slot="actions" @click="onApiConfigEdit(item.model, item.title)">修改</a>
                </template>
                <template v-else>
                  <a-list-item-meta>
                    <a slot="title" @click="onGlobalConfigEdit(item.model, item.title)">{{ item.title }}</a>
                    <span slot="description">
                      <span>{{ item.description }}</span>
                      <span> : </span>
                      <span>{{ _.get(item, 'model.value') }}</span>
                    </span>
                  </a-list-item-meta>
                  <a slot="actions" @click="onGlobalConfigEdit(item.model, item.title)">修改</a>
                </template>
              </a-list-item>
            </template>
          </a-list>
        </a-tab-pane>

        <!-- 标签维护Tab -->
        <a-tab-pane tab="标签维护" key="tagTab">
          <div style="border-bottom: 1px solid #d3d3d3; padding-bottom: 8px;" class="flex flex-row main-between cross-center">
            <span class="tips" style="font-weight: bold;">{{ tagDataSource.length }}个标签</span>
            <a-button type="dashed" icon="plus" @click="onTagAdd">添加标签</a-button>
          </div>
          <a-list
            itemLayout="horizontal"
            :dataSource="tagDataSource">
            <template #renderItem="item, index">
              <a-list-item :key="index">
                <a-list-item-meta>
                  <div slot="title">
                    <span class="tag-dot" :style="{backgroundColor: item.color || tagColors[0]}"></span>
                    {{ item.name }}
                  </div>
                  <span slot="description">
                    <span>{{ item.usageCount || 0 }} 条记录使用</span>
                  </span>
                </a-list-item-meta>
                <div slot="actions">
                  <a @click="onTagEdit(item)">编辑</a>
                  <a-divider type="vertical" />
                  <a @click="onTagMerge(item)">合并</a>
                  <a-divider type="vertical" />
                  <a-popconfirm :title="`确定删除标签 [${item.name}] ?`" @confirm="() => onTagDelete(item)">
                    <a>删除</a>
                  </a-popconfirm>
                </div>
              </a-list-item>
            </template>
          </a-list>
        </a-tab-pane>

        <!-- 内置选项Tab -->
        <a-tab-pane v-if="cptScInnerData.length>0" tab="内置选项" key="scFloTab">
          <a-list
            itemLayout="horizontal"
            :dataSource="cptScInnerData">
            <template #renderItem="item, index">
              <a-list-item :key="index">
                <a-list-item-meta>
                  <div slot="title">
                    <a @click="onScelEdit(item.model, item.title)">{{ item.title }}</a>
                    <a style="margin-left: 20px;" @click="_copyText(_.get(item, 'model.key'), $event)">
                      <a-icon type="copy"/>
                      {{ _.get(item, 'model.key') }}
                    </a>
                  </div>
                  <span slot="description">
                    <template v-if="_scElements(_.get(item, 'model.id')).length>0">
                      <a-tag
                        v-for="(elem) in _scElements(_.get(item, 'model.id'))"
                        :key="elem.id"
                        style="margin-right: 8px; margin-bottom: 8px;">
                        {{ _.get(elem, 'title') }}
                      </a-tag>
                    </template>
                    <span v-else class="text-tips" style="font-style: italic;">暂无选项</span>
                  </span>
                </a-list-item-meta>
                <a slot="actions" @click="onScelEdit(item.model, item.title)">配置</a>
              </a-list-item>
            </template>
          </a-list>
        </a-tab-pane>

        <a-tab-pane
          v-for="categoryItem in _scElements(_.get(_scConfig('sciScCategory'), 'id'))"
          :tab="categoryItem.title"
          :key="`sciScCategory_${categoryItem.id}`">
          <div style="border-bottom: 1px solid #d3d3d3; padding-bottom: 8px;" class="flex flex-row main-between cross-center">
            <span>
              {{ categoryItem.title }}：<span class="tips" style="font-weight: bold;">{{ categoryScConfigs(categoryItem).length }}个选项</span>
            </span>
            <div>
              <a-button
                style="width: 180px;"
                block
                type="dashed"
                icon="plus"
                @click="onScConfigAdd(categoryItem)">
                添加选项
              </a-button>
              <a-button
                v-if="categoryScConfigs(categoryItem).length===0"
                ghost
                type="danger"
                icon="close"
                style="margin-left: 16px;"
                @click="onCategoryDelete(categoryItem)">
                删除分类
              </a-button>
            </div>
          </div>
          <a-list
            itemLayout="horizontal"
            :dataSource="categoryScConfigs(categoryItem)">
            <template #renderItem="item, index">
              <a-list-item :key="index">
                <a-list-item-meta>
                  <div slot="title">
                    <a @click="onScelEdit(item.model, item.title)">{{ item.title }}</a>
                    <a style="margin-left: 20px;" @click="_copyText(_.get(item, 'model.key'), $event)">
                      <a-icon type="copy"/>
                      {{ _.get(item, 'model.key') }}
                    </a>
                  </div>
                  <span slot="description">
                    <template v-if="_scElements(_.get(item, 'model.id')).length>0">
                      <a-tag
                        v-for="(elem) in _scElements(_.get(item, 'model.id'))"
                        :key="elem.id"
                        style="margin-right: 8px; margin-bottom: 8px;">
                        {{ _.get(elem, 'title') }}
                      </a-tag>
                    </template>
                    <span v-else class="text-tips" style="font-style: italic;">暂无选项</span>
                  </span>
                </a-list-item-meta>
                <div slot="actions">
                  <a @click="onScConfigEdit(item.model)">编辑</a>
                  <a-divider type="vertical" />
                  <a @click="onScelEdit(item.model, item.title)">配置</a>
                </div>
              </a-list-item>
            </template>
          </a-list>
        </a-tab-pane>

      </a-tabs>
    </a-card>

    <!-- 弹窗部分 -->
    <GFormPop
      ref="slotGlobalEdit"
      :popVisible="popGlobalEditVisible"
      :configObj="popConfig.globalConfig">
    </GFormPop>
    <GScelPop
      ref="slotScelEdit"
      :popVisible="popScelEditVisible"
      :configObj="popConfig.scConfig">
    </GScelPop>
    <GFormPop
      ref="slotCateEdit"
      :popVisible="popCateEditVisible"
      :configObj="popConfig.cateConfig">
    </GFormPop>
    <GFormPop
      ref="slotApiEdit"
      :popVisible="popApiEditVisible"
      :configObj="popConfig.apiConfig">
      <template #slot_random="{record, item}">
        <a @click="_randomApiSecret">随机秘钥</a>
      </template>
    </GFormPop>
    <GAvatarPop
      ref="slotLogoAvatar"
      :popVisible="popLogoVisible"
      :configObj="popConfig.logoConfig">
    </GAvatarPop>
    <GFormPop
      ref="slotTagEdit"
      :popVisible="popTagEditVisible"
      :configObj="popConfig.tagConfig">
      <template #slot_color="{record}">
        <a-popover trigger="click" placement="bottom">
          <template slot="content">
            <div class="color-picker">
              <span
                v-for="color in tagColors"
                :key="color"
                class="color-dot"
                :class="{'active': record.color === color}"
                :style="{backgroundColor: color}"
                @click="record.color = color"></span>
            </div>
          </template>
          <a-button class="color-btn" :style="{backgroundColor: record.color || tagColors[0]}"></a-button>
        </a-popover>
      </template>
    </GFormPop>
    <GFormPop
      ref="slotTagMerge"
      :popVisible="popTagMergeVisible"
      :configObj="popConfig.mergeConfig">
      <template #slot_target="{record}">
        <a-select v-model="record.targetId" style="width: 100%;" placeholder="请选择目标标签">
          <a-select-option
            v-for="tag in _mergeTargetTags(record.id)"
            :key="tag.id"
            :value="tag.id">
            <span class="tag-dot" :style="{backgroundColor: tag.color || tagColors[0]}"></span>{{ tag.name }}
          </a-select-option>
        </a-select>
      </template>
    </GFormPop>
  </div>
</template>

<script>
  import ATabs from 'ant-design-vue/lib/tabs';
  import AList from 'ant-design-vue/lib/list';
  import Clipboard from 'clipboard'

  import LabPageSelect from '@/pages/LabPageSelect'
  import GFormPop from '@/components/page_pop/GFormPop'
  import GScelPop from '@/components/page_pop/GScelPop'
  import GAvatarPop from '@/components/page_pop/GAvatarPop'

  import GlobalConfig from '@/store/models/GlobalConfig'
  import ScConfig from '@/store/models/ScConfig'
  import ScElement from '@/store/models/ScElement'

  const settings = require('@/settings')
  import { GLOBAL } from '@/constants'
  import { mapGetters } from "vuex";
  import { GETTERS } from '@/constants'
  import { tools } from '@/utils/util'
  import { postAction, putAction, deleteAction } from '@/api/api'
  import popConfig from './configs/popConfig'


  export default {
    name: "ConfigPage",
    components: {
      ATabs,
      ATabPane: ATabs.TabPane,
      AList,
      AListItem: AList.Item,
      AListItemMeta: AList.Item.Meta,
      LabPageSelect,
      GFormPop,
      GScelPop,
      GAvatarPop,
    },
    data(){
      return {
        /** 配置&状态 */
        GLOBAL: GLOBAL,
        popConfig: popConfig,
        popGlobalEditVisible: false,
        popApiEditVisible: false,
        popScelEditVisible: false,
        popCateEditVisible: false,
        popItemEditVisible: false,
        popLogoVisible: false,
        /** 标签维护 */
        tagColors: ['#f50', '#2db7f5', '#87d068', '#108ee9'],
        tagDataSource: [],
        popTagEditVisible: false,
        popTagMergeVisible: false,
      }
    },
    computed: {
      ...mapGetters({
        /** App state */
        isMobile: GETTERS.CPT_APP_DEVICE_MOBILE,
      }),
      pageVisible: function() {
        return this.popGlobalEditVisible || this.popApiEditVisible || this.popLogoVisible
      },
      cptBasicData() {
        let that = this
        let basicData = [
          {
            key: 'logo', // 标识更改Logo
            title: '系统Logo',
            description: '',
            model: that._globalConfig(GLOBAL.LOGO_TITLE),
          },
        ]
        basicData.push(...[
          {
            title: '登录页(标题)',
            description: '登录页面标题信息',
            model: that._globalConfig(GLOBAL.LOGIN_TITLE),
          },
          {
            title: '登录页(描述)',
            description: '登录页面描述信息',
            model: that._globalConfig(GLOBAL.LOGIN_DESCRIPTION),
          },
          {
            title: '欢迎语(桌面)',
            description: '系统上方欢迎词语',
            model: that._globalConfig(GLOBAL.WELCOME_DESKTOP),
          },
          {
            title: '系统版权',
            description: '系统底部版权',
            model: that._globalConfig(GLOBAL.COPYRIGHT),
          },
          {
            title: '三方对接',
            description: '对接密钥：',
            model: that._globalConfig(GLOBAL.API_SECRET),
          },
        ])
        return basicData
      },
      cptScInnerData() {
        let that = this
        let itemArray = []
        itemArray.push({
          title: 'TAB大类',
          description: 'TAB大类下拉项',
          allowRemove: false,
          model: that._scConfig('sciScCategory'),
        })
        itemArray.push({
          title: '流程分类',
          description: '流程分类下拉项',
          model: that._scConfig('sciFloCategory'),
        })
        itemArray.push({
          title: '数据表分类',
          description: '数据表分类下拉项',
          model: that._scConfig('sciDomainCategory'),
        })
        itemArray.push({
          title: '活动类型',
          description: '活动类型下拉项',
          model: that._scConfig('sciScheduleType'),
        })
        itemArray.push({
          title: '演示多级',
          description: '演示多级下拉项',
          model: that._scConfig('sciMultiLevel'),
        })
        itemArray.push({
          title: '设备类型',
          description: '设备类型下拉项',
          model: that._scConfig('sciDeviceType'),
        })
        return itemArray
      },
    },
    mounted(){
      let that = this
      that.doTagSearch()
    },
    methods:{
      /** 弹窗操作 */
      onGlobalConfigEdit(record, title) {
        let that = this
        let slotRef = that.$refs.slotGlobalEdit
        that.popGlobalEditVisible = slotRef.isPage()
        slotRef.showPop(record, {'_title': `配置 - ${title}`},
          (record, context) => { /* submit */
            context || true
            let promise = that._saveOrUpdateGlobalConfig(record)
            promise.then(() => that.popGlobalEditVisible = false)
            return promise
          }, () => { /* cancel */
            that.popGlobalEditVisible = false
          }
        )
      },
      onScelEdit(scConfig, title, allowRemove){
        let that = this
        let slotRef = that.$refs.slotScelEdit
        that.popScelEditVisible = slotRef.isPage()
        slotRef.showPop(scConfig, {'_title': `选项配置 - ${title}`, allowRemove: allowRemove},
          (record, context) => { /* submit */
            context || true
            return Promise.resolve()
          }, () => { /* cancel */
            that.popScelEditVisible = false
          }
        )
      },
      onApiConfigEdit(record, title) {
        let that = this
        let slotRef = that.$refs.slotApiEdit
        that.popApiEditVisible = slotRef.isPage()
        slotRef.showPop(record, {'_title': `配置 - ${title}`},
          (record, context) => { /* submit */
            context || true
            let promise = that._saveOrUpdateGlobalConfig(record)
            promise.then(() => that.popApiEditVisible = false)
            return promise
          }, () => { /* cancel */
            that.popApiEditVisible = false
          }
        )
      },
      onLogoEdit() {
        let that = this
        let slotRef = that.$refs.slotLogoAvatar
        that.popLogoVisible = slotRef.isPage()
        let logoRecord = that._globalConfig(GLOBAL.LOGO_IMAGE)
        slotRef.showPop({}, {_avatarUrl: that._logoUrl(), _avatarName: logoRecord['value']},
          (record, context) => { /* submit */
            logoRecord['value'] = context['_avatarName']
            let promise = that._saveOrUpdateGlobalConfig(logoRecord)
            promise.then(() =>{
              that.$message.success('更新Logo成功')
              that.popLogoVisible = false
            })
            return promise
          }, () => { /* cancel */
            that.popLogoVisible = false
          }
        )
      },
      onCategoryDelete(categoryItem){
        let that = this
        that.$confirm({
          title: '确认删除',
          content: `确定要选项分类 [${categoryItem.title}] ?`,
          onOk() {
            return ScElement.$delete({
              params: {
                id: categoryItem.id,
              }
            })
          }
        });
      },
      categoryScConfigs(categoryItem) {
        let scConfigs = ScConfig.query().where('sciScCategory', categoryItem.id)
          .orderBy('seqOrder', 'asc').orderBy('id', 'desc').get();
        return scConfigs.map(scConfig => {
          return {
            title: scConfig.title || scConfig.key,
            description: scConfig.remark,
            model: scConfig,
          }
        })
      },
      onScConfigAdd(categoryItem) {
        let that = this
        let slotRef = that.$refs.slotCateEdit
        that.popCateEditVisible = slotRef.isPage()
        slotRef.showPop({sciScCategory: categoryItem.id}, {'_title': `添加选项`},
          (record, context) => { /* submit */
            context || true
            let promise = ScConfig.$create({
              data: {
                title: record.title,
                key: record.key,
                sciScCategory: record.sciScCategory,
                remark: record.remark,
                freeCreation: false,
              }
            })
            promise.then(() => that.popCateEditVisible = false)
            return promise
          }, () => { /* cancel */
            that.popCateEditVisible = false
          }
        )
      },
      onScConfigEdit(scConfig) {
        let that = this
        let slotRef = that.$refs.slotCateEdit
        that.popCateEditVisible = slotRef.isPage()
        slotRef.showPop(scConfig, {'_title': `编辑 - ${scConfig.title}`},
          (record, context) => { /* submit */
            context || true
            let promise = ScConfig.$update({
              params: {
                id: scConfig.id
              },
              data: {
                title: record.title,
                key: record.key,
                sciScCategory: record.sciScCategory,
                remark: record.remark,
                freeCreation: false,
              }
            })
            promise.then(() => that.popCateEditVisible = false)
            return promise
          }, () => { /* cancel */
            that.popCateEditVisible = false
          }
        )
      },
      /** 工具方法 */
      _copyText(copyText, e){
        let that = this
        let target = e.target
        if(e.target.tagName === 'svg'){
          target = e.target.parentElement
        }
        let clipboard = new Clipboard(target, {
          text: () => copyText
        })
        clipboard.on('success', () => {
          that.$message.success(`拷贝成功`);
          clipboard.destroy()
        })
        clipboard.on('error', () => {
          clipboard.destroy()
        })
        clipboard.onClick(e)
      },
      _logoUrl(){
        let that = this
        let record = that._globalConfig(GLOBAL.LOGO_IMAGE)
        return tools.avatarUrl(record, 'value')
      },
      _randomApiSecret(){
        let that = this
        let apiSecret = that._globalConfig('apiSecret')
        apiSecret['value'] = tools.randomUuid()
        that.onApiConfigEdit(apiSecret, '对接秘钥')
      },
      _saveOrUpdateGlobalConfig(record){
        let promise = GlobalConfig.$update({
          params: {
            id: undefined
          },
          data: {
            key: record.key,
            value: record.value,
          }
        })
        return promise
      },
      _globalConfig(itemKey, defaultValue){
        let that = this
        let dftConfig = settings.DFT_GLOBAL_CONFIG
        let configItem = GlobalConfig.query().where('key', itemKey).orderBy('id', 'desc').first()
        defaultValue = defaultValue || that._.get(dftConfig, itemKey)
        return {
          id: that._.get(configItem, 'id'),
          key: itemKey,
          value: that._.defaultTo(that._.get(configItem, 'value'), defaultValue),
        }
      },
      _scConfig(itemKey){
        let scConfig = ScConfig.query().where('key', itemKey).orderBy('id', 'desc').first();
        if(!scConfig){
          ScConfig.$create({
            data: { key: itemKey, }
          })
        }
        return scConfig
      },
      _scElements(scConfigId){
        let scElements = ScElement.query()
          .where(item => item.parentId === 0)
          .where(item => item.scConfigId === scConfigId)
          .orderBy('seqOrder').orderBy('id').get()
        return scElements;
      },
      /** 标签维护 */
      doTagSearch(){
        let that = this
        return postAction('/gcsTag/index', {
          pageNum: 1,
          pageSize: 999,
          orderBy: null,
          condition: 'and',
          rules: []
        }).then(resp => {
          let entityList = that._.get(resp, 'gmodel.entityList') || []
          entityList.sort((a, b) => (a.name || '').localeCompare(b.name || ''))
          that.tagDataSource = entityList
        }).catch(() => {
          that.tagDataSource = []
        })
      },
      onTagAdd(){
        let that = this
        let slotRef = that.$refs.slotTagEdit
        that.popTagEditVisible = slotRef.isPage()
        slotRef.showPop({color: that.tagColors[0]}, {'_title': '添加标签'},
          (record, context) => { /* submit */
            context || true
            let promise = postAction('/gcsTag/save', { name: record.name, color: record.color })
            promise.then(() => { that.popTagEditVisible = false; that.doTagSearch() })
            promise.catch(err => { that.$message.error(that._.get(err, 'message') || '保存失败') })
            return promise
          }, () => { /* cancel */
            that.popTagEditVisible = false
          }
        )
      },
      onTagEdit(record){
        let that = this
        let slotRef = that.$refs.slotTagEdit
        that.popTagEditVisible = slotRef.isPage()
        slotRef.showPop(record, {'_title': `编辑标签 - ${record.name}`},
          (record, context) => { /* submit */
            context || true
            let promise = putAction('/gcsTag/update', record.id, { name: record.name, color: record.color })
            promise.then(() => { that.popTagEditVisible = false; that.doTagSearch() })
            promise.catch(err => { that.$message.error(that._.get(err, 'message') || '更新失败') })
            return promise
          }, () => { /* cancel */
            that.popTagEditVisible = false
          }
        )
      },
      onTagMerge(record){
        let that = this
        let slotRef = that.$refs.slotTagMerge
        that.popTagMergeVisible = slotRef.isPage()
        slotRef.showPop(record, {'_title': `合并标签 - ${record.name}`},
          (record, context) => { /* submit */
            context || true
            if(!record.targetId){
              that.$message.warn('请选择目标标签')
              return Promise.reject()
            }
            let promise = postAction('/gcsTag/merge', { sourceId: record.id, targetId: record.targetId })
            promise.then(() => { that.popTagMergeVisible = false; that.doTagSearch() })
            return promise
          }, () => { /* cancel */
            that.popTagMergeVisible = false
          }
        )
      },
      onTagDelete(record){
        let that = this
        let promise = deleteAction('/gcsTag/delete', record.id)
        promise.then(() => { that.$message.success('删除成功'); that.doTagSearch() })
        return promise
      },
      _mergeTargetTags(sourceId){
        return this.tagDataSource.filter(tag => tag.id !== sourceId)
      },
    },
  }
</script>

<style lang="scss" scoped>
  /* Logo展示 */
  .ant-upload-preview {
    position: relative;
    width: 64px;
    border-radius: 50%;
    box-shadow: 0 0 4px #ccc;
    // Image、Mask组件
    img, .mask {
      width: 100%;
      height: 100%;
      border-radius: 50%;
      overflow: hidden;
    }
    // Mask独有样式
    .mask {
      opacity: 0;
      position: absolute;
      background: rgba(0,0,0,0.4);
      cursor: pointer;
      transition: opacity 0.4s;
      &:hover {
        opacity: 1;
      }
      i {
        font-size: 2rem;
        position: absolute;
        top: 50%;
        left: 50%;
        margin-left: -1rem;
        margin-top: -1rem;
        color: #d6d6d6;
      }
    }
  }
  /* 标签维护 */
  .tag-dot {
    display: inline-block;
    width: 10px;
    height: 10px;
    border-radius: 50%;
    margin-right: 6px;
  }
  .color-picker {
    display: flex;
    flex-direction: row;
    flex-wrap: wrap;
    gap: 8px;
    max-width: 176px;
    .color-dot {
      width: 22px;
      height: 22px;
      border-radius: 50%;
      cursor: pointer;
      border: 1px solid rgba(0, 0, 0, .12);
      box-sizing: border-box;
      transition: transform .15s;
      &:hover {
        transform: scale(1.15);
      }
      &.active {
        box-shadow: 0 0 0 2px #ffffff, 0 0 0 3px #333333;
      }
    }
  }
  .color-btn {
    width: 28px;
    height: 28px;
    padding: 0;
    border-radius: 4px;
    border: 1px solid rgba(0, 0, 0, .15);
  }
</style>
