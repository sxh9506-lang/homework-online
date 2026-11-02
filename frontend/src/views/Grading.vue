<template>
  <div>
    <div class="toolbar card">
      <el-select
        v-model="filterAssignmentId"
        placeholder="全部作业"
        clearable
        style="width: 260px"
        @change="loadList"
      >
        <el-option v-for="a in assignments" :key="a.id" :label="a.title" :value="a.id" />
      </el-select>
      <el-radio-group v-model="filterStatus" @change="selectFirstVisible">
        <el-radio-button value="SUBMITTED">待批改</el-radio-button>
        <el-radio-button value="GRADED">已批改</el-radio-button>
        <el-radio-button value="ALL">全部</el-radio-button>
      </el-radio-group>
      <el-input
        v-model="keyword"
        placeholder="搜索学生姓名 / 学号"
        :prefix-icon="Search"
        clearable
        style="width: 220px; margin-left: auto"
      />
      <span class="muted small">共 {{ visibleList.length }} 份</span>
    </div>

    <div class="grading-shell">
      <div class="card grading-list">
        <div class="list-head">
          <span>提交记录</span>
          <span class="pill orange">{{ todoCount }} 待批</span>
        </div>
        <template v-if="visibleList.length">
          <div
            v-for="s in visibleList"
            :key="s.id"
            class="grading-item"
            :class="{ active: current?.id === s.id }"
            @click="select(s)"
          >
            <div class="n">
              <el-avatar :size="22" :style="{ background: avatarColor(s.studentName), fontSize: '10px' }">
                {{ avatarText(s.studentName) }}
              </el-avatar>
              {{ s.studentName }}
              <span class="pill" :class="statusOf(s).type" style="margin-left: auto">{{ statusOf(s).label }}</span>
            </div>
            <div class="m ellipsis">{{ s.assignmentTitle }}</div>
            <div class="m">
              {{ fmtDate(s.submittedAt, 'MM-DD HH:mm') }}
              <span v-if="s.late" style="color: var(--danger)"> · 逾期</span>
              <span v-if="s.status === 'GRADED'" class="score-plain"> · {{ s.score }} 分</span>
            </div>
          </div>
        </template>
        <EmptyState v-else :icon="Finished" title="没有待批改的作业" desc="学生提交后会出现在这里" />
      </div>

      <div v-if="current" class="card">
        <div class="card-head">
          <div style="min-width: 0">
            <h3 style="display: flex; align-items: center; gap: 8px">
              <el-avatar :size="26" :style="{ background: avatarColor(current.studentName), fontSize: '11px' }">
                {{ avatarText(current.studentName) }}
              </el-avatar>
              {{ current.studentName }}
              <span class="muted small" style="font-weight: 400">{{ current.studentNo }}</span>
            </h3>
            <div class="sub">
              {{ current.assignmentTitle }} · {{ current.className }} · 提交于 {{ fmtDate(current.submittedAt) }}
              <span v-if="current.late" style="color: var(--danger)">（逾期提交）</span>
            </div>
          </div>
          <div class="right">
            <el-button size="small" :icon="Document" @click="$router.push(`/assignments/${current.assignmentId}`)">
              查看原题
            </el-button>
            <el-button size="small" :icon="ArrowLeft" :icon-position="'left'" @click="step(-1)" :disabled="!hasPrev">上一份</el-button>
            <el-button size="small" @click="step(1)" :disabled="!hasNext">下一份<el-icon class="el-icon--right"><ArrowRight /></el-icon></el-button>
          </div>
        </div>

        <div class="card-pad grade-body">
          <div class="answer-col">
            <div class="col-title">
              <span>学生作答</span>
              <span class="muted small">{{ (current.content || '').length }} 字</span>
            </div>
            <div v-if="current.content" class="answer-box">{{ current.content }}</div>
            <div v-else class="answer-box muted">学生未录入文字，仅上传了附件。</div>

            <template v-if="current.attachments?.length">
              <div class="col-title" style="margin-top: 18px">上传的附件</div>
              <FileList :files="current.attachments" />
            </template>
          </div>

          <div class="grade-col">
            <div class="col-title">批改打分</div>

            <div class="score-input">
              <el-input-number
                v-model="gradeForm.score"
                :min="0"
                :max="current.totalScore"
                :step="1"
                size="large"
                controls-position="right"
                style="width: 150px"
              />
              <span class="total-score">/ {{ current.totalScore }} 分</span>
              <span
                v-if="gradeForm.score !== null && gradeForm.score !== undefined"
                class="pill"
                :class="ratePill.type"
              >得分率 {{ Math.round((gradeForm.score / current.totalScore) * 100) }}%</span>
            </div>

            <div class="quick-scores">
              <span class="muted small">快捷给分：</span>
              <span
                v-for="p in quickScores"
                :key="p"
                class="rubric-chip"
                @click="gradeForm.score = Math.round(current.totalScore * p)"
              >
                {{ Math.round(current.totalScore * p) }} 分
              </span>
            </div>

            <div class="col-title" style="margin-top: 20px">快捷评语</div>
            <div class="quick-comments">
              <span v-for="c in quickComments" :key="c" class="rubric-chip" @click="appendComment(c)">{{ c }}</span>
            </div>

            <el-input
              v-model="gradeForm.comment"
              type="textarea"
              :rows="6"
              style="margin-top: 12px"
              placeholder="写下批改评语，指出优点与需要改进的地方…"
            />

            <div class="grade-actions">
              <el-button
                type="primary"
                size="large"
                :loading="saving"
                :icon="Select"
                @click="submitGrade('GRADED')"
              >
                完成批改
              </el-button>
              <el-button size="large" :loading="saving" @click="submitGrade('RETURNED')">退回重做</el-button>
              <span v-if="current.status === 'GRADED'" class="muted small" style="margin-left: auto">
                已于 {{ fmtDate(current.gradedAt, 'MM-DD HH:mm') }} 批改，可修改后重新提交
              </span>
            </div>
          </div>
        </div>
      </div>

      <div v-else class="card">
        <EmptyState :icon="Stamp" title="选择左侧一份作业开始批改" desc="可以查看学生作答、上传的照片并给出分数与评语" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Search, Stamp, Finished, Document, ArrowLeft, ArrowRight, Select
} from '@element-plus/icons-vue'
import { assignmentApi, submissionApi } from '../api'
import { SUBMISSION_STATUS, avatarColor, avatarText, fmtDate } from '../utils/format'
import FileList from '../components/FileList.vue'
import EmptyState from '../components/EmptyState.vue'

