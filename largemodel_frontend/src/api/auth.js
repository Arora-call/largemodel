import request from './request'

/**
 * 用户注册
 */
export function register(data) {
  return request.post('/auth/register', data)
}

/**
 * 用户登录
 */
export function login(data) {
  return request.post('/auth/login', data)
}

/**
 * 获取当前用户信息
 */
export function getCurrentUser() {
  return request.get('/auth/me')
}

/**
 * 找回密码：用户名+邮箱验证后重置密码
 */
export function forgotPassword(data) {
  return request.post('/auth/forgot-password', data)
}
