<template>
  <div class="page-container teacher-schedule-page">
    <header class="page-head">
      <div>
        <p class="eyebrow">SYS:SCHEDULE · FACULTY</p>
        <h1>教师课表</h1>
      </div>
      <div class="head-stat">
        <b>{{ calendar.teachingDays || 0 }}</b>
        <span>本月上课天数</span>
      </div>
    </header>

    <section class="schedule-console" aria-label="教师课表筛选">
      <div class="console-controls">
        <div class="control-block teacher-control">
          <label>授课教师</label>
          <el-select
            v-model="selectedTeacherId"
            class="teacher-select"
            filterable
            placeholder="选择教师"
            :disabled="!calendar.teachers.length"
            @change="fetchCalendar"
          >
            <el-option
              v-for="teacher in calendar.teachers"
              :key="teacher.id"
              :label="teacher.name"
              :value="teacher.id"
            />
          </el-select>
        </div>

        <div class="control-divider" aria-hidden="true"></div>

        <div class="control-block month-control">
          <label>查看月份</label>
          <div class="month-switcher">
            <el-button icon="el-icon-arrow-left" aria-label="上个月" @click="changeMonth(-1)" />
            <el-date-picker
              v-model="selectedMonth"
              type="month"
              value-format="yyyy-MM"
              format="yyyy 年 MM 月"
              :clearable="false"
              @change="handleMonthChange"
            />
            <el-button icon="el-icon-arrow-right" aria-label="下个月" @click="changeMonth(1)" />
          </div>
        </div>

        <el-button class="today-button" plain @click="goCurrentMonth">回到本月</el-button>
      </div>

      <aside class="idle-faculty" aria-label="本月无课教师">
        <div class="idle-heading">
          <span><i class="el-icon-coffee-cup"></i> 本月无课教师</span>
          <b>{{ calendar.noCourseTeachers.length }}</b>
        </div>
        <div v-if="calendar.noCourseTeachers.length" class="idle-list">
          <span v-for="teacher in calendar.noCourseTeachers" :key="teacher.id" class="idle-chip">
            {{ teacher.name }}
          </span>
        </div>
        <p v-else class="idle-empty">
          {{ calendar.teachers.length ? '所有教师本月均有课程' : '暂无有效讲师' }}
        </p>
      </aside>
    </section>

    <section v-loading="loading" class="calendar-card">
      <div class="calendar-card-head">
        <div>
          <p class="calendar-kicker">{{ monthTitle }}</p>
          <h2>{{ calendar.teacherName || '请选择教师' }}</h2>
        </div>
        <div class="calendar-legend">
          <span><i class="legend-class"></i>授课安排</span>
          <span><i class="legend-open"></i>无课程</span>
        </div>
      </div>

      <div v-if="calendar.teachers.length" class="calendar-scroll">
        <el-calendar v-model="calendarValue" class="teacher-calendar" :first-day-of-week="1">
          <template slot="dateCell" slot-scope="{ data }">
            <div
              class="day-cell"
              :class="{ 'outside-month': !isCurrentMonth(data.day), 'is-today': isToday(data.day) }"
            >
              <div class="day-meta">
                <span class="day-number">{{ dayNumber(data.day) }}</span>
                <small v-if="isToday(data.day)">今天</small>
              </div>

              <div v-if="isCurrentMonth(data.day) && schedulesByDate[data.day]" class="day-lessons">
                <el-tooltip
                  v-for="item in schedulesByDate[data.day]"
                  :key="item.scheduleId"
                  placement="top"
                  :open-delay="280"
                >
                  <div slot="content" class="lesson-tooltip">
                    <b>{{ item.className }}</b><br>
                    {{ item.teacherName }}<br>
                    {{ item.stageName || '未设置阶段' }} · {{ item.courseContent }}
                  </div>
                  <article class="lesson-ticket" :class="ticketTone(item.classId)" tabindex="0">
                    <div class="lesson-class">
                      <i class="el-icon-school"></i>
                      <strong>{{ item.className }}</strong>
                    </div>
                    <div class="lesson-teacher">
                      <i class="el-icon-user"></i>
                      <span>{{ item.teacherName }}</span>
                    </div>
                    <p>{{ item.stageName || item.courseContent }}</p>
                  </article>
                </el-tooltip>
              </div>
              <span v-else-if="isCurrentMonth(data.day)" class="open-day">无课</span>
            </div>
          </template>
        </el-calendar>
      </div>

      <el-empty v-else description="暂无有效讲师，请先在用户管理中配置 LECTURER 角色" />
    </section>
  </div>
</template>

<script>
import { getTeacherScheduleCalendar } from '@/api/teacherSchedule'

function pad(value) {
  return String(value).padStart(2, '0')
}

