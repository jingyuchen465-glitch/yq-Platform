<template>
  <div class="page-container order-page">
    <header class="page-head">
      <div><p class="eyebrow">PAYMENT LEDGER</p><h1>订单与退款</h1><p class="head-copy">查看支付账单并审核学生发起的退款申请。</p></div>
      <div class="order-counter"><span>PAYMENTS</span><strong>{{ paymentTotal }}</strong></div>
    </header>

    <el-tabs v-model="activeTab" class="order-tabs">
      <el-tab-pane label="支付订单" name="payments">
        <el-form :inline="true" :model="paymentQuery" class="toolbar-form"><el-form-item label="订单号"><el-input v-model.trim="paymentQuery.orderNo" clearable placeholder="商户订单号" /></el-form-item><el-form-item label="状态"><el-select v-model="paymentQuery.status" clearable placeholder="全部"><el-option v-for="item in paymentStatuses" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item><el-button type="primary" icon="el-icon-search" @click="searchPayments">查询</el-button></el-form>
        <el-table v-loading="paymentLoading" :data="payments" class="ledger-table"><el-table-column label="订单号" min-width="210"><template #default="{ row }"><code>{{ row.orderNo }}</code><small>预订单 #{{ row.prepaymentOrderId }}</small></template></el-table-column><el-table-column prop="studentName" label="学生" min-width="100" /><el-table-column prop="paymentChannel" label="渠道" width="90" /><el-table-column label="金额" width="130"><template #default="{ row }">¥ {{ money(row.orderAmount) }}<small v-if="Number(row.refundedAmount)">已退 ¥ {{ money(row.refundedAmount) }}</small></template></el-table-column><el-table-column label="状态" width="110"><template #default="{ row }"><el-tag :type="paymentStatus(row.status).type" size="mini">{{ paymentStatus(row.status).label }}</el-tag></template></el-table-column><el-table-column label="支付时间" min-width="155"><template #default="{ row }">{{ dateTime(row.paySuccessTime) }}</template></el-table-column></el-table>
        <el-pagination class="pager" layout="total, prev, pager, next" :current-page="paymentQuery.current" :page-size="paymentQuery.size" :total="paymentTotal" @current-change="changePaymentPage" />
      </el-tab-pane>

      <el-tab-pane label="退款申请" name="refunds">
        <el-form :inline="true" :model="refundQuery" class="toolbar-form"><el-form-item label="关键词"><el-input v-model.trim="refundQuery.keyword" clearable placeholder="订单号、学生或退款单号" /></el-form-item><el-form-item label="状态"><el-select v-model="refundQuery.status" clearable placeholder="全部"><el-option v-for="item in refundStatuses" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item><el-button type="primary" icon="el-icon-search" @click="searchRefunds">查询</el-button></el-form>
        <el-table v-loading="refundLoading" :data="refunds" class="ledger-table"><el-table-column label="退款单" min-width="190"><template #default="{ row }"><code>{{ row.refundNo }}</code><small>{{ row.paymentOrderNo }}</small></template></el-table-column><el-table-column label="学生" min-width="110"><template #default="{ row }">{{ row.studentName }}<small>{{ row.studentPhone }}</small></template></el-table-column><el-table-column label="金额" width="110"><template #default="{ row }">¥ {{ money(row.refundAmount) }}</template></el-table-column><el-table-column prop="reason" label="申请原因" min-width="190" show-overflow-tooltip /><el-table-column label="状态" width="110"><template #default="{ row }"><el-tag :type="refundStatus(row.status).type" size="mini">{{ refundStatus(row.status).label }}</el-tag></template></el-table-column><el-table-column label="操作" width="170" fixed="right"><template #default="{ row }"><el-button v-if="['PENDING', 'FAILED'].includes(row.status)" type="text" @click="openReview(row, 'approve')">通过并退款</el-button><el-button v-if="row.status === 'PENDING'" type="text" class="reject-action" @click="openReview(row, 'reject')">驳回</el-button></template></el-table-column></el-table>
        <el-pagination class="pager" layout="total, prev, pager, next" :current-page="refundQuery.current" :page-size="refundQuery.size" :total="refundTotal" @current-change="changeRefundPage" />
      </el-tab-pane>
    </el-tabs>

    <el-dialog :visible.sync="reviewVisible" :title="reviewAction === 'approve' ? '通过退款申请' : '驳回退款申请'" width="440px" :close-on-click-modal="false"><el-form label-position="top"><el-form-item :label="reviewAction === 'approve' ? '审核说明' : '驳回原因'" required><el-input v-model.trim="reviewRemark" type="textarea" :rows="4" maxlength="500" show-word-limit /></el-form-item></el-form><template #footer><el-button @click="reviewVisible = false">取消</el-button><el-button :type="reviewAction === 'approve' ? 'primary' : 'danger'" :loading="reviewLoading" @click="submitReview">确认</el-button></template></el-dialog>
  </div>
</template>

<script>
import { pageOrderPayment } from '@/api/orderPayment'
import { approvePaymentRefund, pagePaymentRefunds, rejectPaymentRefund } from '@/api/paymentRefund'

