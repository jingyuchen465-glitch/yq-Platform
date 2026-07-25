<template>
  <div class="page-container permission-page">
    <header class="page-head">
      <div>
        <p class="eyebrow">SYS:PERM · CONTROLLER MAP</p>
        <h1>权限管理</h1>
        <p class="page-intro">权限按后端 Controller 自动归档，展开分类查看接口级权限。</p>
      </div>
      <div class="head-metrics" aria-label="权限统计">
        <div class="head-metric">
          <b>{{ treeData.groupTotal }}</b>
          <span>个分类</span>
        </div>
        <div class="head-metric primary">
          <b>{{ treeData.total }}</b>
          <span>项权限</span>
        </div>
      </div>
    </header>

    <div class="search-bar permission-search">
      <el-form :inline="true" :model="queryParams" @submit.native.prevent>
        <el-form-item label="权限编码">
          <el-input
            v-model.trim="queryParams.permissionCode"
            placeholder="如 sys:campus"
            clearable
            @keyup.enter.native="handleSearch"
          />
        </el-form-item>
        <el-form-item label="权限名称">
          <el-input
            v-model.trim="queryParams.permissionName"
            placeholder="如 查询校区"
            clearable
            @keyup.enter.native="handleSearch"
          />
        </el-form-item>
        <el-form-item label="API 路径">
          <el-input
            v-model.trim="queryParams.apiPath"
            placeholder="如 /emp/sysCampus"
            clearable
            @keyup.enter.native="handleSearch"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" placeholder="全部" clearable class="status-select">
            <el-option label="启用" value="ACTIVE" />
            <el-option label="禁用" value="INACTIVE" />
          </el-select>
        </el-form-item>
        <el-form-item class="search-actions">
          <el-button type="primary" icon="el-icon-search" @click="handleSearch">筛选权限</el-button>
          <el-button icon="el-icon-refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <section class="tree-card" aria-labelledby="permission-directory-title">
      <div class="tree-toolbar">
        <div>
          <div class="toolbar-title-row">
            <span class="live-mark" aria-hidden="true"></span>
            <h2 id="permission-directory-title">Controller 权限目录</h2>
          </div>
          <p>
            当前显示 {{ treeData.groupTotal }} 个分类、{{ treeData.total }} 项权限，
            {{ treeData.activeTotal }} 项已启用
          </p>
        </div>
        <el-button
          v-if="treeData.groups.length"
          size="small"
          :icon="allExpanded ? 'el-icon-folder-delete' : 'el-icon-folder-opened'"
          @click="toggleAllGroups"
        >
          {{ allExpanded ? '全部收起' : '全部展开' }}
        </el-button>
      </div>

      <div v-loading="loading" class="tree-stage">
        <el-empty
          v-if="!loading && !treeData.groups.length"
          description="没有找到符合条件的权限，请调整筛选条件"
          :image-size="88"
        />

        <div v-else class="permission-tree" role="tree" aria-label="按 Controller 分类的权限树">
          <section
            v-for="group in treeData.groups"
            :key="group.groupCode"
            class="tree-group"
            role="treeitem"
            aria-level="1"
            :aria-expanded="isExpanded(group.groupCode) ? 'true' : 'false'"
          >
            <button
              type="button"
              class="group-node"
              :class="{ expanded: isExpanded(group.groupCode) }"
              @click="toggleGroup(group.groupCode)"
            >
              <span class="node-toggle" aria-hidden="true">
                <i :class="isExpanded(group.groupCode) ? 'el-icon-minus' : 'el-icon-plus'"></i>
              </span>
              <span class="group-copy">
                <span class="group-title-line">
                  <strong>{{ group.groupName }}</strong>
                  <code>{{ group.controllerName }}</code>
                </span>
                <span class="group-description">{{ group.groupDescription || '该 Controller 下声明的接口权限' }}</span>
              </span>
              <span class="group-health">
                <span class="health-copy">
                  <b>{{ group.activeTotal }}</b> / {{ group.total }} 已启用
                </span>
                <span class="health-track" aria-hidden="true">
                  <span :style="{ width: activePercent(group) + '%' }"></span>
                </span>
              </span>
              <span class="group-count">{{ group.total }} 项</span>
              <i class="el-icon-arrow-right group-arrow" aria-hidden="true"></i>
            </button>

            <div v-show="isExpanded(group.groupCode)" class="children-scroll" role="group">
              <div class="permission-grid grid-head" aria-hidden="true">
                <span>权限</span>
                <span>API 路径</span>
                <span>描述</span>
                <span>状态</span>
                <span>操作</span>
              </div>
              <article
                v-for="permission in group.permissions"
                :key="permission.id"
                class="permission-grid permission-node"
                role="treeitem"
                aria-level="2"
              >
                <div class="permission-cell permission-identity" data-label="权限">
                  <strong>{{ permission.permissionName }}</strong>
                  <code class="code-chip">{{ permission.permissionCode }}</code>
                </div>
                <div class="permission-cell api-cell" data-label="API 路径">
                  <code class="mono">{{ permission.apiPath }}</code>
                </div>
                <div class="permission-cell description-cell" data-label="描述">
                  {{ permission.description || '暂无描述' }}
                </div>
                <div class="permission-cell status-cell" data-label="状态">
                  <el-switch
                    :value="permission.status === 'ACTIVE'"
                    :aria-label="`${permission.permissionName}状态`"
                    @change="value => handleStatusChange(permission, group, value)"
                  />
                </div>
                <div class="permission-cell action-cell" data-label="操作">
                  <el-button type="text" icon="el-icon-view" @click="handleDetail(permission, group)">详情</el-button>
                </div>
              </article>
            </div>
          </section>
        </div>
      </div>
    </section>

    <el-dialog title="权限详情" :visible.sync="detailVisible" width="560px" custom-class="permission-detail-dialog">
      <el-descriptions v-if="detailData" :column="1" border>
        <el-descriptions-item label="所属分类">
          <span class="detail-group">{{ detailData.groupName }}</span>
          <code class="detail-controller">{{ detailData.controllerName }}</code>
        </el-descriptions-item>
        <el-descriptions-item label="权限 ID">{{ detailData.id }}</el-descriptions-item>
        <el-descriptions-item label="权限编码"><code class="code-chip">{{ detailData.permissionCode }}</code></el-descriptions-item>
        <el-descriptions-item label="权限名称">{{ detailData.permissionName }}</el-descriptions-item>
        <el-descriptions-item label="API 路径"><code class="mono detail-path">{{ detailData.apiPath }}</code></el-descriptions-item>
        <el-descriptions-item label="描述">{{ detailData.description || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="detailData.status === 'ACTIVE' ? 'success' : 'info'" size="small">
            {{ detailData.status === 'ACTIVE' ? '启用' : '禁用' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ detailData.createdAt }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ detailData.updatedAt }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script>
import { getPermission, listPermissionTree, updatePermissionStatus } from '@/api/permission'

const emptyTree = () => ({
  total: 0,
  activeTotal: 0,
  groupTotal: 0,
  groups: []
})

const emptyQuery = () => ({
  permissionCode: '',
  permissionName: '',
  apiPath: '',
  status: ''
})

export default {
  name: 'PermissionManage',
  data() {
    return {
      loading: false,
      treeData: emptyTree(),
      queryParams: emptyQuery(),
      expandedGroupCodes: [],
      detailVisible: false,
      detailData: null
    }
  },
  computed: {
    allExpanded() {
      return this.treeData.groups.length > 0 && this.treeData.groups.every(group =>
        this.expandedGroupCodes.includes(group.groupCode)
      )
    },
    hasFilters() {
      return Object.values(this.queryParams).some(value => value !== '')
    }
  },
  created() {
    this.fetchTree()
  },
  methods: {
    async fetchTree() {
      this.loading = true
      try {
        const res = await listPermissionTree(this.queryParams)
        this.treeData = { ...emptyTree(), ...(res.data || {}) }
        const availableCodes = this.treeData.groups.map(group => group.groupCode)
        this.expandedGroupCodes = this.hasFilters
          ? availableCodes
          : this.expandedGroupCodes.filter(code => availableCodes.includes(code))
      } catch (e) {
        this.treeData = emptyTree()
      } finally {
        this.loading = false
      }
    },
    handleSearch() {
      this.fetchTree()
    },
    handleReset() {
      this.queryParams = emptyQuery()
      this.expandedGroupCodes = []
      this.fetchTree()
    },
    isExpanded(groupCode) {
      return this.expandedGroupCodes.includes(groupCode)
    },
    toggleGroup(groupCode) {
      this.expandedGroupCodes = this.isExpanded(groupCode)
        ? this.expandedGroupCodes.filter(code => code !== groupCode)
        : [...this.expandedGroupCodes, groupCode]
    },
    toggleAllGroups() {
      this.expandedGroupCodes = this.allExpanded
        ? []
        : this.treeData.groups.map(group => group.groupCode)
    },
    activePercent(group) {
      return group.total ? Math.round((group.activeTotal / group.total) * 100) : 0
    },
    async handleDetail(permission, group) {
      try {
        const res = await getPermission(permission.id)
        this.detailData = {
          ...res.data,
          groupName: group.groupName,
          controllerName: group.controllerName
        }
        this.detailVisible = true
      } catch (e) {
        // 错误已由请求拦截器统一处理
      }
    },
    async handleStatusChange(permission, group, value) {
      const newStatus = value ? 'ACTIVE' : 'INACTIVE'
      const action = value ? '启用' : '禁用'
      try {
        await this.$confirm(`确定要${action}接口「${permission.permissionName}」吗？`, '更新权限状态', {
          confirmButtonText: `确认${action}`,
          cancelButtonText: '取消',
          type: 'warning'
        })
        await updatePermissionStatus(permission.id, newStatus)
        permission.status = newStatus
        group.activeTotal += value ? 1 : -1
        this.treeData.activeTotal += value ? 1 : -1
        this.$message.success(`已${action}「${permission.permissionName}」`)
        if (this.queryParams.status) {
          await this.fetchTree()
        }
      } catch (e) {
        this.$nextTick(() => this.$forceUpdate())
      }
    }
  }
}
</script>

<style scoped>
.permission-page {
  --tree-rail: #b7d4c8;
}

.page-intro {
  margin-top: 7px;
  color: var(--ink-3);
  font-size: 13px;
  line-height: 1.6;
}

.head-metrics {
  display: flex;
  align-items: stretch;
  gap: 1px;
  overflow: hidden;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: var(--line);
}

.head-metric {
  min-width: 92px;
  padding: 10px 16px 9px;
  text-align: right;
  background: var(--card);
}

.head-metric b {
  display: block;
  color: var(--ink-2);
  font-family: var(--font-mono);
  font-size: 22px;
  line-height: 1;
}

.head-metric.primary b {
  color: var(--jade-deep);
}

.head-metric span {
  display: block;
  margin-top: 6px;
  color: var(--ink-3);
  font-size: 11px;
}

.permission-search ::v-deep .el-form {
  display: flex;
  align-items: flex-end;
  flex-wrap: wrap;
  gap: 0 6px;
}

.permission-search ::v-deep .el-form-item {
  margin-right: 8px;
}

.permission-search ::v-deep .el-input {
  width: 210px;
}

.permission-search ::v-deep .status-select {
  width: 112px;
}

.permission-search ::v-deep .status-select .el-input {
  width: 112px;
}

.search-actions {
  margin-left: auto;
}

.tree-card {
  flex: 1;
  overflow: hidden;
  background: var(--card);
  border: 1px solid var(--line-soft);
  border-radius: var(--radius);
  box-shadow: var(--shadow-card);
}

.tree-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 17px 20px;
  border-bottom: 1px solid var(--line);
  background: #fbfcfa;
}

.toolbar-title-row {
  display: flex;
  align-items: center;
  gap: 9px;
}

.toolbar-title-row h2 {
  color: var(--ink);
  font-family: var(--font-display);
  font-size: 15px;
  font-weight: 750;
}

.live-mark {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--jade);
  box-shadow: 0 0 0 4px var(--jade-soft);
}

