<template>
  <div class="student-page">
    <student-header :profile="profile" :logging-out="loggingOut" :homework-count="pendingCount" :order-count="headerOrderCount" @logout="handleLogout" />
    <main class="page-shell">
      <header class="page-heading">
        <div><p class="page-kicker">ASSIGNMENT DESK</p><h1>我的作业</h1><p>查看任务要求与截止时间，提交文件后可随时回来确认批改结果。</p></div>
        <div class="task-summary"><span><b>{{ pendingCount }}</b> 待完成</span><i></i><span><b>{{ submittedCount }}</b> 已提交</span></div>
      </header>

      <section class="homework-layout">
        <div class="homework-main">
          <div class="toolbar">
            <div class="filter-tabs" role="tablist" aria-label="筛选作业">
              <button v-for="item in filters" :key="item.value" type="button" :class="{ active: filter === item.value }" @click="filter = item.value">{{ item.label }}<span>{{ countFor(item.value) }}</span></button>
            </div>
            <label class="search-box"><svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="11" cy="11" r="7"/><path d="m16 16 5 5"/></svg><input v-model.trim="keyword" type="search" placeholder="搜索作业或课程"></label>
          </div>

          <div v-if="loading" class="homework-state"><span class="loading-ring"></span>正在加载作业…</div>
          <div v-else-if="errorMessage" class="error-state"><strong>作业加载失败</strong><span>{{ errorMessage }}</span><button class="retry-button" type="button" @click="loadHomeworks">重新加载</button></div>
          <div v-else-if="!filteredHomeworks.length" class="empty-state"><strong>这里暂时没有作业</strong><span>切换筛选条件，或稍后等待老师发布新任务。</span></div>
          <ol v-else class="homework-list">
            <li v-for="item in filteredHomeworks" :key="item.id" :class="{ selected: selected && selected.id === item.id }">
              <button class="task-card" type="button" @click="selectHomework(item)">
                <span :class="['task-status', statusOf(item).tone]">{{ statusOf(item).text }}</span>
                <div class="task-title"><small>{{ courseOf(item) }}</small><h2>{{ item.title || '未命名作业' }}</h2></div>
                <div class="task-meta"><span>截止 {{ formatDeadline(item.deadline || item.dueAt) }}</span><span v-if="item.contentFileName || item.fileName">附件 1</span></div>
                <span class="task-arrow">→</span>
              </button>
            </li>
          </ol>
        </div>

        <aside class="detail-panel" :class="{ empty: !selected }">
          <template v-if="selected">
            <div class="detail-heading"><div><p class="page-kicker">TASK DETAIL</p><h2>{{ selected.title || '未命名作业' }}</h2></div><span :class="['task-status', statusOf(selected).tone]">{{ statusOf(selected).text }}</span></div>
            <p class="course-name">{{ courseOf(selected) }}</p>
            <dl class="detail-meta">
              <div><dt>开始时间</dt><dd>{{ formatDateTime(selected.startTime || selected.createdAt) }}</dd></div>
              <div><dt>截止时间</dt><dd>{{ formatDateTime(selected.deadline || selected.dueAt) }}</dd></div>
              <div><dt>作业分值</dt><dd>{{ selected.totalScore || selected.scoreTotal || 100 }} 分</dd></div>
            </dl>
            <section class="requirement"><h3>任务要求</h3><p>{{ selected.remark || selected.description || selected.classContent || '请按照老师发布的作业附件完成内容，并在截止时间前提交。' }}</p></section>
            <button v-if="selected.contentFileName || selected.fileName" class="attachment" type="button" :disabled="previewLoading" @click="previewHomeworkFile(selected)">
              <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M8 12.5 13.5 7a3 3 0 0 1 4.2 4.2l-7.3 7.3a5 5 0 0 1-7.1-7.1l7.4-7.4"/></svg>
              <span><strong>{{ selected.contentFileName || selected.fileName }}</strong><small>{{ previewLoading ? '正在加载预览…' : '查看老师发布的附件' }}</small></span>
            </button>

            <section v-if="isSubmitted(selected)" class="submission-record">
              <div><span class="check">✓</span><div><strong>已提交</strong><small>{{ formatDateTime(submitTime(selected)) }}</small></div></div>
              <p v-if="submissionFileName(selected)">文件：{{ submissionFileName(selected) }}</p>
              <p v-if="scoreOf(selected) !== null">成绩：<b>{{ scoreOf(selected) }}</b> / {{ selected.totalScore || 100 }}</p>
              <p v-if="teacherRemark(selected)">教师评语：{{ teacherRemark(selected) }}</p>
            </section>
            <form v-else class="submit-box" @submit.prevent="submitHomework">
              <h3>提交作业</h3>
              <label class="file-drop" :class="{ chosen: selectedFile }">
                <input ref="fileInput" type="file" @change="chooseFile">
                <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 16V4m0 0L8 8m4-4 4 4M5 14v6h14v-6"/></svg>
                <span><strong>{{ selectedFile ? selectedFile.name : '选择作业文件' }}</strong><small>{{ selectedFile ? formatFileSize(selectedFile.size) : '单个文件不超过 50MB' }}</small></span>
              </label>
              <p v-if="submitError" class="submit-error" role="alert">{{ submitError }}</p>
              <button class="submit-button" type="submit" :disabled="!selectedFile || submitting">{{ submitting ? '正在提交…' : '确认提交作业' }}</button>
            </form>
          </template>
          <div v-else class="detail-empty"><span>↗</span><strong>选择一项作业</strong><p>在左侧选择任务后，这里会显示要求、附件和提交入口。</p></div>
        </aside>
      </section>
    </main>
    <div v-if="previewVisible" class="file-viewer-backdrop" role="presentation" @click.self="closeHomeworkPreview">
      <section class="file-viewer" role="dialog" aria-modal="true" :aria-label="previewFileName">
        <header class="file-viewer-header">
          <div><p>ASSIGNMENT ATTACHMENT</p><h2>{{ previewFileName }}</h2></div>
          <button class="viewer-close" type="button" aria-label="关闭预览" @click="closeHomeworkPreview">×</button>
        </header>
        <div v-if="previewLoading" class="viewer-state"><span class="loading-ring"></span>正在加载预览</div>
        <div v-else-if="previewError" class="viewer-state error"><strong>无法加载预览</strong><span>{{ previewError }}</span></div>
        <article v-else class="markdown-viewer" v-html="previewHtml"></article>
        <footer class="file-viewer-footer">
          <span>原文件可随时下载留存</span>
          <button class="viewer-download" type="button" :disabled="previewDownloading" @click="downloadHomeworkFile">{{ previewDownloading ? '正在获取下载链接...' : '下载原文件' }}</button>
        </footer>
      </section>
    </div>
  </div>
