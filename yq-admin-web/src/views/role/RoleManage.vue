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
    <el-dialog
      title="角色授权"
      :visible.sync="permDialogVisible"
      width="780px"
      custom-class="role-permission-dialog"
      @closed="resetPermissionDialog"
    >
      <div v-loading="permLoading" class="permission-assignment">
        <div class="assignment-brief">
          <span class="role-key" aria-hidden="true"><i class="el-icon-key"></i></span>
          <div class="role-identity">
            <span>正在为角色分配权限</span>
            <strong>{{ currentRole.roleName }}</strong>
            <code>{{ currentRole.roleCode }}</code>
          </div>
          <div class="selection-total" aria-live="polite">
            <b>{{ selectedPermIds.length }}</b>
            <span>/ {{ permissionTotal }} 项已选</span>
          </div>
        </div>

        <div class="assignment-tools">
          <el-input
            v-model.trim="permKeyword"
            size="small"
            clearable
            prefix-icon="el-icon-search"
            placeholder="搜索分类、权限编码、名称或 API"
            @input="filterPermissionTree"
          />
          <div class="batch-actions">
            <el-button size="mini" icon="el-icon-check" @click="handleCheckAll">全选</el-button>
            <el-button size="mini" icon="el-icon-close" @click="handleClearPermissions">清空</el-button>
          </div>
        </div>

        <div class="assignment-tree-shell">
          <div class="tree-guide" aria-hidden="true">
            <span>权限分类与明细</span>
            <span>勾选父节点可选择整类权限</span>
          </div>
          <el-tree
            v-if="permissionTreeData.length"
            :key="permTreeVersion"
            ref="permTree"
            class="assignment-tree"
            :data="permissionTreeData"
            node-key="nodeKey"
            show-checkbox
            :props="permissionTreeProps"
            :filter-node-method="filterPermissionNode"
            :default-expanded-keys="expandedPermissionGroups"
            @check="handlePermCheckChange"
          >
            <span slot-scope="{ data }" class="permission-tree-node" :class="`is-${data.nodeType}`">
              <template v-if="data.nodeType === 'group'">
                <span class="group-folder"><i class="el-icon-folder-opened"></i></span>
                <span class="tree-group-copy">
                  <span class="tree-group-title">
                    <strong>{{ data.label }}</strong>
                    <code>{{ data.controllerName }}</code>
                  </span>
                  <span class="tree-group-description">{{ data.description || '该 Controller 下声明的接口权限' }}</span>
                </span>
                <span class="tree-group-count">
                  <b>{{ selectedCountForGroup(data) }}</b> / {{ data.children.length }} 已选
                </span>
              </template>
              <template v-else>
                <span class="permission-name">
                  <strong>{{ data.permissionName }}</strong>
                  <code>{{ data.permissionCode }}</code>
                </span>
                <code class="permission-path">{{ data.apiPath }}</code>
                <span class="permission-status" :class="data.status === 'ACTIVE' ? 'active' : 'inactive'">
                  <i></i>{{ data.status === 'ACTIVE' ? '启用' : '禁用' }}
                </span>
              </template>
            </span>
          </el-tree>
          <el-empty
            v-else-if="!permLoading"
            description="暂无可分配权限"
            :image-size="76"
          />
        </div>
      </div>
      <div slot="footer" class="permission-dialog-footer">
        <span class="change-summary" :class="{ changed: permissionDelta.added || permissionDelta.removed }">
          <template v-if="permissionDelta.added || permissionDelta.removed">
            本次新增 {{ permissionDelta.added }} 项，移除 {{ permissionDelta.removed }} 项
          </template>
          <template v-else>授权范围未变化</template>
        </span>
        <div>
          <el-button @click="permDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="permSubmitLoading" @click="handleSubmitPermissions">保存授权</el-button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { pageRoles, addRole, updateRole, deleteRole, batchDeleteRoles, getRolePermissionIds, assignRolePermissions } from '@/api/role'
