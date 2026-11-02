<template>
  <div class="app-shell">
    <aside class="sidebar">
      <div class="brand">
        <div class="brand-logo"><el-icon><Reading /></el-icon></div>
        <div class="brand-text">
          <div class="brand-name">作业云</div>
          <div class="brand-sub">HOMEWORK ONLINE</div>
        </div>
      </div>

      <nav>
        <div class="nav-group-label">教学与学习</div>
        <div
          v-for="item in navItems"
          :key="item.path"
          class="nav-item"
          :class="{ active: isActive(item) }"
          @click="go(item)"
        >
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
          <span v-if="item.badge" class="nav-badge">{{ item.badge }}</span>
        </div>

        <template v-if="isTeacher">
          <div class="nav-group-label">教学管理</div>
          <div
            v-for="item in manageItems"
            :key="item.path"
            class="nav-item"
            :class="{ active: isActive(item) }"
            @click="go(item)"
          >
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.label }}</span>
          </div>
        </template>
      </nav>

      <div class="sidebar-footer">
        <div class="side-user" @click="$router.push('/profile')">
          <el-avatar :size="34" :style="{ background: avatarColor(user?.name), fontSize: '12px' }">
            {{ avatarText(user?.name) }}
          </el-avatar>
          <div class="meta">
            <div class="name">{{ user?.name }}</div>
            <div class="role">{{ isTeacher ? `教师 · ${user?.subject || '任课'}` : `学生 · ${user?.studentNo || '在读'}` }}</div>
          </div>
        </div>
      </div>
    </aside>

    <div class="main">
      <header class="topbar">
        <div>
          <div class="topbar-title">{{ route.meta.title }}</div>
          <div class="topbar-sub">{{ route.meta.subtitle }}</div>
        </div>
        <div class="topbar-spacer" />
        <el-button v-if="isTeacher" type="primary" :icon="Plus" @click="$router.push('/assignments/new')">
          布置作业
        </el-button>
        <el-button v-else :icon="Plus" @click="joinClass">加入班级</el-button>
        <el-tooltip content="刷新数据" placement="bottom">
          <el-button circle :icon="Refresh" :loading="refreshing" @click="refresh" />
        </el-tooltip>
        <el-dropdown trigger="click" @command="onCommand">
          <div class="topbar-user">
            <el-avatar :size="30" :style="{ background: avatarColor(user?.name), fontSize: '11px' }">
              {{ avatarText(user?.name) }}
            </el-avatar>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile" :icon="User">个人中心</el-dropdown-item>
              <el-dropdown-item command="logout" :icon="SwitchButton" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </header>

      <main class="page">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" @changed="loadStats" />
          </transition>
        </router-view>
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Reading, Odometer, Document, EditPen, Stamp, School, Plus, Refresh, User, SwitchButton
} from '@element-plus/icons-vue'
import { auth } from '../store/auth'
import { classApi, statsApi } from '../api'
import { avatarColor, avatarText } from '../utils/format'

const route = useRoute()
const router = useRouter()

const user = computed(() => auth.state.user)
const isTeacher = computed(() => auth.state.user?.role === 'TEACHER')

const stats = ref(null)
const refreshing = ref(false)

const navItems = computed(() => {
  const base = [
    { path: '/dashboard', label: '工作台', icon: Odometer },
    { path: '/assignments', label: '作业列表', icon: Document }
  ]
  if (isTeacher.value) {
    base.push({ path: '/grading', label: '批改中心', icon: Stamp, badge: pendingCount.value || null })
    base.push({ path: '/classes', label: '班级管理', icon: School })
  } else {
    base.push({ path: '/classes', label: '我的班级', icon: School })
  }
  return base
})

const manageItems = computed(() => [
  { path: '/assignments/new', label: '在线录入作业', icon: EditPen }
])

const pendingCount = computed(() => {
  if (!isTeacher.value || !stats.value) return 0
  const card = (stats.value.cards || []).find((c) => c.label === '待批改作业')
  return card ? Number(card.value) || 0 : 0
})

const isActive = (item) => {
  if (item.path === '/assignments') return route.path === '/assignments' || route.path.startsWith('/assignments/') && !['/assignments/new'].includes(route.path) && !route.path.endsWith('/edit')
  if (item.path === '/assignments/new') return route.path === '/assignments/new' || route.path.endsWith('/edit')
  if (item.path === '/grading') return route.path.startsWith('/grading')
  if (item.path === '/classes') return route.path.startsWith('/classes')
  return route.path === item.path
}

function go(item) {
  router.push(item.path)
}

async function loadStats() {
  if (!auth.isLoggedIn.value) return
  try {
    stats.value = await statsApi.overview()
  } catch {
    /* 忽略，导航计数失败不影响使用 */
  }
}

async function refresh() {
  refreshing.value = true
  await loadStats()
  refreshing.value = false
  ElMessage.success('数据已刷新')
  router.go(0)
}

async function joinClass() {
  try {
    const { value } = await ElMessageBox.prompt('请输入老师提供的班级邀请码', '加入班级', {
      confirmButtonText: '加入',
      cancelButtonText: '取消',
      inputPlaceholder: '例如：G1-3A9K',
      inputValidator: (v) => (!!v && v.trim().length >= 3 ? true : '请输入有效的邀请码')
    })
    const cls = await classApi.join(value.trim().toUpperCase())
    ElMessage.success(`已加入「${cls.name}」`)
    router.push(`/classes/${cls.id}`)
  } catch {
    /* 取消 */
  }
}

async function onCommand(cmd) {
  if (cmd === 'profile') router.push('/profile')
  if (cmd === 'logout') {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', { type: 'warning', confirmButtonText: '退出', cancelButtonText: '取消' })
    auth.logout()
    router.replace('/login')
  }
}

watch(() => route.path, loadStats)
onMounted(loadStats)
</script>

<style scoped>
.topbar-user {
  cursor: pointer;
  display: flex;
  align-items: center;
  padding: 4px;
  border-radius: 50%;
  border: 1px solid var(--line);
  transition: border-color 0.15s;
}
.topbar-user:hover { border-color: var(--el-color-primary-light-5); }
</style>
