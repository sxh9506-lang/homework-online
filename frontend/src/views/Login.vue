<template>
  <div class="auth-wrap">
    <aside class="auth-aside">
      <div class="brand" style="padding: 0">
        <div class="brand-logo"><el-icon><Reading /></el-icon></div>
        <div>
          <div class="brand-name">作业云</div>
          <div class="brand-sub">HOMEWORK ONLINE</div>
        </div>
      </div>

      <h1 class="auth-title">让每一次<em>布置与批改</em><br />都更轻松一点</h1>
      <p class="auth-desc">
        老师在线录入作业要求、一键发布到班级；学生拍照上传或直接在线作答；
        批改结果实时同步，得分与评语一目了然。
      </p>

      <div class="auth-features">
        <div v-for="f in features" :key="f.t" class="auth-feature">
          <div class="ico"><el-icon><component :is="f.icon" /></el-icon></div>
          <div>
            <div class="t">{{ f.t }}</div>
            <div class="d">{{ f.d }}</div>
          </div>
        </div>
      </div>
    </aside>

    <div class="auth-form-wrap">
      <div class="auth-form">
        <template v-if="mode === 'login'">
          <h1>欢迎回来 👋</h1>
          <p class="lead">登录后即可查看你的班级、家庭作业与批改结果</p>

          <el-form ref="loginRef" :model="loginForm" :rules="loginRules" size="large" @submit.prevent>
            <el-form-item prop="username">
              <el-input v-model="loginForm.username" placeholder="用户名" :prefix-icon="User" clearable />
            </el-form-item>
            <el-form-item prop="password">
              <el-input
                v-model="loginForm.password"
                type="password"
                placeholder="密码"
                :prefix-icon="Lock"
                show-password
                @keyup.enter="doLogin"
              />
            </el-form-item>
            <el-button type="primary" size="large" style="width: 100%; margin-top: 4px" :loading="loading" @click="doLogin">
              登录
            </el-button>
          </el-form>

          <div style="margin-top: 16px; text-align: center" class="small muted">
            还没有账号？
            <el-link type="primary" :underline="false" @click="mode = 'register'">学生注册</el-link>
          </div>

          <div class="demo-accounts">
            <div class="t">演示账号（点击直接登录）</div>
            <div class="demo-btns">
              <button class="demo-btn" @click="quickLogin('zeng')">
                <div class="n">曾芮 · 语文老师</div>
                <div class="r">zeng / 123456</div>
              </button>
              <button class="demo-btn" @click="quickLogin('zhang')">
                <div class="n">张小明 · 高一学生</div>
                <div class="r">zhang / 123456</div>
              </button>
            </div>
          </div>
        </template>

        <template v-else>
          <h1>学生注册</h1>
          <p class="lead">注册后输入老师给的班级邀请码即可加入班级</p>

          <el-form ref="regRef" :model="regForm" :rules="regRules" size="large" label-position="top">
            <el-form-item prop="name" label="姓名">
              <el-input v-model="regForm.name" placeholder="真实姓名" :prefix-icon="User" />
            </el-form-item>
            <el-form-item prop="username" label="用户名">
              <el-input v-model="regForm.username" placeholder="用于登录，字母或数字" :prefix-icon="Avatar" />
            </el-form-item>
            <el-form-item prop="password" label="密码">
              <el-input v-model="regForm.password" type="password" placeholder="至少 6 位" :prefix-icon="Lock" show-password />
            </el-form-item>
            <el-form-item prop="studentNo" label="学号（选填）">
              <el-input v-model="regForm.studentNo" placeholder="例如 20240107" :prefix-icon="Postcard" />
            </el-form-item>
            <el-form-item prop="classCode" label="班级邀请码（选填）">
              <el-input v-model="regForm.classCode" placeholder="例如 G1-3" :prefix-icon="School" />
            </el-form-item>
            <el-button type="primary" size="large" style="width: 100%" :loading="loading" @click="doRegister">
              注册并登录
            </el-button>
          </el-form>

          <div style="margin-top: 16px; text-align: center" class="small muted">
            已有账号？
            <el-link type="primary" :underline="false" @click="mode = 'login'">返回登录</el-link>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Reading, User, Lock, Avatar, Postcard, School, EditPen, UploadFilled, Stamp, DataLine
} from '@element-plus/icons-vue'
import { auth } from '../store/auth'

const router = useRouter()
const route = useRoute()

const mode = ref('login')
const loading = ref(false)
const loginRef = ref()
const regRef = ref()

const features = [
  { icon: EditPen, t: '在线布置作业', d: '富文本录入要求，可设置截止时间与满分，支持附件材料' },
  { icon: UploadFilled, t: '拍照 / 文件上传', d: '学生可上传手写稿照片或文档，也可直接在系统中作答' },
  { icon: Stamp, t: '在线批改打分', d: '逐份批改、写评语、给出分数，学生实时收到反馈' },
  { icon: DataLine, t: '进度一目了然', d: '提交率、待批改数量、平均分等关键数据实时统计' }
]

const loginForm = reactive({ username: '', password: '' })
const loginRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const regForm = reactive({ name: '', username: '', password: '', studentNo: '', classCode: '' })
const regRules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9_]{3,20}$/, message: '3-20 位字母、数字或下划线', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' }
  ]
}

function afterLogin(user) {
  ElMessage.success(`欢迎回来，${user.name}`)
  const redirect = route.query.redirect
  router.replace(redirect || '/dashboard')
}

async function doLogin() {
  const ok = await loginRef.value?.validate().catch(() => false)
  if (!ok) return
  loading.value = true
  try {
    const user = await auth.login({ username: loginForm.username.trim(), password: loginForm.password })
    afterLogin(user)
  } catch {
    /* 错误提示由拦截器处理 */
  } finally {
    loading.value = false
  }
}

async function quickLogin(username) {
  loginForm.username = username
  loginForm.password = '123456'
  await doLogin()
}

async function doRegister() {
  const ok = await regRef.value?.validate().catch(() => false)
  if (!ok) return
  loading.value = true
  try {
    const user = await auth.register({ ...regForm })
    afterLogin(user)
  } catch {
    /* ignore */
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-aside .brand-logo { box-shadow: 0 6px 18px rgba(74, 92, 245, 0.5); }
</style>
