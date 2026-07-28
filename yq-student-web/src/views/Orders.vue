<template>
  <div class="student-page">
    <student-header :profile="profile" :logging-out="loggingOut" :homework-count="headerHomeworkCount" :order-count="orders.length" @logout="handleLogout" />
    <main class="page-shell">
      <header class="page-heading">
        <div><p class="page-kicker">COURSE ORDERS</p><h1>我的订单</h1><p>核对课程与订单金额，待支付订单可直接前往支付宝收银台完成付款。</p></div>
        <div class="amount-card"><span>订单总额</span><strong>¥{{ formatMoney(totalAmount) }}</strong><small>{{ orders.length }} 笔课程订单</small></div>
      </header>

      <section class="orders-card">
        <div class="orders-toolbar">
          <div class="order-tabs">
            <button v-for="item in filters" :key="item.value" type="button" :class="{ active: filter === item.value }" @click="filter = item.value">{{ item.label }}<span>{{ orderCount(item.value) }}</span></button>
          </div>
          <label class="order-search"><svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="11" cy="11" r="7"/><path d="m16 16 5 5"/></svg><input v-model.trim="keyword" type="search" placeholder="搜索课程或订单号"></label>
        </div>

        <p v-if="paymentError" class="payment-message" role="alert">{{ paymentError }}</p>
        <div v-if="loading" class="orders-state"><span class="loading-ring"></span>正在查询订单…</div>
        <div v-else-if="errorMessage" class="error-state"><strong>订单查询失败</strong><span>{{ errorMessage }}</span><button class="retry-button" type="button" @click="loadOrders">重新查询</button></div>
        <div v-else-if="!filteredOrders.length" class="empty-state"><strong>这里暂时没有订单</strong><span>切换筛选条件，或联系课程顾问确认报名记录。</span></div>

        <ol v-else class="orders-list">
          <li v-for="order in filteredOrders" :key="order.id" class="order-item">
            <div class="order-topline"><span>PRE · {{ order.id }}</span><time :datetime="order.createdAt">创建于 {{ formatDateTime(order.createdAt) }}</time><b :class="statusOf(order).tone">{{ statusOf(order).text }}</b></div>
            <div class="order-content">
              <div class="course-mark"><span>YQ</span><small>COURSE</small></div>
              <div class="order-name"><h2>{{ order.productName || order.subject || '未命名课程' }}</h2><p>{{ order.salespersonName ? `课程顾问 ${order.salespersonName}` : '课程顾问待确认' }}<span v-if="order.outTradeNo"> · {{ order.outTradeNo }}</span></p></div>
              <div class="order-price"><small>订单金额</small><strong>¥{{ formatMoney(order.productPrice || order.totalAmount) }}</strong></div>
              <span v-if="countdownText(order)" class="payment-countdown">{{ countdownText(order) }}</span>
              <button v-if="canPay(order)" class="pay-button" type="button" :disabled="payingOrderId !== null" @click="handlePay(order)">{{ payingOrderId === order.id ? '跳转中…' : statusOf(order).group === 'closed' ? '重新支付' : '去支付' }}</button>
              <div v-else class="complete-actions"><button v-if="canViewDetail(order)" class="detail-button" type="button" @click="showPaymentDetail(order)">查看详情</button><span class="complete-label">{{ statusOf(order).text }}</span></div>
            </div>
          </li>
        </ol>
      </section>

      <div v-if="detailVisible" class="detail-overlay" @click.self="closePaymentDetail">
        <section class="payment-detail-dialog" role="dialog" aria-modal="true" aria-label="订单详情">
          <header><div><p class="page-kicker">PAYMENT DETAIL</p><h2>订单详情</h2></div><button class="dialog-close" type="button" aria-label="关闭" @click="closePaymentDetail">×</button></header>
          <div v-if="detailLoading" class="detail-state">正在加载订单详情</div>
          <div v-else-if="detailError" class="detail-state detail-error">{{ detailError }}</div>
          <template v-else-if="paymentDetail">
            <dl class="detail-grid"><div><dt>课程</dt><dd>{{ paymentDetail.productName }}</dd></div><div><dt>订单金额</dt><dd>¥{{ formatMoney(paymentDetail.orderAmount) }}</dd></div><div><dt>商户订单号</dt><dd class="mono">{{ paymentDetail.orderNo }}</dd></div><div><dt>支付时间</dt><dd>{{ formatDateTime(paymentDetail.paidAt) }}</dd></div><div><dt>支付宝交易号</dt><dd class="mono">{{ paymentDetail.channelTradeNo || '待同步' }}</dd></div><div><dt>退款状态</dt><dd>{{ refundStatusText(paymentDetail.latestRefund && paymentDetail.latestRefund.status) }}</dd></div></dl>
            <section v-if="paymentDetail.latestRefund" class="refund-record"><h3>退款申请</h3><p>{{ paymentDetail.latestRefund.reason }}</p><small v-if="paymentDetail.latestRefund.reviewRemark">审核意见：{{ paymentDetail.latestRefund.reviewRemark }}</small><small v-if="paymentDetail.latestRefund.failureReason">处理说明：{{ paymentDetail.latestRefund.failureReason }}</small></section>
            <section v-if="canRequestRefund()" class="refund-form"><label for="refund-reason">退款原因</label><textarea id="refund-reason" v-model.trim="refundReason" maxlength="500" placeholder="请说明退款原因"></textarea><p v-if="refundError" class="refund-error">{{ refundError }}</p><button class="refund-button" type="button" :disabled="refundSubmitting" @click="submitRefundRequest">{{ refundSubmitting ? '提交中' : '申请退款' }}</button></section>
          </template>
        </section>
      </div>

      <footer class="order-help"><span>?</span><div><strong>订单或支付遇到问题？</strong><p>请保存订单号并联系课程顾问。支付结果以支付宝和学习中心订单状态为准。</p></div></footer>
    </main>
  </div>