.tree-toolbar p {
  margin-top: 5px;
  color: var(--ink-3);
  font-size: 12px;
}

.tree-stage {
  min-height: 180px;
}

.permission-tree {
  position: relative;
  padding: 8px 0 16px;
}

.permission-tree::before {
  position: absolute;
  top: 27px;
  bottom: 35px;
  left: 35px;
  width: 1px;
  background: var(--tree-rail);
  content: '';
}

.tree-group {
  position: relative;
}

.group-node {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: 32px minmax(280px, 1fr) 150px 58px 18px;
  align-items: center;
  gap: 16px;
  width: 100%;
  padding: 15px 20px;
  color: inherit;
  font-family: var(--font-body);
  text-align: left;
  border: 0;
  border-bottom: 1px solid var(--line-soft);
  background: var(--card);
  cursor: pointer;
  transition: background-color .16s ease;
}

.group-node:hover,
.group-node.expanded {
  background: #f5f9f5;
}

.group-node:focus-visible {
  outline: 2px solid var(--jade);
  outline-offset: -3px;
}

.node-toggle {
  display: grid;
  place-items: center;
  width: 32px;
  height: 32px;
  color: var(--jade-deep);
  border: 1px solid #bcd8cc;
  border-radius: 7px;
  background: var(--jade-soft);
  font-size: 12px;
}

