<template>
  <div v-loading="loading" class="edit-shell">
    <div class="card" style="flex: 1; min-width: 0">
      <div class="card-head">
        <div>
          <h3>{{ isEdit ? '编辑作业' : '在线录入作业' }}</h3>
          <div class="sub">填写作业要求，可先保存草稿，确认无误后发布到班级</div>
        </div>
        <div class="right">
          <el-button :icon="Back" @click="$router.back()">返回</el-button>
        </div>
      </div>

      <div class="card-pad">
        <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
          <el-form-item label="作业标题" prop="title">
            <el-input v-model="form.title" maxlength="80" show-word-limit placeholder="例如：《荷塘月色》第 4-6 段赏析" size="large" />
          </el-form-item>

          <div class="row-3">
            <el-form-item label="布置班级" prop="classId">
              <el-select v-model="form.classId" placeholder="选择班级" style="width: 100%">
                <el-option v-for="c in classes" :key="c.id" :label="`${c.name}（${c.studentCount} 人）`" :value="c.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="满分" prop="totalScore">
              <el-input-number v-model="form.totalScore" :min="1" :max="1000" :step="10" controls-position="right" style="width: 100%" />
            </el-form-item>
            <el-form-item label="截止时间">
              <el-date-picker
                v-model="form.dueAt"
                type="datetime"
                placeholder="不填表示不限时"
                value-format="YYYY-MM-DDTHH:mm:ss"
                style="width: 100%"
              />
            </el-form-item>
          </div>

          <div class="quick-due">
            <span class="muted small">快捷设置：</span>
            <span v-for="p in duePresets" :key="p.label" class="rubric-chip" @click="form.dueAt = p.value">
              {{ p.label }}
            </span>
            <span class="rubric-chip" @click="form.dueAt = null">不限时</span>
          </div>

          <el-form-item prop="content" style="margin-top: 18px">
            <template #label>
              <div class="label-row">
                <span>作业内容 / 要求</span>
                <el-radio-group v-model="editorMode" size="small">
                  <el-radio-button value="edit">编辑</el-radio-button>
                  <el-radio-button value="preview">预览</el-radio-button>
                </el-radio-group>
              </div>
            </template>
            <div v-show="editorMode === 'edit'" style="width: 100%">
              <el-input
                v-model="form.content"
                type="textarea"
                :rows="12"
                resize="vertical"
                placeholder="支持 Markdown 写法：## 小标题、- 列表、**加粗**、> 提示&#10;&#10;例如：&#10;## 作业要求&#10;1. 阅读第 4-6 段；&#10;2. 找出三处比喻句并说明作用。"
              />
              <div class="editor-tools">
                <span class="muted small">支持 Markdown 语法，学生端会渲染为富文本</span>
                <span v-for="t in mdTools" :key="t.label" class="md-tool" @click="insertMd(t.text)">{{ t.label }}</span>
              </div>
            </div>
            <div v-show="editorMode === 'preview'" class="preview-box">
              <MarkdownView v-if="form.content" :content="form.content" />
              <div v-else class="muted small">还没有内容，切换到编辑模式输入吧</div>
            </div>
          </el-form-item>

          <el-form-item label="作业附件（选填，可作为阅读材料下发）">
            <FileUploader v-model="form.attachments" :max="5" />
          </el-form-item>

          <el-form-item>
            <el-checkbox v-model="form.allowLate">允许逾期补交（关闭后截止时间一到学生无法提交）</el-checkbox>
          </el-form-item>
        </el-form>
      </div>

      <div class="footer-bar">
        <span class="muted small">
          {{ form.classId ? `将布置给 ${selectedClassName}` : '请先选择布置班级' }}
          <template v-if="form.dueAt"> · 截止 {{ fmtDate(form.dueAt) }}</template>
        </span>
        <div class="spacer" />
        <el-button @click="$router.back()">取消</el-button>
        <el-button :loading="saving" @click="save('DRAFT')">保存草稿</el-button>
        <el-button type="primary" :loading="publishing" @click="save('PUBLISHED')">
          {{ isEdit && assignment?.status === 'PUBLISHED' ? '保存并更新' : '发布作业' }}
        </el-button>
      </div>
    </div>

    <aside class="side">
      <div class="card card-pad">
        <h4 style="margin-bottom: 12px">📌 录入小提示</h4>
        <ul class="tips">
          <li>作业内容支持 Markdown，使用 <code>## 标题</code>、<code>- 列表</code> 让要求更清晰。</li>
          <li>先保存草稿，学生端不可见；确认后再点击「发布作业」。</li>
          <li>上传阅读材料（PDF / 图片）可让学生直接在线查看。</li>
          <li>设置截止时间后，学生端会显示倒计时提醒。</li>
        </ul>
      </div>

      <div v-if="isEdit && assignment?.status === 'PUBLISHED'" class="card card-pad" style="margin-top: 16px">
        <h4 style="margin-bottom: 12px">📊 当前提交情况</h4>
        <div class="mini-stat">
          <span>已提交</span>
          <b>{{ assignment.submittedCount }} / {{ assignment.studentCount }}</b>
        </div>
        <div class="mini-stat">
          <span>已批改</span>
          <b>{{ assignment.gradedCount }}</b>
        </div>
        <el-button size="small" style="width: 100%; margin-top: 10px" @click="$router.push(`/assignments/${assignment.id}`)">
          查看提交详情
        </el-button>
      </div>
    </aside>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Back } from '@element-plus/icons-vue'
