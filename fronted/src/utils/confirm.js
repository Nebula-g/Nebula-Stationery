// 页面内确认框：替代浏览器原生 confirm，配色与轻提示一致
const COLOR = {
  normal: { bg: '#e9f7ef', border: '#5dd39e', text: '#1e7d4d' },
  danger: { bg: '#fdecee', border: '#f5a3ac', text: '#c0293a' }
}

let active = null

const close = (value) => {
  if (!active) return
  const { overlay, onKeydown, resolve } = active
  active = null
  document.removeEventListener('keydown', onKeydown)
  overlay.classList.add('nebula-confirm-hide')
  // 淡出期间禁止残留的遮罩挡住页面点击
  overlay.style.pointerEvents = 'none'
  setTimeout(() => overlay.remove(), 180)
  resolve(value)
}

const confirmAction = (message, options = {}) => {
  const { confirmText = '确定', cancelText = '取消', danger = false } = options
  close(false)

  const color = danger ? COLOR.danger : COLOR.normal
  return new Promise((resolve) => {
    const overlay = document.createElement('div')
    overlay.className = 'nebula-confirm'
    overlay.style.setProperty('--confirm-bg', color.bg)
    overlay.style.setProperty('--confirm-border', color.border)
    overlay.style.setProperty('--confirm-text', color.text)

    const card = document.createElement('div')
    card.className = 'nebula-confirm-card'

    const text = document.createElement('p')
    text.className = 'nebula-confirm-text'
    text.textContent = String(message ?? '')

    const actions = document.createElement('div')
    actions.className = 'nebula-confirm-actions'

    const cancelBtn = document.createElement('button')
    cancelBtn.type = 'button'
    cancelBtn.className = 'nebula-confirm-cancel'
    cancelBtn.textContent = cancelText
    cancelBtn.addEventListener('click', () => close(false))

    const confirmBtn = document.createElement('button')
    confirmBtn.type = 'button'
    confirmBtn.className = 'nebula-confirm-ok'
    confirmBtn.textContent = confirmText
    confirmBtn.addEventListener('click', () => close(true))

    actions.append(cancelBtn, confirmBtn)
    card.append(text, actions)
    overlay.append(card)

    const onKeydown = (event) => {
      if (event.key === 'Escape') close(false)
      if (event.key === 'Enter') close(true)
    }

    overlay.addEventListener('click', (event) => {
      if (event.target === overlay) close(false)
    })
    document.addEventListener('keydown', onKeydown)
    document.body.appendChild(overlay)

    active = { overlay, onKeydown, resolve }
    confirmBtn.focus()
  })
}

export default confirmAction
