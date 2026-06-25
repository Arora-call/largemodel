/**
 * 监控大盘 API — 用户调用 /monitor（自己数据），管理员调用 /admin/stats（全局数据）
 */
import request from './request'

const isAdmin = () => {
  try {
    const user = JSON.parse(localStorage.getItem('user') || 'null')
    return user?.role === 'ADMIN'
  } catch { return false }
}

// admin 前缀：管理员看全局，普通用户看自己
const prefix = () => isAdmin() ? '/admin/stats' : '/monitor'

// 统计概览
export function getStatsOverview() {
  return request.get(`${prefix()}/overview`)
}

// 调用量统计
export function getCallStats(params) {
  return request.get(`${prefix()}/calls`, { params })
}

// Token 消耗统计
export function getTokenStats(params) {
  return request.get(`${prefix()}/tokens`, { params })
}

// 模型调用占比
export function getModelDistribution(params) {
  return request.get(`${prefix()}/models`, { params })
}

// 操作日志（管理员专用）
export function getOperationLogs(params) {
  return request.get('/admin/logs', { params: { page: params.page || 0, size: params.size || 20, level: params.level, keyword: params.keyword } })
}
