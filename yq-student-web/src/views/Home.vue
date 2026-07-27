<template>
  <div class="home-page">
    <header class="topbar">
      <router-link class="brand" to="/" aria-label="YQ 学习中心首页">
        <span>YQ</span><strong>学习中心</strong>
      </router-link>
      <div class="student-menu">
        <span class="avatar">{{ initial }}</span>
        <div><strong>{{ profile.name || '同学' }}</strong><small>{{ maskedPhone }}</small></div>
        <button type="button" :disabled="loggingOut" @click="handleLogout">
          {{ loggingOut ? '退出中' : '退出登录' }}
        </button>
      </div>
    </header>

    <main class="home-content">
      <section class="welcome-card">
        <div class="welcome-copy">
          <p class="eyebrow">TODAY · 学习起点</p>
          <h1>{{ greeting }}，{{ profile.name || '同学' }}</h1>
          <p>你的学生身份已验证。课程与作业模块接入后，会从这里开始今天的学习路线。</p>
          <div class="identity-chip"><i></i>{{ statusText }} · 安全通道在线</div>
        </div>
        <div class="route-visual" aria-hidden="true">
          <span class="route-label">LEARNING ROUTE</span>
          <div class="orbit orbit-one"><i></i></div>
          <div class="orbit orbit-two"><i></i></div>
          <div class="center-mark"><b>YQ</b><small>START</small></div>
        </div>
      </section>

      <p v-if="errorMessage" class="page-message" role="alert">{{ errorMessage }}</p>

      <section class="dashboard-grid">
        <article class="profile-card">
          <header>
            <div><p class="eyebrow">STUDENT IDENTITY</p><h2>学生资料</h2></div>
            <span class="record-id">#{{ profile.id || '—' }}</span>
          </header>
          <dl>
            <div><dt>姓名</dt><dd>{{ profile.name || '未填写' }}</dd></div>
            <div><dt>手机号</dt><dd>{{ maskedPhone }}</dd></div>
            <div><dt>邮箱</dt><dd>{{ profile.email || '未填写' }}</dd></div>
            <div><dt>班级</dt><dd>{{ profile.classId ? `班级 #${profile.classId}` : '待分班' }}</dd></div>
            <div><dt>账号状态</dt><dd><span class="status-dot"></span>{{ statusText }}</dd></div>
          </dl>
        </article>

        <article class="next-card">
          <p class="eyebrow">NEXT STOP</p>
          <div class="next-number">01</div>
          <h2>学习功能正在接入</h2>
          <p>预订单查询已接入。确认课程与金额后，可直接前往支付宝收银台完成支付。</p>
          <div class="module-list">
            <span class="module-ready"><i></i>我的预订单</span><span>课程表</span><span>我的作业</span>
          </div>

          <div class="order-panel" aria-live="polite">
            <div class="order-panel-heading">
              <div><strong>我的预订单</strong><small>{{ orders.length }} 笔记录</small></div>
              <button v-if="orderError" type="button" @click="loadPrepaymentOrders">重新查询</button>
            </div>
            <p v-if="paymentError" class="payment-message" role="alert">{{ paymentError }}</p>

            <div v-if="ordersLoading" class="order-state">
              <span class="loading-ring" aria-hidden="true"></span>正在查询预订单…
            </div>
            <div v-else-if="orderError" class="order-state order-state-error" role="alert">
              {{ orderError }}
            </div>
            <div v-else-if="!orders.length" class="order-state">
              暂无预订单，销售老师录入后会显示在这里。
            </div>
            <ul v-else class="order-list">
              <li v-for="order in orders" :key="order.id" class="order-ticket">
                <div class="order-main">
                  <span class="order-id">PRE · {{ order.id }}</span>
                  <strong>{{ order.productName || '未命名课程' }}</strong>
                  <small>
                    {{ order.salespersonName ? `顾问 ${order.salespersonName}` : '课程顾问待确认' }}
                    · <time :datetime="order.createdAt">{{ formatOrderDate(order.createdAt) }}</time>
                  </small>
                </div>
                <div class="order-action">
                  <span><small>应付</small>¥{{ formatMoney(order.productPrice) }}</span>
                  <button
                    type="button"
                    :disabled="payingOrderId !== null"
                    @click="handlePay(order)"
                  >
                    {{ payingOrderId === order.id ? '跳转中…' : '去支付' }}
                  </button>
                </div>
              </li>
            </ul>
          </div>
        </article>
      </section>
    </main>
  </div>
