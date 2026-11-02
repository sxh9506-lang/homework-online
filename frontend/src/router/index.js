import { createRouter, createWebHashHistory } from 'vue-router'
import { auth } from '../store/auth'
import AppLayout from '../layouts/AppLayout.vue'

const routes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('../views/Login.vue'),
    meta: { public: true, title: '登录' }
  },
  {
    path: '/',
    component: AppLayout,
    children: [
      { path: '', redirect: '/dashboard' },
      {
        path: 'dashboard',
        name: 'dashboard',
        component: () => import('../views/Dashboard.vue'),
        meta: { title: '工作台', subtitle: '今天的作业与待办一览' }
      },
      {
        path: 'assignments',
        name: 'assignments',
        component: () => import('../views/AssignmentList.vue'),
        meta: { title: '作业列表', subtitle: '查看、筛选全部家庭作业' }
      },
      {
        path: 'assignments/new',
        name: 'assignment-new',
        component: () => import('../views/AssignmentEdit.vue'),
        meta: { title: '布置作业', subtitle: '在线录入作业要求并发布到班级', roles: ['TEACHER'] }
      },
      {
        path: 'assignments/:id/edit',
        name: 'assignment-edit',
        component: () => import('../views/AssignmentEdit.vue'),
        meta: { title: '编辑作业', subtitle: '修改作业内容或重新发布', roles: ['TEACHER'] }
      },
      {
        path: 'assignments/:id',
        name: 'assignment-detail',
        component: () => import('../views/AssignmentDetail.vue'),
        meta: { title: '作业详情' }
      },
      {
        path: 'grading',
        name: 'grading',
        component: () => import('../views/Grading.vue'),
        meta: { title: '批改中心', subtitle: '逐份查看学生作业并在线批改打分', roles: ['TEACHER'] }
      },
      {
        path: 'grading/:submissionId',
        name: 'grading-detail',
        component: () => import('../views/Grading.vue'),
        meta: { title: '批改中心', subtitle: '逐份查看学生作业并在线批改打分', roles: ['TEACHER'] }
      },
      {
        path: 'classes',
        name: 'classes',
        component: () => import('../views/Classes.vue'),
        meta: { title: '班级管理', subtitle: '管理班级、邀请码与学生名单' }
      },
      {
        path: 'classes/:id',
        name: 'class-detail',
        component: () => import('../views/ClassDetail.vue'),
        meta: { title: '班级详情' }
      },
      {
        path: 'profile',
        name: 'profile',
        component: () => import('../views/Profile.vue'),
        meta: { title: '个人中心', subtitle: '账号信息与使用说明' }
      }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/dashboard' }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

router.beforeEach((to) => {
  if (to.meta.public) {
    if (to.name === 'login' && auth.isLoggedIn.value) return { path: '/dashboard' }
    return true
  }
  if (!auth.isLoggedIn.value) return { name: 'login', query: { redirect: to.fullPath } }
  const roles = to.meta.roles
  if (roles && !roles.includes(auth.state.user?.role)) return { path: '/dashboard' }
  return true
})

export default router
