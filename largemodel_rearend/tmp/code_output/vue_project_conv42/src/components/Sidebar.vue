<template>
  <aside class="sidebar">
    <div class="logo">
      <h2>Admin</h2>
    </div>
    <nav>
      <router-link to="/dashboard" class="nav-item" active-class="active">
        <span class="icon">📊</span>
        <span class="text">Dashboard</span>
      </router-link>
      <router-link to="/users" class="nav-item" active-class="active">
        <span class="icon">👥</span>
        <span class="text">Users</span>
      </router-link>
    </nav>
  </aside>
</template>

<script setup>
</script>

<style scoped>
.sidebar {
  width: 240px;
  background-color: #001529;
  color: #fff;
  display: flex;
  flex-direction: column;
  height: 100%;
  flex-shrink: 0;
}

.logo {
  padding: 20px;
  text-align: center;
  border-bottom: 1px solid rgba(255,255,255,0.1);
}

.logo h2 {
  margin: 0;
  font-weight: 600;
  letter-spacing: 2px;
  color: #ffd700; /* 改为金色 */
}

nav {
  flex: 1;
  padding: 10px 0;
}

.nav-item {
  display: flex;
  align-items: center;
  padding: 12px 20px;
  color: rgba(255,255,255,0.65);
  transition: all 0.3s;
  cursor: pointer;
}

.nav-item:hover {
  color: #fff;
  background-color: #1890ff;
}

.nav-item.active {
  color: #fff;
  background-color: #1890ff;
}

.icon {
  margin-right: 10px;
  font-size: 18px;
}

.text {
  font-size: 14px;
}

@media (max-width: 768px) {
  .sidebar {
    width: 100%;
    height: auto;
    flex-direction: row;
    align-items: center;
    justify-content: space-between;
  }
  .logo {
    border-bottom: none;
    padding: 10px;
  }
  nav {
    display: flex;
    padding: 0;
  }
  .nav-item {
    padding: 10px 15px;
  }
}
</style>