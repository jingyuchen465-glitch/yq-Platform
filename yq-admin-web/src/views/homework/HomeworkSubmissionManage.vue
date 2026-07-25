<template>
  <div class="grading-page page-container">
    <header class="page-head grading-head">
      <div>
        <button type="button" class="back-link" @click="$router.back()">
          <i class="el-icon-back"></i> 作业详情管理
        </button>
        <p class="eyebrow">ACADEMIC · REVIEW DESK</p>
        <h1>{{ overview.homeworkTitle || '学生作业批改' }}</h1>
        <p class="head-copy">
          {{ overview.className || $route.query.className || '班级作业' }}
          <span v-if="overview.homeworkDate">· {{ overview.homeworkDate }}</span>
          <span v-if="overview.deadline">· 截止 {{ formatDateTime(overview.deadline) }}</span>
        </p>
      </div>
      <div class="immutable-note">
        <i class="el-icon-lock"></i>
        <span><b>原始提交只读</b>文件、提交时间及归属信息不可修改</span>
      </div>
    </header>

    <section class="review-track" aria-label="提交批改概览">
      <div class="track-copy">
        <span>批改进度</span>
        <b>{{ reviewedPercent }}%</b>
      </div>
      <div class="track-line" aria-hidden="true">
        <i :style="{ width: reviewedPercent + '%' }"></i>
      </div>
      <dl>
        <div><dt>已提交</dt><dd>{{ overview.submissionCount || 0 }}</dd></div>
        <div><dt>已批改</dt><dd>{{ overview.reviewedCount || 0 }}</dd></div>
        <div><dt>待批改</dt><dd>{{ pendingCount }}</dd></div>
        <div><dt>逾期提交</dt><dd>{{ overview.lateCount || 0 }}</dd></div>
      </dl>
    </section>

    <section class="search-bar submission-search">
      <el-form :inline="true" :model="query" @submit.native.prevent="handleSearch">
        <el-form-item label="学生">
          <el-input
            v-model.trim="query.studentKeyword"
            clearable
            placeholder="姓名或账号"
            @keyup.enter.native="handleSearch"
            @clear="handleSearch"
          />
        </el-form-item>
        <el-form-item label="批改状态">
          <el-select v-model="query.reviewed" clearable placeholder="全部" @change="handleSearch">
            <el-option label="待批改" :value="false" />
            <el-option label="已批改" :value="true" />
          </el-select>
        </el-form-item>
        <el-form-item label="提交时效">
          <el-select v-model="query.lateSubmitted" clearable placeholder="全部" @change="handleSearch">
            <el-option label="按时提交" :value="false" />
            <el-option label="逾期提交" :value="true" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" :loading="loading" @click="handleSearch">
            查询提交
          </el-button>
        </el-form-item>
      </el-form>
    </section>

    <section v-if="accessDenied" class="table-card request-state">
      <i class="el-icon-lock"></i>
      <h2>暂无批改权限</h2>
      <p>请联系管理员开通作业提交查询、预览和批改权限。</p>
    </section>

    <section v-else class="table-card submissions-card">
      <el-table
        v-loading="loading"
        :data="submissions"
        row-key="id"
        empty-text="当前条件下没有学生提交"
      >
        <el-table-column label="学生" min-width="180">
          <template slot-scope="scope">
            <div class="student-cell">
              <span>{{ studentInitial(scope.row.studentName) }}</span>
              <div>
                <b>{{ scope.row.studentName }}</b>
                <small>{{ scope.row.studentUsername || `ID ${scope.row.studentId}` }}</small>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="提交文件" min-width="230">
          <template slot-scope="scope">
            <button type="button" class="file-link" @click="openSubmission(scope.row)">
              <i class="el-icon-document"></i>
              <span>{{ scope.row.contentFileName }}</span>
              <i class="el-icon-arrow-right"></i>
            </button>
          </template>
        </el-table-column>
        <el-table-column label="提交时间" min-width="160">
          <template slot-scope="scope">
            <div class="time-cell">
              <span>{{ formatDateTime(scope.row.submitTime) }}</span>
              <small v-if="scope.row.lateSubmitted" class="late-label">逾期</small>
              <small v-else>按时</small>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="批改状态" width="130" align="center">
          <template slot-scope="scope">
            <span class="review-state" :class="{ done: scope.row.reviewed }">
              <i></i>{{ scope.row.reviewed ? '已批改' : '待批改' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="分数" width="90" align="center">
          <template slot-scope="scope">
            <b v-if="scope.row.score !== null" class="score-text">{{ scope.row.score }}</b>
            <span v-else>—</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="115" align="right">
          <template slot-scope="scope">
            <el-button type="text" icon="el-icon-edit-outline" @click="openSubmission(scope.row)">
              {{ scope.row.reviewed ? '查看批改' : '开始批改' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next"
          :current-page="query.current"
          :page-size="query.size"
          :page-sizes="[10, 20, 50, 100]"
          :total="submissionTotal"
          @current-change="handleCurrentChange"
          @size-change="handleSizeChange"
        />
      </div>
    </section>

    <el-drawer
      :visible.sync="drawerVisible"
      direction="rtl"
      size="min(920px, 92vw)"
      :wrapper-closable="!saving"
      :before-close="beforeDrawerClose"
      custom-class="grading-drawer"
      append-to-body
    >
      <template slot="title">
        <div v-if="activeSubmission" class="drawer-title">
          <span>学生原始提交</span>
          <b>{{ activeSubmission.studentName }} · {{ activeSubmission.contentFileName }}</b>
        </div>
      </template>
      <div v-if="activeSubmission" class="drawer-body">
        <article class="markdown-sheet">
          <div class="sheet-meta">
            <div class="sheet-facts">
              <span><i class="el-icon-user"></i>{{ activeSubmission.studentName }}</span>
              <span><i class="el-icon-time"></i>{{ formatDateTime(activeSubmission.submitTime) }}</span>
              <span v-if="activeSubmission.lateSubmitted" class="late-label">逾期提交</span>
            </div>
            <el-button
              size="mini"
              icon="el-icon-download"
              :loading="downloading"
              :disabled="previewLoading || Boolean(previewError)"
              @click="downloadSubmission"
            >
              下载原文件
            </el-button>
          </div>
          <div v-if="previewLoading" class="preview-state">
            <i class="el-icon-loading"></i><span>正在读取 Markdown 原文</span>
          </div>
          <div v-else-if="previewError" class="preview-state error">
            <i class="el-icon-warning-outline"></i>
            <span>{{ previewError }}</span>
            <el-button type="text" @click="loadMarkdown">重新读取</el-button>
          </div>
          <div v-else class="markdown-body" v-html="markdownHtml"></div>
        </article>

        <aside class="grading-panel">
          <div class="panel-head">
            <span>教师批改</span>
            <i class="el-icon-lock"> 仅保存分数与评语</i>
          </div>
          <el-form ref="gradeForm" :model="gradeForm" :rules="gradeRules" label-position="top">
            <el-form-item label="分数（满分 100）" prop="score">
              <el-input-number
                v-model="gradeForm.score"
                :min="0"
                :max="100"
                :step="1"
                controls-position="right"
              />
            </el-form-item>
            <el-form-item label="教师评语" prop="teacherRemark">
              <el-input
                v-model="gradeForm.teacherRemark"
                type="textarea"
                :rows="7"
                maxlength="500"
                show-word-limit
                placeholder="记录完成情况、问题和改进建议"
              />
            </el-form-item>
          </el-form>
          <div class="readonly-fields">
            <span>以下字段受保护</span>
            <p>学生 ID <b>{{ activeSubmission.studentId }}</b></p>
            <p>提交 ID <b>{{ activeSubmission.id }}</b></p>
            <p>提交时间 <b>{{ formatDateTime(activeSubmission.submitTime) }}</b></p>
          </div>
          <el-button
            type="primary"
            class="save-grade"
            :loading="saving"
            :disabled="previewLoading"
            @click="saveGrade"
          >
            {{ saving ? '正在保存' : '保存分数与评语' }}
          </el-button>
        </aside>
      </div>
    </el-drawer>
  </div>
</template>

<script>
import {
  getHomeworkSubmissionDownloadUrl,
  gradeHomeworkSubmission,
  pageHomeworkSubmissions
} from '@/api/homework'
import { renderMarkdown } from '@/utils/markdown'

export default {
  name: 'HomeworkSubmissionManage',
  data() {
    return {
      loading: false,
      accessDenied: false,
      overview: {},
      submissions: [],
      submissionTotal: 0,
      query: {
        current: 1,
        size: 10,
        studentKeyword: '',
        reviewed: null,
        lateSubmitted: null
      },
      drawerVisible: false,
      activeSubmission: null,
      previewLoading: false,
      previewError: '',
      markdownHtml: '',
      submissionBlob: null,
      downloading: false,
      saving: false,
      gradeForm: {
        score: null,
        teacherRemark: ''
      },
      gradeRules: {
        score: [{ required: true, message: '请输入分数', trigger: 'change' }],
        teacherRemark: [{ max: 500, message: '教师评语不能超过500个字符', trigger: 'blur' }]
      }
    }
  },
  computed: {
    homeworkId() {
      return Number(this.$route.params.homeworkId)
    },
    pendingCount() {
      return Math.max(0, (this.overview.submissionCount || 0) - (this.overview.reviewedCount || 0))
    },
    reviewedPercent() {
      if (!this.overview.submissionCount) return 0
      return Math.round(this.overview.reviewedCount / this.overview.submissionCount * 100)
    }
  },
  created() {
    if (!Number.isInteger(this.homeworkId) || this.homeworkId <= 0) {
      this.$message.error('作业 ID 无效')
      this.$router.replace('/homework/status')
      return
    }
    this.loadData()
  },
  methods: {
    async loadData() {
      this.loading = true
      this.accessDenied = false
      try {
        const res = await pageHomeworkSubmissions(this.homeworkId, this.query)
        const data = res.data || {}
        this.overview = data
        this.submissions = (data.submissions && data.submissions.records) || []
        this.submissionTotal = (data.submissions && data.submissions.total) || 0
      } catch (error) {
        this.submissions = []
        this.submissionTotal = 0
        const code = Number(error.code || (error.response && error.response.status))
        if (code === 403) this.accessDenied = true
      } finally {
        this.loading = false
      }
    },
    handleSearch() {
      this.query.current = 1
      this.loadData()
    },
    handleCurrentChange(current) {
      this.query.current = current
      this.loadData()
    },
    handleSizeChange(size) {
      this.query.size = size
      this.query.current = 1
      this.loadData()
    },
    formatDateTime(value) {
      return value ? value.replace('T', ' ').slice(0, 16) : '—'
    },
    studentInitial(name) {
      return (name || '学').trim().slice(0, 1).toUpperCase()
    },
    async openSubmission(submission) {
      this.activeSubmission = submission
      this.gradeForm = {
        score: submission.score,
        teacherRemark: submission.teacherRemark || ''
      }
      this.markdownHtml = ''
      this.submissionBlob = null
      this.previewError = ''
      this.drawerVisible = true
      this.$nextTick(() => this.$refs.gradeForm && this.$refs.gradeForm.clearValidate())
      await this.loadMarkdown()
    },
    async loadMarkdown() {
      if (!this.activeSubmission) return
      this.previewLoading = true
      this.previewError = ''
      try {
        const urlRes = await getHomeworkSubmissionDownloadUrl(this.activeSubmission.id)
        // fetch 只读取响应，不导航到 OSS 地址，因此 Content-Disposition: attachment
        // 也不会触发浏览器下载；下载由页面内的专用按钮显式执行。
        const response = await fetch(urlRes.data.downloadUrl)
        if (!response.ok) throw new Error(`文件读取失败（HTTP ${response.status}）`)
        const fileBlob = await response.blob()
        const markdownText = await fileBlob.text()
        this.submissionBlob = fileBlob
        this.markdownHtml = renderMarkdown(markdownText)
      } catch (error) {
        this.submissionBlob = null
        this.previewError = error.message || '无法读取学生提交文件，请检查 OSS 跨域配置后重试'
      } finally {
        this.previewLoading = false
      }
    },
    downloadSubmission() {
      if (!this.submissionBlob || !this.activeSubmission) return
      this.downloading = true
      const objectUrl = URL.createObjectURL(this.submissionBlob)
      try {
        const link = document.createElement('a')
        link.href = objectUrl
        link.download = this.activeSubmission.contentFileName || 'homework-submission.md'
        link.style.display = 'none'
        document.body.appendChild(link)
        link.click()
        document.body.removeChild(link)
        this.$message.success('已开始下载原文件')
      } finally {
        window.setTimeout(() => URL.revokeObjectURL(objectUrl), 1000)
        this.downloading = false
      }
    },
    saveGrade() {
      this.$refs.gradeForm.validate(async valid => {
        if (!valid || !this.activeSubmission) return
        this.saving = true
        try {
          const res = await gradeHomeworkSubmission(this.activeSubmission.id, {
            score: this.gradeForm.score,
            teacherRemark: this.gradeForm.teacherRemark
          })
          const updated = res.data
          const index = this.submissions.findIndex(item => item.id === updated.id)
          if (index >= 0) this.$set(this.submissions, index, updated)
          this.activeSubmission = updated
          this.$message.success('分数与评语已保存')
          await this.loadData()
        } finally {
          this.saving = false
        }
      })
    },
    beforeDrawerClose(done) {
      if (!this.saving) done()
    }
  }
}
</script>

<style scoped>
.grading-page { padding-bottom: 30px; }
.grading-head { align-items: flex-end; }
.back-link { margin: 0 0 14px; padding: 0; border: 0; background: transparent; color: var(--jade-deep); font: inherit; font-size: 12px; cursor: pointer; }
.back-link i { margin-right: 5px; }
.head-copy { margin-top: 8px; color: var(--ink-3); font-size: 13px; }
.immutable-note { display: flex; align-items: center; gap: 10px; max-width: 300px; padding: 12px 14px; border: 1px solid var(--line); border-radius: 7px; background: rgba(255,255,255,.72); }
.immutable-note > i { color: var(--brass-ink); font-size: 19px; }
.immutable-note span { color: var(--ink-3); font-size: 10.5px; line-height: 1.5; }
.immutable-note b { display: block; color: var(--ink-2); font-size: 11px; }
.review-track { display: grid; grid-template-columns: 100px minmax(160px, 1fr) minmax(400px, auto); align-items: center; gap: 18px; margin: 0 0 16px; padding: 17px 20px; border: 1px solid #BFD3C8; border-radius: 9px; background: #F4F8F5; }
.track-copy span { display: block; color: var(--ink-3); font-size: 10px; letter-spacing: .1em; }
.track-copy b { display: block; margin-top: 3px; color: var(--jade-deep); font: 700 23px var(--font-mono); }
.track-line { height: 7px; overflow: hidden; border-radius: 10px; background: #DDE7E0; }
.track-line i { display: block; height: 100%; border-radius: inherit; background: linear-gradient(90deg, var(--jade), #61A989); transition: width .3s ease; }
.review-track dl { display: grid; grid-template-columns: repeat(4, minmax(70px, 1fr)); gap: 20px; margin: 0; }
.review-track dl div { padding-left: 14px; border-left: 1px solid #CEDDD4; }
.review-track dt { color: var(--ink-3); font-size: 10px; }
.review-track dd { margin: 3px 0 0; color: var(--ink); font: 600 17px var(--font-mono); }
.submission-search { padding-bottom: 4px; }
.submission-search .el-select { width: 126px; }
.submissions-card { position: relative; overflow: hidden; }
.submissions-card::before { content: ''; position: absolute; inset: 0 auto 0 0; width: 3px; background: var(--jade); }
.student-cell { display: flex; align-items: center; gap: 10px; }
.student-cell > span { display: grid; width: 33px; height: 33px; place-items: center; border-radius: 7px; background: var(--jade-soft); color: var(--jade-deep); font-family: var(--font-display); font-weight: 700; }
.student-cell div { display: flex; min-width: 0; flex-direction: column; gap: 3px; }
.student-cell b { color: var(--ink); font-size: 13px; }
.student-cell small { color: var(--ink-3); font: 9.5px var(--font-mono); }
.file-link { display: flex; width: 100%; align-items: center; gap: 8px; padding: 0; border: 0; background: transparent; color: var(--jade-deep); font: inherit; font-size: 12px; text-align: left; cursor: pointer; }
.file-link span { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.file-link .el-icon-arrow-right { margin-left: auto; color: var(--ink-3); transition: transform .16s ease; }
.file-link:hover .el-icon-arrow-right { transform: translateX(3px); }
.time-cell { display: flex; align-items: center; gap: 8px; color: var(--ink-2); font: 11px var(--font-mono); }
.time-cell small { color: var(--jade-deep); font-family: var(--font-body); }
.late-label { color: #B95D3F !important; }
.review-state { display: inline-flex; align-items: center; gap: 6px; color: var(--ink-3); font-size: 11px; }
.review-state i { width: 7px; height: 7px; border-radius: 50%; background: #BBC3BC; }
.review-state.done { color: var(--jade-deep); font-weight: 600; }
.review-state.done i { background: var(--jade); box-shadow: 0 0 0 3px var(--jade-soft); }
.score-text { color: var(--jade-deep); font: 700 16px var(--font-mono); }
.request-state { display: flex; min-height: 300px; flex-direction: column; align-items: center; justify-content: center; text-align: center; }
.request-state > i { color: var(--brass-ink); font-size: 42px; }
.request-state h2 { margin: 14px 0 6px; color: var(--ink); font-size: 18px; }
.request-state p { color: var(--ink-3); font-size: 12px; }

@media (max-width: 1050px) {
  .review-track { grid-template-columns: 90px 1fr; }
  .review-track dl { grid-column: 1 / -1; }
}
@media (max-width: 720px) {
  .grading-page { padding: 14px; }
  .grading-head { align-items: flex-start; }
  .immutable-note { display: none; }
  .review-track { grid-template-columns: 80px 1fr; padding: 14px; }
  .review-track dl { grid-template-columns: 1fr 1fr; gap: 12px; }
  .submission-search .el-form { display: grid; }
  .submission-search .el-form-item { margin-right: 0; }
  .submission-search .el-select { width: 100%; }
}
</style>

<style>
.grading-drawer .el-drawer__header { margin-bottom: 0; padding: 18px 22px 14px; border-bottom: 1px solid #E2E7E1; color: #17241E; }
.grading-drawer .el-drawer__body { overflow: hidden; }
.drawer-title { display: flex; min-width: 0; flex-direction: column; gap: 3px; }
.drawer-title span { color: #748079; font-size: 9px; letter-spacing: .12em; }
.drawer-title b { overflow: hidden; font-size: 13px; text-overflow: ellipsis; white-space: nowrap; }
.drawer-body { display: grid; height: 100%; grid-template-columns: minmax(0, 1fr) 300px; overflow: hidden; background: #EEF1ED; }
.markdown-sheet { min-width: 0; margin: 18px; overflow-y: auto; border: 1px solid #DDE3DC; border-radius: 8px; background: #FFFFFF; box-shadow: 0 8px 24px rgba(29, 50, 39, .06); }
.sheet-meta { position: sticky; top: 0; z-index: 2; display: flex; align-items: center; justify-content: space-between; gap: 14px; padding: 9px 14px 9px 18px; border-bottom: 1px solid #E8ECE7; background: rgba(250,251,249,.96); color: #66736B; font-size: 10px; backdrop-filter: blur(5px); }
.sheet-facts { display: flex; flex-wrap: wrap; gap: 10px 18px; }
.sheet-facts span { display: inline-flex; align-items: center; gap: 5px; }
.sheet-meta .el-button { flex: none; border-color: #B8CBC0; color: #176B53; background: #F5FAF7; }
.sheet-meta .el-button:hover, .sheet-meta .el-button:focus { border-color: #278066; color: #115B46; background: #EBF6F0; }
.preview-state { display: flex; min-height: 400px; align-items: center; justify-content: center; gap: 9px; color: #6D7972; font-size: 12px; }
.preview-state > i { font-size: 21px; }
.preview-state.error { flex-direction: column; color: #B45F45; }
.markdown-body { max-width: 760px; margin: 0 auto; padding: 36px 42px 70px; color: #26342D; font: 14px/1.78 -apple-system, BlinkMacSystemFont, "Segoe UI", "Microsoft YaHei", sans-serif; }
.markdown-body h1, .markdown-body h2, .markdown-body h3, .markdown-body h4 { margin: 1.55em 0 .65em; color: #14251C; line-height: 1.3; }
.markdown-body h1 { margin-top: 0; padding-bottom: .4em; border-bottom: 2px solid #C6D5CC; font-size: 27px; }
.markdown-body h2 { padding-bottom: .35em; border-bottom: 1px solid #DEE6E0; font-size: 21px; }
.markdown-body h3 { font-size: 17px; }
.markdown-body p { margin: .75em 0; }
.markdown-body a { color: #116F56; }
.markdown-body code { padding: 2px 5px; border-radius: 4px; background: #EEF4F0; color: #A8462F; font: 12px Consolas, monospace; }
.markdown-body pre { overflow-x: auto; margin: 1.2em 0; padding: 16px 18px; border-left: 3px solid #B98A2F; border-radius: 5px; background: #18251F; }
.markdown-body pre code { padding: 0; background: transparent; color: #E5ECE7; line-height: 1.65; }
.markdown-body blockquote { margin: 1.2em 0; padding: 9px 16px; border-left: 3px solid #2A8469; background: #F1F7F4; color: #52625A; }
.markdown-body ul, .markdown-body ol { padding-left: 1.6em; }
.markdown-body hr { margin: 2em 0; border: 0; border-top: 1px solid #DCE4DE; }
.md-table-wrap { overflow-x: auto; }
.markdown-body table { width: 100%; border-collapse: collapse; font-size: 13px; }
.markdown-body th, .markdown-body td { padding: 8px 10px; border: 1px solid #DCE4DE; text-align: left; }
.markdown-body th { background: #F1F5F2; color: #20362B; }
.grading-panel { overflow-y: auto; padding: 20px; border-left: 1px solid #DCE3DD; background: #FFFFFF; }
.panel-head { display: flex; align-items: flex-start; justify-content: space-between; margin-bottom: 20px; padding-bottom: 13px; border-bottom: 1px solid #E5EAE5; }
.panel-head > span { color: #17241E; font-size: 16px; font-weight: 700; }
.panel-head i { color: #8A7350; font-size: 9px; font-style: normal; }
.grading-panel .el-input-number { width: 100%; }
.readonly-fields { margin-top: 20px; padding: 12px 14px; border: 1px dashed #CDD5CE; border-radius: 6px; background: #F8FAF8; }
.readonly-fields > span { color: #8A7350; font-size: 9px; letter-spacing: .1em; }
.readonly-fields p { display: flex; justify-content: space-between; margin: 8px 0 0; color: #727E76; font-size: 10px; }
.readonly-fields b { max-width: 165px; overflow: hidden; color: #3B4841; font: 500 10px Consolas, monospace; text-overflow: ellipsis; white-space: nowrap; }
.save-grade { width: 100%; margin-top: 18px; }
@media (max-width: 760px) {
  .drawer-body { grid-template-columns: 1fr; overflow-y: auto; }
  .markdown-sheet { min-height: 430px; overflow: visible; }
  .grading-panel { overflow: visible; border-top: 1px solid #DCE3DD; border-left: 0; }
  .markdown-body { padding: 28px 22px 48px; }
  .sheet-meta { align-items: flex-start; }
  .sheet-facts { display: grid; gap: 5px; }
}
@media (prefers-reduced-motion: reduce) {
  .track-line i, .file-link .el-icon-arrow-right { transition: none !important; }
}
</style>
