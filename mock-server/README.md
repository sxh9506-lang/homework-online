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
