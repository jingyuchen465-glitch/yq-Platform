<template>
  <div class="page-container config-page">
    <header class="page-head">
      <div>
        <p class="eyebrow">SYS:CONFIG · RULE CONTROL</p>
        <h1>规则配置</h1>
      </div>
      <div class="head-stat">
        <b>{{ assignedDayCount }}/7</b>
        <span>星期已分配</span>
      </div>
    </header>

    <section v-loading="typeLoading" class="config-selector" aria-label="配置类型选择">
      <div class="selector-field">
        <label for="config-type">规则类型</label>
        <el-select
          id="config-type"
          v-model="selectedTypeCode"
          filterable
          placeholder="选择规则类型"
          :disabled="!typeOptions.length"
          @change="handleTypeChange"
        >
          <el-option
            v-for="item in typeOptions"
            :key="item.id"
            :label="item.typeName"
            :value="item.typeCode"
            :disabled="item.status !== 'ACTIVE'"
          >
            <span class="type-option-name">{{ item.typeName }}</span>
            <code>{{ item.typeCode }}</code>
          </el-option>
        </el-select>
      </div>

      <div v-if="selectedType" class="selected-type-meta">
        <code>{{ selectedType.typeCode }}</code>
        <span :class="['st', selectedType.status === 'ACTIVE' ? 'on' : 'off']">
          <i></i>{{ selectedType.status === 'ACTIVE' ? '已启用' : '已停用' }}
        </span>
        <p>{{ selectedType.description || '暂无描述' }}</p>
      </div>

      <el-tooltip content="重新读取规则" placement="top">
        <el-button
          class="reload-button"
          icon="el-icon-refresh"
          aria-label="重新读取规则"
          :loading="ruleLoading"
          :disabled="!isScheduleRule"
          circle
          @click="reloadRule"
        />
      </el-tooltip>
    </section>

    <section
      v-if="isScheduleRule"
      v-loading="ruleLoading"
      class="rule-editor"
      aria-label="排课规则编辑器"
    >
      <div class="editor-heading">
        <div>
          <p class="section-kicker">WEEKLY RHYTHM</p>
          <h2>一周排课节奏</h2>
        </div>
        <div class="rule-summary" aria-label="规则天数统计">
          <span class="summary-class"><i></i>上课 {{ modeCounts.CLASS }}</span>
          <span class="summary-study"><i></i>自习 {{ modeCounts.SELF_STUDY }}</span>
          <span class="summary-rest"><i></i>休息 {{ modeCounts.REST }}</span>
        </div>
      </div>

      <div class="week-track" role="group" aria-label="星期规则分配">
        <article
          v-for="day in weekDays"
          :key="day.value"
          :class="['day-slot', dayModeClass(day.value)]"
        >
          <div class="day-heading">
            <span>0{{ day.value }}</span>
            <b>{{ day.label }}</b>
            <small>{{ day.code }}</small>
          </div>
          <el-radio-group
            v-model="assignments[day.value]"
            class="rule-mode-group"
            size="small"
            :aria-label="`${day.label}安排`"
          >
            <el-radio-button label="CLASS">上课</el-radio-button>
            <el-radio-button label="SELF_STUDY">自习</el-radio-button>
            <el-radio-button label="REST">休息</el-radio-button>
          </el-radio-group>
        </article>
      </div>

      <div class="holiday-policy">
        <div class="holiday-mark" aria-hidden="true">
          <i class="el-icon-sunny"></i>
        </div>
        <div class="holiday-copy">
          <b>法定节假日</b>
          <span>{{ holidayRest ? '按休息日处理' : '继续按星期规则排课' }}</span>
        </div>
        <el-switch
          v-model="holidayRest"
          active-color="#177E63"
          inactive-color="#B9C2B9"
          aria-label="法定节假日休息"
        />
      </div>

      <footer class="editor-actions">
        <span :class="['sync-state', { dirty }]">
          <i></i>{{ dirty ? '有未保存修改' : '已与服务器同步' }}
        </span>
        <div>
          <el-button
            icon="el-icon-refresh-left"
            :disabled="!dirty || saving"
            @click="resetRule"
          >撤销修改</el-button>
          <el-button
            type="primary"
            icon="el-icon-check"
            :loading="saving"
            :disabled="!ruleLoaded || !dirty"
            @click="saveRule"
          >保存规则</el-button>
        </div>
      </footer>
    </section>

    <section v-else-if="selectedTypeCode && !typeLoading" class="unsupported-state">
      <el-empty description="该配置类型暂未提供可视化编辑器" />
    </section>

    <section v-else-if="!typeLoading" class="unsupported-state">
      <el-empty description="暂无可用的规则类型" />
    </section>
  </div>
</template>

<script>
import {
  getClassScheduleRule,
  listConfigTypes,
  updateClassScheduleRule
} from '@/api/sysConfig'