</template>

<script>
import StudentHeader from '@/components/StudentHeader.vue'
import studentPage from '@/mixins/studentPage'
import { getHomeworkFileUrl, getStudentHomeworks, submitStudentHomework, uploadStudentHomeworkFile } from '@/api/learning'
import { renderMarkdown } from '@/utils/markdown'

export default {
  name: 'StudentHomework', components: { StudentHeader }, mixins: [studentPage],
  data() { return { homeworks: [], loading: true, errorMessage: '', filter: 'all', keyword: '', selected: null, selectedFile: null, submitting: false, submitError: '', previewVisible: false, previewLoading: false, previewDownloading: false, previewFileName: '', previewHtml: '', previewError: '', previewHomeworkId: null, filters: [{ value: 'all', label: '全部' }, { value: 'pending', label: '待完成' }, { value: 'submitted', label: '已提交' }, { value: 'overdue', label: '已逾期' }] } },
  computed: {
    pendingCount() { return this.homeworks.filter(item => !this.isSubmitted(item) && !this.isOverdue(item)).length },
    submittedCount() { return this.homeworks.filter(this.isSubmitted).length },
    filteredHomeworks() {
      const keyword = this.keyword.toLowerCase()
      return this.homeworks.filter(item => {
        const matchesFilter = this.filter === 'all' || (this.filter === 'pending' && !this.isSubmitted(item) && !this.isOverdue(item)) || (this.filter === 'submitted' && this.isSubmitted(item)) || (this.filter === 'overdue' && !this.isSubmitted(item) && this.isOverdue(item))
        return matchesFilter && (!keyword || `${item.title || ''} ${this.courseOf(item)}`.toLowerCase().includes(keyword))
      })
    }
  },
  mounted() { this.loadHomeworks() },
  methods: {
    async loadHomeworks() {
      this.loading = true; this.errorMessage = ''
      try { const response = await getStudentHomeworks(); const data = response.data; this.homeworks = Array.isArray(data) ? data : (data && (data.homeworks || data.records || data.list)) || []; this.selected = this.selected ? this.homeworks.find(item => item.id === this.selected.id) : this.homeworks[0] || null; this.headerHomeworkCount = this.pendingCount }
      catch (error) { this.homeworks = []; this.errorMessage = error.message || '暂时无法获取作业列表' }
      finally { this.loading = false }
    },
    countFor(filter) { if (filter === 'all') return this.homeworks.length; if (filter === 'pending') return this.pendingCount; if (filter === 'submitted') return this.submittedCount; return this.homeworks.filter(item => !this.isSubmitted(item) && this.isOverdue(item)).length },
    selectHomework(item) { this.selected = item; this.selectedFile = null; this.submitError = '' },
    courseOf(item) { return item.courseName || item.course || item.classContent || '课程作业' },
    isSubmitted(item) { return Boolean(item.submission || item.submissionId || item.submitTime || item.submitted) },
    isOverdue(item) { const value = item.deadline || item.dueAt; return value ? new Date(value).getTime() < Date.now() : false },
    statusOf(item) { if (this.isSubmitted(item)) return { text: this.scoreOf(item) !== null ? '已批改' : '已提交', tone: 'done' }; if (this.isOverdue(item)) return { text: '已逾期', tone: 'late' }; return { text: '待完成', tone: 'pending' } },
    scoreOf(item) { const value = item.score !== undefined ? item.score : item.submission && item.submission.score; return value === undefined || value === null ? null : value },
    submitTime(item) { return item.submitTime || (item.submission && (item.submission.submitTime || item.submission.submittedAt)) },
    submissionFileName(item) { return item.submissionFileName || (item.submission && (item.submission.contentFileName || item.submission.fileName)) },
    teacherRemark(item) { return item.teacherRemark || (item.submission && item.submission.teacherRemark) },
    formatDeadline(value) { if (!value) return '待通知'; const date = new Date(value); return Number.isNaN(date.getTime()) ? String(value) : new Intl.DateTimeFormat('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', hour12: false }).format(date) },
    formatDateTime(value) { if (!value) return '待通知'; const date = new Date(value); return Number.isNaN(date.getTime()) ? String(value) : new Intl.DateTimeFormat('zh-CN', { dateStyle: 'medium', timeStyle: 'short', hour12: false }).format(date) },
    chooseFile(event) { const file = event.target.files[0]; this.submitError = ''; if (file && file.size > 50 * 1024 * 1024) { this.selectedFile = null; this.submitError = '文件不能超过 50MB'; event.target.value = ''; return } this.selectedFile = file || null },
    formatFileSize(size) { return size >= 1024 * 1024 ? `${(size / 1024 / 1024).toFixed(1)} MB` : `${Math.max(1, Math.round(size / 1024))} KB` },
    async submitHomework() { if (!this.selectedFile || !this.selected) return; this.submitting = true; this.submitError = ''; try { const submission = await uploadStudentHomeworkFile(this.selected.id, this.selectedFile); await submitStudentHomework(this.selected.id, submission); await this.loadHomeworks() } catch (error) { this.submitError = error.message || '作业提交失败，请稍后重试' } finally { this.submitting = false } },
    async previewHomeworkFile(item) { this.previewVisible = true; this.previewLoading = true; this.previewDownloading = false; this.previewFileName = item.contentFileName || item.fileName || '作业附件'; this.previewHomeworkId = item.id; this.previewHtml = ''; this.previewError = ''; try { const response = await getHomeworkFileUrl(item.id, true); const url = response.data && response.data.downloadUrl; if (!url) throw new Error('暂时无法获取附件预签名地址'); const fileResponse = await fetch(url); if (!fileResponse.ok) throw new Error(`附件加载失败: ${fileResponse.status}`); this.previewHtml = renderMarkdown(await fileResponse.text()) } catch (error) { this.previewError = error.message || '附件暂时无法预览' } finally { this.previewLoading = false } },
    closeHomeworkPreview() { this.previewVisible = false; this.previewLoading = false; this.previewDownloading = false; this.previewHtml = ''; this.previewError = ''; this.previewHomeworkId = null },
    async downloadHomeworkFile() { if (!this.previewHomeworkId) return; this.previewDownloading = true; try { const response = await getHomeworkFileUrl(this.previewHomeworkId, false); const url = response.data && response.data.downloadUrl; if (!url) throw new Error('暂时无法获取下载地址'); window.open(url, '_blank', 'noopener,noreferrer') } catch (error) { this.previewError = error.message || '下载失败' } finally { this.previewDownloading = false } }
  }
}
</script>

