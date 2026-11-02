<template>
  <div v-loading="loading">
    <div class="back-line">
      <el-link :underline="false" :icon="ArrowLeft" @click="$router.push('/assignments')">返回作业列表</el-link>
    </div>

    <template v-if="assignment">
      <div class="card card-pad head-card">
        <div class="detail-head">
          <div style="flex: 1; min-width: 0">
            <div style="display: flex; align-items: center; gap: 10px; flex-wrap: wrap">
              <span class="pill" :class="statusPill.type">{{ statusPill.label }}</span>
              <span class="pill blue">{{ assignment.className }}</span>
              <span class="pill" :class="due.type"><el-icon><Timer /></el-icon>{{ due.text }}</span>
            </div>
            <h2 class="detail-title" style="margin-top: 10px">{{ assignment.title }}</h2>
            <div class="detail-meta">
              <span class="item"><el-icon><User /></el-icon>{{ assignment.teacherName }} 老师</span>
              <span class="item"><el-icon><Medal /></el-icon>满分 {{ assignment.totalScore }} 分</span>
              <span class="item"><el-icon><Calendar /></el-icon>发布于 {{ fmtDate(assignment.publishedAt || assignment.createdAt, 'YYYY-MM-DD HH:mm') }}</span>
              <span class="item"><el-icon><UserFilled /></el-icon>{{ assignment.studentCount }} 名学生</span>
              <span v-if="assignment.allowLate" class="item"><el-icon><Refresh /></el-icon>允许逾期补交</span>
            </div>
          </div>

          <div v-if="isTeacher" class="head-actions">
            <el-button v-if="assignment.status === 'DRAFT'" type="primary" :icon="Promotion" @click="publish">发布作业</el-button>
            <el-button v-else type="primary" :icon="Stamp" @click="$router.push(`/grading?assignmentId=${assignment.id}`)">
              开始批改
            </el-button>
            <el-button :icon="EditPen" @click="$router.push(`/assignments/${assignment.id}/edit`)">编辑</el-button>
            <el-button :icon="Delete" @click="remove">删除</el-button>
          </div>
          <div v-else class="head-actions">
            <div class="my-score" v-if="mySubmission?.status === 'GRADED'">
              <div class="score-badge">
                <span class="big">{{ mySubmission.score }}</span>
                <span class="total">/ {{ assignment.totalScore }}</span>
              </div>
              <div class="muted small" style="text-align: right">教师评分</div>
            </div>
          </div>
        </div>
      </div>

      <div class="detail-grid">
        <div style="min-width: 0">
          <div class="card">
            <div class="card-head"><h3>作业要求</h3></div>
            <div class="card-pad">
              <MarkdownView :content="assignment.content" />
            </div>
          </div>

          <div v-if="assignment.attachments?.length" class="card" style="margin-top: 16px">
            <div class="card-head">
              <h3>作业附件</h3>
              <div class="right"><span class="muted small">点击可在线预览 / 下载</span></div>
            </div>
            <div class="card-pad">
              <FileList :files="assignment.attachments" />
            </div>
          </div>

          <!-- 教师：提交情况 -->
          <div v-if="isTeacher" class="card" style="margin-top: 16px">
            <div class="card-head">
              <div>
                <h3>学生提交情况</h3>
                <div class="sub">已提交 {{ assignment.submittedCount }} / {{ assignment.studentCount }}，已批改 {{ assignment.gradedCount }}</div>
              </div>
              <div class="right">
                <el-button size="small" :icon="Stamp" @click="$router.push(`/grading?assignmentId=${assignment.id}`)">
                  去批改
                </el-button>
              </div>
            </div>
            <el-table :data="submissionRows" style="width: 100%" :row-class-name="() => ''">
              <el-table-column label="学生" min-width="150">
                <template #default="{ row }">
                  <div class="stu-cell">
                    <el-avatar :size="28" :style="{ background: avatarColor(row.studentName), fontSize: '11px' }">
                      {{ avatarText(row.studentName) }}
                    </el-avatar>
                    <div>
                      <div>{{ row.studentName }}</div>
                      <div class="muted small">{{ row.studentNo }}</div>
                    </div>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="状态" width="110">
                <template #default="{ row }">
                  <span v-if="!row.submission" class="pill gray">未提交</span>
                  <span v-else class="pill" :class="subStatus(row.submission).type">{{ subStatus(row.submission).label }}</span>
                </template>
              </el-table-column>
              <el-table-column label="提交时间" width="160">
                <template #default="{ row }">
                  <span class="small muted">{{ row.submission ? fmtDate(row.submission.submittedAt, 'MM-DD HH:mm') : '—' }}</span>
                </template>
              </el-table-column>
              <el-table-column label="得分" width="100">
                <template #default="{ row }">
                  <span v-if="row.submission?.score !== null && row.submission?.status === 'GRADED'" class="score-inline">
                    {{ row.submission.score }}<i>/{{ assignment.totalScore }}</i>
                  </span>
                  <span v-else class="muted">—</span>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="110" align="right">
                <template #default="{ row }">
                  <el-button
                    v-if="row.submission"
                    link
                    type="primary"
                    size="small"
                    @click="$router.push(`/grading?assignmentId=${assignment.id}&submissionId=${row.submission.id}`)"
                  >
                    {{ row.submission.status === 'GRADED' ? '查看批改' : '批改' }}
                  </el-button>
                  <span v-else class="muted small">等待提交</span>
                </template>
              </el-table-column>
              <template #empty>
                <EmptyState title="暂无学生" desc="请先在班级中添加学生" />
              </template>
            </el-table>
          </div>

          <!-- 学生：我的作业 -->
          <div v-else class="card" style="margin-top: 16px">
            <div class="card-head">
              <div>
                <h3>{{ mySubmission ? '我的提交' : '提交作业' }}</h3>
                <div class="sub">
                  {{
                    mySubmission
                      ? `提交于 ${fmtDate(mySubmission.submittedAt)}${mySubmission.late ? '（逾期提交）' : ''}`
                      : '可在线录入内容，也可以拍照 / 上传文件'
                  }}
                </div>
              </div>
              <div class="right">
                <span v-if="mySubmission" class="pill" :class="subStatus(mySubmission).type">{{ subStatus(mySubmission).label }}</span>
              </div>
            </div>

            <div class="card-pad">
              <!-- 已批改 -->
              <template v-if="mySubmission && mySubmission.status === 'GRADED'">
                <div class="result-strip">
                  <div>
                    <div class="muted small">我的得分</div>
                    <div class="score-badge">
                      <span class="big">{{ mySubmission.score }}</span>
                      <span class="total">/ {{ assignment.totalScore }}</span>
                    </div>
                  </div>
                  <div style="flex: 1">
                    <div class="muted small" style="margin-bottom: 6px">老师评语</div>
                    <div class="comment-box">{{ mySubmission.comment || '老师没有留下评语' }}</div>
                  </div>
                </div>
                <div class="answer-title">我的作答</div>
                <div class="answer-box">{{ mySubmission.content || '（仅上传了附件）' }}</div>
                <div v-if="mySubmission.attachments?.length" style="margin-top: 14px">
                  <FileList :files="mySubmission.attachments" />
                </div>
              </template>

              <!-- 待批改 / 已退回 / 未提交 -->
              <template v-else>
                <div v-if="mySubmission" class="pending-hint">
                  <el-icon><Clock /></el-icon>
                  <span>作业已提交，等待老师批改。如需修改可在下方重新提交。</span>
                </div>

                <el-form label-position="top">
                  <el-form-item label="作答内容">
                    <el-input
                      v-model="submitForm.content"
                      type="textarea"
                      :rows="8"
                      placeholder="可以在这里直接录入答案，或上传手写稿照片 / 文档"
                      :disabled="!canSubmit"
                    />
                  </el-form-item>
                  <el-form-item label="上传附件（手写稿照片、文档等，最多 5 个）">
                    <FileUploader v-model="submitForm.attachments" :max="5" :disabled="!canSubmit" />
                  </el-form-item>
                </el-form>

                <div v-if="!canSubmit" class="locked-hint">
                  <el-icon><Lock /></el-icon>
                  作业已截止且不允许补交，无法再提交。如需补交请联系老师。
                </div>

                <div style="display: flex; align-items: center; gap: 12px">
                  <el-button
                    type="primary"
                    size="large"
                    :loading="submitting"
                    :disabled="!canSubmit"
                    :icon="Upload"
                    @click="doSubmit"
                  >
                    {{ mySubmission ? '重新提交' : '提交作业' }}
                  </el-button>
                  <span class="muted small">
                    {{ mySubmission ? '重新提交会覆盖上一次的答案' : '提交后老师即可看到并批改' }}
                  </span>
                </div>
              </template>
            </div>
          </div>
        </div>

        <aside class="side">
          <div class="card card-pad">
            <h4 style="margin-bottom: 14px">{{ isTeacher ? '作业进度' : '我的状态' }}</h4>

            <template v-if="isTeacher">
              <div class="ring-wrap">
                <el-progress
                  type="dashboard"
                  :percentage="submitRate"
                  :width="130"
                  :stroke-width="12"
                  :color="progressColor"
                >
                  <template #default="{ percentage }">
                    <div class="ring-num">{{ percentage }}%</div>
                    <div class="muted small">提交率</div>
                  </template>
                </el-progress>
              </div>
              <div class="kv"><span>学生总数</span><b>{{ assignment.studentCount }} 人</b></div>
              <div class="kv"><span>已提交</span><b>{{ assignment.submittedCount }} 份</b></div>
              <div class="kv"><span>未提交</span><b :class="{ warnText: missing.length > 0 }">{{ missing.length }} 人</b></div>
              <div class="kv"><span>已批改</span><b>{{ assignment.gradedCount }} 份</b></div>
              <div class="kv"><span>待批改</span><b :class="{ warnText: pendingGrade > 0 }">{{ pendingGrade }} 份</b></div>
            </template>

            <template v-else>
              <div class="status-block">
                <div class="big-status" :class="myState.cls">{{ myState.text }}</div>
                <div class="muted small" style="margin-top: 6px">{{ myState.hint }}</div>
              </div>
              <div class="kv"><span>截止时间</span><b>{{ assignment.dueAt ? fmtDate(assignment.dueAt) : '不限时' }}</b></div>
              <div class="kv"><span>满分</span><b>{{ assignment.totalScore }} 分</b></div>
              <div v-if="mySubmission" class="kv"><span>提交时间</span><b>{{ fmtDate(mySubmission.submittedAt, 'MM-DD HH:mm') }}</b></div>
              <div v-if="mySubmission?.gradedAt" class="kv"><span>批改时间</span><b>{{ fmtDate(mySubmission.gradedAt, 'MM-DD HH:mm') }}</b></div>
            </template>
          </div>

          <div v-if="isTeacher && missing.length" class="card card-pad" style="margin-top: 16px">
            <h4 style="margin-bottom: 12px">未提交学生（{{ missing.length }}）</h4>
            <div class="missing-list">
              <span v-for="m in missing" :key="m.studentId" class="pill gray">{{ m.studentName }}</span>
            </div>
          </div>

          <div v-if="!isTeacher && mySubmission?.status === 'GRADED'" class="card card-pad" style="margin-top: 16px">
            <h4 style="margin-bottom: 10px">得分率</h4>
            <el-progress
              :percentage="Math.round((mySubmission.score / assignment.totalScore) * 100)"
              :stroke-width="10"
              :color="progressColor"
            />
            <div class="muted small" style="margin-top: 8px">
              {{ rateComment }}
            </div>
          </div>
        </aside>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowLeft, Timer, User, UserFilled, Medal, Calendar, Refresh, Promotion, Stamp,
  EditPen, Delete, Clock, Lock, Upload
} from '@element-plus/icons-vue'
import { assignmentApi } from '../api'
import { auth } from '../store/auth'
import {
  ASSIGNMENT_STATUS, SUBMISSION_STATUS, avatarColor, avatarText, dueState, fmtDate
} from '../utils/format'
import MarkdownView from '../components/MarkdownView.vue'
import FileList from '../components/FileList.vue'
import FileUploader from '../components/FileUploader.vue'
import EmptyState from '../components/EmptyState.vue'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const submitting = ref(false)
const assignment = ref(null)
const submissionRows = ref([])

