<template>
  <div class="homework-page page-container">
    <header class="page-head homework-head">
      <div>
        <p class="eyebrow">ACADEMIC · HOMEWORK</p>
        <h1>作业发布台</h1>
        <p class="head-copy">从当天课表生成发布草稿，再附上 Markdown 作业文件。</p>
      </div>
      <div class="head-status" :class="{ ready: prefillReady }">
        <span class="status-dot"></span>
        <div>
          <b>{{ prefillReady ? '草稿已就绪' : '等待选择课表' }}</b>
          <small>{{ prefillReady ? '可检查内容并发布' : '先选择班级与作业日期' }}</small>
        </div>
      </div>
    </header>

    <section class="publish-console">
      <aside class="source-panel">
        <div class="panel-kicker">
          <span>01</span>
          <div>
            <b>读取当天课表</b>
            <small>系统会检查班级、重复作业和上课安排</small>
          </div>
        </div>

        <el-form label-position="top" class="source-form" @submit.native.prevent>
          <el-form-item label="发布班级" required>
            <el-select
              v-model="query.classId"
              class="full-control"
              filterable
              :loading="classLoading"
              placeholder="选择一个班级"
            >
              <el-option
                v-for="item in classOptions"
                :key="item.id"
                :label="item.className"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="作业日期" required>
            <el-date-picker
              v-model="query.homeworkDate"
              class="full-control"
              type="date"
              value-format="yyyy-MM-dd"
              format="yyyy 年 MM 月 dd 日"
              placeholder="选择课表日期"
              :clearable="false"
            />
          </el-form-item>
          <el-button
            class="prefill-button"
            type="primary"
            icon="el-icon-magic-stick"
            :loading="prefillLoading"
            :disabled="!query.classId || !query.homeworkDate"
            @click="handlePrefill"
          >
            生成发布草稿
          </el-button>
        </el-form>

        <div class="rule-notes">
          <p><i class="el-icon-circle-check"></i> 同一班级同一天只能发布一次</p>
          <p><i class="el-icon-circle-check"></i> 仅上课日可生成作业草稿</p>
          <p><i class="el-icon-circle-check"></i> 课程内容始终以后端课表为准</p>
        </div>
      </aside>

      <main class="draft-sheet" :class="{ 'is-ready': prefillReady }">
        <div v-if="!prefillReady" class="draft-empty">
          <div class="empty-glyph" aria-hidden="true">
            <span></span><span></span><span></span>
          </div>
          <p class="empty-code">NO SCHEDULE DRAFT</p>
          <h2>选择班级和日期</h2>
          <p>成功读取当天课表后，这里会出现可编辑的作业发布单。</p>
        </div>

        <template v-else>
          <div class="sheet-meta">
            <div>
              <span>发布对象</span>
              <b>{{ prefillInfo.className }}</b>
            </div>
            <div>
              <span>作业日期</span>
              <b class="mono">{{ prefillInfo.homeworkDate }}</b>
            </div>
            <el-tag type="success" size="small">课表已核验</el-tag>
          </div>

          <div class="panel-kicker sheet-kicker">
            <span>02</span>
            <div>
              <b>确认作业内容</b>
              <small>默认值来自课表，标题与发布时间可按需调整</small>
            </div>
          </div>

          <el-form
            ref="publishForm"
            :model="draft"
            :rules="rules"
            label-position="top"
            class="publish-form"
          >
            <el-form-item label="作业标题" prop="title">
              <el-input
                v-model="draft.title"
                maxlength="100"
                show-word-limit
                placeholder="输入学生看到的作业标题"
              />
            </el-form-item>

            <div class="course-strip">
              <span>当天课程</span>
              <strong>{{ draft.classContent }}</strong>
              <small>发布时后端会再次读取课表，不使用前端传值</small>
            </div>

            <div class="time-grid">
              <el-form-item label="开始时间" prop="startTime">
                <el-date-picker
                  v-model="draft.startTime"
                  class="full-control"
                  type="datetime"
                  value-format="yyyy-MM-dd HH:mm:ss"
                  format="yyyy-MM-dd HH:mm"
                  placeholder="选择开始时间"
                />
              </el-form-item>
              <el-form-item label="截止时间" prop="deadline">
                <el-date-picker
                  v-model="draft.deadline"
                  class="full-control"
                  type="datetime"
                  value-format="yyyy-MM-dd HH:mm:ss"
                  format="yyyy-MM-dd HH:mm"
                  placeholder="选择截止时间"
                />
              </el-form-item>
            </div>

            <el-form-item label="作业文件" required>
              <label
                class="file-drop"
                :class="{ 'has-file': selectedFile, uploading: publishing }"
                @dragover.prevent
                @drop.prevent="handleFileDrop"
              >
                <input
                  ref="fileInput"
                  type="file"
                  accept=".md,.markdown,text/markdown,text/plain"
                  :disabled="publishing"
                  @change="handleFileInput"
                >
                <template v-if="!selectedFile">
                  <i class="el-icon-document-add"></i>
                  <b>选择或拖入 Markdown 文件</b>
                  <span>支持 .md / .markdown，文件不超过 10 MB</span>
                </template>
                <template v-else>
                  <i class="el-icon-document-checked"></i>
                  <b>{{ selectedFile.name }}</b>
                  <span>{{ formatFileSize(selectedFile.size) }} · 点击可替换文件</span>
                </template>
              </label>
              <div v-if="selectedFile" class="file-actions">
                <span class="file-state">
                  <i :class="uploadedObjectKey ? 'el-icon-circle-check' : 'el-icon-time'"></i>
                  {{ uploadedObjectKey ? '已上传至 OSS' : '将在发布时上传至 OSS' }}
                </span>
                <el-button type="text" :disabled="publishing" @click="clearFile">移除</el-button>
              </div>
              <el-progress
                v-if="publishing && uploadProgress > 0"
                :percentage="uploadProgress"
                :stroke-width="5"
                :show-text="false"
                class="upload-progress"
              />
            </el-form-item>

            <el-form-item label="备注" prop="remark">
              <el-input
                v-model="draft.remark"
                type="textarea"
                :rows="3"
                maxlength="500"
                show-word-limit
                placeholder="可填写提交要求、注意事项等补充说明"
              />
            </el-form-item>
          </el-form>

          <footer class="publish-footer">
            <div class="publish-sequence">
              <span :class="{ active: publishingStage >= 1 }">校验草稿</span>
              <i></i>
              <span :class="{ active: publishingStage >= 2 }">上传文件</span>
              <i></i>
              <span :class="{ active: publishingStage >= 3 }">写入作业</span>
            </div>
            <el-button
              type="primary"
              icon="el-icon-s-promotion"
              :loading="publishing"
              @click="handlePublish"
            >
              {{ publishingText }}
            </el-button>
          </footer>
        </template>
      </main>
    </section>
  </div>
