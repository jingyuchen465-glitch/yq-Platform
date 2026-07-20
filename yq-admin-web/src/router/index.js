import Vue from 'vue'
import VueRouter from 'vue-router'
import Layout from '@/layout/Layout.vue'

Vue.use(VueRouter)

const routes = [
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
      }
    ]
  }
]

const router = new VueRouter({
  routes
})

export default router
