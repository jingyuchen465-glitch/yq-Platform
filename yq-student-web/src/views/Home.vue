<template>
  <div class="home-page">
    <header class="topbar">
      <router-link class="brand" to="/" aria-label="YQ 学习中心首页">
        <span>YQ</span><strong>学习中心</strong>
      </router-link>
      <div class="student-menu">
        <span class="avatar">{{ initial }}</span>
        <div><strong>{{ profile.name || '同学' }}</strong><small>{{ maskedPhone }}</small></div>
        <button type="button" :disabled="loggingOut" @click="handleLogout">
          {{ loggingOut ? '退出中' : '退出登录' }}
        </button>
      </div>
    </header>

    <main class="home-content">
      <section class="welcome-card">
        <div class="welcome-copy">
          <p class="eyebrow">TODAY · 学习起点</p>
          <h1>{{ greeting }}，{{ profile.name || '同学' }}</h1>
          <p>你的学生身份已验证。课程与作业模块接入后，会从这里开始今天的学习路线。</p>
          <div class="identity-chip"><i></i>{{ statusText }} · 安全通道在线</div>
        </div>
        <div class="route-visual" aria-hidden="true">
          <span class="route-label">LEARNING ROUTE</span>
          <div class="orbit orbit-one"><i></i></div>
          <div class="orbit orbit-two"><i></i></div>
          <div class="center-mark"><b>YQ</b><small>START</small></div>
        </div>
      </section>

      <p v-if="errorMessage" class="page-message" role="alert">{{ errorMessage }}</p>

      <section class="dashboard-grid">
        <article class="profile-card">
          <header>
            <div><p class="eyebrow">STUDENT IDENTITY</p><h2>学生资料</h2></div>
            <span class="record-id">#{{ profile.id || '—' }}</span>
          </header>
          <dl>
            <div><dt>姓名</dt><dd>{{ profile.name || '未填写' }}</dd></div>
            <div><dt>手机号</dt><dd>{{ maskedPhone }}</dd></div>
            <div><dt>邮箱</dt><dd>{{ profile.email || '未填写' }}</dd></div>
            <div><dt>班级</dt><dd>{{ profile.classId ? `班级 #${profile.classId}` : '待分班' }}</dd></div>
            <div><dt>账号状态</dt><dd><span class="status-dot"></span>{{ statusText }}</dd></div>
          </dl>
        </article>

        <article class="next-card">
          <p class="eyebrow">NEXT STOP</p>
          <div class="next-number">01</div>
          <h2>学习功能正在接入</h2>
          <p>当前登录、身份校验和安全请求通道已经可用。下一步可在学生端继续接入课表、作业与提交记录。</p>
          <div class="module-list">
            <span>课程表</span><span>我的作业</span><span>学习记录</span>
          </div>
        </article>
      </section>
    </main>
  </div>
</template>

<script>
import { getCurrentStudent, logout } from '@/api/auth'
import {
  clearStudentSession,
  getStudentProfile,
  saveStudentProfile
} from '@/utils/session'

const STATUS_TEXT = {
  TEMPORARY: '临时学员',
  ATSCHOOL: '在校学习',
  GRADUATE: '已毕业',
  WITCHDRAWAL: '已退学'
}

