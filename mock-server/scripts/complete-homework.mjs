/**
 * 让演示学生把作业做完（演示"提交作业 / 上传附件 / 待批改"全流程）。
 *
 *   node scripts/complete-homework.mjs                    # 默认学生 chen（陈嘉禾）
 *   node scripts/complete-homework.mjs --student moshen   # 指定学生（answers.mjs 中的 profile）
 *   node scripts/complete-homework.mjs --force            # 重新提交（覆盖未批改的答案）
 *   node scripts/complete-homework.mjs --allow-late       # 先由老师放开"允许逾期补交"，交完再改回
 *   API_BASE=http://localhost:8081 node scripts/complete-homework.mjs
 *
 * 只走公开 REST 接口，不直接改数据库；删除 db.json 重置库后可再次运行。
 */
import { PROFILES, HANDWRITING_SVG } from './answers.mjs';

const API_BASE = (process.env.API_BASE || 'http://localhost:8080').replace(/\/$/, '');
const PASSWORD = process.env.STUDENT_PASSWORD || '123456';

const args = process.argv.slice(2);
const flag = (name) => args.includes(`--${name}`);
const option = (name, fallback) => {
  const withEq = args.find((a) => a.startsWith(`--${name}=`));
  if (withEq) return withEq.split('=')[1];
  const idx = args.indexOf(`--${name}`);
  if (idx >= 0 && args[idx + 1] && !args[idx + 1].startsWith('--')) return args[idx + 1];
  return fallback;
};

const studentKey = option('student', process.env.STUDENT_USERNAME || 'chen');
const profile = PROFILES[studentKey];
if (!profile) {
  console.error(`✗ 没有名为「${studentKey}」的答案配置，可选：${Object.keys(PROFILES).join(', ')}`);
  process.exit(1);
}
const FORCE = flag('force');
const ALLOW_LATE = flag('allow-late');
const TEACHER = { username: process.env.TEACHER_USERNAME || 'zeng', password: process.env.TEACHER_PASSWORD || '123456' };

let token = '';
let teacherToken = '';

async function api(pathname, { method = 'GET', body, form, as = 'student' } = {}) {
  const headers = {};
  const current = as === 'teacher' ? teacherToken : token;
  if (current) headers.Authorization = `Bearer ${current}`;
  if (body) headers['Content-Type'] = 'application/json';
  const res = await fetch(API_BASE + pathname, { method, headers, body: form || (body ? JSON.stringify(body) : undefined) });
  const text = await res.text();
  let data = null;
  try {
    data = text ? JSON.parse(text) : null;
  } catch {
    data = text;
  }
  if (!res.ok) throw new Error(`${method} ${pathname} -> ${res.status} ${data?.message || text}`);
  return data;
}

async function login(username, password, as = 'student') {
  const res = await api('/api/auth/login', { method: 'POST', body: { username, password }, as });
  if (as === 'teacher') teacherToken = res.token;
  else token = res.token;
  return res.user;
}

async function uploadHandwriting() {
  const fd = new FormData();
  fd.append('files', new Blob([HANDWRITING_SVG], { type: 'image/svg+xml' }), `${profile.displayName}-手写稿.svg`);
  const [file] = await api('/api/files', { method: 'POST', form: fd });
  console.log(`  ↑ 已上传附件：${file.name}`);
  return file.id;
}

async function main() {
  const user = await login(studentKey, PASSWORD);
  console.log(`✓ 已登录：${user.name}（${user.role}，学号 ${user.studentNo || '—'}）`);

  const assignments = await api('/api/assignments');
  const byId = new Map(assignments.map((a) => [a.id, a]));

  // 支持用作业 id 或作业标题定位
  const findByKey = (key) => {
    if (/^\d+$/.test(String(key))) return byId.get(Number(key));
    const raw = String(key);
    const plain = raw.replace(/^《|》$/g, '');
    return assignments.find((a) => a.title === raw) || assignments.find((a) => a.title === plain);
  };

  const tasks = Object.keys(profile.answers)
    .map((key) => ({ assignment: findByKey(key), content: profile.answers[key], attach: !!profile.attachments?.[key] }))
    .filter((t) => t.assignment);

  const missing = Object.keys(profile.answers).filter((key) => !findByKey(key));
  if (missing.length) console.log(`- 未找到作业：${missing.join('、')}（未发布或不在该学生班级），已跳过`);

  const lateTouched = [];
  let submitted = 0;
  let skipped = 0;

  for (const { assignment, content, attach } of tasks) {
    const target = assignment.title; // 标题本身已带《》
    if (assignment.mySubmission && !FORCE) {
      console.log(`- ${target} 已提交（${assignment.mySubmission.status}），跳过`);
      skipped += 1;
      continue;
    }

    // 逾期且不允许补交：需要老师先放开（仅在 --allow-late 时自动处理）
    if (assignment.overdue && !assignment.allowLate) {
      if (!ALLOW_LATE) {
        console.log(`- ${target} 已截止且不允许补交，跳过（可加 --allow-late 由老师放开后再交）`);
        skipped += 1;
        continue;
      }
      if (!teacherToken) await login(TEACHER.username, TEACHER.password, 'teacher');
      await api(`/api/assignments/${assignment.id}`, {
        method: 'PUT',
        as: 'teacher',
        body: { classId: assignment.classId, allowLate: true }
      });
      lateTouched.push(assignment.id);
      console.log(`  … 老师已临时放开 ${target} 的补交限制`);
    }

    const attachments = attach ? [await uploadHandwriting()] : [];
    const result = await api(`/api/assignments/${assignment.id}/submissions`, {
      method: 'POST',
      body: { content, attachments }
    });
    console.log(
      `✓ 已提交 ${target}：${result.content.length} 字` +
        (attachments.length ? `，含 ${attachments.length} 个附件` : '') +
        (result.late ? '（逾期提交）' : '')
    );
    submitted += 1;
  }

  // 恢复被临时放开的补交限制
  for (const id of lateTouched) {
    const a = byId.get(id);
    await api(`/api/assignments/${id}`, { method: 'PUT', as: 'teacher', body: { classId: a.classId, allowLate: false } });
    console.log(`  … 已恢复作业 ${id} 的补交设置`);
  }

  console.log(`\n完成：新提交 ${submitted} 份，跳过 ${skipped} 份。`);
}

main().catch((err) => {
  console.error('✗ 执行失败：', err.message);
  process.exit(1);
});
