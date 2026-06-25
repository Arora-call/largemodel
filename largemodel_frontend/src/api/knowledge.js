/**
 * 知识库 API — Knowledge Service
 */
import request from './request'

// 文档上传
export function uploadDocument(formData) {
  return request.post('/knowledge/documents', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

// 文档列表
export function listDocuments(params) {
  return request.get('/knowledge/documents', { params })
}

// 删除文档
export function deleteDocument(id) {
  return request.delete(`/knowledge/documents/${id}`)
}

// 语义检索
export function semanticSearch(query, params = {}) {
  return request.post('/knowledge/search', { query, ...params })
}

// 知识库集合列表
export function listCollections() {
  return request.get('/knowledge/collections')
}
