<template>
  <div class="page-container">
    <!-- 页头标识 -->
    <header class="page-head">
      <div>
        <p class="eyebrow">SYS:ROLE · ACCESS</p>
        <h1>角色管理</h1>
      </div>
      <div class="head-stat">
        <b>{{ total }}</b>
        <span>个角色</span>
      </div>
    </header>

    <!-- 搜索区域 -->
    <div class="search-bar">
      <el-form :inline="true" :model="queryParams" @submit.native.prevent>
        <el-form-item label="角色编码">
          <el-input v-model="queryParams.roleCode" placeholder="请输入角色编码" clearable @keyup.enter.native="handleSearch" />
        </el-form-item>
        <el-form-item label="角色名称">
          <el-input v-model="queryParams.roleName" placeholder="请输入角色名称" clearable @keyup.enter.native="handleSearch" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" placeholder="全部" clearable style="width: 120px;">
            <el-option label="启用" value="ACTIVE" />
            <el-option label="禁用" value="INACTIVE" />
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
        <el-button type="primary" icon="el-icon-plus" @click="handleAdd">新增角色</el-button>
        <el-button type="danger" icon="el-icon-delete" :disabled="selectedIds.length === 0" @click="handleBatchDelete">批量删除</el-button>
        <span v-if="selectedIds.length" style="margin-left: 10px; color: #999; font-size: 13px;">已选 {{ selectedIds.length }} 项</span>
      </div>

      <el-table :data="tableData" v-loading="loading" stripe @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="50" align="center" />
        <el-table-column label="ID" width="70" align="center">
          <template slot-scope="{ row }"><code class="mono">{{ row.id }}</code></template>
        </el-table-column>
        <el-table-column label="角色编码" min-width="130" show-overflow-tooltip>
          <template slot-scope="{ row }"><code class="code-chip">{{ row.roleCode }}</code></template>
        </el-table-column>
        <el-table-column prop="roleName" label="角色名称" min-width="120" show-overflow-tooltip />
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="90" align="center">
          <template slot-scope="{ row }">
            <span class="st" :class="row.status === 'ACTIVE' ? 'on' : 'off'">
              <i></i>{{ row.status === 'ACTIVE' ? '启用' : '禁用' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="160" align="center">
          <template slot-scope="{ row }"><code class="mono">{{ row.createdAt }}</code></template>
        </el-table-column>
        <el-table-column label="操作" width="220" align="center" fixed="right">
          <template slot-scope="{ row }">
            <el-button type="text" icon="el-icon-edit" @click="handleEdit(row)">编辑</el-button>
            <el-button type="text" icon="el-icon-key" @click="handleAssignPermission(row)">授权</el-button>
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
    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="500px" @closed="resetForm">
      <el-form ref="roleForm" :model="form" :rules="rules" label-width="90px" class="dialog-form">
        <el-form-item label="角色编码" prop="roleCode">
          <el-input v-model="form.roleCode" placeholder="如：LECTURER（大写字母开头）" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="form.roleName" placeholder="请输入角色名称" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入角色描述" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio label="ACTIVE">启用</el-radio>
            <el-radio label="INACTIVE">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确 定</el-button>
      </div>
    </el-dialog>

    <!-- 权限授权弹窗 -->
    <el-dialog title="角色授权" :visible.sync="permDialogVisible" width="560px">
      <div v-loading="permLoading">
        <p style="margin-bottom: 12px; color: #666;">
          为角色 <b>{{ currentRole.roleName }}（{{ currentRole.roleCode }}）</b> 分配权限：
        </p>
        <div style="margin-bottom: 10px;">
          <el-button size="mini" @click="handleCheckAll">全选</el-button>
          <el-button size="mini" @click="selectedPermIds = []">清空</el-button>
          <span style="margin-left: 10px; color: #999; font-size: 12px;">已选 {{ selectedPermIds.length }} 项</span>
        </div>
        <el-table
          ref="permTable"
          :data="allPermissions"
          border
          max-height="360"
          @selection-change="handlePermSelectionChange"
        >
          <el-table-column type="selection" width="45" align="center" />
          <el-table-column prop="permissionCode" label="权限编码" min-width="160" show-overflow-tooltip />
          <el-table-column prop="permissionName" label="权限名称" min-width="120" show-overflow-tooltip />
          <el-table-column prop="apiPath" label="API路径" min-width="180" show-overflow-tooltip />
        </el-table>
      </div>
      <div slot="footer">
        <el-button @click="permDialogVisible = false">取 消</el-button>
        <el-button type="primary" :loading="permSubmitLoading" @click="handleSubmitPermissions">确 定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { pageRoles, addRole, updateRole, deleteRole, batchDeleteRoles, getRolePermissionIds, assignRolePermissions } from '@/api/role'
import { listPermissions } from '@/api/permission'

export default {
  name: 'RoleManage',
  data() {
    return {
      loading: false,
      tableData: [],
      total: 0,
      selectedIds: [],
      queryParams: {
        current: 1,
        size: 10,
        roleCode: '',
        roleName: '',
        status: ''
      },
      // 弹窗相关
      dialogVisible: false,
      isEdit: false,
      submitLoading: false,
      editId: null,
      form: {
        roleCode: '',
        roleName: '',
        description: '',
        status: 'ACTIVE'
      },
      rules: {
        roleCode: [
          { required: true, message: '请输入角色编码', trigger: 'blur' },
          { pattern: /^[A-Z][A-Z0-9_]{1,63}$/, message: '必须为2~64位大写字母、数字或下划线，且以大写字母开头', trigger: 'blur' }
        ],
        roleName: [
          { required: true, message: '请输入角色名称', trigger: 'blur' },
          { max: 64, message: '长度不能超过64个字符', trigger: 'blur' }
        ],
        description: [
          { max: 255, message: '长度不能超过255个字符', trigger: 'blur' }
        ],
        status: [
          { required: true, message: '请选择状态', trigger: 'change' }
        ]
      },
      // 授权相关
      permDialogVisible: false,
      permLoading: false,
      permSubmitLoading: false,
      currentRole: {},
      allPermissions: [],
      selectedPermIds: [],
      initPermIds: []
    }
  },
  computed: {
    dialogTitle() {
      return this.isEdit ? '编辑角色' : '新增角色'
    }
  },
  created() {
    this.fetchData()
  },
  methods: {
    async fetchData() {
      this.loading = true
      try {
        const res = await pageRoles(this.queryParams)
        this.tableData = res.data.records || []
        this.total = res.data.total || 0
      } finally {
        this.loading = false
      }
    },
    handleSearch() {
      this.queryParams.current = 1
      this.fetchData()
    },
    handleReset() {
      this.queryParams = { current: 1, size: 10, roleCode: '', roleName: '', status: '' }
      this.fetchData()
    },
    handleAdd() {
      this.isEdit = false
      this.editId = null
      this.dialogVisible = true
    },
    handleEdit(row) {
      this.isEdit = true
      this.editId = row.id
      this.form = {
        roleCode: row.roleCode,
        roleName: row.roleName,
        description: row.description || '',
        status: row.status
      }
      this.dialogVisible = true
    },
    handleSubmit() {
      this.$refs.roleForm.validate(async valid => {
        if (!valid) return
        this.submitLoading = true
        try {
          if (this.isEdit) {
            await updateRole(this.editId, this.form)
            this.$message.success('修改成功')
          } else {
            await addRole(this.form)
            this.$message.success('新增成功')
          }
          this.dialogVisible = false
          this.fetchData()
        } finally {
          this.submitLoading = false
        }
      })
    },
    resetForm() {
      this.form = { roleCode: '', roleName: '', description: '', status: 'ACTIVE' }
      this.$nextTick(() => {
        this.$refs.roleForm && this.$refs.roleForm.clearValidate()
      })
    },
    handleDelete(row) {
      this.$confirm(`确定要删除角色「${row.roleName}」吗？角色正在被用户使用时将无法删除。`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        await deleteRole(row.id)
        this.$message.success('删除成功')
        this.fetchData()
      }).catch(() => {})
    },
    // 批量删除
    handleBatchDelete() {
      this.$confirm(`确定要删除选中的 ${this.selectedIds.length} 个角色吗？角色正在被用户使用时将无法删除。`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        await batchDeleteRoles(this.selectedIds)
        this.$message.success('批量删除成功')
        this.selectedIds = []
        this.fetchData()
      }).catch(() => {})
    },
    handleSelectionChange(selection) {
      this.selectedIds = selection.map(item => item.id)
    },
    // 授权
    async handleAssignPermission(row) {
      this.currentRole = row
      this.permDialogVisible = true
      this.permLoading = true
      this.selectedPermIds = []
      try {
        const [permRes, rolePermRes] = await Promise.all([
          listPermissions({}),
          getRolePermissionIds(row.id)
        ])
        this.allPermissions = permRes.data || []
        this.initPermIds = rolePermRes.data || []
        // 回显已勾选的权限
        this.$nextTick(() => {
          this.allPermissions.forEach(item => {
            if (this.initPermIds.includes(item.id)) {
              this.$refs.permTable.toggleRowSelection(item, true)
            }
          })
        })
      } finally {
        this.permLoading = false
      }
    },
    handlePermSelectionChange(selection) {
      this.selectedPermIds = selection.map(item => item.id)
    },
    handleCheckAll() {
      this.allPermissions.forEach(item => {
        this.$refs.permTable.toggleRowSelection(item, true)
      })
    },
    async handleSubmitPermissions() {
      this.permSubmitLoading = true
      try {
        await assignRolePermissions(this.currentRole.id, this.selectedPermIds)
        this.$message.success('授权成功')
        this.permDialogVisible = false
      } finally {
        this.permSubmitLoading = false
      }
    }
  }
}
</script>