const submitForm = reactive({ content: '', attachments: [] })

const isTeacher = computed(() => auth.state.user?.role === 'TEACHER')
const mySubmission = computed(() => assignment.value?.mySubmission)
const due = computed(() => dueState(assignment.value?.dueAt))
const statusPill = computed(() => ASSIGNMENT_STATUS[assignment.value?.status] || { text: '', type: 'gray' })
const subStatus = (s) => SUBMISSION_STATUS[s?.status] || { text: '—', type: 'gray' }

const submitRate = computed(() => {
  if (!assignment.value?.studentCount) return 0
  return Math.round((assignment.value.submittedCount / assignment.value.studentCount) * 100)
})
const pendingGrade = computed(() => assignment.value?.submittedCount - assignment.value?.gradedCount || 0)
const missing = computed(() => submissionRows.value.filter((r) => !r.submission))
const progressColor = computed(() => (submitRate.value >= 100 ? '#10a37f' : '#4a5cf5'))

const canSubmit = computed(() => {
  if (!assignment.value) return false
  if (assignment.value.status !== 'PUBLISHED') return false
  if (mySubmission.value?.status === 'GRADED') return false
  const overdue = assignment.value.dueAt && new Date(assignment.value.dueAt) < new Date()
  if (overdue && !assignment.value.allowLate) return false
  return true
})

