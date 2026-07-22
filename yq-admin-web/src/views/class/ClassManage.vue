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
        title="重新生成将替换已有课程表，并清除教师分配和临时课程"
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
            <div class="calendar-toolbar-actions">
              <div class="calendar-legend" aria-label="课表类型图例">
                <span><i class="legend-class"></i>上课</span>
                <span><i class="legend-self-study"></i>自习</span>
                <span><i class="legend-rest"></i>休息</span>
                <span><i class="legend-holiday"></i>节假日</span>
              </div>
              <el-button type="primary" plain size="small" icon="el-icon-circle-plus-outline" @click="openTemporaryCourse">
                临时加课
              </el-button>
              <el-button type="primary" size="small" icon="el-icon-user" @click="openTeacherAssignment">
                分配教师
              </el-button>
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
                    <div
                      class="calendar-entry is-actionable"
                      role="button"
                      tabindex="0"
                      @click.stop="openScheduleDetail(scheduleMap[data.day])"
                      @keydown.enter.stop="openScheduleDetail(scheduleMap[data.day])"
                    >
                      <span class="calendar-type">{{ scheduleMap[data.day].classTypeName }}</span>
                      <strong v-if="scheduleMap[data.day].stageName" class="calendar-stage">
                        {{ scheduleMap[data.day].stageName }}
                      </strong>
                      <p>{{ scheduleMap[data.day].courseContent }}</p>
                      <span v-if="scheduleMap[data.day].teacherName" class="calendar-teacher">
                        <i class="el-icon-user"></i>{{ scheduleMap[data.day].teacherName }}
                      </span>
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

    <el-dialog
      title="课程日程详情"
      :visible.sync="scheduleDetailVisible"
      width="560px"
      append-to-body
      @closed="resetScheduleDetail"
    >
      <div v-loading="scheduleDetailLoading" class="schedule-detail-body">
        <template v-if="selectedSchedule">
          <div class="schedule-detail-head" :class="getScheduleClass(selectedSchedule)">
            <div>
              <span>{{ selectedSchedule.scheduleDate }}</span>
              <b>{{ selectedSchedule.classTypeName }}</b>
            </div>
            <strong>{{ selectedSchedule.stageName || selectedSchedule.courseContent }}</strong>
          </div>

          <el-form
            v-if="scheduleDetailEditing"
            ref="scheduleEditForm"
            :model="scheduleEditForm"
            :rules="scheduleEditRules"
            label-width="92px"
          >
            <el-form-item label="上课日期">
              <el-input :value="selectedSchedule.scheduleDate" disabled />
            </el-form-item>
            <el-form-item label="课程阶段" prop="stageName">
              <el-select v-model="scheduleEditForm.stageName" filterable class="full-select" placeholder="请选择课程阶段">
                <el-option
                  v-for="stage in scheduleEditOptions.stages"
                  :key="stage.stageName"
                  :label="stage.label"
                  :value="stage.stageName"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="课程内容" prop="courseContent">
              <el-input
                v-model="scheduleEditForm.courseContent"
                type="textarea"
                :rows="3"
                maxlength="500"
                show-word-limit
                placeholder="请输入课程内容"
              />
            </el-form-item>
            <el-form-item label="授课教师" prop="teacherId">
              <el-select v-model="scheduleEditForm.teacherId" filterable class="full-select" placeholder="请选择授课教师">
                <el-option
                  v-for="teacher in scheduleEditOptions.teachers"
                  :key="teacher.id"
                  :label="teacher.label"
                  :value="teacher.id"
                />
              </el-select>
            </el-form-item>
          </el-form>

          <el-descriptions v-else :column="1" border class="schedule-descriptions">
            <el-descriptions-item label="上课日期">{{ selectedSchedule.scheduleDate }}</el-descriptions-item>
            <el-descriptions-item label="课程阶段">{{ selectedSchedule.stageName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="课程内容">{{ selectedSchedule.courseContent || '-' }}</el-descriptions-item>
            <el-descriptions-item label="授课教师">{{ selectedSchedule.teacherName || '未分配' }}</el-descriptions-item>
            <el-descriptions-item label="日程类型">{{ selectedSchedule.classTypeName }}</el-descriptions-item>
          </el-descriptions>
        </template>
      </div>

      <div slot="footer" class="schedule-detail-actions">
        <template v-if="selectedSchedule && selectedSchedule.classType === 'CLASS'">
          <el-button
            v-if="!scheduleDetailEditing"
            type="danger"
            plain
            icon="el-icon-delete"
            :loading="scheduleDeleteLoading"
            @click="deleteSelectedSchedule"
          >
            删除课程
          </el-button>
          <span class="action-spacer"></span>
          <el-button v-if="scheduleDetailEditing" @click="cancelScheduleEdit">取 消</el-button>
          <template v-else>
            <el-button @click="scheduleDetailVisible = false">关 闭</el-button>
            <el-button icon="el-icon-edit" @click="startScheduleEdit">编辑</el-button>
          </template>
          <el-button
            v-if="scheduleDetailEditing"
            type="primary"
            icon="el-icon-check"
            :loading="scheduleEditSubmitting"
            @click="submitScheduleEdit"
          >
            保存修改
          </el-button>
        </template>
        <el-button v-else @click="scheduleDetailVisible = false">关 闭</el-button>
      </div>
    </el-dialog>

    <el-dialog
      title="临时增加课程"
      :visible.sync="temporaryCourseVisible"
      width="580px"
      append-to-body
      @closed="resetTemporaryCourse"
    >
      <div v-if="viewClass" class="schedule-target">
        <span>{{ viewClass.classPeriod }}</span>
        <small>{{ viewClass.courseName }} · {{ viewClass.campusLocation }}</small>
      </div>
      <el-form
        ref="temporaryCourseForm"
        v-loading="temporaryCourseLoading"
        :model="temporaryCourseForm"
        :rules="temporaryCourseRules"
        label-width="92px"
      >
        <el-form-item label="课程阶段" prop="stageName">
          <el-select
            v-model="temporaryCourseForm.stageName"
            placeholder="请选择课程阶段"
            filterable
            class="full-select"
            no-data-text="当前课程暂无阶段"
          >
            <el-option
              v-for="stage in temporaryCourseOptions.stages"
              :key="stage.stageName"
              :label="stage.label"
              :value="stage.stageName"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="授课教师" prop="teacherId">
          <el-select
            v-model="temporaryCourseForm.teacherId"
            placeholder="请选择讲师"
            filterable
            class="full-select"
            no-data-text="暂无LECTURER角色用户"
          >
            <el-option
              v-for="teacher in temporaryCourseOptions.teachers"
              :key="teacher.id"
              :label="teacher.label"
              :value="teacher.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="加课日期" prop="scheduleDate">
          <el-date-picker
            v-model="temporaryCourseForm.scheduleDate"
            type="date"
            value-format="yyyy-MM-dd"
            format="yyyy 年 MM 月 dd 日"
            placeholder="请选择加课日期"
            :clearable="false"
            class="full-select"
          />
        </el-form-item>
      </el-form>

      <div v-if="temporaryCourseForm.scheduleDate" class="temporary-impact" :class="{ 'will-shift': temporaryCourseWillShift }">
        <i :class="temporaryCourseWillShift ? 'el-icon-sort' : 'el-icon-circle-check'"></i>
        <div>
          <b>{{ temporaryCourseWillShift ? '当天已有课程，将自动顺延' : '当天没有课程，可直接加课' }}</b>
          <span v-if="temporaryCourseWillShift">
            {{ temporaryCourseForm.scheduleDate }} 当天原课程及其后的课程会依次移动到后续上课日，已分配教师也会重新校验时间冲突。
          </span>
          <span v-else>提交时仍会校验所选教师当天是否已在其他班级授课。</span>
        </div>
      </div>

      <div slot="footer">
        <el-button @click="temporaryCourseVisible = false">取 消</el-button>
        <el-button
          type="primary"
          icon="el-icon-circle-plus-outline"
          :loading="temporaryCourseSubmitting"
          @click="submitTemporaryCourse"
        >
          确认加课
        </el-button>
      </div>
    </el-dialog>

    <el-dialog
      title="按阶段分配教师"
      :visible.sync="teacherAssignmentVisible"
      width="560px"
      append-to-body
      @closed="resetTeacherAssignment"
    >
      <div v-if="viewClass" class="schedule-target">
        <span>{{ viewClass.classPeriod }}</span>
        <small>{{ viewClass.courseName }} · {{ viewClass.campusLocation }}</small>
      </div>
      <el-form
        ref="teacherAssignmentForm"
        v-loading="teacherAssignmentLoading"
        :model="teacherAssignmentForm"
        :rules="teacherAssignmentRules"
        label-width="92px"
      >
        <el-form-item label="课程阶段" prop="stageName">
          <el-select
            v-model="teacherAssignmentForm.stageName"
            placeholder="请选择课程阶段"
            filterable
            class="full-select"
            @change="handleAssignmentStageChange"
          >
            <el-option
              v-for="stage in teacherAssignmentOptions.stages"
              :key="stage.stageName"
              :label="stage.stageName"
              :value="stage.stageName"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="授课教师" prop="teacherId">
          <el-select
            v-model="teacherAssignmentForm.teacherId"
            placeholder="请选择讲师"
            filterable
            class="full-select"
            no-data-text="暂无LECTURER角色用户"
          >
            <el-option
              v-for="teacher in teacherAssignmentOptions.teachers"
              :key="teacher.id"
              :label="getTeacherOptionLabel(teacher)"
              :value="teacher.id"
              :disabled="isTeacherOccupied(teacher)"
            />
          </el-select>
        </el-form-item>
      </el-form>

      <div v-if="selectedAssignmentStage" class="assignment-stage-panel">
        <div>
          <span>阶段课时</span>
          <b>{{ selectedAssignmentStage.classDates.length }} 天</b>
        </div>
        <div>
          <span>日期范围</span>
          <b>{{ selectedAssignmentStage.classDates[0] }} 至 {{ selectedAssignmentStage.classDates[selectedAssignmentStage.classDates.length - 1] }}</b>
        </div>
        <div>
          <span>当前教师</span>
          <b>{{ selectedAssignmentStage.teacherName || '未分配' }}</b>
        </div>
      </div>
      <el-alert
        v-if="selectedAssignmentStage && occupiedTeacherCount"
        :title="`${occupiedTeacherCount} 位讲师在该阶段的上课日期已有其他班级课程，已禁止选择`"
        type="warning"
        :closable="false"
        show-icon
        class="assignment-alert"
      />

      <div slot="footer">
        <el-button @click="teacherAssignmentVisible = false">取 消</el-button>
        <el-button
          type="primary"
          icon="el-icon-check"
          :loading="teacherAssignmentSubmitting"
          @click="submitTeacherAssignment"
        >
          分配教师
        </el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  addClass,
  addTemporaryCourse,
  assignScheduleTeacher,
  deleteClass,
  deleteClassSchedule,
  generateClassSchedule,
  getClassFormOptions,
  getClassSchedule,
  getScheduleTeacherAssignmentOptions,
  getTemporaryCourseOptions,
  listClassSchedule,
  pageClass,
  updateClass,
  updateClassSchedule
} from '@/api/class'

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
      teacherAssignmentVisible: false,
      teacherAssignmentLoading: false,
      teacherAssignmentSubmitting: false,
      teacherAssignmentOptions: { stages: [], teachers: [] },
      teacherAssignmentForm: {
        stageName: '',
        teacherId: null
      },
      teacherAssignmentRules: {
        stageName: [{ required: true, message: '请选择课程阶段', trigger: 'change' }],
        teacherId: [{ required: true, message: '请选择授课教师', trigger: 'change' }]
      },
      temporaryCourseVisible: false,
      temporaryCourseLoading: false,
      temporaryCourseSubmitting: false,
      temporaryCourseOptions: { stages: [], teachers: [] },
      temporaryCourseForm: {
        stageName: '',
        teacherId: null,
        scheduleDate: ''
      },
      temporaryCourseRules: {
        stageName: [{ required: true, message: '请选择课程阶段', trigger: 'change' }],
        teacherId: [{ required: true, message: '请选择授课教师', trigger: 'change' }],
        scheduleDate: [{ required: true, message: '请选择加课日期', trigger: 'change' }]
      },
      scheduleDetailVisible: false,
      scheduleDetailLoading: false,
      scheduleDetailEditing: false,
      scheduleEditSubmitting: false,
      scheduleDeleteLoading: false,
      selectedSchedule: null,
      scheduleEditOptions: { stages: [], teachers: [] },
      scheduleEditForm: {
        stageName: '',
        courseContent: '',
        teacherId: null
      },
      scheduleEditRules: {
        stageName: [{ required: true, message: '请选择课程阶段', trigger: 'change' }],
        courseContent: [
          { required: true, message: '请输入课程内容', trigger: 'blur' },
          { max: 500, message: '课程内容不能超过500个字符', trigger: 'blur' }
        ],
        teacherId: [{ required: true, message: '请选择授课教师', trigger: 'change' }]
      },
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
    },
    selectedAssignmentStage() {
      return this.teacherAssignmentOptions.stages.find(
        stage => stage.stageName === this.teacherAssignmentForm.stageName
      ) || null
    },
    occupiedTeacherCount() {
      if (!this.selectedAssignmentStage) return 0
      return this.teacherAssignmentOptions.teachers.filter(teacher => this.isTeacherOccupied(teacher)).length
    },
    temporaryCourseTarget() {
      return this.temporaryCourseForm.scheduleDate
        ? this.scheduleMap[this.temporaryCourseForm.scheduleDate] || null
        : null
    },
    temporaryCourseWillShift() {
      return Boolean(this.temporaryCourseTarget && this.temporaryCourseTarget.classType === 'CLASS')
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
      const courseText = schedule.stageName
        ? `${schedule.stageName}：${schedule.courseContent}`
        : schedule.courseContent
      return schedule.teacherName ? `${courseText} · ${schedule.teacherName}` : courseText
    },
    async openScheduleDetail(schedule) {
      if (!schedule) return
      this.scheduleDetailVisible = true
      this.scheduleDetailLoading = true
      this.scheduleDetailEditing = false
      try {
        const res = await getClassSchedule(schedule.id)
        this.selectedSchedule = res.data
      } catch (e) {
        this.scheduleDetailVisible = false
      } finally {
        this.scheduleDetailLoading = false
      }
    },
    async startScheduleEdit() {
      if (!this.selectedSchedule || !this.viewClass) return
      this.scheduleDetailLoading = true
      try {
        const res = await getTemporaryCourseOptions(this.viewClass.id)
        this.scheduleEditOptions = {
          stages: res.data.stages || [],
          teachers: res.data.teachers || []
        }
        this.scheduleEditForm = {
          stageName: this.selectedSchedule.stageName || '',
          courseContent: this.selectedSchedule.courseContent || '',
          teacherId: this.selectedSchedule.teacherId || null
        }
        this.scheduleDetailEditing = true
      } catch (e) {
        // 错误已由请求拦截器统一处理
      } finally {
        this.scheduleDetailLoading = false
      }
    },
    cancelScheduleEdit() {
      this.scheduleDetailEditing = false
      this.scheduleEditForm = { stageName: '', courseContent: '', teacherId: null }
    },
    submitScheduleEdit() {
      this.$refs.scheduleEditForm.validate(async valid => {
        if (!valid || !this.selectedSchedule) return
        this.scheduleEditSubmitting = true
        try {
          const res = await updateClassSchedule(this.selectedSchedule.id, this.scheduleEditForm)
          this.selectedSchedule = res.data
          await this.refreshCurrentSchedule()
          this.scheduleDetailEditing = false
          this.$message.success('课程日程修改成功')
        } catch (e) {
          // 错误已由请求拦截器统一处理
        } finally {
          this.scheduleEditSubmitting = false
        }
      })
    },
    async deleteSelectedSchedule() {
      if (!this.selectedSchedule) return
      const schedule = this.selectedSchedule
      const isLastClass = this.classScheduleCount === 1
      try {
        await this.$confirm(
          `确定删除 ${schedule.scheduleDate} 的课程吗？${isLastClass ? '这是当前课表最后一节课程。' : '非节假日会将后续课程依次前移。'}`,
          '删除课程',
          {
            confirmButtonText: '继续',
            cancelButtonText: '取消',
            type: 'warning'
          }
        )
        if (isLastClass) {
          await this.$confirm('这是课表最后一节课程：非节假日删除后将清空整张课表；节假日临时课将恢复为节假日日程。确认继续吗？', '二次确认', {
            confirmButtonText: '清空课表',
            cancelButtonText: '取消',
            type: 'error'
          })
        }
      } catch (e) {
        return
      }

      this.scheduleDeleteLoading = true
      try {
        await deleteClassSchedule(schedule.id)
        await this.refreshCurrentSchedule()
        this.scheduleDetailVisible = false
        this.$message.success(isLastClass && !this.scheduleRows.length ? '课表已清空' : '课程日程删除成功')
      } catch (e) {
        // 错误已由请求拦截器统一处理
      } finally {
        this.scheduleDeleteLoading = false
      }
    },
    async refreshCurrentSchedule() {
      if (!this.viewClass) return
      const res = await listClassSchedule(this.viewClass.id)
      this.scheduleRows = res.data || []
    },
    async openTeacherAssignment() {
      if (!this.viewClass) return
      this.teacherAssignmentVisible = true
      this.teacherAssignmentLoading = true
      try {
        const res = await getScheduleTeacherAssignmentOptions(this.viewClass.id)
        this.teacherAssignmentOptions = {
          stages: res.data.stages || [],
          teachers: res.data.teachers || []
        }
      } catch (e) {
        this.teacherAssignmentVisible = false
      } finally {
        this.teacherAssignmentLoading = false
      }
    },
    async openTemporaryCourse() {
      if (!this.viewClass) return
      this.temporaryCourseVisible = true
      this.temporaryCourseLoading = true
      try {
        const res = await getTemporaryCourseOptions(this.viewClass.id)
        this.temporaryCourseOptions = {
          stages: res.data.stages || [],
          teachers: res.data.teachers || []
        }
      } catch (e) {
        this.temporaryCourseVisible = false
      } finally {
        this.temporaryCourseLoading = false
      }
    },
    submitTemporaryCourse() {
      this.$refs.temporaryCourseForm.validate(async valid => {
        if (!valid || !this.viewClass) return
        this.temporaryCourseSubmitting = true
        try {
          const res = await addTemporaryCourse({
            classId: this.viewClass.id,
            stageName: this.temporaryCourseForm.stageName,
            teacherId: this.temporaryCourseForm.teacherId,
            scheduleDate: this.temporaryCourseForm.scheduleDate
          })
          const result = res.data
          const shiftText = result.shifted
            ? `，并顺延${result.shiftedClassCount}节原课程`
            : ''
          this.$message.success(`${result.scheduleDate}临时加课成功${shiftText}`)
          const scheduleRes = await listClassSchedule(this.viewClass.id)
          this.scheduleRows = scheduleRes.data || []
          this.calendarValue = this.toLocalDate(result.scheduleDate)
          this.temporaryCourseVisible = false
        } catch (e) {
          // 错误已由请求拦截器统一处理
        } finally {
          this.temporaryCourseSubmitting = false
        }
      })
    },
    handleAssignmentStageChange(stageName) {
      const stage = this.teacherAssignmentOptions.stages.find(item => item.stageName === stageName)
      this.teacherAssignmentForm.teacherId = stage && stage.teacherId ? stage.teacherId : null
      this.$nextTick(() => {
        this.$refs.teacherAssignmentForm && this.$refs.teacherAssignmentForm.clearValidate('teacherId')
      })
    },
    getTeacherConflictDates(teacher) {
      if (!this.selectedAssignmentStage) return []
      const stageDates = new Set(this.selectedAssignmentStage.classDates)
      return (teacher.occupiedDates || []).filter(date => stageDates.has(date))
    },
    isTeacherOccupied(teacher) {
      return this.getTeacherConflictDates(teacher).length > 0
    },
    getTeacherOptionLabel(teacher) {
      const conflictCount = this.getTeacherConflictDates(teacher).length
      return conflictCount ? `${teacher.label}（${conflictCount} 天冲突）` : teacher.label
    },
    submitTeacherAssignment() {
      this.$refs.teacherAssignmentForm.validate(async valid => {
        if (!valid || !this.viewClass) return
        this.teacherAssignmentSubmitting = true
        try {
          const res = await assignScheduleTeacher({
            classId: this.viewClass.id,
            stageName: this.teacherAssignmentForm.stageName,
            teacherId: this.teacherAssignmentForm.teacherId
          })
          const result = res.data
          this.$message.success(`${result.stageName}已分配给${result.teacherName}，共${result.assignedClassDays}个上课日`)
          const scheduleRes = await listClassSchedule(this.viewClass.id)
          this.scheduleRows = scheduleRes.data || []
          this.teacherAssignmentVisible = false
        } catch (e) {
          // 错误已由请求拦截器统一处理
        } finally {
          this.teacherAssignmentSubmitting = false
        }
      })
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
      this.teacherAssignmentVisible = false
      this.temporaryCourseVisible = false
      this.scheduleDetailVisible = false
      this.viewClass = null
      this.scheduleRows = []
      this.calendarValue = new Date()
    },
    resetTeacherAssignment() {
      this.teacherAssignmentOptions = { stages: [], teachers: [] }
      this.teacherAssignmentForm = { stageName: '', teacherId: null }
      this.$nextTick(() => {
        this.$refs.teacherAssignmentForm && this.$refs.teacherAssignmentForm.clearValidate()
      })
    },
    resetTemporaryCourse() {
      this.temporaryCourseOptions = { stages: [], teachers: [] }
      this.temporaryCourseForm = { stageName: '', teacherId: null, scheduleDate: '' }
      this.$nextTick(() => {
        this.$refs.temporaryCourseForm && this.$refs.temporaryCourseForm.clearValidate()
      })
    },
    resetScheduleDetail() {
      this.selectedSchedule = null
      this.scheduleDetailEditing = false
      this.scheduleEditOptions = { stages: [], teachers: [] }
      this.scheduleEditForm = { stageName: '', courseContent: '', teacherId: null }
      this.$nextTick(() => {
        this.$refs.scheduleEditForm && this.$refs.scheduleEditForm.clearValidate()
      })
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
.schedule-view {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
}
.calendar-toolbar {
  flex: none;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 0 0 10px;
  border-bottom: 1px solid var(--line-soft);
}
.calendar-toolbar-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 14px;
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
.calendar-scroll {
  flex: 1;
  min-height: 0;
  overflow-x: auto;
  overflow-y: hidden;
}
.schedule-calendar {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-width: 780px;
}
.calendar-cell {
  height: 100%;
  padding: 5px 7px;
  border-left: 3px solid transparent;
  transition: background-color .16s ease;
}
.calendar-cell.prev-month,
.calendar-cell.next-month { opacity: .42; }
.calendar-day-number {
  display: block;
  margin-bottom: 2px;
  color: var(--ink-3);
  font-family: var(--font-mono);
  font-size: 12px;
}
.calendar-entry { min-width: 0; }
.calendar-entry.is-actionable {
  border-radius: 3px;
  cursor: pointer;
  outline: none;
  transition: background-color .16s ease, box-shadow .16s ease;
}
.calendar-entry.is-actionable:hover { background: rgba(255, 255, 255, .64); }
.calendar-entry.is-actionable:focus-visible { box-shadow: 0 0 0 2px rgba(26, 139, 108, .35); }
.calendar-type {
  display: inline-block;
  margin-bottom: 2px;
  padding: 1px 6px;
  border-radius: 3px;
  font-size: 10px;
  font-weight: 700;
}
.calendar-stage {
  display: block;
  overflow: hidden;
  margin-bottom: 1px;
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
  text-overflow: ellipsis;
  white-space: nowrap;
  -webkit-line-clamp: 1;
}
.calendar-teacher {
  display: flex;
  align-items: center;
  gap: 3px;
  overflow: hidden;
  margin-top: 2px;
  color: var(--jade-deep);
  font-size: 10px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.assignment-stage-panel {
  display: grid;
  gap: 8px;
  margin: 4px 0 14px;
  padding: 12px 14px;
  border-left: 3px solid var(--jade);
  background: #F7FAF8;
}
.assignment-stage-panel div {
  display: grid;
  grid-template-columns: 72px minmax(0, 1fr);
  gap: 12px;
  font-size: 13px;
  line-height: 1.5;
}
.assignment-stage-panel span { color: var(--ink-3); }
.assignment-stage-panel b { overflow-wrap: anywhere; color: var(--ink); }
.assignment-alert { margin-top: 12px; }
.temporary-impact {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin-top: 4px;
  padding: 12px 14px;
  border-left: 3px solid var(--jade);
  background: #F4FAF7;
  color: var(--jade-deep);
}
.temporary-impact.will-shift {
  border-left-color: var(--brass);
  background: #FCF8EE;
  color: var(--brass-ink);
}
.temporary-impact > i { margin-top: 2px; font-size: 18px; }
.temporary-impact div { display: flex; min-width: 0; flex-direction: column; gap: 4px; }
.temporary-impact b { color: var(--ink); font-size: 13px; }
.temporary-impact span { color: var(--ink-3); font-size: 12px; line-height: 1.6; }
.schedule-detail-body { min-height: 180px; }
.schedule-detail-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
  padding: 12px 14px;
  border-left: 3px solid #9AA79F;
  background: #F5F7F4;
}
.schedule-detail-head.schedule-class {
  border-left-color: var(--jade);
  background: var(--jade-soft);
}
.schedule-detail-head.schedule-self-study {
  border-left-color: var(--brass);
  background: var(--brass-soft);
}
.schedule-detail-head.schedule-holiday {
  border-left-color: var(--clay);
  background: var(--clay-soft);
}
.schedule-detail-head div { display: flex; flex-direction: column; gap: 3px; }
.schedule-detail-head span { color: var(--ink-3); font-family: var(--font-mono); font-size: 12px; }
.schedule-detail-head b { color: var(--ink); font-size: 13px; }
.schedule-detail-head strong { min-width: 0; color: var(--ink); text-align: right; overflow-wrap: anywhere; }
.schedule-detail-actions { display: flex; align-items: center; width: 100%; }
.action-spacer { flex: 1; }
::v-deep .schedule-descriptions .el-descriptions-item__label { width: 96px; color: var(--ink-3); }
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
  .calendar-toolbar-actions { align-items: flex-start; justify-content: flex-start; }
  ::v-deep .schedule-calendar-dialog {
    height: calc(100vh - 16px);
    margin-top: 8px !important;
  }
}

::v-deep .schedule-calendar-dialog {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 32px);
  margin-top: 16px !important;
  margin-bottom: 16px;
}
::v-deep .schedule-calendar-dialog .el-dialog__header {
  flex: none;
  padding-top: 18px;
  padding-bottom: 16px;
}
::v-deep .schedule-calendar-dialog .el-dialog__body {
  flex: 1;
  min-height: 0;
  overflow: hidden;
  padding-top: 14px;
  padding-bottom: 16px;
}
::v-deep .schedule-calendar .el-calendar__header {
  flex: none;
  padding: 10px 0;
}
::v-deep .schedule-calendar .el-calendar__body {
  display: flex;
  flex: 1;
  min-height: 0;
  overflow: hidden;
  padding: 0;
}
::v-deep .schedule-calendar .el-calendar-table {
  height: 100%;
  table-layout: fixed;
}
::v-deep .schedule-calendar .el-calendar-table .el-calendar-day {
  height: 100%;
  min-height: 0;
  padding: 0;
}
</style>