</template>

<script>
import {
  listHomeworkClassOptions,
  prefillHomework,
  publishHomework
} from '@/api/homework'
import { uploadToOssWithProgress } from '@/utils/ossUpload'

const MAX_FILE_SIZE = 10 * 1024 * 1024

const createDraft = () => ({
  title: '',
  classContent: '',
  startTime: '',
  deadline: '',
  remark: ''
})

function formatToday() {
  const date = new Date()
  const pad = value => String(value).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

export default {
  name: 'HomeworkPublish',
  data() {
    const validateDeadline = (rule, value, callback) => {
      if (!value || !this.draft.startTime) return callback()
      const startTime = new Date(this.draft.startTime.replace(' ', 'T')).getTime()
      const deadline = new Date(value.replace(' ', 'T')).getTime()
      if (deadline < startTime) return callback(new Error('截止时间不能早于开始时间'))
      callback()
    }

    return {
      classLoading: false,
      prefillLoading: false,
      publishing: false,
      publishingStage: 0,
      uploadProgress: 0,
      classOptions: [],
      query: {
        classId: null,
        homeworkDate: formatToday()
      },
      prefillInfo: {},
      prefillReady: false,
      draft: createDraft(),
      selectedFile: null,
      uploadedObjectKey: '',
      rules: {
        title: [
          { required: true, message: '请输入作业标题', trigger: 'blur' },
          { max: 100, message: '作业标题不能超过100个字符', trigger: 'blur' }
        ],
        startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
        deadline: [
          { required: true, message: '请选择截止时间', trigger: 'change' },
          { validator: validateDeadline, trigger: 'change' }
        ],
        remark: [{ max: 500, message: '备注不能超过500个字符', trigger: 'blur' }]
      }
    }
  },
  computed: {
    publishingText() {
      if (!this.publishing) return '发布作业'
      if (this.publishingStage === 2) return `正在上传 ${this.uploadProgress}%`
      if (this.publishingStage === 3) return '正在发布'
      return '正在校验'
    }
  },
  watch: {
    'query.classId'() {
      this.invalidateDraft()
    },
    'query.homeworkDate'() {
      this.invalidateDraft()
    }
  },
  created() {
    this.loadClassOptions()
  },
  methods: {
    async loadClassOptions() {
      this.classLoading = true
      try {
        const res = await listHomeworkClassOptions()
        this.classOptions = res.data || []
      } finally {
        this.classLoading = false
      }
    },
    async handlePrefill() {
      if (!this.query.classId || !this.query.homeworkDate) {
        this.$message.warning('请先选择班级和作业日期')
        return
      }
      this.prefillLoading = true
      try {
        const res = await prefillHomework(this.query)
        const data = res.data
        this.prefillInfo = data
        this.draft = {
          title: data.title,
          classContent: data.classContent,
          startTime: this.normalizeDateTime(data.startTime),
          deadline: this.normalizeDateTime(data.deadline),
          remark: ''
        }
        this.prefillReady = true
        this.clearFile()
        this.$nextTick(() => this.$refs.publishForm && this.$refs.publishForm.clearValidate())
      } finally {
        this.prefillLoading = false
      }
    },
    invalidateDraft() {
      if (this.prefillLoading) return
      this.prefillReady = false
      this.prefillInfo = {}
      this.draft = createDraft()
      this.clearFile()
    },
    normalizeDateTime(value) {
      return value ? value.replace('T', ' ') : ''
    },
    handleFileInput(event) {
      const file = event.target.files && event.target.files[0]
      this.selectFile(file)
    },
    handleFileDrop(event) {
      if (this.publishing) return
      const file = event.dataTransfer.files && event.dataTransfer.files[0]
      this.selectFile(file)
    },
    selectFile(file) {
      if (!file) return
      if (!/\.(md|markdown)$/i.test(file.name)) {
        this.$message.error('请选择 .md 或 .markdown 文件')
        this.clearFile()
        return
      }
      if (file.size > MAX_FILE_SIZE) {
        this.$message.error('Markdown 文件不能超过 10 MB')
        this.clearFile()
        return
      }
      this.selectedFile = file
      this.uploadedObjectKey = ''
      this.uploadProgress = 0
    },
    clearFile() {
      this.selectedFile = null
      this.uploadedObjectKey = ''
      this.uploadProgress = 0
      if (this.$refs.fileInput) this.$refs.fileInput.value = ''
    },
    formatFileSize(size) {
      if (size < 1024) return `${size} B`
      if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
      return `${(size / 1024 / 1024).toFixed(1)} MB`
    },
    validateForm() {
      return new Promise(resolve => {
        this.$refs.publishForm.validate(valid => resolve(valid))
      })
    },
    async handlePublish() {
      if (!this.prefillReady) {
        this.$message.warning('请先生成发布草稿')
        return
      }
      const valid = await this.validateForm()
      if (!valid) return
      if (!this.selectedFile) {
        this.$message.warning('请选择要发布的 Markdown 作业文件')
        return
      }

      this.publishing = true
      this.publishingStage = 1
      try {
        if (!this.uploadedObjectKey) {
          this.publishingStage = 2
          const uploadResult = await uploadToOssWithProgress(
            this.selectedFile,
            'homework',
            percent => { this.uploadProgress = percent }
          )
          this.uploadedObjectKey = uploadResult.objectKey
          this.uploadProgress = 100
        }

        this.publishingStage = 3
        await publishHomework({
          title: this.draft.title.trim(),
          contentObjectKey: this.uploadedObjectKey,
          contentFileName: this.selectedFile.name,
          classId: this.prefillInfo.classId,
          homeworkDate: this.prefillInfo.homeworkDate,
          startTime: this.draft.startTime,
          deadline: this.draft.deadline,
          remark: this.draft.remark ? this.draft.remark.trim() : null
        })
        this.$message.success('作业发布成功')
        this.resetWorkflow()
      } finally {
        this.publishing = false
        this.publishingStage = 0
      }
    },
    resetWorkflow() {
      this.query.classId = null
      this.query.homeworkDate = formatToday()
      this.prefillReady = false
      this.prefillInfo = {}
      this.draft = createDraft()
      this.clearFile()
    }
  }
}
</script>

<style scoped>
.homework-page { padding-bottom: 26px; }
.homework-head { align-items: center; }
.head-copy { margin-top: 8px; color: var(--ink-3); font-size: 13px; }
.head-status {
  display: flex;
  align-items: center;
  gap: 11px;
  min-width: 218px;
  padding: 12px 16px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: rgba(255, 255, 255, .58);
}
.status-dot { width: 10px; height: 10px; border-radius: 50%; background: #AEB8AF; box-shadow: 0 0 0 5px #E8ECE6; }
.head-status.ready .status-dot { background: var(--jade); box-shadow: 0 0 0 5px var(--jade-soft); }
.head-status div { display: flex; flex-direction: column; gap: 3px; }
.head-status b { color: var(--ink); font-size: 13px; }
.head-status small { color: var(--ink-3); font-size: 11px; }

.publish-console {
  display: grid;
  grid-template-columns: minmax(260px, 320px) minmax(0, 1fr);
  gap: 18px;
  align-items: start;
}
.source-panel,
.draft-sheet {
  border: 1px solid var(--line-soft);
  border-radius: 10px;
  background: var(--card);
  box-shadow: var(--shadow-card);
}
.source-panel { position: sticky; top: 16px; padding: 22px; }
.panel-kicker { display: flex; align-items: center; gap: 13px; }
.panel-kicker > span {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  flex: none;
  border: 1px solid #D7C49A;
  border-radius: 50%;
  color: var(--brass-ink);
  background: var(--brass-soft);
  font: 700 12px var(--font-mono);
}
.panel-kicker div { display: flex; flex-direction: column; gap: 3px; }
.panel-kicker b { color: var(--ink); font-size: 14px; }
.panel-kicker small { color: var(--ink-3); font-size: 11px; line-height: 1.5; }
.source-form { margin-top: 24px; }
.full-control { width: 100%; }
.prefill-button { width: 100%; margin-top: 3px; }
.rule-notes { display: grid; gap: 10px; margin-top: 24px; padding-top: 18px; border-top: 1px dashed var(--line); }
.rule-notes p { display: flex; align-items: flex-start; gap: 8px; color: var(--ink-3); font-size: 11.5px; line-height: 1.5; }
.rule-notes i { margin-top: 2px; color: var(--jade); }

.draft-sheet {
  position: relative;
  min-height: 600px;
  padding: 28px 34px 24px 42px;
  overflow: hidden;
}
.draft-sheet::before {
  content: '';
  position: absolute;
  inset: 0 auto 0 16px;
  width: 1px;
  background: repeating-linear-gradient(to bottom, var(--line) 0 7px, transparent 7px 15px);
}
.draft-empty { min-height: 542px; display: grid; place-content: center; justify-items: center; text-align: center; }
.empty-glyph { display: grid; gap: 7px; width: 88px; padding: 18px; border: 1px solid var(--line); border-radius: 8px; transform: rotate(-2deg); }
.empty-glyph span { height: 4px; border-radius: 2px; background: var(--line); }
.empty-glyph span:nth-child(2) { width: 72%; }
.empty-code { margin-top: 24px; color: var(--brass-ink); font: 600 10px var(--font-mono); letter-spacing: .2em; }
.draft-empty h2 { margin-top: 8px; color: var(--ink); font: 800 23px var(--font-display); }
.draft-empty > p:last-child { max-width: 360px; margin-top: 9px; color: var(--ink-3); font-size: 13px; line-height: 1.7; }
.sheet-meta { display: grid; grid-template-columns: 1fr 1fr auto; align-items: center; gap: 18px; padding-bottom: 18px; border-bottom: 1px solid var(--line); }
.sheet-meta > div { display: flex; flex-direction: column; gap: 5px; }
.sheet-meta span { color: var(--ink-3); font-size: 10px; letter-spacing: .08em; }
.sheet-meta b { color: var(--ink); font-size: 14px; }
.sheet-kicker { margin: 24px 0 20px; }
.publish-form { max-width: 920px; }
.course-strip { margin: 2px 0 22px; padding: 15px 17px; border-left: 3px solid var(--jade); background: #F4F8F4; }
.course-strip span,
.course-strip small { display: block; color: var(--ink-3); font-size: 11px; }
.course-strip strong { display: block; margin: 6px 0; color: var(--ink); font-size: 15px; line-height: 1.55; }
.time-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.file-drop {
  display: grid;
  place-items: center;
  min-height: 132px;
  padding: 18px;
  border: 1px dashed #B9C7BB;
  border-radius: 8px;
  background: #FBFCFA;
  cursor: pointer;
  transition: border-color .16s ease, background-color .16s ease, transform .16s ease;
}
.file-drop:hover,
.file-drop:focus-within { border-color: var(--jade); background: var(--jade-soft); transform: translateY(-1px); }
.file-drop.has-file { border-style: solid; border-color: #A9C9BA; background: #F3F8F5; }
.file-drop.uploading { pointer-events: none; }
.file-drop input { position: absolute; width: 1px; height: 1px; opacity: 0; }
.file-drop i { margin-bottom: 10px; color: var(--jade-deep); font-size: 28px; }
.file-drop b { color: var(--ink); font-size: 13px; }
.file-drop span { margin-top: 6px; color: var(--ink-3); font-size: 11.5px; }
.file-actions { display: flex; align-items: center; justify-content: space-between; min-height: 30px; }
.file-state { color: var(--ink-3); font-size: 11.5px; }
.file-state i { margin-right: 5px; color: var(--jade); }
.upload-progress { margin-top: -3px; }
.publish-footer { display: flex; align-items: center; justify-content: space-between; gap: 20px; padding-top: 18px; border-top: 1px solid var(--line); }
.publish-sequence { display: flex; align-items: center; gap: 8px; color: #A1AAA3; font: 600 10px var(--font-mono); letter-spacing: .05em; }
.publish-sequence span.active { color: var(--jade-deep); }
.publish-sequence i { width: 22px; height: 1px; background: var(--line); }

@media (max-width: 980px) {
  .publish-console { grid-template-columns: 1fr; }
  .source-panel { position: static; }
  .source-form { display: grid; grid-template-columns: 1fr 1fr auto; gap: 14px; align-items: end; }
  .source-form .el-form-item { margin-bottom: 0; }
  .prefill-button { width: auto; margin-bottom: 0; }
  .rule-notes { grid-template-columns: repeat(3, 1fr); }
}

@media (max-width: 700px) {
  .homework-page { padding: 14px; }
  .homework-head { align-items: flex-start; }
  .head-status { display: none; }
  .source-form { grid-template-columns: 1fr; }
  .source-form .el-form-item { margin-bottom: 14px; }
  .prefill-button { width: 100%; }
  .rule-notes { grid-template-columns: 1fr; }
  .draft-sheet { min-height: 520px; padding: 24px 20px 20px 30px; }
  .draft-sheet::before { left: 11px; }
  .sheet-meta { grid-template-columns: 1fr 1fr; }
  .sheet-meta .el-tag { display: none; }
  .time-grid { grid-template-columns: 1fr; gap: 0; }
  .publish-footer { align-items: stretch; flex-direction: column; }
  .publish-sequence { justify-content: center; }
}
</style>