function formatMonth(date) {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}`
}

function monthDate(month) {
  const [year, value] = month.split('-').map(Number)
  return new Date(year, value - 1, 1)
}

export default {
  name: 'TeacherSchedule',
  data() {
    const now = new Date()
    return {
      loading: false,
      requestSequence: 0,
      selectedTeacherId: null,
      selectedMonth: formatMonth(now),
      calendarValue: new Date(now.getFullYear(), now.getMonth(), 1),
      calendar: {
        month: '',
        teacherId: null,
        teacherName: '',
        teachingDays: 0,
        teachers: [],
        schedules: [],
        noCourseTeachers: []
      }
    }
  },
  computed: {
    schedulesByDate() {
      return (this.calendar.schedules || []).reduce((result, item) => {
        if (!result[item.scheduleDate]) result[item.scheduleDate] = []
        result[item.scheduleDate].push(item)
        return result
      }, {})
    },
    monthTitle() {
      const date = monthDate(this.selectedMonth)
      return `${date.getFullYear()} 年 ${pad(date.getMonth() + 1)} 月课程分布`
    }
  },
  watch: {
    calendarValue(date) {
      const month = formatMonth(date)
      if (month !== this.selectedMonth) {
        this.selectedMonth = month
        this.fetchCalendar()
      }
    }
  },
  created() {
    this.fetchCalendar()
  },
  methods: {
    async fetchCalendar() {
      const requestId = ++this.requestSequence
      this.loading = true
      try {
        const params = { month: this.selectedMonth }
        if (this.selectedTeacherId) params.teacherId = this.selectedTeacherId
        const res = await getTeacherScheduleCalendar(params)
        if (requestId !== this.requestSequence) return
        this.calendar = Object.assign({
          month: this.selectedMonth,
          teacherId: null,
          teacherName: '',
          teachingDays: 0,
          teachers: [],
          schedules: [],
          noCourseTeachers: []
        }, res.data || {})
        this.selectedTeacherId = this.calendar.teacherId
      } catch (e) {
        // 错误已由请求拦截器统一处理
      } finally {
        if (requestId === this.requestSequence) this.loading = false
      }
    },
    handleMonthChange(month) {
      this.calendarValue = monthDate(month)
      this.fetchCalendar()
    },
    changeMonth(offset) {
      const date = monthDate(this.selectedMonth)
      date.setMonth(date.getMonth() + offset)
      this.selectedMonth = formatMonth(date)
      this.calendarValue = new Date(date.getFullYear(), date.getMonth(), 1)
      this.fetchCalendar()
    },
    goCurrentMonth() {
      const now = new Date()
      this.selectedMonth = formatMonth(now)
      this.calendarValue = new Date(now.getFullYear(), now.getMonth(), 1)
      this.fetchCalendar()
    },
    isCurrentMonth(day) {
      return day.slice(0, 7) === this.selectedMonth
    },
    isToday(day) {
      const now = new Date()
      return day === `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`
    },
    dayNumber(day) {
      return Number(day.slice(-2))
    },
    ticketTone(classId) {
      const tones = ['tone-jade', 'tone-brass', 'tone-clay', 'tone-blue']
      return tones[Math.abs(Number(classId || 0)) % tones.length]
    }
  }
}
</script>

<style scoped>
.teacher-schedule-page { min-height: 100%; }

.schedule-console {
  display: grid;
  grid-template-columns: minmax(560px, 1.5fr) minmax(300px, 1fr);
  gap: 16px;
  margin-bottom: 16px;
}

.console-controls,
.idle-faculty,
.calendar-card {
  border: 1px solid var(--line-soft);
  border-radius: var(--radius);
  background: var(--card);
  box-shadow: var(--shadow-card);
}

.console-controls {
  display: flex;
  align-items: flex-end;
  gap: 18px;
  padding: 18px 20px;
}

.control-block { display: flex; min-width: 0; flex-direction: column; gap: 8px; }
.control-block label {
  color: var(--ink-3);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: .12em;
}
.teacher-control { flex: 1; }
.teacher-select { width: 100%; }
.month-control { flex: none; }
.month-switcher { display: flex; align-items: center; gap: 6px; }
.month-switcher .el-date-editor { width: 168px; }
.month-switcher .el-button { padding-right: 12px; padding-left: 12px; }
.control-divider { width: 1px; height: 38px; margin-bottom: 1px; background: var(--line-soft); }
.today-button { flex: none; }

.idle-faculty {
  position: relative;
  overflow: hidden;
  padding: 17px 20px;
  border-left: 3px solid var(--brass);
  background: linear-gradient(120deg, #FFFDF7 0%, #FFFFFF 68%);
}
.idle-heading { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
.idle-heading span { color: var(--ink-2); font-size: 13px; font-weight: 700; }
.idle-heading i { margin-right: 5px; color: var(--brass-ink); }
.idle-heading b {
  color: var(--brass-ink);
  font-family: var(--font-mono);
  font-size: 22px;
  line-height: 1;
}
.idle-list { display: flex; max-height: 54px; overflow-y: auto; flex-wrap: wrap; gap: 7px; }
.idle-chip {
  padding: 4px 8px;
  border: 1px solid #E8D6AD;
  border-radius: 4px;
  background: var(--brass-soft);
  color: var(--brass-ink);
  font-size: 11px;
  font-weight: 600;
}
.idle-empty { color: var(--ink-3); font-size: 12px; line-height: 1.6; }

.calendar-card { display: flex; min-height: 610px; flex: 1; flex-direction: column; overflow: hidden; }
.calendar-card-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
  padding: 18px 22px 16px;
  border-bottom: 1px solid var(--line-soft);
  background: #FBFCFA;
}
.calendar-kicker {
  margin-bottom: 5px;
  color: var(--ink-3);
  font-family: var(--font-mono);
  font-size: 10px;
  letter-spacing: .12em;
  text-transform: uppercase;
}
.calendar-card-head h2 { color: var(--ink); font-family: var(--font-display); font-size: 18px; }
.calendar-legend { display: flex; align-items: center; gap: 16px; color: var(--ink-3); font-size: 11px; }
.calendar-legend span { display: inline-flex; align-items: center; gap: 6px; }
.calendar-legend i { width: 9px; height: 9px; border-radius: 2px; }
.legend-class { background: var(--jade); }
.legend-open { border: 1px solid #C9D1C8; background: #F7F9F5; }

.calendar-scroll { flex: 1; overflow-x: auto; }
.teacher-calendar { min-width: 880px; height: 100%; }
.day-cell { height: 100%; min-height: 104px; padding: 7px; background: #FFFFFF; transition: background-color .16s ease; }
.day-cell.outside-month { background: #F8FAF7; opacity: .42; }
.day-cell.is-today { box-shadow: inset 0 3px 0 var(--brass); }
.day-meta { display: flex; align-items: center; justify-content: space-between; margin-bottom: 7px; }
.day-number { color: var(--ink-2); font-family: var(--font-mono); font-size: 12px; font-weight: 600; }
.day-meta small {
  padding: 1px 5px;
  border-radius: 3px;
  background: var(--brass-soft);
  color: var(--brass-ink);
  font-size: 9px;
  font-weight: 700;
}
.day-lessons { display: flex; flex-direction: column; gap: 5px; }
.lesson-ticket {
  overflow: hidden;
  padding: 7px 7px 6px 9px;
  border-left: 3px solid var(--jade);
  border-radius: 3px;
  outline: none;
  background: var(--jade-soft);
  transition: transform .15s ease, box-shadow .15s ease;
}
.lesson-ticket:hover,
.lesson-ticket:focus-visible { transform: translateY(-1px); box-shadow: 0 5px 12px -7px rgba(13, 43, 33, .5); }
.lesson-class,
.lesson-teacher { display: flex; min-width: 0; align-items: center; gap: 4px; }
.lesson-class { color: var(--ink); font-size: 11px; }
.lesson-class strong,
.lesson-teacher span { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.lesson-teacher { margin-top: 3px; color: var(--jade-deep); font-size: 9px; font-weight: 600; }
.lesson-ticket p { overflow: hidden; margin-top: 4px; color: var(--ink-3); font-size: 9px; text-overflow: ellipsis; white-space: nowrap; }
.lesson-ticket.tone-brass { border-left-color: var(--brass); background: var(--brass-soft); }
.lesson-ticket.tone-brass .lesson-teacher { color: var(--brass-ink); }
.lesson-ticket.tone-clay { border-left-color: var(--clay); background: var(--clay-soft); }
.lesson-ticket.tone-clay .lesson-teacher { color: var(--clay); }
.lesson-ticket.tone-blue { border-left-color: #477C91; background: #E6F0F3; }
.lesson-ticket.tone-blue .lesson-teacher { color: #315F72; }
.open-day { display: block; padding-top: 14px; color: #B2BCB3; font-size: 10px; text-align: center; }
.lesson-tooltip { max-width: 260px; line-height: 1.7; }

::v-deep .teacher-calendar .el-calendar__header { display: none; }
::v-deep .teacher-calendar .el-calendar__body { height: 100%; padding: 0; }
::v-deep .teacher-calendar .el-calendar-table { height: 100%; table-layout: fixed; }
::v-deep .teacher-calendar .el-calendar-table th {
  padding: 10px 0;
  border-bottom: 1px solid var(--line);
  background: #F7F9F5;
  color: var(--ink-3);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: .08em;
}
::v-deep .teacher-calendar .el-calendar-table td { border-color: var(--line-soft); }
::v-deep .teacher-calendar .el-calendar-table td.is-selected { background: transparent; }
::v-deep .teacher-calendar .el-calendar-day { height: 100%; min-height: 112px; padding: 0; }

@media (max-width: 1100px) {
  .schedule-console { grid-template-columns: 1fr; }
}

@media (max-width: 720px) {
  .console-controls { align-items: stretch; flex-direction: column; }
  .control-divider { width: 100%; height: 1px; }
  .month-switcher .el-date-editor { flex: 1; width: auto; }
  .calendar-card-head { align-items: flex-start; flex-direction: column; }
  .calendar-card { min-height: 560px; }
}
</style>
