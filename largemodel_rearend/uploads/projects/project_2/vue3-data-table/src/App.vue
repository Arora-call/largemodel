<template>
  <div id="app">
    <h1>用户管理</h1>
    <DataTable :data="users" :columns="columns" :pageSize="5" />
  </div>
</template>

<script setup>
import DataTable from './components/DataTable.vue'

const columns = [
  { key: 'id', title: 'ID', sortable: true },
  { key: 'name', title: '姓名', sortable: true },
  { key: 'email', title: '邮箱', sortable: true },
  { key: 'role', title: '角色', sortable: true },
  { key: 'status', title: '状态' }
]

const users = Array.from({ length: 50 }, (_, i) => ({
  id: i + 1,
  name: `用户${i + 1}`,
  email: `user${i + 1}@example.com`,
  role: ['管理员', '编辑', '普通用户'][Math.floor(Math.random() * 3)],
  status: Math.random() > 0.3 ? '活跃' : '禁用'
}))
</script>

<style>
body {
  font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
  margin: 20px;
  background: #f5f5f5;
}
#app {
  max-width: 1200px;
  margin: 0 auto;
  background: white;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.1);
}
h1 {
  text-align: center;
  color: #333;
}
</style>