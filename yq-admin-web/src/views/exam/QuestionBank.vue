<template>
  <div class="question-page page-container">
    <header class="page-head">
      <div><p class="eyebrow">EXAM · QUESTION VAULT</p><h1>题库管理</h1><p class="subcopy">按课程阶段、题型与难度维护可复用的考试题目。</p></div>
      <el-button type="primary" icon="el-icon-plus" @click="openCreate">新增题目</el-button>
    </header>

    <section class="search-bar">
      <el-form :inline="true" :model="query">
        <el-form-item label="题干"><el-input v-model.trim="query.keyword" clearable placeholder="输入关键词" @keyup.enter.native="search" /></el-form-item>
        <el-form-item label="课程"><el-select v-model="query.courseId" clearable filterable @change="handleQueryCourse"><el-option v-for="item in courses" :key="item.id" :label="item.courseName" :value="item.id" /></el-select></el-form-item>
        <el-form-item label="阶段"><el-select v-model="query.stageName" clearable filterable><el-option v-for="stage in queryStages" :key="stage" :label="stage" :value="stage" /></el-select></el-form-item>
        <el-form-item label="题型"><el-select v-model="query.questionType" clearable><el-option v-for="item in typeOptions" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item>
        <el-form-item label="难度"><el-select v-model="query.difficulty" clearable><el-option label="简单" value="EASY" /><el-option label="普通" value="NORMAL" /><el-option label="困难" value="HARD" /></el-select></el-form-item>
        <el-form-item label="状态"><el-select v-model="query.status" clearable><el-option label="启用" value="ENABLED" /><el-option label="停用" value="DISABLED" /></el-select></el-form-item>
        <el-form-item><el-button type="primary" icon="el-icon-search" @click="search">查询</el-button><el-button @click="resetQuery">重置</el-button></el-form-item>
      </el-form>
    </section>

    <section class="table-card">
      <el-table v-loading="loading" :data="records" row-key="id" stripe>
        <el-table-column type="expand">
          <template slot-scope="scope">
            <div class="question-detail">
              <h3>{{ scope.row.questionContent }}</h3>
              <ol v-if="scope.row.options && scope.row.options.length"><li v-for="option in scope.row.options" :key="option.id"><b>{{ option.optionKey }}</b>{{ option.optionContent }}</li></ol>
              <div class="answer-line"><span>标准答案</span><b>{{ scope.row.answerContent }}</b></div>
              <p v-if="scope.row.analysisContent"><span>解析</span>{{ scope.row.analysisContent }}</p>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="id" label="ID" width="72" />
        <el-table-column label="题干" min-width="300"><template slot-scope="scope"><div class="stem-cell"><b>{{ scope.row.questionContent }}</b><small>{{ scope.row.options.length }} 个选项 · 更新于 {{ formatTime(scope.row.updatedAt) }}</small></div></template></el-table-column>
        <el-table-column label="题型" width="100"><template slot-scope="scope"><el-tag size="small">{{ typeLabel(scope.row.questionType) }}</el-tag></template></el-table-column>
        <el-table-column label="课程阶段" min-width="220"><template slot-scope="scope"><div class="stage-tags"><el-tag v-for="item in scope.row.courseStages" :key="item.id" size="small" type="info">{{ courseName(item.courseId) }} / {{ item.stageName }}</el-tag></div></template></el-table-column>
        <el-table-column label="难度" width="90"><template slot-scope="scope"><el-tag size="small" :type="difficultyTone(scope.row.difficulty)">{{ difficultyLabel(scope.row.difficulty) }}</el-tag></template></el-table-column>
        <el-table-column label="状态" width="95"><template slot-scope="scope"><span class="st" :class="scope.row.status === 'ENABLED' ? 'on' : 'off'"><i></i>{{ scope.row.status === 'ENABLED' ? '启用' : '停用' }}</span></template></el-table-column>
        <el-table-column label="操作" width="190" fixed="right"><template slot-scope="scope"><el-button type="text" icon="el-icon-edit" @click="openEdit(scope.row)">编辑</el-button><el-button type="text" :class="scope.row.status === 'ENABLED' ? 'danger-link' : ''" @click="toggleStatus(scope.row)">{{ scope.row.status === 'ENABLED' ? '停用' : '启用' }}</el-button></template></el-table-column>
      </el-table>
      <div class="pagination-wrapper"><el-pagination background layout="total, sizes, prev, pager, next" :total="total" :current-page="query.current" :page-size="query.size" :page-sizes="[10,20,50,100]" @current-change="page => { query.current = page; load() }" @size-change="size => { query.size = size; query.current = 1; load() }" /></div>
    </section>

    <el-dialog :title="form.id ? '编辑题目' : '新增题目'" :visible.sync="dialogVisible" width="760px" append-to-body @closed="resetForm">
      <el-form ref="form" :model="form" :rules="rules" label-width="92px">
        <div class="form-grid"><el-form-item label="题型" prop="questionType"><el-select v-model="form.questionType" @change="handleTypeChange"><el-option v-for="item in typeOptions" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item><el-form-item label="难度" prop="difficulty"><el-select v-model="form.difficulty"><el-option label="简单" value="EASY" /><el-option label="普通" value="NORMAL" /><el-option label="困难" value="HARD" /></el-select></el-form-item></div>
        <el-form-item label="题干" prop="questionContent"><el-input v-model="form.questionContent" type="textarea" :rows="4" placeholder="输入完整题干" /></el-form-item>
        <div v-if="isChoice" class="option-editor"><div class="section-title"><b>选项</b><el-button type="text" icon="el-icon-plus" @click="addOption">添加选项</el-button></div><div v-for="(option,index) in form.options" :key="index" class="option-row"><el-input v-model="option.optionKey" class="option-key" maxlength="4" /><el-input v-model="option.optionContent" placeholder="选项内容" /><el-button type="text" class="danger-link" icon="el-icon-delete" @click="removeOption(index)" /></div></div>
        <el-form-item label="标准答案" prop="answerContent"><el-select v-if="form.questionType === 'JUDGE'" v-model="form.answerContent"><el-option label="正确" value="TRUE" /><el-option label="错误" value="FALSE" /></el-select><el-input v-else v-model="form.answerContent" :placeholder="form.questionType === 'MULTIPLE' ? '多个选项用逗号分隔，如 A,C' : '输入标准答案'" /></el-form-item>
        <el-form-item label="答案解析"><el-input v-model="form.analysisContent" type="textarea" :rows="3" /></el-form-item>
        <div class="section-title relation-title"><b>课程阶段关联</b><el-button type="text" icon="el-icon-plus" @click="addRelation">添加关联</el-button></div>
        <div v-for="(relation,index) in form.courseStages" :key="index" class="relation-row"><el-select v-model="relation.courseId" filterable placeholder="课程" @change="value => loadStages(value)"><el-option v-for="item in courses" :key="item.id" :label="item.courseName" :value="item.id" /></el-select><el-select v-model="relation.stageName" filterable placeholder="阶段"><el-option v-for="stage in stageMap[relation.courseId] || []" :key="stage" :label="stage" :value="stage" /></el-select><el-button type="text" class="danger-link" icon="el-icon-delete" @click="form.courseStages.splice(index,1)" /></div>
      </el-form>
      <span slot="footer"><el-button @click="dialogVisible=false">取消</el-button><el-button type="primary" :loading="saving" @click="save">保存题目</el-button></span>
    </el-dialog>
  </div>
