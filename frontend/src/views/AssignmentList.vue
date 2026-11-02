<template>
  <div>
    <div class="toolbar card">
      <el-select v-model="filters.classId" placeholder="全部班级" clearable style="width: 200px" @change="load">
        <el-option v-for="c in classes" :key="c.id" :label="c.name" :value="c.id" />
      </el-select>

      <el-radio-group v-model="filters.status" @change="load">
        <el-radio-button v-for="opt in statusOptions" :key="opt.value" :value="opt.value">
          {{ opt.label }}
        </el-radio-button>
      </el-radio-group>

      <el-input
        v-model="filters.keyword"
        placeholder="搜索作业标题或内容"
        :prefix-icon="Search"
        clearable
        style="width: 240px; margin-left: auto"
        @keyup.enter="load"
        @clear="load"
      />
      <el-button :icon="Search" @click="load">搜索</el-button>
      <el-button v-if="isTeacher" type="primary" :icon="Plus" @click="$router.push('/assignments/new')">布置作业</el-button>
    </div>

    <div v-loading="loading" style="min-height: 180px">
      <div v-if="list.length" class="assign-grid">
        <AssignCard v-for="a in list" :key="a.id" :assignment="a" />
      </div>
      <div v-else-if="!loading" class="card">
        <EmptyState :title="emptyTitle" :desc="emptyDesc">
          <el-button v-if="isTeacher" type="primary" size="small" @click="$router.push('/assignments/new')">
            去布置第一份作业
          </el-button>
        </EmptyState>
      </div>
    </div>

    <div v-if="list.length" class="muted small" style="margin-top: 16px; text-align: center">
      共 {{ list.length }} 条作业{{ filters.status === 'DRAFT' ? '（草稿仅自己可见）' : '' }}
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Plus, Search } from '@element-plus/icons-vue'
import { assignmentApi, classApi } from '../api'
import { auth } from '../store/auth'
import AssignCard from '../components/AssignCard.vue'
import EmptyState from '../components/EmptyState.vue'

const router = useRouter()
const loading = ref(false)
const list = ref([])
const classes = ref([])

const isTeacher = computed(() => auth.state.user?.role === 'TEACHER')

const filters = reactive({ classId: null, status: 'ALL', keyword: '' })

const statusOptions = computed(() => {
  const opts = [{ label: '全部', value: 'ALL' }]
  if (isTeacher.value) {
    opts.push({ label: '已发布', value: 'PUBLISHED' }, { label: '草稿箱', value: 'DRAFT' })
  } else {
    opts.push({ label: '待完成', value: 'PENDING' })
  }
  return opts
})

const emptyTitle = computed(() => (isTeacher.value ? '还没有符合条件的作业' : '暂时没有需要完成的作业'))
const emptyDesc = computed(() =>
  isTeacher.value ? '点击右上角「布置作业」在线录入一份新作业' : '老师发布作业后会出现在这里'
)

async function load() {
  loading.value = true
  try {
    list.value = await assignmentApi.list({
      classId: filters.classId || undefined,
      status: filters.status,
      keyword: filters.keyword || undefined
    })
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  classes.value = await classApi.list().catch(() => [])
  const query = router.currentRoute.value.query
  if (query.classId) filters.classId = Number(query.classId)
  if (query.status) filters.status = String(query.status)
  await load()
})
</script>

<style scoped>
.toolbar {
  display: flex; align-items: center; gap: 12px; flex-wrap: wrap;
  padding: 14px 18px; margin-bottom: 18px;
}
</style>
