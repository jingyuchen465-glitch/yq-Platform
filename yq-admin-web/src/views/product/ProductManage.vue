<template>
  <div class="page-container product-page">
    <header class="page-head product-head">
      <div>
        <p class="eyebrow">MARKET:PRODUCT · COURSE BUNDLE</p>
        <h1>产品管理</h1>
        <p class="head-copy">把课程组合成清晰、可定价、可维护的销售产品。</p>
      </div>
      <div class="head-stat">
        <b>{{ total }}</b>
        <span>个在售组合</span>
      </div>
    </header>

    <section class="search-bar product-search">
      <el-form :inline="true" :model="queryParams" @submit.native.prevent>
        <el-form-item label="组合检索">
          <el-input
            v-model="queryParams.keyword"
            class="keyword-input"
            prefix-icon="el-icon-search"
            placeholder="输入产品名称或课程名称"
            clearable
            @keyup.enter.native="handleSearch"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" @click="handleSearch">查询</el-button>
          <el-button icon="el-icon-refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
      <div class="search-note">
        <i class="el-icon-collection-tag"></i>
        支持从产品标题与课程快照中同时匹配
      </div>
    </section>

    <section class="table-card product-card">
      <div class="table-toolbar">
        <div>
          <p class="toolbar-kicker">CATALOG</p>
          <h2>课程产品目录</h2>
        </div>
        <el-button type="primary" icon="el-icon-plus" @click="handleAdd">新增产品</el-button>
      </div>

      <el-table :data="tableData" v-loading="loading" stripe class="product-table">
        <el-table-column label="产品" min-width="210">
          <template slot-scope="{ row }">
            <div class="product-identity">
              <span class="product-index">P-{{ String(row.id).padStart(4, '0') }}</span>
              <strong>{{ row.productName }}</strong>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="课程组合" min-width="300">
          <template slot-scope="{ row }">
            <div v-if="splitCourseNames(row.courseName).length" class="course-tags">
              <span
                v-for="courseName in splitCourseNames(row.courseName)"
                :key="courseName"
                class="course-chip"
              >
                {{ courseName }}
              </span>
            </div>
            <span v-else class="empty-copy">暂无关联课程</span>
          </template>
        </el-table-column>
        <el-table-column label="价格" width="190" align="right">
          <template slot-scope="{ row }">
            <div class="price-stack">
              <span class="current-price">¥ {{ formatMoney(row.newPrice) }}</span>
              <span class="old-price">¥ {{ formatMoney(row.oldPrice) }}</span>
              <span v-if="discountLabel(row)" class="discount-stamp">{{ discountLabel(row) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="更新时间" width="174" align="center">
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
          :page-sizes="[10, 20, 50]"
          @size-change="fetchData"
          @current-change="fetchData"
        />
      </div>
    </section>

    <el-dialog
      :title="dialogTitle"
      :visible.sync="dialogVisible"
      width="760px"
      custom-class="product-dialog"
      :close-on-click-modal="false"
      @closed="resetForm"
    >
      <el-form ref="productForm" :model="form" :rules="formRules" label-position="top">
        <div class="dialog-grid">
          <section class="form-panel identity-panel">
            <p class="panel-cap">01 · 产品身份</p>
            <el-form-item label="产品名称" prop="productName">
              <el-input v-model="form.productName" maxlength="128" show-word-limit placeholder="例如：Java 全栈就业班" />
            </el-form-item>

            <div class="price-grid">
              <el-form-item label="原价" prop="oldPrice">
                <el-input-number
                  v-model="form.oldPrice"
                  :min="0"
                  :max="99999999.99"
                  :precision="2"
                  :step="100"
                  controls-position="right"
                />
              </el-form-item>
              <el-form-item label="现价" prop="newPrice">
                <el-input-number
                  v-model="form.newPrice"
                  :min="0"
                  :max="99999999.99"
                  :precision="2"
                  :step="100"
                  controls-position="right"
                />
              </el-form-item>
            </div>

            <div class="price-preview">
              <span>价格预览</span>
              <strong>¥ {{ formatMoney(form.newPrice) }}</strong>
              <del>¥ {{ formatMoney(form.oldPrice) }}</del>
            </div>
          </section>

          <section class="form-panel course-panel">
            <div class="course-panel-head">
              <p class="panel-cap">02 · 课程组合</p>
              <span>{{ form.courseIds.length }} 门已选</span>
            </div>
            <el-form-item prop="courseIds" class="course-form-item">
              <el-input
                v-model="courseKeyword"
                prefix-icon="el-icon-search"
                size="small"
                clearable
                placeholder="筛选课程"
              />
              <el-checkbox-group v-model="form.courseIds" class="course-picker">
                <label
                  v-for="course in filteredCourseOptions"
                  :key="course.id"
                  class="course-option"
                  :class="{ selected: form.courseIds.includes(course.id) }"
                >
                  <el-checkbox :label="course.id">
                    <span class="course-option-name">{{ course.courseName }}</span>
                  </el-checkbox>
                  <span class="course-meta">
                    {{ course.courseDays }} 天 · {{ course.teachingMode === 'ONLINE' ? '线上' : '线下' }}
                  </span>
                </label>
              </el-checkbox-group>
              <div v-if="!optionsLoading && !filteredCourseOptions.length" class="course-empty">
                <i class="el-icon-folder-opened"></i>
                <span>没有匹配的课程</span>
              </div>
            </el-form-item>
          </section>
        </div>
      </el-form>
      <div slot="footer" class="dialog-actions">
        <span class="selection-summary">{{ selectedCourseNames }}</span>
        <div>
          <el-button @click="dialogVisible = false">取 消</el-button>
          <el-button type="primary" :loading="submitLoading" @click="handleSubmit">保存产品</el-button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  pageProduct,
  getProduct,
  getProductCourseOptions,
  addProduct,
  updateProduct,
  deleteProduct
} from '@/api/product'

export default {
  name: 'ProductManage',
  data() {
    return {
      loading: false,
      optionsLoading: false,
      submitLoading: false,
      tableData: [],
      total: 0,
      queryParams: {
        current: 1,
        size: 10,
        keyword: ''
      },
      dialogVisible: false,
      isEdit: false,
      editId: null,
      courseKeyword: '',
      courseOptions: [],
      form: this.createEmptyForm(),
      formRules: {
        productName: [
          { required: true, message: '请输入产品名称', trigger: 'blur' },
          { max: 128, message: '长度不能超过128个字符', trigger: 'blur' }
        ],
        oldPrice: [
          { required: true, message: '请输入原价', trigger: 'change' }
        ],
        newPrice: [
          { required: true, message: '请输入现价', trigger: 'change' }
        ],
        courseIds: [
          { type: 'array', required: true, min: 1, message: '请至少选择一门关联课程', trigger: 'change' }
        ]
      }
    }
  },
  computed: {
    dialogTitle() {
      return this.isEdit ? '编辑产品组合' : '创建产品组合'
    },
    filteredCourseOptions() {
      const keyword = this.courseKeyword.trim().toLowerCase()
      if (!keyword) return this.courseOptions
      return this.courseOptions.filter(course => course.courseName.toLowerCase().includes(keyword))
    },
    selectedCourseNames() {
      if (!this.form.courseIds.length) return '尚未选择课程'
      const selected = new Set(this.form.courseIds)
      const names = this.courseOptions
        .filter(course => selected.has(course.id))
        .map(course => course.courseName)
      return names.length > 3 ? `${names.slice(0, 3).join(' / ')} 等 ${names.length} 门课程` : names.join(' / ')
    }
  },
  created() {
    this.fetchData()
    this.loadCourseOptions()
  },
  methods: {
    createEmptyForm() {
      return {
        productName: '',
        oldPrice: 0,
        newPrice: 0,
        courseIds: []
      }
    },
    async fetchData() {
      this.loading = true
      try {
        const res = await pageProduct(this.queryParams)
        this.tableData = res.data.records || []
        this.total = res.data.total || 0
      } catch (e) {
        // 错误已由请求拦截器统一处理
      } finally {
        this.loading = false
      }
    },
    async loadCourseOptions() {
      this.optionsLoading = true
      try {
        const res = await getProductCourseOptions()
        this.courseOptions = res.data || []
      } catch (e) {
        this.courseOptions = []
      } finally {
        this.optionsLoading = false
      }
    },
    handleSearch() {
      this.queryParams.current = 1
      this.fetchData()
    },
    handleReset() {
      this.queryParams = { current: 1, size: 10, keyword: '' }
      this.fetchData()
    },
    handleAdd() {
      this.isEdit = false
      this.editId = null
      this.form = this.createEmptyForm()
      this.dialogVisible = true
    },
    async handleEdit(row) {
      this.isEdit = true
      this.editId = row.id
      this.dialogVisible = true
      this.submitLoading = true
      try {
        const res = await getProduct(row.id)
        const product = res.data
        this.form = {
          productName: product.productName,
          oldPrice: Number(product.oldPrice),
          newPrice: Number(product.newPrice),
          courseIds: product.courseIds || []
        }
      } catch (e) {
        this.dialogVisible = false
      } finally {
        this.submitLoading = false
      }
    },
    handleSubmit() {
      this.$refs.productForm.validate(async valid => {
        if (!valid) return
        this.submitLoading = true
        try {
          const payload = {
            productName: this.form.productName.trim(),
            oldPrice: this.form.oldPrice,
            newPrice: this.form.newPrice,
            courseIds: this.form.courseIds
          }
          if (this.isEdit) {
            await updateProduct(this.editId, payload)
            this.$message.success('产品修改成功')
          } else {
            await addProduct(payload)
            this.$message.success('产品新增成功')
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
      this.form = this.createEmptyForm()
      this.courseKeyword = ''
      this.editId = null
      this.$nextTick(() => {
        this.$refs.productForm && this.$refs.productForm.clearValidate()
      })
    },
    handleDelete(row) {
      this.$confirm(`删除产品「${row.productName}」后，其课程关联也会一并清除。确定继续吗？`, '删除产品', {
        confirmButtonText: '确认删除',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        await deleteProduct(row.id)
        this.$message.success('产品已删除')
        if (this.tableData.length === 1 && this.queryParams.current > 1) {
          this.queryParams.current -= 1
        }
        this.fetchData()
      }).catch(() => {})
    },
    splitCourseNames(courseName) {
      if (!courseName) return []
      return courseName.split('/').map(name => name.trim()).filter(Boolean)
    },
    formatMoney(value) {
      const amount = Number(value || 0)
      return amount.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
    },
    discountLabel(row) {
      const oldPrice = Number(row.oldPrice)
      const newPrice = Number(row.newPrice)
      if (oldPrice <= 0 || newPrice >= oldPrice) return ''
      const discount = Math.round((1 - newPrice / oldPrice) * 100)
      return `省 ${discount}%`
    }
  }
}
</script>

<style scoped>
.product-page {
  position: relative;
}

.product-page::before {
  content: '';
  position: absolute;
  top: 0;
  right: 26px;
  width: 150px;
  height: 6px;
  border-radius: 0 0 4px 4px;
  background: repeating-linear-gradient(90deg, var(--brass) 0 18px, transparent 18px 26px);
  opacity: .72;
}

.product-head {
  align-items: center;
}

.head-copy {
  margin-top: 8px;
  color: var(--ink-3);
  font-size: 13px;
}

.product-search {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding-bottom: 4px;
}

.product-search .el-form {
  flex: 1;
}

.keyword-input {
  width: 360px;
}

.search-note {
  display: flex;
  align-items: center;
  gap: 7px;
  padding-bottom: 14px;
  color: var(--ink-3);
  font-size: 12px;
}

.search-note i {
  color: var(--brass-ink);
}

.product-card {
  padding-top: 16px;
}

.table-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.table-toolbar h2 {
  margin-top: 2px;
  font-family: var(--font-display);
  font-size: 17px;
}

.toolbar-kicker,
.panel-cap {
  font-family: var(--font-mono);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: .18em;
  color: var(--brass-ink);
}

.product-identity {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.product-identity strong {
  font-size: 14px;
  color: var(--ink);
}

.product-index {
  width: fit-content;
  padding: 2px 7px;
  border: 1px solid var(--line);
  border-radius: 3px;
  color: var(--ink-3);
  font-family: var(--font-mono);
  font-size: 10px;
  letter-spacing: .08em;
  background: #f8faf6;
}

.course-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.course-chip {
  display: inline-flex;
  align-items: center;
  min-height: 24px;
  padding: 3px 9px;
  border-left: 2px solid var(--jade);
  border-radius: 3px;
  background: var(--jade-soft);
  color: var(--jade-deep);
  font-size: 12px;
}

.empty-copy {
  color: var(--ink-3);
  font-size: 12px;
}

.price-stack {
  position: relative;
  display: inline-grid;
  justify-items: end;
  gap: 2px;
  min-width: 118px;
  padding-right: 50px;
}

.current-price {
  color: var(--clay-deep);
  font-family: var(--font-mono);
  font-size: 16px;
  font-weight: 700;
}

.old-price {
  color: #9ca69f;
  font-family: var(--font-mono);
  font-size: 11px;
  text-decoration: line-through;
}

.discount-stamp {
  position: absolute;
  top: 5px;
  right: 0;
  padding: 3px 6px;
  border-radius: 3px;
  background: var(--clay-soft);
  color: var(--clay-deep);
  font-size: 10px;
  font-weight: 700;
}

.dialog-grid {
  display: grid;
  grid-template-columns: .9fr 1.1fr;
  gap: 18px;
}

.form-panel {
  min-width: 0;
  padding: 18px;
  border: 1px solid var(--line-soft);
  border-radius: 8px;
  background: #fbfcfa;
}

.panel-cap {
  margin-bottom: 16px;
}

.price-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.price-grid .el-input-number {
  width: 100%;
}

.price-preview {
  display: grid;
  grid-template-columns: 1fr auto;
  align-items: end;
  gap: 3px 10px;
  margin-top: 4px;
  padding: 14px;
  border-radius: 6px;
  color: #dce9df;
  background: var(--pine);
}

.price-preview span {
  grid-column: 1 / -1;
  color: #91a89c;
  font-size: 10px;
  letter-spacing: .12em;
}

.price-preview strong {
  color: #fff3d6;
  font-family: var(--font-mono);
  font-size: 22px;
}

.price-preview del {
  color: #91a89c;
  font-family: var(--font-mono);
  font-size: 11px;
}

.course-panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.course-panel-head span {
  color: var(--jade-deep);
  font-family: var(--font-mono);
  font-size: 11px;
  font-weight: 700;
}

.course-form-item {
  margin-bottom: 0;
}

.course-picker {
  display: flex;
  flex-direction: column;
  gap: 7px;
  max-height: 268px;
  margin-top: 10px;
  padding-right: 4px;
  overflow-y: auto;
}

.course-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  min-height: 46px;
  padding: 9px 10px;
  border: 1px solid var(--line-soft);
  border-radius: 6px;
  background: #fff;
  cursor: pointer;
  transition: border-color .16s ease, background-color .16s ease, transform .16s ease;
}

.course-option:hover {
  border-color: #a9c8ba;
  transform: translateX(2px);
}

.course-option.selected {
  border-color: var(--jade);
  background: var(--jade-soft);
}

.course-option-name {
  display: inline-block;
  max-width: 150px;
  overflow: hidden;
  text-overflow: ellipsis;
  vertical-align: middle;
  white-space: nowrap;
}

.course-meta {
  flex: none;
  color: var(--ink-3);
  font-family: var(--font-mono);
  font-size: 10px;
}

.course-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 7px;
  padding: 40px 0;
  color: var(--ink-3);
  font-size: 12px;
}

.course-empty i {
  font-size: 24px;
}

.dialog-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.selection-summary {
  max-width: 430px;
  overflow: hidden;
  color: var(--ink-3);
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 980px) {
  .product-search {
    align-items: flex-start;
    flex-direction: column;
  }

  .search-note {
    padding-top: 0;
  }
}
</style>
