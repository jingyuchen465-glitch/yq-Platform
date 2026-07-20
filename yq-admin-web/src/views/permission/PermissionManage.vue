<template>
  <div class="page-container">
    <!-- 页头标识 -->
    <header class="page-head">
      <div>
        <p class="eyebrow">SYS:PERM · API ACL</p>
        <h1>权限管理</h1>
      </div>
      <div class="head-stat">
        <b>{{ total }}</b>
        <span>项权限</span>
      </div>
    </header>

    <!-- 搜索区域 -->
    <div class="search-bar">
      <el-form :inline="true" :model="queryParams" @submit.native.prevent>
        <el-form-item label="权限编码">
          <el-input v-model="queryParams.permissionCode" placeholder="请输入权限编码" clearable @keyup.enter.native="handleSearch" />
        </el-form-item>
        <el-form-item label="权限名称">
          <el-input v-model="queryParams.permissionName" placeholder="请输入权限名称" clearable @keyup.enter.native="handleSearch" />
        </el-form-item>
        <el-form-item label="API路径">
          <el-input v-model="queryParams.apiPath" placeholder="请输入API路径" clearable @keyup.enter.native="handleSearch" />
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
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column label="ID" width="70" align="center">
          <template slot-scope="{ row }"><code class="mono">{{ row.id }}</code></template>
        </el-table-column>
        <el-table-column label="权限编码" min-width="180" show-overflow-tooltip>
          <template slot-scope="{ row }"><code class="code-chip">{{ row.permissionCode }}</code></template>
        </el-table-column>
        <el-table-column prop="permissionName" label="权限名称" min-width="130" show-overflow-tooltip />
        <el-table-column label="API路径" min-width="220" show-overflow-tooltip>
          <template slot-scope="{ row }"><code class="mono">{{ row.apiPath }}</code></template>
        </el-table-column>
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
        <el-table-column label="操作" width="90" align="center" fixed="right">
          <template slot-scope="{ row }">
            <el-button type="text" icon="el-icon-view" @click="handleDetail(row)">详情</el-button>
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

    <!-- 详情弹窗 -->
    <el-dialog title="权限详情" :visible.sync="detailVisible" width="500px">
      <el-descriptions :column="1" border v-if="detailData">
        <el-descriptions-item label="权限ID">{{ detailData.id }}</el-descriptions-item>
        <el-descriptions-item label="权限编码">{{ detailData.permissionCode }}</el-descriptions-item>
        <el-descriptions-item label="权限名称">{{ detailData.permissionName }}</el-descriptions-item>
        <el-descriptions-item label="API路径">{{ detailData.apiPath }}</el-descriptions-item>
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
import { pagePermissions, getPermission } from '@/api/permission'

export default {
  name: 'PermissionManage',
  data() {
    return {
      loading: false,
      tableData: [],
      total: 0,
      queryParams: {
        current: 1,
        size: 10,
        permissionCode: '',
        permissionName: '',
        apiPath: '',
        status: ''
      },
      detailVisible: false,
      detailData: null
    }
  },
  created() {
    this.fetchData()
  },
  methods: {
    async fetchData() {
      this.loading = true
      try {
        const res = await pagePermissions(this.queryParams)
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
      this.queryParams = { current: 1, size: 10, permissionCode: '', permissionName: '', apiPath: '', status: '' }
      this.fetchData()
    },
    async handleDetail(row) {
      const res = await getPermission(row.id)
      this.detailData = res.data
      this.detailVisible = true
    }
  }
}
</script>