</template>

<script>
import { pageCourse } from '@/api/course'
import { pageCourseDetail } from '@/api/courseDetail'
import { pageExamQuestions, createExamQuestion, updateExamQuestion, updateExamQuestionStatus } from '@/api/exam'

const emptyForm = () => ({ id: null, questionType: 'SINGLE', questionContent: '', answerContent: '', analysisContent: '', difficulty: 'NORMAL', options: [{ optionKey: 'A', optionContent: '', sortOrder: 0 }, { optionKey: 'B', optionContent: '', sortOrder: 1 }, { optionKey: 'C', optionContent: '', sortOrder: 2 }, { optionKey: 'D', optionContent: '', sortOrder: 3 }], courseStages: [{ courseId: null, stageName: '' }] })

export default {
  name: 'QuestionBank',
  data() { return { loading: false, saving: false, dialogVisible: false, records: [], total: 0, courses: [], stageMap: {}, queryStages: [], query: { current: 1, size: 10, keyword: '', courseId: null, stageName: '', questionType: '', difficulty: '', status: '' }, form: emptyForm(), typeOptions: [{ value: 'SINGLE', label: '单选题' }, { value: 'MULTIPLE', label: '多选题' }, { value: 'JUDGE', label: '判断题' }, { value: 'FILL', label: '填空题' }, { value: 'SHORT', label: '简答题' }], rules: { questionType: [{ required: true, message: '请选择题型', trigger: 'change' }], difficulty: [{ required: true, message: '请选择难度', trigger: 'change' }], questionContent: [{ required: true, message: '请输入题干', trigger: 'blur' }], answerContent: [{ required: true, message: '请输入标准答案', trigger: 'blur' }] } } },
  computed: { isChoice() { return ['SINGLE', 'MULTIPLE'].includes(this.form.questionType) } },
  mounted() { this.loadCourses(); this.load() },
  methods: {
    async loadCourses() { const res = await pageCourse({ current: 1, size: 100 }); this.courses = (res.data && res.data.records) || [] },
    async loadStages(courseId) { if (!courseId || this.stageMap[courseId]) return; const res = await pageCourseDetail({ current: 1, size: 100, courseId }); const rows = (res.data && res.data.records) || []; this.$set(this.stageMap, courseId, [...new Set(rows.map(item => item.stageName).filter(Boolean))]) },
    async handleQueryCourse(courseId) { this.query.stageName = ''; if (!courseId) { this.queryStages = []; return } await this.loadStages(courseId); this.queryStages = this.stageMap[courseId] || [] },
    async load() { this.loading = true; try { const res = await pageExamQuestions(this.query); this.records = (res.data && res.data.records) || []; this.total = (res.data && res.data.total) || 0 } finally { this.loading = false } },
    search() { this.query.current = 1; this.load() },
    resetQuery() { this.query = { current: 1, size: 10, keyword: '', courseId: null, stageName: '', questionType: '', difficulty: '', status: '' }; this.queryStages = []; this.load() },
    openCreate() { this.form = emptyForm(); this.dialogVisible = true },
    async openEdit(row) { this.form = { id: row.id, questionType: row.questionType, questionContent: row.questionContent, answerContent: row.answerContent, analysisContent: row.analysisContent || '', difficulty: row.difficulty, options: row.options.map((item, index) => ({ optionKey: item.optionKey, optionContent: item.optionContent, sortOrder: index })), courseStages: row.courseStages.map(item => ({ courseId: item.courseId, stageName: item.stageName })) }; await Promise.all(this.form.courseStages.map(item => this.loadStages(item.courseId))); this.dialogVisible = true },
    handleTypeChange(type) { if (!['SINGLE', 'MULTIPLE'].includes(type)) this.form.options = []; else if (!this.form.options.length) this.form.options = emptyForm().options; this.form.answerContent = type === 'JUDGE' ? 'TRUE' : '' },
    addOption() { const key = String.fromCharCode(65 + this.form.options.length); this.form.options.push({ optionKey: key, optionContent: '', sortOrder: this.form.options.length }) },
    removeOption(index) { this.form.options.splice(index, 1); this.form.options.forEach((item, i) => { item.sortOrder = i }) },
    addRelation() { this.form.courseStages.push({ courseId: null, stageName: '' }) },
    async save() { this.$refs.form.validate(async valid => { if (!valid) return; if (!this.form.courseStages.length || this.form.courseStages.some(item => !item.courseId || !item.stageName)) return this.$message.warning('请完整选择课程阶段'); if (this.isChoice && this.form.options.some(item => !item.optionKey || !item.optionContent)) return this.$message.warning('请完整填写选项'); this.saving = true; try { const data = { questionType: this.form.questionType, questionContent: this.form.questionContent, answerContent: this.form.answerContent, analysisContent: this.form.analysisContent, difficulty: this.form.difficulty, courseStages: this.form.courseStages.map(item => ({ courseId: item.courseId, stageName: item.stageName })), options: this.form.options.map((item, index) => ({ optionKey: item.optionKey, optionContent: item.optionContent, sortOrder: index })) }; this.form.id ? await updateExamQuestion(this.form.id, data) : await createExamQuestion(data); this.$message.success('题目已保存'); this.dialogVisible = false; this.load() } finally { this.saving = false } }) },
    async toggleStatus(row) { const status = row.status === 'ENABLED' ? 'DISABLED' : 'ENABLED'; await this.$confirm(`确定${status === 'ENABLED' ? '启用' : '停用'}这道题吗？`, '状态确认'); await updateExamQuestionStatus(row.id, status); this.$message.success('状态已更新'); this.load() },
    resetForm() { this.form = emptyForm(); if (this.$refs.form) this.$refs.form.clearValidate() },
    courseName(id) { const item = this.courses.find(course => course.id === id); return item ? item.courseName : `课程 #${id}` },
    typeLabel(value) { const item = this.typeOptions.find(type => type.value === value); return item ? item.label : value },
    difficultyLabel(value) { return { EASY: '简单', NORMAL: '普通', HARD: '困难' }[value] || value },
    difficultyTone(value) { return { EASY: 'success', NORMAL: 'warning', HARD: 'danger' }[value] || 'info' },
    formatTime(value) { return value ? String(value).replace('T', ' ').slice(0, 16) : '-' }
  }
}
</script>

