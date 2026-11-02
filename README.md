# 作业云 · 家庭作业在线系统

前后端分离的家庭作业在线系统：老师**在线录入作业**、一键发布到班级；学生**在线作答或上传作业**（拍照 / 附件）；
老师**在线批改打分写评语**，学生实时看到成绩与反馈。

- 后端：**Java 17 + Spring Boot 3 + Spring Security(JWT) + Spring Data JPA + H2/MySQL**
- 前端：**Vue 3 + Vite + Vue Router + Element Plus + Axios**
- 演示：附带一个 Node 编写的 Mock API（与 Java 后端**完全同一套 REST 契约**），没有 JDK 环境也能把整套界面跑起来

---

## 一、功能一览

| 角色 | 功能 |
| --- | --- |
| 教师 | 工作台数据看板（班级数、学生数、待批改、平均分）、创建班级与邀请码、添加/移除学生、**在线录入作业**（Markdown 富文本、附件材料、截止时间、满分、允许逾期补交）、草稿箱、发布/编辑/删除作业、查看提交进度与未交名单、**批改中心**逐份批改（快捷给分、快捷评语、退回重做） |
| 学生 | 工作台待办与倒计时、按班级/状态筛选作业、查看作业要求与附件、**在线录入作答**、**上传手写稿照片/文档**、查看提交状态、查看得分与老师评语、逾期提示与补交规则 |
| 通用 | JWT 登录注册、学生凭邀请码加入班级、个人资料修改、响应式布局（窄屏折叠侧边栏） |

---

## 二、目录结构

```
homework-online/
├── backend/                     # Java 后端（Spring Boot 3，Maven）
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/homework/online/
│       │   ├── config/          # JWT、Spring Security、过滤器
│       │   ├── common/          # 当前用户、统一异常处理
│       │   ├── domain/          # 实体：用户/班级/作业/提交/文件
│       │   ├── repo/            # Spring Data JPA 仓库
│       │   ├── dto/             # 请求/响应对象
│       │   ├── service/         # 业务逻辑 + 演示数据初始化
│       │   └── web/             # REST 控制器
│       └── resources/
│           ├── application.yml        # 默认 H2 配置
│           └── application-mysql.yml  # MySQL 生产配置
├── frontend/                    # Vue 3 前端（Vite）
│   └── src/
│       ├── api/                 # Axios 封装与接口定义
│       ├── components/          # 作业卡片、文件上传/预览、Markdown 渲染…
│       ├── layouts/             # 侧边栏 + 顶栏布局
│       ├── views/               # 登录、工作台、作业列表/详情/录入、批改中心、班级、个人中心
│       ├── router/ store/ utils/
│       └── styles/main.css      # 设计系统
├── mock-server/                 # 演示用 Node Mock API（同契约，数据存 db.json）
├── docker-compose.yml           # MySQL + 后端 + 前端一键部署
├── docs/
│   └── chaoxing-integration-assessment.md   # 接入学习通（超星）可行性评估
└── README.md
```

---

## 三、快速开始

### 方式 1：只想看界面（无需 JDK）

```bash
# 终端 1：启动 Mock 后端（端口 8080，自动写入演示数据）
cd mock-server && npm install && npm start

# 终端 2：启动前端（端口 5173）
cd frontend && npm install && npm run dev
```

打开 http://localhost:5173 ，用下面的演示账号登录。

数据库被重置后，可用脚本一键恢复"陈嘉禾已完成作业"的演示状态：

```bash
cd mock-server
node scripts/complete-homework.mjs                        # 默认学生 chen（陈嘉禾）
node scripts/complete-homework.mjs --student moshen       # 学生 魔神弃子（4 份作业）
node scripts/complete-homework.mjs --student moshen --force --allow-late   # 含逾期作业：老师临时放开补交后提交，再恢复设置
```

### 方式 2：前后端分离真实运行（需要 JDK 17 + Maven 3.9+）

```bash
# 终端 1：Java 后端（端口 8080，H2 文件库自动建表并写入演示数据）
cd backend
mvn spring-boot:run

# 终端 2：前端（/api 请求由 Vite 代理到 8080）
cd frontend
npm install
npm run dev
```

生产打包：

```bash
cd backend && mvn clean package          # 生成 target/homework-online-1.0.0.jar
java -jar target/homework-online-1.0.0.jar

cd frontend && npm run build             # 产物在 dist/，交给 Nginx 托管
```

使用 MySQL：先在数据库中建库 `homework_online`，然后

```bash
java -jar target/homework-online-1.0.0.jar --spring.profiles.active=mysql \
  --DB_HOST=127.0.0.1 --DB_USER=root --DB_PASSWORD=你的密码
```

### 方式 3：Docker Compose 一键部署

```bash
docker compose up -d --build
# 前端 http://localhost:8088 ，后端 http://localhost:8080 ，MySQL 数据卷持久化
```