</template>

<script>
import StudentHeader from '@/components/StudentHeader.vue'
import studentPage from '@/mixins/studentPage'
import { createAlipayTrade, createRefundRequest, getCurrentStudentPrepaymentOrders, getPaymentDetail } from '@/api/prepayment'
import { formatMoney, formatShortDate, submitAlipayForm } from '@/utils/payment'

export default {
  name: 'StudentOrders', components: { StudentHeader }, mixins: [studentPage],
  data() { return { orders: [], loading: true, errorMessage: '', paymentError: '', payingOrderId: null, filter: 'all', keyword: '', nowTick: Date.now(), countdownTimer: null, expiredOrderIds: [], detailVisible: false, detailLoading: false, detailError: '', paymentDetail: null, refundReason: '', refundError: '', refundSubmitting: false, filters: [{ value: 'all', label: '全部订单' }, { value: 'pending', label: '待支付' }, { value: 'paid', label: '已完成' }] } },
  computed: {
    totalAmount() { return this.orders.reduce((sum, item) => sum + (Number(item.productPrice || item.totalAmount) || 0), 0) },
    filteredOrders() { const keyword = this.keyword.toLowerCase(); return this.orders.filter(order => (this.filter === 'all' || this.statusOf(order).group === this.filter) && (!keyword || `${order.productName || ''} ${order.outTradeNo || ''} ${order.id || ''}`.toLowerCase().includes(keyword))) }
  },
  mounted() { this.loadOrders(); this.countdownTimer = window.setInterval(this.refreshCountdown, 1000) },
  beforeDestroy() { window.clearInterval(this.countdownTimer) },
  methods: {
    formatMoney,
    async loadOrders() { this.loading = true; this.errorMessage = ''; this.paymentError = ''; try { const response = await getCurrentStudentPrepaymentOrders(); this.orders = Array.isArray(response.data) ? response.data : (response.data && (response.data.records || response.data.list)) || []; this.headerOrderCount = this.orders.length } catch (error) { this.orders = []; this.errorMessage = error.message || '暂时无法获取订单' } finally { this.loading = false } },
    statusOf(order) { const refundStatus = String(order.refundStatus || '').toUpperCase(); if (refundStatus === 'PENDING') return { text: '退款审核中', tone: 'pending', group: 'paid' }; if (refundStatus === 'PROCESSING') return { text: '退款处理中', tone: 'pending', group: 'paid' }; if (refundStatus === 'REFUNDED' || String(order.paymentStatus || '').toUpperCase() === 'REFUNDED') return { text: '已退款', tone: 'closed', group: 'paid' }; if (refundStatus === 'REJECTED') return { text: '退款已驳回', tone: 'paid', group: 'paid' }; if (refundStatus === 'FAILED') return { text: '退款处理失败', tone: 'closed', group: 'paid' }; const status = String(order.paymentStatus || order.status || '').toUpperCase(); if (['PAID', 'SUCCESS', 'COMPLETED', 'TRADE_SUCCESS'].includes(status)) return { text: '已支付', tone: 'paid', group: 'paid' }; if (['CLOSED', 'CANCELLED'].includes(status)) return { text: '已关闭', tone: 'closed', group: 'closed' }; return { text: '待支付', tone: 'pending', group: 'pending' } },
    canPay(order) { return ['pending', 'closed'].includes(this.statusOf(order).group) },
    canViewDetail(order) { return Boolean(order.paymentId) && this.statusOf(order).group === 'paid' },
    refundStatusText(status) { const labels = { PENDING: '退款审核中', PROCESSING: '退款处理中', REJECTED: '退款已驳回', REFUNDED: '已退款', FAILED: '退款处理失败' }; return labels[String(status || '').toUpperCase()] || '未申请退款' },
    async showPaymentDetail(order) { this.detailVisible = true; this.detailLoading = true; this.detailError = ''; this.paymentDetail = null; this.refundReason = ''; this.refundError = ''; try { const response = await getPaymentDetail(order.paymentId); this.paymentDetail = response.data || null } catch (error) { this.detailError = error.message || '订单详情暂时无法加载' } finally { this.detailLoading = false } },
    closePaymentDetail() { if (this.refundSubmitting) return; this.detailVisible = false; this.paymentDetail = null; this.refundReason = ''; this.refundError = '' },
    async submitRefundRequest() { if (!this.refundReason) { this.refundError = '请填写退款原因'; return } this.refundSubmitting = true; this.refundError = ''; try { const response = await createRefundRequest(this.paymentDetail.paymentOrderId, { reason: this.refundReason }); this.paymentDetail = { ...this.paymentDetail, latestRefund: response.data }; const order = this.orders.find(item => item.paymentId === this.paymentDetail.paymentOrderId); if (order) this.$set(order, 'refundStatus', 'PENDING') } catch (error) { this.refundError = error.message || '退款申请提交失败' } finally { this.refundSubmitting = false } },
    canRequestRefund() { const refundStatus = String(this.paymentDetail && this.paymentDetail.latestRefund && this.paymentDetail.latestRefund.status || '').toUpperCase(); return this.paymentDetail && this.paymentDetail.paymentStatus === 'PAID' && ['', 'REJECTED'].includes(refundStatus) },
    countdownText(order) { const remaining = this.remainingMilliseconds(order); if (remaining === null) return ''; if (remaining <= 0) return '订单状态更新中'; const totalSeconds = Math.ceil(remaining / 1000); const minutes = String(Math.floor(totalSeconds / 60)).padStart(2, '0'); const seconds = String(totalSeconds % 60).padStart(2, '0'); return `剩余 ${minutes}:${seconds}` },
    remainingMilliseconds(order) { if (this.statusOf(order).group !== 'pending' || !order.paymentExpireAt) return null; const expireAt = new Date(order.paymentExpireAt).getTime(); if (Number.isNaN(expireAt)) return null; return expireAt - this.nowTick },
    refreshCountdown() { this.nowTick = Date.now(); this.orders.filter(order => { const remaining = this.remainingMilliseconds(order); return remaining !== null && remaining <= 0 && !this.expiredOrderIds.includes(order.id) }).forEach(order => { this.expiredOrderIds.push(order.id); this.loadOrders() }) },
    orderCount(value) { return value === 'all' ? this.orders.length : this.orders.filter(order => this.statusOf(order).group === value).length },
    formatDateTime(value) { if (!value) return '时间待确认'; const date = new Date(value); return Number.isNaN(date.getTime()) ? String(value) : new Intl.DateTimeFormat('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', hour12: false }).format(date) },
    async handlePay(order) { this.payingOrderId = order.id; this.paymentError = ''; try { const response = await createAlipayTrade(order.id); submitAlipayForm(response.data && response.data.payForm) } catch (error) { this.paymentError = error.message || '支付宝收银台暂时无法打开，请稍后重试'; this.payingOrderId = null } },
    formatOrderDate(value) { return formatShortDate(value) }
  }
}
</script>

