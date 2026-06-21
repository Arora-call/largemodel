<template>
  <div class="data-table">
    <!-- 搜索框 -->
    <div class="search-bar">
      <input
        v-model="searchQuery"
        type="text"
        placeholder="搜索..."
        class="search-input"
      />
    </div>

    <!-- 表格 -->
    <table>
      <thead>
        <tr>
          <th v-for="col in columns" :key="col.key" @click="col.sortable && toggleSort(col.key)">
            {{ col.title }}
            <span v-if="col.sortable" class="sort-icon">
              <span v-if="sortKey === col.key && sortOrder === 'asc'">▲</span>
              <span v-else-if="sortKey === col.key && sortOrder === 'desc'">▼</span>
              <span v-else>⇅</span>
            </span>
          </th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="row in paginatedData" :key="row.id">
          <td v-for="col in columns" :key="col.key">{{ row[col.key] }}</td>
        </tr>
        <tr v-if="paginatedData.length === 0">
          <td :colspan="columns.length" class="empty">无数据</td>
        </tr>
      </tbody>
    </table>

    <!-- 分页 -->
    <div class="pagination">
      <button :disabled="currentPage === 1" @click="currentPage--">上一页</button>
      <span>第 {{ currentPage }} / {{ totalPages }} 页</span>
      <button :disabled="currentPage === totalPages" @click="currentPage++">下一页</button>
      <span>共 {{ filteredData.length }} 条记录</span>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const props = defineProps({
  data: {
    type: Array,
    required: true
  },
  columns: {
    type: Array,
    required: true
  },
  pageSize: {
    type: Number,
    default: 10
  }
})

// 搜索
const searchQuery = ref('')

// 排序
const sortKey = ref('')
const sortOrder = ref('asc') // 'asc' | 'desc'

const toggleSort = (key) => {
  if (sortKey.value === key) {
    sortOrder.value = sortOrder.value === 'asc' ? 'desc' : 'asc'
  } else {
    sortKey.value = key
    sortOrder.value = 'asc'
  }
}

// 过滤 + 排序
const filteredData = computed(() => {
  let result = [...props.data]

  // 搜索过滤：遍历所有列（可搜索列）
  if (searchQuery.value) {
    const query = searchQuery.value.toLowerCase()
    result = result.filter(row => {
      return props.columns.some(col => {
        const val = row[col.key]
        return val != null && String(val).toLowerCase().includes(query)
      })
    })
  }

  // 排序
  if (sortKey.value) {
    result.sort((a, b) => {
      const aVal = a[sortKey.value]
      const bVal = b[sortKey.value]
      if (aVal < bVal) return sortOrder.value === 'asc' ? -1 : 1
      if (aVal > bVal) return sortOrder.value === 'asc' ? 1 : -1
      return 0
    })
  }

  return result
})

// 分页
const currentPage = ref(1)
const totalPages = computed(() => Math.ceil(filteredData.value.length / props.pageSize))

const paginatedData = computed(() => {
  const start = (currentPage.value - 1) * props.pageSize
  const end = start + props.pageSize
  return filteredData.value.slice(start, end)
})

// 当过滤条件变化时重置到第一页
import { watch } from 'vue'
watch(searchQuery, () => {
  currentPage.value = 1
})
</script>

<style scoped>
.data-table {
  margin-top: 20px;
}

.search-bar {
  margin-bottom: 15px;
}

.search-input {
  width: 100%;
  padding: 10px 15px;
  border: 1px solid #ccc;
  border-radius: 4px;
  font-size: 14px;
  box-sizing: border-box;
}

table {
  width: 100%;
  border-collapse: collapse;
  background: white;
  box-shadow: 0 1px 3px rgba(0,0,0,0.1);
}

th {
  background: #f8f9fa;
  padding: 12px 15px;
  text-align: left;
  cursor: pointer;
  user-select: none;
  border-bottom: 2px solid #dee2e6;
  color: #333;
  font-weight: 600;
  position: relative;
}

th:hover {
  background: #e9ecef;
}

.sort-icon {
  margin-left: 5px;
  font-size: 12px;
  color: #6c757d;
}

td {
  padding: 10px 15px;
  border-bottom: 1px solid #eee;
  color: #555;
}

tr:hover td {
  background: #f1f3f5;
}

.empty {
  text-align: center;
  color: #999;
  padding: 20px;
}

.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: 20px;
  gap: 15px;
}

.pagination button {
  padding: 8px 20px;
  border: 1px solid #007bff;
  background: white;
  color: #007bff;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s;
}

.pagination button:hover:not(:disabled) {
  background: #007bff;
  color: white;
}

.pagination button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.pagination span {
  color: #666;
  font-size: 14px;
}
</style>