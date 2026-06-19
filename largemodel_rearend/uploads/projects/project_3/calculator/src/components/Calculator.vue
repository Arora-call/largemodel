<template>
  <div class="calculator">
    <!-- 显示区域 -->
    <div class="display">
      <div class="expression">{{ expression || '0' }}</div>
      <div class="result">{{ result !== null ? '= ' + result : '' }}</div>
    </div>

    <!-- 按钮网格 -->
    <div class="buttons">
      <button
        v-for="btn in buttons"
        :key="btn.label"
        :class="['btn', btn.type]"
        @click="handleClick(btn)"
      >
        {{ btn.label }}
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'

// 按钮配置
const buttons = reactive([
  { label: 'C', type: 'clear' },
  { label: '±', type: 'toggle' },
  { label: '%', type: 'percent' },
  { label: '÷', type: 'operator' },
  { label: '7', type: 'number' },
  { label: '8', type: 'number' },
  { label: '9', type: 'number' },
  { label: '×', type: 'operator' },
  { label: '4', type: 'number' },
  { label: '5', type: 'number' },
  { label: '6', type: 'number' },
  { label: '-', type: 'operator' },
  { label: '1', type: 'number' },
  { label: '2', type: 'number' },
  { label: '3', type: 'number' },
  { label: '+', type: 'operator' },
  { label: '0', type: 'number zero' },
  { label: '.', type: 'decimal' },
  { label: '⌫', type: 'backspace' },
  { label: '=', type: 'equals' }
])

// 状态
const expression = ref('')
const result = ref(null)
const currentInput = ref('')
const operator = ref(null)
const previousValue = ref(null)
const resetNext = ref(false)

// 处理按钮点击
function handleClick(btn) {
  switch (btn.type) {
    case 'number':
      inputNumber(btn.label)
      break
    case 'decimal':
      inputDecimal()
      break
    case 'operator':
      inputOperator(btn.label)
      break
    case 'equals':
      calculateResult()
      break
    case 'clear':
      clearAll()
      break
    case 'toggle':
      toggleSign()
      break
    case 'percent':
      toPercent()
      break
    case 'backspace':
      backspace()
      break
  }
}

// 输入数字
function inputNumber(num) {
  if (resetNext.value) {
    currentInput.value = ''
    result.value = null
    resetNext.value = false
  }
  if (currentInput.value === '0' && num !== '.') {
    currentInput.value = num
  } else {
    currentInput.value += num
  }
  expression.value += num
}

// 输入小数点
function inputDecimal() {
  if (resetNext.value) {
    currentInput.value = '0.'
    expression.value = '0.'
    resetNext.value = false
    return
  }
  if (!currentInput.value.includes('.')) {
    if (currentInput.value === '') {
      currentInput.value = '0.'
      expression.value += '0.'
    } else {
      currentInput.value += '.'
      expression.value += '.'
    }
  }
}

// 输入运算符
function inputOperator(op) {
  if (operator.value && !resetNext.value) {
    // 有上一个运算符，先计算结果
    calculateResult()
    // 用结果继续运算
    previousValue.value = result.value
    result.value = null
  } else {
    previousValue.value = parseFloat(currentInput.value)
  }
  operator.value = op
  expression.value += opSymbol(op)
  resetNext.value = true
}

// 运算符符号映射
function opSymbol(op) {
  const map = { '+': '+', '-': '-', '×': '×', '÷': '÷' }
  return map[op] || op
}

// 计算结果
function calculateResult() {
  const current = parseFloat(currentInput.value)
  const prev = previousValue.value
  let calcResult = 0

  if (prev === null || operator.value === null) {
    result.value = current
    return
  }

  switch (operator.value) {
    case '+':
      calcResult = prev + current
      break
    case '-':
      calcResult = prev - current
      break
    case '×':
      calcResult = prev * current
      break
    case '÷':
      calcResult = current !== 0 ? prev / current : 'Error'
      break
    default:
      calcResult = current
  }

  result.value = calcResult
  currentInput.value = String(calcResult)
  previousValue.value = null
  operator.value = null
  resetNext.value = true
}

// 清空所有
function clearAll() {
  expression.value = ''
  result.value = null
  currentInput.value = ''
  operator.value = null
  previousValue.value = null
  resetNext.value = false
}

// 正负切换
function toggleSign() {
  if (currentInput.value && currentInput.value !== '0') {
    if (currentInput.value.startsWith('-')) {
      currentInput.value = currentInput.value.slice(1)
      expression.value = expression.value.replace(/^-/, '')
    } else {
      currentInput.value = '-' + currentInput.value
      expression.value = '-' + expression.value
    }
  }
}

// 百分比
function toPercent() {
  if (currentInput.value) {
    const num = parseFloat(currentInput.value) / 100
    currentInput.value = String(num)
    expression.value = expression.value.replace(/\d+(\.\d+)?$/, num)
  }
}

// 退格
function backspace() {
  if (resetNext.value) return
  if (currentInput.value.length > 0) {
    currentInput.value = currentInput.value.slice(0, -1)
    expression.value = expression.value.slice(0, -1)
    if (currentInput.value === '') currentInput.value = '0'
  }
}
</script>

<style scoped>
.calculator {
  width: 340px;
  background: #1c1c1e;
  border-radius: 20px;
  padding: 20px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.3);
  user-select: none;
}

.display {
  background: #2c2c2e;
  border-radius: 12px;
  padding: 20px 16px;
  margin-bottom: 16px;
  min-height: 80px;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  align-items: flex-end;
  overflow: hidden;
}

.expression {
  font-size: 28px;
  color: #f5f5f5;
  word-break: break-all;
  text-align: right;
  width: 100%;
  line-height: 1.3;
}

.result {
  font-size: 20px;
  color: #a0a0a5;
  margin-top: 4px;
}

.buttons {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
}

.btn {
  padding: 16px 0;
  border: none;
  border-radius: 12px;
  font-size: 22px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.15s ease;
  text-align: center;
  outline: none;
}

.btn:active {
  transform: scale(0.95);
  opacity: 0.8;
}

.btn.number {
  background: #3a3a3c;
  color: #f5f5f5;
}

.btn.number.zero {
  grid-column: span 2;
}

.btn.operator {
  background: #ff9f0a;
  color: white;
}

.btn.clear,
.btn.toggle,
.btn.percent,
.btn.backspace {
  background: #505054;
  color: #f5f5f5;
}

.btn.decimal {
  background: #3a3a3c;
  color: #f5f5f5;
}

.btn.equals {
  background: #ff9f0a;
  color: white;
}

.btn:hover {
  filter: brightness(1.15);
}
</style>