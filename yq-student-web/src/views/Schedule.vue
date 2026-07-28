<template>
  <div class="student-page">
    <student-header :profile="profile" :logging-out="loggingOut" :homework-count="headerHomeworkCount" :order-count="headerOrderCount" @logout="handleLogout" />
    <main class="page-shell">
      <header class="page-heading">
        <div><p class="page-kicker">WEEKLY ROUTE</p><h1>我的课程表</h1><p>按周查看课程、授课教师与上课安排，点击课程可查看详细信息。</p></div>
        <div v-if="classAssigned" class="week-switcher" aria-label="切换周次">
          <button type="button" aria-label="上一周" @click="changeWeek(-1)">←</button>
          <button class="today-button" type="button" :disabled="weekOffset === 0" @click="goToday">本周</button>
          <button type="button" aria-label="下一周" @click="changeWeek(1)">→</button>
        </div>
      </header>

      <section class="week-card" :aria-busy="loading">
        <header class="week-card-heading">
          <div><strong>{{ weekRange }}</strong><span>{{ weekLabel }}</span></div>
          <p v-if="classAssigned"><i></i>{{ courses.length }} 项安排</p>
          <p v-else class="unassigned-label">未分班</p>
        </header>

        <div v-if="loading" class="schedule-state"><span class="loading-ring"></span>正在加载课程表…</div>
        <div v-else-if="errorMessage" class="error-state"><strong>课程表加载失败</strong><span>{{ errorMessage }}</span><button class="retry-button" type="button" @click="loadSchedule">重新加载</button></div>
        <div v-else-if="!classAssigned" class="schedule-state unassigned-state">
          <div><strong>暂未分班</strong><span>课程表会在分配班级后自动显示。</span></div>
        </div>
        <div v-else class="week-grid">
          <article v-for="day in days" :key="day.key" :class="['day-column', { today: day.isToday }]">
            <header><span>{{ day.weekday }}</span><strong>{{ day.day }}</strong><small>{{ day.month }}月</small></header>
            <div class="day-track">
              <button v-for="course in coursesFor(day)" :key="course.id" :class="['course-block', `tone-${tone(course)}`]" type="button" @click="selectedCourse = course">
                <time>{{ courseTime(course) }}</time>
                <strong>{{ courseName(course) }}</strong>
                <small>{{ course.teacherName || course.teacher || '教师待定' }}</small>
                <span>{{ course.room || course.classTypeName || '课程安排' }}</span>
              </button>
              <p v-if="!coursesFor(day).length" class="day-empty">无课程</p>
            </div>
          </article>
        </div>
      </section>

      <aside v-if="selectedCourse" class="course-detail" role="dialog" aria-modal="true" aria-labelledby="course-title" @click.self="selectedCourse = null">
        <div>
          <button class="close-button" type="button" aria-label="关闭课程详情" @click="selectedCourse = null">×</button>
          <p class="page-kicker">COURSE DETAIL</p><h2 id="course-title">{{ courseName(selectedCourse) }}</h2>
          <dl>
            <div><dt>上课日期</dt><dd>{{ fullDate(selectedCourse) }}</dd></div>
            <div><dt>上课时间</dt><dd>{{ courseTime(selectedCourse) }}</dd></div>
            <div><dt>授课教师</dt><dd>{{ selectedCourse.teacherName || selectedCourse.teacher || '待定' }}</dd></div>
            <div><dt>上课地点</dt><dd>{{ selectedCourse.room || selectedCourse.classTypeName || '待通知' }}</dd></div>
            <div><dt>课程类型</dt><dd>{{ selectedCourse.classTypeName || selectedCourse.stageName || '正式课程' }}</dd></div>
          </dl>
        </div>
      </aside>
    </main>
  </div>
</template>

<script>
import StudentHeader from '@/components/StudentHeader.vue'
import studentPage from '@/mixins/studentPage'
import { getStudentSchedule } from '@/api/learning'

const WEEKDAYS = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']

