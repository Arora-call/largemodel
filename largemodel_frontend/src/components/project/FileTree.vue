<template>
  <div class="file-tree">
    <div class="tree-title">📂 项目文件</div>
    <div class="tree-list">
      <TreeNode
        v-for="node in treeData"
        :key="node.path"
        :node="node"
        :active-file="activeFile"
        :depth="0"
        @select="$emit('select', $event)"
      />
    </div>
    <div v-if="!treeData.length" class="tree-empty">暂无文件</div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  files: { type: Array, default: () => [] },
  activeFile: { type: String, default: '' }
})

defineEmits(['select'])

// 将扁平 files[] 转为树形结构
const treeData = computed(() => {
  if (!props.files.length) return []
  const root = { children: {}, files: [] }
  for (const f of props.files) {
    const parts = f.path.split('/')
    let current = root
    for (let i = 0; i < parts.length - 1; i++) {
      if (!current.children[parts[i]]) current.children[parts[i]] = { children: {}, files: [] }
      current = current.children[parts[i]]
    }
    current.files.push({ name: parts[parts.length - 1], ...f })
  }
  return buildNodes(root)
})

function buildNodes(node, basePath = '') {
  const result = []
  // 文件夹
  for (const [name, child] of Object.entries(node.children)) {
    const path = basePath ? basePath + '/' + name : name
    result.push({
      type: 'folder',
      name,
      path,
      children: buildNodes(child, path)
    })
  }
  // 文件
  for (const f of node.files) {
    result.push({ type: 'file', name: f.name, path: f.path, language: f.language })
  }
  // 排序：文件夹在前
  result.sort((a, b) => {
    if (a.type !== b.type) return a.type === 'folder' ? -1 : 1
    return a.name.localeCompare(b.name)
  })
  return result
}
</script>

<!-- TreeNode recursive component -->
<script>
import { ref, h } from 'vue'

const TreeNode = {
  name: 'TreeNode',
  props: { node: Object, activeFile: String, depth: Number },
  emits: ['select'],
  setup(props, { emit }) {
    const expanded = ref(props.depth < 2)
    const langIcon = (lang) => {
      const map = { vue: '🟢', java: '🟠', python: '🔵', javascript: '🟡', json: '🔷', html: '🔴', xml: '🟤', css: '🟣', markdown: '📝' }
      return map[lang] || '📄'
    }
    return () => {
      if (props.node.type === 'folder') {
        return h('div', { class: 'tree-folder' }, [
          h('div', {
            class: 'tree-node folder-node',
            style: { paddingLeft: props.depth * 16 + 8 + 'px' },
            onClick: () => { expanded.value = !expanded.value }
          }, [
            h('span', { class: 'node-icon' }, expanded.value ? '📂' : '📁'),
            h('span', { class: 'node-name' }, props.node.name)
          ]),
          expanded.value && props.node.children
            ? props.node.children.map(c => h(TreeNode, {
              node: c, activeFile: props.activeFile, depth: props.depth + 1,
              onSelect: (p) => emit('select', p)
            }))
            : null
        ])
      }
      return h('div', {
        class: ['tree-node file-node', { active: props.activeFile === props.node.path }],
        style: { paddingLeft: props.depth * 16 + 28 + 'px' },
        onClick: () => emit('select', props.node.path)
      }, [
        h('span', { class: 'node-icon' }, langIcon(props.node.language)),
        h('span', { class: 'node-name' }, props.node.name)
      ])
    }
  }
}
</script>

<style scoped>
.file-tree { background: var(--bg-sidebar); border-right: 1px solid var(--border); height: 100%; overflow-y: auto; user-select: none; }
.tree-title { padding: 14px 16px; font-size: 13px; font-weight: 600; color: #e5e7eb; border-bottom: 1px solid var(--border); }
.tree-list { padding: 4px 0; }
.tree-empty { padding: 20px; text-align: center; color: var(--text-dim); font-size: 13px; }
.tree-node { display: flex; align-items: center; gap: 6px; padding: 5px 8px; cursor: pointer; font-size: 13px; color: #c9d1d9; transition: background .15s; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.tree-node:hover { background: rgba(124,138,255,0.08); }
.tree-node.active { background: rgba(124,138,255,0.15); color: var(--accent); }
.node-icon { font-size: 14px; flex-shrink: 0; }
.node-name { overflow: hidden; text-overflow: ellipsis; }
</style>
