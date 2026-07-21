<template>
  <div class="page-container">
    <!-- 页头标识 -->
    <header class="page-head">
      <div>
        <p class="eyebrow">SYS:USER · EMPLOYEE</p>
        <h1>用户管理</h1>
      </div>
      <div class="head-stat">
        <b>{{ total }}</b>
        <span>名员工</span>
      </div>
    </header>

    <!-- 搜索区域 -->
    <div class="search-bar">
      <el-form :inline="true" :model="queryParams" @submit.native.prevent>
        <el-form-item label="用户名">
          <el-input v-model="queryParams.username" placeholder="请输入用户名" clearable @keyup.enter.native="handleSearch" />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="queryParams.nickname" placeholder="请输入昵称" clearable @keyup.enter.native="handleSearch" />
        </el-form-item>
        <el-form-item label="真实姓名">
          <el-input v-model="queryParams.realName" placeholder="请输入真实姓名" clearable @keyup.enter.native="handleSearch" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select
            v-model="queryParams.roleId"
            placeholder="请选择角色"
            clearable
            filterable
            :loading="roleOptionsLoading"
            style="width: 180px;"
            @change="handleSearch"
          >
            <el-option
              v-for="role in roleOptions"
              :key="role.id"
              :label="`${role.roleName}（${role.roleCode}）`"
              :value="role.id"
            />
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
        <el-button type="primary" icon="el-icon-plus" @click="handleAdd">新增用户</el-button>
        <el-button type="danger" icon="el-icon-delete" :disabled="selectedIds.length === 0" @click="handleBatchDelete">批量删除</el-button>
        <span v-if="selectedIds.length" style="margin-left: 10px; color: #999; font-size: 13px;">已选 {{ selectedIds.length }} 项</span>
      </div>

      <el-table :data="tableData" v-loading="loading" stripe @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="50" align="center" />
        <el-table-column label="ID" width="70" align="center">
          <template slot-scope="{ row }"><code class="mono">{{ row.id }}</code></template>
        </el-table-column>
        <el-table-column prop="username" label="用户名" min-width="110" show-overflow-tooltip />
        <el-table-column prop="nickname" label="昵称" min-width="100" show-overflow-tooltip />
        <el-table-column label="角色" min-width="180">
          <template slot-scope="{ row }">
            <template v-if="row.roles && row.roles.length">
              <el-tag
                v-for="(role, index) in row.roles"
                :key="role.id"
                :type="roleTagTypes[index % roleTagTypes.length]"
                size="small"
                style="margin-right: 6px; margin-bottom: 4px;"
              >
                {{ role.roleName }}
              </el-tag>
            </template>
            <el-tag v-else type="info" size="small">未分配</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="realName" label="真实姓名" min-width="100" show-overflow-tooltip />
        <el-table-column label="手机号" min-width="120" show-overflow-tooltip>
          <template slot-scope="{ row }"><code class="mono">{{ row.phone || '—' }}</code></template>
        </el-table-column>
        <el-table-column prop="email" label="邮箱" min-width="160" show-overflow-tooltip />
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
            <el-button type="text" icon="el-icon-s-custom" @click="handleAssignRole(row)">分配角色</el-button>
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
      <el-form ref="userForm" :model="form" :rules="formRules" label-width="90px" class="dialog-form">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入登录用户名" :disabled="isEdit" />
        </el-form-item>
        <el-form-item v-if="!isEdit" label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入登录密码（至少6位）" show-password />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="form.nickname" placeholder="请输入用户昵称" />
        </el-form-item>
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="form.realName" placeholder="请输入真实姓名" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="飞书ID" prop="unionId">
          <el-input v-model="form.unionId" placeholder="请输入飞书 union_id" />
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

    <!-- 分配角色弹窗 -->
    <el-dialog title="分配角色" :visible.sync="roleDialogVisible" width="460px">
      <div v-loading="roleLoading">
        <p style="margin-bottom: 12px; color: #666;">
          为用户 <b>{{ currentUser.nickname || currentUser.username }}</b> 分配角色：
        </p>
        <el-checkbox-group v-model="selectedRoleIds">
          <el-checkbox
            v-for="role in allRoles"
            :key="role.id"
            :label="role.id"
            style="display: block; margin-left: 0; margin-bottom: 8px;"
          >
            {{ role.roleName }}（{{ role.roleCode }}）
          </el-checkbox>
        </el-checkbox-group>
        <el-empty v-if="allRoles.length === 0" description="暂无可用角色" :image-size="60" />
      </div>
      <div slot="footer">
        <el-button @click="roleDialogVisible = false">取 消</el-button>
        <el-button type="primary" :loading="roleSubmitLoading" @click="handleSubmitRoles">确 定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { pageUsers, addUser, updateUser, deleteUser, batchDeleteUsers, getUserRoleIds, assignUserRoles } from '@/api/user'
import { pageRoles, listRoleOptions } from '@/api/role'

