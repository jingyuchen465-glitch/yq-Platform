import { getCurrentStudent, logout } from '@/api/auth'
import { getStudentHomeworks } from '@/api/learning'
import { getCurrentStudentPrepaymentOrders } from '@/api/prepayment'
import { clearStudentSession, getStudentProfile, saveStudentProfile } from '@/utils/session'

export default {
  data() {
    return {
      profile: getStudentProfile(),
      loggingOut: false,
      headerHomeworkCount: 0,
      headerOrderCount: 0
    }
  },
  created() {
    this.refreshStudentHeader()
  },
  methods: {
    async refreshStudentHeader() {
      const [profileResult, homeworkResult, orderResult] = await Promise.allSettled([
        getCurrentStudent(),
        getStudentHomeworks(),
        getCurrentStudentPrepaymentOrders()
      ])
      if (profileResult.status === 'fulfilled') {
        this.profile = profileResult.value.data || {}
        saveStudentProfile(this.profile)
      }
      if (homeworkResult.status === 'fulfilled') {
        const items = homeworkResult.value.data || []
        this.headerHomeworkCount = items.filter(item => !item.submission).length
      }
      if (orderResult.status === 'fulfilled') {
        this.headerOrderCount = Array.isArray(orderResult.value.data) ? orderResult.value.data.length : 0
      }
    },
    async handleLogout() {
      this.loggingOut = true
      try {
        await logout()
      } catch (error) {
        // 本地会话始终结束，避免失效服务端会话阻塞退出。
      } finally {
        clearStudentSession()
        this.loggingOut = false
        await this.$router.replace('/login')
      }
    }
  }
}
