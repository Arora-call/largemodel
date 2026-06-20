/**
 * 监控大盘 API — Monitor / Admin Service
 */
import request from './request'

// 统计概览
export function getStatsOverview() {
  return request.get('/admin/stats/overview')
}

// 调用量统计
export function getCallStats(params) {
  return request.get('/admin/stats/calls', { params })
}

// Token 消耗统计
export function getTokenStats(params) {
  return request.get('/admin/stats/tokens', { params })
}

// 延迟统计
export function getLatencyStats(params) {
  return request.get('/admin/stats/latency', { params })
}

// 模型调用占比
export function getModelDistribution(params) {
  return request.get('/admin/stats/models', { params })
}

// 操作日志（已移到 admin/logs 复用）
export function getOperationLogs(params) {
  return request.get('/admin/logs', { params })
}
