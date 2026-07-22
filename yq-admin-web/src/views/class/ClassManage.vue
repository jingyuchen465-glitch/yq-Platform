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
        <el-table-column label="操作" width="150" align="center" fixed="right">
          <template slot-scope="{ row }">
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
  </div>
</template>

<script>
import { addClass, deleteClass, getClassFormOptions, pageClass, updateClass } from '@/api/class'

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
      isEdit: false,
      editId: null,
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
</style>
