<template>
  <div class="calculator">
    <div class="display">
      <div class="expression">{{ expression }}</div>
      <div class="result">{{ result }}</div>
    </div>
    <div class="buttons">
      <button v-for="btn in buttons" :key="btn" :class="getClass(btn)" @click="handleClick(btn)">{{ btn }}</button>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const expression = ref('')
const result = ref('0')
const current = ref('0')
const operator = ref(null)
const waitingForOperand = ref(false)

const buttons = [
  'C', '±', '%', '÷',
  '7', '8', '9', '×',
  '4', '5', '6', '-',
  '1', '2', '3', '+',
  '0', '.', '='
]

function getClass(btn) {
  if (btn === '=') return 'button-equal'
  if (['+', '-', '×', '÷'].includes(btn)) return 'button-operator'
  if (btn === 'C') return 'button-clear'
  return ''
}

function handleClick(btn) {
  if (btn === 'C') {
    clear()
  } else if (btn === '±') {
    toggleSign()
  } else if (btn === '%') {
    percent()
  } else if (btn === '=') {
    calculate()
  } else if (btn === '+') {
    setOperator('+')
  } else if (btn === '-') {
    setOperator('-')
  } else if (btn === '×') {
    setOperator('*')
  } else if (btn === '÷') {
    setOperator('/')
  } else if (btn === '.') {
    inputDot()
  } else {
    inputDigit(btn)
  }
}

function clear() {
  expression.value = ''
  result.value = '0'
  current.value = '0'
  operator.value = null
  waitingForOperand.value = false
}

function toggleSign() {
  current.value = (parseFloat(current.value) * -1).toString()
  expression.value = expression.value.startsWith('-') ? expression.value.slice(1) : '-' + expression.value
}

function percent() {
  current.value = (parseFloat(current.value) / 100).toString()
  result.value = current.value
}

function inputDigit(digit) {
  if (waitingForOperand.value) {
    current.value = digit
    waitingForOperand.value = false
  } else {
    current.value = current.value === '0' ? digit : current.value + digit
  }
  result.value = current.value
}

function inputDot() {
  if (!current.value.includes('.')) {
    current.value += '.'
    result.value = current.value
  }
}

function setOperator(op) {
  if (operator.value && !waitingForOperand.value) {
    calculate(false)
  }
  expression.value = current.value + ' ' + operatorSymbol(op) + ' '
  operator.value = op
  waitingForOperand.value = true
}

function operatorSymbol(op) {
  const map = { '+': '+', '-': '-', '*': '×', '/': '÷' }
  return map[op] || op
}

function calculate(updateExpression = true) {
  if (!operator.value) return
  const prev = parseFloat(expression.value.split(' ')[0])
  const next = parseFloat(current.value)
  let computed
  switch (operator.value) {
    case '+': computed = prev + next; break
    case '-': computed = prev - next; break
    case '*': computed = prev * next; break
    case '/': computed = next !== 0 ? prev / next : 'Error'; break
  }
  if (computed === 'Error') {
    result.value = '错误'
    current.value = '0'
    operator.value = null
    expression.value = ''
    return
  }
  current.value = computed.toString()
  result.value = current.value
  if (updateExpression) {
    expression.value = prev + ' ' + operatorSymbol(operator.value) + ' ' + next + ' ='
  }
  operator.value = null
  waitingForOperand.value = true
}
</script>

<style scoped>
.calculator {
  width: 320px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.1);
  padding: 20px;
}
.display {
  background: #222;
  color: #fff;
  border-radius: 8px;
  padding: 15px;
  margin-bottom: 16px;
  text-align: right;
  min-height: 70px;
}
.expression {
  font-size: 14px;
  color: #aaa;
  min-height: 20px;
}
.result {
  font-size: 32px;
  font-weight: bold;
  word-break: break-all;
}
.buttons {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
}
button {
  padding: 18px 0;
  font-size: 20px;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  background: #f0f0f0;
  transition: background 0.2s;
}
button:hover {
  background: #ddd;
}
button:active {
  background: #bbb;
}
.button-operator {
  background: #fe9241;
  color: #fff;
}
.button-operator:hover {
  background: #e07b30;
}
.button-equal {
  background: #4caf50;
  color: #fff;
  grid-column: span 1;
}
.button-equal:hover {
  background: #43a047;
}
.button-clear {
  background: #f44336;
  color: #fff;
}
.button-clear:hover {
  background: #d32f2f;
}
.buttons button:last-child {
  /* 最后的 = 按钮可以不特殊处理，已用类控制 */
}
</style>