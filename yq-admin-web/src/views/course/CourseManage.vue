<template>
  <div class="page-container">
    <!-- 页头标识 -->
    <header class="page-head">
      <div>
        <p class="eyebrow">SYS:COURSE · TEACHING</p>
        <h1>课程管理</h1>
      </div>
      <div class="head-stat">
        <b>{{ total }}</b>
        <span>门课程</span>
      </div>
    </header>

    <!-- 搜索区域 -->
    <div class="search-bar">
      <el-form :inline="true" :model="queryParams" @submit.native.prevent>
        <el-form-item label="课程名称">
          <el-input v-model="queryParams.courseName" placeholder="请输入课程名称" clearable @keyup.enter.native="handleSearch" />
        </el-form-item>
        <el-form-item label="上课方式">
          <el-select v-model="queryParams.teachingMode" placeholder="全部" clearable style="width: 140px;" @change="handleSearch">
            <el-option label="线上" value="ONLINE" />
            <el-option label="线下" value="OFFLINE" />
          </el-select>
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
        <el-button type="primary" icon="el-icon-plus" @click="handleAdd">新增课程</el-button>
      </div>

      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column label="ID" width="70" align="center">
          <template slot-scope="{ row }"><code class="mono">{{ row.id }}</code></template>
        </el-table-column>
        <el-table-column prop="courseName" label="课程名称" min-width="160" show-overflow-tooltip />
        <el-table-column label="课程天数" width="100" align="center">
          <template slot-scope="{ row }"><code class="mono">{{ row.courseDays }}</code> 天</template>
        </el-table-column>
        <el-table-column label="上课方式" width="100" align="center">
          <template slot-scope="{ row }">
            <el-tag :type="row.teachingMode === 'ONLINE' ? '' : 'warning'" size="small">
              {{ row.teachingMode === 'ONLINE' ? '线上' : '线下' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="materialPath" label="资料路径" min-width="200" show-overflow-tooltip>
          <template slot-scope="{ row }"><code class="mono">{{ row.materialPath }}</code></template>
        </el-table-column>
        <el-table-column label="创建时间" width="170" align="center">
          <template slot-scope="{ row }"><code class="mono">{{ row.createdAt }}</code></template>
        </el-table-column>
        <el-table-column label="操作" width="200" align="center" fixed="right">
          <template slot-scope="{ row }">
            <el-button type="text" icon="el-icon-tickets" @click="handleDetail(row)">详情</el-button>
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
      <el-form ref="courseForm" :model="form" :rules="formRules" label-width="100px" class="dialog-form">
        <el-form-item label="课程名称" prop="courseName">
          <el-input v-model="form.courseName" placeholder="请输入课程名称" />
        </el-form-item>
        <el-form-item label="课程天数" prop="courseDays">
          <el-input-number v-model="form.courseDays" :min="1" :max="9999" placeholder="请输入课程天数" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="上课方式" prop="teachingMode">
          <el-radio-group v-model="form.teachingMode">
            <el-radio label="ONLINE">线上</el-radio>
            <el-radio label="OFFLINE">线下</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="资料路径" prop="materialPath">
          <el-input v-model="form.materialPath" placeholder="请输入资料路径" />
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
import { pageCourse, addCourse, updateCourse, deleteCourse } from '@/api/course'

export default {
  name: 'CourseManage',
  data() {
    return {
      loading: false,
      tableData: [],
      total: 0,
      queryParams: {
        current: 1,
        size: 10,
        courseName: '',
        teachingMode: ''
      },
      // 弹窗相关
      dialogVisible: false,
      isEdit: false,
      submitLoading: false,
      editId: null,
      form: {
        courseName: '',
        courseDays: 1,
        teachingMode: 'ONLINE',
        materialPath: ''
      },
      formRules: {
        courseName: [
          { required: true, message: '请输入课程名称', trigger: 'blur' },
          { max: 128, message: '长度不能超过128个字符', trigger: 'blur' }
        ],
        courseDays: [
          { required: true, message: '请输入课程天数', trigger: 'blur' }
        ],
        teachingMode: [
          { required: true, message: '请选择上课方式', trigger: 'change' }
        ],
        materialPath: [
          { required: true, message: '请输入资料路径', trigger: 'blur' },
          { max: 500, message: '长度不能超过500个字符', trigger: 'blur' }
        ]
      }
    }
  },
  computed: {
    dialogTitle() {
      return this.isEdit ? '编辑课程' : '新增课程'
    }
  },
  created() {
    this.fetchData()
  },
  methods: {
    // 查询列表
    async fetchData() {
      this.loading = true
      try {
        const res = await pageCourse(this.queryParams)
        this.tableData = res.data.records || []
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
      this.queryParams = { current: 1, size: 10, courseName: '', teachingMode: '' }
      this.fetchData()
    },
    // 详情
    handleDetail(row) {
      this.$router.push(`/course-detail/${row.id}`)
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
        courseName: row.courseName,
        courseDays: row.courseDays,
        teachingMode: row.teachingMode,
        materialPath: row.materialPath
      }
      this.dialogVisible = true
    },
    // 提交表单
    handleSubmit() {
      this.$refs.courseForm.validate(async valid => {
        if (!valid) return
        this.submitLoading = true
        try {
          if (this.isEdit) {
            await updateCourse(this.editId, this.form)
            this.$message.success('修改成功')
          } else {
            await addCourse(this.form)
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
      this.form = { courseName: '', courseDays: 1, teachingMode: 'ONLINE', materialPath: '' }
      this.$nextTick(() => {
        this.$refs.courseForm && this.$refs.courseForm.clearValidate()
      })
    },
    // 删除
    handleDelete(row) {
      this.$confirm(`确定要删除课程「${row.courseName}」吗？`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        await deleteCourse(row.id)
        this.$message.success('删除成功')
        this.fetchData()
      }).catch(() => {})
    }
  }
}
</script>
