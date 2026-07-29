import Vue from 'vue'
import VueRouter, { isNavigationFailure } from 'vue-router'
import { hasStudentSession } from '@/utils/session'

Vue.use(VueRouter)

// Vue Router 3 rejects the Promise returned by push/replace when a navigation
// is redirected, duplicated or superseded by a newer navigation. Those are
// expected control-flow results, so do not surface them as runtime errors.
function wrapNavigationMethod(methodName) {
  const originalMethod = VueRouter.prototype[methodName]
  VueRouter.prototype[methodName] = function navigation(location, onResolve, onReject) {
    if (onResolve || onReject) {
      return originalMethod.call(this, location, onResolve, onReject)
    }
    return originalMethod.call(this, location).catch(error => {
      if (isNavigationFailure(error)) return error
      return Promise.reject(error)
    })
  }
}

if (!VueRouter.prototype.__yqNavigationFailureHandled) {
  wrapNavigationMethod('push')
  wrapNavigationMethod('replace')
  Object.defineProperty(VueRouter.prototype, '__yqNavigationFailureHandled', {
    value: true,
    configurable: false,
    enumerable: false
  })
}

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
    {
      path: '/schedule',
      name: 'StudentSchedule',
      component: () => import('@/views/Schedule.vue'),
      meta: { title: '我的课程表' }
    },
    {
      path: '/homework',
      name: 'StudentHomework',
      component: () => import('@/views/Homework.vue'),
      meta: { title: '我的作业' }
    },
    {
      path: '/exams',
      name: 'StudentExams',
      component: () => import('@/views/Exams.vue'),
      meta: { title: '我的考试' }
    },
    {
      path: '/exams/:examId/take',
      name: 'StudentExamRoom',
      component: () => import('@/views/ExamRoom.vue'),
      meta: { title: '考试答题' }
    },
    {
      path: '/exam-records/:recordId/result',
      name: 'StudentExamResult',
      component: () => import('@/views/ExamResult.vue'),
      meta: { title: '答卷详情' }
    },
    {
      path: '/orders',
      name: 'StudentOrders',
      component: () => import('@/views/Orders.vue'),
      meta: { title: '我的订单' }
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
