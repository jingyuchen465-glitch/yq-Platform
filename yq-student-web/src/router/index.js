import Vue from 'vue'
import VueRouter from 'vue-router'
import { hasStudentSession } from '@/utils/session'

Vue.use(VueRouter)

const router = new VueRouter({
  mode: 'history',
  routes: [
    {
      path: '/login',
      name: 'StudentLogin',
      component: () => import('@/views/Login.vue'),
      meta: { public: true, title: '登录' }
    },
    {
      path: '/',
      name: 'StudentHome',
      component: () => import('@/views/Home.vue'),
      meta: { title: '学习首页' }
    },
    { path: '*', redirect: '/' }
  ]
})

router.beforeEach((to, from, next) => {
  document.title = `${to.meta.title || '学习中心'} · YQ`
  const loggedIn = hasStudentSession()
  if (!to.meta.public && !loggedIn) {
    next({ path: '/login', query: { redirect: to.fullPath } })
  } else if (to.path === '/login' && loggedIn) {
    next('/')
  } else {
    next()
  }
})

export default router
