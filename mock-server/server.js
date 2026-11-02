/**
 * 家庭作业在线系统 —— 演示用 Mock API
 * -------------------------------------------------------------
 * 与 Spring Boot 后端（backend/）保持完全相同的 REST 契约，
 * 数据保存在 mock-server/db.json，上传文件保存在 mock-server/uploads。
 * 仅用于没有 JDK 环境时的界面预览 / 演示；生产部署请使用 backend/。
 */
const express = require('express');
const cors = require('cors');
const multer = require('multer');
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');

const PORT = process.env.PORT || 8080;
const DATA_DIR = __dirname;
const DB_FILE = path.join(DATA_DIR, 'db.json');
const UPLOAD_DIR = path.join(DATA_DIR, 'uploads');
fs.mkdirSync(UPLOAD_DIR, { recursive: true });

/* ---------------------------- 种子数据 ---------------------------- */
function nowISO(offsetMs = 0) {
  return new Date(Date.now() + offsetMs).toISOString();
}
const DAY = 86400000;

function seed() {
  return {
    seq: 1000,
    users: [
      { id: 1, username: 'zeng', password: '123456', name: '曾芮', role: 'TEACHER', subject: '语文', email: 'zeng@school.edu.cn', studentNo: null },
      { id: 2, username: 'li', password: '123456', name: '李文博', role: 'TEACHER', subject: '数学', email: 'li@school.edu.cn', studentNo: null },
      { id: 11, username: 'zhang', password: '123456', name: '张小明', role: 'STUDENT', studentNo: '20240101', email: 'zhang@stu.edu.cn' },
      { id: 12, username: 'zhao', password: '123456', name: '赵一鸣', role: 'STUDENT', studentNo: '20240102', email: 'zhao@stu.edu.cn' },
      { id: 13, username: 'sun', password: '123456', name: '孙悦', role: 'STUDENT', studentNo: '20240103', email: 'sun@stu.edu.cn' },
      { id: 14, username: 'qian', password: '123456', name: '钱多多', role: 'STUDENT', studentNo: '20240104', email: 'qian@stu.edu.cn' },
      { id: 15, username: 'zhou', password: '123456', name: '周思远', role: 'STUDENT', studentNo: '20240105', email: 'zhou@stu.edu.cn' },
      { id: 16, username: 'wu', password: '123456', name: '吴桐', role: 'STUDENT', studentNo: '20240106', email: 'wu@stu.edu.cn' },
      { id: 17, username: 'chen', password: '123456', name: '陈佳和', role: 'STUDENT', studentNo: '20240107', email: 'chen@stu.edu.cn' }
    ],
    classes: [
      { id: 1, name: '高一（3）班 · 语文', code: 'G1-3', teacherId: 1, createdAt: nowISO(-30 * DAY) },
      { id: 2, name: '高一（5）班 · 语文', code: 'G1-5', teacherId: 1, createdAt: nowISO(-25 * DAY) },
      { id: 3, name: '高一（3）班 · 数学', code: 'M1-3', teacherId: 2, createdAt: nowISO(-20 * DAY) }
    ],
    members: [
      { classId: 1, userId: 11 }, { classId: 1, userId: 12 }, { classId: 1, userId: 13 },
      { classId: 1, userId: 14 }, { classId: 1, userId: 15 }, { classId: 1, userId: 16 }, { classId: 1, userId: 17 },
      { classId: 2, userId: 13 }, { classId: 2, userId: 15 }, { classId: 2, userId: 17 },
      { classId: 3, userId: 11 }, { classId: 3, userId: 14 }
    ],
    files: [],
    assignments: [
      {
        id: 101, title: '《荷塘月色》第 4-6 段赏析', classId: 1, teacherId: 1,
        content:
          '## 作业要求\n\n1. 阅读朱自清《荷塘月色》第 4—6 段，从**通感、比喻、叠词**三个角度各找出一处例句；\n2. 任选其中一处，写一段不少于 300 字的赏析文字；\n3. 可以手写拍照上传，也可以直接在系统中在线录入。\n\n> 注意：请勿直接复制网络赏析，独立完成，标注引用来源。',
        dueAt: nowISO(2 * DAY + 6 * 3600000), totalScore: 100, allowLate: true, status: 'PUBLISHED',
        attachments: [], createdAt: nowISO(-2 * DAY), publishedAt: nowISO(-2 * DAY)
      },
      {
        id: 102, title: '古诗文默写 · 必修上册第三单元', classId: 1, teacherId: 1,
        content:
          '## 默写范围\n\n- 《短歌行》全文\n- 《归园田居（其一）》前八句\n- 《念奴娇·赤壁怀古》下阕\n\n**提交方式**：在系统内在线录入默写内容，错别字每处扣 2 分。',
        dueAt: nowISO(-1 * DAY), totalScore: 50, allowLate: true, status: 'PUBLISHED',
        attachments: [], createdAt: nowISO(-6 * DAY), publishedAt: nowISO(-6 * DAY)
      },
      {
        id: 103, title: '现代文阅读训练（一）', classId: 1, teacherId: 1,
        content:
          '## 材料\n\n阅读下发材料《故乡的榕树》，完成文后 4 道小题。\n\n1. 分析第 3 段景物描写的作用（6 分）\n2. 理解“榕树是故乡的魂”的含义（8 分）\n3. 概括作者情感变化（6 分）\n4. 结合自身经历写一段 200 字感悟（10 分）',
        dueAt: nowISO(-4 * DAY), totalScore: 30, allowLate: false, status: 'PUBLISHED',
        attachments: [], createdAt: nowISO(-10 * DAY), publishedAt: nowISO(-10 * DAY)
      },
      {
        id: 104, title: '周末随笔：我身边的小人物', classId: 2, teacherId: 1,
        content: '## 写作任务\n\n观察身边一位普通劳动者，用细节描写刻画人物，不少于 600 字。\n\n要求：不出现真实姓名，可用化名；结尾有一段抒情性议论。',
        dueAt: nowISO(4 * DAY), totalScore: 100, allowLate: false, status: 'PUBLISHED',
        attachments: [], createdAt: nowISO(-1 * DAY), publishedAt: nowISO(-1 * DAY)
      },
      {
        id: 106, title: '魔神弃子', classId: 1, teacherId: 1,
        content:
          '## 写作任务\n\n' +
          '以《魔神弃子》为题，写一篇不少于 800 字的记叙性散文或小说节选。\n\n' +
          '## 具体要求\n\n' +
          '1. **标题即主题**：“弃”字要落到实处——被谁弃、为何弃、弃后如何；\n' +
          '2. 至少有一处**环境描写与心理变化互相映照**，不要写与情绪无关的风景；\n' +
          '3. 允许虚构，但细节必须真实可感：气味、声音、触感、光线；\n' +
          '4. 结尾不落俗套，慎用“从此过上幸福生活”一类的收束。\n\n' +
          '> 提示：这个题目可以写成山野传说中的弃儿，也可以写成被时代、被家庭“放下”的人。想清楚你要写的“魔神”究竟是谁。',
        dueAt: nowISO(3 * DAY), totalScore: 100, allowLate: true, status: 'PUBLISHED',
        attachments: [], createdAt: nowISO(-4 * 3600000), publishedAt: nowISO(-4 * 3600000)
      },
      {
        id: 105, title: '《劝学》论证结构梳理', classId: 1, teacherId: 1,
        content: '（草稿）用思维导图梳理《劝学》的论证层次，下周三前上传图片。',
        dueAt: nowISO(8 * DAY), totalScore: 100, allowLate: false, status: 'DRAFT',
        attachments: [], createdAt: nowISO(-3 * 3600000), publishedAt: null
      }
    ],
    submissions: [
      {
        id: 201, assignmentId: 102, studentId: 11,
        content:
          '对酒当歌，人生几何！譬如朝露，去日苦多。\n慨当以慷，忧思难忘。何以解忧？唯有杜康。\n青青子衿，悠悠我心。但为君故，沉吟至今。\n……\n少无适俗韵，性本爱丘山。误落尘网中，一去三十年。',
        attachments: [], submittedAt: nowISO(-1.4 * DAY), status: 'GRADED', score: 44,
        comment: '默写整体准确，第 4 句“慨当以慷”后漏写“忧思难忘”，扣 2 分；结尾两句顺序有误，扣 4 分。订正后交组长检查。',
        gradedAt: nowISO(-1 * DAY), gradedBy: 1
      },
      {
        id: 202, assignmentId: 102, studentId: 12,
        content: '对酒当歌，人生几何！譬如朝露，去日苦多。……\n山不厌高，海不厌深。周公吐哺，天下归心。',
        attachments: [], submittedAt: nowISO(-1.2 * DAY), status: 'GRADED', score: 48,
        comment: '字迹材料完整，仅一处标点缺失。继续保持！',
        gradedAt: nowISO(-0.9 * DAY), gradedBy: 1
      },
      {
        id: 203, assignmentId: 102, studentId: 13,
        content: '（在线录入内容略）',
        attachments: [], submittedAt: nowISO(-1.5 * DAY), status: 'SUBMITTED', score: null, comment: null,
        gradedAt: null, gradedBy: null
      },
      {
        id: 204, assignmentId: 102, studentId: 14,
        content: '对酒当歌，人生几何！譬如朝露，去日苦多。慨当以慷，忧思难忘。',
        attachments: [], submittedAt: nowISO(-1.1 * DAY), status: 'SUBMITTED', score: null, comment: null,
        gradedAt: null, gradedBy: null
      },
      {
        id: 205, assignmentId: 101, studentId: 15,
        content: '赏析角度：通感。\n\n“微风过处，送来缕缕清香，仿佛远处高楼上渺茫的歌声似的。”作者用听觉上的歌声来写嗅觉上的清香，两种感官彼此打通……（后文略，共 420 字）',
        attachments: [], submittedAt: nowISO(-0.5 * DAY), status: 'SUBMITTED', score: null, comment: null,
        gradedAt: null, gradedBy: null
      },
      {
        id: 206, assignmentId: 103, studentId: 16,
        content: '一、景物描写作用：渲染了……（略）\n二、“榕树是故乡的魂”……（略）',
        attachments: [], submittedAt: nowISO(-3.5 * DAY), status: 'GRADED', score: 26,
        comment: '第 3 题概括不够准确，注意分点作答。',
        gradedAt: nowISO(-3 * DAY), gradedBy: 1
      },
      {
        id: 207, assignmentId: 103, studentId: 11,
        content: '一、景物描写作用：交代时间地点，营造宁静氛围，为下文抒情做铺垫。\n二、榕树见证了几代人的生活，是乡愁的寄托，所以说它是故乡的魂。\n三、由思念到感伤，再到释然。\n四、（略）',
        attachments: [], submittedAt: nowISO(-3.2 * DAY), status: 'GRADED', score: 28,
        comment: '要点齐全，第 4 题感悟部分可以更具体。',
        gradedAt: nowISO(-2.8 * DAY), gradedBy: 1
      }
    ]
  };
}