const myState = computed(() => {
  if (!mySubmission.value) {
    return due.value.overdue
      ? { text: '已逾期未交', hint: assignment.value?.allowLate ? '仍可补交，请尽快提交' : '已无法提交，请联系老师', cls: 'danger' }
      : { text: '待完成', hint: '完成后在右侧提交作业', cls: 'warn' }
  }
  if (mySubmission.value.status === 'GRADED') {
    return { text: '已批改', hint: `得分 ${mySubmission.value.score} / ${assignment.value.totalScore}`, cls: 'ok' }
  }
  if (mySubmission.value.status === 'RETURNED') {
    return { text: '已退回', hint: '按老师要求修改后重新提交', cls: 'danger' }
  }
  return { text: '等待批改', hint: '老师正在批改中，请耐心等待', cls: 'warn' }
})

const rateComment = computed(() => {
  if (!mySubmission.value) return ''
  const r = (mySubmission.value.score / assignment.value.totalScore) * 100
  if (r >= 90) return '表现优秀，继续保持！'
  if (r >= 75) return '完成得不错，注意细节可以更好。'
  if (r >= 60) return '基本达标，建议结合评语订正。'
  return '需要加强练习，建议复习后重新作答。'
})

async function load() {
  loading.value = true
  try {
    assignment.value = await assignmentApi.detail(route.params.id)
    submissionRows.value = assignment.value.submissions || []
    if (mySubmission.value && mySubmission.value.status !== 'GRADED') {
      submitForm.content = mySubmission.value.content || ''
      submitForm.attachments = mySubmission.value.attachments || []
    }
  } finally {
    loading.value = false
  }
}

