<template>
  <div class="calculator">
    <div class="display">{{ display }}</div>

    <div class="buttons">
      <button
        v-for="btn in buttons"
        :key="btn.label"
        @click="handleButton(btn)"
        :class="['btn', btn.type]"
      >
        {{ btn.label }}
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'

// 计算器状态
const display = ref('0')
const currentNumber = ref('')
const previousNumber = ref(null)
const operator = ref(null)
const waitingForNumber = ref(false)

// 按钮定义
const buttons = reactive([
  // 清空和符号
  { label: 'C', value: 'clear', type: 'function' },
  { label: '±', value: 'toggleSign', type: 'function' },
  { label: '%', value: 'percent', type: 'function' },
  { label: '÷', value: '/', type: 'operator' },

  // 数字键1-9
  { label: '7', value: '7', type: 'number' },
  { label: '8', value: '8', type: 'number' },
  { label: '9', value: '9', type: 'number' },
  { label: '×', value: '*', type: 'operator' },

  { label: '4', value: '4', type: 'number' },
  { label: '5', value: '5', type: 'number' },
  { label: '6', value: '6', type: 'number' },
  { label: '−', value: '-', type: 'operator' },

  { label: '1', value: '1', type: 'number' },
  { label: '2', value: '2', type: 'number' },
  { label: '3', value: '3', type: 'number' },
  { label: '+', value: '+', type: 'operator' },

  // 0、小数点和等号
  { label: '0', value: '0', type: 'number', span: true },
  { label: '.', value: '.', type: 'number' },
  { label: '=', value: 'equals', type: 'equals' },
])

// 数字点击
function inputNumber(value) {
  if (waitingForNumber.value) {
    currentNumber.value = value === '.' ? '0.' : value
    waitingForNumber.value = false
  } else {
    if (value === '.' && currentNumber.value.includes('.')) return
    currentNumber.value = currentNumber.value === '0' && value !== '.' ? value : currentNumber.value + value
  }
  display.value = currentNumber.value
}

// 运算符点击
function inputOperator(op) {
  if (operator.value && !waitingForNumber.value) {
    calculate()
  }
  previousNumber.value = parseFloat(currentNumber.value)
  operator.value = op
  waitingForNumber.value = true
  display.value = currentNumber.value + ' ' + getSymbol(op)
}

// 计算结果
function calculate() {
  const prev = previousNumber.value
  const current = parseFloat(currentNumber.value)
  if (isNaN(prev) || isNaN(current)) return

  let result
  switch (operator.value) {
    case '+':
      result = prev + current
      break
    case '-':
      result = prev - current
      break
    case '*':
      result = prev * current
      break
    case '/':
      result = current === 0 ? 'Error' : prev / current
      break
    default:
      return
  }
  if (result === 'Error') {
    display.value = 'Error'
    currentNumber.value = ''
    previousNumber.value = null
    operator.value = null
    waitingForNumber.value = false
    return
  }
  display.value = result.toString()
  currentNumber.value = result.toString()
  previousNumber.value = null
  operator.value = null
  waitingForNumber.value = true
}

// 辅助函数：获取运算符符号
function getSymbol(op) {
  const map = { '+': '+', '-': '−', '*': '×', '/': '÷' }
  return map[op] || op
}

// 按钮处理
function handleButton(btn) {
  switch (btn.type) {
    case 'number':
      inputNumber(btn.value)
      break
    case 'operator':
      inputOperator(btn.value)
      break
    case 'equals':
      if (operator.value) calculate()
      break
    case 'function':
      if (btn.value === 'clear') {
        currentNumber.value = ''
        previousNumber.value = null
        operator.value = null
        waitingForNumber.value = false
        display.value = '0'
      } else if (btn.value === 'toggleSign') {
        if (currentNumber.value) {
          currentNumber.value = (parseFloat(currentNumber.value) * -1).toString()
          display.value = currentNumber.value
        }
      } else if (btn.value === 'percent') {
        if (currentNumber.value) {
          currentNumber.value = (parseFloat(currentNumber.value) / 100).toString()
          display.value = currentNumber.value
        }
      }
      break
  }
}
</script>

<style scoped>
.calculator {
  background: #16213e;
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.5);
  width: 300px;
}

.display {
  background: #0f3460;
  color: #e4f1fe;
  font-size: 2.5rem;
  text-align: right;
  padding: 20px 15px;
  border-radius: 10px;
  margin-bottom: 20px;
  min-height: 80px;
  display: flex;
  align-items: flex-end;
  justify-content: flex-end;
  word-break: break-all;
  line-height: 1.2;
}

.buttons {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
}

.btn {
  padding: 15px 0;
  font-size: 1.3rem;
  border: none;
  border-radius: 10px;
  cursor: pointer;
  background: #1a1a40;
  color: #e4f1fe;
  transition: background 0.2s, transform 0.1s;
}

.btn:active {
  transform: scale(0.95);
}

.btn.function {
  background: #533483;
  color: #fff;
}

.btn.function:hover {
  background: #6a4a9a;
}

.btn.operator {
  background: #e94560;
  color: #fff;
}

.btn.operator:hover {
  background: #f0607a;
}

.btn.equals {
  background: #0f3460;
  color: #e4f1fe;
}

.btn.equals:hover {
  background: #1a4a80;
}

.btn.number:hover {
  background: #2a2a5a;
}
</style>