import { listPermissionTree } from '@/api/permission'

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
      permissionTreeData: [],
      permissionTotal: 0,
      selectedPermIds: [],
      initPermIds: [],
      permKeyword: '',
      permTreeVersion: 0,
      expandedPermissionGroups: [],
      permissionTreeProps: {
        children: 'children',
        label: 'label'
      }
    }
  },
  computed: {
    dialogTitle() {
      return this.isEdit ? '编辑角色' : '新增角色'
    },
    permissionDelta() {
      const initialIds = new Set(this.initPermIds)
      const selectedIds = new Set(this.selectedPermIds)
      return {
        added: this.selectedPermIds.filter(id => !initialIds.has(id)).length,
        removed: this.initPermIds.filter(id => !selectedIds.has(id)).length
      }
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
        } catch (e) {
          // 错误已由请求拦截器统一处理
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
      this.permissionTreeData = []
      this.permissionTotal = 0
      this.permKeyword = ''
      this.expandedPermissionGroups = []
      try {
        const [permRes, rolePermRes] = await Promise.all([
          listPermissionTree({}),
          getRolePermissionIds(row.id)
        ])
        const groups = (permRes.data && permRes.data.groups) || []
        this.permissionTreeData = groups.map(group => ({
          nodeKey: `group:${group.groupCode}`,
          nodeType: 'group',
          label: group.groupName,
          controllerName: group.controllerName,
          description: group.groupDescription,
          children: (group.permissions || []).map(permission => ({
            nodeKey: `permission:${permission.id}`,
            nodeType: 'permission',
            permissionId: permission.id,
            permissionCode: permission.permissionCode,
            permissionName: permission.permissionName,
            apiPath: permission.apiPath,
            status: permission.status,
            label: `${permission.permissionName} ${permission.permissionCode}`
          }))
        }))
        this.permissionTotal = this.permissionTreeData.reduce((total, group) => total + group.children.length, 0)
        this.initPermIds = rolePermRes.data || []
        this.selectedPermIds = [...this.initPermIds]
        this.permTreeVersion += 1
        // 回显已勾选的权限
        this.$nextTick(() => {
          const checkedKeys = this.initPermIds.map(id => `permission:${id}`)
          this.$refs.permTree && this.$refs.permTree.setCheckedKeys(checkedKeys)
        })
      } catch (e) {
        // 错误已由请求拦截器统一处理
      } finally {
        this.permLoading = false
      }
    },
    handlePermCheckChange(data, checkState) {
      this.selectedPermIds = checkState.checkedNodes
        .filter(node => node.nodeType === 'permission')
        .map(node => node.permissionId)
    },
    handleCheckAll() {
      const allPermissionKeys = this.permissionTreeData
        .flatMap(group => group.children)
        .map(permission => permission.nodeKey)
      this.$refs.permTree && this.$refs.permTree.setCheckedKeys(allPermissionKeys)
      this.syncSelectedPermissionIds()
    },
    handleClearPermissions() {
      this.$refs.permTree && this.$refs.permTree.setCheckedKeys([])
      this.selectedPermIds = []
    },
    syncSelectedPermissionIds() {
      if (!this.$refs.permTree) return
      this.selectedPermIds = this.$refs.permTree.getCheckedNodes(true, false)
        .filter(node => node.nodeType === 'permission')
        .map(node => node.permissionId)
    },
    selectedCountForGroup(group) {
      const selectedIds = new Set(this.selectedPermIds)
      return group.children.filter(permission => selectedIds.has(permission.permissionId)).length
    },
    filterPermissionTree(value) {
      this.$refs.permTree && this.$refs.permTree.filter(value)
    },
    filterPermissionNode(value, data) {
      if (!value) return true
      const keyword = value.toLowerCase()
      const searchableText = data.nodeType === 'group'
        ? `${data.label} ${data.controllerName} ${data.description || ''}`
        : `${data.permissionName} ${data.permissionCode} ${data.apiPath}`
      return searchableText.toLowerCase().includes(keyword)
    },
    resetPermissionDialog() {
      this.permissionTreeData = []
      this.permissionTotal = 0
      this.selectedPermIds = []
      this.initPermIds = []
      this.permKeyword = ''
      this.expandedPermissionGroups = []
    },
    async handleSubmitPermissions() {
      this.syncSelectedPermissionIds()
      this.permSubmitLoading = true
      try {
        await assignRolePermissions(this.currentRole.id, this.selectedPermIds)
        this.$message.success('授权成功')
        this.permDialogVisible = false
      } catch (e) {
        // 错误已由请求拦截器统一处理
      } finally {
        this.permSubmitLoading = false
      }
    }
  }
}
</script>