import { assignmentApi, classApi } from '../api'
import { fmtDate } from '../utils/format'
import FileUploader from '../components/FileUploader.vue'
import MarkdownView from '../components/MarkdownView.vue'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const saving = ref(false)
const publishing = ref(false)
const formRef = ref()
const classes = ref([])
const assignment = ref(null)
const editorMode = ref('edit')

const isEdit = computed(() => !!route.params.id)

const form = reactive({
  title: '',
  classId: null,
  totalScore: 100,
  dueAt: null,
  content: '',
  allowLate: true,
  attachments: []
})

const rules = {
  title: [
    { required: true, message: '请输入作业标题', trigger: 'blur' },
    { min: 2, message: '标题至少 2 个字', trigger: 'blur' }
  ],
  classId: [{ required: true, message: '请选择布置班级', trigger: 'change' }],
  content: [{ required: true, message: '请填写作业内容', trigger: 'blur' }],
  totalScore: [{ required: true, message: '请填写满分', trigger: 'change' }]
}

const selectedClassName = computed(() => {
  const c = classes.value.find((x) => x.id === form.classId)
  return c ? c.name : ''
})

const pad = (n) => String(n).padStart(2, '0')
function atHour(offsetDays, hour) {
  const d = new Date()
  d.setDate(d.getDate() + offsetDays)
  d.setHours(hour, 0, 0, 0)
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(hour)}:00:00`
}

const duePresets = [
  { label: '今晚 21:00', value: atHour(0, 21) },
  { label: '明天 21:00', value: atHour(1, 21) },
  { label: '三天后', value: atHour(3, 21) },
  { label: '一周后', value: atHour(7, 21) }
]

const mdTools = [
  { label: 'H2 标题', text: '\n## 小标题\n' },
  { label: '列表', text: '\n- 第一项\n- 第二项\n' },
  { label: '加粗', text: '**重点内容**' },
  { label: '引用', text: '\n> 提示内容\n' }
]

function insertMd(text) {
  form.content = (form.content || '') + text
  editorMode.value = 'edit'
}

async function save(status) {
  const ok = await formRef.value?.validate().catch(() => false)
  if (!ok) return
  if (status === 'PUBLISHED' && isEdit.value && assignment.value?.status === 'PUBLISHED') {
    // 更新已发布作业
  } else if (status === 'PUBLISHED') {
    try {
      await ElMessageBox.confirm(
        `发布后「${selectedClassName.value}」的学生会立即看到这份作业，确定发布吗？`,
        '确认发布',
        { confirmButtonText: '确定发布', cancelButtonText: '再想想', type: 'info' }
      )
    } catch {
      return
    }
  }

  const payload = {
    title: form.title.trim(),
    classId: form.classId,
    totalScore: form.totalScore,
    dueAt: form.dueAt || null,
    content: form.content,
    allowLate: form.allowLate,
    status,
    attachments: form.attachments.map((f) => f.id)
  }

  const flag = status === 'DRAFT' ? saving : publishing
  flag.value = true
  try {
    if (isEdit.value) {
      await assignmentApi.update(route.params.id, payload)
      ElMessage.success(status === 'PUBLISHED' ? '作业已更新并发布' : '草稿已保存')
      router.push(`/assignments/${route.params.id}`)
    } else {
      const created = await assignmentApi.create(payload)
      ElMessage.success(status === 'PUBLISHED' ? '作业已发布，学生可以提交了' : '草稿已保存')
      router.push(status === 'PUBLISHED' ? `/assignments/${created.id}` : '/assignments?status=DRAFT')
    }
  } catch {
    /* ignore */
  } finally {
    flag.value = false
  }
}

onMounted(async () => {
  loading.value = true
  try {
    classes.value = await classApi.list()
    if (isEdit.value) {
      assignment.value = await assignmentApi.detail(route.params.id)
      const a = assignment.value
      Object.assign(form, {
        title: a.title,
        classId: a.classId,
        totalScore: a.totalScore,
        dueAt: a.dueAt,
        content: a.content,
        allowLate: a.allowLate,
        attachments: a.attachments || []
      })
    } else if (route.query.classId) {
      form.classId = Number(route.query.classId)
    } else if (classes.value.length === 1) {
      form.classId = classes.value[0].id
    }
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.edit-shell { display: flex; gap: 18px; align-items: flex-start; }
.side { width: 300px; flex-shrink: 0; }
.row-3 { display: grid; grid-template-columns: 1.3fr 0.8fr 1fr; gap: 14px; }
.label-row { display: flex; align-items: center; justify-content: space-between; width: 100%; }
.editor-tools {
  display: flex; align-items: center; gap: 10px; margin-top: 8px; flex-wrap: wrap;
}
.md-tool {
  font-size: 12px; padding: 3px 9px; border-radius: 6px; border: 1px solid var(--line);
  cursor: pointer; color: var(--ink-2); background: #fbfcff;
}
.md-tool:hover { color: var(--brand); border-color: var(--el-color-primary-light-5); background: var(--brand-soft); }
.preview-box {
  width: 100%; min-height: 240px; border: 1px solid var(--line); border-radius: 10px;
  padding: 16px 18px; background: #fbfcff;
}
.quick-due { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; margin-top: -6px; }
.footer-bar {
  display: flex; align-items: center; gap: 10px;
  padding: 14px 22px; border-top: 1px solid var(--line); background: #fcfdff;
  border-radius: 0 0 var(--radius) var(--radius);
}
.footer-bar .spacer { flex: 1; }
.tips { margin: 0; padding-left: 18px; color: var(--ink-3); font-size: 12.5px; line-height: 1.95; }
.tips code { background: #f2f4fa; padding: 1px 4px; border-radius: 4px; }
.mini-stat {
  display: flex; justify-content: space-between; font-size: 13px; color: var(--ink-3); padding: 6px 0;
}
.mini-stat b { color: var(--ink); font-variant-numeric: tabular-nums; }
@media (max-width: 1180px) {
  .edit-shell { flex-direction: column; }
  .side { width: 100%; }
  .row-3 { grid-template-columns: 1fr; }
}
</style>