/* ---------------------------- 持久化 ---------------------------- */
function loadDB() {
  if (fs.existsSync(DB_FILE)) {
    try {
      return JSON.parse(fs.readFileSync(DB_FILE, 'utf8'));
    } catch (e) {
      console.warn('db.json 解析失败，重新生成种子数据', e.message);
    }
  }
  const db = seed();
  fs.writeFileSync(DB_FILE, JSON.stringify(db, null, 2));
  return db;
}

// 首屏用的示例附件（手写稿示意图），以 SVG 形式保存在 uploads 目录
function ensureSampleAttachment(db) {
  if (db.files.some((f) => f.name === '古诗文默写-手写稿.svg')) return;
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="720" height="960" viewBox="0 0 720 960">
  <rect width="720" height="960" fill="#fdfcf8"/>
  <g stroke="#dfe3ea" stroke-width="1">
    ${Array.from({ length: 18 }, (_, i) => `<line x1="60" y1="${120 + i * 42}" x2="660" y2="${120 + i * 42}"/>`).join('')}
    <line x1="60" y1="60" x2="60" y2="900"/><line x1="660" y1="60" x2="660" y2="900"/>
  </g>
  <text x="360" y="90" text-anchor="middle" font-family="serif" font-size="24" fill="#334">古诗文默写 · 学生手写稿</text>
  <g font-family="KaiTi, STKaiti, serif" font-size="30" fill="#26344a">
    <text x="90" y="160">对酒当歌，人生几何！</text>
    <text x="90" y="202">譬如朝露，去日苦多。</text>
    <text x="90" y="244">慨当以慷，忧思难忘。</text>
    <text x="90" y="286">何以解忧？唯有杜康。</text>
    <text x="90" y="328">青青子衿，悠悠我心。</text>
    <text x="90" y="370">但为君故，沉吟至今。</text>
    <text x="90" y="412">呦呦鹿鸣，食野之苹。</text>
    <text x="90" y="454">我有嘉宾，鼓瑟吹笙。</text>
  </g>
  <g stroke="#c0392b" stroke-width="3" fill="none" stroke-linecap="round">
    <path d="M120 224 q18 -14 34 0"/><path d="M250 306 l40 22"/>
  </g>
  <text x="470" y="470" font-family="KaiTi, serif" font-size="22" fill="#c0392b">此处漏字</text>
  <text x="360" y="930" text-anchor="middle" font-family="sans-serif" font-size="16" fill="#8a94a6">示例附件 · 仅用于界面演示</text>
</svg>`;
  const fileName = 'seed-' + crypto.randomUUID() + '.svg';
  fs.writeFileSync(path.join(UPLOAD_DIR, fileName), svg);
  const file = {
    id: ++db.seq,
    name: '古诗文默写-手写稿.svg',
    size: Buffer.byteLength(svg),
    contentType: 'image/svg+xml',
    storedName: fileName,
    createdAt: nowISO(-1.3 * DAY),
    uploaderId: 13
  };
  db.files.push(file);
  const s = db.submissions.find((x) => x.id === 203);
  if (s) s.attachments = [file.id];
}

let db = loadDB();
ensureSampleAttachment(db);
const save = () => fs.writeFileSync(DB_FILE, JSON.stringify(db, null, 2));

/* ---------------------------- 工具 ---------------------------- */
const nextId = () => ++db.seq;
const pad = (n) => String(n).padStart(2, '0');
function genClassCode() {
  const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
  let s = '';
  for (let i = 0; i < 6; i++) s += chars[Math.floor(Math.random() * chars.length)];
  return s;
}
const userById = (id) => db.users.find((u) => u.id === Number(id));
const publicUser = (u) =>
  u && { id: u.id, username: u.username, name: u.name, role: u.role, studentNo: u.studentNo, email: u.email, subject: u.subject || null };
const classMembers = (classId) => db.members.filter((m) => m.classId === Number(classId));
const classById = (id) => db.classes.find((c) => c.id === Number(id));
const assignmentById = (id) => db.assignments.find((a) => a.id === Number(id));
const submissionById = (id) => db.submissions.find((s) => s.id === Number(id));
const fileById = (id) => db.files.find((f) => f.id === Number(id));
const fileDto = (f) => (f ? { id: f.id, name: f.name, size: f.size, contentType: f.contentType, url: `/api/files/${f.id}` } : null);
const filesDto = (ids) => (ids || []).map((id) => fileDto(fileById(id))).filter(Boolean);

function classDto(c) {
  return {
    id: c.id,
    name: c.name,
    code: c.code,
    teacherId: c.teacherId,
    teacherName: (userById(c.teacherId) || {}).name || '',
    studentCount: classMembers(c.id).length,
    createdAt: c.createdAt
  };
}

function submissionsOf(assignmentId) {
  return db.submissions.filter((s) => s.assignmentId === Number(assignmentId));
}

function submissionDto(s, viewer) {
  if (!s) return null;
  const stu = userById(s.studentId) || {};
  const a = assignmentById(s.assignmentId) || {};
  return {
    id: s.id,
    assignmentId: s.assignmentId,
    assignmentTitle: a.title,
    totalScore: a.totalScore,
    dueAt: a.dueAt,
    className: a.classId ? (classById(a.classId) || {}).name : null,
    studentId: s.studentId,
    studentName: stu.name || '',
    studentNo: stu.studentNo || '',
    content: s.content || '',
    attachments: filesDto(s.attachments),
    submittedAt: s.submittedAt,
    status: s.status,
    score: s.score,
    comment: s.comment,
    gradedAt: s.gradedAt,
    late: a.dueAt ? new Date(s.submittedAt) > new Date(a.dueAt) : false
  };
}

function assignmentDto(a, viewer) {
  const list = submissionsOf(a.id);
  const members = classMembers(a.classId);
  const my = viewer && viewer.role === 'STUDENT' ? list.find((s) => s.studentId === viewer.id) : null;
  return {
    id: a.id,
    title: a.title,
    content: a.content,
    classId: a.classId,
    className: (classById(a.classId) || {}).name || '',
    teacherId: a.teacherId,
    teacherName: (userById(a.teacherId) || {}).name || '',
    dueAt: a.dueAt,
    totalScore: a.totalScore,
    allowLate: a.allowLate,
    status: a.status,
    attachments: filesDto(a.attachments),
    createdAt: a.createdAt,
    publishedAt: a.publishedAt,
    studentCount: members.length,
    submittedCount: list.length,
    gradedCount: list.filter((s) => s.status === 'GRADED').length,
    overdue: a.dueAt ? new Date(a.dueAt) < new Date() : false,
    mySubmission: my ? submissionDto(my, viewer) : null
  };
}

/* ---------------------------- 应用 ---------------------------- */
const app = express();
app.use(cors());
app.use(express.json({ limit: '5mb' }));

// 每个 /api 请求记录一行日志：方法、路径、凭证来源、状态码，便于排查代理链路问题
app.use('/api', (req, res, next) => {
  const start = Date.now();
  res.on('finish', () => {
    const { source } = resolveToken(req);
    const who = req.user ? req.user.username : '-';
    console.log(
      `[api] ${new Date().toISOString()} ${req.method} ${req.originalUrl} -> ${res.statusCode} ` +
        `auth=${source || 'none'} user=${who} ${Date.now() - start}ms`
    );
  });
  next();
});

const upload = multer({
  storage: multer.diskStorage({
    destination: (req, file, cb) => cb(null, UPLOAD_DIR),
    filename: (req, file, cb) => {
      const ext = path.extname(file.originalname || '').slice(0, 10);
      cb(null, crypto.randomUUID() + ext);
    }
  }),
  limits: { fileSize: 20 * 1024 * 1024, files: 5 }
});

function signToken(user) {
  return Buffer.from(JSON.stringify({ uid: user.id, exp: Date.now() + 7 * DAY })).toString('base64url');
}
/** 从 Cookie 头中解析指定名称的值 */
function readCookie(req, name) {
  const raw = req.headers.cookie || '';
  for (const part of raw.split(';')) {
    const index = part.indexOf('=');
    if (index < 0) continue;
    if (part.slice(0, index).trim() === name) {
      try {
        return decodeURIComponent(part.slice(index + 1).trim());
      } catch {
        return part.slice(index + 1).trim();
      }
    }
  }
  return '';
}

/**
 * 令牌来源按优先级：Authorization: Bearer → X-Auth-Token → query.token → Cookie。
 * 多通道是为了兼容预览代理 / 反向代理可能丢弃某个请求头的场景。
 */
function resolveToken(req) {
  const header = req.headers.authorization || '';
  const bearer = /^bearer\s+(.+)$/i.exec(header.trim());
  if (bearer) return { token: bearer[1].trim(), source: 'authorization-header' };
  if (req.headers['x-auth-token']) return { token: String(req.headers['x-auth-token']).trim(), source: 'x-auth-token-header' };
  if (req.query && req.query.token) return { token: String(req.query.token).trim(), source: 'query' };
  const cookie = readCookie(req, 'hw_token');
  if (cookie) return { token: cookie, source: 'cookie' };
  return { token: null, source: null };
}

function auth(req, res, next) {
  const { token, source } = resolveToken(req);
  req.authSource = source;
  if (!token) {
    return res.status(401).json({ message: '未登录或登录已过期（请求未携带登录凭证）' });
  }
  try {
    const payload = JSON.parse(Buffer.from(token, 'base64url').toString('utf8'));
    if (!payload.exp || payload.exp < Date.now()) {
      return res.status(401).json({ message: '登录已过期，请重新登录' });
    }
    const user = userById(payload.uid);
    if (!user) return res.status(401).json({ message: '用户不存在' });
    req.user = user;
    next();
  } catch {
    return res.status(401).json({ message: '登录凭证无效' });
  }
}
const teacherOnly = (req, res, next) => (req.user.role === 'TEACHER' ? next() : res.status(403).json({ message: '仅教师可执行该操作' }));

function canAccessClass(user, classId) {
  const c = classById(classId);
  if (!c) return false;
  if (user.role === 'TEACHER') return c.teacherId === user.id;
  return db.members.some((m) => m.classId === c.id && m.userId === user.id);
}
function canViewAssignment(user, a) {
  if (!a) return false;
  if (user.role === 'TEACHER') return a.teacherId === user.id;
  return a.status === 'PUBLISHED' && db.members.some((m) => m.classId === a.classId && m.userId === user.id);
}

/* ---------------- 认证 ---------------- */
app.post('/api/auth/login', (req, res) => {
  const { username, password } = req.body || {};
  const user = db.users.find((u) => u.username === String(username || '').trim());
  if (!user || user.password !== password) return res.status(400).json({ message: '用户名或密码不正确' });
  res.json({ token: signToken(user), user: publicUser(user) });
});

app.post('/api/auth/register', (req, res) => {
  const { username, password, name, classCode, studentNo } = req.body || {};
  if (!username || !password || !name) return res.status(400).json({ message: '用户名、密码和姓名不能为空' });
  if (String(password).length < 6) return res.status(400).json({ message: '密码至少 6 位' });
  if (db.users.some((u) => u.username === String(username).trim())) return res.status(400).json({ message: '该用户名已被注册' });
  const cls = classCode ? db.classes.find((c) => c.code.toUpperCase() === String(classCode).trim().toUpperCase()) : null;
  if (classCode && !cls) return res.status(400).json({ message: '班级邀请码不存在' });
  const user = {
    id: nextId(), username: String(username).trim(), password: String(password), name: String(name).trim(),
    role: 'STUDENT', studentNo: studentNo || null, email: null
  };
  db.users.push(user);
  if (cls) db.members.push({ classId: cls.id, userId: user.id });
  save();
  res.json({ token: signToken(user), user: publicUser(user) });
});

app.get('/api/auth/me', auth, (req, res) => res.json(publicUser(req.user)));

/** 连接诊断：用于定位预览环境里“登录后仍提示未登录”的问题 */
app.get('/api/debug/auth', (req, res) => {
  const { token, source } = resolveToken(req);
  const result = {
    ok: false,
    tokenSource: source || 'none',
    hasAuthorizationHeader: !!req.headers.authorization,
    hasXAuthTokenHeader: !!req.headers['x-auth-token'],
    hasCookie: !!readCookie(req, 'hw_token'),
    tokenLength: token ? token.length : 0,
    user: null,
    reason: null
  };
  if (!token) {
    result.reason = '服务端没有收到任何登录凭证（请求头与 Cookie 都没有令牌）';
    return res.json(result);
  }
  try {
    const payload = JSON.parse(Buffer.from(token, 'base64url').toString('utf8'));
    if (!payload.exp || payload.exp < Date.now()) {
      result.reason = '令牌已过期';
      return res.json(result);
    }
    const user = userById(payload.uid);
    if (!user) {
      result.reason = '令牌对应的用户不存在（可能演示数据被重建）';
      return res.json(result);
    }
    result.ok = true;
    result.user = publicUser(user);
    res.json(result);
  } catch {
    result.reason = '令牌格式无效';
    res.json(result);
  }
});

app.put('/api/auth/profile', auth, (req, res) => {
  const { name, email } = req.body || {};
  if (name) req.user.name = String(name).trim();
  if (email !== undefined) req.user.email = email;
  save();
  res.json(publicUser(req.user));
});

/* ---------------- 班级 ---------------- */
app.get('/api/classes', auth, (req, res) => {
  const list = req.user.role === 'TEACHER'
    ? db.classes.filter((c) => c.teacherId === req.user.id)
    : db.classes.filter((c) => db.members.some((m) => m.classId === c.id && m.userId === req.user.id));
  res.json(list.map(classDto));
});

app.post('/api/classes', auth, teacherOnly, (req, res) => {
  const { name, code } = req.body || {};
  if (!name) return res.status(400).json({ message: '班级名称不能为空' });
  let finalCode = (code || genClassCode()).toUpperCase();
  while (db.classes.some((c) => c.code === finalCode)) finalCode = genClassCode();
  const c = { id: nextId(), name: String(name).trim(), code: finalCode, teacherId: req.user.id, createdAt: nowISO() };
  db.classes.push(c);
  save();
  res.json(classDto(c));
});

app.get('/api/classes/:id', auth, (req, res) => {
  const c = classById(req.params.id);
  if (!c) return res.status(404).json({ message: '班级不存在' });
  if (!canAccessClass(req.user, c.id)) return res.status(403).json({ message: '无权查看该班级' });
  const students = classMembers(c.id).map((m) => publicUser(userById(m.userId))).filter(Boolean);
  const assignments = db.assignments
    .filter((a) => a.classId === c.id && (req.user.role === 'TEACHER' || a.status === 'PUBLISHED'))
    .map((a) => assignmentDto(a, req.user));
  res.json({ ...classDto(c), students, assignments });
});

app.put('/api/classes/:id', auth, teacherOnly, (req, res) => {
  const c = classById(req.params.id);
  if (!c || c.teacherId !== req.user.id) return res.status(404).json({ message: '班级不存在' });
  if (req.body.name) c.name = String(req.body.name).trim();
  save();
  res.json(classDto(c));
});

app.delete('/api/classes/:id', auth, teacherOnly, (req, res) => {
  const c = classById(req.params.id);
  if (!c || c.teacherId !== req.user.id) return res.status(404).json({ message: '班级不存在' });
  if (db.assignments.some((a) => a.classId === c.id)) return res.status(400).json({ message: '该班级下还有作业，无法删除' });
  db.classes = db.classes.filter((x) => x.id !== c.id);
  db.members = db.members.filter((m) => m.classId !== c.id);
  save();
  res.json({ ok: true });
});

app.post('/api/classes/join', auth, (req, res) => {
  const code = String((req.body || {}).code || '').trim().toUpperCase();
  const c = db.classes.find((x) => x.code.toUpperCase() === code);
  if (!c) return res.status(400).json({ message: '邀请码不存在' });
  if (db.members.some((m) => m.classId === c.id && m.userId === req.user.id)) return res.status(400).json({ message: '你已在该班级中' });
  db.members.push({ classId: c.id, userId: req.user.id });
  save();
  res.json(classDto(c));
});

app.post('/api/classes/:id/students', auth, teacherOnly, (req, res) => {
  const c = classById(req.params.id);
  if (!c || c.teacherId !== req.user.id) return res.status(404).json({ message: '班级不存在' });
  const key = String((req.body || {}).username || '').trim();
  const stu = db.users.find((u) => u.role === 'STUDENT' && (u.username === key || u.studentNo === key));
  if (!stu) return res.status(400).json({ message: '未找到该学生账号或学号' });
  if (db.members.some((m) => m.classId === c.id && m.userId === stu.id)) return res.status(400).json({ message: '该学生已在班级中' });
  db.members.push({ classId: c.id, userId: stu.id });
  save();
  res.json(classDto(c));
});

app.delete('/api/classes/:id/students/:studentId', auth, teacherOnly, (req, res) => {
  const c = classById(req.params.id);
  if (!c || c.teacherId !== req.user.id) return res.status(404).json({ message: '班级不存在' });
  db.members = db.members.filter((m) => !(m.classId === c.id && m.userId === Number(req.params.studentId)));
  save();
  res.json({ ok: true });
});

/* ---------------- 作业 ---------------- */
app.get('/api/assignments', auth, (req, res) => {
  const { classId, status, keyword } = req.query;
  let list = db.assignments.filter((a) => canViewAssignment(req.user, a));
  if (classId) list = list.filter((a) => a.classId === Number(classId));
  if (status && status !== 'ALL') {
    if (status === 'PENDING') {
      list = list.filter((a) => a.status === 'PUBLISHED' && !submissionsOf(a.id).some((s) => s.studentId === req.user.id));
    } else {
      list = list.filter((a) => a.status === status);
    }
  }
  if (keyword) {
    const k = String(keyword).trim();
    list = list.filter((a) => a.title.includes(k) || (a.content || '').includes(k));
  }
  list.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
  res.json(list.map((a) => assignmentDto(a, req.user)));
});

app.post('/api/assignments', auth, teacherOnly, (req, res) => {
  const b = req.body || {};
  if (!b.title || !b.classId) return res.status(400).json({ message: '作业标题和班级不能为空' });
  const c = classById(b.classId);
  if (!c || c.teacherId !== req.user.id) return res.status(400).json({ message: '班级不存在或无权布置作业' });
  if (!b.content) return res.status(400).json({ message: '作业内容不能为空' });
  const now = nowISO();
  const a = {
    id: nextId(), title: String(b.title).trim(), classId: c.id, teacherId: req.user.id,
    content: b.content, dueAt: b.dueAt || null, totalScore: Number(b.totalScore) || 100,
    allowLate: b.allowLate !== false, status: b.status === 'PUBLISHED' ? 'PUBLISHED' : 'DRAFT',
    attachments: Array.isArray(b.attachments) ? b.attachments.map(Number) : [],
    createdAt: now, publishedAt: b.status === 'PUBLISHED' ? now : null
  };
  db.assignments.push(a);
  save();
  res.json(assignmentDto(a, req.user));
});

app.get('/api/assignments/:id', auth, (req, res) => {
  const a = assignmentById(req.params.id);
  if (!canViewAssignment(req.user, a)) return res.status(404).json({ message: '作业不存在或无权查看' });
  const dto = assignmentDto(a, req.user);
  if (req.user.role === 'TEACHER') {
    dto.submissions = classMembers(a.classId).map((m) => {
      const stu = userById(m.userId);
      const sub = submissionsOf(a.id).find((s) => s.studentId === m.userId);
      return {
        studentId: m.userId, studentName: stu ? stu.name : '', studentNo: stu ? stu.studentNo : '',
        submission: sub ? submissionDto(sub, req.user) : null
      };
    });
  }
  res.json(dto);
});

app.put('/api/assignments/:id', auth, teacherOnly, (req, res) => {
  const a = assignmentById(req.params.id);
  if (!a || a.teacherId !== req.user.id) return res.status(404).json({ message: '作业不存在' });
  const b = req.body || {};
  if (b.title !== undefined) a.title = String(b.title).trim();
  if (b.content !== undefined) a.content = b.content;
  if (b.dueAt !== undefined) a.dueAt = b.dueAt || null;
  if (b.totalScore !== undefined) a.totalScore = Number(b.totalScore) || 100;
  if (b.allowLate !== undefined) a.allowLate = !!b.allowLate;
  if (b.classId !== undefined) a.classId = Number(b.classId);
  if (Array.isArray(b.attachments)) a.attachments = b.attachments.map(Number);
  if (b.status === 'PUBLISHED' && a.status !== 'PUBLISHED') { a.status = 'PUBLISHED'; a.publishedAt = nowISO(); }
  save();
  res.json(assignmentDto(a, req.user));
});

app.post('/api/assignments/:id/publish', auth, teacherOnly, (req, res) => {
  const a = assignmentById(req.params.id);
  if (!a || a.teacherId !== req.user.id) return res.status(404).json({ message: '作业不存在' });
  a.status = 'PUBLISHED';
  a.publishedAt = a.publishedAt || nowISO();
  save();
  res.json(assignmentDto(a, req.user));
});

app.delete('/api/assignments/:id', auth, teacherOnly, (req, res) => {
  const a = assignmentById(req.params.id);
  if (!a || a.teacherId !== req.user.id) return res.status(404).json({ message: '作业不存在' });
  db.assignments = db.assignments.filter((x) => x.id !== a.id);
  db.submissions = db.submissions.filter((s) => s.assignmentId !== a.id);
  save();
  res.json({ ok: true });
});

/* ---------------- 提交与批改 ---------------- */
app.get('/api/assignments/:id/submissions', auth, teacherOnly, (req, res) => {
  const a = assignmentById(req.params.id);
  if (!a || a.teacherId !== req.user.id) return res.status(404).json({ message: '作业不存在' });
  const rows = classMembers(a.classId).map((m) => {
    const stu = userById(m.userId);
    const sub = submissionsOf(a.id).find((s) => s.studentId === m.userId);
    return {
      studentId: m.userId, studentName: stu ? stu.name : '', studentNo: stu ? stu.studentNo : '',
      submission: sub ? submissionDto(sub, req.user) : null
    };
  });
  res.json(rows);
});

app.post('/api/assignments/:id/submissions', auth, (req, res) => {
  if (req.user.role !== 'STUDENT') return res.status(403).json({ message: '仅学生可提交作业' });
  const a = assignmentById(req.params.id);
  if (!a) return res.status(404).json({ message: '作业不存在' });
  if (a.status !== 'PUBLISHED') return res.status(400).json({ message: '该作业尚未发布' });
  if (!db.members.some((m) => m.classId === a.classId && m.userId === req.user.id)) return res.status(403).json({ message: '你不在此班级中' });
  const content = (req.body.content || '').trim();
  const attachments = Array.isArray(req.body.attachments) ? req.body.attachments.map(Number) : [];
  if (!content && attachments.length === 0) return res.status(400).json({ message: '请先填写作业内容或上传附件' });
  const late = a.dueAt ? new Date() > new Date(a.dueAt) : false;
  if (late && !a.allowLate) return res.status(400).json({ message: '已超过截止时间，该作业不允许补交' });
  const now = nowISO();
  let sub = submissionsOf(a.id).find((s) => s.studentId === req.user.id);
  if (sub) {
    if (sub.status === 'GRADED') return res.status(400).json({ message: '该作业已批改，如需重交请联系老师' });
    sub.content = content;
    sub.attachments = attachments;
    sub.submittedAt = now;
    sub.status = 'SUBMITTED';
  } else {
    sub = {
      id: nextId(), assignmentId: a.id, studentId: req.user.id, content, attachments,
      submittedAt: now, status: 'SUBMITTED', score: null, comment: null, gradedAt: null, gradedBy: null
    };
    db.submissions.push(sub);
  }
  save();
  res.json(submissionDto(sub, req.user));
});

app.get('/api/submissions/mine', auth, (req, res) => {
  if (req.user.role !== 'STUDENT') return res.status(403).json({ message: '仅学生可查看' });
  const list = db.submissions.filter((s) => s.studentId === req.user.id)
    .sort((a, b) => new Date(b.submittedAt) - new Date(a.submittedAt));
  res.json(list.map((s) => submissionDto(s, req.user)));
});

app.get('/api/submissions', auth, teacherOnly, (req, res) => {
  const { status, assignmentId, classId, keyword } = req.query;
  let list = db.submissions.filter((s) => {
    const a = assignmentById(s.assignmentId);
    return a && a.teacherId === req.user.id;
  });
  if (assignmentId) list = list.filter((s) => s.assignmentId === Number(assignmentId));
  if (classId) list = list.filter((s) => (assignmentById(s.assignmentId) || {}).classId === Number(classId));
  if (status && status !== 'ALL') list = list.filter((s) => s.status === status);
  if (keyword) {
    const k = String(keyword).trim();
    list = list.filter((s) => {
      const stu = userById(s.studentId) || {};
      const a = assignmentById(s.assignmentId) || {};
      return (stu.name || '').includes(k) || (stu.studentNo || '').includes(k) || (a.title || '').includes(k);
    });
  }
  list.sort((a, b) => new Date(b.submittedAt) - new Date(a.submittedAt));
  res.json(list.map((s) => submissionDto(s, req.user)));
});

app.get('/api/submissions/:id', auth, (req, res) => {
  const s = submissionById(req.params.id);
  if (!s) return res.status(404).json({ message: '提交记录不存在' });
  const a = assignmentById(s.assignmentId);
  if (req.user.role === 'TEACHER' ? !a || a.teacherId !== req.user.id : s.studentId !== req.user.id) {
    return res.status(403).json({ message: '无权查看该提交' });
  }
  res.json({ ...submissionDto(s, req.user), assignment: assignmentDto(a, req.user) });
});

app.post('/api/submissions/:id/grade', auth, teacherOnly, (req, res) => {
  const s = submissionById(req.params.id);
  if (!s) return res.status(404).json({ message: '提交记录不存在' });
  const a = assignmentById(s.assignmentId);
  if (!a || a.teacherId !== req.user.id) return res.status(403).json({ message: '无权批改该作业' });
  const { score, comment, status } = req.body || {};
  if (score === undefined || score === null || score === '') return res.status(400).json({ message: '请填写分数' });
  const n = Number(score);
  if (Number.isNaN(n) || n < 0 || n > a.totalScore) return res.status(400).json({ message: `分数需在 0 ~ ${a.totalScore} 之间` });
  s.score = n;
  s.comment = comment || '';
  s.status = status === 'RETURNED' ? 'RETURNED' : 'GRADED';
  s.gradedAt = nowISO();
  s.gradedBy = req.user.id;
  save();
  res.json(submissionDto(s, req.user));
});

app.delete('/api/submissions/:id', auth, (req, res) => {
  const s = submissionById(req.params.id);
  if (!s) return res.status(404).json({ message: '提交记录不存在' });
  if (req.user.role === 'STUDENT' && s.studentId === req.user.id && s.status !== 'GRADED') {
    db.submissions = db.submissions.filter((x) => x.id !== s.id);
    save();
    return res.json({ ok: true });
  }
  res.status(403).json({ message: '无权撤回该提交' });
});

/* ---------------- 文件 ---------------- */
app.post('/api/files', auth, upload.array('files', 5), (req, res) => {
  const files = req.files || [];
  if (!files.length) return res.status(400).json({ message: '未收到文件' });
  const created = files.map((f) => {
    const rec = {
      id: nextId(), name: Buffer.from(f.originalname, 'latin1').toString('utf8'),
      size: f.size, contentType: f.mimetype, storedName: f.filename, createdAt: nowISO(), uploaderId: req.user.id
    };
    db.files.push(rec);
    return rec;
  });
  save();
  res.json(created.map(fileDto));
});

app.get('/api/files/:id', (req, res) => {
  const f = fileById(req.params.id);
  if (!f) return res.status(404).json({ message: '文件不存在' });
  const p = path.join(UPLOAD_DIR, f.storedName);
  if (!fs.existsSync(p)) return res.status(404).json({ message: '文件已丢失' });
  const inline = req.query.download !== '1';
  res.setHeader('Content-Type', f.contentType || 'application/octet-stream');
  res.setHeader('Content-Disposition', `${inline ? 'inline' : 'attachment'}; filename*=UTF-8''${encodeURIComponent(f.name)}`);
  fs.createReadStream(p).pipe(res);
});

app.delete('/api/files/:id', auth, (req, res) => {
  const f = fileById(req.params.id);
  if (!f) return res.status(404).json({ message: '文件不存在' });
  if (f.uploaderId && f.uploaderId !== req.user.id) return res.status(403).json({ message: '无权删除该文件' });
  db.files = db.files.filter((x) => x.id !== f.id);
  try { fs.unlinkSync(path.join(UPLOAD_DIR, f.storedName)); } catch {}
  save();
  res.json({ ok: true });
});

/* ---------------- 统计 ---------------- */
app.get('/api/stats/overview', auth, (req, res) => {
  const user = req.user;
  if (user.role === 'TEACHER') {
    const classes = db.classes.filter((c) => c.teacherId === user.id);
    const classIds = classes.map((c) => c.id);
    const students = new Set(db.members.filter((m) => classIds.includes(m.classId)).map((m) => m.userId));
    const assignments = db.assignments.filter((a) => a.teacherId === user.id);
    const pending = db.submissions.filter((s) => s.status === 'SUBMITTED' && (assignmentById(s.assignmentId) || {}).teacherId === user.id);
    const graded = db.submissions.filter((s) => s.status === 'GRADED' && (assignmentById(s.assignmentId) || {}).teacherId === user.id);
    const avg = graded.length ? (graded.reduce((t, s) => t + (s.score || 0), 0) / graded.length) : 0;
    return res.json({
      role: 'TEACHER',
      cards: [
        { label: '我的班级', value: classes.length, unit: '个', icon: 'class' },
        { label: '学生人数', value: students.size, unit: '人', icon: 'student' },
        { label: '待批改作业', value: pending.length, unit: '份', icon: 'pen', highlight: pending.length > 0 },
        { label: '平均得分', value: avg ? avg.toFixed(1) : '—', unit: graded.length ? '分' : '', icon: 'chart' }
      ],
      todo: pending
        .sort((a, b) => new Date(a.submittedAt) - new Date(b.submittedAt))
        .slice(0, 6)
        .map((s) => {
          const stu = userById(s.studentId) || {};
          const a = assignmentById(s.assignmentId) || {};
          return {
            kind: 'SUBMISSION', id: s.id, title: a.title || '', subtitle: `${stu.name || ''} · ${stu.studentNo || ''}`,
            meta: '提交于 ' + new Date(s.submittedAt).toLocaleString('zh-CN', { hour12: false }),
            submittedAt: s.submittedAt, late: a.dueAt ? new Date(s.submittedAt) > new Date(a.dueAt) : false
          };
        }),
      recent: assignments
        .slice().sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt)).slice(0, 5)
        .map((a) => {
          const list = submissionsOf(a.id);
          return {
            kind: 'ASSIGNMENT', id: a.id, title: a.title, subtitle: (classById(a.classId) || {}).name || '',
            meta: `已交 ${list.length}/${classMembers(a.classId).length} · 已批 ${list.filter((s) => s.status === 'GRADED').length}`,
            status: a.status, dueAt: a.dueAt
          };
        })
    });
  }

  const myClasses = db.classes.filter((c) => db.members.some((m) => m.classId === c.id && m.userId === user.id));
  const myAssignments = db.assignments.filter((a) => a.status === 'PUBLISHED' && myClasses.some((c) => c.id === a.classId));
  const mySubs = db.submissions.filter((s) => s.studentId === user.id);
  const graded = mySubs.filter((s) => s.status === 'GRADED');
  const pendingAssignments = myAssignments.filter((a) => !mySubs.some((s) => s.assignmentId === a.id));
  const avgRate = graded.length
    ? graded.reduce((t, s) => {
        const a = assignmentById(s.assignmentId) || { totalScore: 100 };
        return t + (s.score || 0) / (a.totalScore || 100);
      }, 0) / graded.length * 100
    : 0;
  res.json({
    role: 'STUDENT',
    cards: [
      { label: '我的班级', value: myClasses.length, unit: '个', icon: 'class' },
      { label: '待完成作业', value: pendingAssignments.length, unit: '项', icon: 'todo', highlight: pendingAssignments.length > 0 },
      { label: '已提交', value: mySubs.length, unit: '份', icon: 'upload' },
      { label: '平均得分率', value: avgRate ? avgRate.toFixed(0) : '—', unit: graded.length ? '%' : '', icon: 'chart' }
    ],
    todo: pendingAssignments
      .sort((a, b) => new Date(a.dueAt || 0) - new Date(b.dueAt || 0))
      .map((a) => ({
        kind: 'ASSIGNMENT', id: a.id, title: a.title,
        subtitle: (classById(a.classId) || {}).name || '',
        meta: a.dueAt ? '截止 ' + new Date(a.dueAt).toLocaleString('zh-CN', { hour12: false }) : '不限时',
        dueAt: a.dueAt, overdue: a.dueAt ? new Date(a.dueAt) < new Date() : false
      })),
    recent: mySubs
      .slice().sort((a, b) => new Date(b.submittedAt) - new Date(a.submittedAt)).slice(0, 5)
      .map((s) => ({
        kind: 'SUBMISSION', id: s.id, assignmentId: s.assignmentId,
        title: (assignmentById(s.assignmentId) || {}).title || '',
        subtitle: s.status === 'GRADED' ? '已批改' : '等待批改',
        meta: s.status === 'GRADED' ? `${s.score} / ${(assignmentById(s.assignmentId) || {}).totalScore || 100} 分` : '已提交',
        status: s.status, gradedAt: s.gradedAt
      }))
  });
});

app.use('/api', (req, res) => res.status(404).json({ message: '接口不存在：' + req.method + ' ' + req.originalUrl }));

app.use((err, req, res, next) => {
  if (err instanceof multer.MulterError) {
    const msg = err.code === 'LIMIT_FILE_SIZE' ? '单个文件不能超过 20MB' : '文件上传失败：' + err.message;
    return res.status(400).json({ message: msg });
  }
  console.error(err);
  res.status(500).json({ message: err.message || '服务器内部错误' });
});

app.listen(PORT, '0.0.0.0', () => console.log(`[mock-api] 家庭作业在线系统 Mock API 已启动: http://localhost:${PORT}/api`));
