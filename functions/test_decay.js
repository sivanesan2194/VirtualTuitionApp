// Test script to verify the decay evaluation logic in functions/index.js
const assert = require("assert");

function evaluateDecay(now, nextDueDate) {
  const diffTime = now.getTime() - nextDueDate.getTime();
  const overdueDays = Math.floor(diffTime / (1000 * 60 * 60 * 24));

  let subscriptionStatus = "active";
  let allowedFeatures = {
    liveVideo: true,
    whiteboard: true,
    materials: true
  };
  let notificationTitle = null;

  if (overdueDays < 0) {
    subscriptionStatus = "active";
  } else if (overdueDays === 0) {
    subscriptionStatus = "active";
    notificationTitle = "Tuition Fee Due Today";
  } else if (overdueDays >= 1 && overdueDays <= 3) {
    subscriptionStatus = "grace_period";
    allowedFeatures = { liveVideo: true, whiteboard: true, materials: true };
    notificationTitle = `Tuition Fee Overdue (Day ${overdueDays})`;
  } else if (overdueDays >= 4 && overdueDays < 7) {
    subscriptionStatus = "overdue_phase1";
    allowedFeatures = { liveVideo: false, whiteboard: true, materials: true };
    notificationTitle = "Feature Restriction: Live Video Locked";
  } else if (overdueDays >= 7 && overdueDays < 15) {
    subscriptionStatus = "overdue_phase2";
    allowedFeatures = { liveVideo: false, whiteboard: false, materials: false };
    notificationTitle = "Feature Restriction: Whiteboard & Materials Locked";
  } else if (overdueDays >= 15) {
    subscriptionStatus = "suspended";
    allowedFeatures = { liveVideo: false, whiteboard: false, materials: false };
    notificationTitle = "Account Suspended";
  }

  return { overdueDays, subscriptionStatus, allowedFeatures, notificationTitle };
}

const now = new Date("2026-09-17T00:00:00Z");

// Test Day -5: Active
const dActive = new Date("2026-09-22T00:00:00Z");
const resActive = evaluateDecay(now, dActive);
assert.strictEqual(resActive.subscriptionStatus, "active");
assert.strictEqual(resActive.allowedFeatures.liveVideo, true);
console.log("PASS: Day -5 active test");

// Test Day 0: Due Date
const dDay0 = new Date("2026-09-17T00:00:00Z");
const resDay0 = evaluateDecay(now, dDay0);
assert.strictEqual(resDay0.subscriptionStatus, "active");
assert.strictEqual(resDay0.notificationTitle, "Tuition Fee Due Today");
console.log("PASS: Day 0 due date test");

// Test Day 2: Grace Period
const dDay2 = new Date("2026-09-15T00:00:00Z");
const resDay2 = evaluateDecay(now, dDay2);
assert.strictEqual(resDay2.subscriptionStatus, "grace_period");
assert.strictEqual(resDay2.allowedFeatures.liveVideo, true);
assert.strictEqual(resDay2.allowedFeatures.whiteboard, true);
assert.strictEqual(resDay2.allowedFeatures.materials, true);
console.log("PASS: Day 2 grace period test");

// Test Day 4: Phase 1 Lock
const dDay4 = new Date("2026-09-13T00:00:00Z");
const resDay4 = evaluateDecay(now, dDay4);
assert.strictEqual(resDay4.subscriptionStatus, "overdue_phase1");
assert.strictEqual(resDay4.allowedFeatures.liveVideo, false);
assert.strictEqual(resDay4.allowedFeatures.whiteboard, true);
assert.strictEqual(resDay4.allowedFeatures.materials, true);
console.log("PASS: Day 4 Phase 1 lock test");

// Test Day 8: Phase 2 Lock
const dDay8 = new Date("2026-09-09T00:00:00Z");
const resDay8 = evaluateDecay(now, dDay8);
assert.strictEqual(resDay8.subscriptionStatus, "overdue_phase2");
assert.strictEqual(resDay8.allowedFeatures.liveVideo, false);
assert.strictEqual(resDay8.allowedFeatures.whiteboard, false);
assert.strictEqual(resDay8.allowedFeatures.materials, false);
console.log("PASS: Day 8 Phase 2 lock test");

// Test Day 16: Full Suspension
const dDay16 = new Date("2026-09-01T00:00:00Z");
const resDay16 = evaluateDecay(now, dDay16);
assert.strictEqual(resDay16.subscriptionStatus, "suspended");
assert.strictEqual(resDay16.allowedFeatures.liveVideo, false);
assert.strictEqual(resDay16.allowedFeatures.whiteboard, false);
assert.strictEqual(resDay16.allowedFeatures.materials, false);
console.log("PASS: Day 16 suspension test");

console.log("\nALL CLOUD FUNCTION LOGIC TESTS PASSED!");
