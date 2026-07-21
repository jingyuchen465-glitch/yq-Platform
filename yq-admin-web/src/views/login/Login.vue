<template>
  <div class="login-page">
    <!-- 左侧：安全协议面板 -->
    <div class="login-brand">
      <div class="brand-top">
        <span class="seal">YQ</span>
        <div class="brand-name">
          <h1>YQ 管理台</h1>
          <p>OPERATION CONSOLE</p>
        </div>
      </div>

      <!-- 签名协议信封（页面加载时实时计算） -->
      <div class="protocol-card">
        <div class="protocol-head">
          <span class="protocol-label">REQUEST ENVELOPE</span>
          <span class="protocol-dot"></span>
        </div>
        <div class="protocol-body">
          <p class="proto-line proto-method">
            <span class="k">POST</span>
            <span class="v dim">/yq-admin/emp/**</span>
          </p>
          <p class="proto-line">
            <span class="k">Authorization</span>
            <span class="v">Bearer <i class="hl">••••</i></span>
          </p>
          <p class="proto-line">
            <span class="k">X-Timestamp</span>
            <span class="v">{{ demo.timestamp }}</span>
          </p>
          <p class="proto-line">
            <span class="k">X-Nonce</span>
            <span class="v">{{ demo.nonce }}</span>
          </p>
          <p class="proto-line">
            <span class="k">X-Sign</span>
            <span class="v brass">{{ demo.sign }}</span>
          </p>
        </div>
        <div class="protocol-foot">HMAC-SHA256 · 5min TTL · SETNX 防重放</div>
      </div>

      <p class="brand-desc">权限体系 · 用户管理 · 角色控制</p>
      <div class="brand-foot">v1.0 · RBAC ENGINE · SIGNED CHANNEL</div>
    </div>

    <!-- 右侧：表单区 -->
    <div class="login-form-side">
      <div class="form-card">
        <div class="form-head">
          <p class="form-eyebrow">SECURE ACCESS</p>
          <h2 class="form-title">登录到控制台</h2>
          <p class="form-hint">登录后所有请求自动携带签名，通道即时生效</p>
        </div>

        <el-form
          ref="loginForm"
          :model="form"
          :rules="rules"
          label-position="top"
          @submit.native.prevent="handleLogin"
        >
          <el-form-item label="用户名" prop="username">
            <el-input
              v-model="form.username"
              prefix-icon="el-icon-user"
              placeholder="请输入用户名"
              autocomplete="username"
            />
          </el-form-item>

          <el-form-item label="密码" prop="password">
            <el-input
              v-model="form.password"
              prefix-icon="el-icon-lock"
              type="password"
              placeholder="请输入密码"
              autocomplete="current-password"
              show-password
              @keyup.enter.native="handleLogin"
            />
          </el-form-item>

          <el-button
            type="primary"
            class="login-btn"
            :loading="loading"
            @click="handleLogin"
          >
            {{ loading ? '签署通道建立中…' : '登 录' }}
          </el-button>
        </el-form>
      </div>

      <p class="form-foot">YQ Admin · 签名密钥登录时下发，无需手动配置</p>
    </div>
  </div>
</template>

<script>
import { login } from '@/api/auth'
import { buildRequestSign } from '@/utils/sign'

export default {
  name: 'Login',
  data() {
    return {
      form: {
        username: '',
        password: ''
      },
      rules: {
        username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
        password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
      },
      loading: false,
      // 协议卡片展示值（页面加载时用真实签名算法实时生成）
      demo: {
        timestamp: '—',
        nonce: '—',
        sign: '—'
      }
    }
  },
  mounted() {
    this.renderProtocolDemo()
  },
  methods: {
    /** 用真实签名工具生成一组展示值，让协议卡呈现的是活数据 */
    async renderProtocolDemo() {
      try {
        const { timestamp, nonce, sign } = await buildRequestSign(
          'POST',
          '/yq-admin/emp/sysUser/page',
          'page=1&size=10',
          'demo-secret-key'
        )
        this.demo = { timestamp, nonce, sign: sign.slice(0, 24) + '…' }
      } catch (e) {
        // 非安全上下文（http）降级为静态占位
        this.demo = {
          timestamp: String(Date.now()),
          nonce: 'a3f8c1e90b7d4265',
          sign: 'kQ9x2Wm7vJ4pR8sT1uN6…'
        }
      }
    },
    handleLogin() {
      this.$refs.loginForm.validate(async valid => {
        if (!valid) return
        this.loading = true
        try {
          const res = await login(this.form)
          const { token, signSecret, userDetailRes } = res.data
          // 持久化登录态：token + 签名密钥
          localStorage.setItem('yq_token', token)
          localStorage.setItem('yq_sign_secret', signSecret)
          localStorage.setItem('yq_user', JSON.stringify(userDetailRes))
          this.$message.success('登录成功，签名通道已建立')
          this.$router.push('/')
        } catch (e) {
          // 错误已由拦截器统一提示
        } finally {
          this.loading = false
        }
      })
    }
  }
}
</script>