---

## 四、演示账号

| 角色 | 用户名 | 密码 | 说明 |
| --- | --- | --- | --- |
| 教师 | `zeng` | `123456` | 曾芮 · 语文，含 2 个班级、5 份作业（含 1 份草稿） |
| 教师 | `li` | `123456` | 李文博 · 数学 |
| 学生 | `zhang` | `123456` | 张小明，含已批改 / 待批改 / 待完成三种状态 |
| 学生 | `chen` | `123456` | **陈嘉禾**（学号 20240107），在 3 班与 5 班；3 份作业已由脚本真实提交（含 1 个手写稿附件） |
| 学生 | `moshen` | `123456` | **魔神弃子**（学号 20240108），在 3 班与 5 班；**4 份作业全部已提交**（含逾期补交的《现代文阅读训练（一）》） |
| 学生 | `zhao` / `sun` / `qian` / `zhou` / `wu` | `123456` | 其他学生，用于批改列表演示 |
| 班级邀请码 | `G1-3` / `G1-5` / `M1-3` | — | 学生端可用「加入班级」 |

> 演示数据仅在数据库为空时写入，可通过 `app.seed-demo-data=false` 关闭。

---

## 四之二、演示脚本

`mock-server/scripts/complete-homework.mjs` 用接口（而不是直接改数据库）以学生「陈嘉禾」的身份完成作业，
因此走的正是前端同一条链路：登录 → 上传附件 → 提交 → 出现在教师批改中心。

| 学生 | 作业 | 字数 | 说明 |
| --- | --- | --- | --- |
| 陈嘉禾 `chen` | 《荷塘月色》第 4-6 段赏析 | 679 | 含 1 个手写稿附件（SVG），演示"拍照/文件上传" |
| 陈嘉禾 `chen` | 古诗文默写 · 必修上册第三单元 | 327 | 已逾期但允许补交，标记为"逾期提交" |
| 陈嘉禾 `chen` | 周末随笔：我身边的小人物 | 719 | 纯在线录入的长文本作答 |
| 魔神弃子 `moshen` | 《荷塘月色》第 4-6 段赏析 | 666 | 换角度写"像亭亭的舞女的裙" |
| 魔神弃子 `moshen` | 古诗文默写 · 必修上册第三单元 | 327 | 逾期补交 |
| 魔神弃子 `moshen` | 现代文阅读训练（一） | 902 | 4 道小题全部作答（作业已截止，脚本以"老师临时放开补交"的方式提交） |
| 魔神弃子 `moshen` | 周末随笔：我身边的小人物 | 645 | 早市卖豆腐的女人 |

> 两份答案刻意写成不同内容（除默写原文外），避免演示时看起来像抄作业。

答案库在 `mock-server/scripts/answers.mjs`，按学生组织；新增演示学生只需加一个 profile，脚本自动可用。

## 五、核心业务流程

```
教师创建班级(G1-3) ──> 在线录入作业 ──> 发布(PUBLISHED)
                                    │
学生凭邀请码加入班级 ──> 看到作业 ──> 在线作答 / 上传照片附件 ──> 提交(SUBMITTED)
                                    │
教师批改中心 ──> 打分 + 评语 ──> 已批改(GRADED) ──> 学生端展示得分与评语
                              └─> 退回重做(RETURNED) ──> 学生可重新提交
```

- 截止时间与「允许逾期补交」由作业控制：超期且不允许补交时，后端直接拒绝提交，前端按钮置灰。
- 已批改的提交不允许学生自行覆盖；老师可在批改中心修改分数或改为退回。
- 学生重新提交会覆盖上一次未批改的答案。

---

## 六、接口约定

