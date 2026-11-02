import axios from 'axios'
import { ElMessage } from 'element-plus'

export const TOKEN_KEY = 'hw_token'
export const USER_KEY = 'hw_user'

export const http = axios.create({ baseURL: '/api', timeout: 60000 })

http.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

http.interceptors.response.use(
  (res) => res.data,
  (error) => {
    const status = error.response?.status
    const message =
      error.response?.data?.message ||
      (error.code === 'ECONNABORTED' ? '请求超时，请稍后重试' : null) ||
      (status ? `请求失败（${status}）` : '网络异常，请确认后端服务已启动')
    if (status === 401) {
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USER_KEY)
      if (!location.hash.includes('/login')) {
        ElMessage.error(message)
        location.hash = '#/login'
      }
    } else {
      ElMessage.error(message)
    }
    return Promise.reject(error)
  }
)

export const authApi = {
  login: (data) => http.post('/auth/login', data),
  register: (data) => http.post('/auth/register', data),
  me: () => http.get('/auth/me'),
  updateProfile: (data) => http.put('/auth/profile', data)
}

export const classApi = {
  list: () => http.get('/classes'),
  detail: (id) => http.get(`/classes/${id}`),
  create: (data) => http.post('/classes', data),
  update: (id, data) => http.put(`/classes/${id}`, data),
  remove: (id) => http.delete(`/classes/${id}`),
  join: (code) => http.post('/classes/join', { code }),
  addStudent: (id, username) => http.post(`/classes/${id}/students`, { username }),
  removeStudent: (id, studentId) => http.delete(`/classes/${id}/students/${studentId}`)
}

export const assignmentApi = {
  list: (params) => http.get('/assignments', { params }),
  detail: (id) => http.get(`/assignments/${id}`),
  create: (data) => http.post('/assignments', data),
  update: (id, data) => http.put(`/assignments/${id}`, data),
  publish: (id) => http.post(`/assignments/${id}/publish`),
  remove: (id) => http.delete(`/assignments/${id}`),
  submissions: (id) => http.get(`/assignments/${id}/submissions`),
  submit: (id, data) => http.post(`/assignments/${id}/submissions`, data)
}

export const submissionApi = {
  mine: () => http.get('/submissions/mine'),
  list: (params) => http.get('/submissions', { params }),
  detail: (id) => http.get(`/submissions/${id}`),
  grade: (id, data) => http.post(`/submissions/${id}/grade`, data),
  withdraw: (id) => http.delete(`/submissions/${id}`)
}

export const fileApi = {
  upload(files, onProgress) {
    const fd = new FormData()
    ;(Array.isArray(files) ? files : [files]).forEach((f) => fd.append('files', f))
    return http.post('/files', fd, {
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress: (e) => {
        if (e.total && onProgress) onProgress(Math.round((e.loaded / e.total) * 100))
      }
    })
  },
  url: (id, download = false) => `/api/files/${id}${download ? '?download=1' : ''}`
}

export const statsApi = {
  overview: () => http.get('/stats/overview')
}
