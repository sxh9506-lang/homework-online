<template>
  <div v-loading="loading">
    <div class="welcome">
      <div>
        <div class="hello">{{ greeting }}，{{ user.name }}</div>
        <div class="sub">
          {{ isTeacher ? `你有 ${pendingGrading} 份作业等待批改，${todayDue} 份作业今天截止` : `还有 ${todo.length} 项作业待完成，加油！` }}
        </div>
      </div>
      <div class="welcome-actions">
        <el-button v-if="isTeacher" type="primary" :icon="EditPen" @click="$router.push('/assignments/new')">在线录入作业</el-button>
        <el-button v-if="isTeacher" :icon="Stamp" @click="$router.push('/grading')">开始批改</el-button>
        <el-button v-else type="primary" :icon="Document" @click="$router.push('/assignments')">查看全部作业</el-button>
      </div>
    </div>

    <div class="stat-grid" style="margin-bottom: 20px">
      <div v-for="card in cards" :key="card.label" class="stat-card" :class="{ hl: card.highlight }">
        <div class="label">{{ card.label }}</div>
        <div class="value">
          {{ card.value }}<span v-if="card.unit" class="unit">{{ card.unit }}</span>
        </div>
        <div class="stat-icon"><el-icon><component :is="iconOf(card.icon)" /></el-icon></div>
      </div>
    </div>

    <div class="dash-grid">
      <div class="card">
        <div class="card-head">
          <div>
            <h3>{{ isTeacher ? '待批改作业' : '待完成作业' }}</h3>
            <div class="sub">{{ isTeacher ? '按提交时间排序，点击进入批改' : '按截止时间排序' }}</div>
          </div>
          <div class="right">
            <el-link v-if="isTeacher" type="primary" :underline="false" @click="$router.push('/grading')">全部</el-link>
            <el-link v-else type="primary" :underline="false" @click="$router.push('/assignments')">全部</el-link>
          </div>
        </div>

        <template v-if="todo.length">
          <div v-for="item in todo" :key="item.kind + item.id" class="todo-row" @click="openTodo(item)">
            <span class="todo-dot" :class="{ danger: item.late || item.overdue, warn: !item.late && !item.overdue }" />
            <div class="todo-main">
              <div class="todo-title">{{ item.title }}</div>
              <div class="todo-sub">{{ item.subtitle }}</div>
            </div>
            <div class="todo-side">
              <span v-if="item.late" class="pill red">逾期提交</span>
              <span v-else-if="item.overdue" class="pill red">已逾期</span>
              <span v-else class="muted">{{ item.meta }}</span>
            </div>
          </div>
        </template>
        <EmptyState v-else :icon="Select" :title="isTeacher ? '所有作业都批改完啦 🎉' : '暂时没有待办作业'" desc="可以休息一下，或者去看看历史记录">
          <el-button v-if="isTeacher" size="small" @click="$router.push('/assignments/new')">布置新作业</el-button>
        </EmptyState>
      </div>

      <div class="card">
        <div class="card-head">
          <div>
            <h3>{{ isTeacher ? '最近布置的作业' : '最近提交记录' }}</h3>
            <div class="sub">{{ isTeacher ? '提交与批改进度' : '查看批改结果' }}</div>
          </div>
        </div>
        <template v-if="recent.length">
          <div v-for="item in recent" :key="item.kind + item.id" class="todo-row" @click="openRecent(item)">
            <div class="todo-main">
              <div class="todo-title">{{ item.title }}</div>
              <div class="todo-sub">{{ item.subtitle }}</div>
            </div>
            <div class="todo-side">
              <span v-if="isTeacher" class="pill" :class="item.status === 'PUBLISHED' ? 'blue' : 'gray'">
                {{ item.status === 'PUBLISHED' ? '已发布' : '草稿' }}
              </span>
              <span v-else class="pill" :class="item.status === 'GRADED' ? 'green' : 'orange'">
                {{ item.status === 'GRADED' ? item.meta : '等待批改' }}
              </span>
              <div v-if="isTeacher" class="muted small" style="margin-top: 4px">{{ item.meta }}</div>
            </div>
          </div>
        </template>
        <EmptyState v-else :title="isTeacher ? '还没有布置过作业' : '还没有提交过作业'" desc="这里会显示最近的动态" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  EditPen, Stamp, Document, School, User, DataLine, Timer, Upload, Select
} from '@element-plus/icons-vue'
import { statsApi } from '../api'
import { auth } from '../store/auth'
import EmptyState from '../components/EmptyState.vue'

const router = useRouter()
const loading = ref(true)
const data = ref({ cards: [], todo: [], recent: [] })

const user = computed(() => auth.state.user || {})
const isTeacher = computed(() => user.value.role === 'TEACHER')
const cards = computed(() => data.value.cards || [])
const todo = computed(() => data.value.todo || [])
const recent = computed(() => data.value.recent || [])

const pendingGrading = computed(() => {
  const c = cards.value.find((x) => x.label === '待批改作业')
  return c?.value ?? 0
})
const todayDue = computed(() => todo.value.filter((t) => t.meta && t.meta.includes('截止')).length)

const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 6) return '夜深了'
  if (h < 11) return '早上好'
  if (h < 14) return '中午好'
  if (h < 18) return '下午好'
  return '晚上好'
})

const ICONS = { class: School, student: User, pen: Stamp, chart: DataLine, todo: Document, upload: Upload }
const iconOf = (key) => ICONS[key] || Document

function openTodo(item) {
  if (item.kind === 'SUBMISSION') router.push(`/grading/${item.id}`)
  else router.push(`/assignments/${item.id}`)
}
function openRecent(item) {
  if (item.kind === 'ASSIGNMENT') router.push(`/assignments/${item.id}`)
  else router.push(`/assignments/${item.assignmentId}`)
}

onMounted(async () => {
  try {
    data.value = await statsApi.overview()
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.welcome {
  display: flex; align-items: center; gap: 20px; flex-wrap: wrap;
  background: linear-gradient(120deg, #1f2a52 0%, #2a3670 52%, #3b3f9e 100%);
  border-radius: 18px; padding: 26px 28px; color: #fff; margin-bottom: 20px;
  position: relative; overflow: hidden;
}
.welcome::after {
  content: ''; position: absolute; right: -60px; top: -80px; width: 300px; height: 300px;
  border-radius: 50%; background: radial-gradient(circle, rgba(120, 140, 255, 0.45), transparent 65%);
}
.welcome .hello { font-size: 22px; font-weight: 650; letter-spacing: -0.01em; }
.welcome .sub { color: #b9c2e4; font-size: 13.5px; margin-top: 8px; }
.welcome-actions { margin-left: auto; display: flex; gap: 10px; position: relative; z-index: 1; }
.dash-grid { display: grid; grid-template-columns: 1.25fr 1fr; gap: 18px; }
@media (max-width: 1100px) {
  .dash-grid { grid-template-columns: 1fr; }
}
</style>
