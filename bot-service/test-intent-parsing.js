/**
 * Quick manual test script for intent parsing fixes
 * Run with: node dist/test-intent-parsing.js
 *
 * To use:
 * 1. cd bot-service
 * 2. npm run build
 * 3. node test-intent-parsing.js
 */

const { parseIntentRules } = require('./dist/intent');

// Test cases - expecting balance checks, NOT payments
const balanceTests = [
  "what is my balance?",
  "check my balance",
  "check balance for ACC-10001",
  "how much money do I have?",
  "my balance in USD",
  "balance in RS",
  "what's my balance in EUR",
  "show me my account balance"
];

// Test cases - expecting payment creation
const paymentTests = [
  "send 100 EUR to ACC-123",
  "transfer 50 RS from ACC-10001 to ACC-10002",
  "make a payment of 200 USD to ACC-456",
  "create payment 1000 INR to ACC-789",
  "pay 75 euros to ACC-999"
];

// Test cases - expecting unknown (not payments)
const ambiguousTests = [
  "send me my balance",
  "can I send money?",
  "check if I can send 100"
];

console.log("=== BALANCE CHECK TESTS ===");
console.log("Expected: type='check_balance', readOnly=true\n");

balanceTests.forEach(text => {
  const result = parseIntentRules(text, { accountNumber: 'ACC-USER-123', role: 'member' });
  const status = result.type === 'check_balance' ? '✓ PASS' : '✗ FAIL';
  console.log(`${status}: "${text}"`);
  console.log(`   Result: type=${result.type}, confidence=${result.confidence}, readOnly=${result.readOnly}`);
  if (result.payload?.currency) {
    console.log(`   Currency: ${result.payload.currency}`);
  }
  console.log();
});

console.log("\n=== PAYMENT CREATION TESTS ===");
console.log("Expected: type='create_payment', readOnly=undefined/false\n");

paymentTests.forEach(text => {
  const result = parseIntentRules(text, { accountNumber: 'ACC-USER-123', role: 'member' });
  const status = result.type === 'create_payment' ? '✓ PASS' : '✗ FAIL';
  console.log(`${status}: "${text}"`);
  console.log(`   Result: type=${result.type}, confidence=${result.confidence}`);
  if (result.payload) {
    console.log(`   Amount: ${result.payload.amount}, Currency: ${result.payload.currency}`);
    console.log(`   From: ${result.payload.sourceAccount}, To: ${result.payload.destinationAccount}`);
  }
  console.log();
});

console.log("\n=== AMBIGUOUS TESTS (should NOT be payments) ===");
console.log("Expected: type='unknown' or 'check_balance', NOT 'create_payment'\n");

ambiguousTests.forEach(text => {
  const result = parseIntentRules(text, { accountNumber: 'ACC-USER-123', role: 'member' });
  const status = result.type !== 'create_payment' ? '✓ PASS' : '✗ FAIL';
  console.log(`${status}: "${text}"`);
  console.log(`   Result: type=${result.type}, confidence=${result.confidence}`);
  console.log();
});

console.log("\n=== SUMMARY ===");
console.log("Review results above. All balance checks should parse as 'check_balance'.");
console.log("All payment tests should parse as 'create_payment'.");
console.log("Ambiguous tests should NOT parse as 'create_payment'.");