const route = useRoute()
const router = useRouter()

const assignments = ref([])
const list = ref([])
const current = ref(null)
const filterAssignmentId = ref(null)
const filterStatus = ref('SUBMITTED')
const keyword = ref('')
const saving = ref(false)

const gradeForm = reactive({ score: null, comment: '' })

const statusOf = (s) => SUBMISSION_STATUS[s.status] || { text: s.status, type: 'gray' }

const visibleList = computed(() =>
  list.value.filter((s) => {
    if (filterStatus.value !== 'ALL' && s.status !== filterStatus.value) return false
    if (keyword.value) {
      const k = keyword.value.trim()
      return s.studentName.includes(k) || (s.studentNo || '').includes(k)
    }
    return true
  })
)
const todoCount = computed(() => list.value.filter((s) => s.status === 'SUBMITTED').length)
const index = computed(() => visibleList.value.findIndex((s) => s.id === current.value?.id))
const hasPrev = computed(() => index.value > 0)
const hasNext = computed(() => index.value >= 0 && index.value < visibleList.value.length - 1)

const quickScores = [1, 0.95, 0.9, 0.85, 0.8, 0.7, 0.6]
const quickComments = [
  '完成认真，要点齐全。',
  '思路清晰，但细节有疏漏。',
  '注意书写规范与错别字。',
  '答案不完整，请补全后重交。',
  '有抄袭痕迹，请独立完成。'
]

