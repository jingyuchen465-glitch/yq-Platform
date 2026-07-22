<template>
  <div class="page-container">
    <!-- 页头标识 -->
    <header class="page-head">
      <div>
        <p class="eyebrow">SYS:CAMPUS · LOCATION</p>
        <h1>校区管理</h1>
      </div>
      <div class="head-stat">
        <b>{{ total }}</b>
        <span>个校区</span>
      </div>
    </header>

    <!-- 搜索区域 -->
    <div class="search-bar">
      <el-form :inline="true" :model="queryParams" @submit.native.prevent>
        <el-form-item label="校区地点">
          <el-input v-model="queryParams.campusLocation" placeholder="请输入校区地点" clearable @keyup.enter.native="handleSearch" />
        </el-form-item>
        <el-form-item label="负责人">
          <el-input v-model="queryParams.managerName" placeholder="请输入负责人" clearable @keyup.enter.native="handleSearch" />
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
        <el-button type="primary" icon="el-icon-plus" @click="handleAdd">新增校区</el-button>
      </div>

      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column label="ID" width="70" align="center">
          <template slot-scope="{ row }"><code class="mono">{{ row.id }}</code></template>
        </el-table-column>
        <el-table-column prop="campusLocation" label="校区地点" min-width="200" show-overflow-tooltip />
        <el-table-column prop="managerName" label="负责人" min-width="120" show-overflow-tooltip />
        <el-table-column label="负责人电话" min-width="140">
          <template slot-scope="{ row }"><code class="mono">{{ row.managerPhone }}</code></template>
        </el-table-column>
        <el-table-column label="创建时间" width="170" align="center">
          <template slot-scope="{ row }"><code class="mono">{{ row.createdAt }}</code></template>
        </el-table-column>
        <el-table-column label="更新时间" width="170" align="center">
          <template slot-scope="{ row }"><code class="mono">{{ row.updatedAt }}</code></template>
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
    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="480px" @closed="resetForm">
      <el-form ref="campusForm" :model="form" :rules="formRules" label-width="100px" class="dialog-form">
        <el-form-item label="校区地点" prop="campusLocation">
          <el-input v-model="form.campusLocation" placeholder="请输入校区地点" />
        </el-form-item>
        <el-form-item label="负责人" prop="managerName">
          <el-input v-model="form.managerName" placeholder="请输入负责人姓名" />
        </el-form-item>
        <el-form-item label="负责人电话" prop="managerPhone">
          <el-input v-model="form.managerPhone" placeholder="请输入负责人手机号" />
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
import { pageCampus, addCampus, updateCampus, deleteCampus } from '@/api/campus'

export default {
  name: 'CampusManage',
  data() {
    return {
      loading: false,
      tableData: [],
      total: 0,
      queryParams: {
        current: 1,
        size: 10,
        campusLocation: '',
        managerName: ''
      },
      // 弹窗相关
      dialogVisible: false,
      isEdit: false,
      submitLoading: false,
      editId: null,
      form: {
        campusLocation: '',
        managerName: '',
        managerPhone: ''
      },
      formRules: {
        campusLocation: [
          { required: true, message: '请输入校区地点', trigger: 'blur' },
          { max: 255, message: '长度不能超过255个字符', trigger: 'blur' }
        ],
        managerName: [
          { required: true, message: '请输入负责人姓名', trigger: 'blur' },
          { max: 64, message: '长度不能超过64个字符', trigger: 'blur' }
        ],
        managerPhone: [
          { required: true, message: '请输入负责人电话', trigger: 'blur' },
          { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
        ]
      }
    }
  },
  computed: {
    dialogTitle() {
      return this.isEdit ? '编辑校区' : '新增校区'
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
        const res = await pageCampus(this.queryParams)
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
      this.queryParams = { current: 1, size: 10, campusLocation: '', managerName: '' }
      this.fetchData()
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
        campusLocation: row.campusLocation,
        managerName: row.managerName,
        managerPhone: row.managerPhone
      }
      this.dialogVisible = true
    },
    // 提交表单
    handleSubmit() {
      this.$refs.campusForm.validate(async valid => {
        if (!valid) return
        this.submitLoading = true
        try {
          if (this.isEdit) {
            await updateCampus(this.editId, this.form)
            this.$message.success('修改成功')
          } else {
            await addCampus(this.form)
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
      this.form = { campusLocation: '', managerName: '', managerPhone: '' }
      this.$nextTick(() => {
        this.$refs.campusForm && this.$refs.campusForm.clearValidate()
      })
    },
    // 删除
    handleDelete(row) {
      this.$confirm(`确定要删除校区「${row.campusLocation}」吗？`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        await deleteCampus(row.id)
        this.$message.success('删除成功')
        this.fetchData()
      }).catch(() => {})
    }
  }
}
</script>
