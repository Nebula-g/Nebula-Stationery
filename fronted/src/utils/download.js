import api from '../api'

/** 调后端导出接口并触发浏览器下载 */
export async function downloadExcel(url, params, fallbackName = '导出数据.xlsx') {
  const res = await api.get(url, { params, responseType: 'blob' })

  if (res.data.type && res.data.type.includes('application/json')) {
    const text = await res.data.text()
    throw new Error(JSON.parse(text).message || '导出失败')
  }

  const disposition = res.headers['content-disposition'] || ''
  const matched = /filename\*=UTF-8''([^;]+)/.exec(disposition) || /filename="?([^";]+)"?/.exec(disposition)
  const fileName = matched ? decodeURIComponent(matched[1]) : fallbackName

  const link = document.createElement('a')
  link.href = window.URL.createObjectURL(res.data)
  link.download = fileName
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(link.href)
}