export default {
  name: 'StudentSchedule',
  components: { StudentHeader },
  mixins: [studentPage],
  data() { return { weekOffset: 0, courses: [], classAssigned: true, loading: true, errorMessage: '', selectedCourse: null } },
  computed: {
    weekStart() {
      const date = new Date(); const day = date.getDay() || 7
      date.setHours(0, 0, 0, 0); date.setDate(date.getDate() - day + 1 + this.weekOffset * 7)
      return date
    },
    days() {
      const today = this.key(new Date())
      return WEEKDAYS.map((weekday, index) => {
        const date = new Date(this.weekStart); date.setDate(date.getDate() + index)
        return { key: this.key(date), date, weekday, day: date.getDate(), month: date.getMonth() + 1, isToday: this.key(date) === today }
      })
    },
    weekRange() { return `${this.formatDate(this.days[0].date)} — ${this.formatDate(this.days[6].date)}` },
    weekLabel() { return this.weekOffset === 0 ? '本周' : this.weekOffset > 0 ? `${this.weekOffset} 周后` : `${Math.abs(this.weekOffset)} 周前` }
  },
  watch: { weekOffset: 'loadSchedule' },
  mounted() { this.loadSchedule() },
  methods: {
    async loadSchedule() {
      this.loading = true; this.errorMessage = ''; this.selectedCourse = null
      try {
        const response = await getStudentSchedule({ startDate: this.days[0].key, endDate: this.days[6].key })
        const data = response.data
        this.classAssigned = !data || data.classAssigned !== false
        this.courses = Array.isArray(data) ? data : (data && (data.courses || data.records || data.list)) || []
      } catch (error) { this.courses = []; this.classAssigned = true; this.errorMessage = error.message || '暂时无法获取课程安排' }
      finally { this.loading = false }
    },
    changeWeek(step) { this.weekOffset += step },
    goToday() { this.weekOffset = 0 },
    coursesFor(day) { return this.courses.filter(course => this.key(course.scheduleDate || course.date || course.startAt || course.startTime) === day.key) },
    key(value) {
      if (typeof value === 'string' && /^\d{4}-\d{2}-\d{2}/.test(value)) return value.slice(0, 10)
      const date = new Date(value); if (Number.isNaN(date.getTime())) return ''
      const pad = number => String(number).padStart(2, '0')
      return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
    },
    formatDate(date) { return `${date.getMonth() + 1}月${date.getDate()}日` },
    fullDate(course) {
      const value = course.scheduleDate || course.date || course.startAt || course.startTime
      const date = new Date(value); return Number.isNaN(date.getTime()) ? String(value || '待定').slice(0, 10) : new Intl.DateTimeFormat('zh-CN', { dateStyle: 'long', weekday: 'long' }).format(date)
    },
    courseName(course) { return course.courseContent || course.courseName || course.name || course.stageName || '课程安排' },
    courseTime(course) {
      if (!course.startAt && !course.startTime) return course.period || (course.classType === 'CLASS' ? '全天' : course.classTypeName || '全天')
      const format = value => { const date = new Date(value); return Number.isNaN(date.getTime()) ? String(value).slice(11, 16) : new Intl.DateTimeFormat('zh-CN', { hour: '2-digit', minute: '2-digit', hour12: false }).format(date) }
      return `${format(course.startAt || course.startTime)}${course.endAt || course.endTime ? `–${format(course.endAt || course.endTime)}` : ''}`
    },
    tone(course) { return course.tone || ['lagoon', 'coral', 'mint', 'violet', 'sand'][Number(course.id || 0) % 5] }
  }
}
</script>

