<template>
  <div class="student-page">
    <student-header
      :profile="profile"
      :logging-out="loggingOut"
      :homework-count="pendingHomeworkCount"
      :order-count="orders.length"
      @logout="handleLogout"
    />

    <main class="home-content">
      <section class="welcome-card">
        <div class="welcome-copy">
          <p class="eyebrow">TODAY · 学习起点</p>
          <h1>{{ greeting }}，{{ profile.name || '同学' }}</h1>
          <p>{{ todaySummary }}</p>
          <div class="identity-chip"><i></i>{{ statusText }} · 学习通道在线</div>
        </div>
        <div class="route-visual" aria-label="今日学习路线">
          <span class="route-label">LEARNING ROUTE</span>
          <div class="orbit orbit-one"><i></i></div>
          <div class="orbit orbit-two"><i></i></div>
          <div class="center-mark"><b>{{ todayCourses.length }}</b><small>今日课程</small></div>
        </div>
      </section>

      <p v-if="errorMessage" class="page-message" role="alert">{{ errorMessage }}</p>

      <section class="learning-rail" aria-label="学习功能">
        <router-link class="rail-card rail-schedule" to="/schedule">
          <span class="rail-index">COURSE</span>
          <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M7 3v3m10-3v3M4 9h16M5 5h14a1 1 0 0 1 1 1v13H4V6a1 1 0 0 1 1-1Zm3 8h3m2 0h3m-8 3h3"/></svg>
          <div><h2>课程表</h2><p>{{ todayCourses.length ? `今天有 ${todayCourses.length} 节课` : '今天没有课程安排' }}</p></div>
          <span class="rail-arrow">→</span>
        </router-link>
        <router-link class="rail-card rail-homework" to="/homework">
          <span class="rail-index">TASK</span>
          <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M6 3h9l3 3v15H6V3Zm8 0v4h4M9 12h6m-6 4h4"/></svg>
          <div><h2>我的作业</h2><p>{{ pendingHomeworkCount ? `${pendingHomeworkCount} 项等待完成` : '作业均已完成' }}</p></div>
          <span class="rail-arrow">→</span>
        </router-link>
        <router-link class="rail-card rail-orders" to="/orders">
          <span class="rail-index">ORDER</span>
          <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M5 4h14v17l-3-2-4 2-4-2-3 2V4Zm4 5h6m-6 4h6"/></svg>
          <div><h2>我的订单</h2><p>{{ orders.length ? `${orders.length} 笔课程订单` : '暂无课程订单' }}</p></div>
          <span class="rail-arrow">→</span>
        </router-link>
      </section>

      <section class="class-briefing" aria-labelledby="class-briefing-title">
        <div class="class-stamp" aria-hidden="true">
          <span>CLASS</span>
          <strong>{{ classInitial }}</strong>
          <small>{{ profile.classId ? `#${profile.classId}` : '待分班' }}</small>
        </div>
        <div class="class-overview">
          <p class="eyebrow">MY CLASS · 班级归属</p>
          <h2 id="class-briefing-title">{{ profile.className || '班级信息待完善' }}</h2>
          <p>{{ classDescription }}</p>
          <dl class="class-facts">
            <div><dt>学习课程</dt><dd>{{ profile.courseName || '暂未配置' }}</dd></div>
            <div><dt>所在校区</dt><dd>{{ profile.campusLocation || '暂未配置' }}</dd></div>
          </dl>
        </div>
        <div class="teacher-profile">
          <div class="teacher-avatar" aria-hidden="true">{{ teacherInitial }}</div>
          <div class="teacher-copy">
            <span>班主任</span>
            <strong>{{ profile.headTeacherName || '暂未配置' }}</strong>
            <a v-if="profile.headTeacherPhone" :href="`tel:${profile.headTeacherPhone}`">{{ profile.headTeacherPhone }}</a>
            <small v-else>联系方式待补充</small>
          </div>
          <a v-if="profile.headTeacherEmail" class="teacher-mail" :href="`mailto:${profile.headTeacherEmail}`" aria-label="给班主任发送邮件">
            <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M3 6h18v12H3V6Zm1 1 8 6 8-6"/></svg>
          </a>
        </div>
      </section>

      <section class="dashboard-grid">
        <article class="profile-card">
          <header><div><p class="eyebrow">STUDENT IDENTITY</p><h2>学生资料</h2></div><span class="record-id">#{{ profile.id || '—' }}</span></header>
          <dl>
            <div><dt>姓名</dt><dd>{{ profile.name || '未填写' }}</dd></div>
            <div><dt>手机号</dt><dd>{{ maskedPhone }}</dd></div>
            <div><dt>邮箱</dt><dd>{{ profile.email || '未填写' }}</dd></div>
            <div><dt>班级</dt><dd>{{ profile.className || (profile.classId ? `班级 #${profile.classId}` : '待分班') }}</dd></div>
            <div><dt>账号状态</dt><dd><span class="status-dot"></span>{{ statusText }}</dd></div>
          </dl>
        </article>

        <article class="today-card">
          <header><div><p class="eyebrow">TODAY'S ROUTE</p><h2>今天的学习安排</h2></div><router-link to="/schedule">查看完整课表</router-link></header>
          <div v-if="loading" class="today-state">正在整理今天的安排…</div>
          <div v-else-if="!todayCourses.length" class="today-state">今天没有课程，留一点时间复习和休息。</div>
          <ol v-else class="today-list">
            <li v-for="course in todayCourses" :key="course.id">
              <time>{{ courseTime(course) }}</time>
              <i></i>
              <div><strong>{{ courseName(course) }}</strong><small>{{ course.teacherName || course.teacher || '教师待定' }} · {{ course.room || course.classTypeName || '上课' }}</small></div>
            </li>
          </ol>
        </article>
      </section>
    </main>
  </div>
