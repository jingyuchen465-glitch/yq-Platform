<template>
  <div class="student-page result-page">
    <student-header
      :profile="profile"
      :logging-out="loggingOut"
      :homework-count="headerHomeworkCount"
      :order-count="headerOrderCount"
      @logout="handleLogout"
    />

    <main class="page-shell">
      <button class="back-link" type="button" @click="$router.push('/exams')">
        <span aria-hidden="true">←</span> 返回考试列表
      </button>

      <div v-if="loading" class="empty-state">
        <strong>正在读取答卷</strong>
        <span>成绩、教师评语和题目解析正在加载。</span>
      </div>

      <div v-else-if="errorMessage" class="error-state">
        <strong>暂时无法查看答卷</strong>
        <span>{{ errorMessage }}</span>
        <button class="retry-button" type="button" @click="load">重新加载</button>
      </div>

      <template v-else-if="result">
        <header class="result-hero">
          <div>
            <p class="page-kicker">ANSWER REVIEW</p>
            <h1>{{ result.paperName }}</h1>
            <p>逐题核对你的作答、标准答案与教师评语。客观题已由系统判分，主观题由教师批改。</p>
          </div>
          <div class="score-seal" aria-label="考试得分">
            <small>FINAL SCORE</small>
            <strong>{{ formatScore(result.score) }}</strong>
            <span>/ {{ formatScore(result.totalScore) }}</span>
          </div>
        </header>

        <section class="result-summary">
          <div><span>答题记录</span><b>#{{ result.recordId }}</b></div>
          <div><span>题目数量</span><b>{{ result.questions.length }} 题</b></div>
          <div><span>客观题正确</span><b>{{ objectiveCorrect }} / {{ objectiveTotal }}</b></div>
          <div><span>批改状态</span><b>已完成</b></div>
        </section>

        <section class="review-list">
          <article
            v-for="(question, index) in result.questions"
            :key="question.paperQuestionId"
            :class="['review-card', verdictClass(question)]"
          >
            <header>
              <div class="question-number">{{ String(index + 1).padStart(2, '0') }}</div>
              <div class="question-meta">
                <span>{{ typeLabel(question.questionType) }}</span>
                <b>{{ formatScore(question.score) }} / {{ formatScore(question.maxScore) }} 分</b>
              </div>
              <span class="verdict">{{ verdictLabel(question) }}</span>
            </header>

            <h2>{{ question.questionContent }}</h2>

            <div class="answer-grid">
              <div class="answer-panel student-answer">
                <span>你的答案</span>
                <p>{{ displayAnswer(question.studentAnswer) }}</p>
              </div>
              <div class="answer-panel correct-answer">
                <span>标准答案</span>
                <p>{{ displayAnswer(question.correctAnswer) }}</p>
              </div>
            </div>

            <div v-if="question.graderComment" class="teacher-note">
              <span>教师评语</span>
              <p>{{ question.graderComment }}</p>
            </div>

            <div class="analysis-panel">
              <span>题目解析</span>
              <p>{{ question.analysisContent || '本题暂未填写解析。' }}</p>
            </div>
          </article>
        </section>
      </template>
    </main>
  </div>
</template>

<script>
import StudentHeader from '@/components/StudentHeader.vue'
import studentPage from '@/mixins/studentPage'
import { getStudentExamResult } from '@/api/exam'

export default {
  name: 'StudentExamResult',
  components: { StudentHeader },
  mixins: [studentPage],
  data() {
    return {
      result: null,
      loading: true,
      errorMessage: ''
    }
  },
  computed: {
    objectiveQuestions() {
      if (!this.result) return []
      return this.result.questions.filter(item => ['SINGLE', 'MULTIPLE', 'JUDGE'].includes(item.questionType))
    },
    objectiveCorrect() {
      return this.objectiveQuestions.filter(item => item.correct === true).length
    },
    objectiveTotal() {
      return this.objectiveQuestions.length
    }
  },
  created() {
    this.load()
  },
  methods: {
    async load() {
      this.loading = true
      this.errorMessage = ''
      try {
        const response = await getStudentExamResult(this.$route.params.recordId)
        this.result = response.data
      } catch (error) {
        this.errorMessage = error.message
      } finally {
        this.loading = false
      }
    },
    typeLabel(type) {
      return {
        SINGLE: '单选题',
        MULTIPLE: '多选题',
        JUDGE: '判断题',
        FILL: '填空题',
        SHORT: '简答题'
      }[type] || type
    },
    verdictLabel(question) {
      if (question.correct === true) return '回答正确'
      if (question.correct === false) return '回答错误'
      return '教师评分'
    },
    verdictClass(question) {
      if (question.correct === true) return 'correct'
      if (question.correct === false) return 'incorrect'
      return 'manual'
    },
    displayAnswer(value) {
      if (value === null || value === undefined || String(value).trim() === '') return '未作答'
      if (value === 'TRUE') return '正确'
      if (value === 'FALSE') return '错误'
      return value
    },
    formatScore(value) {
      const number = Number(value || 0)
      return Number.isInteger(number) ? String(number) : number.toFixed(1)
    }
  }
}
</script>

