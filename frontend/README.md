# 作业云 · 前端

Vue 3 + Vite + Element Plus，前后端分离；开发时 `/api` 由 Vite 代理到后端。

## 开发

```bash
npm install
npm run dev          # http://localhost:5173
```

后端地址通过环境变量指定（默认 http://localhost:8080），复制 `.env.example` 为 `.env.local` 后修改：

```
VITE_API_TARGET=http://localhost:8080
```

## 构建

```bash
npm run build        # 产物 dist/
npm run preview
```

## 页面结构

| 路由 | 说明 |
| --- | --- |
| `/login` | 登录 / 学生注册（含一键演示账号） |
| `/dashboard` | 工作台：统计卡片、待办、最近动态 |
| `/assignments` | 作业列表（按班级、状态、关键字筛选） |
| `/assignments/new`、`/assignments/:id/edit` | 在线录入 / 编辑作业（Markdown 编辑与预览、附件上传） |
| `/assignments/:id` | 作业详情：学生作答与提交、教师查看提交进度与未交名单 |
| `/grading` | 批改中心：左列表右批改，快捷给分与评语、退回重做 |
| `/classes`、`/classes/:id` | 班级管理 / 详情（邀请码、学生名单、班级作业） |
| `/profile` | 个人中心 |

## 约定

- 所有接口封装在 `src/api/index.js`，统一处理 JWT 与错误提示（401 自动跳登录）。
- `src/styles/main.css` 为设计系统（颜色、卡片、标签、侧边栏等），Element Plus 主题色通过 CSS 变量覆盖。
- 组件：`AssignCard`（作业卡片）、`FileUploader`（拖拽上传）、`FileList`（附件预览/下载）、`MarkdownView`（作业内容渲染）、`EmptyState`。
