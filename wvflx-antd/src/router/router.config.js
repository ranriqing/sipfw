export default {
  staticRouters: function(){
    let allStaticRouters = [
      {
        name: 'auth',
        path: '/auth',
        meta: {
          title: '用户'
        },
        component: () => import('@/components/layout/UserLayout'),
        redirect: '/auth/login',
        hidden: true,
        children: [
          {
            name: 'authLogin',
            path: '/auth/login',
            meta: {
              title: '用户登录',
              keepAlive: 'true',
            },
            component: () => import('@/views_base/auth/Login')
          },
        ]
      },
      {
        name: 'profile',
        path: '/profile',
        meta: {
          title: '个人资料'
        },
        component: () => import('@/components/layout/TabLayout'),
        redirect: '/user/profile',
        hidden: true,
        children: [
          {
            name: 'userProfile',
            path: '/user/profile',
            meta: {
              title: '个人资料',
              keepAlive: 'true',
            },
            component: () => import('@/views_base/profile/ProfilePage')
          },
        ]
      },
      {
        name: 'root',
        path: '/',
        meta: {
          title: '默认路由',
        },
        component: () => import('@/components/layout/TabLayout'),
        children: [
          {
            name: 'hrefPage',
            path: '/hrefPage',
            meta: {
              title: '外部页面',
            },
            component: () => import('@/views_app/other/page_ot_href/HrefPage'),
          },
        ],
      },
      {
        name: 'blank',
        path: '/b',
        meta: {
          title: 'Blank',
        },
        component: () => import('@/components/layout/BlankLayout'),
        children: [],
      },
      {
        name: '404',
        path: '*',
        meta: {
          title: '页面不存在'
        },
        component: () => import('@/views_base/exception/404')
      },
    ]
    // 固定菜单（LabState 下的配置入口：视图菜单 / 数据表格 / 数据字段 / 触发流程）
    allStaticRouters.push({
      name: 'fixed',
      path: '/fixed',
      meta: {
        title: '固定菜单',
      },
      component: () => import('@/components/layout/TabLayout'),
      children: [
        {
          name: 'menuEdit',
          path: '/menu/edit',
          meta: {
            title: '视图菜单',
          },
          component: () => import('@/views_app/base/page_menu/MenuPage')
        },
        {
          name: 'modelDomain',
          path: '/model/domain',
          meta: {
            title: '数据表格',
          },
          component: () => import('@/views_app/base/page_domain/DomainPage')
        },
        {
          name: 'modelField',
          path: '/model/field',
          meta: {
            title: '数据字段',
          },
          component: () => import('@/views_app/base/page_domain/FieldPage')
        },
      ]
    })
    return allStaticRouters
  }
}