<style scoped>
.permission-assignment {
  min-height: 300px;
  color: var(--ink);
}

.assignment-brief {
  display: grid;
  grid-template-columns: 42px minmax(0, 1fr) auto;
  align-items: center;
  gap: 14px;
  padding: 17px 20px;
  border-bottom: 1px solid var(--line);
  background: #f7f9f5;
}

.role-key {
  display: grid;
  place-items: center;
  width: 42px;
  height: 42px;
  color: var(--jade-deep);
  border: 1px solid #bad7ca;
  border-radius: 9px;
  background: var(--jade-soft);
  font-size: 18px;
}

.role-identity {
  display: flex;
  align-items: baseline;
  min-width: 0;
  gap: 8px;
}

.role-identity > span {
  color: var(--ink-3);
  font-size: 12px;
}

.role-identity strong {
  color: var(--ink);
  font-size: 15px;
}

.role-identity code {
  overflow: hidden;
  color: var(--brass-ink);
  font-family: var(--font-mono);
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.selection-total {
  display: flex;
  align-items: baseline;
  gap: 5px;
  padding-left: 18px;
  border-left: 1px solid var(--line);
  color: var(--ink-3);
  font-size: 11px;
}

.selection-total b {
  color: var(--jade-deep);
  font-family: var(--font-mono);
  font-size: 21px;
  line-height: 1;
}

.assignment-tools {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding: 13px 20px;
  border-bottom: 1px solid var(--line-soft);
}

.assignment-tools ::v-deep .el-input {
  width: 330px;
}

.batch-actions {
  display: flex;
  flex: none;
}

.assignment-tree-shell {
  margin: 0 20px 20px;
  overflow: hidden;
  border: 1px solid var(--line);
  border-radius: 7px;
  background: var(--card);
}

.tree-guide {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 9px 14px;
  color: var(--ink-3);
  border-bottom: 1px solid var(--line);
  background: #f7f9f5;
  font-size: 10.5px;
  font-weight: 600;
  letter-spacing: .06em;
}

.assignment-tree {
  max-height: 390px;
  overflow-y: auto;
  background: transparent;
}

.assignment-tree ::v-deep .el-tree-node__content {
  min-height: 56px;
  height: auto;
  padding-right: 14px;
  border-bottom: 1px solid var(--line-soft);
  background: var(--card);
  transition: background-color .15s ease;
}

.assignment-tree ::v-deep > .el-tree-node > .el-tree-node__content {
  min-height: 64px;
  background: #fbfcfa;
}

.assignment-tree ::v-deep .el-tree-node__content:hover,
.assignment-tree ::v-deep .el-tree-node:focus > .el-tree-node__content {
  background: var(--jade-soft);
}

.assignment-tree ::v-deep .el-tree-node__children {
  position: relative;
  background: #fbfcfa;
}

.assignment-tree ::v-deep .el-tree-node__children::before {
  position: absolute;
  top: 0;
  bottom: 0;
  left: 25px;
  width: 1px;
  background: #bed7cc;
  content: '';
}

.assignment-tree ::v-deep .el-tree-node__expand-icon {
  color: var(--jade-deep);
  font-size: 13px;
}

.assignment-tree ::v-deep .el-tree-node__expand-icon.is-leaf {
  color: transparent;
}

.permission-tree-node {
  flex: 1;
  min-width: 0;
  margin-left: 8px;
  font-family: var(--font-body);
}

.permission-tree-node.is-group {
  display: grid;
  grid-template-columns: 30px minmax(0, 1fr) 82px;
  align-items: center;
  gap: 10px;
}

.group-folder {
  display: grid;
  place-items: center;
  width: 30px;
  height: 30px;
  color: var(--jade-deep);
  border-radius: 6px;
  background: var(--jade-soft);
  font-size: 14px;
}

.tree-group-copy,
.permission-name {
  min-width: 0;
}

.tree-group-title {
  display: flex;
  align-items: center;
  min-width: 0;
  gap: 9px;
}

.tree-group-title strong {
  color: var(--ink);
  font-size: 13.5px;
  font-weight: 700;
}

.tree-group-title code {
  overflow: hidden;
  color: var(--brass-ink);
  font-family: var(--font-mono);
  font-size: 10px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tree-group-description {
  display: block;
  overflow: hidden;
  margin-top: 4px;
  color: var(--ink-3);
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tree-group-count {
  color: var(--ink-3);
  font-family: var(--font-mono);
  font-size: 10px;
  text-align: right;
  white-space: nowrap;
}

.tree-group-count b {
  color: var(--jade-deep);
  font-size: 11px;
}

.permission-tree-node.is-permission {
  display: grid;
  grid-template-columns: minmax(190px, 1.15fr) minmax(210px, 1.35fr) 55px;
  align-items: center;
  gap: 14px;
  min-width: 0;
  padding: 8px 0;
}

.permission-name {
  display: flex;
  align-items: flex-start;
  flex-direction: column;
  gap: 3px;
}

.permission-name strong {
  color: var(--ink);
  font-size: 12.5px;
  font-weight: 650;
  line-height: 1.35;
}

.permission-name code,
.permission-path {
  overflow: hidden;
  font-family: var(--font-mono);
  font-size: 10.5px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.permission-name code {
  color: var(--jade-deep);
}

.permission-path {
  color: var(--ink-2);
}

.permission-status {
  display: inline-flex;
  align-items: center;
  justify-content: flex-end;
  gap: 6px;
  color: var(--ink-3);
  font-size: 10.5px;
}

.permission-status i {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #b9c2b9;
}

.permission-status.active {
  color: var(--jade-deep);
}

.permission-status.active i {
  background: var(--jade);
  box-shadow: 0 0 0 3px var(--jade-soft);
}

.permission-dialog-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
}

.change-summary {
  color: var(--ink-3);
  font-size: 11px;
}

.change-summary.changed {
  color: var(--brass-ink);
}

.page-container ::v-deep .role-permission-dialog .el-dialog__body {
  padding: 0;
}

.page-container ::v-deep .role-permission-dialog .el-dialog__footer {
  padding: 14px 20px 16px;
}

@media (max-width: 760px) {
  .page-container ::v-deep .role-permission-dialog {
    width: calc(100% - 24px) !important;
    margin-top: 4vh !important;
  }

  .assignment-brief {
    grid-template-columns: 38px minmax(0, 1fr);
    padding: 14px;
  }

  .role-key {
    width: 38px;
    height: 38px;
  }

  .role-identity {
    align-items: flex-start;
    flex-direction: column;
    gap: 3px;
  }

  .selection-total {
    grid-column: 1 / -1;
    padding: 10px 0 0;
    border-top: 1px solid var(--line);
    border-left: 0;
  }

  .assignment-tools {
    align-items: stretch;
    flex-direction: column;
    padding: 12px 14px;
  }

  .assignment-tools ::v-deep .el-input {
    width: 100%;
  }

  .assignment-tree-shell {
    margin: 0 14px 14px;
  }

  .tree-guide span:last-child,
  .group-folder,
  .tree-group-description,
  .tree-group-title code {
    display: none;
  }

  .permission-tree-node.is-group {
    grid-template-columns: minmax(0, 1fr) 62px;
  }

  .permission-tree-node.is-permission {
    display: flex;
    align-items: stretch;
    flex-direction: column;
    gap: 5px;
    padding: 9px 0;
  }

  .permission-path {
    max-width: 100%;
  }

  .permission-status {
    justify-content: flex-start;
  }

  .permission-dialog-footer {
    align-items: stretch;
    flex-direction: column;
  }

  .permission-dialog-footer > div {
    display: flex;
    justify-content: flex-end;
  }
}
</style>
