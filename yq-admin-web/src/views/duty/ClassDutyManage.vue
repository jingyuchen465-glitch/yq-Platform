<template>
  <div class="page-container duty-page">
    <header class="page-head">
      <div>
        <p class="eyebrow">SYS:DUTY · DAILY ROSTER</p>
        <h1>值班管理</h1>
      </div>
      <div class="head-stat">
        <b>{{ assignedCount }}/{{ slotCount }}</b>
        <span>当日已安排</span>
      </div>
    </header>

    <section class="duty-console" aria-label="值班查询条件">
      <div class="console-field campus-field">
        <label>校区</label>
        <el-select
          v-model="selectedCampusId"
          filterable
          placeholder="选择校区"
          :disabled="!options.campuses.length"
          @change="fetchDaily"
        >
          <el-option
            v-for="campus in options.campuses"
            :key="campus.id"
            :label="campus.name"
            :value="campus.id"
          />
        </el-select>
      </div>

      <div class="console-rule" aria-hidden="true"></div>

      <div class="console-field date-field">
        <label>值班日期</label>
        <div class="date-switcher">
          <el-button icon="el-icon-arrow-left" aria-label="前一天" @click="changeDate(-1)" />
          <el-date-picker
            v-model="selectedDate"
            type="date"
            value-format="yyyy-MM-dd"
            format="yyyy 年 MM 月 dd 日"
            :clearable="false"
            @change="fetchDaily"
          />
          <el-button icon="el-icon-arrow-right" aria-label="后一天" @click="changeDate(1)" />
        </div>
      </div>

      <el-button class="today-button" plain @click="goToday">回到今天</el-button>
      <div class="console-summary">
        <span>{{ weekdayText }}</span>
        <b>{{ daily.campusName || '请选择校区' }}</b>
        <small>{{ dutyModeText }}</small>
      </div>
    </section>

    <section v-if="isNoDutyDay" class="no-duty-board" aria-label="今日无需值班">
      <i class="el-icon-coffee-cup"></i>
      <div>
        <p>NO DUTY TODAY</p>
        <h2>今日为节假日或配置休息日</h2>
        <span>无需安排校区统一值班、晚自习值班或自习日值班</span>
      </div>
    </section>

    <section v-else class="time-board" aria-label="当日值班时段">
      <div class="time-board-head">
        <div>
          <p>当日时间轨</p>
          <b>{{ isSelfStudyDay ? '今天是自习日，仅安排班级自习值班' : '今天是晚自习值班日' }}</b>
        </div>
        <span><i></i>老师当天有正常课程时会在下拉框中提示</span>
      </div>
      <div class="time-rail" :class="isSelfStudyDay ? 'self-study-only' : 'evening-only'">
        <div class="rail-line"></div>
        <template v-if="isSelfStudyDay">
          <div class="rail-segment self-segment"><span>自习日班级值班</span></div>
          <span class="rail-tick tick-09">09:00</span>
          <span class="rail-tick tick-18">18:00</span>
        </template>
        <template v-else>
          <div class="rail-segment evening-segment"><span>晚自习班级值班</span></div>
          <div class="rail-segment campus-segment"><span>校区统一值班</span></div>
          <span class="rail-tick tick-19">19:00</span>
          <span class="rail-tick tick-21">21:00</span>
          <span class="rail-tick tick-2230">22:30</span>
        </template>
      </div>
    </section>

    <div v-if="daily.unscheduledClasses.length" class="unscheduled-note">
      <i class="el-icon-warning-outline"></i>
      <div>
        <b>未生成课表</b>
        <p>以下班级暂不参与值班安排：{{ unscheduledClassNames }}</p>
      </div>
    </div>

    <main v-if="!isNoDutyDay" v-loading="loading" class="duty-board">
      <section v-if="isEveningStudyDay" class="duty-section campus-duty-section">
        <div class="section-heading">
          <div class="section-index">21:00—22:30</div>
          <div>
            <h2>校区统一值班</h2>
            <p>当前校区统一安排一位值班老师</p>
          </div>
          <span class="type-tag campus-tag">EVENING_STUDY_CAMPUS</span>
        </div>

        <div class="campus-duty-row">
          <div class="campus-mark">
            <i class="el-icon-office-building"></i>
            <span><b>{{ daily.campusName || '当前校区' }}</b><small>全局值班</small></span>
          </div>
          <teacher-picker
            :value="campusDraft.teacherId"
            :teachers="options.teachers"
            :busy-teacher-ids="daily.busyTeacherIds"
            @input="campusDraft.teacherId = $event"
          />
          <el-input
            v-model="campusDraft.remark"
            maxlength="255"
            placeholder="备注（选填）"
          />
          <div class="row-actions">
            <el-button
              type="primary"
              size="small"
              :loading="campusDraft.saving"
              @click="saveRow(campusDraft, 'EVENING_STUDY_CAMPUS')"
            >保存</el-button>
            <el-button
              type="text"
              class="danger-link"
              :disabled="!campusDraft.assignmentId"
              @click="clearRow(campusDraft)"
            >清空</el-button>
          </div>
        </div>
      </section>

      <duty-table-section
        v-if="isEveningStudyDay"
        title="晚自习班级值班"
        subtitle="教学周期内的每个班级可安排一位老师"
        time="19:00—21:00"
        code="EVENING_STUDY_CLASS"
        tone="evening"
        :rows="eveningRows"
        :teachers="options.teachers"
        :busy-teacher-ids="daily.busyTeacherIds"
        @save="saveRow($event, 'EVENING_STUDY_CLASS')"
        @clear="clearRow"
      />

      <duty-table-section
        v-if="isSelfStudyDay"
        title="自习日班级值班"
        subtitle="每日展示，按班级独立安排"
        time="09:00—18:00"
        code="SELF_STUDY_CLASS"
        tone="self"
        :rows="selfStudyRows"
        :teachers="options.teachers"
        :busy-teacher-ids="daily.busyTeacherIds"
        @save="saveRow($event, 'SELF_STUDY_CLASS')"
        @clear="clearRow"
      />

      <el-empty
        v-if="!loading && !options.campuses.length"
        description="暂无校区，请先在校区管理中维护校区"
      />
    </main>
  </div>