<style scoped>
.login-page {
  height: 100%;
  display: flex;
}

/* ========== 左侧：协议面板 ========== */
.login-brand {
  width: 400px;
  flex: none;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  gap: 0;
  padding: 48px 36px;
  background:
    radial-gradient(ellipse 70% 46% at 50% 38%, rgba(30, 75, 59, .5) 0%, transparent 72%),
    var(--pine);
  position: relative;
  overflow: hidden;
}
/* 网格纹理：暗示工程蓝图 */
.login-brand::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    repeating-linear-gradient(0deg, transparent, transparent 63px, rgba(255,255,255,.022) 63px, rgba(255,255,255,.022) 64px),
    repeating-linear-gradient(90deg, transparent, transparent 63px, rgba(255,255,255,.022) 63px, rgba(255,255,255,.022) 64px);
  pointer-events: none;
}

/* 品牌行 */
.brand-top {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 14px;
  align-self: flex-start;
  margin-bottom: 36px;
}
.seal {
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--brass);
  color: var(--pine);
  font-family: var(--font-display);
  font-size: 17px;
  font-weight: 800;
  border-radius: 10px;
  box-shadow: 0 6px 24px -6px rgba(185, 138, 47, .55);
  flex: none;
}
.brand-name h1 {
  font-family: var(--font-display);
  font-size: 19px;
  font-weight: 800;
  color: #F2F6F1;
  letter-spacing: .03em;
  line-height: 1.2;
}
.brand-name p {
  font-family: var(--font-mono);
  font-size: 9px;
  letter-spacing: .3em;
  color: #6E8578;
  margin-top: 3px;
}

/* 协议信封卡片 —— 页面签名元素 */
.protocol-card {
  position: relative;
  z-index: 1;
  width: 100%;
  background: rgba(255, 255, 255, .04);
  border: 1px solid rgba(255, 255, 255, .09);
  border-radius: 12px;
  backdrop-filter: blur(6px);
  overflow: hidden;
}
.protocol-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 18px;
  border-bottom: 1px solid rgba(255, 255, 255, .07);
}
.protocol-label {
  font-family: var(--font-mono);
  font-size: 9.5px;
  font-weight: 600;
  letter-spacing: .26em;
  color: #7FA392;
}
.protocol-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--jade);
  box-shadow: 0 0 8px rgba(23, 126, 99, .8);
  animation: dot-breathe 2.4s ease-in-out infinite;
}
@keyframes dot-breathe {
  0%, 100% { opacity: 1; }
  50% { opacity: .35; }
}