</template>

<script>
import { getCurrentStudent, logout } from '@/api/auth'
import { createAlipayTrade, getCurrentStudentPrepaymentOrders } from '@/api/prepayment'
import {
  clearStudentSession,
  getStudentProfile,
  saveStudentProfile
} from '@/utils/session'

const STATUS_TEXT = {
  TEMPORARY: '临时学员',
  ATSCHOOL: '在校学习',
  GRADUATE: '已毕业',
  WITCHDRAWAL: '已退学'
}

export default {
  name: 'StudentHome',
  data() {
    return {
      profile: getStudentProfile(),
      errorMessage: '',
      loggingOut: false,
      orders: [],
      ordersLoading: true,
      orderError: '',
      paymentError: '',
      payingOrderId: null
    }
  },
  computed: {
    initial() {
      return (this.profile.name || '同').trim().slice(0, 1)
    },
    maskedPhone() {
      const phone = this.profile.phone || ''
      return /^\d{11}$/.test(phone) ? `${phone.slice(0, 3)}****${phone.slice(-4)}` : '手机号未填写'
    },
    statusText() {
      return STATUS_TEXT[this.profile.status] || '资料已建立'
    },
    greeting() {
      const hour = new Date().getHours()
      if (hour < 11) return '早上好'
      if (hour < 14) return '中午好'
      if (hour < 18) return '下午好'
      return '晚上好'
    }
  },
  mounted() {
    this.refreshProfile()
    this.loadPrepaymentOrders()
  },
  methods: {
    async refreshProfile() {
      try {
        const response = await getCurrentStudent()
        this.profile = response.data || {}
        saveStudentProfile(this.profile)
      } catch (error) {
        this.errorMessage = error.message || '学生资料暂时无法更新'
      }
    },
    async loadPrepaymentOrders() {
      this.ordersLoading = true
      this.orderError = ''
      this.paymentError = ''
      try {
        const response = await getCurrentStudentPrepaymentOrders()
        this.orders = Array.isArray(response.data) ? response.data : []
      } catch (error) {
        this.orders = []
        this.orderError = error.message || '预订单暂时无法查询，请稍后重试'
      } finally {
        this.ordersLoading = false
      }
    },
    async handlePay(order) {
      const amount = Number(order.productPrice)
      if (!Number.isFinite(amount) || amount <= 0) {
        this.paymentError = '当前预订单金额无效，请联系课程顾问确认'
        return
      }

      this.payingOrderId = order.id
      this.paymentError = ''
      try {
        const response = await createAlipayTrade({
          outTradeNo: order.outTradeNo,
          totalAmount: order.productPrice,
          subject: `${order.productName || '课程'}预订单`,
          body: `YQ 学习中心预订单 #${order.id}`,
          timeoutExpress: '30m'
        })
        const payForm = response.data && response.data.payForm
        this.submitAlipayForm(payForm)
      } catch (error) {
        this.paymentError = error.message || '支付宝收银台暂时无法打开，请稍后重试'
        this.payingOrderId = null
      }
    },
    submitAlipayForm(payForm) {
      if (typeof payForm !== 'string' || !payForm.trim()) {
        throw new Error('支付表单为空，请稍后重试')
      }

      const parsed = new DOMParser().parseFromString(payForm, 'text/html')
      const sourceForm = parsed.querySelector('form')
      if (!sourceForm) throw new Error('支付表单格式无效，请稍后重试')

      const action = new URL(sourceForm.getAttribute('action') || '', window.location.href)
      const hostname = action.hostname.toLowerCase()
      const isAlipayHost = hostname === 'alipay.com' || hostname.endsWith('.alipay.com') ||
        hostname === 'alipaydev.com' || hostname.endsWith('.alipaydev.com')
      if (action.protocol !== 'https:' || !isAlipayHost) {
        throw new Error('支付地址校验失败，请稍后重试')
      }

      const form = document.importNode(sourceForm, true)
      form.style.display = 'none'
      form.removeAttribute('target')
      document.body.appendChild(form)
      HTMLFormElement.prototype.submit.call(form)
    },
    formatMoney(value) {
      const amount = Number(value)
      return Number.isFinite(amount)
        ? new Intl.NumberFormat('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(amount)
        : '0.00'
    },
    formatOrderDate(value) {
      if (!value) return '时间待确认'
      const date = new Date(value)
      if (Number.isNaN(date.getTime())) return '时间待确认'
      return new Intl.DateTimeFormat('zh-CN', { month: '2-digit', day: '2-digit' }).format(date)
    },
    async handleLogout() {
      this.loggingOut = true
      try {
        await logout()
      } catch (error) {
        // 无论服务端会话是否已失效，本地都应结束当前登录状态。
      } finally {
        clearStudentSession()
        this.loggingOut = false
        await this.$router.replace('/login')
      }
    }
  }
}
</script>

<style scoped>
.home-page { min-height: 100vh; background: linear-gradient(180deg, #eef7f8 0, var(--paper) 360px); }
.topbar { height: 76px; display: flex; align-items: center; justify-content: space-between; padding: 0 clamp(24px, 6vw, 88px); border-bottom: 1px solid rgba(14, 116, 144, .12); background: rgba(247, 250, 252, .78); backdrop-filter: blur(14px); }
.brand { display: flex; align-items: center; gap: 11px; color: var(--ink); text-decoration: none; }
.brand span { width: 39px; height: 39px; display: grid; place-items: center; border-radius: 13px 13px 4px 13px; color: #fff; background: var(--lagoon); font: 800 13px var(--display); }
.brand strong { font: 750 15px var(--display); }
.student-menu { display: flex; align-items: center; gap: 10px; }
.avatar { width: 38px; height: 38px; display: grid; place-items: center; border-radius: 50%; color: var(--lagoon-deep); background: var(--mint); font-weight: 750; }
.student-menu div { display: flex; flex-direction: column; min-width: 85px; }
.student-menu strong { font-size: 12px; }
.student-menu small { margin-top: 2px; color: var(--ink-soft); font: 9px var(--mono); }
.student-menu button { margin-left: 14px; padding: 8px 12px; border: 1px solid var(--line); border-radius: 9px; color: var(--ink-soft); background: #fff; font-size: 11px; }
.home-content { width: min(1180px, calc(100% - 48px)); margin: 0 auto; padding: 54px 0 70px; }
.welcome-card { min-height: 320px; position: relative; overflow: hidden; display: grid; grid-template-columns: 1.1fr .9fr; border-radius: 28px 28px 8px 28px; color: #fff; background: linear-gradient(128deg, #07566b, #0e7490 72%); box-shadow: var(--shadow); }
.welcome-copy { align-self: center; padding: clamp(38px, 6vw, 72px); position: relative; z-index: 1; }
.eyebrow { color: var(--coral); font: 650 10px var(--mono); letter-spacing: .2em; }
.welcome-copy .eyebrow { color: #9fe3d5; }
.welcome-copy h1 { margin-top: 14px; font: 750 clamp(34px, 4vw, 52px)/1.16 var(--display); letter-spacing: -.04em; }
.welcome-copy > p:not(.eyebrow) { max-width: 570px; margin-top: 18px; color: rgba(255, 255, 255, .67); font-size: 14px; line-height: 1.9; }
.identity-chip { display: inline-flex; align-items: center; gap: 9px; margin-top: 28px; padding: 9px 13px; border: 1px solid rgba(255, 255, 255, .15); border-radius: 999px; background: rgba(2, 41, 53, .18); font-size: 11px; }
.identity-chip i, .status-dot { width: 7px; height: 7px; border-radius: 50%; background: #83deb0; box-shadow: 0 0 0 4px rgba(131, 222, 176, .12); }
.route-visual { min-height: 320px; position: relative; }
.route-label { position: absolute; right: 26px; top: 25px; color: rgba(255, 255, 255, .35); font: 9px var(--mono); letter-spacing: .18em; }
.orbit { position: absolute; border: 1px solid rgba(255, 255, 255, .18); border-radius: 50%; }
.orbit-one { width: 280px; height: 280px; right: 5%; top: 20px; }
.orbit-two { width: 190px; height: 190px; right: calc(5% + 45px); top: 65px; }
.orbit i { position: absolute; width: 12px; height: 12px; border: 3px solid #fff; border-radius: 50%; background: var(--coral); }
.orbit-one i { left: 21px; top: 58px; }
.orbit-two i { right: 9px; bottom: 45px; background: #9fe3d5; }
.center-mark { width: 100px; height: 100px; position: absolute; right: calc(5% + 90px); top: 110px; display: grid; place-content: center; border-radius: 50%; text-align: center; color: var(--lagoon-deep); background: var(--mint); }
.center-mark b { font: 800 24px var(--display); }
.center-mark small { margin-top: 2px; font: 8px var(--mono); letter-spacing: .18em; }
.page-message { margin-top: 20px; padding: 12px 16px; border-left: 3px solid var(--coral); background: #fff0ec; color: #9d3f2a; font-size: 13px; }
.dashboard-grid { display: grid; grid-template-columns: .9fr 1.1fr; gap: 22px; margin-top: 24px; }
.profile-card, .next-card { min-height: 325px; padding: 30px; border: 1px solid var(--line); background: #fff; box-shadow: 0 18px 50px -44px rgba(16, 42, 67, .5); }
.profile-card { border-radius: 8px 24px 24px 24px; }
.profile-card header { display: flex; align-items: flex-start; justify-content: space-between; }
.profile-card h2, .next-card h2 { margin-top: 7px; font: 750 22px var(--display); letter-spacing: -.025em; }
.record-id { color: var(--lagoon); font: 600 11px var(--mono); }
.profile-card dl { display: grid; grid-template-columns: repeat(2, 1fr); gap: 0 30px; margin-top: 24px; }
.profile-card dl div { padding: 14px 0; border-top: 1px solid #e8eff3; }
.profile-card dt { color: var(--ink-soft); font-size: 11px; }
.profile-card dd { display: flex; align-items: center; gap: 8px; margin-top: 6px; font-size: 13px; font-weight: 650; }
.next-card { position: relative; overflow: hidden; border-radius: 24px 8px 24px 24px; background: var(--sky); }
.next-number { position: absolute; right: -4px; top: -28px; color: rgba(14, 116, 144, .08); font: 800 120px var(--display); }
.next-card > *:not(.next-number) { position: relative; z-index: 1; }
.next-card > p:not(.eyebrow) { max-width: 390px; margin-top: 14px; color: var(--ink-soft); font-size: 13px; line-height: 1.8; }
.module-list { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 20px; }
.module-list span { padding: 8px 11px; border: 1px dashed rgba(14, 116, 144, .3); border-radius: 8px; color: var(--lagoon-deep); background: rgba(255, 255, 255, .5); font-size: 11px; }
.module-list .module-ready { display: inline-flex; align-items: center; gap: 7px; border-style: solid; background: #fff; font-weight: 700; }
.module-ready i { width: 6px; height: 6px; border-radius: 50%; background: #2bb381; box-shadow: 0 0 0 3px rgba(43, 179, 129, .12); }
.order-panel { margin-top: 20px; border-top: 1px solid rgba(14, 116, 144, .13); padding-top: 18px; }
.order-panel-heading { display: flex; align-items: center; justify-content: space-between; margin-bottom: 11px; }
.order-panel-heading > div { display: flex; align-items: baseline; gap: 9px; }
.order-panel-heading strong { font: 700 13px var(--display); }
.order-panel-heading small { color: var(--ink-soft); font: 9px var(--mono); }
.order-panel-heading button { border: 0; color: var(--lagoon); background: transparent; font-size: 11px; font-weight: 700; }
.payment-message { margin-bottom: 10px; padding: 9px 11px; border-radius: 8px; color: #9d3f2a; background: #fff5f2; font-size: 10px; line-height: 1.5; }
.order-state { min-height: 74px; display: flex; align-items: center; justify-content: center; gap: 10px; padding: 16px; border: 1px dashed rgba(14, 116, 144, .22); border-radius: 12px; color: var(--ink-soft); background: rgba(255, 255, 255, .44); text-align: center; font-size: 11px; line-height: 1.6; }
.order-state-error { color: #9d3f2a; border-color: rgba(255, 122, 89, .32); background: #fff5f2; }
.loading-ring { width: 15px; height: 15px; border: 2px solid rgba(14, 116, 144, .18); border-top-color: var(--lagoon); border-radius: 50%; animation: spin .75s linear infinite; }
.order-list { max-height: 300px; display: grid; gap: 9px; overflow-y: auto; list-style: none; scrollbar-width: thin; scrollbar-color: rgba(14, 116, 144, .25) transparent; }
.order-ticket { min-width: 0; display: grid; grid-template-columns: minmax(0, 1fr) auto; gap: 18px; align-items: center; padding: 14px 14px 14px 17px; border: 1px solid rgba(14, 116, 144, .13); border-left: 3px solid var(--lagoon); border-radius: 6px 14px 14px 6px; background: rgba(255, 255, 255, .88); box-shadow: 0 10px 24px -23px rgba(7, 86, 107, .9); }
.order-main { min-width: 0; display: flex; flex-direction: column; }
.order-main .order-id { color: var(--lagoon); font: 650 8px var(--mono); letter-spacing: .14em; }
.order-main strong { margin-top: 5px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 12px; }
.order-main small { margin-top: 5px; overflow: hidden; color: var(--ink-soft); text-overflow: ellipsis; white-space: nowrap; font-size: 9px; }
.order-action { display: flex; align-items: center; gap: 11px; }
.order-action > span { display: flex; flex-direction: column; color: var(--ink); font: 750 14px var(--display); text-align: right; }
.order-action > span small { margin-bottom: 2px; color: var(--ink-soft); font: 8px var(--body); }
.order-action button { min-width: 68px; padding: 9px 11px; border: 0; border-radius: 9px; color: #fff; background: var(--coral); box-shadow: 0 8px 18px -11px #9d3f2a; font-size: 11px; font-weight: 750; transition: transform .18s ease, box-shadow .18s ease, opacity .18s ease; }
.order-action button:hover:not(:disabled) { transform: translateY(-1px); box-shadow: 0 10px 20px -10px #9d3f2a; }
.order-action button:disabled { cursor: wait; opacity: .55; }
@keyframes spin { to { transform: rotate(360deg); } }
@media (max-width: 800px) {
  .topbar { padding-inline: 20px; }
  .student-menu div { display: none; }
  .student-menu button { margin-left: 2px; }
  .home-content { width: min(100% - 32px, 620px); padding-top: 28px; }
  .welcome-card { grid-template-columns: 1fr; }
  .route-visual { min-height: 190px; opacity: .85; }
  .orbit-one { width: 220px; height: 220px; top: -10px; right: 50%; transform: translateX(50%); }
  .orbit-two { width: 145px; height: 145px; top: 28px; right: 50%; transform: translateX(50%); }
  .center-mark { width: 78px; height: 78px; top: 61px; right: 50%; transform: translateX(50%); }
  .route-label { display: none; }
  .dashboard-grid { grid-template-columns: 1fr; }
}
@media (max-width: 500px) {
  .welcome-copy { padding: 34px 26px 22px; }
  .welcome-copy h1 { font-size: 34px; }
  .profile-card, .next-card { padding: 24px; }
  .profile-card dl { grid-template-columns: 1fr; }
  .order-ticket { grid-template-columns: 1fr; gap: 12px; }
  .order-action { justify-content: space-between; }
  .order-action > span { text-align: left; }
}
</style>
