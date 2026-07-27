<template>
  <div class="page-container order-page">
    <header class="page-head order-head">
      <div>
        <p class="eyebrow">PAY:ORDER-PAYMENT · TRANSACTION LEDGER</p>
        <h1>订单管理</h1>
        <p class="head-copy">追踪每一笔支付订单的状态、金额与退款进度。</p>
      </div>
      <div class="order-counter">
        <span class="counter-label">PAYMENTS</span>
        <strong>{{ total }}</strong>
        <span>笔支付订单</span>
      </div>
    </header>

    <section class="search-bar order-search">
      <el-form :inline="true" :model="queryParams" @submit.native.prevent>
        <el-form-item label="订单号">
          <el-input
            v-model="queryParams.orderNo"
            prefix-icon="el-icon-search"
            placeholder="精确匹配订单号"
            clearable
            @keyup.enter.native="handleSearch"
          />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input
            v-model="queryParams.studentPhone"
            prefix-icon="el-icon-mobile-phone"
            placeholder="学生手机号"
            clearable
            @keyup.enter.native="handleSearch"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" clearable placeholder="全部状态">
            <el-option
              v-for="item in statusOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" @click="handleSearch">查询</el-button>
          <el-button icon="el-icon-refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </section>

    <section class="table-card order-card">
      <div class="table-toolbar">
        <div>
          <p class="toolbar-kicker">TRANSACTION DESK</p>
          <h2>支付订单台账</h2>
        </div>
      </div>

      <el-table :data="tableData" v-loading="loading" stripe class="order-table">
        <el-table-column label="订单号" min-width="190">
          <template slot-scope="{ row }">
            <div class="orderno-cell">
              <code class="mono">{{ row.orderNo }}</code>
              <span class="prepay-ref">预付 {{ row.prepaymentOrderId }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="学生" min-width="150">
          <template slot-scope="{ row }">
            <div class="student-cell">
              <span class="student-avatar">{{ studentInitial(row.studentName) }}</span>
              <div class="student-profile">
                <strong>{{ row.studentName }}</strong>
                <span><i class="el-icon-mobile-phone"></i>{{ row.studentPhone }}</span>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="金额" width="130" align="right">
          <template slot-scope="{ row }">
            <div class="amount-cell">
              <b>¥ {{ formatMoney(row.orderAmount) }}</b>
              <span v-if="row.refundedAmount > 0" class="refunded">退 ¥ {{ formatMoney(row.refundedAmount) }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="110" align="center">
          <template slot-scope="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="渠道订单号" min-width="180">
          <template slot-scope="{ row }">
            <code v-if="row.uniqueOrderNo" class="mono channel-code">{{ row.uniqueOrderNo }}</code>
            <span v-else class="muted">—</span>
          </template>
        </el-table-column>

        <el-table-column label="支付时间" width="170" align="center">
          <template slot-scope="{ row }">
            <code v-if="row.paySuccessTime" class="mono">{{ row.paySuccessTime }}</code>
            <span v-else class="muted">—</span>
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
      title="修改支付订单"
      :visible.sync="dialogVisible"
      width="620px"
      custom-class="order-dialog"
      :close-on-click-modal="false"
      @closed="resetForm"
    >
      <div class="order-summary" v-if="form.orderNo">
        <div class="summary-item">
          <span>订单号</span>
          <code>{{ form.orderNo }}</code>
        </div>
        <div class="summary-item">
          <span>学生</span>
          <b>{{ form.studentName }} · {{ form.studentPhone }}</b>
        </div>
        <div class="summary-item">
          <span>订单金额</span>
          <b class="amount-highlight">¥ {{ formatMoney(form.orderAmount) }}</b>
        </div>
      </div>

      <el-form ref="orderForm" :model="form" :rules="formRules" label-position="top">
        <div class="form-grid">
          <el-form-item label="订单状态" prop="status">
            <el-select v-model="form.status" placeholder="请选择状态">
              <el-option
                v-for="item in statusOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="已退款金额" prop="refundedAmount">
            <el-input-number
              v-model="form.refundedAmount"
              :min="0"
              :precision="2"
              :step="0.01"
              controls-position="right"
            />
          </el-form-item>
        </div>
        <el-form-item label="渠道唯一订单号" prop="uniqueOrderNo">
          <el-input v-model="form.uniqueOrderNo" maxlength="128" placeholder="支付宝交易号等（选填）" />
        </el-form-item>
        <el-form-item label="支付成功时间" prop="paySuccessTime">
          <el-date-picker
            v-model="form.paySuccessTime"
            type="datetime"
            value-format="yyyy-MM-dd HH:mm:ss"
            placeholder="选择支付成功时间（选填）"
          />
        </el-form-item>
      </el-form>

      <div slot="footer" class="dialog-actions">
        <span class="form-note">
          <i class="el-icon-warning-outline"></i>
          修改状态前请确认退款流程已同步
        </span>
        <div>
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitLoading" @click="handleSubmit">保存修改</el-button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  pageOrderPayment,
  getOrderPayment,
  updateOrderPayment,
  deleteOrderPayment
} from '@/api/orderPayment'

const STATUS_MAP = {
  PENDING: { label: '待支付', tag: 'warning' },
  PAID: { label: '已支付', tag: 'success' },
  REFUNDING: { label: '退款中', tag: '' },
  REFUNDED: { label: '已退款', tag: 'info' },
  CLOSED: { label: '已关闭', tag: 'danger' }
}

export default {
  name: 'OrderPaymentManage',
  data() {
    return {
      loading: false,
      submitLoading: false,
      tableData: [],
      total: 0,
      queryParams: {
        current: 1,
        size: 10,
        orderNo: '',
        studentPhone: '',
        status: ''
      },
      dialogVisible: false,
      editId: null,
      form: this.createEmptyForm(),
      formRules: {
        status: [
          { required: true, message: '请选择订单状态', trigger: 'change' }
        ],
        refundedAmount: [
          { type: 'number', min: 0, message: '退款金额不能为负数', trigger: 'blur' }
        ]
      },
      statusOptions: Object.entries(STATUS_MAP).map(([value, { label }]) => ({ value, label }))
    }
  },
  created() {
    this.fetchData()
  },
  methods: {
    createEmptyForm() {
      return {
        orderNo: '',
        studentName: '',
        studentPhone: '',
        orderAmount: 0,
        status: '',
        refundedAmount: 0,
        uniqueOrderNo: '',
        paySuccessTime: null
      }
    },
    async fetchData() {
      this.loading = true
      try {
        const res = await pageOrderPayment(this.queryParams)
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
      this.queryParams = { current: 1, size: 10, orderNo: '', studentPhone: '', status: '' }
      this.fetchData()
    },
    async handleEdit(row) {
      this.editId = row.id
      this.dialogVisible = true
      this.submitLoading = true
      try {
        const res = await getOrderPayment(row.id)
        const order = res.data
        this.form = {
          orderNo: order.orderNo,
          studentName: order.studentName,
          studentPhone: order.studentPhone,
          orderAmount: order.orderAmount,
          status: order.status || '',
          refundedAmount: order.refundedAmount || 0,
          uniqueOrderNo: order.uniqueOrderNo || '',
          paySuccessTime: order.paySuccessTime || null
        }
      } catch (e) {
        this.dialogVisible = false
      } finally {
        this.submitLoading = false
      }
    },
    handleSubmit() {
      this.$refs.orderForm.validate(async valid => {
        if (!valid) return
        this.submitLoading = true
        try {
          await updateOrderPayment(this.editId, {
            status: this.form.status,
            refundedAmount: this.form.refundedAmount,
            uniqueOrderNo: this.form.uniqueOrderNo || null,
            paySuccessTime: this.form.paySuccessTime || null
          })
          this.$message.success('订单修改成功')
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
      this.$confirm(`确定删除订单「${row.orderNo}」吗？删除后不可恢复。`, '删除支付订单', {
        confirmButtonText: '确认删除',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        await deleteOrderPayment(row.id)
        this.$message.success('订单已删除')
        if (this.tableData.length === 1 && this.queryParams.current > 1) {
          this.queryParams.current -= 1
        }
        this.fetchData()
      }).catch(() => {})
    },
    statusLabel(status) {
      return (STATUS_MAP[status] || {}).label || status || '未知'
    },
    statusTagType(status) {
      return (STATUS_MAP[status] || {}).tag || 'info'
    },
    studentInitial(name) {
      return name ? name.trim().slice(0, 1) : '生'
    },
    formatMoney(value) {
      const amount = Number(value || 0)
      return amount.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
    }
  }
}
</script>

<style scoped>
.order-page {
  position: relative;
}

.order-page::before {
  content: '';
  position: absolute;
  top: 0;
  right: 26px;
  width: 170px;
  height: 7px;
  border-radius: 0 0 4px 4px;
  background: linear-gradient(90deg, var(--brass) 0 40%, var(--jade) 40% 76%, var(--clay) 76%);
}

.order-head {
  align-items: center;
}

.head-copy {
  margin-top: 8px;
  color: var(--ink-3);
  font-size: 13px;
}

.order-counter {
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

.order-counter .counter-label {
  grid-column: 1 / -1;
  color: var(--brass-ink);
  font-family: var(--font-mono);
  font-size: 9px;
  font-weight: 700;
  letter-spacing: .16em;
}

.order-counter strong {
  color: var(--jade-deep);
  font-family: var(--font-mono);
  font-size: 28px;
  line-height: 1;
}

.order-counter span:last-child {
  padding-bottom: 2px;
  color: var(--ink-3);
  font-size: 11px;
}

.order-search .el-select {
  width: 140px;
}

.order-card {
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

.orderno-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.orderno-cell .mono {
  font-size: 12px;
  font-weight: 600;
  color: var(--ink);
}

.prepay-ref {
  color: var(--ink-3);
  font-size: 10px;
}

.student-cell {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.student-avatar {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  flex: none;
  border: 1px solid #b9d1c6;
  border-radius: 5px;
  color: var(--jade-deep);
  background: var(--jade-soft);
  font-family: var(--font-display);
  font-size: 14px;
  font-weight: 800;
}

.student-profile {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 3px;
}

.student-profile strong {
  color: var(--ink);
  font-size: 13px;
}

.student-profile > span {
  overflow: hidden;
  color: var(--ink-3);
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.student-profile i {
  width: 16px;
  color: var(--brass-ink);
}

.amount-cell {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 3px;
}

.amount-cell b {
  color: var(--ink);
  font-family: var(--font-mono);
  font-size: 13px;
}

.amount-cell .refunded {
  color: var(--clay);
  font-size: 10px;
}

.channel-code {
  font-size: 11px;
  word-break: break-all;
}

.muted {
  color: var(--ink-3);
  font-size: 12px;
}

/* ---------- 编辑弹窗 ---------- */
.order-summary {
  display: flex;
  gap: 20px;
  padding: 14px 16px;
  margin-bottom: 18px;
  border: 1px solid var(--line-soft);
  border-radius: 8px;
  background: #fbfcfa;
}

.summary-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.summary-item span {
  color: var(--ink-3);
  font-size: 10px;
  letter-spacing: .06em;
}

.summary-item code {
  color: var(--ink);
  font-family: var(--font-mono);
  font-size: 12px;
  font-weight: 600;
}

.summary-item b {
  color: var(--ink);
  font-size: 13px;
}

.amount-highlight {
  color: var(--jade-deep) !important;
  font-family: var(--font-mono);
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 16px;
}

.form-grid .el-select,
.form-grid .el-input-number {
  width: 100%;
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
</style>