<style scoped>
.result-page { min-height: 100vh; }
.back-link { display: inline-flex; align-items: center; gap: 8px; margin-bottom: 22px; border: 0; color: var(--lagoon-deep); background: transparent; font-size: 12px; font-weight: 700; }
.back-link span { font: 18px var(--mono); }
.result-hero { min-height: 230px; display: flex; align-items: center; justify-content: space-between; gap: 40px; padding: 42px 48px; overflow: hidden; position: relative; border-radius: 12px 42px 42px 42px; color: #fff; background: var(--lagoon-deep); box-shadow: var(--shadow); }
.result-hero::after { content: ''; width: 260px; height: 260px; position: absolute; right: -70px; bottom: -145px; border: 34px solid rgba(217, 243, 238, .12); border-radius: 50%; }
.result-hero .page-kicker { color: #8fe3d6; }
.result-hero h1 { max-width: 700px; margin-top: 10px; font: 760 clamp(28px, 4vw, 46px)/1.15 var(--display); letter-spacing: -.04em; }
.result-hero p:not(.page-kicker) { max-width: 610px; margin-top: 14px; color: rgba(255, 255, 255, .72); font-size: 13px; line-height: 1.75; }
.score-seal { width: 150px; height: 150px; z-index: 1; display: grid; flex: 0 0 auto; place-content: center; border: 1px solid rgba(255, 255, 255, .32); border-radius: 50%; background: rgba(255, 255, 255, .08); text-align: center; }
.score-seal small { font: 8px var(--mono); letter-spacing: .18em; opacity: .68; }
.score-seal strong { margin-top: 5px; font: 800 44px/1 var(--display); }
.score-seal span { margin-top: 4px; font: 11px var(--mono); opacity: .72; }
.result-summary { display: grid; grid-template-columns: repeat(4, 1fr); gap: 1px; margin: 18px 0 28px; overflow: hidden; border: 1px solid var(--line); border-radius: 14px; background: var(--line); }
.result-summary div { display: flex; flex-direction: column; gap: 7px; padding: 19px 22px; background: #fff; }
.result-summary span { color: var(--ink-soft); font-size: 9px; }
.result-summary b { font: 700 14px var(--display); }
.review-list { display: grid; gap: 18px; }
.review-card { overflow: hidden; border: 1px solid var(--line); border-left: 5px solid var(--lagoon); border-radius: 8px 22px 22px 22px; background: #fff; box-shadow: var(--shadow); }
.review-card.incorrect { border-left-color: var(--coral); }
.review-card.manual { border-left-color: #d6a84b; }
.review-card > header { display: grid; grid-template-columns: 52px 1fr auto; align-items: center; gap: 14px; padding: 17px 22px; border-bottom: 1px solid var(--line); background: #fbfdfe; }
.question-number { color: var(--lagoon-deep); font: 800 18px var(--mono); }
.question-meta { display: flex; align-items: center; gap: 12px; }
.question-meta span { color: var(--ink-soft); font-size: 10px; }
.question-meta b { font: 700 10px var(--mono); }
.verdict { padding: 6px 9px; border-radius: 999px; color: var(--lagoon-deep); background: var(--mint); font-size: 9px; font-weight: 700; }
.incorrect .verdict { color: #a5442b; background: #fff0eb; }
.manual .verdict { color: #7a5a18; background: #fff5db; }
.review-card h2 { padding: 25px 28px 20px; font: 680 17px/1.75 var(--display); }
.answer-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; padding: 0 28px 18px; }
.answer-panel, .teacher-note, .analysis-panel { padding: 17px 18px; border-radius: 12px; }
.answer-panel { border: 1px solid var(--line); background: var(--paper); }
.answer-panel span, .teacher-note span, .analysis-panel span { display: block; margin-bottom: 8px; color: var(--ink-soft); font: 700 9px var(--mono); letter-spacing: .08em; }
.answer-panel p, .teacher-note p, .analysis-panel p { white-space: pre-wrap; font-size: 13px; line-height: 1.75; }
.correct-answer { border-color: rgba(14, 116, 144, .2); background: #eff9f7; }
.teacher-note { margin: 0 28px 12px; color: #6e5117; background: #fff7e4; }
.analysis-panel { margin: 0 28px 28px; color: var(--ink-soft); background: var(--sky); }
@media (max-width: 760px) {
  .result-hero { align-items: flex-start; flex-direction: column; padding: 32px 26px; }
  .score-seal { width: 112px; height: 112px; }
  .score-seal strong { font-size: 34px; }
  .result-summary { grid-template-columns: 1fr 1fr; }
  .answer-grid { grid-template-columns: 1fr; }
  .review-card > header { grid-template-columns: 44px 1fr; }
  .verdict { grid-column: 2; justify-self: start; }
}
</style>
