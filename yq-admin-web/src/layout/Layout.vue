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
          v-for="item in permMenuItems"
          :key="item.path"
          :to="item.path"
          class="nav-item"
          :class="{ active: activeMenu === item.path }"
        >
          <i :class="item.icon"></i>
          <span>{{ item.title }}</span>
        </router-link>
      </nav>

      <p class="nav-cap">教务管理</p>
      <nav class="nav">
        <div
          v-for="item in eduMenuItems"
          :key="item.path"
          class="nav-entry"
        >
          <template v-if="item.children">
            <button
              type="button"
              class="nav-item nav-group-toggle"
              :class="{ active: isGroupActive(item) }"
              :aria-expanded="String(isMenuOpen(item.path))"
              @click="toggleMenu(item.path)"
            >
              <i :class="item.icon"></i>
              <span>{{ item.title }}</span>
              <i class="el-icon-arrow-down nav-arrow" :class="{ open: isMenuOpen(item.path) }"></i>
            </button>
            <transition name="submenu">
              <div v-show="isMenuOpen(item.path)" class="submenu">
                <router-link
                  v-for="child in item.children"
                  :key="child.path"
                  :to="child.path"
                  class="submenu-item"
                  :class="{ active: activeMenu === child.path }"
                >
                  <i :class="child.icon"></i>
                  <span>{{ child.title }}</span>
                </router-link>
              </div>
            </transition>
          </template>
          <router-link
            v-else
            :to="item.path"
            class="nav-item"
            :class="{ active: activeMenu === item.path }"
          >
            <i :class="item.icon"></i>
            <span>{{ item.title }}</span>
          </router-link>
        </div>
      </nav>

      <p class="nav-cap">系统设置</p>
      <nav class="nav">
        <router-link
          v-for="item in systemMenuItems"
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
          <span class="admin-chip"><i class="presence"></i>{{ currentUserName }}</span>
          <el-tooltip content="退出登录" placement="bottom">
            <el-button
              class="logout-btn"
              icon="el-icon-switch-button"
              :loading="logoutLoading"
              circle
              @click="handleLogout"
            />
          </el-tooltip>
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
import { logout } from '@/api/auth'

export default {
  name: 'Layout',
  data() {
    return {
      permMenuItems: [
        { path: '/user', title: '用户管理', icon: 'el-icon-user' },
        { path: '/role', title: '角色管理', icon: 'el-icon-s-check' },
        { path: '/permission', title: '权限管理', icon: 'el-icon-lock' }
      ],
      eduMenuItems: [
        { path: '/campus', title: '校区管理', icon: 'el-icon-office-building' },
        { path: '/course', title: '课程管理', icon: 'el-icon-reading' },
        { path: '/class', title: '班级管理', icon: 'el-icon-school' },
        {
          path: '/homework',
          title: '作业管理',
          icon: 'el-icon-edit-outline',
          children: [
            { path: '/homework', title: '作业发布台', icon: 'el-icon-s-promotion' },
            { path: '/homework/status', title: '作业详情管理', icon: 'el-icon-data-analysis' }
          ]
        },
        { path: '/teacher-schedule', title: '教师课表', icon: 'el-icon-date' },
        { path: '/class-duty', title: '值班管理', icon: 'el-icon-alarm-clock' }
      ],
      systemMenuItems: [
        { path: '/config', title: '规则配置', icon: 'el-icon-setting' }
      ],
      now: new Date(),
      timer: null,
      logoutLoading: false,
      openMenus: { '/homework': true }
    }
  },
  computed: {
    activeMenu() {
      if (/^\/homework\/\d+\/submissions$/.test(this.$route.path)) {
        return '/homework/status'
      }
      return this.$route.path
    },
    clockText() {
      const d = this.now
      const week = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'][d.getDay()]
      const pad = n => String(n).padStart(2, '0')
      return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${week} ${pad(d.getHours())}:${pad(d.getMinutes())}`
    },
    currentUserName() {
      const userText = localStorage.getItem('yq_user')
      if (!userText) return '管理员'
      try {
        const user = JSON.parse(userText)
        return user.nickname || user.realName || user.username || '管理员'
      } catch (e) {
        return '管理员'
      }
    }
  },
  methods: {
    isGroupActive(item) {
      return item.children.some(child => child.path === this.activeMenu)
    },
    isMenuOpen(path) {
      return Boolean(this.openMenus[path])
    },
    toggleMenu(path) {
      this.$set(this.openMenus, path, !this.isMenuOpen(path))
    },
    clearLoginStorage() {
      localStorage.removeItem('yq_token')
      localStorage.removeItem('yq_sign_secret')
      localStorage.removeItem('yq_user')
    },
    async handleLogout() {
      try {
        await this.$confirm('确定退出当前账号吗？', '退出登录', {
          confirmButtonText: '退出',
          cancelButtonText: '取消',
          type: 'warning'
        })
      } catch (e) {
        return
      }

      this.logoutLoading = true
      try {
        await logout()
        this.$message.success('已退出登录')
      } catch (e) {
        // 后端登录态可能已经失效，本地仍然需要清理，避免坏 token 留在浏览器里。
      } finally {
        this.logoutLoading = false
        this.clearLoginStorage()
        if (this.$route.path !== '/login') {
          this.$router.replace('/login')
        }
      }
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
.nav-entry { display: block; }
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
.nav-group-toggle {
  width: 100%;
  border: 0;
  background: transparent;
  font-family: inherit;
  text-align: left;
  cursor: pointer;
}
.nav-item i { font-size: 16px; transition: transform .18s ease; }
.nav-item .nav-arrow {
  margin-left: auto;
  color: #71877A;
  font-size: 12px;
  transform: none;
}
.nav-item .nav-arrow.open { transform: rotate(180deg); }
.nav-item:hover { background: var(--pine-2); color: #E8EFE9; }
.nav-item:hover i { transform: translateX(2px); }
.nav-item:hover .nav-arrow { transform: none; }
.nav-item:hover .nav-arrow.open { transform: rotate(180deg); }
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
.submenu {
  display: grid;
  gap: 2px;
  padding: 3px 0 5px 25px;
}
.submenu-item {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 8px 10px;
  border-radius: 6px;
  color: #82968A;
  font-size: 12.5px;
  text-decoration: none;
  transition: color .16s ease, background-color .16s ease;
}
.submenu-item > i {
  width: 14px;
  color: #71877A;
  font-size: 12px;
  text-align: center;
}
.submenu-item:hover { color: #E8EFE9; background: rgba(255, 255, 255, .045); }
.submenu-item.active { color: #FFFFFF; background: rgba(185, 138, 47, .13); }
.submenu-item.active > i { color: var(--brass); }
.submenu-enter-active,
.submenu-leave-active { transition: opacity .16s ease, transform .16s ease; }
.submenu-enter,
.submenu-leave-to { opacity: 0; transform: translateY(-4px); }

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
.logout-btn {
  width: 32px;
  height: 32px;
  padding: 0;
  border-color: var(--line);
  color: var(--ink-3);
  background: #FFFFFF;
}
.logout-btn:hover,
.logout-btn:focus {
  border-color: var(--brass);
  color: var(--brass-ink);
  background: #FFFCF5;
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
  .nav-item .nav-arrow { display: none; }
  .submenu { padding: 2px 0 4px; }
  .submenu-item { justify-content: center; padding: 9px 0; }
  .submenu-item span { display: none; }
  .side-foot { justify-content: center; }
  .page-container { padding: 18px 14px 32px; }
}
</style>
