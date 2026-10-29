// 全局轻提示：浅绿=成功，浅红=失败，自动消失，无需点击确定
const TOAST_TYPE_COLOR = {
  success: { bg: '#e9f7ef', border: '#5dd39e', text: '#1e7d4d' },
  error: { bg: '#fdecee', border: '#f5a3ac', text: '#c0293a' },
  info: { bg: '#eaf2fb', border: '#9dc3ec', text: '#2b5f92' }
}

const AUTO_DISMISS_MS = 2200

let container = null

const getContainer = () => {
  if (container && document.body.contains(container)) return container
  container = document.createElement('div')
  container.className = 'nebula-toast-container'
  container.style.cssText = [
    'position:fixed',
    'top:16px',
    'left:50%',
    'transform:translateX(-50%)',
    'z-index:10000',
    'display:flex',
    'flex-direction:column',
    'align-items:center',
    'gap:8px',
    'pointer-events:none'
  ].join(';')
  document.body.appendChild(container)
  return container
}

const show = (message, type) => {
  const color = TOAST_TYPE_COLOR[type] || TOAST_TYPE_COLOR.info
  const text = String(message ?? '')

  const parent = getContainer()
  // 拦截器和调用方可能对同一次失败各提示一次，重复内容只显示一条
  const key = `${type}:${text}`
  const dup = parent.querySelector(`[data-key="${CSS.escape(key)}"]`)
  if (dup) return

  const toast = document.createElement('div')
  toast.dataset.key = key
  toast.textContent = text
  toast.style.cssText = [
    'background:' + color.bg,
    'color:' + color.text,
    'border:1px solid ' + color.border,
    'border-left:4px solid ' + color.border,
    'box-shadow:0 4px 16px rgba(0,0,0,0.10)',
    'border-radius:8px',
    'padding:10px 18px',
    'font-size:14px',
    'line-height:1.5',
    'max-width:70vw',
    'word-break:break-all',
    'opacity:0',
    'transform:translateY(-10px)',
    'transition:opacity .22s ease,transform .22s ease'
  ].join(';')

  parent.appendChild(toast)
  requestAnimationFrame(() => {
    toast.style.opacity = '1'
    toast.style.transform = 'translateY(0)'
  })

  setTimeout(() => {
    toast.style.opacity = '0'
    toast.style.transform = 'translateY(-10px)'
    setTimeout(() => toast.remove(), 240)
  }, AUTO_DISMISS_MS)
}

export default {
  success: (message) => show(message, 'success'),
  error: (message) => show(message, 'error'),
  info: (message) => show(message, 'info')
}
