<template>
  <div class="page-container">
    <!-- 页头标识 -->
    <header class="page-head">
      <div>
        <p class="eyebrow">SYS:COURSE · DETAIL</p>
        <h1>
          <el-button type="text" icon="el-icon-back" class="back-btn" @click="goBack">课程列表</el-button>
          <span class="head-sep">/</span>
          {{ courseName }}
        </h1>
      </div>
      <div class="head-stat">
        <b>{{ total }}</b>
        <span>条详情</span>
      </div>
    </header>

    <!-- 搜索区域 -->
    <div class="search-bar">
      <el-form :inline="true" :model="queryParams" @submit.native.prevent>
        <el-form-item label="阶段">
          <el-input v-model="queryParams.stageName" placeholder="请输入阶段名称" clearable @keyup.enter.native="handleSearch" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" @click="handleSearch">查询</el-button>
          <el-button icon="el-icon-refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 表格区域 -->
    <div class="table-card">
      <div style="margin-bottom: 14px;">
        <el-button type="primary" icon="el-icon-plus" @click="handleAdd">新增详情</el-button>
        <el-button icon="el-icon-upload2" :loading="importLoading" @click="handleImportClick">导入 Excel</el-button>
        <el-button
          type="danger"
          icon="el-icon-delete"
          :disabled="selectedRows.length === 0"
          @click="handleBatchDelete"
        >批量删除</el-button>
        <span v-if="selectedRows.length" class="selection-tip">已选 {{ selectedRows.length }} 项</span>
        <input
          ref="fileInput"
          type="file"
          accept=".xlsx,.xls"
          style="display: none;"
          @change="handleFileChange"
        />
      </div>

      <el-table :data="tableData" v-loading="loading" stripe @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="48" align="center" />
        <el-table-column label="ID" width="70" align="center">
          <template slot-scope="{ row }"><code class="mono">{{ row.id }}</code></template>
        </el-table-column>
        <el-table-column prop="stageName" label="阶段" min-width="140" show-overflow-tooltip />
        <el-table-column label="第几天" width="90" align="center">
          <template slot-scope="{ row }">
            <code class="mono" v-if="row.dayNumber">D{{ row.dayNumber }}</code>
            <span v-else style="color: var(--ink-3);">—</span>
          </template>
        </el-table-column>
        <el-table-column prop="classContent" label="上课内容" min-width="260" show-overflow-tooltip />
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

    <!-- 新增/编辑弹窗 -->
    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="520px" @closed="resetForm">
      <el-form ref="detailForm" :model="form" :rules="formRules" label-width="100px" class="dialog-form">
        <el-form-item label="阶段" prop="stageName">
          <el-input v-model="form.stageName" placeholder="请输入阶段名称" />
        </el-form-item>
        <el-form-item label="第几天" prop="dayNumber">
          <el-input-number v-model="form.dayNumber" :min="1" :max="9999" placeholder="线下课程填写" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="上课内容" prop="classContent">
          <el-input v-model="form.classContent" type="textarea" :rows="4" placeholder="请输入上课内容" />
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
import { pageCourseDetail, addCourseDetail, updateCourseDetail, deleteCourseDetail, batchDeleteCourseDetails, importCourseDetail } from '@/api/courseDetail'
import { getCourse } from '@/api/course'

