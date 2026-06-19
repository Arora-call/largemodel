<template>
  <div class="calculator">
    <div class="display">
      <div class="expression">{{ expression || '0' }}</div>
    </div>
    <div class="buttons">
      <button class="btn function" @click="clear">C</button>
      <button class="btn function" @click="toggleSign">±</button>
      <button class="btn function" @click="percent">%</button>
      <button class="btn operator" @click="setOperator('/')">÷</button>

      <button class="btn number" @click="appendNumber('7')">7</button>
      <button class="btn number" @click="appendNumber('8')">8</button>
      <button class="btn number" @click="appendNumber('9')">9</button>
      <button class="btn operator" @click="setOperator('*')">×</button>

      <button class="btn number" @click="appendNumber('4')">4</button>
      <button class="btn number" @click="appendNumber('5')">5</button>
      <button class="btn number" @click="appendNumber('6')">6</button>
      <button class="btn operator" @click="setOperator('-')">−</button>

      <button class="btn number" @click="appendNumber('1')">1</button>
      <button class="btn number" @click="appendNumber('2')">2</button>
      <button class="btn number" @click="appendNumber('3')">3</button>
      <button class="btn operator" @click="setOperator('+')">+</button>

      <button class="btn number zero" @click="appendNumber('0')">0</button>
      <button class="btn number" @click="appendDecimal">.</button>
      <button class="btn equals" @click="calculate">=</button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const currentInput = ref('0')
const previousInput = ref('')
const operator = ref(null)
const shouldReset = ref(false)

const expression = computed(() => {
  if (!operator.value) return currentInput.value
  return `${previousInput.value} ${operator.value} ${currentInput.value}`
})

function appendNumber(num) {
  if (shouldReset.value) {
    currentInput.value = num
    shouldReset.value = false
  } else {
    if (currentInput.value === '0' && num !== '.') {
      currentInput.value = num
    } else {
      currentInput.value += num
    }
  }
}

function appendDecimal() {
  if (shouldReset.value) {
    currentInput.value = '0.'
    shouldReset.value = false
    return
  }
  if (!currentInput.value.includes('.')) {
    currentInput.value += '.'
  }
}

function setOperator(op) {
  if (operator.value) {
    calculate()
  }
  previousInput.value = currentInput.value
  operator.value = op
  shouldReset.value = true
}

function calculate() {
  if (!operator.value) return
  const prev = parseFloat(previousInput.value)
  const curr = parseFloat(currentInput.value)
  let result
  switch (operator.value) {
    case '+':
      result = prev + curr
      break
    case '-':
      result = prev - curr
      break
    case '*':
      result = prev * curr
      break
    case '/':
      result = curr !== 0 ? prev / curr : 'Error'
      break
    default:
      return
  }
  currentInput.value = String(result)
  previousInput.value = ''
  operator.value = null
  shouldReset.value = true
}

function clear() {
  currentInput.value = '0'
  previousInput.value = ''
  operator.value = null
  shouldReset.value = false
}

function toggleSign() {
  if (currentInput.value !== '0') {
    currentInput.value = currentInput.value.startsWith('-')
      ? currentInput.value.slice(1)
      : '-' + currentInput.value
  }
}

function percent() {
  const num = parseFloat(currentInput.value)
  currentInput.value = String(num / 100)
}
</script>

<style scoped>
.calculator {
  width: 300px;
  margin: 50px auto;
  background: #222;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 10px 30px rgba(0,0,0,0.3);
}

.display {
  background: #111;
  color: #fff;
  font-size: 2.5rem;
  text-align: right;
  padding: 20px 15px;
  border-radius: 8px;
  margin-bottom: 20px;
  min-height: 60px;
  overflow: hidden;
}

.expression {
  word-break: break-all;
}

.buttons {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
}

.btn {
  font-size: 1.5rem;
  padding: 18px;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.2s;
}

.btn:active {
  transform: scale(0.95);
}

.number {
  background: #444;
  color: #fff;
}

.number:hover {
  background: #555;
}

.operator {
  background: #f39c12;
  color: #fff;
}

.operator:hover {
  background: #e67e22;
}

.function {
  background: #a0a0a0;
  color: #000;
}

.function:hover {
  background: #b8b8b8;
}

.equals {
  background: #3498db;
  color: #fff;
  grid-column: span 2;
}

.equals:hover {
  background: #2980b9;
}

.zero {
  grid-column: span 1;
}
</style>