async function doSubmit() {
  const content = submitForm.content.trim()
  if (!content && submitForm.attachments.length === 0) {
    ElMessage.warning('请先填写作答内容或上传附件')
    return
  }
  submitting.value = true
  try {
    await assignmentApi.submit(route.params.id, {
      content,
      attachments: submitForm.attachments.map((f) => f.id)
    })
    ElMessage.success('提交成功，等待老师批改')
    await load()
  } catch {
    /* ignore */
  } finally {
    submitting.value = false
  }
}

async function publish() {
  await assignmentApi.publish(route.params.id)
  ElMessage.success('作业已发布')
  await load()
}

async function remove() {
  try {
    await ElMessageBox.confirm('删除后所有学生提交的记录也会一并删除，确定删除吗？', '删除作业', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  await assignmentApi.remove(route.params.id)
  ElMessage.success('作业已删除')
  router.push('/assignments')
}

onMounted(load)
</script>

<style scoped>
.back-line { margin-bottom: 14px; }
.head-card { margin-bottom: 18px; }
.head-actions { display: flex; gap: 8px; flex-wrap: wrap; align-items: flex-start; }
.detail-grid { display: grid; grid-template-columns: 1fr 300px; gap: 18px; align-items: start; }
.side { display: flex; flex-direction: column; gap: 0; }
.stu-cell { display: flex; align-items: center; gap: 10px; }
.score-inline { font-weight: 650; color: var(--brand); font-variant-numeric: tabular-nums; }
.score-inline i { font-style: normal; font-weight: 400; color: var(--ink-3); font-size: 11px; }
.my-score { text-align: right; }
.ring-wrap { display: flex; justify-content: center; margin-bottom: 12px; }
.ring-num { font-size: 24px; font-weight: 650; letter-spacing: -0.02em; }
.kv {
  display: flex; justify-content: space-between; gap: 10px;
  font-size: 13px; color: var(--ink-3); padding: 8px 0; border-bottom: 1px dashed #f0f2f7;
}
.kv:last-child { border-bottom: 0; }
.kv b { color: var(--ink); font-weight: 550; }
.warnText { color: #d97706 !important; }
.status-block { padding-bottom: 12px; }
.big-status { font-size: 20px; font-weight: 650; letter-spacing: -0.01em; }
.big-status.ok { color: var(--ok); }
.big-status.warn { color: #d97706; }
.big-status.danger { color: var(--danger); }
.result-strip { display: flex; gap: 22px; align-items: flex-start; flex-wrap: wrap; margin-bottom: 18px; }
.answer-title { font-size: 13px; color: var(--ink-3); margin: 16px 0 8px; }
.pending-hint {
  display: flex; align-items: center; gap: 8px; padding: 10px 14px; margin-bottom: 16px;
  background: #fffaf0; border: 1px solid #fbe9c6; border-radius: 10px; color: #92660a; font-size: 13px;
}
.locked-hint {
  display: flex; align-items: center; gap: 8px; padding: 10px 14px; margin-bottom: 16px;
  background: #fdf0f0; border: 1px solid #f8d7d8; border-radius: 10px; color: #b03033; font-size: 13px;
}
.missing-list { display: flex; flex-wrap: wrap; gap: 8px; }
@media (max-width: 1180px) {
  .detail-grid { grid-template-columns: 1fr; }
}
</style>
