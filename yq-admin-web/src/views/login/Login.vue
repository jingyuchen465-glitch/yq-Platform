<template>
  <div class="login-page">
    <!-- 左侧品牌面板 -->
    <div class="login-brand">
      <div class="brand-inner">
        <div class="seal-wrap">
          <span class="seal">YQ</span>
          <span class="seal-glow"></span>
        </div>
        <h1 class="brand-title">YQ 管理台</h1>
        <p class="brand-sub">OPERATION CONSOLE</p>
        <div class="brand-line"></div>
        <p class="brand-desc">权限体系 · 用户管理 · 角色控制</p>
      </div>
      <div class="brand-foot mono">v1.0 · RBAC Engine</div>
    </div>

    <!-- 右侧表单区 -->
    <div class="login-form-side">
      <div class="form-card">
        <div class="form-head">
          <p class="form-eyebrow">SECURE ACCESS</p>
          <h2 class="form-title">登录到控制台</h2>
          <p class="form-hint">使用管理员账号进入权限管理系统</p>
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
            {{ loading ? '验证中…' : '登 录' }}
          </el-button>
        </el-form>
      </div>

      <p class="form-foot mono">YQ Admin · localhost:8080</p>
    </div>
  </div>
</template>

<script>
import { login } from '@/api/auth'

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
      loading: false
    }
  },
  methods: {
    handleLogin() {
      this.$refs.loginForm.validate(async valid => {
        if (!valid) return
        this.loading = true
        try {
          const res = await login(this.form)
          const { token, signSecret, userDetailRes } = res.data
          // 持久化登录态
          localStorage.setItem('yq_token', token)
          localStorage.setItem('yq_sign_secret', signSecret)
          localStorage.setItem('yq_user', JSON.stringify(userDetailRes))
          this.$message.success('登录成功')
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

/* ---------- 左侧品牌面板 ---------- */
.login-brand {
  width: 380px;
  flex: none;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  background:
    radial-gradient(ellipse 60% 50% at 50% 42%, rgba(30, 75, 59, .55) 0%, transparent 70%),
    linear-gradient(180deg, rgba(255,255,255,.03) 0%, rgba(255,255,255,0) 260px),
    var(--pine);
  position: relative;
  overflow: hidden;
}
/* 纹理装饰线 */
.login-brand::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    repeating-linear-gradient(
      0deg,
      transparent,
      transparent 79px,
      rgba(255,255,255,.025) 79px,
      rgba(255,255,255,.025) 80px
    );
  pointer-events: none;
}

.brand-inner {
  position: relative;
  text-align: center;
  z-index: 1;
}

.seal-wrap {
  position: relative;
  display: inline-flex;
  margin-bottom: 28px;
}
.seal {
  width: 72px;
  height: 72px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--brass);
  color: var(--pine);
  font-family: var(--font-display);
  font-size: 26px;
  font-weight: 800;
  letter-spacing: .03em;
  border-radius: 16px;
  box-shadow: 0 8px 32px -8px rgba(185, 138, 47, .6);
}
.seal-glow {
  position: absolute;
  inset: -18px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(185, 138, 47, .18) 0%, transparent 70%);
  animation: glow-pulse 3.2s ease-in-out infinite;
}
@keyframes glow-pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: .5; transform: scale(1.12); }
}

.brand-title {
  font-family: var(--font-display);
  font-size: 26px;
  font-weight: 800;
  color: #F2F6F1;
  letter-spacing: .04em;
  margin-bottom: 8px;
}
.brand-sub {
  font-family: var(--font-mono);
  font-size: 10px;
  letter-spacing: .32em;
  color: #6E8578;
  margin-bottom: 24px;
}
.brand-line {
  width: 36px;
  height: 2px;
  background: var(--brass);
  margin: 0 auto 20px;
  border-radius: 1px;
}
.brand-desc {
  font-size: 13px;
  color: #8FA79A;
  letter-spacing: .12em;
}

.brand-foot {
  position: absolute;
  bottom: 24px;
  font-size: 10.5px;
  color: #5C7266;
  letter-spacing: .06em;
}

/* ---------- 右侧表单区 ---------- */
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
  font-size: 10.5px;
  color: var(--ink-3);
  letter-spacing: .04em;
}

/* ---------- 响应式 ---------- */
@media (max-width: 860px) {
  .login-brand { display: none; }
  .login-form-side { padding: 24px; }
}

/* ---------- 入场动效 ---------- */
.form-card {
  animation: card-in .4s ease both;
}
@keyframes card-in {
  from { opacity: 0; transform: translateY(14px); }
  to   { opacity: 1; transform: translateY(0); }
}

@media (prefers-reduced-motion: reduce) {
  .form-card { animation: none; }
  .seal-glow { animation: none; }
}
</style>