const SCHEDULE_RULE_TYPE = 'CLASS_SCHEDULE_RULE'
const WEEK_DAYS = [
  { value: 1, label: '周一', code: 'MON' },
  { value: 2, label: '周二', code: 'TUE' },
  { value: 3, label: '周三', code: 'WED' },
  { value: 4, label: '周四', code: 'THU' },
  { value: 5, label: '周五', code: 'FRI' },
  { value: 6, label: '周六', code: 'SAT' },
  { value: 7, label: '周日', code: 'SUN' }
]

function emptyAssignments() {
  return WEEK_DAYS.reduce((result, day) => {
    result[day.value] = ''
    return result
  }, {})
}

export default {
  name: 'ConfigManage',
  data() {
    return {
      typeLoading: false,
      ruleLoading: false,
      saving: false,
      typeOptions: [],
      selectedTypeCode: '',
      loadedTypeCode: '',
      weekDays: WEEK_DAYS,
      assignments: emptyAssignments(),
      holidayRest: true,
      savedPayload: null,
      ruleLoaded: false
    }
  },
  computed: {
    selectedType() {
      return this.typeOptions.find(item => item.typeCode === this.selectedTypeCode) || null
    },
    isScheduleRule() {
      return this.selectedTypeCode === SCHEDULE_RULE_TYPE
    },
    modeCounts() {
      return WEEK_DAYS.reduce((counts, day) => {
        const mode = this.assignments[day.value]
        if (mode) counts[mode] += 1
        return counts
      }, { CLASS: 0, SELF_STUDY: 0, REST: 0 })
    },
    assignedDayCount() {
      return this.modeCounts.CLASS + this.modeCounts.SELF_STUDY + this.modeCounts.REST
    },
    currentPayload() {
      return this.buildPayload()
    },
    dirty() {
      if (!this.ruleLoaded || !this.savedPayload) return false
      return this.payloadSignature(this.currentPayload) !== this.payloadSignature(this.savedPayload)
    }
  },
  created() {
    this.loadTypes()
  },
  mounted() {
    window.addEventListener('beforeunload', this.handleBeforeUnload)
  },
  beforeDestroy() {
    window.removeEventListener('beforeunload', this.handleBeforeUnload)
  },
  beforeRouteLeave(to, from, next) {
    if (!this.dirty) {
      next()
      return
    }
    this.confirmDiscard().then(() => next()).catch(() => next(false))
  },
  methods: {
    async loadTypes() {
      this.typeLoading = true
      try {
        const res = await listConfigTypes()
        this.typeOptions = Array.isArray(res.data) ? res.data : []
        const scheduleType = this.typeOptions.find(item => (
          item.typeCode === SCHEDULE_RULE_TYPE && item.status === 'ACTIVE'
        ))
        const firstActiveType = this.typeOptions.find(item => item.status === 'ACTIVE')
        const initialType = scheduleType || firstActiveType || this.typeOptions[0]
        if (initialType) {
          this.selectedTypeCode = initialType.typeCode
          this.loadedTypeCode = initialType.typeCode
          await this.loadSelectedRule()
        }
      } catch (e) {
        this.typeOptions = []
      } finally {
        this.typeLoading = false
      }
    },
    async handleTypeChange(typeCode) {
      if (this.dirty) {
        try {
          await this.confirmDiscard()
        } catch (e) {
          this.selectedTypeCode = this.loadedTypeCode
          return
        }
      }
      this.loadedTypeCode = typeCode
      await this.loadSelectedRule()
    },
    async loadSelectedRule() {
      this.ruleLoaded = false
      this.savedPayload = null
      this.assignments = emptyAssignments()
      if (!this.isScheduleRule) return

      this.ruleLoading = true
      try {
        const res = await getClassScheduleRule()
        this.applyPayload(res.data)
        this.savedPayload = this.buildPayload()
        this.ruleLoaded = true
      } catch (e) {
        this.assignments = emptyAssignments()
      } finally {
        this.ruleLoading = false
      }
    },
    applyPayload(payload) {
      const source = payload || {}
      const assignments = emptyAssignments()
      ;(source.classDays || []).forEach(day => { assignments[day] = 'CLASS' })
      ;(source.selfStudyDays || []).forEach(day => { assignments[day] = 'SELF_STUDY' })
      ;(source.restDays || []).forEach(day => { assignments[day] = 'REST' })
      this.assignments = assignments
      this.holidayRest = source.holidayRest !== false
    },
    buildPayload() {
      const daysFor = mode => WEEK_DAYS
        .filter(day => this.assignments[day.value] === mode)
        .map(day => day.value)
      return {
        classDays: daysFor('CLASS'),
        selfStudyDays: daysFor('SELF_STUDY'),
        restDays: daysFor('REST'),
        holidayRest: this.holidayRest
      }
    },
    payloadSignature(payload) {
      return JSON.stringify(payload || {})
    },
    dayModeClass(day) {
      const mode = this.assignments[day]
      return mode ? `mode-${mode.toLowerCase().replace('_', '-')}` : 'mode-empty'
    },
    async reloadRule() {
      if (this.dirty) {
        try {
          await this.confirmDiscard()
        } catch (e) {
          return
        }
      }
      await this.loadSelectedRule()
    },
    resetRule() {
      if (this.savedPayload) this.applyPayload(this.savedPayload)
    },
    async saveRule() {
      const payload = this.buildPayload()
      if (payload.classDays.length === 0) {
        this.$message.warning('至少需要设置一个上课日')
        return
      }
      if (this.assignedDayCount !== 7) {
        this.$message.warning('请为星期一到星期日完整分配规则')
        return
      }

      this.saving = true
      try {
        await updateClassScheduleRule(payload)
        this.savedPayload = JSON.parse(JSON.stringify(payload))
        this.$message.success('排课规则已保存')
      } catch (e) {
        // 错误由请求层统一展示
      } finally {
        this.saving = false
      }
    },
    confirmDiscard() {
      return this.$confirm('当前规则尚未保存，确定放弃这些修改吗？', '放弃修改', {
        confirmButtonText: '放弃修改',
        cancelButtonText: '继续编辑',
        type: 'warning'
      })
    },
    handleBeforeUnload(event) {
      if (!this.dirty) return
      event.preventDefault()
      event.returnValue = ''
    }
  }
}
</script>

