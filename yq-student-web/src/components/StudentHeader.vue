<template>
  <header class="student-header">
    <router-link class="brand" to="/" aria-label="YQ 学习中心首页">
      <span class="brand-mark">YQ</span>
      <strong>学习中心</strong>
    </router-link>

    <nav class="main-nav" aria-label="学生端主导航">
      <router-link to="/schedule">
        <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M7 3v3m10-3v3M4 9h16M5 5h14a1 1 0 0 1 1 1v13H4V6a1 1 0 0 1 1-1Zm3 8h3m2 0h3m-8 3h3"/></svg>
        <span><strong>课程表</strong><small>本周安排</small></span>
      </router-link>
      <router-link to="/homework">
        <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M6 3h9l3 3v15H6V3Zm8 0v4h4M9 12h6m-6 4h4"/></svg>
        <span><strong>作业</strong><small>{{ homeworkCount ? `${homeworkCount} 项待完成` : '任务中心' }}</small></span>
        <b v-if="homeworkCount" class="nav-badge">{{ homeworkCount }}</b>
      </router-link>
      <router-link to="/orders">
        <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M5 4h14v17l-3-2-4 2-4-2-3 2V4Zm4 5h6m-6 4h6"/></svg>
        <span><strong>订单</strong><small>{{ orderCount ? `${orderCount} 笔记录` : '课程订单' }}</small></span>
      </router-link>
    </nav>

    <div class="student-menu">
      <span class="avatar">{{ initial }}</span>
      <div class="student-copy"><strong>{{ profile.name || '同学' }}</strong><small>{{ maskedPhone }}</small></div>
      <button type="button" :disabled="loggingOut" @click="$emit('logout')">
        {{ loggingOut ? '退出中' : '退出登录' }}
      </button>
    </div>
  </header>
</template>

<script>
export default {
  name: 'StudentHeader',
  props: {
    profile: { type: Object, default: () => ({}) },
    loggingOut: { type: Boolean, default: false },
    homeworkCount: { type: Number, default: 0 },
    orderCount: { type: Number, default: 0 }
  },
  computed: {
    initial() {
      return (this.profile.name || '同').trim().slice(0, 1)
    },
    maskedPhone() {
      const phone = this.profile.phone || ''
      return /^\d{11}$/.test(phone) ? `${phone.slice(0, 3)}****${phone.slice(-4)}` : '手机号未填写'
    }
  }
}
</script>

<style scoped>
.student-header { height: 78px; position: sticky; z-index: 20; top: 0; display: grid; grid-template-columns: 210px minmax(430px, 1fr) 250px; align-items: center; padding: 0 clamp(24px, 5vw, 84px); border-bottom: 1px solid rgba(14, 116, 144, .12); background: rgba(247, 250, 252, .92); backdrop-filter: blur(16px); }
.brand { display: flex; align-items: center; gap: 11px; color: var(--ink); text-decoration: none; }
.brand-mark { width: 39px; height: 39px; display: grid; place-items: center; border-radius: 13px 13px 4px 13px; color: #fff; background: var(--lagoon); font: 800 13px var(--display); }
.brand strong { font: 750 15px var(--display); }
.main-nav { height: 100%; display: flex; align-items: stretch; justify-content: center; gap: clamp(4px, 1.4vw, 22px); }
.main-nav a { min-width: 128px; position: relative; display: flex; align-items: center; justify-content: center; gap: 10px; padding: 0 13px; color: var(--ink-soft); text-decoration: none; transition: color .18s ease, background .18s ease; }
.main-nav a::after { content: ''; width: 0; height: 3px; position: absolute; bottom: 0; left: 50%; border-radius: 3px 3px 0 0; background: var(--coral); transform: translateX(-50%); transition: width .2s ease; }
.main-nav a:hover { color: var(--lagoon-deep); background: rgba(217, 243, 238, .3); }
.main-nav a.router-link-active { color: var(--lagoon-deep); background: rgba(217, 243, 238, .42); }
.main-nav a.router-link-active::after { width: 42px; }
.main-nav svg { width: 21px; flex: 0 0 auto; fill: none; stroke: currentColor; stroke-width: 1.7; stroke-linecap: round; stroke-linejoin: round; }
.main-nav span { display: flex; flex-direction: column; }
.main-nav strong { font-size: 12px; }
.main-nav small { margin-top: 2px; color: #78909f; font-size: 9px; }
.nav-badge { min-width: 17px; height: 17px; position: absolute; top: 13px; right: 5px; display: grid; place-items: center; padding: 0 4px; border: 2px solid var(--paper); border-radius: 9px; color: #fff; background: var(--coral); font: 700 8px var(--mono); }
.student-menu { display: flex; align-items: center; justify-content: flex-end; gap: 10px; }
.avatar { width: 38px; height: 38px; display: grid; place-items: center; flex: 0 0 auto; border-radius: 50%; color: var(--lagoon-deep); background: var(--mint); font-weight: 750; }
.student-copy { display: flex; flex-direction: column; min-width: 78px; }
.student-copy strong { font-size: 12px; }
.student-copy small { margin-top: 2px; color: var(--ink-soft); font: 9px var(--mono); }
.student-menu button { margin-left: 5px; padding: 8px 11px; border: 1px solid var(--line); border-radius: 9px; color: var(--ink-soft); background: #fff; font-size: 10px; }
@media (max-width: 1050px) {
  .student-header { grid-template-columns: 150px 1fr 155px; padding-inline: 22px; }
  .main-nav a { min-width: 105px; }
  .student-copy { display: none; }
}
@media (max-width: 720px) {
  .student-header { height: 68px; grid-template-columns: auto 1fr auto; padding-inline: 16px; }
  .brand strong { display: none; }
  .main-nav { justify-content: flex-end; }
  .main-nav a { min-width: 47px; padding: 0 9px; }
  .main-nav a span { display: none; }
  .nav-badge { top: 11px; right: 2px; }
  .avatar { display: none; }
  .student-menu button { width: 34px; height: 34px; overflow: hidden; margin: 0 0 0 5px; padding: 0; color: transparent; }
  .student-menu button::after { content: '退'; display: grid; place-items: center; color: var(--ink-soft); }
}
</style>
