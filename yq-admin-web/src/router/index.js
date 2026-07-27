import Vue from 'vue'
import VueRouter, { isNavigationFailure } from 'vue-router'
import Layout from '@/layout/Layout.vue'

Vue.use(VueRouter)

// Vue Router 3 会将重复、重定向或被新导航取消的正常流程作为 Promise reject 返回。
// 统一吸收这类预期结果，避免开发环境把登录重定向显示成整页运行时错误。
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
      },
      {
        path: 'course',
        name: 'CourseManage',
        component: () => import('@/views/course/CourseManage.vue'),
        meta: { title: '课程管理', icon: 'el-icon-reading' }
      },
      {
        path: 'product',
        name: 'ProductManage',
        component: () => import('@/views/product/ProductManage.vue'),
        meta: { title: '产品管理', icon: 'el-icon-goods' }
      },
      {
        path: 'prepayment-order',
        name: 'PrepaymentOrderManage',
        component: () => import('@/views/prepayment/PrepaymentOrderManage.vue'),
        meta: { title: '预订单管理', icon: 'el-icon-s-order' }
      },
      {
        path: 'student',
        name: 'StudentManage',
        component: () => import('@/views/student/StudentManage.vue'),
        meta: { title: '学员管理', icon: 'el-icon-user-solid' }
      },
      {
        path: 'class',
        name: 'ClassManage',
        component: () => import('@/views/class/ClassManage.vue'),
        meta: { title: '班级管理', icon: 'el-icon-school' }
      },
      {
        path: 'homework',
        name: 'HomeworkPublish',
        component: () => import('@/views/homework/HomeworkPublish.vue'),
        meta: { title: '作业管理', icon: 'el-icon-edit-outline' }
      },
      {
        path: 'homework/status',
        name: 'HomeworkStatus',
        component: () => import('@/views/homework/HomeworkStatus.vue'),
        meta: { title: '作业详情管理', icon: 'el-icon-data-analysis' }
      },
      {
        path: 'homework/templates',
        name: 'CourseHomeworkTemplateManage',
        component: () => import('@/views/homework/CourseHomeworkTemplateManage.vue'),
        meta: { title: '作业标准管理', icon: 'el-icon-notebook-2' }
      },
      {
        path: 'homework/:homeworkId/submissions',
        name: 'HomeworkSubmissionManage',
        component: () => import('@/views/homework/HomeworkSubmissionManage.vue'),
        meta: { title: '学生作业批改', icon: 'el-icon-document-checked', hidden: true }
      },
      {
        path: 'teacher-schedule',
        name: 'TeacherSchedule',
        component: () => import('@/views/schedule/TeacherSchedule.vue'),
        meta: { title: '教师课表', icon: 'el-icon-date' }
      },
      {
        path: 'class-duty',
        name: 'ClassDutyManage',
        component: () => import('@/views/duty/ClassDutyManage.vue'),
        meta: { title: '值班管理', icon: 'el-icon-alarm-clock' }
      },
      {
        path: 'config',
        name: 'ConfigManage',
        component: () => import('@/views/config/ConfigManage.vue'),
        meta: { title: '规则配置', icon: 'el-icon-setting' }
      },
      {
        path: 'course-detail/:courseId',
        name: 'CourseDetailManage',
        component: () => import('@/views/course/CourseDetailManage.vue'),
        meta: { title: '课程详情', icon: 'el-icon-tickets', hidden: true }
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
