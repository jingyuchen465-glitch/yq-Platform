<template>
  <div class="status-page page-container">
    <header class="page-head status-head">
      <div>
        <p class="eyebrow">ACADEMIC · HOMEWORK LEDGER</p>
        <h1>作业详情管理</h1>
        <p class="head-copy">按日期查找班级作业，维护标准答案，并进入学生提交批改台。</p>
      </div>
      <div class="date-stamp">
        <span>{{ dateTypeLabel }}</span>
        <b>{{ query.queryDate }}</b>
      </div>
    </header>

    <section class="search-bar ledger-search">
      <el-form :inline="true" :model="query" @submit.native.prevent="handleSearch">
        <el-form-item label="日期口径">
          <el-select v-model="query.dateType" class="date-type-select" @change="handleSearch">
            <el-option label="发布日期" value="HOMEWORK_DATE" />
            <el-option label="截止日期" value="DEADLINE_DATE" />
          </el-select>
        </el-form-item>
        <el-form-item :label="dateTypeLabel">
          <el-date-picker
            v-model="query.queryDate"
            type="date"
            value-format="yyyy-MM-dd"
            format="yyyy 年 MM 月 dd 日"
            :clearable="false"
            @change="handleSearch"
          />
        </el-form-item>
        <el-form-item label="班级">
          <el-input
            v-model.trim="query.className"
            clearable
            placeholder="输入班级名称"
            @keyup.enter.native="handleSearch"
            @clear="handleSearch"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" :loading="loading" @click="handleSearch">
            查询作业
          </el-button>
        </el-form-item>
      </el-form>
    </section>

    <section v-if="accessDenied" class="table-card request-state permission-state">
      <i class="el-icon-lock"></i>
      <h2>暂无访问权限</h2>
      <p>你的账号暂时不能查看作业详情，如有需要请联系管理员开通相应权限。</p>
      <el-button icon="el-icon-back" @click="$router.back()">返回上一页</el-button>
    </section>

    <section v-else-if="loadError" class="table-card request-state error-state">
      <i class="el-icon-warning-outline"></i>
      <h2>数据加载失败</h2>
      <p>{{ loadError }}</p>
      <el-button type="primary" icon="el-icon-refresh" :loading="loading" @click="loadData">
        重新加载
      </el-button>
    </section>

    <section v-else class="table-card ledger-card">
      <div class="ledger-tally">
        <div>
          <span>班级总数</span>
          <b>{{ total }}</b>
        </div>
        <div>
          <span>{{ query.dateType === 'HOMEWORK_DATE' ? '本页已发布' : '本页有到期作业' }}</span>
          <b>{{ publishedClassCount }}</b>
        </div>
        <div>
          <span>本页匹配作业</span>
          <b>{{ matchedHomeworkCount }}</b>
        </div>
        <div>
          <span>本页已传答案</span>
          <b>{{ answeredCount }}</b>
        </div>
      </div>

      <el-table
        v-loading="loading"
        :data="records"
        row-key="classId"
        stripe
        class="status-table"
        empty-text="当前条件下没有班级数据"
      >
        <el-table-column label="班级" width="190">
          <template slot-scope="scope">
            <div class="class-cell">
              <span class="class-index mono">CLASS {{ String(scope.$index + 1).padStart(2, '0') }}</span>
              <b>{{ scope.row.className }}</b>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="匹配情况" width="145" align="center">
          <template slot-scope="scope">
            <span v-if="scope.row.published" class="publish-state is-published">
              <i></i>{{ publishedStatusText }}
            </span>
            <span v-else class="publish-state">
              <i></i>{{ unpublishedStatusText }}
            </span>
            <small v-if="scope.row.homeworkCount > 1" class="match-count">
              共 {{ scope.row.homeworkCount }} 份
            </small>
          </template>
        </el-table-column>

        <el-table-column label="作业与标准答案" min-width="720">
          <template slot-scope="scope">
            <div v-if="scope.row.homeworks.length" class="homework-stack">
              <article
                v-for="homework in scope.row.homeworks"
                :key="homework.id"
                class="homework-record"
              >
                <div class="record-main">
                  <div class="record-title">
                    <b>{{ homework.title }}</b>
                    <span class="code-chip">#{{ homework.id }}</span>
                  </div>
                  <p>{{ homework.classContent }}</p>
                  <div class="record-meta">
                    <span><i class="el-icon-date"></i> 发布 {{ homework.homeworkDate }}</span>
                    <span><i class="el-icon-timer"></i> 截止 {{ formatDateTime(homework.deadline) }}</span>
                    <span><i class="el-icon-document"></i> {{ homework.contentFileName }}</span>
                  </div>
                </div>

                <div class="answer-desk" :class="{ ready: homework.answerObjectKey }">
                  <div class="answer-file">
                    <span>标准答案</span>
                    <b>{{ homework.answerFileName || '尚未上传' }}</b>
                  </div>
                  <div class="answer-actions">
                    <el-button
                      type="primary"
                      size="mini"
                      icon="el-icon-edit-outline"
                      @click="manageSubmissions(homework, scope.row)"
                    >
                      批改提交
                    </el-button>
                    <el-button
                      size="mini"
                      :icon="homework.answerObjectKey ? 'el-icon-refresh' : 'el-icon-upload2'"
                      @click="openAnswerDialog(homework)"
                    >
                      {{ homework.answerObjectKey ? '替换' : '上传' }}
                    </el-button>
                    <el-button
                      v-if="homework.answerObjectKey"
                      type="text"
                      size="mini"
                      icon="el-icon-view"
                      @click="previewAnswer(homework)"
                    >
                      查看
                    </el-button>
                  </div>
                  <div class="visibility-control">
                    <span>学生可见</span>
                    <el-tooltip
                      :disabled="Boolean(homework.answerObjectKey)"
                      content="请先上传标准答案"
                      placement="top"
                    >
                      <span>
                        <el-switch
                          :value="homework.answerStudentVisible"
                          :disabled="!homework.answerObjectKey || Boolean(visibilityUpdating[homework.id])"
                          active-color="#177E63"
                          inactive-color="#C6CEC3"
                          @change="value => handleVisibilityChange(homework, value)"
                        />
                      </span>
                    </el-tooltip>
                  </div>
                </div>
              </article>
            </div>
            <div v-else class="no-homework">
              <i class="el-icon-collection-tag"></i>
              <div>
                <b>{{ unpublishedStatusText }}</b>
                <span>{{ emptyStatusHint }}</span>
              </div>
            </div>
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
          :total="total"
          @current-change="handleCurrentChange"
          @size-change="handleSizeChange"
        />
      </div>
    </section>

    <el-dialog
      title="上传标准答案"
      :visible.sync="answerDialogVisible"
      width="520px"
      append-to-body
      :close-on-click-modal="!answerUploading"
      :close-on-press-escape="!answerUploading"
      @closed="resetAnswerDialog"
    >
      <div v-if="answerTarget" class="answer-dialog-body">
        <div class="answer-target">
          <span>对应作业</span>
          <b>{{ answerTarget.title }}</b>
          <small>{{ answerTarget.classContent }}</small>
        </div>
        <label
          class="answer-drop"
          :class="{ 'has-file': answerFile }"
          @dragover.prevent
          @drop.prevent="handleAnswerDrop"
        >
          <input
            ref="answerFileInput"
            type="file"
            accept=".md,.markdown,.pdf,.doc,.docx,.txt"
            :disabled="answerUploading"
            @change="handleAnswerFileInput"
          >
          <i :class="answerFile ? 'el-icon-document-checked' : 'el-icon-upload'"></i>
          <b>{{ answerFile ? answerFile.name : '选择或拖入标准答案' }}</b>
          <span>{{ answerFile ? formatFileSize(answerFile.size) : '支持 Markdown、PDF、Word、TXT，最大 20 MB' }}</span>
        </label>
        <el-progress
          v-if="answerUploading"
          :percentage="answerUploadProgress"
          :stroke-width="6"
          class="answer-progress"
        />
      </div>
      <span slot="footer" class="dialog-footer">
        <el-button :disabled="answerUploading" @click="answerDialogVisible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="answerUploading"
          :disabled="!answerFile"
          @click="handleAnswerUpload"
        >
          {{ answerUploading ? '正在上传' : '保存标准答案' }}
        </el-button>
      </span>
    </el-dialog>

    <el-dialog
      :title="answerPreviewTitle"
      :visible.sync="answerPreviewVisible"
      width="min(900px, 92vw)"
      top="5vh"
      append-to-body
      custom-class="answer-preview-dialog"
      @closed="resetAnswerPreview"
    >
      <div class="answer-preview-toolbar">
        <div>
          <span>标准答案原文件</span>
          <b>{{ answerPreviewTarget ? answerPreviewTarget.answerFileName : '' }}</b>
        </div>
        <el-button
          size="mini"
          icon="el-icon-download"
          :disabled="!answerPreviewBlob"
          @click="downloadPreviewedAnswer"
        >
          下载原文件
        </el-button>
      </div>

      <div v-if="answerPreviewLoading" class="answer-preview-state">
        <i class="el-icon-loading"></i>
        <span>正在读取文件内容</span>
      </div>
      <div v-else-if="answerPreviewError" class="answer-preview-state is-error">
        <i class="el-icon-warning-outline"></i>
        <span>{{ answerPreviewError }}</span>
        <el-button type="text" @click="loadAnswerPreview">重新读取</el-button>
      </div>
      <iframe
        v-else-if="answerPreviewType === 'pdf'"
        :src="answerPreviewObjectUrl"
        class="answer-pdf-frame"
        title="标准答案 PDF 预览"
      />
      <article
        v-else-if="answerPreviewType === 'markdown'"
        class="answer-preview-markdown"
        v-html="answerPreviewHtml"
      ></article>
      <div v-else class="answer-preview-state is-unsupported">
        <i class="el-icon-document"></i>
        <b>此文件格式不能在浏览器中直接预览</b>
        <span>可以使用上方“下载原文件”按钮在本地打开。</span>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  pageHomeworkStatus,
  saveHomeworkAnswer,
  updateHomeworkAnswerVisibility
} from '@/api/homework'
import { getOssDownloadUrl, uploadToOssWithProgress } from '@/utils/ossUpload'
import { renderMarkdown } from '@/utils/markdown'

