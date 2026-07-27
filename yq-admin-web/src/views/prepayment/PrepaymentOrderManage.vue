<template>
  <div class="page-container prepayment-page">
    <header class="page-head intake-head">
      <div>
        <p class="eyebrow">MARKET:PREPAYMENT · STUDENT INTAKE</p>
        <h1>预订单管理</h1>
        <p class="head-copy">记录学生意向、锁定产品价格，并把线索交给明确的销售人员。</p>
      </div>
      <div class="intake-counter">
        <span class="counter-label">PRE-ORDERS</span>
        <strong>{{ total }}</strong>
        <span>条招生线索</span>
      </div>
    </header>

    <section class="search-bar intake-search">
      <el-form :inline="true" :model="queryParams" @submit.native.prevent>
        <el-form-item label="线索检索">
          <el-input
            v-model="queryParams.keyword"
            class="keyword-input"
            prefix-icon="el-icon-search"
            placeholder="姓名、手机号、邮箱、产品、销售或院校"
            clearable
            @keyup.enter.native="handleSearch"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" @click="handleSearch">查询</el-button>
          <el-button icon="el-icon-refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
      <div class="intake-hint">
        <span class="hint-dot"></span>
        新增线索时自动检查并创建学生账号
      </div>
    </section>

    <section class="table-card intake-card">
      <div class="table-toolbar">
        <div>
          <p class="toolbar-kicker">INTAKE DESK</p>
          <h2>预订单档案</h2>
        </div>
        <el-button type="primary" icon="el-icon-plus" @click="handleAdd">新增预订单</el-button>
      </div>

      <el-table :data="tableData" v-loading="loading" stripe class="intake-table">
        <el-table-column label="学生档案" min-width="245">
          <template slot-scope="{ row }">
            <div class="student-cell">
              <span class="student-avatar">{{ studentInitial(row.name) }}</span>
              <div class="student-profile">
                <div class="student-name-line">
                  <strong>{{ row.name }}</strong>
                  <code>PO-{{ String(row.id).padStart(4, '0') }}</code>
                </div>
                <span><i class="el-icon-mobile-phone"></i>{{ row.phone }}</span>
                <span v-if="row.email"><i class="el-icon-message"></i>{{ row.email }}</span>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="教育背景" min-width="180">
          <template slot-scope="{ row }">
            <div class="education-cell">
              <el-tag v-if="row.education" size="small" type="info">{{ educationLabel(row.education) }}</el-tag>
              <span :class="{ muted: !row.graduateSchool }">
                {{ row.graduateSchool || '未填写毕业院校' }}
              </span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="意向产品" min-width="220">
          <template slot-scope="{ row }">
            <div class="product-intent">
              <strong>{{ row.productName }}</strong>
              <span>快照价 <b>¥ {{ formatMoney(row.productPrice) }}</b></span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="归属销售" width="150">
          <template slot-scope="{ row }">
            <div class="salesperson-cell">
              <span class="sales-mark">S</span>
              <span>{{ row.salespersonName }}</span>
            </div>
          </template>
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
          :page-sizes="[10, 20, 50]"
          @size-change="fetchData"
          @current-change="fetchData"
        />
      </div>
    </section>

    <el-dialog
      :title="dialogTitle"
      :visible.sync="dialogVisible"
      width="860px"
      custom-class="prepayment-dialog"
      :close-on-click-modal="false"
      @closed="resetForm"
    >
      <el-form ref="orderForm" :model="form" :rules="formRules" label-position="top">
        <div class="form-section student-section">
          <div class="section-heading">
            <span class="section-index">A</span>
            <div>
              <h3>学生资料</h3>
              <p>{{ isEdit ? '修改预订单不会改动已有学生账号。' : '手机号不存在时，将同步创建学生登录账号。' }}</p>
            </div>
          </div>
          <div class="form-grid form-grid-four">
            <el-form-item label="学生姓名" prop="name">
              <el-input v-model="form.name" maxlength="30" placeholder="请输入学生姓名" />
            </el-form-item>
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="form.phone" maxlength="11" placeholder="用于学生登录" />
            </el-form-item>
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="form.email" maxlength="128" placeholder="选填" />
            </el-form-item>
            <el-form-item label="出生日期" prop="birthday">
              <el-date-picker
                v-model="form.birthday"
                type="date"
                value-format="yyyy-MM-dd"
                :picker-options="birthdayPickerOptions"
                placeholder="选择日期"
              />
            </el-form-item>
          </div>
        </div>

        <div class="form-section background-section">
          <div class="section-heading">
            <span class="section-index">B</span>
            <div>
              <h3>教育背景</h3>
              <p>补充判断学习基础所需的信息。</p>
            </div>
          </div>
          <div class="form-grid form-grid-background">
            <el-form-item label="学历" prop="education">
              <el-select
                v-model="form.education"
                filterable
                clearable
                :loading="optionsLoading"
                no-data-text="暂无启用的学历规则"
                placeholder="请选择学历"
              >
                <el-option
                  v-for="item in educationOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="毕业院校" prop="graduateSchool">
              <el-input v-model="form.graduateSchool" maxlength="100" placeholder="请输入毕业院校" />
            </el-form-item>
            <el-form-item label="家庭地址" prop="homeAddress">
              <el-input v-model="form.homeAddress" maxlength="200" placeholder="请输入家庭地址" />
            </el-form-item>
          </div>
        </div>

        <div class="form-section intent-section">
          <div class="section-heading">
            <span class="section-index">C</span>
            <div>
              <h3>意向与归属</h3>
              <p>保存时固定当前产品价格和销售人员姓名。</p>
            </div>
          </div>
          <div class="intent-grid">
            <div class="intent-fields">
              <el-form-item label="意向产品" prop="productId">
                <el-select
                  v-model="form.productId"
                  filterable
                  :loading="optionsLoading"
                  placeholder="请选择意向产品"
                  @change="handleProductChange"
                >
                  <el-option
                    v-for="product in productOptions"
                    :key="product.id"
                    :label="product.productName"
                    :value="product.id"
                  >
                    <div class="product-option">
                      <span>{{ product.productName }}</span>
                      <b>¥ {{ formatMoney(product.productPrice) }}</b>
                    </div>
                  </el-option>
                </el-select>
              </el-form-item>
              <el-form-item label="销售人员" prop="salespersonUserId">
                <el-select
                  v-model="form.salespersonUserId"
                  filterable
                  :loading="optionsLoading"
                  placeholder="请选择SALES角色用户"
                >
                  <el-option
                    v-for="salesperson in salespersonOptions"
                    :key="salesperson.id"
                    :label="salesperson.name"
                    :value="salesperson.id"
                  >
                    <div class="sales-option">
                      <span>{{ salesperson.name }}</span>
                      <code>{{ salesperson.phone || '未填写手机号' }}</code>
                    </div>
                  </el-option>
                </el-select>
              </el-form-item>
            </div>

            <div class="intent-ticket" :class="{ empty: !selectedProduct }">
              <span class="ticket-cap">PRICE SNAPSHOT</span>
              <template v-if="selectedProduct">
                <strong>{{ selectedProduct.productName }}</strong>
                <b>¥ {{ formatMoney(selectedProduct.productPrice) }}</b>
                <p>{{ compactCourseNames(selectedProduct.courseName) }}</p>
              </template>
              <template v-else>
                <i class="el-icon-goods"></i>
                <p>选择产品后预览价格快照</p>
              </template>
            </div>
          </div>
        </div>
      </el-form>

      <div slot="footer" class="dialog-actions">
        <span class="form-note">
          <i class="el-icon-lock"></i>
          学生初始密码仅在首次建档后展示一次
        </span>
        <div>
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitLoading" @click="handleSubmit">
            {{ isEdit ? '保存修改' : '创建预订单' }}
          </el-button>
        </div>
      </div>
    </el-dialog>

    <el-dialog
      title="学生账号已创建"
      :visible.sync="credentialVisible"
      width="460px"
      custom-class="credential-dialog"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      :show-close="false"
    >
      <div class="credential-card">
        <span class="credential-seal">NEW<br>STUDENT</span>
        <p class="credential-intro">请立即把登录凭据交给学生。关闭窗口后，系统不会再次显示初始密码。</p>
        <div class="credential-row">
          <span>登录手机号</span>
          <code>{{ studentCredential.phone }}</code>
        </div>
        <div class="credential-row password-row">
          <span>初始密码</span>
          <code>{{ studentCredential.password }}</code>
        </div>
      </div>
      <div slot="footer" class="credential-actions">
        <el-button icon="el-icon-document-copy" @click="copyCredential">复制账号信息</el-button>
        <el-button type="primary" @click="credentialVisible = false">我已妥善保存</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  pagePrepaymentOrder,
  getPrepaymentOrder,
  getSalespersonOptions,
  getPrepaymentProductOptions,
  getEducationOptions,
  addPrepaymentOrder,
  updatePrepaymentOrder,
  deletePrepaymentOrder
} from '@/api/prepaymentOrder'

