<template>
  <div class="profile-grid">
    <div class="card">
      <div class="card-pad profile-head">
        <el-avatar :size="66" :style="{ background: avatarColor(user.name), fontSize: '22px' }">
          {{ avatarText(user.name) }}
        </el-avatar>
        <div style="flex: 1">
          <h2 style="font-size: 20px">{{ user.name }}</h2>
          <div class="muted small" style="margin-top: 6px">
            {{ isTeacher ? `教师${user.subject ? ' · ' + user.subject : ''}` : `学生${user.studentNo ? ' · 学号 ' + user.studentNo : ''}` }}
          </div>
        </div>
        <span class="pill" :class="isTeacher ? 'blue' : 'green'">{{ isTeacher ? '教师账号' : '学生账号' }}</span>
      </div>

      <div class="card-pad" style="border-top: 1px solid var(--line)">
        <el-form label-position="top" style="max-width: 460px">
          <el-form-item label="用户名（登录账号）">
            <el-input :model-value="user.username" disabled />
          </el-form-item>
          <el-form-item label="姓名">
            <el-input v-model="form.name" />
          </el-form-item>
          <el-form-item label="邮箱">
            <el-input v-model="form.email" placeholder="用于接收通知（选填）" />
          </el-form-item>
          <el-button type="primary" :loading="saving" @click="save">保存修改</el-button>
        </el-form>
      </div>
    </div>

    <div>
      <div class="card card-pad">
        <h4 style="margin-bottom: 12px">📖 使用说明</h4>
        <ol class="guide">
          <li v-if="isTeacher">在「班级管理」中创建班级，把邀请码发给学生。</li>
          <li v-if="isTeacher">在「布置作业」中在线录制作业要求，可上传阅读材料，设置截止时间与满分。</li>
          <li v-if="isTeacher">学生提交后，在「批改中心」逐份查看作答与附件，打分并写评语。</li>
          <li v-if="!isTeacher">使用老师提供的邀请码加入班级。</li>
          <li v-if="!isTeacher">在「作业列表」或工作台中打开作业，直接在线作答或上传手写稿照片。</li>
          <li v-if="!isTeacher">老师批改后，作业详情页会显示得分与评语。</li>
        </ol>
      </div>

      <div class="card card-pad" style="margin-top: 16px">
        <div style="display: flex; align-items: center; gap: 10px; margin-bottom: 12px">
          <h4 style="margin: 0">🩺 连接诊断</h4>
          <el-button size="small" :loading="diagnosing" style="margin-left: auto" @click="runDiagnose">
            开始诊断
          </el-button>
        </div>
        <div class="muted small" style="line-height: 1.9; margin-bottom: 10px">
          若出现「登录后又提示未登录」，点这里可以确认后端是否可达、登录凭证走了哪条通道（请求头 / Cookie）。
        </div>
        <div v-if="diagnose" class="diag">
          <div class="diag-row">
            <span>鉴权结果</span>
            <b :class="diagnose.ok ? 'ok' : 'bad'">{{ diagnose.ok ? '正常' : '异常' }}</b>
          </div>
          <div class="diag-row"><span>凭证通道</span><b>{{ diagSource }}</b></div>
          <div class="diag-row"><span>Authorization 头</span><b>{{ diagnose.hasAuthorizationHeader ? '已发送' : '未发送' }}</b></div>
          <div class="diag-row"><span>Cookie 通道</span><b>{{ diagnose.hasCookie ? '已发送' : '未发送' }}</b></div>
          <div class="diag-row"><span>本地令牌</span><b>{{ localToken ? `已保存（${localToken.length} 字符）` : '无' }}</b></div>
          <div class="diag-row"><span>服务端识别用户</span><b>{{ diagnose.user ? `${diagnose.user.name}（${diagnose.user.role}）` : '—' }}</b></div>
          <div v-if="diagnose.reason" class="diag-reason">{{ diagnose.reason }}</div>
        </div>
      </div>

      <div class="card card-pad" style="margin-top: 16px">
        <h4 style="margin-bottom: 12px">🔐 账号安全</h4>
        <div class="muted small" style="line-height: 1.9">
          当前使用 JWT 无状态登录，令牌保存在浏览器本地；如遇令牌过期，系统会自动跳转登录页。
          演示环境密码统一为 <code>123456</code>，正式部署请接入学校统一认证或修改初始密码。
        </div>
        <el-button size="small" style="margin-top: 12px" @click="logout">退出登录</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { auth } from '../store/auth'
import { authApi, readToken } from '../api'
import { avatarColor, avatarText } from '../utils/format'

const router = useRouter()
const user = computed(() => auth.state.user || {})
const isTeacher = computed(() => user.value.role === 'TEACHER')

const saving = ref(false)
const form = reactive({ name: user.value.name || '', email: user.value.email || '' })

const diagnose = ref(null)
const diagnosing = ref(false)
const localToken = computed(() => readToken())
const diagSource = computed(() => {
  const map = {
    'authorization-header': 'Authorization 请求头',
    'x-auth-token-header': 'X-Auth-Token 请求头',
    query: 'URL 参数',
    cookie: 'Cookie（请求头通道被拦截时的兜底）',
    none: '未携带任何凭证'
  }
  return map[diagnose.value?.tokenSource] || diagnose.value?.tokenSource || '—'
})

async function runDiagnose() {
  diagnosing.value = true
  try {
    diagnose.value = await authApi.diagnose()
    ElMessage[diagnose.value.ok ? 'success' : 'warning'](
      diagnose.value.ok ? '鉴权正常，后端连接通畅' : diagnose.value.reason || '鉴权异常'
    )
  } catch (e) {
    diagnose.value = {
      ok: false,
      tokenSource: 'none',
      reason: '无法连接后端接口，请确认后端服务已启动（默认 8080）',
      user: null
    }
    ElMessage.error('诊断请求失败')
  } finally {
    diagnosing.value = false
  }
}

async function save() {
  saving.value = true
  try {
    const updated = await authApi.updateProfile({ ...form })
    auth.setUser(updated)
    ElMessage.success('资料已更新')
  } finally {
    saving.value = false
  }
}

function logout() {
  auth.logout()
  router.replace('/login')
}
</script>

<style scoped>
.profile-grid { display: grid; grid-template-columns: 1.1fr 1fr; gap: 18px; align-items: start; }
.profile-head { display: flex; align-items: center; gap: 18px; }
.guide { margin: 0; padding-left: 20px; color: var(--ink-2); font-size: 13px; line-height: 2.1; }
code { background: #f2f4fa; padding: 1px 5px; border-radius: 4px; }
.diag { border-top: 1px dashed var(--line); padding-top: 10px; }
.diag-row {
  display: flex; justify-content: space-between; gap: 10px;
  font-size: 12.5px; color: var(--ink-3); padding: 5px 0;
}
.diag-row b { color: var(--ink); font-weight: 550; }
.diag-row b.ok { color: var(--ok); }
.diag-row b.bad { color: var(--danger); }
.diag-reason {
  margin-top: 8px; padding: 8px 10px; border-radius: 8px;
  background: #fdf0f0; color: #b03033; font-size: 12.5px; line-height: 1.7;
}
@media (max-width: 1000px) {
  .profile-grid { grid-template-columns: 1fr; }
}
</style>