export default {
  name: 'StudentHome',
  data() {
    return {
      profile: getStudentProfile(),
      errorMessage: '',
      loggingOut: false
    }
  },
  computed: {
    initial() {
      return (this.profile.name || '同').trim().slice(0, 1)
    },
    maskedPhone() {
      const phone = this.profile.phone || ''
      return /^\d{11}$/.test(phone) ? `${phone.slice(0, 3)}****${phone.slice(-4)}` : '手机号未填写'
    },
    statusText() {
      return STATUS_TEXT[this.profile.status] || '资料已建立'
    },
    greeting() {
      const hour = new Date().getHours()
      if (hour < 11) return '早上好'
      if (hour < 14) return '中午好'
      if (hour < 18) return '下午好'
      return '晚上好'
    }
  },
  mounted() {
    this.refreshProfile()
  },
  methods: {
    async refreshProfile() {
      try {
        const response = await getCurrentStudent()
        this.profile = response.data || {}
        saveStudentProfile(this.profile)
      } catch (error) {
        this.errorMessage = error.message || '学生资料暂时无法更新'
      }
    },
    async handleLogout() {
      this.loggingOut = true
      try {
        await logout()
      } catch (error) {
        // 无论服务端会话是否已失效，本地都应结束当前登录状态。
      } finally {
        clearStudentSession()
        this.loggingOut = false
        await this.$router.replace('/login')
      }
    }
  }
}
</script>

