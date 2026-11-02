# 作业云 · 后端（Spring Boot 3）

Java 17 + Spring Boot 3 + Spring Security(JWT) + Spring Data JPA，默认 H2 文件库开箱即用，可切换 MySQL。

## 运行

```bash
mvn spring-boot:run                 # http://localhost:8080 ，首次启动自动建表 + 写入演示数据
mvn clean package                   # 打包 target/homework-online-1.0.0.jar
java -jar target/homework-online-1.0.0.jar

# MySQL
java -jar target/homework-online-1.0.0.jar --spring.profiles.active=mysql \
  --DB_HOST=127.0.0.1 --DB_NAME=homework_online --DB_USER=root --DB_PASSWORD=xxx
```

## 结构

| 包 | 说明 |
| --- | --- |
| `config` | `JwtService`（HS256 签发/校验）、`JwtAuthFilter`、`SecurityConfig`（无状态鉴权、CORS） |
| `common` | `AuthUser` / `CurrentUser`（读取当前登录者）、`ApiException`、`GlobalExceptionHandler` |
| `domain` | `User` `SchoolClass` `ClassMember` `Assignment` `Submission` `StoredFile` 与状态枚举 |
| `repo` | Spring Data JPA 仓库 |
| `dto` | 请求/响应对象（与前端字段一一对应） |
| `service` | 认证、班级、作业、提交批改、文件、统计；`DataSeeder` 初始化演示数据 |
| `web` | REST 控制器（`/api/auth` `/api/classes` `/api/assignments` `/api/submissions` `/api/files` `/api/stats`） |

## 配置（`application.yml`）

| 配置项 | 说明 |
| --- | --- |
| `app.upload-dir` | 附件存放目录，默认 `./uploads` |
| `app.jwt.secret` | JWT 密钥，生产请用环境变量 `APP_JWT_SECRET` 覆盖 |
| `app.jwt.ttl-hours` | 令牌有效期（小时），默认 168 |
| `app.cors.allowed-origins` | 允许的前端来源，`*` 表示全部 |
| `app.seed-demo-data` | 数据库为空时是否写入演示数据 |

## 说明

- 密码使用 BCrypt 存储；`SecurityConfig` 中 `/api/auth/login|register` 与 `GET /api/files/**` 放行，其余 `/api/**` 需登录。
- 教师只能操作自己的班级与作业，学生只能看自己的提交，越权返回 403 / 404。
- 批改接口校验分数范围 `0 ~ totalScore`，支持 `GRADED`（完成）与 `RETURNED`（退回重做）。
