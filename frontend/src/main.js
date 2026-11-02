import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import App from './App.vue'
import router from './router'
import { auth } from './store/auth'
import './styles/main.css'

const app = createApp(App)
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}
app.use(ElementPlus, { locale: zhCn })
app.use(router)
app.mount('#app')

// 已登录时在后台同步一次用户信息（保证侧边栏/顶栏的姓名、角色与后端一致）
if (auth.isLoggedIn.value) {
  auth.refresh()
}
