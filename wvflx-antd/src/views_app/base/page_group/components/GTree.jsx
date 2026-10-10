import Menu from 'ant-design-vue/lib/menu';
import Input from 'ant-design-vue/lib/input';
import Icon from 'ant-design-vue/lib/icon';
const { Item, ItemGroup, SubMenu } = Menu
const { Search } = Input

/**
 支持多级部门
 [{
  'key': 'key-01',
  'title': '研发中心',
  'icon': 'mail',
  'children': [{
    'key': 'key-01-01',
    'title': '后端组',
    'icon': null,
    'group': true,
    children: [{
        'key': 'key-01-01-01',
        'title': 'JAVA',
        'icon': null
      },
      {
        'key': 'key-01-01-03',
        'title': 'Golang',
        'icon': null
      }
    ]
  }, {
    'key': 'key-01-02',
    'title': '前端组',
    'icon': null,
    'group': true,
    children: [{
        'key': 'key-01-02-01',
        'title': 'React',
        'icon': null
      },
      {
        'key': 'key-01-02-02',
        'title': 'Vue',
        'icon': null
      },
    ]
  }]
 },
 {
  'key': 'key-02',
  'title': '财务部',
  'icon': 'dollar',
  'children': [{
    'key': 'key-02-01',
    'title': '会计核算',
    'icon': null
  }, {
    'key': 'key-02-02',
    'title': '成本控制',
    'icon': null
  }]
 }]
*/

export default {
  name: 'GTree',
  props: {
    dataSource: {
      type: Array,
      required: true
    },
    openKeys: {
      type: Array,
      default: function(){
        return []
      }
    },
    search: {
      type: Boolean,
      default: false
    }
  },
  data () {
    return {
      selectedKeys: [],
      localOpenKeys: []
    }
  },
  methods: {
    setSelectKey(selectedKeys){
      this.selectedKeys = selectedKeys
    },
    /** Item 操作 */
    handleAdd(pItem) {
      this.$emit('itemAdd', pItem)
    },
    handleEdit (item) {
      this.$emit('itemEdit', item)
    },
    handleDelete (item) {
      this.$emit('itemDelete', item)
    },
    /** Item 渲染 */
    renderItem (item) {
      return item.children ? this.renderChildrenItem(item) : this.renderLeafItem(item)
    },
    renderLeafItem(item) {
      return (
        <Item key={item.key}>
          { this._renderIcon(item.icon) }
          { item.title }
          <div class="btn">
            <a
              style="width: 20px;"
              {...{ on: { click: () => this.handleEdit(item) } }}>
              <a-icon type="edit"/>
            </a>
            <a
              style="width: 20px;"
              {...{ on: { click: () => this.handleDelete(item) } }}>
              <a-icon type="delete"/>
            </a>
          </div>
        </Item>
      )
    },
    renderChildrenItem (item) {
      let that = this
      const childrenItems = item.children && item.children.map(cItem => {
        return this.renderItem(cItem)
      })
      // 子组件
      if (item.group) {
        // Group子组件
        return (
          <ItemGroup key={item.key}>
            <template slot="title">
              <span>{ item.title }</span>
              <a-dropdown>
                <a class="btn"><a-icon type="ellipsis" /></a>
                <a-menu slot="overlay">
                  <a-menu-item key="1">编辑</a-menu-item>
                  <a-menu-item key="2">移除</a-menu-item>
                </a-menu>
              </a-dropdown>
            </template>
            { childrenItems }
          </ItemGroup>
        )
      }else{
        // SubMenu子组件
        let title = (
          <span slot="title">
            { this._renderIcon(item.icon) }
            <span>{ item.title }</span>
            <a-dropdown>
              <a class="btn">
                <a-icon type="ellipsis"/>
              </a>
              <a-menu
                slot="overlay"
                {...{on: {
                  click: function(menuItem){
                    if(menuItem.key === 'add'){
                      that.handleAdd(item)
                    }else if(menuItem.key === 'edit'){
                      that.handleEdit(item)
                    }else if(menuItem.key === 'delete'){
                      that.handleDelete(item)
                    }
                  },
                }}}>
                <a-menu-item key="add">添加</a-menu-item>
                <a-menu-item key="edit">编辑</a-menu-item>
                {item.children.length>0?null:<a-menu-item key="delete">删除</a-menu-item>}
              </a-menu>
            </a-dropdown>
          </span>
        )
        return (
          // titleClick={this.$emit('titleClick', item)}
          <SubMenu
            {...{on: {
              titleClick: function(item){
                that.selectedKeys=[item.key]
                that.$emit('itemSelect', item.key )
              },
            }}}
            key={item.key}
            >
            { title }
            { childrenItems }
          </SubMenu>
        )  
      }
    },
    /** 辅助方法 */
    _renderSearch () {
      return (
        <Search
          placeholder="请输入 ..."
          style="width: 100%;"
        />
      )
    },
    _renderIcon (icon) {
      return icon && (<Icon type={icon} />) || null
    },
  },
  render () {
    let that = this
    const { dataSource, search } = this.$props
    // 渲染全部元素
    const list = dataSource.map(item => {
      return this.renderItem(item)
    })
    return (
      <div class="tree-wrapper">
        { search ? this._renderSearch() : null }
        <Menu
          mode="inline"
          class="custom-tree"
          {...{on: {
            click: function(item){
              that.selectedKeys=[item.key]
              that.$emit('itemSelect', item.key )
            },
            openChange: function(openKeys){
              openKeys || true
              // that.selectedKeys=[openKeys[0]]
              // that.$emit('itemSelect', openKeys[0])
            },
            // 'update:openKeys': val => { this.localOpenKeys = val }
          }}}
          selectedKeys={this.selectedKeys}
          openKeys={this.openKeys}>
          { list }
        </Menu>
      </div>
    )
  },
  created () {
    // this.localOpenKeys = this.openKeys
    // this.localOpenKeys = this.openKeys.slice(0)
  },
}
