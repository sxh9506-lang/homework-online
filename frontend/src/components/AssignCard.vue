<template>
  <div class="assign-card" @click="$router.push(`/assignments/${assignment.id}`)">
    <div class="head-row">
      <div class="assign-title">{{ assignment.title }}</div>
      <span class="pill" :class="statusPill.type">{{ statusPill.label }}</span>
    </div>

    <div class="assign-meta">
      <span class="pill blue">{{ assignment.className }}</span>
      <span class="pill" :class="due.type">
        <el-icon><Timer /></el-icon>{{ due.text }}
      </span>
      <span v-if="assignment.attachments?.length" class="pill gray">
        <el-icon><Paperclip /></el-icon>{{ assignment.attachments.length }}
      </span>
    </div>

    <div v-if="isTeacher && assignment.status === 'PUBLISHED'">
      <div class="progress-line">
        <span>提交进度</span>
        <span class="mono-num">{{ assignment.submittedCount }}/{{ assignment.studentCount }}</span>
      </div>
      <div class="progress-track">
        <div
          class="progress-bar"
          :class="{ green: ratio === 100 }"
          :style="{ width: (ratio || 0) + '%' }"
        />
      </div>
    </div>

    <div v-if="!isTeacher" class="assign-foot">
      <template v-if="assignment.mySubmission">
        <span class="pill" :class="subStatus.type">{{ subStatus.label }}</span>
        <span v-if="assignment.mySubmission.status === 'GRADED'" class="score-mini">
          {{ assignment.mySubmission.score }}<i>/{{ assignment.totalScore }}</i>
        </span>
        <span class="muted small" style="margin-left: auto">提交于 {{ fromNow(assignment.mySubmission.submittedAt) }}</span>
      </template>
      <template v-else>
        <span class="pill" :class="assignment.overdue ? 'red' : 'orange'">
          {{ assignment.overdue ? '已逾期未交' : '待完成' }}
        </span>
        <span class="muted small" style="margin-left: auto">去完成 →</span>
      </template>
    </div>

    <div v-else-if="isTeacher" class="assign-foot">
      <span class="muted small">已批 {{ assignment.gradedCount }} 份</span>
      <span class="muted small" style="margin-left: auto">发布于 {{ fmtDate(assignment.publishedAt || assignment.createdAt, 'MM-DD') }}</span>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { Timer, Paperclip } from '@element-plus/icons-vue'
import { auth } from '../store/auth'
import { dueState, fmtDate, fromNow, ASSIGNMENT_STATUS, SUBMISSION_STATUS } from '../utils/format'

const props = defineProps({ assignment: { type: Object, required: true } })

const isTeacher = computed(() => auth.state.user?.role === 'TEACHER')
const due = computed(() => dueState(props.assignment.dueAt))
const statusPill = computed(() => ASSIGNMENT_STATUS[props.assignment.status] || { text: props.assignment.status, type: 'gray' })
const subStatus = computed(() =>
  SUBMISSION_STATUS[props.assignment.mySubmission?.status] || { text: '已提交', type: 'blue' }
)
const ratio = computed(() => {
  const total = props.assignment.studentCount || 0
  if (!total) return 0
  return Math.round(((props.assignment.submittedCount || 0) / total) * 100)
})
</script>

<style scoped>
.head-row { display: flex; align-items: flex-start; gap: 10px; }
.head-row .pill { flex-shrink: 0; margin-top: 2px; }
.progress-line {
  display: flex; justify-content: space-between; font-size: 12px; color: var(--ink-3); margin-bottom: 6px;
}
.score-mini {
  font-size: 17px; font-weight: 700; color: var(--brand); font-variant-numeric: tabular-nums;
}
.score-mini i { font-style: normal; font-size: 12px; color: var(--ink-3); font-weight: 500; }
</style>