</template>

<script>
import { deleteClassDuty, getClassDutyOptions, getDailyClassDuties, saveClassDuty } from '@/api/classDuty'
import TeacherPicker from '@/components/duty/TeacherPicker.vue'
import DutyTableSection from '@/components/duty/DutyTableSection.vue'

function pad(value) {
  return String(value).padStart(2, '0')
}

function formatDate(date) {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

function emptyDaily() {
  return {
    dutyDate: '',
    campusId: null,
    campusName: '',
    dutyMode: 'EVENING_STUDY',
    busyTeacherIds: [],
    campusDuty: null,
    eveningClassDuties: [],
    selfStudyClassDuties: [],
    unscheduledClasses: []
  }
}

export default {
  name: 'ClassDutyManage',
  components: { TeacherPicker, DutyTableSection },
  data() {
    return {
      loading: false,
      requestSequence: 0,
      selectedCampusId: null,
      selectedDate: formatDate(new Date()),
      options: { campuses: [], teachers: [], dutyTypes: [] },
      daily: emptyDaily(),
      campusDraft: this.createDraft(null, null),
      eveningRows: [],
      selfStudyRows: []
    }
  },
  computed: {
    weekdayText() {
      if (!this.selectedDate) return ''
      const date = new Date(`${this.selectedDate}T00:00:00`)
      return ['周日', '周一', '周二', '周三', '周四', '周五', '周六'][date.getDay()]
    },
    isSelfStudyDay() {
      return this.daily.dutyMode === 'SELF_STUDY'
    },
    isEveningStudyDay() {
      return this.daily.dutyMode === 'EVENING_STUDY'
    },
    isNoDutyDay() {
      return this.daily.dutyMode === 'NO_DUTY'
    },
    dutyModeText() {
      if (this.isNoDutyDay) return '休息日 · 无需值班'
      return this.isSelfStudyDay ? '自习日值班' : '晚自习值班'
    },
    unscheduledClassNames() {
      return this.daily.unscheduledClasses.map(item => item.className).join('、')
    },
    slotCount() {
      if (!this.selectedCampusId) return 0
      if (this.isNoDutyDay) return 0
      return (this.isEveningStudyDay ? 1 : 0) + this.eveningRows.length + this.selfStudyRows.length
    },
    assignedCount() {
      return [this.campusDraft, ...this.eveningRows, ...this.selfStudyRows]
        .filter(row => row.assignmentId).length
    }
  },
  created() {
    this.loadOptions()
  },
  methods: {
    createDraft(row, assignment) {
      const source = row || {}
      const duty = assignment || source.assignment || null
      return {
        classId: source.classId || null,
        className: source.className || '',
        teachingStartDate: source.teachingStartDate || '',
        teachingEndDate: source.teachingEndDate || '',
        assignmentId: duty ? duty.id : null,
        teacherId: duty ? duty.teacherId : null,
        remark: duty && duty.remark ? duty.remark : '',
        saving: false
      }
    },
    async loadOptions() {
      try {
        const res = await getClassDutyOptions()
        this.options = Object.assign({ campuses: [], teachers: [], dutyTypes: [] }, res.data || {})
        if (!this.selectedCampusId && this.options.campuses.length) {
          this.selectedCampusId = this.options.campuses[0].id
        }
        await this.fetchDaily()
      } catch (e) {
        // 错误由请求层统一展示
      }
    },
    async fetchDaily() {
      if (!this.selectedCampusId || !this.selectedDate) {
        this.daily = emptyDaily()
        return
      }
      const requestId = ++this.requestSequence
      this.loading = true
      try {
        const res = await getDailyClassDuties({
          campusId: this.selectedCampusId,
          dutyDate: this.selectedDate
        })
        if (requestId !== this.requestSequence) return
        this.daily = Object.assign(emptyDaily(), res.data || {})
        this.campusDraft = this.createDraft(null, this.daily.campusDuty)
        this.eveningRows = this.daily.eveningClassDuties.map(row => this.createDraft(row))
        this.selfStudyRows = this.daily.selfStudyClassDuties.map(row => this.createDraft(row))
      } catch (e) {
        // 错误由请求层统一展示
      } finally {
        if (requestId === this.requestSequence) this.loading = false
      }
    },
    async saveRow(row, dutyType) {
      if (!row.teacherId) {
        this.$message.warning('请先选择值班老师')
        return
      }
      row.saving = true
      try {
        await saveClassDuty({
          campusId: this.selectedCampusId,
          classId: row.classId,
          teacherId: row.teacherId,
          dutyDate: this.selectedDate,
          dutyType,
          remark: row.remark
        })
        this.$message.success('值班安排已保存')
        await this.fetchDaily()
      } catch (e) {
        // 错误由请求层统一展示
      } finally {
        row.saving = false
      }
    },
    async clearRow(row) {
      if (!row.assignmentId) {
        row.teacherId = null
        row.remark = ''
        return
      }
      try {
        await this.$confirm('确定清空这条值班安排吗？', '清空值班', {
          confirmButtonText: '清空',
          cancelButtonText: '取消',
          type: 'warning'
        })
      } catch (e) {
        return
      }
      try {
        await deleteClassDuty(row.assignmentId)
        this.$message.success('值班安排已清空')
        await this.fetchDaily()
      } catch (e) {
        // 错误由请求层统一展示
      }
    },
    changeDate(offset) {
      const date = new Date(`${this.selectedDate}T00:00:00`)
      date.setDate(date.getDate() + offset)
      this.selectedDate = formatDate(date)
      this.fetchDaily()
    },
    goToday() {
      this.selectedDate = formatDate(new Date())
      this.fetchDaily()
    }
  }
}
</script>

<style>
.duty-page { min-height: 100%; }

.duty-console {
  display: grid;
  grid-template-columns: minmax(220px, .8fr) 1px minmax(420px, 1.35fr) auto minmax(150px, .65fr);
  align-items: end;
  gap: 20px;
  padding: 18px 20px;
  margin-bottom: 14px;
  background: var(--card);
  border: 1px solid var(--line-soft);
  border-radius: var(--radius);
  box-shadow: var(--shadow-card);
}
.console-field { display: flex; flex-direction: column; gap: 8px; }
.console-field label {
  color: var(--ink-3);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: .12em;
}
.console-field .el-select { width: 100%; }
.console-rule { width: 1px; height: 40px; background: var(--line); align-self: end; }
.date-switcher { display: grid; grid-template-columns: 40px minmax(220px, 1fr) 40px; gap: 8px; }
.date-switcher .el-date-editor { width: 100%; }
.today-button { height: 40px; }
.console-summary { align-self: center; text-align: right; }
.console-summary span { display: block; color: var(--brass-ink); font-family: var(--font-mono); font-size: 12px; }
.console-summary b { display: block; margin-top: 4px; color: var(--ink); font-size: 14px; }
.console-summary small { display: block; margin-top: 3px; color: var(--jade-deep); font-size: 11px; }

.no-duty-board {
  display: flex;
  align-items: center;
  gap: 18px;
  min-height: 132px;
  padding: 24px 28px;
  background: #E9EFEA;
  border: 1px solid #D8E2DA;
  border-radius: var(--radius);
  color: var(--ink-2);
}
.no-duty-board > i {
  display: grid;
  place-items: center;
  width: 54px;
  height: 54px;
  flex: none;
  border-radius: 50%;
  background: #FFFFFF;
  color: var(--jade-deep);
  font-size: 23px;
  box-shadow: var(--shadow-card);
}
.no-duty-board p {
  margin-bottom: 5px;
  color: var(--brass-ink);
  font-family: var(--font-mono);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: .18em;
}
.no-duty-board h2 { font-size: 19px; color: var(--ink); }
.no-duty-board span { display: block; margin-top: 7px; color: var(--ink-3); font-size: 13px; }

.time-board {
  padding: 17px 20px 14px;
  margin-bottom: 14px;
  background: #102F25;
  border-radius: var(--radius);
  color: #EAF1EC;
  overflow: hidden;
}
.time-board-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 24px; }
.time-board-head p { color: #83A597; font-size: 11px; letter-spacing: .18em; }
.time-board-head b { display: block; margin-top: 4px; font-size: 14px; }
.time-board-head > span { color: #9DB3A9; font-size: 12px; }
.time-board-head > span i { display: inline-block; width: 7px; height: 7px; margin-right: 7px; border-radius: 50%; background: var(--brass); }
.time-rail { position: relative; height: 64px; margin: 11px 2px 0; }
.rail-line { position: absolute; left: 0; right: 0; top: 27px; height: 1px; background: rgba(255, 255, 255, .18); }
.rail-segment { position: absolute; top: 18px; height: 19px; border-radius: 4px; }
.rail-segment span { position: absolute; left: 8px; top: -18px; white-space: nowrap; font-size: 10px; color: #C8D7D0; }
.self-segment { left: 0; width: 66.66%; background: #2A8069; }
.evening-segment { left: 74.07%; width: 14.82%; background: #B98A2F; }
.campus-segment { left: 88.89%; width: 11.11%; background: #A95742; }
.rail-tick { position: absolute; top: 43px; font-family: var(--font-mono); font-size: 10px; color: #7F9A8E; transform: translateX(-50%); }
.tick-09 { left: 0; transform: none; }
.tick-18 { left: 66.66%; }
.tick-19 { left: 74.07%; }
.tick-21 { left: 88.89%; }
.tick-2230 { right: 0; transform: none; }
.time-rail.self-study-only .self-segment { left: 0; width: 100%; }
.time-rail.self-study-only .tick-18 { left: auto; right: 0; transform: none; }
.time-rail.evening-only .evening-segment { left: 0; width: 57.14%; }
.time-rail.evening-only .campus-segment { left: 57.14%; width: 42.86%; }
.time-rail.evening-only .tick-19 { left: 0; transform: none; }
.time-rail.evening-only .tick-21 { left: 57.14%; }

.unscheduled-note {
  display: flex;
  align-items: flex-start;
  gap: 11px;
  padding: 12px 16px;
  margin-bottom: 14px;
  color: #745821;
  background: var(--brass-soft);
  border-left: 3px solid var(--brass);
  border-radius: 5px;
}
.unscheduled-note i { margin-top: 2px; }
.unscheduled-note b { font-size: 13px; }
.unscheduled-note p { margin-top: 3px; font-size: 12px; color: #8A7445; }

.duty-board {
  background: var(--card);
  border: 1px solid var(--line-soft);
  border-radius: var(--radius);
  box-shadow: var(--shadow-card);
  overflow: hidden;
}
.duty-section + .duty-section { border-top: 1px solid var(--line); }
.section-heading {
  display: grid;
  grid-template-columns: 118px minmax(220px, 1fr) auto;
  align-items: center;
  gap: 16px;
  padding: 18px 20px 14px;
}
.section-index { font-family: var(--font-mono); font-size: 12px; color: var(--brass-ink); }
.section-heading h2 { font-size: 17px; line-height: 1.2; }
.section-heading p { margin-top: 4px; color: var(--ink-3); font-size: 12px; }
.type-tag {
  padding: 4px 8px;
  border-radius: 4px;
  font-family: var(--font-mono);
  font-size: 10px;
  color: var(--jade-deep);
  background: var(--jade-soft);
}
.campus-tag { color: #874333; background: #F4E4DF; }
.evening-tag { color: var(--brass-ink); background: var(--brass-soft); }

.campus-duty-row {
  display: grid;
  grid-template-columns: minmax(210px, .8fr) minmax(240px, 1fr) minmax(220px, 1.2fr) 132px;
  align-items: center;
  gap: 14px;
  padding: 14px 20px 20px;
  background: #FBFCFA;
}
.campus-mark { display: flex; align-items: center; gap: 11px; }
.campus-mark > i {
  display: grid;
  place-items: center;
  width: 36px; height: 36px;
  border-radius: 8px;
  color: #874333;
  background: #F4E4DF;
}
.campus-mark span { display: flex; flex-direction: column; gap: 2px; }
.campus-mark b { font-size: 13px; }
.campus-mark small { color: var(--ink-3); }
.row-actions { display: flex; align-items: center; }
.teacher-picker { width: 100%; }
.duty-table-wrap { padding: 0 20px 22px; overflow-x: auto; }
.class-cell { display: flex; align-items: center; gap: 9px; }
.class-cell i { color: var(--jade); }
.period-text { font-family: var(--font-mono); font-size: 11.5px; color: var(--ink-2); }
.teacher-option-name { float: left; }
.teacher-busy { float: right; color: var(--brass-ink); }

@media (max-width: 1100px) {
  .duty-console { grid-template-columns: 1fr 1.4fr auto; }
  .console-rule, .console-summary { display: none; }
  .campus-duty-row { grid-template-columns: 1fr 1fr; }
  .row-actions { justify-content: flex-end; }
}

@media (max-width: 760px) {
  .duty-console { grid-template-columns: 1fr; align-items: stretch; }
  .time-board-head { flex-direction: column; gap: 6px; }
  .time-rail { height: auto; display: grid; gap: 7px; margin-top: 16px; }
  .rail-line, .rail-tick { display: none; }
  .rail-segment { position: relative; inset: auto; width: 100%; height: 24px; }
  .rail-segment span { top: 4px; color: #FFFFFF; }
  .section-heading { grid-template-columns: 1fr; gap: 6px; }
  .type-tag { justify-self: start; }
  .campus-duty-row { grid-template-columns: 1fr; }
  .row-actions { justify-content: flex-start; }
}

@media (prefers-reduced-motion: reduce) {
  .duty-page * { scroll-behavior: auto !important; }
}
</style>
