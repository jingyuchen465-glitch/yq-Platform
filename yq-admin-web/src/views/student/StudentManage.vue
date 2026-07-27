<template>
  <div class="page-container student-page">
    <header class="page-head student-head">
      <div>
        <p class="eyebrow">MARKET:STUDENT · LEARNER LIFECYCLE</p>
        <h1>学员管理</h1>
        <p class="head-copy">维护学员联系方式、所属班级与学习状态，保留完整的状态流转记录。</p>
      </div>
      <div class="head-stat">
        <b>{{ total }}</b>
        <span>条学员档案</span>
      </div>
    </header>

    <section class="search-bar student-search">
      <el-form :inline="true" :model="queryParams" @submit.native.prevent>
        <el-form-item label="快速检索">
          <el-input
            v-model.trim="queryParams.keyword"
            class="keyword-input"
            prefix-icon="el-icon-search"
            placeholder="姓名 / 手机号 / 邮箱"
            clearable
            @keyup.enter.native="handleSearch"
          />
        </el-form-item>
        <el-form-item label="所属班级">
          <el-select
            v-model="queryParams.classId"
            class="class-filter"
            placeholder="全部班级"
            filterable
            clearable
            :loading="optionsLoading"
          >
            <el-option
              v-for="item in classOptions"
              :key="item.id"
              :label="item.className"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="学习状态">
          <el-select v-model="queryParams.status" class="status-filter" placeholder="全部状态" clearable>
            <el-option
              v-for="item in statusOptions"
              :key="item.code"
              :label="item.label"
              :value="item.code"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" @click="handleSearch">查询</el-button>
          <el-button icon="el-icon-refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
      <div class="search-note">
        <i class="el-icon-info"></i>
        已退学档案会继续保留，可通过状态筛选查看并恢复。
      </div>
    </section>

    <section class="table-card student-card">
      <div class="table-toolbar">
        <div>
          <p class="toolbar-kicker">LEARNER DIRECTORY</p>
          <h2>学员档案目录</h2>
        </div>
        <div class="status-legend" aria-label="状态图例">
          <span><i class="temporary"></i>临时</span>
          <span><i class="at-school"></i>在校</span>
          <span><i class="graduate"></i>毕业</span>
          <span><i class="withdrawal"></i>退学</span>
        </div>
      </div>

      <el-table :data="tableData" v-loading="loading" stripe class="student-table">
        <template slot="empty">
          <div class="student-empty">
            <i class="el-icon-user"></i>
            <b>没有匹配的学员</b>
            <span>调整筛选条件后重新查询。</span>
          </div>
        </template>

        <el-table-column label="学员" min-width="185">
          <template slot-scope="{ row }">
            <div class="student-identity">
              <span class="student-avatar">{{ studentInitial(row.name) }}</span>
              <div>
                <strong>{{ row.name }}</strong>
                <code>S-{{ String(row.id).padStart(5, '0') }}</code>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="联系方式" min-width="210">
          <template slot-scope="{ row }">
            <div class="contact-stack">
              <code>{{ row.phone }}</code>
              <span>{{ row.email || '未填写邮箱' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="所属班级" min-width="150">
          <template slot-scope="{ row }">
            <span v-if="row.className" class="class-chip">{{ row.className }}</span>
            <span v-else class="empty-copy">暂未分班</span>
          </template>
        </el-table-column>
        <el-table-column label="学习状态" width="126" align="center">
          <template slot-scope="{ row }">
            <el-tag size="small" :type="statusTagType(row.status)">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="156" align="center">
          <template slot-scope="{ row }"><code class="date-code">{{ formatDate(row.createdAt) }}</code></template>
        </el-table-column>
        <el-table-column label="更新时间" width="156" align="center">
          <template slot-scope="{ row }"><code class="date-code">{{ formatDate(row.updatedAt) }}</code></template>
        </el-table-column>
        <el-table-column label="操作" width="166" align="center" fixed="right">
          <template slot-scope="{ row }">
            <el-button type="text" icon="el-icon-edit" @click="handleEdit(row)">编辑</el-button>
            <el-button
              v-if="row.status !== 'WITCHDRAWAL'"
              type="text"
              icon="el-icon-remove-outline"
              class="danger-link"
              @click="handleWithdraw(row)"
            >办理退学</el-button>
            <span v-else class="withdrawn-copy"><i class="el-icon-lock"></i>已停用</span>
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
          :page-sizes="[10, 20, 50]"
          @size-change="fetchData"
          @current-change="fetchData"
        />
      </div>
    </section>

    <el-dialog
      title="编辑学员档案"
      :visible.sync="dialogVisible"
      width="720px"
      custom-class="student-dialog"
      :close-on-click-modal="false"
      @closed="resetForm"
    >
      <el-form ref="studentForm" :model="form" :rules="formRules" label-position="top">
        <div class="dialog-intro">
          <span class="student-avatar large">{{ studentInitial(form.name) }}</span>
          <div>
            <b>{{ form.name || '未命名学员' }}</b>
            <code>{{ editId ? `S-${String(editId).padStart(5, '0')}` : 'LEARNER PROFILE' }}</code>
          </div>
          <p>保存后将终止该学员当前登录会话，确保新资料即时生效。</p>
        </div>

        <div class="form-grid">
          <el-form-item label="学员姓名" prop="name">
            <el-input v-model="form.name" maxlength="30" show-word-limit placeholder="请输入学员姓名" />
          </el-form-item>
          <el-form-item label="登录手机号" prop="phone">
            <el-input v-model.trim="form.phone" maxlength="11" placeholder="请输入11位手机号" />
          </el-form-item>
          <el-form-item label="邮箱" prop="email">
            <el-input v-model.trim="form.email" maxlength="128" placeholder="选填，用于接收学习通知" />
          </el-form-item>
          <el-form-item label="所属班级" prop="classId">
            <el-select v-model="form.classId" placeholder="暂不分班" filterable clearable :loading="optionsLoading">
              <el-option
                v-for="item in classOptions"
                :key="item.id"
                :label="item.className"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
        </div>

        <el-form-item label="学习状态" prop="status" class="lifecycle-form-item">
          <div class="lifecycle-track" role="radiogroup" aria-label="学习状态">
            <button
              v-for="(item, index) in statusOptions"
              :key="item.code"
              type="button"
              class="lifecycle-step"
              :class="[statusClass(item.code), { selected: form.status === item.code }]"
              role="radio"
              :aria-checked="String(form.status === item.code)"
              @click="form.status = item.code"
            >
              <span class="step-index">{{ String(index + 1).padStart(2, '0') }}</span>
              <b>{{ item.label }}</b>
              <small>{{ item.description }}</small>
            </button>
          </div>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-actions">
        <span><i class="el-icon-warning-outline"></i>修改会使学生端重新登录</span>
        <div>
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitLoading" @click="handleSubmit">保存修改</el-button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  pageStudents,
  getStudent,
  getStudentClassOptions,
  updateStudent,
  withdrawStudent
} from '@/api/student'

const STATUS_OPTIONS = [
  { code: 'TEMPORARY', label: '临时学员', description: '待确认班级' },
  { code: 'ATSCHOOL', label: '在校学习', description: '正常学习中' },
  { code: 'GRADUATE', label: '已毕业', description: '完成学习' },
  { code: 'WITCHDRAWAL', label: '已退学', description: '停止访问' }
]

export default {
  name: 'StudentManage',
  data() {
    return {
      loading: false,
      optionsLoading: false,
      submitLoading: false,
      tableData: [],
      total: 0,
      classOptions: [],
      statusOptions: STATUS_OPTIONS,
      queryParams: this.createEmptyQuery(),
      dialogVisible: false,
      editId: null,
      form: this.createEmptyForm(),
      formRules: {
        name: [
          { required: true, message: '请输入学员姓名', trigger: 'blur' },
          { max: 30, message: '姓名长度不能超过30个字符', trigger: 'blur' }
        ],
        phone: [
          { required: true, message: '请输入登录手机号', trigger: 'blur' },
          { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的11位手机号', trigger: 'blur' }
        ],
        email: [
          { type: 'email', message: '请输入正确的邮箱地址', trigger: ['blur', 'change'] },
          { max: 128, message: '邮箱长度不能超过128个字符', trigger: 'blur' }
        ],
        status: [
          { required: true, message: '请选择学习状态', trigger: 'change' }
        ]
      }
    }
  },
  created() {
    this.fetchData()
    this.loadClassOptions()
  },
  methods: {
    createEmptyQuery() {
      return { current: 1, size: 10, keyword: '', classId: '', status: '' }
    },
    createEmptyForm() {
      return { name: '', phone: '', email: '', classId: '', status: 'TEMPORARY' }
    },
    async fetchData() {
      this.loading = true
      try {
        const res = await pageStudents(this.queryParams)
        this.tableData = (res.data && res.data.records) || []
        this.total = (res.data && res.data.total) || 0
      } catch (e) {
        this.tableData = []
        this.total = 0
      } finally {
        this.loading = false
      }
    },
    async loadClassOptions() {
      this.optionsLoading = true
      try {
        const res = await getStudentClassOptions()
        this.classOptions = res.data || []
      } catch (e) {
        this.classOptions = []
      } finally {
        this.optionsLoading = false
      }
    },
    handleSearch() {
      this.queryParams.current = 1
      this.fetchData()
    },
    handleReset() {
      this.queryParams = this.createEmptyQuery()
      this.fetchData()
    },
    async handleEdit(row) {
      this.editId = row.id
      this.dialogVisible = true
      this.submitLoading = true
      try {
        const res = await getStudent(row.id)
        const student = res.data
        this.form = {
          name: student.name || '',
          phone: student.phone || '',
          email: student.email || '',
          classId: student.classId || '',
          status: student.status || 'TEMPORARY'
        }
      } catch (e) {
        this.dialogVisible = false
      } finally {
        this.submitLoading = false
      }
    },
    handleSubmit() {
      this.$refs.studentForm.validate(async valid => {
        if (!valid) return
        this.submitLoading = true
        try {
          await updateStudent(this.editId, {
            name: this.form.name.trim(),
            phone: this.form.phone.trim(),
            email: this.form.email.trim() || null,
            classId: this.form.classId || null,
            status: this.form.status
          })
          this.$message.success('学员资料已更新')
          this.dialogVisible = false
          this.fetchData()
        } catch (e) {
          // 错误由请求拦截器统一展示
        } finally {
          this.submitLoading = false
        }
      })
    },
    handleWithdraw(row) {
      this.$confirm(
        `办理“${row.name}”退学后，学生端会立即退出，档案仍会保留。确定继续吗？`,
        '办理学员退学',
        {
          confirmButtonText: '确认退学',
          cancelButtonText: '取消',
          type: 'warning'
        }
      ).then(async () => {
        await withdrawStudent(row.id)
        this.$message.success('学员已办理退学')
        this.fetchData()
      }).catch(() => {})
    },
    resetForm() {
      this.editId = null
      this.form = this.createEmptyForm()
      this.$nextTick(() => {
        this.$refs.studentForm && this.$refs.studentForm.clearValidate()
      })
    },
    studentInitial(name) {
      const value = (name || '学').trim()
      return value.slice(0, 1).toUpperCase()
    },
    statusLabel(code) {
      const item = STATUS_OPTIONS.find(status => status.code === code)
      return item ? item.label : '未知状态'
    },
    statusTagType(code) {
      return {
        TEMPORARY: 'warning',
        ATSCHOOL: 'success',
        GRADUATE: 'info',
        WITCHDRAWAL: 'danger'
      }[code] || 'info'
    },
    statusClass(code) {
      return code.toLowerCase().replace('_', '-')
    },
    formatDate(value) {
      if (!value) return '—'
      return String(value).replace('T', ' ').slice(0, 16)
    }
  }
}
</script>

<style scoped>
.student-head { align-items: center; }
.head-copy { margin-top: 9px; color: var(--ink-3); font-size: 13px; line-height: 1.65; }
::v-deep .student-dialog { max-width: calc(100vw - 28px); }

.student-search { display: flex; align-items: flex-start; justify-content: space-between; gap: 18px; padding-bottom: 14px; }
.student-search .el-form { display: flex; flex-wrap: wrap; align-items: flex-end; }
.student-search .el-form-item { margin-bottom: 0; }
.keyword-input { width: 235px; }
.class-filter { width: 175px; }
.status-filter { width: 145px; }
.search-note { display: flex; align-items: center; gap: 7px; max-width: 265px; padding-top: 11px; color: var(--ink-3); font-size: 12px; line-height: 1.5; }
.search-note i { color: var(--brass-ink); }

.table-toolbar { display: flex; align-items: center; justify-content: space-between; gap: 20px; margin-bottom: 16px; }
.toolbar-kicker { margin-bottom: 4px; color: var(--brass-ink); font: 600 10px var(--font-mono); letter-spacing: .2em; }
.table-toolbar h2 { color: var(--ink); font-family: var(--font-display); font-size: 17px; }
.status-legend { display: flex; align-items: center; gap: 15px; color: var(--ink-3); font-size: 11px; }
.status-legend span { display: inline-flex; align-items: center; gap: 5px; }
.status-legend i { width: 7px; height: 7px; border-radius: 50%; }
.status-legend .temporary { background: var(--brass); }
.status-legend .at-school { background: var(--jade); }
.status-legend .graduate { background: #82948A; }
.status-legend .withdrawal { background: var(--clay); }

.student-identity { display: flex; align-items: center; gap: 11px; }
.student-avatar { display: grid; width: 36px; height: 36px; flex: none; place-items: center; border-radius: 9px; background: var(--jade-soft); color: var(--jade-deep); font-family: var(--font-display); font-size: 14px; font-weight: 800; box-shadow: inset 0 0 0 1px rgba(23, 126, 99, .08); }
.student-avatar.large { width: 46px; height: 46px; border-radius: 12px; font-size: 17px; }
.student-identity > div { display: flex; min-width: 0; flex-direction: column; gap: 4px; }
.student-identity strong { overflow: hidden; color: var(--ink); font-size: 13.5px; text-overflow: ellipsis; white-space: nowrap; }
.student-identity code { color: var(--brass-ink); font: 10px var(--font-mono); letter-spacing: .08em; }
.contact-stack { display: flex; min-width: 0; flex-direction: column; gap: 5px; }
.contact-stack code { color: var(--ink-2); font: 12px var(--font-mono); }
.contact-stack span { overflow: hidden; color: var(--ink-3); font-size: 12px; text-overflow: ellipsis; white-space: nowrap; }
.class-chip { display: inline-flex; max-width: 100%; overflow: hidden; border-radius: 4px; background: var(--brass-soft); color: var(--brass-ink); padding: 3px 8px; font-size: 12px; text-overflow: ellipsis; white-space: nowrap; }
.empty-copy { color: var(--ink-3); font-size: 12px; }
.date-code { color: var(--ink-3); font: 10.5px var(--font-mono); }
.withdrawn-copy { display: inline-flex; align-items: center; gap: 4px; margin-left: 8px; color: var(--ink-3); font-size: 12px; }
.student-empty { display: flex; min-height: 190px; align-items: center; justify-content: center; flex-direction: column; color: var(--ink-3); }
.student-empty i { margin-bottom: 12px; color: #AEB9B0; font-size: 34px; }
.student-empty b { margin-bottom: 5px; color: var(--ink-2); font-size: 14px; }
.student-empty span { font-size: 12px; }

.dialog-intro { display: grid; grid-template-columns: auto 1fr minmax(190px, 260px); align-items: center; gap: 12px; margin: -4px 0 22px; padding: 14px 16px; border: 1px solid var(--line-soft); border-radius: 9px; background: #F8FAF7; }
.dialog-intro > div { display: flex; flex-direction: column; gap: 4px; }
.dialog-intro b { color: var(--ink); font-size: 14px; }
.dialog-intro code { color: var(--brass-ink); font: 10px var(--font-mono); letter-spacing: .08em; }
.dialog-intro p { border-left: 1px solid var(--line); padding-left: 14px; color: var(--ink-3); font-size: 11.5px; line-height: 1.6; }
.form-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 0 18px; }
.form-grid .el-select { width: 100%; }
.lifecycle-form-item { margin-top: 2px; }
.lifecycle-track { position: relative; display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 8px; }
.lifecycle-track::before { content: ''; position: absolute; top: 27px; right: 12%; left: 12%; height: 1px; background: var(--line); }
.lifecycle-step { position: relative; z-index: 1; display: flex; min-height: 94px; align-items: center; flex-direction: column; justify-content: center; border: 1px solid var(--line); border-radius: 8px; background: #FFFFFF; color: var(--ink-3); cursor: pointer; font-family: inherit; transition: border-color .16s ease, background-color .16s ease, transform .16s ease, box-shadow .16s ease; }
.lifecycle-step:hover, .lifecycle-step:focus-visible { border-color: var(--jade); outline: none; transform: translateY(-2px); box-shadow: var(--shadow-card); }
.lifecycle-step .step-index { display: grid; width: 25px; height: 25px; place-items: center; margin-bottom: 7px; border-radius: 50%; background: #EEF1EC; color: var(--ink-3); font: 9px var(--font-mono); }
.lifecycle-step b { margin-bottom: 3px; color: var(--ink-2); font-size: 12px; }
.lifecycle-step small { font-size: 10px; }
.lifecycle-step.selected { border-color: var(--jade); background: var(--jade-soft); box-shadow: inset 0 0 0 1px rgba(23, 126, 99, .08); }
.lifecycle-step.selected .step-index { background: var(--jade); color: #FFFFFF; }
.lifecycle-step.selected b { color: var(--jade-deep); }
.lifecycle-step.temporary.selected { border-color: var(--brass); background: var(--brass-soft); }
.lifecycle-step.temporary.selected .step-index { background: var(--brass); }
.lifecycle-step.temporary.selected b { color: var(--brass-ink); }
.lifecycle-step.graduate.selected { border-color: #899A90; background: #EDF0EC; }
.lifecycle-step.graduate.selected .step-index { background: #71837A; }
.lifecycle-step.graduate.selected b { color: var(--ink-2); }
.lifecycle-step.witchdrawal.selected { border-color: var(--clay); background: var(--clay-soft); }
.lifecycle-step.witchdrawal.selected .step-index { background: var(--clay); }
.lifecycle-step.witchdrawal.selected b { color: var(--clay-deep); }
.dialog-actions { display: flex; align-items: center; justify-content: space-between; gap: 16px; }
.dialog-actions > span { color: var(--ink-3); font-size: 11.5px; }
.dialog-actions > span i { margin-right: 5px; color: var(--brass-ink); }

@media (max-width: 1180px) {
  .student-search { flex-direction: column; }
  .search-note { max-width: none; padding-top: 0; }
  .status-legend { display: none; }
}

@media (max-width: 760px) {
  .student-head { align-items: flex-end; }
  .student-head .head-copy { display: none; }
  .student-search .el-form { display: grid; width: 100%; grid-template-columns: 1fr; }
  .student-search .el-form-item { width: 100%; margin-right: 0; margin-bottom: 10px; }
  .keyword-input, .class-filter, .status-filter { width: 100%; }
  .dialog-intro { grid-template-columns: auto 1fr; }
  .dialog-intro p { display: none; }
  .form-grid { grid-template-columns: 1fr; }
  .lifecycle-track { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .lifecycle-track::before { display: none; }
  .dialog-actions { align-items: flex-end; flex-direction: column; }
}
</style>
