# 演示用 Mock API

没有 JDK 环境时，用这个 Node 服务代替 Java 后端，**接口路径、请求体、响应字段与 `backend/` 完全一致**，
前端无需任何改动即可切换到真实后端。

```bash
npm install
npm start          # http://localhost:8080/api ，首次启动生成 db.json 演示数据
```

- 数据文件：`mock-server/db.json`（已加入 .gitignore，删除后重启会重新生成）
- 附件目录：`mock-server/uploads/`
- 令牌为 Base64 编码的演示 token，仅用于本地预览，**不可用于生产**
- 若要与 Java 后端并行，可 `PORT=8081 npm start` 并在前端 `.env.local` 中设置 `VITE_API_TARGET=http://localhost:8081`

## 演示脚本

```bash
node scripts/complete-homework.mjs                       # 学生「陈嘉禾」把作业做完（已提交的跳过）
node scripts/complete-homework.mjs --student moshen      # 学生「魔神弃子」（4 份作业，含逾期那份）
node scripts/complete-homework.mjs --force               # 重新提交
node scripts/complete-homework.mjs --allow-late          # 逾期且不允许补交时，由老师临时放开后提交再恢复
API_BASE=http://localhost:8081 node scripts/complete-homework.mjs
```

脚本走真实 HTTP 接口（登录 → 上传手写稿附件 → 提交），不直接改数据库；删除 `db.json` 重置数据库后可再次运行。
答案库见 `scripts/answers.mjs`（按学生组织，可自行扩展）。