</template>

<script>
import StudentHeader from '@/components/StudentHeader.vue'
import studentPage from '@/mixins/studentPage'
import { getStudentHomeworks, getStudentSchedule } from '@/api/learning'
import { getCurrentStudentPrepaymentOrders } from '@/api/prepayment'

const STATUS_TEXT = { TEMPORARY: '临时学员', ATSCHOOL: '在校学习', GRADUATE: '已毕业', WITCHDRAWAL: '已退学' }

export default {
  name: 'StudentHome',
  components: { StudentHeader },
  mixins: [studentPage],
  data() {
    return { schedule: [], homeworks: [], orders: [], loading: true, errorMessage: '' }
  },
  computed: {
    greeting() {
      const hour = new Date().getHours()
      if (hour < 11) return '早上好'
      if (hour < 14) return '中午好'
      if (hour < 18) return '下午好'
      return '晚上好'
    },
    statusText() { return STATUS_TEXT[this.profile.status] || '资料已建立' },
    maskedPhone() {
      const phone = this.profile.phone || ''
      return /^\d{11}$/.test(phone) ? `${phone.slice(0, 3)}****${phone.slice(-4)}` : '手机号未填写'
    },
    classInitial() {
      const name = this.profile.className || ''
      return name.trim().charAt(0).toUpperCase() || 'YQ'
    },
    teacherInitial() {
      const name = this.profile.headTeacherName || ''
      return name.trim().charAt(0).toUpperCase() || '师'
    },
    classDescription() {
      if (!this.profile.classId) return '完成分班后，这里会同步展示你的课程归属、所在校区和班主任联系方式。'
      return '班级是你在 YQ 的学习坐标。课程安排、作业进度与日常通知都将围绕这里展开。'
    },
    todayCourses() {
      const today = this.dateKey(new Date())
      return this.schedule.filter(item => this.dateKey(item.scheduleDate || item.startAt || item.date) === today)
    },
    pendingHomeworkCount() { return this.homeworks.filter(item => !this.isSubmitted(item)).length },
    todaySummary() {
      if (this.loading) return '正在为你整理今天的课程、作业和订单。'
      return `今天有 ${this.todayCourses.length} 节课，${this.pendingHomeworkCount} 项作业待完成。沿着学习轨道继续前进吧。`
    }
  },
  mounted() { this.loadDashboard() },
  methods: {
    async loadDashboard() {
      this.loading = true
      this.errorMessage = ''
      try {
        const [scheduleResult, homeworkResult, orderResult] = await Promise.all([
          getStudentSchedule(), getStudentHomeworks(), getCurrentStudentPrepaymentOrders()
        ])
        this.schedule = this.toArray(scheduleResult.data, ['courses', 'records', 'list'])
        this.homeworks = this.toArray(homeworkResult.data, ['homeworks', 'records', 'list'])
        this.orders = Array.isArray(orderResult.data) ? orderResult.data : []
      } catch (error) {
        this.errorMessage = error.message || '学习数据暂时无法更新，请稍后重试'
      } finally { this.loading = false }
    },
    toArray(value, keys) {
      if (Array.isArray(value)) return value
      for (const key of keys) if (value && Array.isArray(value[key])) return value[key]
      return []
    },
    isSubmitted(item) { return Boolean(item.submission || item.submissionId || item.submitTime || item.submitted) },
    dateKey(value) {
      const date = new Date(value)
      if (Number.isNaN(date.getTime())) return String(value || '').slice(0, 10)
      return `${date.getFullYear()}-${date.getMonth() + 1}-${date.getDate()}`
    },
    courseName(course) { return course.courseContent || course.courseName || course.name || course.stageName || '课程安排' },
    courseTime(course) {
      const value = course.startAt || course.startTime
      if (!value) return course.period || '全天'
      const date = new Date(value)
      return Number.isNaN(date.getTime()) ? String(value).slice(11, 16) : new Intl.DateTimeFormat('zh-CN', { hour: '2-digit', minute: '2-digit', hour12: false }).format(date)
    }
  }
}
</script>

