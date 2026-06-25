<template>
  <aside class="sidebar">
    <div class="logo">Admin</div>
    <nav>
      <router-link to="/">仪表盘</router-link>
      <router-link to="/users">用户管理</router-link>
    </nav>
  </aside>
</template>

<style scoped>
.sidebar {
  width: 200px;
  background: #001529;
  color: white;
  display: flex;
  flex-direction: column;
  padding: 20px;
}
.logo {
  font-size: 20px;
  font-weight: bold;
  margin-bottom: 30px;
}
nav a {
  display: block;
  padding: 10px 0;
  color: #aaa;
  transition: color 0.3s;
}
nav a:hover {
  color: white;
}
nav a.router-link-active {
  color: #1890ff;
}
</style>