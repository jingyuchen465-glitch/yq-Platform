import Vue from 'vue'
import VueRouter from 'vue-router'
import Layout from '@/layout/Layout.vue'

Vue.use(VueRouter)

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/Login.vue'),
    meta: { title: '登录', public: true }
  },
  {
    path: '/',
    component: Layout,
    redirect: '/user',
    children: [
      {
        path: 'user',
        name: 'UserManage',
        component: () => import('@/views/user/UserManage.vue'),
        meta: { title: '用户管理', icon: 'el-icon-user' }
      },
      {
        path: 'role',
        name: 'RoleManage',
        component: () => import('@/views/role/RoleManage.vue'),
        meta: { title: '角色管理', icon: 'el-icon-s-check' }
      },
      {
        path: 'permission',
        name: 'PermissionManage',
        component: () => import('@/views/permission/PermissionManage.vue'),
        meta: { title: '权限管理', icon: 'el-icon-lock' }
      },
      {
        path: 'campus',
        name: 'CampusManage',
        component: () => import('@/views/campus/CampusManage.vue'),
        meta: { title: '校区管理', icon: 'el-icon-office-building' }
      }
    ]
  },
  // 兜底：未匹配路径重定向到首页（由守卫决定是否需要登录）
  { path: '*', redirect: '/' }
]

const router = new VueRouter({
  routes
})

// 导航守卫：未登录（token 或签名密钥缺失）自动跳转登录页
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('yq_token')
  const signSecret = localStorage.getItem('yq_sign_secret')
  const loggedIn = !!(token && signSecret)

  if (!to.meta.public && !loggedIn) {
    next('/login')
  } else if (to.path === '/login' && loggedIn) {
    next('/')
  } else {
    next()
  }
})

export default router