<style scoped>
.task-summary { display: flex; align-items: center; gap: 15px; padding: 14px 18px; border-radius: 14px; background: #fff; box-shadow: 0 12px 30px -24px rgba(7,86,107,.8); color: var(--ink-soft); font-size: 11px; }.task-summary b { margin-right: 4px; color: var(--lagoon); font: 800 20px var(--display); }.task-summary i { width: 1px; height: 24px; background: var(--line); }
.homework-layout { display: grid; grid-template-columns: minmax(0,1.15fr) minmax(340px,.85fr); gap: 20px; align-items: start; }.homework-main,.detail-panel { border: 1px solid var(--line); background: rgba(255,255,255,.92); box-shadow: var(--shadow); }.homework-main { overflow: hidden; border-radius: 8px 24px 24px 24px; }.detail-panel { min-height: 560px; position: sticky; top: 98px; padding: 28px; border-radius: 24px 8px 24px 24px; }
.toolbar { display: flex; align-items: center; justify-content: space-between; gap: 15px; padding: 16px 18px; border-bottom: 1px solid var(--line); }.filter-tabs { display: flex; gap: 4px; }.filter-tabs button { padding: 8px 10px; border: 0; border-radius: 8px; color: var(--ink-soft); background: transparent; font-size: 10px; }.filter-tabs button.active { color: var(--lagoon-deep); background: var(--mint); font-weight: 700; }.filter-tabs span { margin-left: 5px; font: 8px var(--mono); }.search-box { width: 180px; height: 34px; display: flex; align-items: center; gap: 7px; padding: 0 10px; border: 1px solid var(--line); border-radius: 9px; }.search-box svg { width: 15px; fill: none; stroke: var(--ink-soft); stroke-width: 1.8; }.search-box input { min-width: 0; width: 100%; border: 0; outline: 0; background: transparent; font-size: 10px; }
.homework-state { min-height: 420px; display: flex; align-items: center; justify-content: center; gap: 10px; color: var(--ink-soft); font-size: 12px; }.loading-ring { width: 17px; height: 17px; border: 2px solid rgba(14,116,144,.18); border-top-color: var(--lagoon); border-radius: 50%; animation: spin .7s linear infinite; }@keyframes spin { to { transform: rotate(360deg); } }.homework-list { list-style: none; }.homework-list li { border-bottom: 1px solid #e8eff3; }.homework-list li:last-child { border-bottom: 0; }.homework-list li.selected { background: rgba(233,244,251,.7); box-shadow: inset 3px 0 var(--lagoon); }.task-card { width: 100%; min-height: 105px; display: grid; grid-template-columns: 65px minmax(0,1fr) auto 20px; align-items: center; gap: 14px; padding: 17px 20px; border: 0; color: var(--ink); background: transparent; text-align: left; }.task-status { justify-self: start; padding: 6px 8px; border-radius: 999px; font-size: 9px; font-weight: 700; }.task-status.pending { color: #98612d; background: #fff2d9; }.task-status.done { color: #267c63; background: #e2f5eb; }.task-status.late { color: #a44634; background: #fff0ec; }.task-title { min-width: 0; }.task-title small { color: var(--lagoon); font: 650 8px var(--mono); }.task-title h2 { margin-top: 6px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font: 700 13px var(--display); }.task-meta { display: flex; flex-direction: column; gap: 5px; color: var(--ink-soft); font-size: 9px; text-align: right; }.task-arrow { color: var(--lagoon); font-size: 17px; }
.detail-heading { display: flex; align-items: flex-start; justify-content: space-between; gap: 14px; }.detail-heading h2 { margin-top: 9px; font: 750 20px/1.4 var(--display); }.course-name { margin-top: 7px; color: var(--lagoon); font-size: 10px; }.detail-meta { display: grid; grid-template-columns: repeat(3,1fr); gap: 1px; margin-top: 20px; overflow: hidden; border: 1px solid var(--line); border-radius: 11px; background: var(--line); }.detail-meta div { min-width: 0; padding: 10px; background: #fff; }.detail-meta dt { color: var(--ink-soft); font-size: 8px; }.detail-meta dd { margin-top: 5px; font-size: 9px; font-weight: 650; }.requirement { margin-top: 22px; }.requirement h3,.submit-box h3 { font: 700 12px var(--display); }.requirement p { margin-top: 9px; color: var(--ink-soft); font-size: 11px; line-height: 1.8; }.attachment { width: 100%; display: flex; align-items: center; gap: 11px; margin-top: 16px; padding: 12px; border: 1px solid var(--line); border-radius: 11px; color: var(--ink); background: var(--sky); text-align: left; }.attachment svg { width: 23px; fill: none; stroke: var(--lagoon); stroke-width: 1.7; stroke-linecap: round; }.attachment span { display: flex; flex-direction: column; min-width: 0; }.attachment strong { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 10px; }.attachment small { margin-top: 3px; color: var(--ink-soft); font-size: 8px; }
.submit-box { margin-top: 22px; padding-top: 20px; border-top: 1px solid var(--line); }.file-drop { min-height: 74px; display: flex; align-items: center; gap: 12px; margin-top: 10px; padding: 13px; border: 1px dashed rgba(14,116,144,.35); border-radius: 11px; background: #f9fcfd; cursor: pointer; }.file-drop.chosen { border-style: solid; background: var(--mint); }.file-drop input { position: absolute; width: 1px; height: 1px; opacity: 0; }.file-drop svg { width: 25px; fill: none; stroke: var(--lagoon); stroke-width: 1.7; }.file-drop span { display: flex; flex-direction: column; min-width: 0; }.file-drop strong { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 10px; }.file-drop small { margin-top: 4px; color: var(--ink-soft); font-size: 8px; }.submit-button { width: 100%; height: 40px; margin-top: 11px; border: 0; border-radius: 10px; color: #fff; background: var(--lagoon); font-size: 11px; font-weight: 700; }.submit-button:disabled { opacity: .45; cursor: not-allowed; }.submit-error { margin-top: 9px; color: #a44634; font-size: 9px; line-height: 1.5; }.submission-record { margin-top: 22px; padding: 16px; border-radius: 12px; background: #e8f7f0; }.submission-record > div { display: flex; align-items: center; gap: 10px; }.check { width: 28px; height: 28px; display: grid; place-items: center; border-radius: 50%; color: #fff; background: #4cad88; }.submission-record div div { display: flex; flex-direction: column; }.submission-record strong { font-size: 11px; }.submission-record small,.submission-record p { margin-top: 4px; color: var(--ink-soft); font-size: 9px; }.submission-record p { margin-top: 10px; }.detail-panel.empty { display: grid; place-items: center; }.detail-empty { max-width: 230px; text-align: center; }.detail-empty > span { width: 54px; height: 54px; display: grid; place-items: center; margin: 0 auto 16px; border-radius: 50%; color: var(--lagoon); background: var(--sky); font-size: 21px; }.detail-empty strong { font: 700 15px var(--display); }.detail-empty p { margin-top: 8px; color: var(--ink-soft); font-size: 10px; line-height: 1.7; }
.file-viewer-backdrop { position: fixed; z-index: 30; inset: 0; display: grid; place-items: center; padding: 20px; background: rgba(16, 41, 58, .5); }.file-viewer { width: min(900px, 100%); max-height: min(760px, calc(100vh - 40px)); display: grid; grid-template-rows: auto minmax(0, 1fr) auto; overflow: hidden; border: 1px solid var(--line); border-radius: 12px; background: #fff; box-shadow: 0 28px 80px rgba(12, 35, 50, .3); }.file-viewer-header { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; padding: 20px 22px 16px; border-bottom: 1px solid var(--line); }.file-viewer-header p { color: var(--lagoon); font: 700 9px var(--mono); letter-spacing: .14em; }.file-viewer-header h2 { max-width: 680px; margin-top: 7px; overflow: hidden; color: var(--ink); font: 700 16px var(--display); text-overflow: ellipsis; white-space: nowrap; }.viewer-close { width: 32px; height: 32px; flex: none; border: 1px solid var(--line); border-radius: 8px; color: var(--ink-soft); background: #fff; font-size: 23px; line-height: 1; }.viewer-close:hover { color: var(--lagoon-deep); background: var(--sky); }.viewer-state { min-height: 300px; display: flex; align-items: center; justify-content: center; gap: 9px; padding: 28px; color: var(--ink-soft); font-size: 12px; text-align: center; }.viewer-state.error { flex-direction: column; }.viewer-state.error strong { color: #a44634; }.markdown-viewer { min-height: 0; overflow: auto; padding: 26px max(24px, 6%); color: #21384b; font-size: 13px; line-height: 1.85; }.markdown-viewer :deep(h1),.markdown-viewer :deep(h2),.markdown-viewer :deep(h3),.markdown-viewer :deep(h4) { margin: 1.4em 0 .55em; color: #102f48; font-family: var(--display); line-height: 1.35; }.markdown-viewer :deep(h1) { margin-top: 0; font-size: 25px; }.markdown-viewer :deep(h2) { font-size: 20px; }.markdown-viewer :deep(h3) { font-size: 16px; }.markdown-viewer :deep(p),.markdown-viewer :deep(ul),.markdown-viewer :deep(ol),.markdown-viewer :deep(pre),.markdown-viewer :deep(blockquote) { margin: 0 0 14px; }.markdown-viewer :deep(ul),.markdown-viewer :deep(ol) { padding-left: 22px; }.markdown-viewer :deep(a) { color: var(--lagoon); text-decoration: underline; }.markdown-viewer :deep(code) { padding: 2px 4px; border-radius: 4px; background: #eef4f7; font: 12px var(--mono); }.markdown-viewer :deep(pre) { overflow: auto; padding: 14px; border-radius: 8px; background: #102f48; color: #eff8fa; }.markdown-viewer :deep(pre code) { padding: 0; background: transparent; color: inherit; }.markdown-viewer :deep(blockquote) { padding: 8px 14px; border-left: 3px solid var(--lagoon); background: var(--sky); color: var(--ink-soft); }.markdown-viewer :deep(hr) { border: 0; border-top: 1px solid var(--line); }.markdown-viewer :deep(.md-table-wrap) { overflow: auto; margin-bottom: 14px; }.markdown-viewer :deep(table) { width: 100%; border-collapse: collapse; font-size: 12px; }.markdown-viewer :deep(th),.markdown-viewer :deep(td) { padding: 8px 10px; border: 1px solid var(--line); text-align: left; }.markdown-viewer :deep(th) { background: var(--sky); }.file-viewer-footer { display: flex; align-items: center; justify-content: space-between; gap: 16px; padding: 13px 22px; border-top: 1px solid var(--line); background: #f8fbfc; }.file-viewer-footer span { color: var(--ink-soft); font-size: 10px; }.viewer-download { height: 34px; padding: 0 14px; border: 0; border-radius: 8px; color: #fff; background: var(--lagoon); font-size: 11px; font-weight: 700; }.viewer-download:disabled { opacity: .55; cursor: not-allowed; }
@media (max-width: 900px) { .homework-layout { grid-template-columns: 1fr; }.detail-panel { position: static; min-height: 0; }.detail-panel.empty { display: none; } }
@media (max-width: 620px) { .task-summary { align-self: stretch; justify-content: center; }.toolbar { align-items: stretch; flex-direction: column; }.filter-tabs { overflow-x: auto; }.search-box { width: 100%; }.task-card { grid-template-columns: 60px minmax(0,1fr) 18px; }.task-meta { grid-column: 2; text-align: left; }.task-arrow { grid-column: 3; grid-row: 1 / 3; }.detail-meta { grid-template-columns: 1fr; }.file-viewer-backdrop { padding: 10px; }.file-viewer { max-height: calc(100vh - 20px); }.file-viewer-header,.file-viewer-footer { padding-left: 15px; padding-right: 15px; }.file-viewer-footer { align-items: stretch; flex-direction: column; }.viewer-download { width: 100%; }.markdown-viewer { padding: 19px 16px; } }
</style>
