import request from './request'

/**
 * 获取用户列表（分页）
 */
export function listUsers(params) {
  return request.get('/admin/users', { params })
}

/**
 * 获取用户详情
 */
export function getUserDetail(userId) {
  return request.get(`/admin/users/${userId}`)
}

/**
 * 更新用户状态
 */
export function updateUserStatus(userId, status) {
  return request.put(`/admin/users/${userId}/status`, null, { params: { status } })
}

/**
 * 更新用户角色
 */
export function updateUserRole(userId, role) {
  return request.put(`/admin/users/${userId}/role`, null, { params: { role } })
}

/**
 * 删除用户
 */
export function deleteUser(userId) {
  return request.delete(`/admin/users/${userId}`)
}

/**
 * 重置用户密码
 */
export function resetUserPassword(userId, newPassword) {
  return request.put(`/admin/users/${userId}/password`, null, { params: { newPassword } })
}

/**
 * 获取统计数据
 */
export function getStats() {
  return request.get('/admin/stats')
}

// ====== AI 模型配置 ✨ ======

/** 获取全部模型列表（管理用） */
export function listModels() {
  return request.get('/admin/models')
}
/** 获取单个模型详情 */
export function getModel(id) {
  return request.get(`/admin/models/${id}`)
}
/** 新增模型 */
export function createModel(data) {
  return request.post('/admin/models', data)
}
/** 更新模型 */
export function updateModel(id, data) {
  return request.put(`/admin/models/${id}`, data)
}
/** 删除模型 */
export function deleteModel(id) {
  return request.delete(`/admin/models/${id}`)
}
/** 测试模型连接 */
export function testModelConnection(id) {
  return request.post(`/admin/models/${id}/test`)
}
/** 获取已启用模型列表（前端 AI 对话页用，apiKey 已脱敏） */
export function listEnabledModels() {
  return request.get('/admin/models/enabled')
}
