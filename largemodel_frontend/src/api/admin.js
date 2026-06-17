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