<style scoped>
.config-page { min-height: 100%; }

.config-selector {
  display: grid;
  grid-template-columns: minmax(280px, 420px) minmax(260px, 1fr) 42px;
  align-items: end;
  gap: 24px;
  min-height: 92px;
  padding: 18px 20px;
  margin-bottom: 16px;
  background: var(--card);
  border: 1px solid var(--line-soft);
  border-radius: var(--radius);
  box-shadow: var(--shadow-card);
}
.selector-field { display: flex; flex-direction: column; gap: 8px; }
.selector-field label {
  color: var(--ink-3);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: .12em;
}
.selector-field .el-select { width: 100%; }
.type-option-name { float: left; }
.type-option-name + code {
  float: right;
  color: var(--ink-3);
  font-family: var(--font-mono);
  font-size: 10px;
}
.selected-type-meta {
  display: grid;
  grid-template-columns: auto auto 1fr;
  align-items: center;
  gap: 10px 16px;
  min-width: 0;
  padding-bottom: 3px;
}
.selected-type-meta > code {
  color: var(--jade-deep);
  font-family: var(--font-mono);
  font-size: 11px;
  white-space: nowrap;
}
.selected-type-meta p {
  min-width: 0;
  color: var(--ink-3);
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.reload-button { width: 40px; height: 40px; padding: 0; }

.rule-editor {
  background: var(--card);
  border: 1px solid var(--line-soft);
  border-radius: var(--radius);
  box-shadow: var(--shadow-card);
  overflow: hidden;
}
.editor-heading {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  padding: 22px 24px 18px;
  border-bottom: 1px solid var(--line);
}
.section-kicker {
  margin-bottom: 5px;
  color: var(--brass-ink);
  font-family: var(--font-mono);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: .18em;
}
.editor-heading h2 {
  color: var(--ink);
  font-family: var(--font-display);
  font-size: 19px;
}
.rule-summary { display: flex; align-items: center; gap: 18px; }
.rule-summary span {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  color: var(--ink-2);
  font-size: 12px;
}
.rule-summary i { width: 8px; height: 8px; border-radius: 2px; }
.summary-class i { background: var(--jade); }
.summary-study i { background: var(--brass); }
.summary-rest i { background: #8A9690; }

.week-track {
  display: grid;
  grid-template-columns: repeat(7, minmax(112px, 1fr));
  overflow-x: auto;
}
.day-slot {
  min-width: 112px;
  padding: 18px 14px 20px;
  border-right: 1px solid var(--line-soft);
  box-shadow: inset 0 3px 0 transparent;
  transition: background-color .18s ease, box-shadow .18s ease;
}
.day-slot:last-child { border-right: 0; }
.day-slot.mode-class { background: #F4FAF7; box-shadow: inset 0 3px 0 var(--jade); }
.day-slot.mode-self-study { background: #FFFAF0; box-shadow: inset 0 3px 0 var(--brass); }
.day-slot.mode-rest { background: #F5F7F5; box-shadow: inset 0 3px 0 #8A9690; }
.day-heading {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 4px 8px;
  margin-bottom: 16px;
}
.day-heading span {
  color: var(--ink-3);
  font-family: var(--font-mono);
  font-size: 10px;
}
.day-heading b {
  grid-column: 1;
  color: var(--ink);
  font-size: 16px;
}
.day-heading small {
  grid-column: 2;
  grid-row: 1 / span 2;
  align-self: end;
  color: #A2ACA5;
  font-family: var(--font-mono);
  font-size: 9px;
}
.rule-mode-group { display: grid; gap: 7px; width: 100%; }
::v-deep .rule-mode-group .el-radio-button { display: block; width: 100%; }
::v-deep .rule-mode-group .el-radio-button__inner {
  width: 100%;
  padding: 8px 6px;
  border: 1px solid var(--line) !important;
  border-radius: 5px !important;
  box-shadow: none !important;
  color: var(--ink-2);
  background: rgba(255, 255, 255, .82);
}
::v-deep .rule-mode-group .el-radio-button__orig-radio:checked + .el-radio-button__inner {
  color: #FFFFFF;
  border-color: var(--jade) !important;
  background: var(--jade);
}
::v-deep .mode-self-study .rule-mode-group .el-radio-button__orig-radio:checked + .el-radio-button__inner {
  border-color: var(--brass) !important;
  background: var(--brass-ink);
}
::v-deep .mode-rest .rule-mode-group .el-radio-button__orig-radio:checked + .el-radio-button__inner {
  border-color: #78847E !important;
  background: #78847E;
}

.holiday-policy {
  display: grid;
  grid-template-columns: 42px 1fr auto;
  align-items: center;
  gap: 14px;
  padding: 18px 24px;
  border-top: 1px solid var(--line);
  background: #FBFCFA;
}
.holiday-mark {
  display: grid;
  place-items: center;
  width: 40px;
  height: 40px;
  color: var(--brass-ink);
  background: var(--brass-soft);
  border-radius: 7px;
  font-size: 18px;
}
.holiday-copy { display: flex; flex-direction: column; gap: 3px; }
.holiday-copy b { color: var(--ink); font-size: 14px; }
.holiday-copy span { color: var(--ink-3); font-size: 12px; }

.editor-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  min-height: 70px;
  padding: 14px 24px;
  border-top: 1px solid var(--line);
}
.sync-state {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: var(--ink-3);
  font-size: 12px;
}
.sync-state i {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--jade);
  box-shadow: 0 0 0 3px var(--jade-soft);
}
.sync-state.dirty { color: var(--brass-ink); }
.sync-state.dirty i { background: var(--brass); box-shadow: 0 0 0 3px var(--brass-soft); }
.editor-actions > div { display: flex; gap: 8px; }

.unsupported-state {
  display: grid;
  place-items: center;
  min-height: 360px;
  background: var(--card);
  border: 1px solid var(--line-soft);
  border-radius: var(--radius);
}

@media (max-width: 1050px) {
  .config-selector { grid-template-columns: minmax(240px, 1fr) minmax(240px, 1fr) 42px; }
  .selected-type-meta p { display: none; }
}

@media (max-width: 760px) {
  .config-selector { grid-template-columns: 1fr auto; gap: 14px; }
  .selected-type-meta { grid-column: 1 / -1; grid-row: 2; }
  .reload-button { grid-column: 2; grid-row: 1; }
  .editor-heading { align-items: flex-start; flex-direction: column; }
  .rule-summary { width: 100%; justify-content: space-between; gap: 8px; }
  .week-track { grid-template-columns: 1fr; overflow: visible; }
  .day-slot {
    display: grid;
    grid-template-columns: 92px 1fr;
    align-items: center;
    gap: 12px;
    min-width: 0;
    padding: 13px 16px;
    border-right: 0;
    border-bottom: 1px solid var(--line-soft);
    box-shadow: inset 3px 0 0 transparent;
  }
  .day-slot.mode-class { box-shadow: inset 3px 0 0 var(--jade); }
  .day-slot.mode-self-study { box-shadow: inset 3px 0 0 var(--brass); }
  .day-slot.mode-rest { box-shadow: inset 3px 0 0 #8A9690; }
  .day-heading { margin-bottom: 0; }
  .rule-mode-group { grid-template-columns: repeat(3, 1fr); gap: 5px; }
  .holiday-policy { padding: 16px; }
  .editor-actions { align-items: stretch; flex-direction: column; padding: 16px; }
  .editor-actions > div { display: grid; grid-template-columns: 1fr 1fr; }
}

@media (max-width: 420px) {
  .day-slot { grid-template-columns: 72px 1fr; padding: 12px; }
  ::v-deep .rule-mode-group .el-radio-button__inner { padding: 8px 2px; }
  .holiday-policy { grid-template-columns: 36px 1fr auto; gap: 10px; }
  .holiday-mark { width: 34px; height: 34px; }
}
</style>