<style scoped>
.subcopy { margin-top: 8px; color: var(--ink-3); font-size: 13px; }.stem-cell { display:flex; flex-direction:column; gap:6px; }.stem-cell b { max-width:520px; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; }.stem-cell small { color:var(--ink-3); }.stage-tags { display:flex; flex-wrap:wrap; gap:5px; }.question-detail { margin: 8px 36px 16px; padding: 20px 24px; border-left: 3px solid var(--brass); background:#F8FAF7; }.question-detail h3 { font-size:15px; line-height:1.8; }.question-detail ol { display:grid; gap:7px; margin:14px 0; list-style:none; }.question-detail li { display:flex; gap:10px; }.question-detail li b { color:var(--jade-deep); }.answer-line,.question-detail p { margin-top:12px; padding-top:12px; border-top:1px solid var(--line); }.answer-line span,.question-detail p span { margin-right:14px; color:var(--ink-3); font-size:11px; }.form-grid { display:grid; grid-template-columns:1fr 1fr; gap:18px; }.form-grid .el-select { width:100%; }.section-title { display:flex; align-items:center; justify-content:space-between; margin:4px 0 10px 92px; }.option-editor { margin-bottom:18px; }.option-row,.relation-row { display:flex; gap:9px; margin:0 0 9px 92px; }.option-key { width:76px; flex:none; }.relation-title { margin-top:18px; }.relation-row .el-select { flex:1; }.danger-link { color:var(--clay); }
@media(max-width:800px){.form-grid{grid-template-columns:1fr}.option-row,.relation-row,.section-title{margin-left:0}.relation-row{flex-direction:column}}
</style>
