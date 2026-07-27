<template>
  <main class="login-shell">
    <section class="journey-panel" aria-labelledby="journey-title">
      <header class="brand">
        <span class="brand-mark" aria-hidden="true">YQ</span>
        <div>
          <strong>YQ 学习中心</strong>
          <span>STUDENT JOURNEY</span>
        </div>
      </header>

      <div class="journey-copy">
        <p class="eyebrow">你的学习轨道</p>
        <h1 id="journey-title">从今天这一站，<br>继续向前。</h1>
        <p class="lead">登录后进入专属学习空间。课程、作业与学习记录将在这里汇合。</p>
      </div>

      <ol class="route-map" aria-label="学习中心使用步骤">
        <li class="active">
          <span class="route-node">01</span>
          <div><strong>验证身份</strong><small>使用预留手机号登录</small></div>
        </li>
        <li>
          <span class="route-node">02</span>
          <div><strong>进入学习</strong><small>查看你的课程与任务</small></div>
        </li>
        <li>
          <span class="route-node">03</span>
          <div><strong>留下进度</strong><small>每一步都有记录</small></div>
        </li>
      </ol>

      <div class="channel-badge">
        <span class="pulse" aria-hidden="true"></span>
        <div><strong>安全通道已就绪</strong><small>登录后自动保护每次请求</small></div>
      </div>
    </section>

    <section class="form-panel" aria-labelledby="login-title">
      <div class="mobile-brand">YQ <span>学习中心</span></div>
      <form class="login-card" novalidate @submit.prevent="handleLogin">
        <div class="form-heading">
          <p class="eyebrow">WELCOME BACK</p>
          <h2 id="login-title">回到你的学习空间</h2>
          <p>输入报名时预留的手机号和登录密码。</p>
        </div>

        <div v-if="errorMessage" class="error-message" role="alert">
          <span aria-hidden="true">!</span>{{ errorMessage }}
        </div>

        <label class="field">
          <span>手机号</span>
          <div class="input-wrap" :class="{ invalid: errors.phone }">
            <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M7 2h10a2 2 0 0 1 2 2v16a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2Zm5 17h.01M9 5h6"/></svg>
            <input
              v-model.trim="form.phone"
              type="tel"
              inputmode="numeric"
              autocomplete="username"
              maxlength="11"
              placeholder="请输入 11 位手机号"
              @blur="validatePhone"
            >
          </div>
          <small v-if="errors.phone" class="field-error">{{ errors.phone }}</small>
        </label>

        <label class="field">
          <span>密码</span>
          <div class="input-wrap" :class="{ invalid: errors.password }">
            <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M7 10V8a5 5 0 0 1 10 0v2m-9 0h8a2 2 0 0 1 2 2v7H6v-7a2 2 0 0 1 2-2Zm4 4v2"/></svg>
            <input
              v-model="form.password"
              :type="showPassword ? 'text' : 'password'"
              autocomplete="current-password"
              placeholder="请输入登录密码"
              @blur="validatePassword"
            >
            <button class="visibility-button" type="button" :aria-label="showPassword ? '隐藏密码' : '显示密码'" @click="showPassword = !showPassword">
              {{ showPassword ? '隐藏' : '显示' }}
            </button>
          </div>
          <small v-if="errors.password" class="field-error">{{ errors.password }}</small>
        </label>

        <button class="submit-button" type="submit" :disabled="loading">
          <span>{{ loading ? '正在进入学习空间' : '进入学习空间' }}</span>
          <svg v-if="!loading" viewBox="0 0 24 24" aria-hidden="true"><path d="m9 18 6-6-6-6"/></svg>
          <span v-else class="spinner" aria-hidden="true"></span>
        </button>

        <p class="support-copy">首次登录密码由老师创建账号时提供。如无法登录，请联系教务老师重置。</p>
      </form>
      <footer>YQ Student · 让每次学习都有清晰下一站</footer>
    </section>
  </main>
</template>

<script>
import { login } from '@/api/auth'
import { saveStudentSession } from '@/utils/session'

