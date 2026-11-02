import { reactive, computed } from 'vue'
import { authApi, TOKEN_KEY, USER_KEY } from '../api'

function readUser() {
  try {
    return JSON.parse(localStorage.getItem(USER_KEY) || 'null')
  } catch {
    return null
  }
}

const state = reactive({
  token: localStorage.getItem(TOKEN_KEY) || '',
  user: readUser(),
  loading: false
})

function persist(token, user) {
  state.token = token
  state.user = user
  if (token) localStorage.setItem(TOKEN_KEY, token)
  else localStorage.removeItem(TOKEN_KEY)
  if (user) localStorage.setItem(USER_KEY, JSON.stringify(user))
  else localStorage.removeItem(USER_KEY)
}

export const auth = {
  state,
  user: computed(() => state.user),
  isLoggedIn: computed(() => !!state.token),
  isTeacher: computed(() => state.user?.role === 'TEACHER'),
  isStudent: computed(() => state.user?.role === 'STUDENT'),

  async login(payload) {
    const res = await authApi.login(payload)
    persist(res.token, res.user)
    return res.user
  },
  async register(payload) {
    const res = await authApi.register(payload)
    persist(res.token, res.user)
    return res.user
  },
  async refresh() {
    if (!state.token) return null
    try {
      const user = await authApi.me()
      persist(state.token, user)
      return user
    } catch {
      return null
    }
  },
  setUser(user) {
    persist(state.token, user)
  },
  logout() {
    persist('', null)
  }
}