export default {
  name: 'PrepaymentOrderManage',
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
      credentialVisible: false,
      isEdit: false,
      editId: null,
      productOptions: [],
      salespersonOptions: [],
      educationOptions: [],
      form: this.createEmptyForm(),
      studentCredential: {
        phone: '',
        password: ''
      },
      birthdayPickerOptions: {
        disabledDate(date) {
          return date.getTime() > Date.now()
        }
      },
      formRules: {
        name: [
          { required: true, message: '请输入学生姓名', trigger: 'blur' },
          { max: 30, message: '长度不能超过30个字符', trigger: 'blur' }
        ],
        phone: [
          { required: true, message: '请输入学生手机号', trigger: 'blur' },
          { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的11位手机号', trigger: 'blur' }
        ],
        email: [
          { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' },
          { max: 128, message: '长度不能超过128个字符', trigger: 'blur' }
        ],
        education: [
          { max: 20, message: '长度不能超过20个字符', trigger: 'change' }
        ],
        graduateSchool: [
          { max: 100, message: '长度不能超过100个字符', trigger: 'blur' }
        ],
        homeAddress: [
          { max: 200, message: '长度不能超过200个字符', trigger: 'blur' }
        ],
        productId: [
          { required: true, message: '请选择意向产品', trigger: 'change' }
        ],
        salespersonUserId: [
          { required: true, message: '请选择销售人员', trigger: 'change' }
        ]
      }
    }
  },
  computed: {
    dialogTitle() {
      return this.isEdit ? '编辑预订单档案' : '登记新预订单'
    },
    selectedProduct() {
      return this.productOptions.find(product => product.id === this.form.productId) || null
    }
  },
  created() {
    this.fetchData()
    this.loadOptions()
  },
  methods: {
    createEmptyForm() {
      return {
        name: '',
        phone: '',
        email: '',
        education: '',
        graduateSchool: '',
        homeAddress: '',
        birthday: null,
        productId: null,
        salespersonUserId: null
      }
    },
    async fetchData() {
      this.loading = true
      try {
        const res = await pagePrepaymentOrder(this.queryParams)
        this.tableData = res.data.records || []
        this.total = res.data.total || 0
      } catch (e) {
        // 错误已由请求拦截器统一处理
      } finally {
        this.loading = false
      }
    },
    async loadOptions() {
      this.optionsLoading = true
      try {
        const [productRes, salespersonRes, educationRes] = await Promise.all([
          getPrepaymentProductOptions().catch(() => ({ data: [] })),
          getSalespersonOptions().catch(() => ({ data: [] })),
          getEducationOptions().catch(() => ({ data: [] }))
        ])
        this.productOptions = productRes.data || []
        this.salespersonOptions = salespersonRes.data || []
        this.educationOptions = educationRes.data || []
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
        const res = await getPrepaymentOrder(row.id)
        const order = res.data
        this.form = {
          name: order.name || '',
          phone: order.phone || '',
          email: order.email || '',
          education: order.education || '',
          graduateSchool: order.graduateSchool || '',
          homeAddress: order.homeAddress || '',
          birthday: order.birthday || null,
          productId: order.productId,
          salespersonUserId: order.salespersonUserId
        }
      } catch (e) {
        this.dialogVisible = false
      } finally {
        this.submitLoading = false
      }
    },
    handleProductChange() {
      this.$refs.orderForm && this.$refs.orderForm.validateField('productId')
    },
    handleSubmit() {
      this.$refs.orderForm.validate(async valid => {
        if (!valid) return
        this.submitLoading = true
        try {
          const payload = {
            name: this.form.name.trim(),
            phone: this.form.phone.trim(),
            email: this.form.email ? this.form.email.trim() : null,
            education: this.form.education || null,
            graduateSchool: this.form.graduateSchool ? this.form.graduateSchool.trim() : null,
            homeAddress: this.form.homeAddress ? this.form.homeAddress.trim() : null,
            birthday: this.form.birthday || null,
            productId: this.form.productId,
            salespersonUserId: this.form.salespersonUserId
          }
          if (this.isEdit) {
            await updatePrepaymentOrder(this.editId, payload)
            this.$message.success('预订单修改成功')
          } else {
            const res = await addPrepaymentOrder(payload)
            if (res.data && res.data.studentCreated) {
              this.studentCredential = {
                phone: res.data.studentPhone,
                password: res.data.initialPassword
              }
              this.credentialVisible = true
            } else {
              this.$message.success('预订单已新增，该手机号已有学生账号')
            }
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
      this.editId = null
      this.$nextTick(() => {
        this.$refs.orderForm && this.$refs.orderForm.clearValidate()
      })
    },
    handleDelete(row) {
      this.$confirm(`确定删除「${row.name}」的预订单吗？学生账号会保留。`, '删除预订单', {
        confirmButtonText: '确认删除',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        await deletePrepaymentOrder(row.id)
        this.$message.success('预订单已删除')
        if (this.tableData.length === 1 && this.queryParams.current > 1) {
          this.queryParams.current -= 1
        }
        this.fetchData()
      }).catch(() => {})
    },
    async copyCredential() {
      const text = `学生账号：${this.studentCredential.phone}\n初始密码：${this.studentCredential.password}`
      try {
        if (navigator.clipboard && window.isSecureContext) {
          await navigator.clipboard.writeText(text)
        } else {
          const textarea = document.createElement('textarea')
          textarea.value = text
          textarea.style.position = 'fixed'
          textarea.style.opacity = '0'
          document.body.appendChild(textarea)
          textarea.select()
          document.execCommand('copy')
          document.body.removeChild(textarea)
        }
        this.$message.success('账号信息已复制')
      } catch (e) {
        this.$message.warning('复制失败，请手动保存账号信息')
      }
    },
    studentInitial(name) {
      return name ? name.trim().slice(0, 1) : '生'
    },
    educationLabel(value) {
      const option = this.educationOptions.find(item => item.value === value)
      return option ? option.label : value
    },
    formatMoney(value) {
      const amount = Number(value || 0)
      return amount.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
    },
    compactCourseNames(courseName) {
      if (!courseName) return '暂无课程快照'
      const names = courseName.split('/').filter(Boolean)
      return names.length > 3 ? `${names.slice(0, 3).join(' / ')} 等 ${names.length} 门课程` : names.join(' / ')
    }
  }
}
</script>

<style scoped>
.prepayment-page {
  position: relative;
}

.prepayment-page::before {
  content: '';
  position: absolute;
  top: 0;
  right: 26px;
  width: 170px;
  height: 7px;
  border-radius: 0 0 4px 4px;
  background: linear-gradient(90deg, var(--jade) 0 64%, var(--brass) 64% 82%, var(--clay) 82%);
}

.intake-head {
  align-items: center;
}

.head-copy {
  margin-top: 8px;
  color: var(--ink-3);
  font-size: 13px;
}

.intake-counter {
  display: grid;
  grid-template-columns: auto auto;
  align-items: end;
  gap: 3px 12px;
  min-width: 154px;
  padding: 12px 14px;
  border: 1px solid var(--line);
  border-radius: 6px;
  background: #f8faf6;
}

.intake-counter .counter-label {
  grid-column: 1 / -1;
  color: var(--brass-ink);
  font-family: var(--font-mono);
  font-size: 9px;
  font-weight: 700;
  letter-spacing: .16em;
}

.intake-counter strong {
  color: var(--jade-deep);
  font-family: var(--font-mono);
  font-size: 28px;
  line-height: 1;
}

.intake-counter span:last-child {
  padding-bottom: 2px;
  color: var(--ink-3);
  font-size: 11px;
}

.intake-search {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
}

.keyword-input {
  width: 420px;
}

.intake-hint {
  display: flex;
  align-items: center;
  gap: 8px;
  padding-bottom: 14px;
  color: var(--ink-3);
  font-size: 12px;
}

.hint-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--jade);
  box-shadow: 0 0 0 4px var(--jade-soft);
}

.intake-card {
  padding-top: 16px;
}

.table-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.toolbar-kicker {
  color: var(--brass-ink);
  font-family: var(--font-mono);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: .18em;
}

.table-toolbar h2 {
  margin-top: 2px;
  font-family: var(--font-display);
  font-size: 17px;
}

.student-cell {
  display: flex;
  align-items: flex-start;
  gap: 11px;
}

.student-avatar {
  display: grid;
  place-items: center;
  width: 38px;
  height: 38px;
  flex: none;
  border: 1px solid #b9d1c6;
  border-radius: 5px;
  color: var(--jade-deep);
  background: var(--jade-soft);
  font-family: var(--font-display);
  font-size: 15px;
  font-weight: 800;
}

.student-profile {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 4px;
}

.student-name-line {
  display: flex;
  align-items: center;
  gap: 8px;
}

.student-name-line strong {
  color: var(--ink);
  font-size: 14px;
}

.student-name-line code {
  color: var(--ink-3);
  font-family: var(--font-mono);
  font-size: 9px;
}

.student-profile > span {
  overflow: hidden;
  color: var(--ink-3);
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.student-profile i {
  width: 17px;
  color: var(--brass-ink);
}

.education-cell,
.product-intent {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 7px;
}

.education-cell span {
  max-width: 170px;
  overflow: hidden;
  color: var(--ink-2);
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.education-cell span.muted {
  color: var(--ink-3);
}

.product-intent strong {
  max-width: 210px;
  overflow: hidden;
  color: var(--ink);
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product-intent span {
  color: var(--ink-3);
  font-size: 11px;
}

.product-intent b {
  margin-left: 4px;
  color: var(--clay-deep);
  font-family: var(--font-mono);
  font-size: 13px;
}

.salesperson-cell {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--ink-2);
  font-size: 13px;
}

.sales-mark {
  display: grid;
  place-items: center;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  color: #fff;
  background: var(--pine-3);
  font-family: var(--font-mono);
  font-size: 10px;
  font-weight: 700;
}

.form-section {
  padding: 17px 18px 5px;
  border: 1px solid var(--line-soft);
  border-radius: 8px;
  background: #fbfcfa;
}

.form-section + .form-section {
  margin-top: 12px;
}

.section-heading {
  display: flex;
  align-items: flex-start;
  gap: 11px;
  margin-bottom: 14px;
}

.section-index {
  display: grid;
  place-items: center;
  width: 25px;
  height: 25px;
  flex: none;
  border: 1px solid var(--brass);
  border-radius: 3px;
  color: var(--brass-ink);
  font-family: var(--font-mono);
  font-size: 11px;
  font-weight: 700;
}

.section-heading h3 {
  font-family: var(--font-display);
  font-size: 14px;
}

.section-heading p {
  margin-top: 3px;
  color: var(--ink-3);
  font-size: 11px;
}

.form-grid {
  display: grid;
  gap: 0 12px;
}

.form-grid-four {
  grid-template-columns: 1fr 1fr 1.25fr 1fr;
}

.form-grid-background {
  grid-template-columns: .7fr 1.15fr 1.4fr;
}

.form-grid .el-select,
.form-grid .el-date-editor,
.intent-fields .el-select {
  width: 100%;
}

.intent-grid {
  display: grid;
  grid-template-columns: 1fr 280px;
  gap: 16px;
}

.intent-fields {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.intent-ticket {
  position: relative;
  display: flex;
  min-height: 88px;
  flex-direction: column;
  justify-content: center;
  padding: 13px 16px;
  border-radius: 6px;
  color: #dce9df;
  background: var(--pine);
  overflow: hidden;
}

.intent-ticket::after {
  content: '';
  position: absolute;
  top: -14px;
  right: -14px;
  width: 48px;
  height: 48px;
  border: 8px solid rgba(185, 138, 47, .22);
  border-radius: 50%;
}

.ticket-cap {
  margin-bottom: 5px;
  color: #9ab0a4;
  font-family: var(--font-mono);
  font-size: 8px;
  font-weight: 700;
  letter-spacing: .16em;
}

.intent-ticket strong {
  max-width: 220px;
  overflow: hidden;
  color: #fff;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.intent-ticket b {
  margin-top: 3px;
  color: #f3d897;
  font-family: var(--font-mono);
  font-size: 18px;
}

.intent-ticket p {
  max-width: 235px;
  margin-top: 4px;
  overflow: hidden;
  color: #9ab0a4;
  font-size: 10px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.intent-ticket.empty {
  align-items: center;
  color: #9ab0a4;
  text-align: center;
}

.intent-ticket.empty i {
  margin-bottom: 5px;
  font-size: 21px;
}

.product-option,
.sales-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.product-option b {
  color: var(--clay-deep);
  font-family: var(--font-mono);
  font-size: 11px;
}

.sales-option code {
  color: var(--ink-3);
  font-family: var(--font-mono);
  font-size: 10px;
}

.dialog-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.form-note {
  color: var(--ink-3);
  font-size: 11px;
}

.form-note i {
  margin-right: 5px;
  color: var(--brass-ink);
}

.credential-card {
  position: relative;
  padding: 18px;
  border: 1px solid #c5d8cf;
  border-radius: 8px;
  background: linear-gradient(145deg, #f6faf7, #edf4ef);
  overflow: hidden;
}

.credential-seal {
  position: absolute;
  top: 15px;
  right: 16px;
  display: grid;
  place-items: center;
  width: 54px;
  height: 54px;
  border: 2px solid var(--jade);
  border-radius: 50%;
  color: var(--jade-deep);
  font-family: var(--font-mono);
  font-size: 8px;
  font-weight: 700;
  line-height: 1.2;
  text-align: center;
  transform: rotate(-9deg);
  opacity: .8;
}

.credential-intro {
  width: calc(100% - 70px);
  margin-bottom: 18px;
  color: var(--ink-2);
  font-size: 12px;
  line-height: 1.7;
}

.credential-row {
  display: grid;
  grid-template-columns: 90px 1fr;
  align-items: center;
  padding: 11px 0;
  border-top: 1px dashed #c7d6ce;
}

.credential-row span {
  color: var(--ink-3);
  font-size: 11px;
}

.credential-row code {
  color: var(--ink);
  font-family: var(--font-mono);
  font-size: 14px;
  font-weight: 700;
  letter-spacing: .05em;
}

.password-row code {
  color: var(--clay-deep);
  font-size: 16px;
}

.credential-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

@media (max-width: 1120px) {
  .intake-search {
    align-items: flex-start;
    flex-direction: column;
  }

  .intake-hint {
    padding-bottom: 12px;
  }
}
</style>