统一返回 JSON，错误统一为 `{"message": "错误说明"}`；除登录/注册与文件读取外都需要
`Authorization: Bearer <token>`。

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/auth/login` | 登录，返回 `{token, user}` |
| POST | `/api/auth/register` | 学生注册（可带邀请码直接入班） |
| GET | `/api/auth/me` | 当前登录用户 |
| PUT | `/api/auth/profile` | 修改姓名 / 邮箱 |
| GET | `/api/classes` | 我的班级列表（教师=创建，学生=已加入） |
| POST | `/api/classes` | 创建班级（教师） |
| GET | `/api/classes/{id}` | 班级详情（学生名单 + 班级作业） |
| PUT/DELETE | `/api/classes/{id}` | 重命名 / 删除班级（无作业时） |
| POST | `/api/classes/join` | 学生凭邀请码加入 |
| POST | `/api/classes/{id}/students` | 教师添加学生（用户名或学号） |
| DELETE | `/api/classes/{id}/students/{studentId}` | 移出学生 |
| GET | `/api/assignments` | 作业列表，支持 `classId` `status`(ALL/DRAFT/PUBLISHED/PENDING) `keyword` |
| POST | `/api/assignments` | 布置作业（`status=DRAFT` 存草稿） |
| GET | `/api/assignments/{id}` | 作业详情（教师视角附带每位学生提交情况） |
| PUT | `/api/assignments/{id}` | 编辑作业 |
| POST | `/api/assignments/{id}/publish` | 发布草稿 |
| DELETE | `/api/assignments/{id}` | 删除作业及其提交 |
| GET | `/api/assignments/{id}/submissions` | 教师：学生提交情况表 |
| POST | `/api/assignments/{id}/submissions` | 学生：提交 / 重新提交 |
| GET | `/api/submissions/mine` | 学生：我的提交 |
| GET | `/api/submissions` | 教师：待批改/已批改列表，支持 `status` `assignmentId` `classId` `keyword` |
| GET | `/api/submissions/{id}` | 提交详情 |
| POST | `/api/submissions/{id}/grade` | 批改：`{score, comment, status}` |
| DELETE | `/api/submissions/{id}` | 学生撤回未批改的提交 |
| POST | `/api/files` | 上传文件（multipart，字段名 `files`，最多 5 个 / 单个 20MB） |
| GET | `/api/files/{id}` | 预览或下载（`?download=1`） |
| GET | `/api/stats/overview` | 工作台统计（按角色返回不同卡片/待办/动态） |

示例：

```bash
# 登录
curl -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"zeng","password":"123456"}'

# 布置作业
curl -X POST http://localhost:8080/api/assignments \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"title":"《劝学》背诵","content":"## 要求\n背诵全文","classId":1,
       "dueAt":"2026-11-10T21:00:00","totalScore":100,"allowLate":true,"status":"PUBLISHED"}'

# 批改
curl -X POST http://localhost:8080/api/submissions/1/grade \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"score":92,"comment":"完成认真，继续保持","status":"GRADED"}'
```

---

## 七、数据模型

| 表 | 关键字段 |
| --- | --- |
| `users` | id, username(唯一), password(BCrypt), name, role(TEACHER/STUDENT), student_no, email, subject |
| `classes` | id, name, code(邀请码, 唯一), teacher_id, created_at |
| `class_members` | class_id, student_id（唯一约束） |
| `assignments` | id, title, content, class_id, teacher_id, due_at, total_score, allow_late, status, created_at, published_at |
| `assignment_attachments` | assignment_id, file_id |
| `submissions` | id, assignment_id, student_id（唯一），content, submitted_at, status, score, comment, graded_at, graded_by |
| `submission_attachments` | submission_id, file_id |
| `files` | id, name, size, content_type, stored_name, uploader_id, created_at |

---

## 八、安全与生产注意事项

- 登录使用 JWT（HS256，默认 7 天有效），无状态鉴权；令牌存于浏览器 localStorage，并同步写入同源 Cookie
  `hw_token`，请求时**同时**通过 `Authorization: Bearer` / `X-Auth-Token` 头与 Cookie 发送（多通道冗余，
  兼容会丢弃请求头或分区存储的预览/反向代理环境）。服务端按 `Authorization → X-Auth-Token → ?token= → Cookie` 顺序取令牌。
- 前端提供「个人中心 → 连接诊断」（`GET /api/debug/auth`），可直接看到后端是否可达、凭证走了哪条通道、服务端识别到的用户。
- 密码使用 BCrypt 加盐哈希存储，接口不返回密码字段。
- 所有业务接口做角色与数据归属校验（教师只能操作自己的班级/作业/提交，学生只能看自己的提交）。
- **`GET /api/files/**` 未鉴权**（为让浏览器 `<img>` 直接显示作业照片），生产环境建议改为带签名的临时 URL 或网关鉴权。
- 生产务必替换 `app.jwt.secret`（环境变量 `APP_JWT_SECRET`），限制 CORS 域名，改用 MySQL + 对象存储（如 OSS/S3）保存附件。
- H2 控制台仅在开发默认配置下开启（`/h2-console`），生产 profile 已关闭。

---

## 九、常见问题

| 问题 | 处理 |
| --- | --- |
| 前端 401 | 令牌过期或后端未启动，重新登录 |
| 上传失败 | 单文件 ≤ 20MB，最多 5 个；检查 `app.upload-dir` 目录权限 |
| 登录后又提示未登录 | 打开「个人中心 → 连接诊断」：`凭证通道` 显示 `none` 说明代理没有转发请求头/Cookie；`本地令牌` 为「无」说明浏览器禁止了本地存储（此时 Cookie 通道会兜底，重新登录一次即可） |
| 学生看不到作业 | 确认作业已「发布」且学生已加入对应班级 |
| 提交时提示已截止 | 作业不允许补交时会拒绝，教师可编辑作业打开「允许逾期补交」 |
| 端口冲突 | 后端 `--server.port=8081`，前端改 `vite.config.js` 中 `VITE_API_TARGET` |