const MAX_ANSWER_SIZE = 20 * 1024 * 1024
const ANSWER_FILE_PATTERN = /\.(md|markdown|pdf|doc|docx|txt)$/i

function formatToday() {
  const date = new Date()
  const pad = value => String(value).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

export default {
  name: 'HomeworkStatus',
  data() {
    return {
      loading: false,
      records: [],
      total: 0,
      accessDenied: false,
      loadError: '',
      query: {
        current: 1,
        size: 10,
        dateType: 'HOMEWORK_DATE',
        queryDate: formatToday(),
        className: ''
      },
      visibilityUpdating: {},
      answerDialogVisible: false,
      answerTarget: null,
      answerFile: null,
      answerUploading: false,
      answerUploadProgress: 0,
      answerPreviewVisible: false,
      answerPreviewTarget: null,
      answerPreviewLoading: false,
      answerPreviewError: '',
      answerPreviewType: '',
      answerPreviewHtml: '',
      answerPreviewBlob: null,
      answerPreviewObjectUrl: ''
    }
  },
  computed: {
    dateTypeLabel() {
      return this.query.dateType === 'HOMEWORK_DATE' ? '发布日期' : '截止日期'
    },
    publishedStatusText() {
      return this.query.dateType === 'HOMEWORK_DATE' ? '已发布' : '有到期作业'
    },
    unpublishedStatusText() {
      return this.query.dateType === 'HOMEWORK_DATE' ? '未发布' : '无到期作业'
    },
    emptyStatusHint() {
      return this.query.dateType === 'HOMEWORK_DATE'
        ? '该班级在所选日期没有发布作业'
        : '该班级没有作业在所选日期截止'
    },
    publishedClassCount() {
      return this.records.filter(item => item.published).length
    },
    matchedHomeworkCount() {
      return this.records.reduce((total, item) => total + item.homeworkCount, 0)
    },
    answeredCount() {
      return this.records.reduce((total, item) => {
        return total + item.homeworks.filter(homework => homework.answerObjectKey).length
      }, 0)
    },
    answerPreviewTitle() {
      return this.answerPreviewTarget
        ? `查看标准答案 · ${this.answerPreviewTarget.title}`
        : '查看标准答案'
    }
  },
  created() {
    this.loadData()
  },
  methods: {
    async loadData() {
      this.loading = true
      this.accessDenied = false
      this.loadError = ''
      try {
        const res = await pageHomeworkStatus(this.query)
        this.records = res.data.records || []
        this.total = res.data.total || 0
      } catch (error) {
        this.records = []
        this.total = 0
        const responseCode = error.response && error.response.data && error.response.data.code
        const responseStatus = error.response && error.response.status
        const errorCode = Number(responseCode || responseStatus || error.code)
        if (errorCode === 403) {
          this.accessDenied = true
        } else {
          this.loadError = error.message || '暂时无法加载数据，请稍后重试'
        }
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
    formatFileSize(size) {
      if (size < 1024) return `${size} B`
      if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
      return `${(size / 1024 / 1024).toFixed(1)} MB`
    },
    manageSubmissions(homework, classRow) {
      this.$router.push({
        name: 'HomeworkSubmissionManage',
        params: { homeworkId: homework.id },
        query: { className: classRow.className }
      })
    },
    openAnswerDialog(homework) {
      this.answerTarget = homework
      this.answerFile = null
      this.answerUploadProgress = 0
      this.answerDialogVisible = true
    },
    handleAnswerFileInput(event) {
      const file = event.target.files && event.target.files[0]
      this.selectAnswerFile(file)
    },
    handleAnswerDrop(event) {
      if (this.answerUploading) return
      const file = event.dataTransfer.files && event.dataTransfer.files[0]
      this.selectAnswerFile(file)
    },
    selectAnswerFile(file) {
      if (!file) return
      if (!ANSWER_FILE_PATTERN.test(file.name)) {
        this.$message.error('标准答案仅支持 Markdown、PDF、Word 或 TXT 文件')
        this.clearAnswerFile()
        return
      }
      if (file.size > MAX_ANSWER_SIZE) {
        this.$message.error('标准答案文件不能超过 20 MB')
        this.clearAnswerFile()
        return
      }
      this.answerFile = file
      this.answerUploadProgress = 0
    },
    clearAnswerFile() {
      this.answerFile = null
      if (this.$refs.answerFileInput) this.$refs.answerFileInput.value = ''
    },
    async handleAnswerUpload() {
      if (!this.answerTarget || !this.answerFile) return
      this.answerUploading = true
      try {
        const uploadResult = await uploadToOssWithProgress(
          this.answerFile,
          'homework-answer',
          percent => { this.answerUploadProgress = percent }
        )
        const res = await saveHomeworkAnswer(this.answerTarget.id, {
          answerObjectKey: uploadResult.objectKey,
          answerFileName: this.answerFile.name
        })
        this.replaceHomeworkItem(res.data)
        this.$message.success('标准答案已保存')
        this.answerDialogVisible = false
      } finally {
        this.answerUploading = false
      }
    },
    async handleVisibilityChange(homework, studentVisible) {
      this.$set(this.visibilityUpdating, homework.id, true)
      try {
        const res = await updateHomeworkAnswerVisibility(homework.id, { studentVisible })
        this.replaceHomeworkItem(res.data)
        this.$message.success(studentVisible ? '学生现在可以查看答案' : '答案已对学生隐藏')
      } finally {
        this.$delete(this.visibilityUpdating, homework.id)
      }
    },
    async previewAnswer(homework) {
      this.answerPreviewTarget = homework
      this.answerPreviewVisible = true
      await this.loadAnswerPreview()
    },
    async loadAnswerPreview() {
      if (!this.answerPreviewTarget) return
      this.answerPreviewLoading = true
      this.answerPreviewError = ''
      this.answerPreviewType = ''
      this.answerPreviewHtml = ''
      this.revokeAnswerPreviewUrl()
      try {
        const url = await getOssDownloadUrl(this.answerPreviewTarget.answerObjectKey, true)
        const response = await fetch(url)
        if (!response.ok) throw new Error(`文件读取失败（HTTP ${response.status}）`)
        const blob = await response.blob()
        this.answerPreviewBlob = blob
        const fileName = this.answerPreviewTarget.answerFileName || ''
        if (/\.pdf$/i.test(fileName)) {
          this.answerPreviewType = 'pdf'
          this.answerPreviewObjectUrl = URL.createObjectURL(blob)
        } else if (/\.(md|markdown|txt)$/i.test(fileName)) {
          this.answerPreviewType = 'markdown'
          this.answerPreviewHtml = renderMarkdown(await blob.text())
        } else {
          this.answerPreviewType = 'unsupported'
        }
      } catch (error) {
        this.answerPreviewBlob = null
        this.answerPreviewError = error.message || '无法读取标准答案，请检查 OSS 跨域配置后重试'
      } finally {
        this.answerPreviewLoading = false
      }
    },
    downloadPreviewedAnswer() {
      if (!this.answerPreviewBlob || !this.answerPreviewTarget) return
      const objectUrl = URL.createObjectURL(this.answerPreviewBlob)
      const link = document.createElement('a')
      link.href = objectUrl
      link.download = this.answerPreviewTarget.answerFileName || 'homework-answer'
      link.style.display = 'none'
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      window.setTimeout(() => URL.revokeObjectURL(objectUrl), 1000)
      this.$message.success('已开始下载原文件')
    },
    revokeAnswerPreviewUrl() {
      if (this.answerPreviewObjectUrl) {
        URL.revokeObjectURL(this.answerPreviewObjectUrl)
        this.answerPreviewObjectUrl = ''
      }
    },
    resetAnswerPreview() {
      this.revokeAnswerPreviewUrl()
      this.answerPreviewTarget = null
      this.answerPreviewError = ''
      this.answerPreviewType = ''
      this.answerPreviewHtml = ''
      this.answerPreviewBlob = null
    },
    replaceHomeworkItem(updatedHomework) {
      const classRow = this.records.find(row => {
        return row.homeworks.some(homework => homework.id === updatedHomework.id)
      })
      if (!classRow) return
      const index = classRow.homeworks.findIndex(homework => homework.id === updatedHomework.id)
      this.$set(classRow.homeworks, index, updatedHomework)
    },
    resetAnswerDialog() {
      this.answerTarget = null
      this.answerFile = null
      this.answerUploadProgress = 0
      if (this.$refs.answerFileInput) this.$refs.answerFileInput.value = ''
    }
  }
}
</script>

<style scoped>
.status-page { padding-bottom: 26px; }
.request-state {
  display: flex;
  min-height: 330px;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
}
.request-state > i { margin-bottom: 18px; color: var(--ink-3); font-size: 46px; }
.request-state h2 { margin: 0; color: var(--ink); font-size: 20px; }
.request-state p { max-width: 520px; margin: 10px 0 22px; color: var(--ink-3); font-size: 13px; line-height: 1.7; }
.permission-state > i { color: var(--brass-ink); }
.error-state > i { color: #C46B50; }
.status-head { align-items: center; }
.head-copy { margin-top: 8px; color: var(--ink-3); font-size: 13px; }
.date-stamp {
  min-width: 205px;
  padding: 12px 16px;
  border: 1px solid var(--line);
  border-left: 3px solid var(--brass);
  border-radius: 7px;
  background: rgba(255, 255, 255, .68);
}
.date-stamp span { display: block; color: var(--ink-3); font-size: 10px; letter-spacing: .14em; }
.date-stamp b { display: block; margin-top: 5px; color: var(--ink); font: 600 16px var(--font-mono); }
.ledger-search { padding-bottom: 4px; }
.date-type-select { width: 132px; }
.ledger-card { position: relative; overflow: hidden; }
.ledger-card::before { content: ''; position: absolute; inset: 0 auto 0 0; width: 3px; background: var(--brass); }
.ledger-tally {
  display: grid;
  grid-template-columns: repeat(4, minmax(120px, 1fr));
  margin-bottom: 18px;
  border: 1px solid var(--line-soft);
  border-radius: 7px;
  background: #F8FAF7;
}
.ledger-tally > div { padding: 12px 16px; border-right: 1px solid var(--line-soft); }
.ledger-tally > div:last-child { border-right: 0; }
.ledger-tally span { color: var(--ink-3); font-size: 10px; letter-spacing: .08em; }
.ledger-tally b { display: block; margin-top: 4px; color: var(--jade-deep); font: 600 20px var(--font-mono); }
.class-cell { display: flex; flex-direction: column; gap: 5px; }
.class-cell b { color: var(--ink); font-size: 14px; }
.class-index { color: var(--brass-ink); font-size: 9px; letter-spacing: .12em; }
.publish-state { display: inline-flex; align-items: center; gap: 7px; color: var(--ink-3); font-size: 12.5px; }
.publish-state i { width: 7px; height: 7px; border-radius: 50%; background: #B7C0B8; }
.publish-state.is-published { color: var(--jade-deep); font-weight: 600; }
.publish-state.is-published i { background: var(--jade); box-shadow: 0 0 0 3px var(--jade-soft); }
.match-count { display: block; margin-top: 6px; color: var(--ink-3); font-size: 10px; }
.homework-stack { display: grid; gap: 9px; padding: 5px 0; }
.homework-record {
  display: grid;
  grid-template-columns: minmax(300px, 1fr) minmax(270px, 330px);
  gap: 16px;
  padding: 14px 15px;
  border: 1px solid var(--line-soft);
  border-radius: 8px;
  background: #FFFFFF;
}
.record-main { min-width: 0; }
.record-title { display: flex; align-items: center; gap: 9px; }
.record-title b { color: var(--ink); font-size: 13.5px; }
.record-main > p { margin-top: 7px; color: var(--ink-2); font-size: 12px; line-height: 1.55; }
.record-meta { display: flex; flex-wrap: wrap; gap: 7px 14px; margin-top: 10px; color: var(--ink-3); font-size: 10.5px; }
.record-meta span { display: inline-flex; align-items: center; gap: 4px; }
.answer-desk {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 9px 12px;
  align-items: center;
  padding: 10px 12px;
  border: 1px dashed #CBD3CB;
  border-radius: 7px;
  background: #F8FAF7;
}
.answer-desk.ready { border-style: solid; border-color: #B9D2C5; background: #F1F7F4; }
.answer-file { min-width: 0; }
.answer-file span { display: block; color: var(--ink-3); font-size: 9.5px; letter-spacing: .08em; }
.answer-file b { display: block; margin-top: 4px; overflow: hidden; color: var(--ink); font-size: 11.5px; text-overflow: ellipsis; white-space: nowrap; }
.answer-actions { display: flex; align-items: center; }
.visibility-control { grid-column: 1 / -1; display: flex; align-items: center; justify-content: space-between; padding-top: 8px; border-top: 1px solid rgba(23, 126, 99, .12); }
.visibility-control > span:first-child { color: var(--ink-2); font-size: 11px; }
.no-homework { display: flex; align-items: center; gap: 11px; min-height: 74px; color: var(--ink-3); }
.no-homework > i { font-size: 24px; color: #B5BFB6; }
.no-homework div { display: flex; flex-direction: column; gap: 4px; }
.no-homework b { color: var(--ink-2); font-size: 12.5px; }
.no-homework span { font-size: 11px; }
.answer-target { display: flex; flex-direction: column; gap: 5px; margin-bottom: 16px; padding: 12px 14px; border-left: 3px solid var(--brass); background: #FAFBF8; }
.answer-target span { color: var(--ink-3); font-size: 10px; }
.answer-target b { color: var(--ink); font-size: 13px; }
.answer-target small { color: var(--ink-3); line-height: 1.5; }
.answer-drop {
  display: grid;
  place-items: center;
  min-height: 150px;
  padding: 20px;
  border: 1px dashed #B9C7BB;
  border-radius: 8px;
  background: #FBFCFA;
  cursor: pointer;
  transition: border-color .16s ease, background-color .16s ease;
}
.answer-drop:hover,
.answer-drop:focus-within { border-color: var(--jade); background: var(--jade-soft); }
.answer-drop.has-file { border-style: solid; border-color: #A9C9BA; background: #F3F8F5; }
.answer-drop input { position: absolute; width: 1px; height: 1px; opacity: 0; }
.answer-drop i { margin-bottom: 10px; color: var(--jade-deep); font-size: 29px; }
.answer-drop b { color: var(--ink); font-size: 13px; }
.answer-drop span { margin-top: 6px; color: var(--ink-3); font-size: 11px; }
.answer-progress { margin-top: 16px; }

@media (max-width: 1100px) {
  .homework-record { grid-template-columns: 1fr; }
  .answer-desk { grid-template-columns: minmax(0, 1fr) auto; }
}

@media (max-width: 760px) {
  .status-page { padding: 14px; }
  .status-head { align-items: flex-start; }
  .date-stamp { display: none; }
  .ledger-search .el-form { display: grid; }
  .ledger-search .el-form-item { margin-right: 0; }
  .date-type-select { width: 100%; }
  .ledger-tally { grid-template-columns: 1fr 1fr; }
  .ledger-tally > div:nth-child(2) { border-right: 0; }
  .ledger-tally > div:nth-child(-n+2) { border-bottom: 1px solid var(--line-soft); }
  .homework-record { padding: 12px; }
  .record-meta { display: grid; }
  .answer-desk { grid-template-columns: 1fr; }
  .answer-actions { grid-row: 2; }
  .visibility-control { grid-column: 1; }
}
</style>

<style>
.answer-preview-dialog .el-dialog__body { padding: 0; }
.answer-preview-dialog .el-dialog { max-width: 900px; }
.answer-preview-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 11px 16px 11px 20px;
  border-top: 1px solid #E5EAE5;
  border-bottom: 1px solid #DDE5DF;
  background: #F7FAF8;
}
.answer-preview-toolbar > div { display: flex; min-width: 0; flex-direction: column; gap: 3px; }
.answer-preview-toolbar span { color: #748079; font-size: 9px; letter-spacing: .1em; }
.answer-preview-toolbar b { overflow: hidden; color: #26342D; font-size: 12px; text-overflow: ellipsis; white-space: nowrap; }
.answer-preview-toolbar .el-button { flex: none; border-color: #B8CBC0; color: #176B53; background: #FFFFFF; }
.answer-preview-state {
  display: flex;
  min-height: 500px;
  align-items: center;
  justify-content: center;
  gap: 9px;
  color: #6D7972;
  font-size: 12px;
}
.answer-preview-state > i { font-size: 24px; }
.answer-preview-state.is-error { flex-direction: column; color: #B45F45; }
.answer-preview-state.is-unsupported { flex-direction: column; }
.answer-preview-state.is-unsupported b { margin-top: 6px; color: #34423A; font-size: 14px; }
.answer-pdf-frame { display: block; width: 100%; height: 72vh; border: 0; background: #E8ECE9; }
.answer-preview-markdown {
  box-sizing: border-box;
  min-height: 500px;
  max-height: 72vh;
  overflow-y: auto;
  padding: 36px 48px 64px;
  color: #26342D;
  font: 14px/1.78 -apple-system, BlinkMacSystemFont, "Segoe UI", "Microsoft YaHei", sans-serif;
}
.answer-preview-markdown h1,
.answer-preview-markdown h2,
.answer-preview-markdown h3 { margin: 1.5em 0 .65em; color: #14251C; line-height: 1.3; }
.answer-preview-markdown h1 { margin-top: 0; padding-bottom: .4em; border-bottom: 2px solid #C6D5CC; font-size: 27px; }
.answer-preview-markdown h2 { padding-bottom: .35em; border-bottom: 1px solid #DEE6E0; font-size: 21px; }
.answer-preview-markdown h3 { font-size: 17px; }
.answer-preview-markdown p { margin: .75em 0; }
.answer-preview-markdown a { color: #116F56; }
.answer-preview-markdown code { padding: 2px 5px; border-radius: 4px; background: #EEF4F0; color: #A8462F; font: 12px Consolas, monospace; }
.answer-preview-markdown pre { overflow-x: auto; padding: 16px 18px; border-left: 3px solid #B98A2F; border-radius: 5px; background: #18251F; }
.answer-preview-markdown pre code { padding: 0; background: transparent; color: #E5ECE7; }
.answer-preview-markdown blockquote { margin: 1.2em 0; padding: 9px 16px; border-left: 3px solid #2A8469; background: #F1F7F4; color: #52625A; }
.answer-preview-markdown table { width: 100%; border-collapse: collapse; font-size: 13px; }
.answer-preview-markdown th,
.answer-preview-markdown td { padding: 8px 10px; border: 1px solid #DCE4DE; text-align: left; }
.answer-preview-markdown th { background: #F1F5F2; }
@media (max-width: 720px) {
  .answer-preview-markdown { padding: 26px 20px 44px; }
  .answer-preview-toolbar { align-items: flex-start; }
}
</style>
