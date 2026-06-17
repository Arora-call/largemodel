import request from './request'

/**
 * 获取用户个人信息
 */
export function getUserInfo() {
  return request.get('/user/info')
}

/**
 * 更新个人信息
 */
export function updateUserInfo(data) {
  return request.put('/user/info', data)
}

/**
 * 修改密码
 */
export function changePassword(data) {
  return request.put('/user/password', data)
}

/**
 * 注销账户
 */
export function deleteAccount() {
  return request.delete('/user/account')
}
