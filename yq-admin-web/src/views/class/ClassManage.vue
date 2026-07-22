<template>
  <div class="page-container">
    <header class="page-head">
      <div>
        <p class="eyebrow">SYS:CLASS · COHORT</p>
        <h1>班级管理</h1>
      </div>
      <div class="head-stat">
        <b>{{ total }}</b>
        <span>个班级</span>
      </div>
    </header>

    <div class="search-bar">
      <el-form :inline="true" :model="queryParams" @submit.native.prevent>
        <el-form-item label="班级期数">
          <el-input v-model="queryParams.classPeriod" placeholder="请输入班级期数" clearable @keyup.enter.native="handleSearch" />
        </el-form-item>
        <el-form-item label="校区">
          <el-select v-model="queryParams.campusId" placeholder="全部校区" clearable filterable @change="handleSearch">
            <el-option v-for="item in formOptions.campuses" :key="item.id" :label="item.label" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="班主任">
          <el-select v-model="queryParams.headTeacherId" placeholder="全部班主任" clearable filterable @change="handleSearch">
            <el-option v-for="item in formOptions.headTeachers" :key="item.id" :label="item.label" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="课程">
          <el-select v-model="queryParams.courseId" placeholder="全部线下课程" clearable filterable @change="handleSearch">
            <el-option v-for="item in formOptions.courses" :key="item.id" :label="item.label" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" @click="handleSearch">查询</el-button>
          <el-button icon="el-icon-refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="table-card">
      <div class="table-actions">
        <el-button type="primary" icon="el-icon-plus" @click="handleAdd">新增班级</el-button>
      </div>

      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column label="ID" width="70" align="center">
          <template slot-scope="{ row }"><code class="mono">{{ row.id }}</code></template>
        </el-table-column>
        <el-table-column label="班级期数" min-width="160">
          <template slot-scope="{ row }"><span class="period-chip">{{ row.classPeriod }}</span></template>
        </el-table-column>
        <el-table-column prop="campusLocation" label="校区" min-width="180" show-overflow-tooltip>
          <template slot-scope="{ row }">{{ row.campusLocation || '-' }}</template>
        </el-table-column>
        <el-table-column prop="headTeacherName" label="班主任" min-width="150" show-overflow-tooltip>
          <template slot-scope="{ row }">{{ row.headTeacherName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="courseName" label="课程" min-width="180" show-overflow-tooltip>
          <template slot-scope="{ row }">{{ row.courseName || '-' }}</template>
        </el-table-column>
        <el-table-column label="创建时间" width="170" align="center">
          <template slot-scope="{ row }"><code class="mono">{{ row.createdAt }}</code></template>
        </el-table-column>
        <el-table-column label="操作" width="340" align="center" fixed="right">
          <template slot-scope="{ row }">
            <el-button type="text" icon="el-icon-view" @click="handleViewSchedule(row)">查看课表</el-button>
            <el-button type="text" icon="el-icon-date" @click="handleGenerate(row)">生成课表</el-button>
            <el-button type="text" icon="el-icon-edit" @click="handleEdit(row)">编辑</el-button>
            <el-button type="text" icon="el-icon-delete" class="danger-link" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          :current-page.sync="queryParams.current"
          :page-size.sync="queryParams.size"
          :page-sizes="[10, 20, 50, 100]"
          @size-change="fetchData"
          @current-change="fetchData"
        />
      </div>
    </div>

    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="520px" @closed="resetForm">
      <el-form ref="classForm" :model="form" :rules="formRules" label-width="92px" class="dialog-form">
        <el-form-item label="班级期数" prop="classPeriod">
          <el-input v-model="form.classPeriod" placeholder="请输入班级期数" maxlength="64" show-word-limit />
        </el-form-item>
        <el-form-item label="校区" prop="campusId">
          <el-select v-model="form.campusId" placeholder="请选择校区" filterable :loading="optionsLoading" no-data-text="暂无可选校区" class="full-select">
            <el-option v-for="item in formOptions.campuses" :key="item.id" :label="item.label" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="班主任" prop="headTeacherId">
          <el-select v-model="form.headTeacherId" placeholder="请选择班主任" filterable :loading="optionsLoading" no-data-text="暂无COORDINATOR角色用户" class="full-select">
            <el-option v-for="item in formOptions.headTeachers" :key="item.id" :label="item.label" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="课程" prop="courseId">
          <el-select v-model="form.courseId" placeholder="请选择线下课程" filterable :loading="optionsLoading" no-data-text="暂无线下课程" class="full-select">
            <el-option v-for="item in formOptions.courses" :key="item.id" :label="item.label" :value="item.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确 定</el-button>
      </div>
    </el-dialog>

    <el-dialog title="生成课程表" :visible.sync="scheduleDialogVisible" width="480px" @closed="resetScheduleForm">
      <div v-if="selectedClass" class="schedule-target">
        <span>{{ selectedClass.classPeriod }}</span>
        <small>{{ selectedClass.courseName }} · {{ selectedClass.campusLocation }}</small>
      </div>
      <el-alert
        title="重新生成将替换该班级已有课程表"
        type="warning"
        :closable="false"
        show-icon
        class="schedule-warning"
      />
      <el-form ref="scheduleForm" :model="scheduleForm" :rules="scheduleRules" label-width="126px">
        <el-form-item label="第一天上课日期" prop="startDate">
          <el-date-picker
            v-model="scheduleForm.startDate"
            type="date"
            value-format="yyyy-MM-dd"
            format="yyyy 年 MM 月 dd 日"
            placeholder="请选择日期"
            :clearable="false"
            class="full-select"
          />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="scheduleDialogVisible = false">取 消</el-button>
        <el-button type="primary" icon="el-icon-date" :loading="scheduleLoading" @click="submitSchedule">生成课表</el-button>
      </div>
    </el-dialog>

    <el-dialog
      :title="viewClass ? `${viewClass.classPeriod} · 课程表` : '课程表'"
      :visible.sync="scheduleViewVisible"
      width="1050px"
      top="5vh"
      custom-class="schedule-calendar-dialog"
      @closed="resetScheduleView"
    >
      <div v-loading="scheduleViewLoading" class="schedule-view">
        <template v-if="scheduleRows.length">
          <div class="calendar-toolbar">
            <div class="calendar-summary">
              <b>{{ viewClass && viewClass.courseName }}</b>
              <span>{{ scheduleRows.length }} 个日程 · {{ classScheduleCount }} 个上课日</span>
            </div>
            <div class="calendar-legend" aria-label="课表类型图例">
              <span><i class="legend-class"></i>上课</span>
              <span><i class="legend-self-study"></i>自习</span>
              <span><i class="legend-rest"></i>休息</span>
              <span><i class="legend-holiday"></i>节假日</span>
            </div>
          </div>
          <div class="calendar-scroll">
            <el-calendar v-model="calendarValue" class="schedule-calendar">
              <template slot="dateCell" slot-scope="{ data }">
                <div
                  class="calendar-cell"
                  :class="[data.type, getScheduleClass(scheduleMap[data.day])]"
                >
                  <span class="calendar-day-number">{{ getDayNumber(data.day) }}</span>
                  <el-tooltip
                    v-if="scheduleMap[data.day]"
                    :content="getScheduleTooltip(scheduleMap[data.day])"
                    placement="top"
                    :open-delay="350"
                  >
                    <div class="calendar-entry">
                      <span class="calendar-type">{{ scheduleMap[data.day].classTypeName }}</span>
                      <strong v-if="scheduleMap[data.day].stageName" class="calendar-stage">
                        {{ scheduleMap[data.day].stageName }}
                      </strong>
                      <p>{{ scheduleMap[data.day].courseContent }}</p>
                    </div>
                  </el-tooltip>
                </div>
              </template>
            </el-calendar>
          </div>
        </template>
        <el-empty v-else-if="!scheduleViewLoading" description="暂无课程表">
          <el-button type="primary" icon="el-icon-date" @click="generateFromEmpty">生成课表</el-button>
        </el-empty>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { addClass, deleteClass, generateClassSchedule, getClassFormOptions, listClassSchedule, pageClass, updateClass } from '@/api/class'

const emptyQuery = () => ({
  current: 1,
  size: 10,
  classPeriod: '',
  campusId: null,
  headTeacherId: null,
  courseId: null
})

const emptyForm = () => ({
  classPeriod: '',
  campusId: null,
  headTeacherId: null,
  courseId: null
})

export default {
  name: 'ClassManage',
  data() {
    return {
      loading: false,
      optionsLoading: false,
      submitLoading: false,
      tableData: [],
      total: 0,
      queryParams: emptyQuery(),
      formOptions: { campuses: [], headTeachers: [], courses: [] },
      dialogVisible: false,
      scheduleDialogVisible: false,
      scheduleViewVisible: false,
      isEdit: false,
      editId: null,
      selectedClass: null,
      pendingScheduleViewClass: null,
      scheduleLoading: false,
      scheduleViewLoading: false,
      viewClass: null,
      scheduleRows: [],
      calendarValue: new Date(),
      scheduleForm: {
        startDate: ''
      },
      scheduleRules: {
        startDate: [{ required: true, message: '请选择第一天上课日期', trigger: 'change' }]
      },
      form: emptyForm(),
      formRules: {
        classPeriod: [
          { required: true, message: '请输入班级期数', trigger: 'blur' },
          { max: 64, message: '长度不能超过64个字符', trigger: 'blur' }
        ],
        campusId: [{ required: true, message: '请选择校区', trigger: 'change' }],
        headTeacherId: [{ required: true, message: '请选择班主任', trigger: 'change' }],
        courseId: [{ required: true, message: '请选择线下课程', trigger: 'change' }]
      }
    }
  },
  computed: {
    dialogTitle() {
      return this.isEdit ? '编辑班级' : '新增班级'
    },
    scheduleMap() {
      return this.scheduleRows.reduce((result, item) => {
        result[item.scheduleDate] = item
        return result
      }, {})
    },
    classScheduleCount() {
      return this.scheduleRows.filter(item => item.classType === 'CLASS').length
    }
  },
  created() {
    this.fetchOptions()
    this.fetchData()
  },
  methods: {
    async fetchData() {
      this.loading = true
      try {
        const res = await pageClass(this.queryParams)
        this.tableData = res.data.records || []
        this.total = res.data.total || 0
      } catch (e) {
        // 错误已由请求拦截器统一处理
      } finally {
        this.loading = false
      }
    },
    async fetchOptions() {
      this.optionsLoading = true
      try {
        const res = await getClassFormOptions()
        this.formOptions = {
          campuses: res.data.campuses || [],
          headTeachers: res.data.headTeachers || [],
          courses: res.data.courses || []
        }
      } catch (e) {
        // 错误已由请求拦截器统一处理
      } finally {
        this.optionsLoading = false
      }
    },
    handleSearch() {
      this.queryParams.current = 1
      this.fetchData()
    },
    handleReset() {
      this.queryParams = emptyQuery()
      this.fetchData()
    },
    handleAdd() {
      this.isEdit = false
      this.editId = null
      this.form = emptyForm()
      this.dialogVisible = true
    },
    handleEdit(row) {
      this.isEdit = true
      this.editId = row.id
      this.form = {
        classPeriod: row.classPeriod,
        campusId: row.campusId,
        headTeacherId: row.headTeacherId,
        courseId: row.courseId
      }
      this.dialogVisible = true
    },
    handleGenerate(row) {
      this.selectedClass = row
      this.scheduleForm = { startDate: '' }
      this.scheduleDialogVisible = true
    },
    async handleViewSchedule(row) {
      this.viewClass = row
      this.scheduleRows = []
      this.scheduleViewVisible = true
      this.scheduleViewLoading = true
      try {
        const res = await listClassSchedule(row.id)
        this.scheduleRows = res.data || []
        if (this.scheduleRows.length) {
          this.calendarValue = this.toLocalDate(this.scheduleRows[0].scheduleDate)
        }
      } catch (e) {
        // 错误已由请求拦截器统一处理
      } finally {
        this.scheduleViewLoading = false
      }
    },
    generateFromEmpty() {
      const currentClass = this.viewClass
      this.scheduleViewVisible = false
      this.$nextTick(() => this.handleGenerate(currentClass))
    },
    toLocalDate(dateText) {
      const parts = dateText.split('-').map(Number)
      return new Date(parts[0], parts[1] - 1, parts[2])
    },
    getDayNumber(dateText) {
      return Number(dateText.slice(-2))
    },
    getScheduleClass(schedule) {
      return schedule ? `schedule-${schedule.classType.toLowerCase().split('_').join('-')}` : ''
    },
    getScheduleTooltip(schedule) {
      return schedule.stageName
        ? `${schedule.stageName}：${schedule.courseContent}`
        : schedule.courseContent
    },
    submitSchedule() {
      this.$refs.scheduleForm.validate(async valid => {
        if (!valid) return
        this.scheduleLoading = true
        try {
          const res = await generateClassSchedule({
            classId: this.selectedClass.id,
            startDate: this.scheduleForm.startDate
          })
          const result = res.data
          this.$message.success(`课表已生成：${result.startDate} 至 ${result.endDate}，共 ${result.classDays} 个上课日`)
          this.pendingScheduleViewClass = this.selectedClass
          this.scheduleDialogVisible = false
        } catch (e) {
          // 错误已由请求拦截器统一处理
        } finally {
          this.scheduleLoading = false
        }
      })
    },
    handleSubmit() {
      this.$refs.classForm.validate(async valid => {
        if (!valid) return
        this.submitLoading = true
        try {
          if (this.isEdit) {
            await updateClass(this.editId, this.form)
            this.$message.success('修改成功')
          } else {
            await addClass(this.form)
            this.$message.success('新增成功')
          }
          this.dialogVisible = false
          this.fetchData()
        } catch (e) {
          // 错误已由请求拦截器统一处理
        } finally {
          this.submitLoading = false
        }
      })
    },
    resetForm() {
      this.form = emptyForm()
      this.$nextTick(() => {
        this.$refs.classForm && this.$refs.classForm.clearValidate()
      })
    },
    resetScheduleForm() {
      const scheduleViewClass = this.pendingScheduleViewClass
      this.pendingScheduleViewClass = null
      this.selectedClass = null
      this.scheduleForm = { startDate: '' }
      this.$nextTick(() => {
        this.$refs.scheduleForm && this.$refs.scheduleForm.clearValidate()
        if (scheduleViewClass) {
          this.handleViewSchedule(scheduleViewClass)
        }
      })
    },
    resetScheduleView() {
      this.viewClass = null
      this.scheduleRows = []
      this.calendarValue = new Date()
    },
    handleDelete(row) {
      this.$confirm(`确定要删除班级「${row.classPeriod}」吗？`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        await deleteClass(row.id)
        this.$message.success('删除成功')
        this.fetchData()
      }).catch(() => {})
    }
  }
}
</script>

<style scoped>
.table-actions { margin-bottom: 14px; }
.full-select { width: 100%; }
.schedule-target {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-bottom: 16px;
  padding: 10px 12px;
  border-left: 3px solid var(--brass);
  background: #FBFCFA;
}
.schedule-target span { color: var(--ink); font-weight: 700; }
.schedule-target small { color: var(--ink-3); }
.schedule-warning { margin-bottom: 20px; }
.schedule-view { min-height: 360px; }
.calendar-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 2px 0 14px;
  border-bottom: 1px solid var(--line-soft);
}
.calendar-summary { display: flex; flex-direction: column; gap: 4px; }
.calendar-summary b { color: var(--ink); font-size: 15px; }
.calendar-summary span { color: var(--ink-3); font-size: 12px; }
.calendar-legend { display: flex; align-items: center; flex-wrap: wrap; gap: 14px; }
.calendar-legend span { display: inline-flex; align-items: center; gap: 6px; color: var(--ink-2); font-size: 12px; }
.calendar-legend i { width: 9px; height: 9px; border-radius: 2px; }
.legend-class { background: var(--jade); }
.legend-self-study { background: var(--brass); }
.legend-rest { background: #9AA79F; }
.legend-holiday { background: var(--clay); }
.calendar-scroll { overflow-x: auto; }
.schedule-calendar { min-width: 780px; }
.calendar-cell {
  height: 100%;
  padding: 7px 8px;
  border-left: 3px solid transparent;
  transition: background-color .16s ease;
}
.calendar-cell.prev-month,
.calendar-cell.next-month { opacity: .42; }
.calendar-day-number {
  display: block;
  margin-bottom: 5px;
  color: var(--ink-3);
  font-family: var(--font-mono);
  font-size: 12px;
}
.calendar-entry { min-width: 0; }
.calendar-type {
  display: inline-block;
  margin-bottom: 5px;
  padding: 1px 6px;
  border-radius: 3px;
  font-size: 10px;
  font-weight: 700;
}
.calendar-stage {
  display: block;
  overflow: hidden;
  margin-bottom: 3px;
  color: var(--ink);
  font-size: 11px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.calendar-entry p {
  display: -webkit-box;
  overflow: hidden;
  color: var(--ink-2);
  font-size: 12px;
  line-height: 1.45;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}
.calendar-cell.schedule-class { border-left-color: var(--jade); background: rgba(226, 241, 234, .56); }
.calendar-cell.schedule-class .calendar-type { color: var(--jade-deep); background: var(--jade-soft); }
.calendar-cell.schedule-self-study { border-left-color: var(--brass); background: rgba(245, 235, 212, .48); }
.calendar-cell.schedule-self-study .calendar-type { color: var(--brass-ink); background: var(--brass-soft); }
.calendar-cell.schedule-rest { border-left-color: #9AA79F; background: #F5F7F4; }
.calendar-cell.schedule-rest .calendar-type { color: var(--ink-3); background: #E8ECE7; }
.calendar-cell.schedule-holiday { border-left-color: var(--clay); background: rgba(248, 231, 227, .58); }
.calendar-cell.schedule-holiday .calendar-type { color: var(--clay); background: var(--clay-soft); }
.period-chip {
  display: inline-flex;
  align-items: center;
  min-height: 24px;
  padding: 2px 9px;
  border-left: 3px solid var(--brass);
  border-radius: 3px;
  background: var(--brass-soft);
  color: var(--brass-ink);
  font-weight: 600;
}

@media (max-width: 600px) {
  ::v-deep .el-dialog {
    width: calc(100% - 28px) !important;
  }
  .calendar-toolbar { align-items: flex-start; flex-direction: column; gap: 10px; }
}

::v-deep .schedule-calendar-dialog .el-dialog__body {
  max-height: 82vh;
  overflow-y: auto;
  padding-top: 18px;
}
::v-deep .schedule-calendar .el-calendar__header { padding: 16px 0; }
::v-deep .schedule-calendar .el-calendar__body { padding: 0; }
::v-deep .schedule-calendar .el-calendar-table .el-calendar-day { height: 112px; padding: 0; }
</style>