<style scoped>
.home-page { min-height: 100vh; background: linear-gradient(180deg, #eef7f8 0, var(--paper) 360px); }
.topbar { height: 76px; display: flex; align-items: center; justify-content: space-between; padding: 0 clamp(24px, 6vw, 88px); border-bottom: 1px solid rgba(14, 116, 144, .12); background: rgba(247, 250, 252, .78); backdrop-filter: blur(14px); }
.brand { display: flex; align-items: center; gap: 11px; color: var(--ink); text-decoration: none; }
.brand span { width: 39px; height: 39px; display: grid; place-items: center; border-radius: 13px 13px 4px 13px; color: #fff; background: var(--lagoon); font: 800 13px var(--display); }
.brand strong { font: 750 15px var(--display); }
.student-menu { display: flex; align-items: center; gap: 10px; }
.avatar { width: 38px; height: 38px; display: grid; place-items: center; border-radius: 50%; color: var(--lagoon-deep); background: var(--mint); font-weight: 750; }
.student-menu div { display: flex; flex-direction: column; min-width: 85px; }
.student-menu strong { font-size: 12px; }
.student-menu small { margin-top: 2px; color: var(--ink-soft); font: 9px var(--mono); }
.student-menu button { margin-left: 14px; padding: 8px 12px; border: 1px solid var(--line); border-radius: 9px; color: var(--ink-soft); background: #fff; font-size: 11px; }
.home-content { width: min(1180px, calc(100% - 48px)); margin: 0 auto; padding: 54px 0 70px; }
.welcome-card { min-height: 320px; position: relative; overflow: hidden; display: grid; grid-template-columns: 1.1fr .9fr; border-radius: 28px 28px 8px 28px; color: #fff; background: linear-gradient(128deg, #07566b, #0e7490 72%); box-shadow: var(--shadow); }
.welcome-copy { align-self: center; padding: clamp(38px, 6vw, 72px); position: relative; z-index: 1; }
.eyebrow { color: var(--coral); font: 650 10px var(--mono); letter-spacing: .2em; }
.welcome-copy .eyebrow { color: #9fe3d5; }
.welcome-copy h1 { margin-top: 14px; font: 750 clamp(34px, 4vw, 52px)/1.16 var(--display); letter-spacing: -.04em; }
.welcome-copy > p:not(.eyebrow) { max-width: 570px; margin-top: 18px; color: rgba(255, 255, 255, .67); font-size: 14px; line-height: 1.9; }
.identity-chip { display: inline-flex; align-items: center; gap: 9px; margin-top: 28px; padding: 9px 13px; border: 1px solid rgba(255, 255, 255, .15); border-radius: 999px; background: rgba(2, 41, 53, .18); font-size: 11px; }
.identity-chip i, .status-dot { width: 7px; height: 7px; border-radius: 50%; background: #83deb0; box-shadow: 0 0 0 4px rgba(131, 222, 176, .12); }
.route-visual { min-height: 320px; position: relative; }
.route-label { position: absolute; right: 26px; top: 25px; color: rgba(255, 255, 255, .35); font: 9px var(--mono); letter-spacing: .18em; }
.orbit { position: absolute; border: 1px solid rgba(255, 255, 255, .18); border-radius: 50%; }
.orbit-one { width: 280px; height: 280px; right: 5%; top: 20px; }
.orbit-two { width: 190px; height: 190px; right: calc(5% + 45px); top: 65px; }
.orbit i { position: absolute; width: 12px; height: 12px; border: 3px solid #fff; border-radius: 50%; background: var(--coral); }
.orbit-one i { left: 21px; top: 58px; }
.orbit-two i { right: 9px; bottom: 45px; background: #9fe3d5; }
.center-mark { width: 100px; height: 100px; position: absolute; right: calc(5% + 90px); top: 110px; display: grid; place-content: center; border-radius: 50%; text-align: center; color: var(--lagoon-deep); background: var(--mint); }
.center-mark b { font: 800 24px var(--display); }
.center-mark small { margin-top: 2px; font: 8px var(--mono); letter-spacing: .18em; }
.page-message { margin-top: 20px; padding: 12px 16px; border-left: 3px solid var(--coral); background: #fff0ec; color: #9d3f2a; font-size: 13px; }
.dashboard-grid { display: grid; grid-template-columns: 1.15fr .85fr; gap: 22px; margin-top: 24px; }
.profile-card, .next-card { min-height: 325px; padding: 30px; border: 1px solid var(--line); background: #fff; box-shadow: 0 18px 50px -44px rgba(16, 42, 67, .5); }
.profile-card { border-radius: 8px 24px 24px 24px; }
.profile-card header { display: flex; align-items: flex-start; justify-content: space-between; }
.profile-card h2, .next-card h2 { margin-top: 7px; font: 750 22px var(--display); letter-spacing: -.025em; }
.record-id { color: var(--lagoon); font: 600 11px var(--mono); }
.profile-card dl { display: grid; grid-template-columns: repeat(2, 1fr); gap: 0 30px; margin-top: 24px; }
.profile-card dl div { padding: 14px 0; border-top: 1px solid #e8eff3; }
.profile-card dt { color: var(--ink-soft); font-size: 11px; }
.profile-card dd { display: flex; align-items: center; gap: 8px; margin-top: 6px; font-size: 13px; font-weight: 650; }
.next-card { position: relative; overflow: hidden; border-radius: 24px 8px 24px 24px; background: var(--sky); }
.next-number { position: absolute; right: -4px; top: -28px; color: rgba(14, 116, 144, .08); font: 800 120px var(--display); }
.next-card > p:not(.eyebrow) { max-width: 390px; margin-top: 14px; color: var(--ink-soft); font-size: 13px; line-height: 1.8; }
.module-list { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 32px; }
.module-list span { padding: 8px 11px; border: 1px dashed rgba(14, 116, 144, .3); border-radius: 8px; color: var(--lagoon-deep); background: rgba(255, 255, 255, .5); font-size: 11px; }
@media (max-width: 800px) {
  .topbar { padding-inline: 20px; }
  .student-menu div { display: none; }
  .student-menu button { margin-left: 2px; }
  .home-content { width: min(100% - 32px, 620px); padding-top: 28px; }
  .welcome-card { grid-template-columns: 1fr; }
  .route-visual { min-height: 190px; opacity: .85; }
  .orbit-one { width: 220px; height: 220px; top: -10px; right: 50%; transform: translateX(50%); }
  .orbit-two { width: 145px; height: 145px; top: 28px; right: 50%; transform: translateX(50%); }
  .center-mark { width: 78px; height: 78px; top: 61px; right: 50%; transform: translateX(50%); }
  .route-label { display: none; }
  .dashboard-grid { grid-template-columns: 1fr; }
}
@media (max-width: 500px) {
  .welcome-copy { padding: 34px 26px 22px; }
  .welcome-copy h1 { font-size: 34px; }
  .profile-card, .next-card { padding: 24px; }
  .profile-card dl { grid-template-columns: 1fr; }
}
</style>
