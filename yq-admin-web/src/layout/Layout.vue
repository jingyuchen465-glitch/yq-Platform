<template>
  <el-container class="layout-container">
    <!-- 侧边栏 -->
    <aside class="side">
      <div class="brand">
        <span class="brand-seal">YQ</span>
        <span class="brand-text">
          <b>YQ 管理台</b>
          <i>OPERATION CONSOLE</i>
        </span>
      </div>

      <p class="nav-cap">权限体系</p>
      <nav class="nav">
        <router-link
          v-for="item in menuItems"
          :key="item.path"
          :to="item.path"
          class="nav-item"
          :class="{ active: activeMenu === item.path }"
        >
          <i :class="item.icon"></i>
          <span>{{ item.title }}</span>
        </router-link>
      </nav>

      <div class="side-foot">
        <span class="live-dot"></span>
        <span class="mono">dev · localhost:8080</span>
      </div>
    </aside>

    <el-container class="body-col">
      <!-- 顶部栏 -->
      <header class="topbar">
        <span class="top-clock mono">{{ clockText }}</span>
        <div class="top-right">
          <span class="env-tag">DEV</span>
          <span class="admin-chip"><i class="presence"></i>管理员</span>
        </div>
      </header>

      <!-- 主内容区 -->
      <el-main class="main">
        <transition name="page" mode="out-in">
          <router-view :key="$route.path" />
        </transition>
      </el-main>
    </el-container>
  </el-container>
</template>

<script>
export default {
  name: 'Layout',
  data() {
    return {
      menuItems: [
        { path: '/user', title: '用户管理', icon: 'el-icon-user' },
        { path: '/role', title: '角色管理', icon: 'el-icon-s-check' },
        { path: '/permission', title: '权限管理', icon: 'el-icon-lock' }
      ],
      now: new Date(),
      timer: null
    }
  },
  computed: {
    activeMenu() {
      return this.$route.path
    },
    clockText() {
      const d = this.now
      const week = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'][d.getDay()]
      const pad = n => String(n).padStart(2, '0')
      return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${week} ${pad(d.getHours())}:${pad(d.getMinutes())}`
    }
  },
  mounted() {
    this.timer = setInterval(() => { this.now = new Date() }, 30000)
  },
  beforeDestroy() {
    clearInterval(this.timer)
  }
}
</script>

<style scoped>
.layout-container { height: 100%; }

/* ---------- 侧边栏 ---------- */
.side {
  width: 218px;
  flex: none;
  display: flex;
  flex-direction: column;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, .03) 0%, rgba(255, 255, 255, 0) 220px),
    var(--pine);
  color: #C7D4CC;
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 22px 20px 20px;
}
.brand-seal {
  width: 38px; height: 38px;
  flex: none;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--brass);
  color: var(--pine);
  font-family: var(--font-display);
  font-size: 15px;
  font-weight: 800;
  letter-spacing: .02em;
  border-radius: 8px;
  box-shadow: 0 4px 12px -4px rgba(185, 138, 47, .6);
}
.brand-text { display: flex; flex-direction: column; gap: 3px; }
.brand-text b {
  font-family: var(--font-display);
  font-size: 16px;
  font-weight: 700;
  color: #F2F6F1;
  letter-spacing: .03em;
}
.brand-text i {
  font-style: normal;
  font-family: var(--font-mono);
  font-size: 9px;
  letter-spacing: .28em;
  color: #6E8578;
}

.nav-cap {
  padding: 14px 22px 8px;
  font-size: 11px;
  letter-spacing: .24em;
  color: #5C7266;
}

.nav { display: flex; flex-direction: column; gap: 2px; padding: 0 10px; }
.nav-item {
  position: relative;
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 11px 14px;
  border-radius: 7px;
  color: #AFC0B5;
  font-size: 14px;
  text-decoration: none;
  transition: background-color .18s ease, color .18s ease;
}
.nav-item i { font-size: 16px; transition: transform .18s ease; }
.nav-item:hover { background: var(--pine-2); color: #E8EFE9; }
.nav-item:hover i { transform: translateX(2px); }
.nav-item.active {
  background: var(--pine-3);
  color: #FFFFFF;
  font-weight: 600;
}
.nav-item.active::before {
  content: '';
  position: absolute;
  left: -10px;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 20px;
  border-radius: 0 3px 3px 0;
  background: var(--brass);
}

.side-foot {
  margin-top: auto;
  padding: 16px 22px;
  border-top: 1px solid rgba(255, 255, 255, .07);
  display: flex;
  align-items: center;
  gap: 8px;
}
.side-foot .mono { font-size: 10.5px; color: #5C7266; letter-spacing: .04em; }
.live-dot {
  width: 6px; height: 6px;
  border-radius: 50%;
  background: #3FBF8F;
  box-shadow: 0 0 0 3px rgba(63, 191, 143, .18);
  animation: pulse 2.4s ease-in-out infinite;
}
@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: .45; }
}

/* ---------- 主列 ---------- */
.body-col { min-width: 0; flex-direction: column; }

.topbar {
  height: 52px;
  flex: none;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 26px;
  background: var(--card);
  border-bottom: 1px solid var(--line);
}
.top-clock { font-size: 12px; color: var(--ink-3); letter-spacing: .03em; }
.top-right { display: flex; align-items: center; gap: 14px; }
.env-tag {
  font-family: var(--font-mono);
  font-size: 10px;
  font-weight: 600;
  letter-spacing: .18em;
  color: var(--brass-ink);
  border: 1px solid var(--brass);
  border-radius: 4px;
  padding: 2px 7px;
}
.admin-chip {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  font-size: 13px;
  color: var(--ink-2);
  font-weight: 500;
}
.presence {
  width: 7px; height: 7px;
  border-radius: 50%;
  background: var(--jade);
  box-shadow: 0 0 0 3px var(--jade-soft);
}

.main {
  background: var(--paper);
  overflow-y: auto;
  padding: 0;
}

/* ---------- 路由切换动效 ---------- */
.page-enter-active,
.page-leave-active {
  transition: opacity .22s ease, transform .22s ease;
}
.page-enter {
  opacity: 0;
  transform: translateY(8px);
}
.page-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}

/* ---------- 窄屏 ---------- */
@media (max-width: 900px) {
  .side { width: 64px; }
  .brand { justify-content: center; padding: 18px 8px; }
  .brand-text, .nav-cap, .side-foot .mono { display: none; }
  .nav { padding: 0 8px; }
  .nav-item { justify-content: center; padding: 12px 0; }
  .nav-item span { display: none; }
  .side-foot { justify-content: center; }
  .page-container { padding: 18px 14px 32px; }
}
</style>
