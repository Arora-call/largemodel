<template>
  <div class="calculator">
    <div class="display">
      <div class="expression">{{ displayExpression }}</div>
      <div class="result" :class="{ 'result-small': displayValue.length > 12 }">{{ displayValue }}</div>
    </div>
    <div class="buttons">
      <button
        v-for="btn in buttons"
        :key="btn.label"
        :class="['btn', btn.type]"
        :style="btn.span ? { gridColumn: `span ${btn.span}` } : {}"
        @click="handleClick(btn)"
      >
        {{ btn.label }}
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

// ── 状态 ──
const currentValue = ref('0')        // 当前显示的数字
const previousValue = ref('')        // 上一个操作数
const operator = ref('')             // 当前运算符
const waitingForOperand = ref(false) // 是否等待输入下一个操作数
const expression = ref('')           // 表达式显示

// ── 计算显示 ──
const displayValue = computed(() => {
  const val = currentValue.value
  // 格式化数字，带千位分隔符
  const parts = val.split('.')
  parts[0] = parts[0].replace(/\B(?=(\d{3})+(?!\d))/g, ',')
  return parts.join('.')
})

const displayExpression = computed(() => {
  if (!expression.value) return '0'
  return expression.value
})

// ── 按钮配置 ──
const buttons = [
  { label: 'AC', type: 'function', action: 'clear' },
  { label: '±', type: 'function', action: 'negate' },
  { label: '%', type: 'function', action: 'percent' },
  { label: '÷', type: 'operator', action: 'operator', value: '/' },
  { label: '7', type: 'number', action: 'digit' },
  { label: '8', type: 'number', action: 'digit' },
  { label: '9', type: 'number', action: 'digit' },
  { label: '×', type: 'operator', action: 'operator', value: '*' },
  { label: '4', type: 'number', action: 'digit' },
  { label: '5', type: 'number', action: 'digit' },
  { label: '6', type: 'number', action: 'digit' },
  { label: '−', type: 'operator', action: 'operator', value: '-' },
  { label: '1', type: 'number', action: 'digit' },
  { label: '2', type: 'number', action: 'digit' },
  { label: '3', type: 'number', action: 'digit' },
  { label: '+', type: 'operator', action: 'operator', value: '+' },
  { label: '0', type: 'number', action: 'digit', span: 2 },
  { label: '.', type: 'number', action: 'decimal' },
  { label: '=', type: 'equal', action: 'calculate' },
]

// ── 工具函数 ──
function formatDisplayValue(val) {
  // 限制长度，防止溢出
  const str = String(val)
  if (str.length > 15) {
    return parseFloat(val).toExponential(6)
  }
  return str
}

function calculate(a, op, b) {
  const numA = parseFloat(a)
  const numB = parseFloat(b)
  let result = 0
  switch (op) {
    case '+': result = numA + numB; break
    case '-': result = numA - numB; break
    case '*': result = numA * numB; break
    case '/':
      if (numB === 0) {
        return 'Error'
      }
      result = numA / numB
      break
    default: return b
  }
  // 处理浮点精度
  const rounded = Math.round(result * 1e10) / 1e10
  return String(rounded)
}

// ── 核心逻辑 ──
function handleClick(btn) {
  switch (btn.action) {
    case 'digit':
      inputDigit(btn.label)
      break
    case 'decimal':
      inputDecimal()
      break
    case 'operator':
      handleOperator(btn.value)
      break
    case 'calculate':
      handleEqual()
      break
    case 'clear':
      handleClear()
      break
    case 'negate':
      handleNegate()
      break
    case 'percent':
      handlePercent()
      break
  }
}

function inputDigit(digit) {
  if (waitingForOperand.value) {
    currentValue.value = digit
    waitingForOperand.value = false
  } else {
    if (currentValue.value === '0' && digit !== '.') {
      currentValue.value = digit
    } else {
      if (currentValue.value.replace('-', '').replace('.', '').length >= 15) return
      currentValue.value += digit
    }
  }
  // 更新表达式显示
  updateExpressionDisplay()
}