export default {
  name: 'UserManage',
  data() {
    return {
      loading: false,
      tableData: [],
      total: 0,
      roleTagTypes: ['', 'success', 'warning', 'danger'],
      selectedIds: [],
      queryParams: {
        current: 1,
        size: 10,
        username: '',
        nickname: '',
        realName: '',
        roleId: ''
      },
      roleOptionsLoading: false,
      roleOptions: [],
      // 弹窗相关
      dialogVisible: false,
      isEdit: false,
      submitLoading: false,
      editId: null,
      form: {
        username: '',
        password: '',
        nickname: '',
        realName: '',
        phone: '',
        email: '',
        unionId: '',
        status: 'ACTIVE'
      },
      rules: {
        username: [
          { required: true, message: '请输入登录用户名', trigger: 'blur' },
          { max: 64, message: '长度不能超过64个字符', trigger: 'blur' }
        ],
        password: [
          { required: true, message: '请输入登录密码', trigger: 'blur' },
          { min: 6, max: 255, message: '密码长度必须在6到255个字符之间', trigger: 'blur' }
        ],
        nickname: [
          { required: true, message: '请输入用户昵称', trigger: 'blur' },
          { max: 64, message: '长度不能超过64个字符', trigger: 'blur' }
        ],
        phone: [
          { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
        ],
        email: [
          { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
        ],
        status: [
          { required: true, message: '请选择状态', trigger: 'change' }
        ]
      },
      // 分配角色相关
      roleDialogVisible: false,
      roleLoading: false,
      roleSubmitLoading: false,
      currentUser: {},
      allRoles: [],
      selectedRoleIds: []
    }
  },
  computed: {
    dialogTitle() {
      return this.isEdit ? '编辑用户' : '新增用户'
    },
    // 编辑模式下后端不接受密码字段，移除密码校验
    formRules() {
      if (this.isEdit) {
        const { password, ...rest } = this.rules
        return rest
      }
      return this.rules
    }
  },
  created() {
    this.fetchRoleOptions()
    this.fetchData()
  },
  methods: {
    async fetchRoleOptions() {
      this.roleOptionsLoading = true
      try {
        const res = await listRoleOptions()
        this.roleOptions = res.data || []
      } finally {
        this.roleOptionsLoading = false
      }
    },
    // 查询列表
    async fetchData() {
      this.loading = true
      try {
        const res = await pageUsers(this.queryParams)
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
      this.queryParams = { current: 1, size: 10, username: '', nickname: '', realName: '', roleId: '' }
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
        username: row.username,
        nickname: row.nickname,
        realName: row.realName || '',
        phone: row.phone || '',
        email: row.email || '',
        unionId: row.unionId || '',
        status: row.status
      }
      this.dialogVisible = true
    },
    // 提交表单
    handleSubmit() {
      this.$refs.userForm.validate(async valid => {
        if (!valid) return
        this.submitLoading = true
        try {
          if (this.isEdit) {
            // 后端 SysUserUpdateReq 不含 password，编辑时剔除该字段
            const { password, ...updateData } = this.form
            await updateUser(this.editId, updateData)
            this.$message.success('修改成功')
          } else {
            await addUser(this.form)
            this.$message.success('添加成功')
          }
          this.dialogVisible = false
          this.fetchData()
        } finally {
          this.submitLoading = false
        }
      })
    },
    resetForm() {
      this.form = {
        username: '', password: '', nickname: '', realName: '',
        phone: '', email: '', unionId: '', status: 'ACTIVE'
      }
      this.$nextTick(() => {
        this.$refs.userForm && this.$refs.userForm.clearValidate()
      })
    },
    // 删除
    handleDelete(row) {
      this.$confirm(`确定要删除用户「${row.nickname || row.username}」吗？删除后将同时清理其角色关系。`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        await deleteUser(row.id)
        this.$message.success('删除成功')
        this.fetchData()
      }).catch(() => {})
    },
    // 批量删除
    handleBatchDelete() {
      this.$confirm(`确定要删除选中的 ${this.selectedIds.length} 名用户吗？删除后将同时清理其角色关系。`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        await batchDeleteUsers(this.selectedIds)
        this.$message.success('批量删除成功')
        this.selectedIds = []
        this.fetchData()
      }).catch(() => {})
    },
    handleSelectionChange(selection) {
      this.selectedIds = selection.map(item => item.id)
    },
    // 分配角色
    async handleAssignRole(row) {
      this.currentUser = row
      this.roleDialogVisible = true
      this.roleLoading = true
      try {
        // 并行加载全部角色和用户已有角色
        const [rolesRes, userRolesRes] = await Promise.all([
          pageRoles({ current: 1, size: 100 }),
          getUserRoleIds(row.id)
        ])
        this.allRoles = rolesRes.data.records || []
        this.selectedRoleIds = userRolesRes.data || []
      } finally {
        this.roleLoading = false
      }
    },
    async handleSubmitRoles() {
      this.roleSubmitLoading = true
      try {
        await assignUserRoles(this.currentUser.id, this.selectedRoleIds)
        this.$message.success('角色分配成功')
        this.roleDialogVisible = false
      } finally {
        this.roleSubmitLoading = false
      }
    }
  }
}
</script>
