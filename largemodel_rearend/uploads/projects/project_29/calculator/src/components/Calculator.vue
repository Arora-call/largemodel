<template>
  <div class="calculator">
    <div class="display">{{ display }}</div>
    <div class="buttons">
      <button v-for="btn in buttons" :key="btn" @click="handleInput(btn)" :class="getClass(btn)">
        {{ btn }}
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const display = ref('0')
const current = ref('')
const operand1 = ref(null)
const operator = ref('')
const waitingForOperand2 = ref(false)

const buttons = [
  '7', '8', '9', '/',
  '4', '5', '6', '*',
  '1', '2', '3', '-',
  '0', '.', '=', '+',
  'C', '←', '%', '±'
]

function getClass(btn) {
  if (btn === '=') return 'btn-equal'
  if (['+', '-', '*', '/', '%'].includes(btn)) return 'btn-operator'
  if (btn === 'C' || btn === '←') return 'btn-clear'
  if (btn === '±') return 'btn-sign'
  return 'btn-number'
}

function handleInput(value) {
  if (value === 'C') {
    clearAll()
  } else if (value === '←') {
    backspace()
  } else if (value === '±') {
    toggleSign()
  } else if (value === '%') {
    percent()
  } else if (['+', '-', '*', '/'].includes(value)) {
    handleOperator(value)
  } else if (value === '=') {
    calculate()
  } else if (value === '.') {
    inputDecimal()
  } else {
    inputDigit(value)
  }
}

function inputDigit(digit) {
  if (waitingForOperand2.value) {
    current.value = digit
    waitingForOperand2.value = false
  } else {
    current.value = current.value === '0' ? digit : current.value + digit
  }
  updateDisplay()
}

function inputDecimal() {
  if (waitingForOperand2.value) {
    current.value = '0.'
    waitingForOperand2.value = false
    return
  }
  if (!current.value.includes('.')) {
    current.value += '.'
  }
  updateDisplay()
}

function handleOperator(op) {
  const inputValue = parseFloat(current.value)
  if (operator.value && !waitingForOperand2.value) {
    calculate()
  }
  operand1.value = inputValue
  operator.value = op
  waitingForOperand2.value = true
}

function calculate() {
  const inputValue = parseFloat(current.value)
  const op1 = operand1.value
  if (isNaN(op1) || isNaN(inputValue)) return
  let result
  switch (operator.value) {
    case '+': result = op1 + inputValue; break
    case '-': result = op1 - inputValue; break
    case '*': result = op1 * inputValue; break
    case '/': result = inputValue !== 0 ? op1 / inputValue : 'Infinity'; break
    default: return
  }
  current.value = String(result)
  operand1.value = null
  operator.value = ''
  waitingForOperand2.value = false
  updateDisplay()
}

function clearAll() {
  current.value = '0'
  operand1.value = null
  operator.value = ''
  waitingForOperand2.value = false
  updateDisplay()
}

function backspace() {
  if (current.value.length > 1) {
    current.value = current.value.slice(0, -1)
  } else {
    current.value = '0'
  }
  updateDisplay()
}

function toggleSign() {
  const val = parseFloat(current.value)
  if (!isNaN(val)) {
    current.value = String(-val)
    updateDisplay()
  }
}

function percent() {
  const val = parseFloat(current.value)
  if (!isNaN(val)) {
    current.value = String(val / 100)
    updateDisplay()
  }
}

function updateDisplay() {
  display.value = current.value || '0'
}
</script>

<style scoped>
.calculator {
  width: 320px;
  background: #16213e;
  border-radius: 20px;
  padding: 20px;
  box-shadow: 0 10px 30px rgba(0,0,0,0.5);
}

.display {
  background: #0f3460;
  color: #fff;
  font-size: 2.5rem;
  text-align: right;
  padding: 20px 15px;
  border-radius: 12px;
  margin-bottom: 20px;
  overflow: hidden;
  min-height: 60px;
  word-break: break-all;
}

.buttons {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

button {
  padding: 18px;
  font-size: 1.3rem;
  border: none;
  border-radius: 12px;
  cursor: pointer;
  background: #1a1a40;
  color: #eee;
  transition: all 0.15s;
}

button:active {
  transform: scale(0.92);
}

.btn-number {
  background: #2d2d5e;
}
.btn-number:hover {
  background: #3d3d7e;
}

.btn-operator {
  background: #e94560;
  color: #fff;
}
.btn-operator:hover {
  background: #d63851;
}

.btn-equal {
  background: #0f3460;
  color: #e94560;
  font-weight: bold;
}
.btn-equal:hover {
  background: #1a4a7a;
}

.btn-clear {
  background: #533483;
  color: #fff;
}
.btn-clear:hover {
  background: #6b44a8;
}

.btn-sign {
  background: #2d2d5e;
}
.btn-sign:hover {
  background: #3d3d7e;
}
</style>