export default {
  name: 'CourseDetailManage',
  data() {
    return {
      courseId: null,
      courseName: '课程详情',
      loading: false,
      tableData: [],
      selectedRows: [],
      total: 0,
      queryParams: {
        current: 1,
        size: 10,
        courseId: null,
        stageName: ''
      },
      // 弹窗相关
      dialogVisible: false,
      isEdit: false,
      submitLoading: false,
      importLoading: false,
      editId: null,
      form: {
        stageName: '',
        dayNumber: null,
        classContent: ''
      },
      formRules: {
        stageName: [
          { required: true, message: '请输入阶段名称', trigger: 'blur' },
          { max: 128, message: '长度不能超过128个字符', trigger: 'blur' }
        ],
        classContent: [
          { max: 1000, message: '长度不能超过1000个字符', trigger: 'blur' }
        ]
      }
    }
  },
  computed: {
    dialogTitle() {
      return this.isEdit ? '编辑课程详情' : '新增课程详情'
    }
  },
  created() {
    this.courseId = Number(this.$route.params.courseId)
    this.queryParams.courseId = this.courseId
    this.fetchCourseInfo()
    this.fetchData()
  },
  methods: {
    goBack() {
      this.$router.push('/course')
    },
    // 获取课程名称
    async fetchCourseInfo() {
      try {
        const res = await getCourse(this.courseId)
        if (res.data) {
          this.courseName = res.data.courseName
        }
      } catch (e) {
        // 错误已由请求拦截器统一处理
      }
    },
    // 查询列表
    async fetchData() {
      this.loading = true
      try {
        const res = await pageCourseDetail(this.queryParams)
        this.tableData = res.data.records || []
        this.selectedRows = []
        this.total = res.data.total || 0
      } catch (e) {
        // 错误已由请求拦截器统一处理
      } finally {
        this.loading = false
      }
    },
    handleSearch() {
      this.queryParams.current = 1
      this.fetchData()
    },
    handleReset() {
      this.queryParams.stageName = ''
      this.queryParams.current = 1
      this.fetchData()
    },
    handleSelectionChange(selection) {
      this.selectedRows = selection
    },
    // 新增
    handleAdd() {
      this.isEdit = false
      this.editId = null
      this.dialogVisible = true
    },
    // 编辑
    handleEdit(row) {
      this.isEdit = true
      this.editId = row.id
      this.form = {
        stageName: row.stageName,
        dayNumber: row.dayNumber,
        classContent: row.classContent || ''
      }
      this.dialogVisible = true
    },
    // 提交表单
    handleSubmit() {
      this.$refs.detailForm.validate(async valid => {
        if (!valid) return
        this.submitLoading = true
        try {
          if (this.isEdit) {
            await updateCourseDetail(this.editId, this.form)
            this.$message.success('修改成功')
          } else {
            await addCourseDetail({ ...this.form, courseId: this.courseId })
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
      this.form = { stageName: '', dayNumber: null, classContent: '' }
      this.$nextTick(() => {
        this.$refs.detailForm && this.$refs.detailForm.clearValidate()
      })
    },
    // 删除
    handleDelete(row) {
      this.$confirm(`确定要删除「${row.stageName}」的详情记录吗？`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        await deleteCourseDetail(row.id)
        this.$message.success('删除成功')
        this.fetchData()
      }).catch(() => {})
    },
    // 批量删除
    handleBatchDelete() {
      if (!this.selectedRows.length) return
      const ids = this.selectedRows.map(item => item.id)
      const days = this.selectedRows
        .map(item => item.dayNumber ? `D${item.dayNumber}` : `ID:${item.id}`)
        .join('、')
      this.$confirm(`确定要批量删除已选的 ${ids.length} 条课程详情吗？删除后将自动重新调整天数顺序。已选：${days}`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        await batchDeleteCourseDetails(ids)
        this.$message.success('批量删除成功')
        this.fetchData()
      }).catch(() => {})
    },
    // Excel 导入
    handleImportClick() {
      this.$refs.fileInput.value = ''
      this.$refs.fileInput.click()
    },
    async handleFileChange(e) {
      const file = e.target.files[0]
      if (!file) return
      this.importLoading = true
      try {
        const res = await importCourseDetail(this.courseId, file)
        this.$message.success(`导入成功，共 ${res.data} 条记录`)
        this.fetchData()
      } catch (err) {
        // 错误已由请求拦截器统一处理
      } finally {
        this.importLoading = false
      }
    }
  }
}
</script>

<style scoped>
.back-btn {
  font-size: 16px;
  padding: 0;
  vertical-align: baseline;
}
.head-sep {
  margin: 0 8px;
  color: var(--ink-3);
  font-weight: 300;
}
.selection-tip {
  margin-left: 12px;
  color: var(--ink-3);
  font-size: 13px;
}
</style>
