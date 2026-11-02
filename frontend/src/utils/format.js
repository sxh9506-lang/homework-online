import dayjs from 'dayjs'

export const fmtDate = (v, pattern = 'YYYY-MM-DD HH:mm') => (v ? dayjs(v).format(pattern) : '—')
export const fmtDay = (v) => (v ? dayjs(v).format('MM-DD') : '—')

export function fromNow(v) {
  if (!v) return '—'
  const diff = Date.now() - new Date(v).getTime()
  const abs = Math.abs(diff)
  const min = 60000
  if (abs < min) return diff >= 0 ? '刚刚' : '即将'
  if (abs < 60 * min) return diff >= 0 ? `${Math.floor(abs / min)} 分钟前` : `${Math.floor(abs / min)} 分钟后`
  if (abs < 24 * 60 * min) return diff >= 0 ? `${Math.floor(abs / (60 * min))} 小时前` : `${Math.floor(abs / (60 * min))} 小时后`
  if (abs < 30 * 24 * 60 * min) return diff >= 0 ? `${Math.floor(abs / (24 * 60 * min))} 天前` : `${Math.floor(abs / (24 * 60 * min))} 天后`
  return fmtDate(v, 'YYYY-MM-DD')
}

export function dueState(dueAt) {
  if (!dueAt) return { text: '不限时', type: 'gray', urgent: false }
  const diff = new Date(dueAt).getTime() - Date.now()
  if (diff < 0) return { text: `已截止 · ${fmtDate(dueAt, 'MM-DD HH:mm')}`, type: 'red', urgent: false, overdue: true }
  const hours = diff / 3600000
  const text = hours < 24 ? `${fmtDate(dueAt, 'HH:mm')} 截止（剩 ${Math.max(1, Math.round(hours))} 小时）` : `${fmtDate(dueAt, 'MM-DD HH:mm')} 截止`
  return { text, type: hours < 24 ? 'orange' : 'gray', urgent: hours < 24 }
}

export const ASSIGNMENT_STATUS = {
  DRAFT: { text: '草稿', type: 'gray' },
  PUBLISHED: { text: '已发布', type: 'blue' },
  CLOSED: { text: '已结束', type: 'gray' }
}

export const SUBMISSION_STATUS = {
  SUBMITTED: { text: '待批改', type: 'orange' },
  GRADED: { text: '已批改', type: 'green' },
  RETURNED: { text: '已退回', type: 'red' }
}

export function fileSize(bytes) {
  if (!bytes && bytes !== 0) return ''
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`
}

export function avatarText(name = '') {
  return name.slice(-2) || '用户'
}

export function avatarColor(seed = '') {
  const colors = ['#4a5cf5', '#0ea5a4', '#d97706', '#db2777', '#7c3aed', '#0d8a6a', '#c2410c']
  let sum = 0
  for (const ch of seed) sum += ch.charCodeAt(0)
  return colors[sum % colors.length]
}