.protocol-body {
  padding: 16px 18px 14px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.proto-line {
  display: flex;
  align-items: baseline;
  gap: 12px;
  font-family: var(--font-mono);
  font-size: 11.5px;
  line-height: 1.5;
  animation: line-in .45s ease both;
}
.proto-line:nth-child(1) { animation-delay: .10s; }
.proto-line:nth-child(2) { animation-delay: .22s; }
.proto-line:nth-child(3) { animation-delay: .34s; }
.proto-line:nth-child(4) { animation-delay: .46s; }
.proto-line:nth-child(5) { animation-delay: .58s; }
@keyframes line-in {
  from { opacity: 0; transform: translateX(-8px); }
  to   { opacity: 1; transform: translateX(0); }
}
.proto-line .k {
  flex: none;
  width: 108px;
  color: #7FA392;
  font-weight: 500;
}
.proto-line .v {
  color: #D4E2DA;
  word-break: break-all;
}
.proto-line .v.dim { color: #5C7266; }
.proto-line .v .hl {
  font-style: normal;
  color: var(--brass);
}
.proto-line .v.brass { color: #D9A94E; }
.proto-method .k {
  color: #F2F6F1;
  font-weight: 700;
}

.protocol-foot {
  padding: 10px 18px;
  border-top: 1px solid rgba(255, 255, 255, .07);
  font-family: var(--font-mono);
  font-size: 9px;
  letter-spacing: .14em;
  color: #5C7266;
}

.brand-desc {
  position: relative;
  z-index: 1;
  margin-top: 32px;
  font-size: 13px;
  color: #8FA79A;
  letter-spacing: .14em;
}
.brand-foot {
  position: absolute;
  bottom: 22px;
  font-family: var(--font-mono);
  font-size: 9.5px;
  letter-spacing: .12em;
  color: #4E6357;
}

/* ========== 右侧：表单区 ========== */
.login-form-side {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: var(--paper);
  padding: 40px;
  position: relative;
}

.form-card {
  width: 100%;
  max-width: 380px;
  background: var(--card);
  border: 1px solid var(--line-soft);
  border-radius: 12px;
  box-shadow: var(--shadow-card);
  padding: 36px 32px 32px;
  animation: card-in .4s ease both;
}
@keyframes card-in {
  from { opacity: 0; transform: translateY(14px); }
  to   { opacity: 1; transform: translateY(0); }
}

.form-head {
  margin-bottom: 28px;
}
.form-eyebrow {
  font-family: var(--font-mono);
  font-size: 10px;
  font-weight: 600;
  letter-spacing: .24em;
  color: var(--brass-ink);
  margin-bottom: 10px;
}
.form-eyebrow::before {
  content: '';
  display: inline-block;
  width: 16px;
  height: 2px;
  background: var(--brass);
  vertical-align: middle;
  margin-right: 8px;
  transform: translateY(-1px);
}
.form-title {
  font-family: var(--font-display);
  font-size: 22px;
  font-weight: 800;
  color: var(--ink);
  margin-bottom: 8px;
}
.form-hint {
  font-size: 13px;
  color: var(--ink-3);
}

/* 表单控件微调 */
.form-card .el-form-item {
  margin-bottom: 20px;
}
.form-card .el-form-item__label {
  font-size: 13px;
  padding-bottom: 6px;
}
.form-card .el-input__inner {
  height: 42px;
  line-height: 42px;
}

.login-btn {
  width: 100%;
  height: 44px;
  font-size: 15px;
  font-weight: 700;
  letter-spacing: .12em;
  margin-top: 6px;
}

.form-foot {
  position: absolute;
  bottom: 24px;
  font-family: var(--font-mono);
  font-size: 10.5px;
  color: var(--ink-3);
  letter-spacing: .04em;
}

/* ========== 响应式 ========== */
@media (max-width: 860px) {
  .login-brand { display: none; }
  .login-form-side { padding: 24px; }
}

@media (prefers-reduced-motion: reduce) {
  .form-card { animation: none; }
  .proto-line { animation: none; }
  .protocol-dot { animation: none; }
}
</style>
