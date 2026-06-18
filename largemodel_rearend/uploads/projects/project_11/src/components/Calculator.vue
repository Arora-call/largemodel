<template>
  <div class="calculator">
    <div class="display">{{ display }}</div>
    <div class="buttons">
      <button v-for="btn in buttons" :key="btn" :class="getClass(btn)" @click="handleClick(btn)">
        {{ btn }}
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const display = ref('0')
const currentInput = ref('')
const previousInput = ref('')
const operator = ref(null)
const waitingForSecond = ref(false)

const buttons = [
  'C', '±', '%', '÷',
  '7', '8', '9', '×',
  '4', '5', '6', '-',
  '1', '2', '3', '+',
  '0', '.', '='
]

function getClass(btn) {
  if (['C', '±', '%'].includes(btn)) return 'btn function'
  if (['÷', '×', '-', '+'].includes(btn)) return 'btn operator'
  if (btn === '=') return 'btn equals'
  return 'btn'
}

function handleClick(btn) {
  if (!isNaN(btn) || btn === '.') {
    inputDigit(btn)
  } else if (btn === 'C') {
    clear()
  } else if (btn === '±') {
    toggleSign()
  } else if (btn === '%') {
    percent()
  } else if (['+', '-', '×', '÷'].includes(btn)) {
    handleOperator(btn)
  } else if (btn === '=') {
    calculate()
  }
}

function inputDigit(num) {
  if (waitingForSecond.value) {
    currentInput.value = num
    waitingForSecond.value = false
  } else {
    if (num === '.' && currentInput.value.includes('.')) return
    currentInput.value = currentInput.value === '0' ? num : currentInput.value + num
  }
  updateDisplay()
}

function clear() {
  currentInput.value = '0'
  previousInput.value = ''
  operator.value = null
  waitingForSecond.value = false
  updateDisplay()
}

function toggleSign() {
  if (currentInput.value !== '0') {
    currentInput.value = String(-parseFloat(currentInput.value))
    updateDisplay()
  }
}

function percent() {
  currentInput.value = String(parseFloat(currentInput.value) / 100)
  updateDisplay()
}

function handleOperator(op) {
  const current = parseFloat(currentInput.value)
  if (operator.value && waitingForSecond.value) {
    operator.value = op
    return
  }
  if (previousInput.value !== '') {
    const prev = parseFloat(previousInput.value)
    const result = compute(prev, current, operator.value)
    currentInput.value = String(result)
    previousInput.value = ''
  } else {
    previousInput.value = currentInput.value
  }
  operator.value = op
  waitingForSecond.value = true
  updateDisplay()
}

function calculate() {
  if (operator.value == null || waitingForSecond.value) return
  const current = parseFloat(currentInput.value)
  const prev = parseFloat(previousInput.value)
  const result = compute(prev, current, operator.value)
  currentInput.value = String(result)
  previousInput.value = ''
  operator.value = null
  waitingForSecond.value = false
  updateDisplay()
}

function compute(a, b, op) {
  switch (op) {
    case '+': return a + b
    case '-': return a - b
    case '×': return a * b
    case '÷': return b !== 0 ? a / b : 'Error'
    default: return b
  }
}

function updateDisplay() {
  display.value = currentInput.value
}
</script>

<style scoped>
.calculator {
  width: 320px;
  background: #16213e;
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 10px 20px rgba(0,0,0,0.5);
}

.display {
  background: #0f3460;
  color: #eaeaea;
  font-size: 2.5rem;
  text-align: right;
  padding: 20px 15px;
  border-radius: 12px;
  margin-bottom: 20px;
  min-height: 60px;
  word-wrap: break-word;
}

.buttons {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
}

.btn {
  padding: 18px;
  font-size: 1.4rem;
  border: none;
  border-radius: 12px;
  cursor: pointer;
  background: #1a1a2e;
  color: #eaeaea;
  transition: background 0.2s;
}

.btn:hover {
  background: #0f3460;
}

.btn.function {
  background: #533483;
  color: white;
}

.btn.function:hover {
  background: #6a4c93;
}

.btn.operator {
  background: #e94560;
  color: white;
}

.btn.operator:hover {
  background: #ff6b81;
}

.btn.equals {
  background: #0f3460;
  color: white;
  grid-column: span 2;
}

.btn.equals:hover {
  background: #16213e;
}
</style>