const ratePill = computed(() => {
  const r = (gradeForm.score || 0) / (current.value?.totalScore || 100)
  if (r >= 0.9) return { type: 'green' }
  if (r >= 0.6) return { type: 'orange' }
  return { type: 'red' }
})

function appendComment(text) {
  gradeForm.comment = gradeForm.comment ? `${gradeForm.comment}\n${text}` : text
}

function select(s) {
  current.value = s
  gradeForm.score = s.status === 'GRADED' ? s.score : null
  gradeForm.comment = s.comment || ''
}

function selectFirstVisible() {
  if (!visibleList.value.some((s) => s.id === current.value?.id)) {
    const first = visibleList.value[0]
    if (first) select(first)
    else current.value = null
  }
}

function step(delta) {
  const next = visibleList.value[index.value + delta]
  if (next) select(next)
}

async function loadList() {
  list.value = await submissionApi.list({
    assignmentId: filterAssignmentId.value || undefined
  })
  const targetId = Number(route.query.submissionId || route.params.submissionId)
  const target = targetId ? list.value.find((s) => s.id === targetId) : null
  if (target) {
    if (target.status !== 'SUBMITTED') filterStatus.value = 'ALL'
    select(target)
  } else {
    selectFirstVisible()
  }
}

async function submitGrade(status) {
  if (gradeForm.score === null || gradeForm.score === undefined || gradeForm.score === '') {
    ElMessage.warning('请先填写分数')
    return
  }
  saving.value = true
  try {
    const updated = await submissionApi.grade(current.value.id, {
      score: gradeForm.score,
      comment: gradeForm.comment,
      status
    })
    ElMessage.success(status === 'RETURNED' ? '已退回给学生重做' : '批改完成，学生端已同步')
    const idx = list.value.findIndex((s) => s.id === updated.id)
    if (idx >= 0) list.value[idx] = updated
    const keep = updated
    await loadList()
    const same = list.value.find((s) => s.id === keep.id)
    if (same) select(same)
  } finally {
    saving.value = false
  }
}

watch(() => route.query.submissionId, () => loadList())

onMounted(async () => {
  assignments.value = await assignmentApi.list({ status: 'PUBLISHED' }).catch(() => [])
  if (route.query.assignmentId) filterAssignmentId.value = Number(route.query.assignmentId)
  await loadList()
})
</script>

<style scoped>
.toolbar {
  display: flex; align-items: center; gap: 12px; flex-wrap: wrap;
  padding: 14px 18px; margin-bottom: 18px;
}
.list-head {
  display: flex; align-items: center; justify-content: space-between;
  padding: 14px 18px; border-bottom: 1px solid var(--line);
  font-size: 13px; font-weight: 600; color: var(--ink-2);
}
.ellipsis { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.score-plain { color: var(--brand); font-weight: 600; }
.grade-body { display: grid; grid-template-columns: 1fr 380px; gap: 24px; align-items: start; }
.answer-col { min-width: 0; }
.grade-col { border-left: 1px solid var(--line); padding-left: 24px; }
.col-title {
  font-size: 13px; font-weight: 600; color: var(--ink-2); margin-bottom: 10px;
  display: flex; align-items: center; gap: 8px;
}
.score-input { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.total-score { color: var(--ink-3); font-size: 13px; }
.quick-scores, .quick-comments { display: flex; gap: 8px; flex-wrap: wrap; margin-top: 12px; align-items: center; }
.grade-actions { display: flex; align-items: center; gap: 10px; margin-top: 18px; flex-wrap: wrap; }
@media (max-width: 1280px) {
  .grade-body { grid-template-columns: 1fr; }
  .grade-col { border-left: 0; padding-left: 0; border-top: 1px solid var(--line); padding-top: 18px; }
}
</style>