<style scoped>
.amount-card { min-width: 190px; display: grid; padding: 16px 20px; border-radius: 16px 16px 5px 16px; color: #fff; background: var(--lagoon-deep); box-shadow: 0 17px 35px -24px rgba(7,86,107,.9); }.amount-card span { color: rgba(255,255,255,.62); font-size: 9px; }.amount-card strong { margin-top: 5px; font: 750 22px var(--display); }.amount-card small { margin-top: 3px; color: #9fe3d5; font-size: 8px; }
.orders-card { overflow: hidden; border: 1px solid var(--line); border-radius: 8px 26px 26px 26px; background: rgba(255,255,255,.92); box-shadow: var(--shadow); }.orders-toolbar { min-height: 70px; display: flex; align-items: center; justify-content: space-between; gap: 20px; padding: 15px 22px; border-bottom: 1px solid var(--line); }.order-tabs { display: flex; gap: 5px; }.order-tabs button { padding: 9px 13px; border: 0; border-radius: 9px; color: var(--ink-soft); background: transparent; font-size: 10px; }.order-tabs button.active { color: var(--lagoon-deep); background: var(--mint); font-weight: 700; }.order-tabs span { margin-left: 6px; font: 8px var(--mono); }.order-search { width: 230px; height: 36px; display: flex; align-items: center; gap: 8px; padding: 0 11px; border: 1px solid var(--line); border-radius: 10px; }.order-search svg { width: 16px; fill: none; stroke: var(--ink-soft); stroke-width: 1.8; }.order-search input { min-width: 0; width: 100%; border: 0; outline: 0; background: transparent; font-size: 10px; }
.payment-message { margin: 16px 22px 0; padding: 10px 12px; border-left: 3px solid var(--coral); color: #9d3f2a; background: #fff0ec; font-size: 10px; }.orders-state { min-height: 330px; display: flex; align-items: center; justify-content: center; gap: 11px; color: var(--ink-soft); font-size: 12px; }.loading-ring { width: 17px; height: 17px; border: 2px solid rgba(14,116,144,.18); border-top-color: var(--lagoon); border-radius: 50%; animation: spin .7s linear infinite; }@keyframes spin { to { transform: rotate(360deg); } }
.orders-list { padding: 0 22px; list-style: none; }.order-item { padding: 20px 0; border-bottom: 1px solid var(--line); }.order-item:last-child { border-bottom: 0; }.order-topline { display: flex; align-items: center; gap: 13px; margin-bottom: 11px; color: var(--ink-soft); font-size: 9px; }.order-topline > span { color: var(--lagoon); font: 650 8px var(--mono); letter-spacing: .12em; }.order-topline b { margin-left: auto; padding: 5px 8px; border-radius: 999px; font-size: 8px; }.order-topline b.pending { color: #976126; background: #fff1d2; }.order-topline b.paid { color: #267c63; background: #e2f5eb; }.order-topline b.closed { color: #6f7f89; background: #edf1f3; }
.order-content { position: relative; min-height: 84px; display: grid; grid-template-columns: 66px minmax(0,1fr) 130px 80px; align-items: center; gap: 18px; padding: 14px 16px; border: 1px solid #e7eef2; border-left: 3px solid var(--lagoon); border-radius: 5px 15px 15px 5px; background: #fbfdfe; }.course-mark { width: 64px; height: 54px; display: grid; place-content: center; border-radius: 11px 11px 3px 11px; color: var(--lagoon-deep); background: var(--mint); text-align: center; }.course-mark span { font: 800 16px var(--display); }.course-mark small { margin-top: 1px; font: 7px var(--mono); letter-spacing: .12em; }.order-name { min-width: 0; }.order-name h2 { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font: 700 14px var(--display); }.order-name p { margin-top: 7px; overflow: hidden; color: var(--ink-soft); text-overflow: ellipsis; white-space: nowrap; font-size: 9px; }.order-price { display: flex; flex-direction: column; align-items: flex-end; }.order-price small { color: var(--ink-soft); font-size: 8px; }.order-price strong { margin-top: 4px; font: 750 16px var(--display); }.payment-countdown { position: absolute; right: 16px; top: 7px; color: #976126; font: 700 8px var(--mono); }.pay-button { height: 38px; border: 0; border-radius: 10px; color: #fff; background: var(--coral); box-shadow: 0 10px 20px -14px #9d3f2a; font-size: 10px; font-weight: 700; }.pay-button:disabled { opacity: .5; cursor: wait; }.complete-label { justify-self: center; color: #55957d; font-size: 10px; font-weight: 700; }
.order-help { display: flex; align-items: center; gap: 13px; margin-top: 18px; padding: 18px 22px; border: 1px solid var(--line); border-radius: 16px; background: rgba(255,255,255,.72); }.order-help > span { width: 32px; height: 32px; display: grid; place-items: center; border: 1px solid var(--lagoon); border-radius: 50%; color: var(--lagoon); font: 700 12px var(--mono); }.order-help strong { font-size: 11px; }.order-help p { margin-top: 4px; color: var(--ink-soft); font-size: 9px; }
.complete-actions { display: grid; justify-items: center; gap: 5px; }.detail-button { border: 0; color: var(--lagoon); background: transparent; font-size: 9px; font-weight: 700; }.detail-overlay { position: fixed; z-index: 30; inset: 0; display: grid; place-items: center; padding: 20px; background: rgba(10, 37, 54, .42); }.payment-detail-dialog { width: min(560px, 100%); max-height: min(680px, 100%); overflow: auto; padding: 24px; border: 1px solid var(--line); border-radius: 8px; background: #fff; box-shadow: 0 24px 60px rgba(8, 42, 57, .3); }.payment-detail-dialog > header { display: flex; align-items: flex-start; justify-content: space-between; gap: 18px; }.payment-detail-dialog h2 { margin-top: 5px; font: 700 20px var(--display); }.dialog-close { width: 28px; height: 28px; border: 1px solid var(--line); border-radius: 5px; color: var(--ink-soft); background: #fff; font-size: 20px; line-height: 1; }.detail-state { min-height: 180px; display: grid; place-items: center; color: var(--ink-soft); font-size: 12px; }.detail-error,.refund-error { color: #9d3f2a; }.detail-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 14px 20px; margin: 22px 0; }.detail-grid div { min-width: 0; }.detail-grid dt { color: var(--ink-soft); font-size: 9px; }.detail-grid dd { margin-top: 5px; overflow-wrap: anywhere; color: var(--ink); font-size: 11px; }.refund-record { margin-top: 18px; padding: 14px; border-left: 3px solid var(--lagoon); background: #f5fafb; }.refund-record h3 { font-size: 11px; }.refund-record p,.refund-record small { display: block; margin-top: 7px; color: var(--ink-soft); font-size: 10px; line-height: 1.6; }.refund-form { margin-top: 20px; padding-top: 18px; border-top: 1px solid var(--line); }.refund-form label { display: block; margin-bottom: 8px; font-size: 11px; font-weight: 700; }.refund-form textarea { width: 100%; min-height: 88px; padding: 10px; border: 1px solid var(--line); border-radius: 5px; resize: vertical; font: inherit; font-size: 11px; }.refund-error { margin-top: 8px; font-size: 10px; }.refund-button { margin-top: 12px; height: 36px; padding: 0 16px; border: 0; border-radius: 5px; color: #fff; background: var(--coral); font-size: 10px; font-weight: 700; }.refund-button:disabled { opacity: .55; }
@media (max-width: 720px) { .amount-card { align-self: stretch; }.orders-toolbar { align-items: stretch; flex-direction: column; }.order-search { width: 100%; }.order-content { grid-template-columns: 56px minmax(0,1fr); }.course-mark { width: 54px; }.order-price { align-items: flex-start; grid-column: 2; }.pay-button,.complete-label { grid-column: 2; width: 100%; }.order-topline time { display: none; } }
</style>