function inputDecimal() {
  if (waitingForOperand.value) {
    currentValue.value = '0.'
    waitingForOperand.value = false
    return
  }
  if (!currentValue.value.includes('.')) {
    currentValue.value += '.'
  }
  updateExpressionDisplay()
}

function handleOperator(op) {
  const cur = currentValue.value
  if (operator.value && !waitingForOperand.value) {
    const result = calculate(previousValue.value, operator.value, cur)
    if (result === 'Error') {
      currentValue.value = 'Error'
      previousValue.value = ''
      operator.value = ''
      expression.value = ''
      waitingForOperand.value = true
      return
    }
    previousValue.value = result
    currentValue.value = result
  } else {
    previousValue.value = cur
  }
  operator.value = op
  waitingForOperand.value = true
  // 更新表达式
  expression.value = `${previousValue.value} ${getOperatorSymbol(op)} `
}

function handleEqual() {
  const cur = currentValue.value
  if (!operator.value || waitingForOperand.value) {
    // 没有运算符时按等号不做任何事
    if (!operator.value) return
    // 如果正在等待操作数，直接显示之前的结果
    currentValue.value = previousValue.value
    return
  }
  const result = calculate(previousValue.value, operator.value, cur)
  if (result === 'Error') {
    currentValue.value = 'Error'
    expression.value = ''
    previousValue.value = ''
    operator.value = ''
    waitingForOperand.value = true
    return
  }
  expression.value = `${previousValue.value} ${getOperatorSymbol(operator.value)} ${cur} =`
  currentValue.value = result
  previousValue.value = result
  operator.value = ''
  waitingForOperand.value = true
}

function handleClear() {
  currentValue.value = '0'
  previousValue.value = ''
  operator.value = ''
  waitingForOperand.value = false
  expression.value = ''
}

function handleNegate() {
  if (currentValue.value === '0') return
  if (currentValue.value.startsWith('-')) {
    currentValue.value = currentValue.value.slice(1)
  } else {
    currentValue.value = '-' + currentValue.value
  }
  updateExpressionDisplay()
}

function handlePercent() {
  const num = parseFloat(currentValue.value)
  if (isNaN(num)) return
  currentValue.value = String(num / 100)
  updateExpressionDisplay()
}

function getOperatorSymbol(op) {
  const map = { '+': '+', '-': '−', '*': '×', '/': '÷' }
  return map[op] || op
}

function updateExpressionDisplay() {
  if (operator.value) {
    expression.value = `${previousValue.value} ${getOperatorSymbol(operator.value)} ${currentValue.value}`
  } else {
    expression.value = currentValue.value
  }
}
</script>

<style scoped>
.calculator {
  width: 340px;
  background: #16213e;
  border-radius: 20px;
  padding: 20px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.5);
}

.display {
  background: #0f3460;
  border-radius: 12px;
  padding: 16px 20px;
  margin-bottom: 16px;
  min-height: 90px;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  align-items: flex-end;
  overflow: hidden;
}

.expression {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.5);
  word-break: break-all;
  text-align: right;
  width: 100%;
  min-height: 20px;
  line-height: 1.4;
}

.result {
  font-size: 40px;
  font-weight: 300;
  color: #fff;
  word-break: break-all;
  text-align: right;
  width: 100%;
  line-height: 1.2;
  transition: font-size 0.15s;
}

.result-small {
  font-size: 28px;
}

.buttons {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
}

.btn {
  height: 60px;
  border: none;
  border-radius: 12px;
  font-size: 22px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.15s ease;
  user-select: none;
  outline: none;
  display: flex;
  align-items: center;
  justify-content: center;
}

.btn:active {
  transform: scale(0.94);
}

.btn.number {
  background: #1a1a40;
  color: #fff;
}
.btn.number:hover {
  background: #2a2a5a;
}

.btn.function {
  background: #0f3460;
  color: #8ab4f8;
}
.btn.function:hover {
  background: #1a4a80;
}

.btn.operator {
  background: #e94560;
  color: #fff;
}
.btn.operator:hover {
  background: #ff6b81;
}

.btn.equal {
  background: #e94560;
  color: #fff;
}
.btn.equal:hover {
  background: #ff6b81;
}
</style>