.group-copy {
  min-width: 0;
}

.group-title-line {
  display: flex;
  align-items: center;
  gap: 10px;
}

.group-title-line strong {
  color: var(--ink);
  font-size: 14px;
  font-weight: 700;
}

.group-title-line code,
.detail-controller {
  overflow: hidden;
  color: var(--brass-ink);
  font-family: var(--font-mono);
  font-size: 10.5px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.group-description {
  display: block;
  overflow: hidden;
  margin-top: 5px;
  color: var(--ink-3);
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.group-health {
  color: var(--ink-3);
  font-family: var(--font-mono);
  font-size: 10.5px;
}

.health-copy b {
  color: var(--jade-deep);
  font-size: 11px;
}

.health-track {
  display: block;
  overflow: hidden;
  width: 100%;
  height: 3px;
  margin-top: 7px;
  border-radius: 2px;
  background: #e4e9e2;
}

.health-track span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: var(--jade);
  transition: width .2s ease;
}

.group-count {
  color: var(--ink-2);
  font-family: var(--font-mono);
  font-size: 11px;
  text-align: right;
}

.group-arrow {
  color: var(--ink-3);
  font-size: 12px;
  transition: transform .18s ease;
}

.group-node.expanded .group-arrow {
  transform: rotate(90deg);
}

.children-scroll {
  overflow-x: auto;
  padding: 0 20px 10px 67px;
  background: #fbfcfa;
  border-bottom: 1px solid var(--line);
}

.permission-grid {
  display: grid;
  grid-template-columns: minmax(210px, 1.2fr) minmax(220px, 1.35fr) minmax(190px, 1fr) 76px 68px;
  align-items: center;
  min-width: 900px;
}

.grid-head {
  padding: 10px 13px 8px;
  color: var(--ink-3);
  font-size: 10.5px;
  font-weight: 650;
  letter-spacing: .08em;
}

.permission-node {
  position: relative;
  min-height: 72px;
  border: 1px solid var(--line-soft);
  border-bottom: 0;
  background: var(--card);
}

.permission-node:last-child {
  border-bottom: 1px solid var(--line-soft);
  border-radius: 0 0 6px 6px;
}

.permission-node::before {
  position: absolute;
  top: 50%;
  left: -32px;
  width: 31px;
  height: 1px;
  background: var(--tree-rail);
  content: '';
}

.permission-cell {
  min-width: 0;
  padding: 12px 13px;
  color: var(--ink-2);
  font-size: 12.5px;
  line-height: 1.55;
}

.permission-cell + .permission-cell {
  border-left: 1px solid var(--line-soft);
}

.permission-cell::before {
  display: none;
}

.permission-identity {
  display: flex;
  align-items: flex-start;
  flex-direction: column;
  gap: 7px;
}

.permission-identity strong {
  color: var(--ink);
  font-size: 13.5px;
  font-weight: 650;
}

.permission-identity .code-chip {
  overflow: hidden;
  max-width: 100%;
  text-overflow: ellipsis;
}

.api-cell .mono {
  overflow-wrap: anywhere;
}

.description-cell {
  color: var(--ink-3);
}

.status-cell,
.action-cell {
  text-align: center;
}

.detail-group {
  margin-right: 10px;
  color: var(--ink);
  font-weight: 650;
}

.detail-path {
  overflow-wrap: anywhere;
}

@media (max-width: 1100px) {
  .search-actions {
    margin-left: 0;
  }

  .group-node {
    grid-template-columns: 32px minmax(240px, 1fr) 130px 52px 18px;
  }
}

@media (max-width: 760px) {
  .permission-page {
    padding: 14px;
  }

  .page-head {
    align-items: flex-start;
    flex-direction: column;
  }

  .head-metrics {
    width: 100%;
  }

  .head-metric {
    flex: 1;
    text-align: left;
  }

  .permission-search ::v-deep .el-form,
  .permission-search ::v-deep .el-form-item,
  .permission-search ::v-deep .el-form-item__content,
  .permission-search ::v-deep .el-input,
  .permission-search ::v-deep .status-select,
  .permission-search ::v-deep .status-select .el-input {
    width: 100%;
  }

  .tree-toolbar {
    align-items: flex-start;
  }

  .tree-toolbar p {
    max-width: 230px;
  }

  .permission-tree::before {
    left: 27px;
  }

  .group-node {
    grid-template-columns: 28px minmax(0, 1fr) 18px;
    gap: 10px;
    padding: 14px;
  }

  .node-toggle {
    width: 28px;
    height: 28px;
  }

  .group-title-line {
    align-items: flex-start;
    flex-direction: column;
    gap: 3px;
  }

  .group-health,
  .group-count {
    display: none;
  }

  .children-scroll {
    overflow: visible;
    padding: 0 12px 12px 47px;
  }

  .grid-head {
    display: none;
  }

  .permission-grid {
    display: block;
    min-width: 0;
  }

  .permission-node {
    margin-top: 9px;
    padding: 8px 12px;
    border-bottom: 1px solid var(--line-soft);
    border-radius: 6px;
  }

  .permission-node::before {
    left: -20px;
    width: 19px;
  }

  .permission-cell {
    padding: 8px 0;
  }

  .permission-cell + .permission-cell {
    border-top: 1px dashed var(--line-soft);
    border-left: 0;
  }

  .permission-cell::before {
    display: block;
    margin-bottom: 4px;
    color: var(--ink-3);
    font-size: 10px;
    font-weight: 650;
    letter-spacing: .08em;
    content: attr(data-label);
  }

  .status-cell,
  .action-cell {
    text-align: left;
  }

  .permission-page ::v-deep .permission-detail-dialog {
    width: calc(100% - 28px) !important;
  }
}
</style>
