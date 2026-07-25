<template>
  <div class="template-page page-container">
    <header class="page-head">
      <div>
        <p class="eyebrow">ACADEMIC · HOMEWORK STANDARD</p>
        <h1>作业标准管理</h1>
        <p class="head-copy">为课程的每个阶段或天次绑定一份标准文档，发布作业时自动匹配。</p>
      </div>
      <div class="head-stat">
        <b>{{ total }}</b>
        <span>份课程标准</span>
      </div>
    </header>

    <section class="search-bar">
      <el-form :inline="true" :model="queryParams" @submit.native.prevent>
        <el-form-item label="课程">
          <el-select
            v-model="queryParams.courseId"
            filterable
            clearable
            placeholder="全部课程"
            class="course-filter"
          >
            <el-option
              v-for="course in courseOptions"
              :key="course.id"
              :label="course.courseName"
              :value="course.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" clearable placeholder="全部状态" style="width: 140px">
            <el-option label="生效" value="ACTIVE" />
            <el-option label="失效" value="INACTIVE" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" @click="handleSearch">查询</el-button>
          <el-button icon="el-icon-refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </section>

    <section class="table-card">
      <div class="table-toolbar">
        <div>
          <el-button type="primary" icon="el-icon-plus" @click="handleAdd">新增作业标准</el-button>
          <span class="toolbar-note">一个课程阶段/天次只保留一份标准</span>
        </div>
        <span class="sync-note"><i class="el-icon-connection"></i> 发布台按课表实时匹配</span>
      </div>

      <el-table v-loading="loading" :data="tableData" stripe>
        <el-table-column label="课程" min-width="180">
          <template slot-scope="{ row }">
            <div class="course-cell">
              <strong>{{ row.courseName }}</strong>
              <small>{{ row.teachingMode === 'ONLINE' ? '线上课程' : '线下课程' }}</small>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="绑定位置" width="150">
          <template slot-scope="{ row }">
            <span class="position-chip">
              {{ row.teachingMode === 'ONLINE' ? row.stageName : `D${row.dayNumber}` }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="标准文档" min-width="240" show-overflow-tooltip>
          <template slot-scope="{ row }">
            <button type="button" class="file-link" @click="handlePreview(row)">
              <i class="el-icon-document"></i>
              <span>{{ row.contentFileName }}</span>
            </button>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template slot-scope="{ row }">
            <span class="st" :class="row.status === 'ACTIVE' ? 'on' : 'off'">
              <i></i>{{ row.status === 'ACTIVE' ? '生效' : '失效' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="170" show-overflow-tooltip>
          <template slot-scope="{ row }">{{ row.remark || '—' }}</template>
        </el-table-column>
        <el-table-column label="更新时间" width="170" align="center">
          <template slot-scope="{ row }"><code class="mono">{{ row.updatedAt }}</code></template>
        </el-table-column>
        <el-table-column label="操作" width="190" align="center" fixed="right">
          <template slot-scope="{ row }">
            <el-button type="text" icon="el-icon-view" @click="handlePreview(row)">预览</el-button>
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
    </section>

    <el-dialog
      :title="isEdit ? '编辑作业标准' : '新增作业标准'"
      :visible.sync="dialogVisible"
      width="680px"
      class="template-dialog"
      @closed="resetDialog"
    >
      <div class="binding-rail" aria-label="绑定流程">
        <div :class="{ active: form.courseId }">
          <span>1</span><b>课程</b><small>{{ selectedCourseName || '待选择' }}</small>
        </div>
        <i></i>
        <div :class="{ active: form.courseDetailId }">
          <span>2</span><b>阶段 / 天次</b><small>{{ selectedPositionLabel || '待选择' }}</small>
        </div>
        <i></i>
        <div :class="{ active: selectedFile || form.contentObjectKey }">
          <span>3</span><b>标准文档</b><small>{{ selectedFile ? selectedFile.name : (form.contentFileName || '待上传') }}</small>
        </div>
      </div>

      <el-form ref="templateForm" :model="form" :rules="rules" label-position="top" class="template-form">
        <div class="form-grid">
          <el-form-item label="绑定课程" prop="courseId">
            <el-select
              v-model="form.courseId"
              filterable
              placeholder="选择课程"
              class="full-control"
              @change="handleCourseChange"
            >
              <el-option
                v-for="course in courseOptions"
                :key="course.id"
                :label="`${course.courseName} · ${course.teachingMode === 'ONLINE' ? '线上' : '线下'}`"
                :value="course.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="课程阶段 / 天次" prop="courseDetailId">
            <el-select
              v-model="form.courseDetailId"
              filterable
              :loading="positionLoading"
              :disabled="!form.courseId"
              placeholder="先选择课程"
              class="full-control"
            >
              <el-option
                v-for="position in positionOptions"
                :key="position.courseDetailId"
                :label="position.label"
                :value="position.courseDetailId"
              />
            </el-select>
          </el-form-item>
        </div>

        <el-form-item label="作业标准文档" required>
          <label class="standard-drop" :class="{ filled: selectedFile || form.contentObjectKey }">
            <input ref="fileInput" type="file" :disabled="submitting" @change="handleFileChange">
            <i :class="selectedFile || form.contentObjectKey ? 'el-icon-document-checked' : 'el-icon-upload2'"></i>
            <div>
              <b>{{ selectedFile ? selectedFile.name : (form.contentFileName || '选择标准文档') }}</b>
              <span>{{ fileHint }}</span>
            </div>
            <em>{{ selectedFile || form.contentObjectKey ? '更换' : '选择文件' }}</em>
          </label>
          <el-progress
            v-if="submitting && uploadProgress > 0"
            :percentage="uploadProgress"
            :stroke-width="5"
            class="upload-progress"
          />
        </el-form-item>

        <div class="form-grid compact-grid">
          <el-form-item label="状态" prop="status">
            <el-radio-group v-model="form.status">
              <el-radio-button label="ACTIVE">生效</el-radio-button>
              <el-radio-button label="INACTIVE">失效</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="备注" prop="remark">
            <el-input v-model="form.remark" maxlength="500" show-word-limit placeholder="可填写适用范围或版本说明" />
          </el-form-item>
        </div>
      </el-form>

      <div slot="footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          {{ submitting ? submitText : '保存标准' }}
        </el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  pageCourseHomeworkTemplates,
  listTemplateCourseOptions,
  listTemplatePositionOptions,
  createCourseHomeworkTemplate,
  updateCourseHomeworkTemplate,
  deleteCourseHomeworkTemplate,
  getCourseHomeworkTemplatePreview
} from '@/api/courseHomeworkTemplate'
import { uploadToOssWithProgress } from '@/utils/ossUpload'

const MAX_FILE_SIZE = 20 * 1024 * 1024

const emptyForm = () => ({
  courseId: null,
  courseDetailId: null,
  contentObjectKey: '',
  contentFileName: '',
  status: 'ACTIVE',
  remark: ''
})

export default {
  name: 'CourseHomeworkTemplateManage',
  data() {
    return {
      loading: false,
      positionLoading: false,
      submitting: false,
      uploadProgress: 0,
      tableData: [],
      courseOptions: [],
      positionOptions: [],
      total: 0,
      dialogVisible: false,
      isEdit: false,
      editId: null,
      selectedFile: null,
      queryParams: { current: 1, size: 10, courseId: null, status: '' },
      form: emptyForm(),
      rules: {
        courseId: [{ required: true, message: '请选择课程', trigger: 'change' }],
        courseDetailId: [{ required: true, message: '请选择课程阶段或天次', trigger: 'change' }],
        status: [{ required: true, message: '请选择状态', trigger: 'change' }],
        remark: [{ max: 500, message: '备注不能超过500个字符', trigger: 'blur' }]
      }
    }
  },
  computed: {
    selectedCourse() {
      return this.courseOptions.find(item => item.id === this.form.courseId)
    },
    selectedCourseName() {
      return this.selectedCourse ? this.selectedCourse.courseName : ''
    },
    selectedPositionLabel() {
      const position = this.positionOptions.find(item => item.courseDetailId === this.form.courseDetailId)
      return position ? position.label : ''
    },
    fileHint() {
      if (this.selectedFile) return `${this.formatFileSize(this.selectedFile.size)} · 保存时上传至 OSS`
      if (this.form.contentObjectKey) return '当前文档已保存至 OSS，点击可替换'
      return '支持 PDF、Office、Markdown、文本等文档，最大 20 MB'
    },
    submitText() {
      if (this.uploadProgress > 0 && this.uploadProgress < 100) return `上传中 ${this.uploadProgress}%`
      return '正在保存'
    }
  },
  created() {
    this.loadCourseOptions()
    this.fetchData()
  },
  methods: {
    async loadCourseOptions() {
      const res = await listTemplateCourseOptions()
      this.courseOptions = res.data || []
    },
    async fetchData() {
      this.loading = true
      try {
        const res = await pageCourseHomeworkTemplates(this.queryParams)
        this.tableData = res.data.records || []
        this.total = res.data.total || 0
      } finally {
        this.loading = false
      }
    },
    handleSearch() {
      this.queryParams.current = 1
      this.fetchData()
    },
    handleReset() {
      this.queryParams = { ...this.queryParams, current: 1, courseId: null, status: '' }
      this.fetchData()
    },
    handleAdd() {
      this.isEdit = false
      this.editId = null
      this.form = emptyForm()
      this.positionOptions = []
      this.dialogVisible = true
    },
    async handleEdit(row) {
      this.isEdit = true
      this.editId = row.id
      this.dialogVisible = true
      this.form = {
        courseId: row.courseId,
        courseDetailId: null,
        contentObjectKey: row.contentObjectKey,
        contentFileName: row.contentFileName,
        status: row.status,
        remark: row.remark || ''
      }
      await this.loadPositions(row.courseId)
      const matched = this.positionOptions.find(item => row.teachingMode === 'ONLINE'
        ? item.stageName === row.stageName
        : item.dayNumber === row.dayNumber)
      this.form.courseDetailId = matched ? matched.courseDetailId : null
    },
    async handleCourseChange(courseId) {
      this.form.courseDetailId = null
      await this.loadPositions(courseId)
    },
    async loadPositions(courseId) {
      this.positionOptions = []
      if (!courseId) return
      this.positionLoading = true
      try {
        const res = await listTemplatePositionOptions(courseId)
        this.positionOptions = res.data || []
        if (!this.positionOptions.length) {
          this.$message.warning('该课程还没有可绑定的课程详情')
        }
      } finally {
        this.positionLoading = false
      }
    },
    handleFileChange(event) {
      const file = event.target.files && event.target.files[0]
      if (!file) return
      if (file.size > MAX_FILE_SIZE) {
        this.$message.error('作业标准文档不能超过 20 MB')
        event.target.value = ''
        return
      }
      this.selectedFile = file
      this.uploadProgress = 0
    },
    validateForm() {
      return new Promise(resolve => this.$refs.templateForm.validate(resolve))
    },
    async handleSubmit() {
      const valid = await this.validateForm()
      if (!valid) return
      if (!this.selectedFile && !this.form.contentObjectKey) {
        this.$message.warning('请选择作业标准文档')
        return
      }

      this.submitting = true
      try {
        let objectKey = this.form.contentObjectKey
        let fileName = this.form.contentFileName
        if (this.selectedFile) {
          const result = await uploadToOssWithProgress(
            this.selectedFile,
            'homework-template',
            value => { this.uploadProgress = value }
          )
          objectKey = result.objectKey
          fileName = this.selectedFile.name
        }
        const payload = {
          courseId: this.form.courseId,
          courseDetailId: this.form.courseDetailId,
          contentObjectKey: objectKey,
          contentFileName: fileName,
          status: this.form.status,
          remark: this.form.remark ? this.form.remark.trim() : null
        }
        if (this.isEdit) {
          await updateCourseHomeworkTemplate(this.editId, payload)
          this.$message.success('作业标准已更新')
        } else {
          await createCourseHomeworkTemplate(payload)
          this.$message.success('作业标准已保存')
        }
        this.dialogVisible = false
        this.fetchData()
      } finally {
        this.submitting = false
        this.uploadProgress = 0
      }
    },
    async handlePreview(row) {
      const previewWindow = window.open('', '_blank')
      try {
        const res = await getCourseHomeworkTemplatePreview(row.id)
        if (previewWindow) previewWindow.location.href = res.data.previewUrl
        else window.open(res.data.previewUrl, '_blank')
      } catch (error) {
        if (previewWindow) previewWindow.close()
      }
    },
    handleDelete(row) {
      const position = row.teachingMode === 'ONLINE' ? row.stageName : `D${row.dayNumber}`
      this.$confirm(
        `确定删除「${row.courseName} · ${position}」的作业标准吗？OSS 原文件不会被删除。`,
        '删除作业标准',
        { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
      ).then(async () => {
        await deleteCourseHomeworkTemplate(row.id)
        this.$message.success('作业标准已删除')
        this.fetchData()
      }).catch(() => {})
    },
    resetDialog() {
      this.form = emptyForm()
      this.positionOptions = []
      this.selectedFile = null
      this.uploadProgress = 0
      if (this.$refs.fileInput) this.$refs.fileInput.value = ''
      this.$nextTick(() => this.$refs.templateForm && this.$refs.templateForm.clearValidate())
    },
    formatFileSize(size) {
      if (size < 1024) return `${size} B`
      if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
      return `${(size / 1024 / 1024).toFixed(1)} MB`
    }
  }
}
</script>

<style scoped>
.template-page { padding-bottom: 26px; }
.head-copy { margin-top: 8px; color: var(--ink-3); font-size: 13px; }
.course-filter { width: 260px; }
.table-toolbar { display: flex; align-items: center; justify-content: space-between; gap: 18px; margin-bottom: 16px; }
.toolbar-note { margin-left: 12px; color: var(--ink-3); font-size: 12px; }
.sync-note { color: var(--jade-deep); font: 600 11px var(--font-mono); letter-spacing: .03em; }
.sync-note i { margin-right: 6px; }
.course-cell { display: flex; flex-direction: column; gap: 4px; }
.course-cell strong { color: var(--ink); font-size: 13.5px; }
.course-cell small { color: var(--ink-3); font-size: 11px; }
.position-chip { display: inline-flex; padding: 4px 9px; border: 1px solid #D7C49A; border-radius: 5px; background: var(--brass-soft); color: var(--brass-ink); font: 700 12px var(--font-mono); }
.file-link { display: inline-flex; align-items: center; gap: 8px; max-width: 100%; padding: 0; border: 0; background: transparent; color: var(--jade-deep); cursor: pointer; font: inherit; }
.file-link span { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.file-link:hover { text-decoration: underline; }

.binding-rail { display: grid; grid-template-columns: 1fr 24px 1fr 24px 1fr; align-items: center; margin: -4px 0 26px; padding: 16px; border: 1px solid var(--line-soft); border-radius: 9px; background: #F7F9F5; }
.binding-rail > div { min-width: 0; display: grid; grid-template-columns: 28px 1fr; column-gap: 9px; align-items: center; }
.binding-rail > div > span { grid-row: 1 / 3; display: grid; place-items: center; width: 28px; height: 28px; border: 1px solid var(--line); border-radius: 50%; color: var(--ink-3); background: #FFF; font: 700 11px var(--font-mono); }
.binding-rail > div b { color: var(--ink-2); font-size: 12px; }
.binding-rail > div small { overflow: hidden; color: var(--ink-3); font-size: 10px; text-overflow: ellipsis; white-space: nowrap; }
.binding-rail > div.active > span { border-color: var(--jade); background: var(--jade); color: #FFF; }
.binding-rail > div.active b { color: var(--jade-deep); }
.binding-rail > i { height: 1px; background: repeating-linear-gradient(90deg, var(--line) 0 4px, transparent 4px 8px); }
.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.compact-grid { grid-template-columns: 180px 1fr; align-items: start; }
.full-control { width: 100%; }
.standard-drop { display: flex; align-items: center; gap: 14px; min-height: 82px; padding: 14px 16px; border: 1px dashed #B9C7BB; border-radius: 8px; background: #FBFCFA; cursor: pointer; transition: border-color .16s ease, background-color .16s ease; }
.standard-drop:hover, .standard-drop:focus-within { border-color: var(--jade); background: var(--jade-soft); }
.standard-drop.filled { border-style: solid; }
.standard-drop input { position: absolute; width: 1px; height: 1px; opacity: 0; }
.standard-drop > i { color: var(--jade); font-size: 26px; }
.standard-drop > div { min-width: 0; flex: 1; display: flex; flex-direction: column; gap: 5px; }
.standard-drop b { overflow: hidden; color: var(--ink); font-size: 13px; text-overflow: ellipsis; white-space: nowrap; }
.standard-drop span { color: var(--ink-3); font-size: 11px; }
.standard-drop em { color: var(--jade-deep); font-size: 12px; font-style: normal; font-weight: 600; }
.upload-progress { margin-top: 8px; }
@media (max-width: 900px) {
  .table-toolbar { align-items: flex-start; flex-direction: column; }
  .toolbar-note, .sync-note { display: none; }
  .binding-rail { grid-template-columns: 1fr; gap: 9px; }
  .binding-rail > i { display: none; }
  .form-grid, .compact-grid { grid-template-columns: 1fr; gap: 0; }
}
</style>
