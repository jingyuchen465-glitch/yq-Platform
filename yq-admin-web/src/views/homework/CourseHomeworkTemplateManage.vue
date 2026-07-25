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

    <el-dialog
      :visible.sync="previewVisible"
      width="92%"
      top="4vh"
      custom-class="template-preview-dialog"
      :close-on-click-modal="false"
      append-to-body
      @closed="resetPreview"
    >
      <div slot="title" class="preview-title">
        <span class="preview-seal"><i class="el-icon-notebook-2"></i></span>
        <div>
          <b>{{ previewData.fileName || (previewTarget && previewTarget.contentFileName) || '作业标准预览' }}</b>
          <small>
            {{ previewTarget ? previewTarget.courseName : '' }}
            <template v-if="previewPosition"> · {{ previewPosition }}</template>
            <template v-if="previewData.expireSeconds"> · 地址有效 {{ formatExpire(previewData.expireSeconds) }}</template>
          </small>
        </div>
      </div>

      <div class="preview-toolbar">
        <span><i class="el-icon-lock"></i> 文件通过 OSS 临时地址加载</span>
        <el-button
          size="small"
          icon="el-icon-refresh"
          :loading="previewLoading"
          :disabled="!previewTarget"
          @click="refreshPreview"
        >刷新地址</el-button>
      </div>

      <div v-loading="previewLoading" class="preview-stage">
        <article
          v-if="previewKind === 'markdown' && previewHtml"
          class="preview-markdown"
          v-html="previewHtml"
        />
        <pre v-else-if="previewKind === 'text' && previewText" class="preview-text">{{ previewText }}</pre>
        <img
          v-else-if="previewData.previewUrl && previewKind === 'image'"
          :src="previewData.previewUrl"
          :alt="previewData.fileName || '作业标准预览'"
          class="preview-image"
        >
        <iframe
          v-else-if="previewData.previewUrl && (previewKind === 'pdf' || previewFallback)"
          :key="previewData.previewUrl"
          :src="previewData.previewUrl"
          :title="`${previewData.fileName || '作业标准'}预览`"
          class="preview-frame"
        />
        <div v-else-if="previewError" class="preview-state is-error">
          <i class="el-icon-warning-outline"></i>
          <b>预览加载失败</b>
          <span>{{ previewError }}</span>
          <el-button type="text" @click="refreshPreview">重新加载</el-button>
        </div>
        <div v-else-if="!previewLoading" class="preview-state">
          <i class="el-icon-document"></i>
          <b>该格式无法在浏览器中直接预览</b>
          <span>可以使用下方“下载原文件”在本地打开。</span>
        </div>
      </div>

      <div slot="footer" class="preview-footer">
        <p><i class="el-icon-info"></i> PDF、图片、Markdown 和文本可在线查看，Office 文档请下载后打开。</p>
        <div>
          <el-button @click="previewVisible = false">关闭</el-button>
          <el-button
            type="primary"
            icon="el-icon-download"
            :loading="previewDownloading"
            :disabled="!previewTarget"
            @click="downloadPreviewFile"
          >下载原文件</el-button>
        </div>
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
  getCourseHomeworkTemplatePreview,
  getCourseHomeworkTemplateDownload
} from '@/api/courseHomeworkTemplate'
import { uploadToOssWithProgress } from '@/utils/ossUpload'
import { renderMarkdown } from '@/utils/markdown'

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
      previewVisible: false,
      previewLoading: false,
      previewDownloading: false,
      previewTarget: null,
      previewData: {},
      previewHtml: '',
      previewText: '',
      previewFallback: false,
      previewError: '',
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
    },
    previewPosition() {
      if (!this.previewTarget) return ''
      return this.previewTarget.teachingMode === 'ONLINE'
        ? this.previewTarget.stageName
        : `D${this.previewTarget.dayNumber}`
    },
    previewKind() {
      const fileName = this.previewData.fileName ||
        (this.previewTarget && this.previewTarget.contentFileName) || ''
      const extension = fileName.includes('.') ? fileName.split('.').pop().toLowerCase() : ''
      if (['md', 'markdown'].includes(extension)) return 'markdown'
      if (['txt', 'log', 'csv', 'json', 'xml'].includes(extension)) return 'text'
      if (extension === 'pdf') return 'pdf'
      if (['png', 'jpg', 'jpeg', 'gif', 'webp', 'svg'].includes(extension)) return 'image'
      return 'unsupported'
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
      this.previewTarget = row
      this.previewVisible = true
      await this.loadPreview(row.id)
    },
    async loadPreview(id) {
      this.previewLoading = true
      this.previewError = ''
      this.previewHtml = ''
      this.previewText = ''
      this.previewFallback = false
      try {
        const res = await getCourseHomeworkTemplatePreview(id)
        this.previewData = res.data || {}
        if (['markdown', 'text'].includes(this.previewKind)) {
          try {
            const response = await fetch(this.previewData.previewUrl)
            if (!response.ok) throw new Error(`OSS 文件读取失败（HTTP ${response.status}）`)
            const content = await response.text()
            if (this.previewKind === 'markdown') this.previewHtml = renderMarkdown(content)
            else this.previewText = content
          } catch (error) {
            // 跨域未放行时仍可借助 inline 预览地址在 iframe 中展示原文。
            this.previewFallback = true
          }
        }
      } catch (error) {
        this.previewData = {}
        this.previewError = error.message || '作业标准预览加载失败'
      } finally {
        this.previewLoading = false
      }
    },
    refreshPreview() {
      if (this.previewTarget) this.loadPreview(this.previewTarget.id)
    },
    async downloadPreviewFile() {
      if (!this.previewTarget) return
      this.previewDownloading = true
      try {
        const res = await getCourseHomeworkTemplateDownload(this.previewTarget.id)
        const link = document.createElement('a')
        link.href = res.data.downloadUrl
        link.style.display = 'none'
        document.body.appendChild(link)
        link.click()
        document.body.removeChild(link)
        this.$message.success('已开始下载原文件')
      } finally {
        this.previewDownloading = false
      }
    },
    resetPreview() {
      this.previewTarget = null
      this.previewData = {}
      this.previewHtml = ''
      this.previewText = ''
      this.previewFallback = false
      this.previewError = ''
      this.previewLoading = false
      this.previewDownloading = false
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
    },
    formatExpire(seconds) {
      if (!seconds) return '一段时间'
      if (seconds >= 3600) return `${Math.round(seconds / 3600)} 小时`
      return `${Math.max(1, Math.round(seconds / 60))} 分钟`
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
.preview-title { display: flex; align-items: center; gap: 12px; padding-right: 42px; }
.preview-seal { display: grid; place-items: center; width: 36px; height: 36px; flex: none; border-radius: 7px; color: var(--brass-ink); background: var(--brass-soft); font-size: 17px; }
.preview-title > div { min-width: 0; display: flex; flex-direction: column; gap: 3px; }
.preview-title b { overflow: hidden; color: var(--ink); font: 700 15px var(--font-display); text-overflow: ellipsis; white-space: nowrap; }
.preview-title small { overflow: hidden; color: var(--ink-3); font: 10px var(--font-mono); letter-spacing: .04em; text-overflow: ellipsis; white-space: nowrap; }
.preview-toolbar { display: flex; align-items: center; justify-content: space-between; gap: 16px; min-height: 48px; padding: 8px 13px; border: 1px solid var(--line); border-bottom: 0; border-radius: 8px 8px 0 0; background: #F7F9F5; }
.preview-toolbar > span { color: var(--ink-3); font-size: 11px; }
.preview-toolbar > span i { margin-right: 6px; color: var(--jade); }
.preview-stage { position: relative; min-height: 66vh; border: 1px solid var(--line); background: #DDE3DC; }
.preview-frame { display: block; width: 100%; height: 66vh; border: 0; background: #FFF; }
.preview-image { display: block; max-width: 100%; max-height: 66vh; margin: auto; object-fit: contain; }
.preview-text { box-sizing: border-box; min-height: 66vh; max-height: 66vh; margin: 0; overflow: auto; padding: 32px 38px; background: #FFF; color: #26342D; font: 13px/1.75 var(--font-mono); white-space: pre-wrap; word-break: break-word; }
.preview-markdown { box-sizing: border-box; min-height: 66vh; max-height: 66vh; overflow-y: auto; padding: 36px 48px 64px; background: #FFF; color: #26342D; font: 14px/1.78 -apple-system, BlinkMacSystemFont, "Segoe UI", "Microsoft YaHei", sans-serif; }
.preview-markdown h1, .preview-markdown h2, .preview-markdown h3 { margin: 1.5em 0 .65em; color: #14251C; line-height: 1.3; }
.preview-markdown h1 { margin-top: 0; padding-bottom: .4em; border-bottom: 2px solid #C6D5CC; font-size: 27px; }
.preview-markdown h2 { padding-bottom: .35em; border-bottom: 1px solid #DEE6E0; font-size: 21px; }
.preview-markdown h3 { font-size: 17px; }
.preview-markdown p { margin: .75em 0; }
.preview-markdown a { color: #116F56; }
.preview-markdown code { padding: 2px 5px; border-radius: 4px; background: #EEF4F0; color: #A8462F; font: 12px Consolas, monospace; }
.preview-markdown pre { overflow-x: auto; padding: 16px 18px; border-left: 3px solid #B98A2F; border-radius: 5px; background: #18251F; }
.preview-markdown pre code { padding: 0; background: transparent; color: #E5ECE7; }
.preview-markdown blockquote { margin: 1.2em 0; padding: 9px 16px; border-left: 3px solid #2A8469; background: #F1F7F4; color: #52625A; }
.preview-markdown table { width: 100%; border-collapse: collapse; font-size: 13px; }
.preview-markdown th, .preview-markdown td { padding: 8px 10px; border: 1px solid #DCE4DE; text-align: left; }
.preview-markdown th { background: #F1F5F2; }
.preview-state { min-height: 66vh; display: grid; place-content: center; justify-items: center; padding: 20px; color: var(--ink-3); text-align: center; }
.preview-state i { margin-bottom: 13px; font-size: 34px; }
.preview-state b { color: var(--ink-2); font-size: 14px; }
.preview-state span { max-width: 560px; margin-top: 6px; font-size: 11px; line-height: 1.6; }
.preview-state.is-error { color: #B45F45; }
.preview-footer { display: flex; align-items: center; justify-content: space-between; gap: 20px; }
.preview-footer p { margin: 0; color: var(--ink-3); font-size: 11px; text-align: left; }
.preview-footer p i { margin-right: 5px; color: var(--brass-ink); }
@media (max-width: 900px) {
  .table-toolbar { align-items: flex-start; flex-direction: column; }
  .toolbar-note, .sync-note { display: none; }
  .binding-rail { grid-template-columns: 1fr; gap: 9px; }
  .binding-rail > i { display: none; }
  .form-grid, .compact-grid { grid-template-columns: 1fr; gap: 0; }
  .preview-footer { align-items: stretch; flex-direction: column; }
  .preview-footer > div { display: flex; }
  .preview-footer .el-button { flex: 1; }
  .preview-markdown { padding: 26px 20px 44px; }
}
</style>

<style>
.template-preview-dialog { max-width: 1280px; border-radius: 10px; }
.template-preview-dialog .el-dialog__header { padding: 13px 18px; }
.template-preview-dialog .el-dialog__body { padding: 14px 18px 0; }
.template-preview-dialog .el-dialog__footer { padding: 12px 18px 14px; }
@media (max-width: 700px) {
  .template-preview-dialog { width: calc(100% - 20px) !important; margin-top: 10px !important; }
  .template-preview-dialog .el-dialog__body { padding: 10px 10px 0; }
  .template-preview-dialog .el-dialog__footer { padding: 10px; }
}
</style>