const PAYMENT_STATUS = { PENDING: { label: '待支付', type: 'warning' }, PAID: { label: '已支付', type: 'success' }, REFUNDING: { label: '退款中', type: 'warning' }, REFUNDED: { label: '已退款', type: 'info' }, CLOSED: { label: '已关闭', type: 'danger' } }
const REFUND_STATUS = { PENDING: { label: '待审核', type: 'warning' }, PROCESSING: { label: '退款中', type: 'warning' }, REJECTED: { label: '已驳回', type: 'info' }, REFUNDED: { label: '已退款', type: 'success' }, FAILED: { label: '退款失败', type: 'danger' } }

export default {
  name: 'OrderPaymentManage',
  data() { return { activeTab: 'payments', payments: [], refunds: [], paymentTotal: 0, refundTotal: 0, paymentLoading: false, refundLoading: false, reviewVisible: false, reviewLoading: false, reviewAction: 'approve', reviewTarget: null, reviewRemark: '', paymentQuery: { current: 1, size: 10, orderNo: '', status: '' }, refundQuery: { current: 1, size: 10, keyword: '', status: '' }, paymentStatuses: Object.entries(PAYMENT_STATUS).map(([value, item]) => ({ value, label: item.label })), refundStatuses: Object.entries(REFUND_STATUS).map(([value, item]) => ({ value, label: item.label })) } },
  watch: { activeTab(value) { if (value === 'refunds' && !this.refunds.length) this.loadRefunds() } },
  mounted() { this.loadPayments() },
  methods: {
    money(value) { return Number(value || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) },
    dateTime(value) { if (!value) return '—'; const date = new Date(value); return Number.isNaN(date.getTime()) ? value : new Intl.DateTimeFormat('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', hour12: false }).format(date) },
    paymentStatus(value) { return PAYMENT_STATUS[value] || { label: value || '未知', type: 'info' } },
    refundStatus(value) { return REFUND_STATUS[value] || { label: value || '未知', type: 'info' } },
    async loadPayments() { this.paymentLoading = true; try { const response = await pageOrderPayment(this.paymentQuery); this.payments = response.data.records || []; this.paymentTotal = response.data.total || 0 } finally { this.paymentLoading = false } },
    async loadRefunds() { this.refundLoading = true; try { const response = await pagePaymentRefunds(this.refundQuery); this.refunds = response.data.records || []; this.refundTotal = response.data.total || 0 } finally { this.refundLoading = false } },
    searchPayments() { this.paymentQuery.current = 1; this.loadPayments() },
    searchRefunds() { this.refundQuery.current = 1; this.loadRefunds() },
    changePaymentPage(page) { this.paymentQuery.current = page; this.loadPayments() },
    changeRefundPage(page) { this.refundQuery.current = page; this.loadRefunds() },
    openReview(row, action) { this.reviewTarget = row; this.reviewAction = action; this.reviewRemark = ''; this.reviewVisible = true },
    async submitReview() { if (!this.reviewRemark) { this.$message.warning('请填写审核说明'); return } this.reviewLoading = true; try { if (this.reviewAction === 'approve') await approvePaymentRefund(this.reviewTarget.id, { remark: this.reviewRemark }); else await rejectPaymentRefund(this.reviewTarget.id, { remark: this.reviewRemark }); this.$message.success(this.reviewAction === 'approve' ? '退款已处理' : '退款申请已驳回'); this.reviewVisible = false; await Promise.all([this.loadRefunds(), this.loadPayments()]) } finally { this.reviewLoading = false } }
  }
}
</script>

<style scoped>
.order-page { max-width: 1440px; margin: 0 auto; }.page-head { display: flex; justify-content: space-between; align-items: flex-start; gap: 24px; margin-bottom: 22px; }.eyebrow { margin-bottom: 8px; color: #0e7490; font: 700 11px var(--mono, monospace); letter-spacing: .12em; }.page-head h1 { margin: 0; color: #132d45; font-size: 28px; }.head-copy { margin: 8px 0 0; color: #63788a; }.order-counter { min-width: 128px; padding: 15px 18px; border-left: 3px solid #14b8a6; background: #effaf8; }.order-counter span { display: block; color: #53707d; font-size: 10px; }.order-counter strong { display: block; margin-top: 5px; color: #0f566c; font-size: 26px; }.order-tabs { padding: 0 20px 20px; border: 1px solid #dbe7ec; border-radius: 6px; background: #fff; }.toolbar-form { padding: 18px 0 8px; }.ledger-table code { display: block; color: #155e75; font-size: 11px; }.ledger-table small { display: block; margin-top: 4px; color: #8496a3; font-size: 10px; }.pager { margin-top: 18px; text-align: right; }.reject-action { color: #c2410c; }.el-button--text { font-weight: 600; }
@media (max-width: 760px) { .page-head { flex-direction: column; }.order-counter { width: 100%; }.toolbar-form .el-form-item { display: block; margin-right: 0; }.toolbar-form .el-input,.toolbar-form .el-select { width: 100%; } }
</style>
