<template>
  <div id="app">
    <router-view />

    <transition name="auth-dialog">
      <div
        v-if="authExpiredVisible"
        class="auth-dialog-backdrop"
        role="presentation"
      >
        <section
          class="auth-dialog"
          role="alertdialog"
          aria-modal="true"
          aria-labelledby="auth-dialog-title"
          aria-describedby="auth-dialog-description"
        >
          <div class="auth-dialog-icon" aria-hidden="true">!</div>
          <p class="auth-dialog-kicker">SESSION EXPIRED</p>
          <h2 id="auth-dialog-title">登录状态已失效</h2>
          <p id="auth-dialog-description">
            为保护你的账号安全，本次登录已结束。请重新登录后继续刚才的操作。
          </p>
          <button type="button" @click="confirmAuthExpired">重新登录</button>
        </section>
      </div>
    </transition>
  </div>
</template>

<script>
import {
  resetStudentAuthExpiredNotification,
  STUDENT_AUTH_EXPIRED_EVENT
} from '@/utils/authEvents'

export default {
  name: 'App',
  data() {
    return {
      authExpiredVisible: false,
      authExpiredRedirect: '/'
    }
  },
  created() {
    window.addEventListener(STUDENT_AUTH_EXPIRED_EVENT, this.showAuthExpired)
  },
  beforeDestroy() {
    window.removeEventListener(STUDENT_AUTH_EXPIRED_EVENT, this.showAuthExpired)
  },
  methods: {
    showAuthExpired(event) {
      const redirect = event.detail && event.detail.redirect
      this.authExpiredRedirect = typeof redirect === 'string' ? redirect : '/'
      this.authExpiredVisible = true
      this.$nextTick(() => {
        const button = this.$el.querySelector('.auth-dialog button')
        if (button) button.focus()
      })
    },
    async confirmAuthExpired() {
      this.authExpiredVisible = false
      try {
        if (this.$route.path !== '/login') {
          await this.$router.replace({
            path: '/login',
            query: { redirect: this.authExpiredRedirect }
          })
        }
      } finally {
        resetStudentAuthExpiredNotification()
      }
    }
  }
}
</script>

<style scoped>
.auth-dialog-backdrop {
  position: fixed;
  z-index: 9999;
  inset: 0;
  display: grid;
  place-items: center;
  padding: 24px;
  background: rgba(7, 31, 43, .56);
  backdrop-filter: blur(5px);
}

.auth-dialog {
  width: min(100%, 420px);
  padding: 34px;
  border: 1px solid rgba(14, 116, 144, .15);
  border-radius: 22px;
  background: #fff;
  box-shadow: 0 28px 80px -28px rgba(5, 42, 55, .65);
  text-align: center;
}

.auth-dialog-icon {
  width: 52px;
  height: 52px;
  display: grid;
  place-items: center;
  margin: 0 auto 20px;
  border-radius: 50%;
  color: #fff;
  background: var(--coral);
  font: 800 24px var(--display);
  box-shadow: 0 0 0 9px rgba(255, 122, 89, .12);
}

.auth-dialog-kicker {
  color: var(--coral);
  font: 650 10px var(--mono);
  letter-spacing: .18em;
}

.auth-dialog h2 {
  margin-top: 9px;
  font: 750 27px/1.25 var(--display);
}

.auth-dialog p:last-of-type {
  margin-top: 13px;
  color: var(--ink-soft);
  font-size: 14px;
  line-height: 1.75;
}

.auth-dialog button {
  width: 100%;
  height: 48px;
  margin-top: 26px;
  border: 0;
  border-radius: 13px;
  color: #fff;
  background: var(--lagoon);
  font-weight: 700;
  transition: background .18s, transform .18s;
}

.auth-dialog button:hover {
  background: var(--lagoon-deep);
  transform: translateY(-1px);
}

.auth-dialog-enter-active,
.auth-dialog-leave-active {
  transition: opacity .18s ease;
}

.auth-dialog-enter-active .auth-dialog,
.auth-dialog-leave-active .auth-dialog {
  transition: transform .18s ease, opacity .18s ease;
}

.auth-dialog-enter,
.auth-dialog-leave-to {
  opacity: 0;
}

.auth-dialog-enter .auth-dialog,
.auth-dialog-leave-to .auth-dialog {
  opacity: 0;
  transform: translateY(10px) scale(.98);
}
</style>
