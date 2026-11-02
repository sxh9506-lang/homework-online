<template>
  <div v-loading="loading">
    <div class="back-line">
      <el-link :underline="false" :icon="ArrowLeft" @click="$router.push('/classes')">返回班级列表</el-link>
    </div>

    <template v-if="detail">
      <div class="card card-pad head">
        <div style="flex: 1; min-width: 0">
          <h2 style="font-size: 22px">{{ detail.name }}</h2>
          <div class="detail-meta">
            <span class="item"><el-icon><User /></el-icon>{{ detail.teacherName }} 老师</span>
            <span class="item"><el-icon><UserFilled /></el-icon>{{ detail.studentCount }} 名学生</span>
            <span class="item"><el-icon><Document /></el-icon>{{ detail.assignments?.length || 0 }} 份作业</span>
            <span class="item"><el-icon><Calendar /></el-icon>创建于 {{ fmtDate(detail.createdAt, 'YYYY-MM-DD') }}</span>
          </div>
        </div>
        <div class="invite">
          <div class="muted small">班级邀请码</div>
          <div class="code-row">
            <span class="code">{{ detail.code }}</span>
            <el-button size="small" :icon="CopyDocument" @click="copyCode">复制</el-button>
          </div>
        </div>
      </div>

      <div class="detail-grid">
        <div>
          <div class="card">
            <div class="card-head">
              <h3>班级作业</h3>
              <div class="right">
                <el-button v-if="isTeacher" type="primary" size="small" :icon="Plus" @click="newAssignment">
                  为班级布置作业
                </el-button>
              </div>
            </div>
            <div v-if="detail.assignments?.length" class="assign-grid" style="padding: 18px">
              <AssignCard v-for="a in detail.assignments" :key="a.id" :assignment="a" />
            </div>
            <EmptyState v-else title="这个班级还没有作业" :desc="isTeacher ? '点击右上角布置第一份作业' : '老师布置后会出现在这里'" />
          </div>
        </div>

        <aside>
          <div class="card">
            <div class="card-head">
              <div>
                <h3>学生名单</h3>
                <div class="sub">{{ detail.students?.length || 0 }} 人</div>
              </div>
              <div v-if="isTeacher" class="right">
                <el-button size="small" :icon="Plus" @click="addVisible = true">添加学生</el-button>
              </div>
            </div>
            <div class="student-list">
              <div v-for="s in detail.students" :key="s.id" class="student-row">
                <el-avatar :size="30" :style="{ background: avatarColor(s.name), fontSize: '11px' }">
                  {{ avatarText(s.name) }}
                </el-avatar>
                <div style="min-width: 0; flex: 1">
                  <div class="n">{{ s.name }}</div>
                  <div class="m">{{ s.studentNo || s.username }}</div>
                </div>
                <el-button
                  v-if="isTeacher"
                  link
                  type="danger"
                  size="small"
                  @click="removeStudent(s)"
                >
                  移出
                </el-button>
              </div>
              <EmptyState v-if="!detail.students?.length" :icon="User" title="还没有学生" desc="学生可用邀请码自行加入，也可以由老师手动添加" />
            </div>
          </div>
        </aside>
      </div>
    </template>

    <el-dialog v-model="addVisible" title="添加学生到班级" width="420px">
      <el-form label-position="top" @submit.prevent>
        <el-form-item label="学生用户名或学号">
          <el-input v-model="addKey" placeholder="例如：zhang 或 20240101" @keyup.enter="addStudent" />
        </el-form-item>
        <div class="muted small">学生需先注册账号；也可以把邀请码 <b>{{ detail?.code }}</b> 发给学生自行加入。</div>
      </el-form>
      <template #footer>
        <el-button @click="addVisible = false">取消</el-button>
        <el-button type="primary" :loading="adding" @click="addStudent">添加</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowLeft, User, UserFilled, Document, Calendar, CopyDocument, Plus
} from '@element-plus/icons-vue'
import { classApi } from '../api'
import { auth } from '../store/auth'
import { avatarColor, avatarText, fmtDate } from '../utils/format'
import AssignCard from '../components/AssignCard.vue'
import EmptyState from '../components/EmptyState.vue'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const detail = ref(null)
const addVisible = ref(false)
const addKey = ref('')
const adding = ref(false)

const isTeacher = computed(() => auth.state.user?.role === 'TEACHER')

async function load() {
  loading.value = true
  try {
    detail.value = await classApi.detail(route.params.id)
  } finally {
    loading.value = false
  }
}

async function copyCode() {
  try {
    await navigator.clipboard.writeText(detail.value.code)
    ElMessage.success('邀请码已复制')
  } catch {
    ElMessage.info(detail.value.code)
  }
}

function newAssignment() {
  router.push({ path: '/assignments/new', query: { classId: route.params.id } })
}

async function addStudent() {
  if (!addKey.value.trim()) return
  adding.value = true
  try {
    await classApi.addStudent(route.params.id, addKey.value.trim())
    ElMessage.success('已添加学生')
    addVisible.value = false
    addKey.value = ''
    await load()
  } finally {
    adding.value = false
  }
}

async function removeStudent(s) {
  try {
    await ElMessageBox.confirm(`确定将「${s.name}」移出班级吗？`, '移出学生', { type: 'warning' })
  } catch {
    return
  }
  await classApi.removeStudent(route.params.id, s.id)
  ElMessage.success('已移出')
  await load()
}

onMounted(load)
</script>

<style scoped>
.back-line { margin-bottom: 14px; }
.head { display: flex; gap: 20px; align-items: center; flex-wrap: wrap; margin-bottom: 18px; }
.invite { text-align: right; }
.code-row { display: flex; align-items: center; gap: 10px; margin-top: 6px; }
.code {
  font-size: 20px; font-weight: 700; letter-spacing: 0.14em;
  color: var(--brand); background: var(--brand-soft); padding: 6px 12px; border-radius: 8px;
}
.detail-grid { display: grid; grid-template-columns: 1fr 330px; gap: 18px; align-items: start; }
.student-list { max-height: 560px; overflow: auto; }
.student-row {
  display: flex; align-items: center; gap: 10px;
  padding: 10px 18px; border-bottom: 1px solid #f3f5fa;
}
.student-row:last-child { border-bottom: 0; }
.student-row .n { font-size: 13.5px; }
.student-row .m { font-size: 11.5px; color: var(--ink-3); margin-top: 2px; }
@media (max-width: 1180px) {
  .detail-grid { grid-template-columns: 1fr; }
}
</style>
