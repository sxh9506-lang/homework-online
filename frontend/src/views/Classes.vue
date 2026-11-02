<template>
  <div v-loading="loading">
    <div class="toolbar">
      <div class="muted small">
        {{ isTeacher ? '创建班级后，把邀请码发给学生，学生即可自主加入' : '加入班级后就能看到老师布置的家庭作业' }}
      </div>
      <div style="margin-left: auto; display: flex; gap: 10px">
        <el-button :icon="Plus" @click="joinClass">使用邀请码加入</el-button>
        <el-button v-if="isTeacher" type="primary" :icon="Plus" @click="createVisible = true">新建班级</el-button>
      </div>
    </div>

    <div v-if="list.length" class="class-grid">
      <div v-for="c in list" :key="c.id" class="card class-card" @click="$router.push(`/classes/${c.id}`)">
        <div class="cover">
          <span class="cover-icon"><el-icon><School /></el-icon></span>
          <span class="pill" :class="isTeacher ? 'blue' : 'green'">{{ isTeacher ? '我创建的班级' : '已加入' }}</span>
        </div>
        <div class="body">
          <div class="name">{{ c.name }}</div>
          <div class="meta">
            <span><el-icon><UserFilled /></el-icon>{{ c.studentCount }} 名学生</span>
            <span v-if="isTeacher"><el-icon><Key /></el-icon>邀请码 {{ c.code }}</span>
            <span v-else><el-icon><User /></el-icon>{{ c.teacherName }} 老师</span>
          </div>
        </div>
        <div class="foot">
          <el-button size="small" @click.stop="$router.push(`/classes/${c.id}`)">查看班级</el-button>
          <el-button size="small" @click.stop="$router.push({ path: '/assignments', query: { classId: c.id } })">班级作业</el-button>
          <el-button v-if="isTeacher" size="small" link type="primary" @click.stop="copyCode(c)">复制邀请码</el-button>
        </div>
      </div>
    </div>

    <div v-else-if="!loading" class="card">
      <EmptyState
        :icon="School"
        :title="isTeacher ? '还没有创建班级' : '还没有加入班级'"
        :desc="isTeacher ? '创建班级后即可布置作业、管理学生名单' : '向老师索要班级邀请码，加入后就能完成作业'"
      >
        <el-button v-if="isTeacher" type="primary" size="small" @click="createVisible = true">新建班级</el-button>
        <el-button v-else type="primary" size="small" @click="joinClass">使用邀请码加入</el-button>
      </EmptyState>
    </div>

    <el-dialog v-model="createVisible" title="新建班级" width="440px">
      <el-form ref="createRef" :model="createForm" :rules="createRules" label-position="top">
        <el-form-item label="班级名称" prop="name">
          <el-input v-model="createForm.name" placeholder="例如：高一（7）班 · 语文" />
        </el-form-item>
        <el-form-item label="邀请码（选填，留空自动生成）">
          <el-input v-model="createForm.code" placeholder="例如：G1-7" maxlength="12" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="create">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, School, UserFilled, User, Key } from '@element-plus/icons-vue'
import { classApi } from '../api'
import { auth } from '../store/auth'
import EmptyState from '../components/EmptyState.vue'

const router = useRouter()
const loading = ref(false)
const list = ref([])
const createVisible = ref(false)
const creating = ref(false)
const createRef = ref()

const isTeacher = computed(() => auth.state.user?.role === 'TEACHER')

const createForm = reactive({ name: '', code: '' })
const createRules = {
  name: [{ required: true, message: '请输入班级名称', trigger: 'blur' }]
}

async function load() {
  loading.value = true
  try {
    list.value = await classApi.list()
  } finally {
    loading.value = false
  }
}

async function create() {
  const ok = await createRef.value?.validate().catch(() => false)
  if (!ok) return
  creating.value = true
  try {
    const cls = await classApi.create({ name: createForm.name.trim(), code: createForm.code.trim() || undefined })
    ElMessage.success(`班级已创建，邀请码：${cls.code}`)
    createVisible.value = false
    createForm.name = ''
    createForm.code = ''
    await load()
    router.push(`/classes/${cls.id}`)
  } finally {
    creating.value = false
  }
}

async function joinClass() {
  try {
    const { value } = await ElMessageBox.prompt('请输入老师提供的班级邀请码', '加入班级', {
      confirmButtonText: '加入',
      cancelButtonText: '取消',
      inputPlaceholder: '例如：G1-3'
    })
    const cls = await classApi.join(value.trim().toUpperCase())
    ElMessage.success(`已加入「${cls.name}」`)
    await load()
  } catch {
    /* cancel */
  }
}

async function copyCode(c) {
  try {
    await navigator.clipboard.writeText(c.code)
    ElMessage.success(`邀请码 ${c.code} 已复制`)
  } catch {
    ElMessage.info(`邀请码：${c.code}`)
  }
}

onMounted(load)
</script>

<style scoped>
.toolbar { display: flex; align-items: center; margin-bottom: 18px; gap: 12px; flex-wrap: wrap; }
.class-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); gap: 16px; }
.class-card { cursor: pointer; transition: transform 0.18s, box-shadow 0.18s; overflow: hidden; }
.class-card:hover { transform: translateY(-2px); box-shadow: var(--shadow-md); }
.cover {
  height: 96px; background: linear-gradient(120deg, #303c73, #4a5cf5);
  display: flex; align-items: flex-start; justify-content: space-between;
  padding: 16px 18px; position: relative;
}
.cover::after {
  content: ''; position: absolute; right: -30px; bottom: -60px; width: 180px; height: 180px;
  border-radius: 50%; background: rgba(255, 255, 255, 0.12);
}
.cover-icon { color: #fff; font-size: 26px; }
.body { padding: 16px 18px 10px; }
.name { font-size: 16px; font-weight: 600; }
.meta { display: flex; flex-wrap: wrap; gap: 6px 16px; margin-top: 10px; color: var(--ink-3); font-size: 12.5px; }
.meta span { display: inline-flex; align-items: center; gap: 5px; }
.foot { padding: 10px 18px 16px; display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
</style>