<style scoped>
.week-switcher { display: flex; gap: 7px; padding: 5px; border: 1px solid var(--line); border-radius: 13px; background: #fff; }.week-switcher button { min-width: 38px; height: 36px; border: 0; border-radius: 9px; color: var(--lagoon-deep); background: var(--sky); font-weight: 700; }.week-switcher .today-button { min-width: 58px; color: #fff; background: var(--lagoon); }.week-switcher button:disabled { opacity: .45; cursor: default; }
.week-card { overflow: hidden; border: 1px solid var(--line); border-radius: 8px 26px 26px 26px; background: rgba(255,255,255,.92); box-shadow: var(--shadow); }.week-card-heading { min-height: 80px; display: flex; align-items: center; justify-content: space-between; padding: 20px 25px; border-bottom: 1px solid var(--line); }.week-card-heading div { display: flex; align-items: baseline; gap: 12px; }.week-card-heading strong { font: 750 17px var(--display); }.week-card-heading span { color: var(--coral); font: 650 9px var(--mono); letter-spacing: .12em; }.week-card-heading p { display: flex; align-items: center; gap: 8px; color: var(--ink-soft); font-size: 11px; }.week-card-heading i { width: 7px; height: 7px; border-radius: 50%; background: #65c8a5; box-shadow: 0 0 0 4px rgba(101,200,165,.13); }
.schedule-state { min-height: 420px; display: flex; align-items: center; justify-content: center; gap: 12px; color: var(--ink-soft); font-size: 12px; }.loading-ring { width: 18px; height: 18px; border: 2px solid rgba(14,116,144,.18); border-top-color: var(--lagoon); border-radius: 50%; animation: spin .7s linear infinite; }@keyframes spin { to { transform: rotate(360deg); } }.unassigned-label { color: #98612d; font-weight: 700; }.unassigned-state div { display: flex; flex-direction: column; gap: 8px; align-items: center; text-align: center; }.unassigned-state strong { color: var(--ink); font: 700 18px var(--display); }.unassigned-state span { max-width: 260px; line-height: 1.8; }
.week-grid { display: grid; grid-template-columns: repeat(7, minmax(128px,1fr)); overflow-x: auto; }.day-column { min-height: 460px; border-right: 1px solid #e7eef2; }.day-column:last-child { border-right: 0; }.day-column > header { height: 84px; display: grid; grid-template-columns: 1fr auto; align-content: center; padding: 13px 15px; border-bottom: 1px solid #e7eef2; }.day-column > header span { grid-column: 1 / -1; color: var(--ink-soft); font-size: 10px; }.day-column > header strong { margin-top: 3px; font: 750 22px var(--display); }.day-column > header small { align-self: end; margin-bottom: 3px; color: #8ca3b2; font-size: 9px; }.day-column.today > header { color: #fff; background: var(--lagoon); }.day-column.today > header span,.day-column.today > header small { color: rgba(255,255,255,.7); }
.day-track { display: grid; align-content: start; gap: 9px; padding: 11px; }.course-block { min-height: 112px; display: flex; flex-direction: column; padding: 13px; border: 0; border-left: 3px solid currentColor; border-radius: 5px 13px 13px 5px; color: var(--lagoon-deep); background: #e5f5f5; text-align: left; transition: transform .18s ease, box-shadow .18s ease; }.course-block:hover { transform: translateY(-2px); box-shadow: 0 10px 18px -14px currentColor; }.course-block time { font: 650 8px var(--mono); }.course-block strong { margin-top: 8px; font-size: 11px; line-height: 1.4; }.course-block small,.course-block span { margin-top: 5px; color: var(--ink-soft); font-size: 8px; }.tone-coral { color: #b6523b; background: #fff0ec; }.tone-mint { color: #267c63; background: #e6f6ef; }.tone-violet { color: #67528f; background: #f0ecfa; }.tone-sand { color: #916c2f; background: #faf3df; }.day-empty { padding-top: 25px; color: #9db0bc; text-align: center; font-size: 9px; }
.course-detail { position: fixed; z-index: 40; inset: 0; display: flex; align-items: center; justify-content: center; padding: 24px; background: rgba(7,36,48,.38); backdrop-filter: blur(4px); }.course-detail > div { width: min(100%,440px); position: relative; padding: 34px; border-radius: 8px 26px 26px 26px; background: #fff; box-shadow: 0 30px 80px -30px rgba(4,44,56,.8); }.close-button { width: 34px; height: 34px; position: absolute; top: 18px; right: 18px; border: 0; border-radius: 50%; color: var(--ink-soft); background: var(--sky); font-size: 21px; }.course-detail h2 { margin-top: 10px; padding-right: 35px; font: 750 25px var(--display); }.course-detail dl { margin-top: 24px; }.course-detail dl div { display: grid; grid-template-columns: 86px 1fr; padding: 13px 0; border-top: 1px solid var(--line); }.course-detail dt { color: var(--ink-soft); font-size: 11px; }.course-detail dd { font-size: 12px; font-weight: 650; }
@media (max-width: 720px) { .week-card-heading { align-items: flex-start; flex-direction: column; gap: 10px; }.week-grid { grid-template-columns: repeat(7, minmax(150px,1fr)); }.page-heading .week-switcher { align-self: stretch; justify-content: space-between; } }
</style>