<style scoped>
.home-content { width: min(1180px, calc(100% - 48px)); margin: 0 auto; padding: 46px 0 70px; }
.welcome-card { min-height: 318px; position: relative; overflow: hidden; display: grid; grid-template-columns: 1.1fr .9fr; border-radius: 28px 28px 8px 28px; color: #fff; background: linear-gradient(128deg, #07566b, #0e7490 72%); box-shadow: var(--shadow); }
.welcome-copy { align-self: center; padding: clamp(38px, 6vw, 72px); position: relative; z-index: 1; }
.eyebrow { color: var(--coral); font: 650 10px var(--mono); letter-spacing: .2em; }
.welcome-copy .eyebrow { color: #9fe3d5; }
.welcome-copy h1 { margin-top: 14px; font: 750 clamp(34px, 4vw, 52px)/1.16 var(--display); letter-spacing: -.04em; }
.welcome-copy > p:not(.eyebrow) { max-width: 570px; margin-top: 18px; color: rgba(255,255,255,.68); font-size: 14px; line-height: 1.9; }
.identity-chip { display: inline-flex; align-items: center; gap: 9px; margin-top: 28px; padding: 9px 13px; border: 1px solid rgba(255,255,255,.15); border-radius: 999px; background: rgba(2,41,53,.18); font-size: 11px; }
.identity-chip i,.status-dot { width: 7px; height: 7px; border-radius: 50%; background: #83deb0; box-shadow: 0 0 0 4px rgba(131,222,176,.12); }
.route-visual { min-height: 318px; position: relative; }
.route-label { position: absolute; right: 26px; top: 25px; color: rgba(255,255,255,.35); font: 9px var(--mono); letter-spacing: .18em; }
.orbit { position: absolute; border: 1px solid rgba(255,255,255,.18); border-radius: 50%; }
.orbit-one { width: 280px; height: 280px; right: 5%; top: 19px; }
.orbit-two { width: 190px; height: 190px; right: calc(5% + 45px); top: 64px; }
.orbit i { width: 12px; height: 12px; position: absolute; border: 3px solid #fff; border-radius: 50%; background: var(--coral); }
.orbit-one i { left: 21px; top: 58px; }.orbit-two i { right: 9px; bottom: 45px; background: #9fe3d5; }
.center-mark { width: 100px; height: 100px; position: absolute; right: calc(5% + 90px); top: 109px; display: grid; place-content: center; border-radius: 50%; text-align: center; color: var(--lagoon-deep); background: var(--mint); }
.center-mark b { font: 800 28px var(--display); }.center-mark small { margin-top: 2px; font: 8px var(--mono); letter-spacing: .12em; }
.page-message { margin-top: 18px; padding: 12px 16px; border-left: 3px solid var(--coral); color: #9d3f2a; background: #fff0ec; font-size: 12px; }
.learning-rail { display: grid; grid-template-columns: repeat(3, 1fr); gap: 14px; margin-top: 22px; }
.rail-card { min-height: 106px; position: relative; overflow: hidden; display: grid; grid-template-columns: 44px 1fr auto; align-items: center; gap: 13px; padding: 22px; border: 1px solid var(--line); border-radius: 10px 20px 20px 20px; color: var(--ink); background: rgba(255,255,255,.9); text-decoration: none; transition: transform .18s ease, box-shadow .18s ease, border-color .18s ease; }
.rail-card:hover { transform: translateY(-3px); border-color: rgba(14,116,144,.35); box-shadow: 0 16px 34px -28px rgba(7,86,107,.8); }
.rail-card > svg { width: 42px; height: 42px; padding: 10px; border-radius: 13px; fill: none; stroke: currentColor; stroke-width: 1.7; stroke-linecap: round; stroke-linejoin: round; color: var(--lagoon); background: var(--sky); }
.rail-card h2 { font: 750 15px var(--display); }.rail-card p { margin-top: 5px; color: var(--ink-soft); font-size: 10px; }
.rail-index { position: absolute; top: 9px; right: 12px; color: rgba(14,116,144,.16); font: 700 8px var(--mono); letter-spacing: .14em; }
.rail-arrow { color: var(--lagoon); font-size: 20px; }.rail-homework > svg { color: #b6523b; background: #fff0ec; }.rail-orders > svg { color: #6d5a9a; background: #f0ecfa; }
.class-briefing { min-height: 220px; display: grid; grid-template-columns: 150px minmax(0,1fr) minmax(270px,.72fr); align-items: stretch; margin-top: 22px; overflow: hidden; border: 1px solid var(--line); border-radius: 24px 8px 24px 24px; background: #fff; box-shadow: 0 18px 50px -44px rgba(16,42,67,.5); }
.class-stamp { position: relative; display: flex; flex-direction: column; align-items: center; justify-content: center; color: #fff; background: var(--lagoon-deep); }
.class-stamp::after { content: ''; width: 78px; height: 78px; position: absolute; right: -39px; bottom: -39px; border: 15px solid rgba(217,243,238,.12); border-radius: 50%; }
.class-stamp span { font: 650 9px var(--mono); letter-spacing: .22em; opacity: .55; }.class-stamp strong { margin-top: 16px; font: 800 42px/1 var(--display); }.class-stamp small { margin-top: 12px; padding: 5px 9px; border: 1px solid rgba(255,255,255,.16); border-radius: 999px; font: 9px var(--mono); opacity: .72; }
.class-overview { align-self: center; padding: 34px 38px; }.class-overview h2 { margin-top: 8px; font: 760 clamp(24px,3vw,32px)/1.2 var(--display); letter-spacing: -.035em; }.class-overview > p:not(.eyebrow) { max-width: 570px; margin-top: 11px; color: var(--ink-soft); font-size: 12px; line-height: 1.75; }
.class-facts { display: flex; gap: 36px; margin-top: 24px; }.class-facts div { min-width: 130px; padding-left: 13px; border-left: 2px solid var(--mint); }.class-facts dt { color: var(--ink-soft); font-size: 9px; }.class-facts dd { margin-top: 5px; font-size: 12px; font-weight: 700; }
.teacher-profile { position: relative; display: flex; align-items: center; gap: 15px; margin: 24px 24px 24px 0; padding: 24px; border-radius: 18px 6px 18px 18px; background: var(--sky); }
.teacher-avatar { width: 54px; height: 54px; flex: 0 0 auto; display: grid; place-items: center; border-radius: 18px 18px 5px 18px; color: #fff; background: var(--lagoon); font: 750 20px var(--display); box-shadow: 0 12px 26px -18px rgba(7,86,107,.8); }
.teacher-copy { min-width: 0; display: flex; flex-direction: column; }.teacher-copy span { color: var(--ink-soft); font-size: 9px; }.teacher-copy strong { margin-top: 4px; font: 750 15px var(--display); }.teacher-copy a,.teacher-copy small { margin-top: 7px; color: var(--lagoon); font-size: 10px; text-decoration: none; }.teacher-copy a:hover { text-decoration: underline; }
.teacher-mail { width: 35px; height: 35px; flex: 0 0 auto; display: grid; place-items: center; margin-left: auto; border-radius: 50%; color: var(--lagoon); background: #fff; }.teacher-mail svg { width: 17px; fill: none; stroke: currentColor; stroke-width: 1.7; stroke-linecap: round; stroke-linejoin: round; }
.dashboard-grid { display: grid; grid-template-columns: .9fr 1.1fr; gap: 22px; margin-top: 22px; }
.profile-card,.today-card { min-height: 310px; padding: 30px; border: 1px solid var(--line); background: #fff; box-shadow: 0 18px 50px -44px rgba(16,42,67,.5); }
.profile-card { border-radius: 8px 24px 24px 24px; }.today-card { border-radius: 24px 8px 24px 24px; background: var(--sky); }
.profile-card header,.today-card header { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; }
.profile-card h2,.today-card h2 { margin-top: 7px; font: 750 21px var(--display); }.record-id { color: var(--lagoon); font: 600 11px var(--mono); }
.profile-card dl { display: grid; grid-template-columns: repeat(2,1fr); gap: 0 30px; margin-top: 23px; }.profile-card dl div { padding: 13px 0; border-top: 1px solid #e8eff3; }
.profile-card dt { color: var(--ink-soft); font-size: 11px; }.profile-card dd { display: flex; align-items: center; gap: 8px; margin-top: 6px; font-size: 13px; font-weight: 650; }
.today-card header a { color: var(--lagoon); font-size: 10px; font-weight: 700; text-decoration: none; }.today-state { min-height: 180px; display: grid; place-items: center; color: var(--ink-soft); font-size: 12px; }
.today-list { margin-top: 22px; list-style: none; }.today-list li { min-height: 62px; display: grid; grid-template-columns: 50px 12px 1fr; gap: 12px; align-items: start; }
.today-list time { color: var(--lagoon-deep); font: 650 10px var(--mono); }.today-list i { width: 9px; height: 9px; position: relative; border: 2px solid var(--lagoon); border-radius: 50%; background: #fff; }
.today-list i::after { content: ''; width: 1px; height: 43px; position: absolute; top: 9px; left: 2px; background: rgba(14,116,144,.22); }.today-list li:last-child i::after { display: none; }
.today-list div { display: flex; flex-direction: column; }.today-list strong { font-size: 12px; }.today-list small { margin-top: 5px; color: var(--ink-soft); font-size: 10px; }
@media (max-width: 940px) { .class-briefing { grid-template-columns: 120px 1fr; }.teacher-profile { grid-column: 1 / -1; margin: 0 22px 22px; } }
@media (max-width: 860px) { .learning-rail { grid-template-columns: 1fr; }.dashboard-grid { grid-template-columns: 1fr; } }
@media (max-width: 720px) { .home-content { width: min(100% - 32px,620px); padding-top: 28px; }.welcome-card { grid-template-columns: 1fr; }.route-visual { display: none; }.welcome-copy { padding: 42px 28px; } }
@media (max-width: 560px) { .class-briefing { grid-template-columns: 1fr; }.class-stamp { min-height: 108px; flex-direction: row; gap: 13px; }.class-stamp strong,.class-stamp small { margin-top: 0; }.class-overview { padding: 28px 24px; }.class-facts { flex-direction: column; gap: 14px; }.teacher-profile { margin: 0 16px 16px; }.teacher-mail { display: none; } }
@media (max-width: 500px) { .welcome-copy h1 { font-size: 34px; }.profile-card,.today-card { padding: 24px; }.profile-card dl { grid-template-columns: 1fr; }.rail-card { padding: 19px; } }
</style>