export default {
  name: 'StudentLogin',
  data() {
    return {
      form: { phone: '', password: '' },
      errors: { phone: '', password: '' },
      errorMessage: '',
      showPassword: false,
      loading: false
    }
  },
  methods: {
    validatePhone() {
      this.errors.phone = /^1[3-9]\d{9}$/.test(this.form.phone) ? '' : '请输入正确的 11 位手机号'
      return !this.errors.phone
    },
    validatePassword() {
      const length = this.form.password.length
      this.errors.password = length >= 6 && length <= 64 ? '' : '密码长度须为 6 到 64 位'
      return !this.errors.password
    },
    async handleLogin() {
      this.errorMessage = ''
      if (!this.validatePhone() || !this.validatePassword()) return
      this.loading = true
      try {
        const response = await login(this.form)
        saveStudentSession(response.data)
        const redirect = this.safeRedirect(this.$route.query.redirect)
        await this.$router.replace(redirect)
      } catch (error) {
        this.errorMessage = error.message || '登录未完成，请检查手机号和密码'
      } finally {
        this.loading = false
      }
    },
    safeRedirect(value) {
      if (typeof value !== 'string') return '/'
      const cleanPath = value.split(/[?#]/)[0]
      return /^\/(?![\\/])/.test(value) && cleanPath !== '/login' ? value : '/'
    }
  }
}
</script>

<style scoped>
.login-shell { min-height: 100vh; display: grid; grid-template-columns: minmax(440px, 1.05fr) minmax(480px, .95fr); background: var(--paper); }
.journey-panel { min-height: 100vh; position: relative; overflow: hidden; display: flex; flex-direction: column; padding: 54px clamp(46px, 7vw, 108px); color: #fff; background: linear-gradient(145deg, #07566b 0%, #0e7490 54%, #278ca0 100%); }
.journey-panel::after { content: ''; position: absolute; width: 600px; height: 600px; right: -330px; top: 9%; border: 88px solid rgba(217, 243, 238, .1); border-radius: 50%; }
.brand { display: flex; align-items: center; gap: 13px; position: relative; z-index: 1; }
.brand-mark { width: 46px; height: 46px; display: grid; place-items: center; border-radius: 15px 15px 4px 15px; color: var(--lagoon-deep); background: var(--mint); font: 800 16px var(--display); letter-spacing: .04em; }
.brand div { display: flex; flex-direction: column; gap: 2px; }
.brand strong { font: 700 17px var(--display); letter-spacing: .04em; }
.brand span:last-child { font: 10px var(--mono); letter-spacing: .22em; color: rgba(255, 255, 255, .58); }
.journey-copy { position: relative; z-index: 1; margin-top: clamp(60px, 10vh, 110px); max-width: 600px; }
.eyebrow { font: 600 11px var(--mono); letter-spacing: .2em; text-transform: uppercase; }
.journey-copy .eyebrow { color: #9fe3d5; }
.journey-copy h1 { margin-top: 15px; font: 750 clamp(42px, 5vw, 68px)/1.08 var(--display); letter-spacing: -.045em; }
.lead { max-width: 490px; margin-top: 24px; color: rgba(255, 255, 255, .72); font-size: 15px; line-height: 1.9; }
.route-map { position: relative; z-index: 1; list-style: none; display: flex; margin-top: auto; padding-top: 56px; }
.route-map::before { content: ''; position: absolute; left: 23px; right: 18%; top: 79px; height: 1px; background: rgba(255, 255, 255, .25); }
.route-map li { flex: 1; min-width: 0; position: relative; }
.route-node { width: 47px; height: 47px; position: relative; display: grid; place-items: center; border: 1px solid rgba(255, 255, 255, .35); border-radius: 50%; background: #0c6b80; color: rgba(255, 255, 255, .65); font: 11px var(--mono); }
.route-map .active .route-node { border-color: var(--coral); color: #fff; background: var(--coral); box-shadow: 0 0 0 8px rgba(255, 122, 89, .15); }
.route-map li div { margin-top: 17px; padding-right: 22px; display: flex; flex-direction: column; }
.route-map strong { font-size: 13px; }
.route-map small { margin-top: 4px; color: rgba(255, 255, 255, .48); font-size: 11px; }
.channel-badge { position: relative; z-index: 1; align-self: flex-start; display: flex; align-items: center; gap: 11px; margin-top: 42px; padding: 11px 16px; border: 1px solid rgba(255, 255, 255, .14); border-radius: 13px; background: rgba(4, 44, 56, .24); }
.channel-badge div { display: flex; flex-direction: column; }
.channel-badge strong { font-size: 11px; }
.channel-badge small { margin-top: 2px; color: rgba(255, 255, 255, .5); font-size: 10px; }
.pulse { width: 8px; height: 8px; border-radius: 50%; background: #8ee1b5; box-shadow: 0 0 0 5px rgba(142, 225, 181, .12); animation: pulse 2.2s ease-in-out infinite; }
@keyframes pulse { 50% { opacity: .45; transform: scale(.8); } }
.form-panel { min-height: 100vh; display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 58px clamp(34px, 7vw, 100px) 34px; }
.mobile-brand { display: none; }
.login-card { width: min(100%, 430px); animation: rise .5s ease both; }
@keyframes rise { from { opacity: 0; transform: translateY(18px); } }
.form-heading .eyebrow { color: var(--coral); }
.form-heading h2 { margin-top: 12px; font: 750 clamp(28px, 3vw, 38px)/1.2 var(--display); letter-spacing: -.035em; }
.form-heading > p:last-child { margin-top: 13px; color: var(--ink-soft); font-size: 14px; }
.error-message { display: flex; align-items: center; gap: 9px; margin-top: 24px; padding: 12px 14px; border-left: 3px solid var(--coral); background: #fff0ec; color: #9d3f2a; font-size: 13px; }
.error-message span { width: 19px; height: 19px; display: grid; place-items: center; border: 1px solid currentColor; border-radius: 50%; font: 700 11px var(--mono); }
.field { display: block; margin-top: 26px; }
.field > span { display: block; margin-bottom: 9px; font-size: 13px; font-weight: 650; }
.input-wrap { height: 52px; display: flex; align-items: center; gap: 11px; padding: 0 15px; border: 1px solid var(--line); border-radius: 13px; background: #fff; transition: border-color .18s, box-shadow .18s; }
.input-wrap:focus-within { border-color: var(--lagoon); box-shadow: 0 0 0 4px rgba(14, 116, 144, .09); }
.input-wrap.invalid { border-color: var(--coral); }
.input-wrap svg { width: 20px; fill: none; stroke: var(--lagoon); stroke-width: 1.7; stroke-linecap: round; stroke-linejoin: round; }
.input-wrap input { min-width: 0; flex: 1; height: 100%; border: 0; outline: 0; color: var(--ink); background: transparent; font-size: 14px; }
.input-wrap input::placeholder { color: #9aafbd; }
.visibility-button { border: 0; color: var(--ink-soft); background: transparent; font-size: 12px; }
.field-error { display: block; margin-top: 7px; color: #b6452f; font-size: 12px; }
.submit-button { width: 100%; height: 54px; display: flex; align-items: center; justify-content: center; gap: 12px; margin-top: 32px; border: 0; border-radius: 14px; color: #fff; background: var(--lagoon); font-weight: 700; box-shadow: 0 15px 32px -18px rgba(14, 116, 144, .8); transition: transform .18s, background .18s; }
.submit-button:hover:not(:disabled) { transform: translateY(-2px); background: var(--lagoon-deep); }
.submit-button:disabled { cursor: wait; opacity: .75; }
.submit-button svg { width: 19px; fill: none; stroke: currentColor; stroke-width: 2; stroke-linecap: round; stroke-linejoin: round; }
.spinner { width: 18px; height: 18px; border: 2px solid rgba(255, 255, 255, .35); border-top-color: #fff; border-radius: 50%; animation: spin .8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
.support-copy { margin-top: 22px; padding-top: 20px; border-top: 1px solid var(--line); color: var(--ink-soft); font-size: 12px; line-height: 1.7; }
.form-panel footer { margin-top: auto; padding-top: 46px; color: #8ca3b2; font: 10px var(--mono); letter-spacing: .08em; }
@media (max-width: 940px) {
  .login-shell { grid-template-columns: 1fr; }
  .journey-panel { display: none; }
  .form-panel { padding: 38px 24px 26px; justify-content: flex-start; }
  .mobile-brand { width: min(100%, 430px); display: block; margin-bottom: clamp(55px, 12vh, 100px); color: var(--lagoon); font: 800 18px var(--display); }
  .mobile-brand span { color: var(--ink); font-size: 14px; }
  .form-panel footer { margin-top: auto; }
}
@media (max-width: 480px) {
  .form-panel { padding-inline: 20px; }
  .form-heading h2 { font-size: 29px; }
}
</style>
