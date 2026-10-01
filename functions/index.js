/**
 * Virtual Tuition App - Firebase Cloud Functions
 * Automated Feature Decay System & FCM Notification Engine
 */

const { onSchedule } = require("firebase-functions/v2/scheduler");
const { onRequest } = require("firebase-functions/v2/https");
const admin = require("firebase-admin");

admin.initializeApp();
const db = admin.firestore();

/**
 * Daily midnight cron job: evaluates overdue tuition days and triggers FCM alerts.
 * Cron expression: 0 0 * * * (Runs daily at midnight UTC)
 */
exports.evaluateFeatureDecayDaily = onSchedule("0 0 * * *", async (event) => {
  console.log("Starting daily feature decay and subscription evaluation...");
  const now = new Date();
  
  try {
    const studentsSnapshot = await db.collection("users")
      .where("role", "==", "student")
      .get();

    console.log(`Found ${studentsSnapshot.size} student profiles to evaluate.`);

    const batch = db.batch();
    const notificationPromises = [];

    studentsSnapshot.forEach((doc) => {
      const data = doc.data();
      const nextDueDate = data.nextDueDate ? data.nextDueDate.toDate() : null;

      if (!nextDueDate) {
        return; // Skip if no due date set
      }

      // Calculate days overdue
      const diffTime = now.getTime() - nextDueDate.getTime();
      const overdueDays = Math.floor(diffTime / (1000 * 60 * 60 * 24));

      let subscriptionStatus = "active";
      let allowedFeatures = {
        liveVideo: true,
        whiteboard: true,
        materials: true
      };
      let notificationTitle = null;
      let notificationBody = null;

      if (overdueDays < 0) {
        // Not due yet
        subscriptionStatus = "active";
      } else if (overdueDays === 0) {
        // Day 0: Due Date
        subscriptionStatus = "active";
        notificationTitle = "Tuition Fee Due Today";
        notificationBody = "Your monthly tuition fee is due today. Please pay to keep full access active.";
      } else if (overdueDays >= 1 && overdueDays <= 3) {
        // Days 1-3: Grace Period
        subscriptionStatus = "grace_period";
        allowedFeatures = { liveVideo: true, whiteboard: true, materials: true };
        notificationTitle = `Tuition Fee Overdue (Day ${overdueDays})`;
        notificationBody = `You are in the grace period. Please pay your tuition fee to avoid feature restrictions.`;
      } else if (overdueDays >= 4 && overdueDays < 7) {
        // Day 4: Phase 1 Lock (Live video locked)
        subscriptionStatus = "overdue_phase1";
        allowedFeatures = { liveVideo: false, whiteboard: true, materials: true };
        notificationTitle = "Feature Restriction: Live Video Locked";
        notificationBody = "Access to Live Online Video Classes is now locked due to overdue fees. Pay now to unlock.";
      } else if (overdueDays >= 7 && overdueDays < 15) {
        // Day 7: Phase 2 Lock (Whiteboard & materials locked)
        subscriptionStatus = "overdue_phase2";
        allowedFeatures = { liveVideo: false, whiteboard: false, materials: false };
        notificationTitle = "Feature Restriction: Whiteboard & Materials Locked";
        notificationBody = "Access to Whiteboard and Material downloads is locked due to overdue fees. Pay now to restore access.";
      } else if (overdueDays >= 15) {
        // Day 15: Full Suspension
        subscriptionStatus = "suspended";
        allowedFeatures = { liveVideo: false, whiteboard: false, materials: false };
        notificationTitle = "Account Suspended";
        notificationBody = "Your student account is suspended due to unpaid tuition. Redirecting to payment screen.";
      }

      // Queue Firestore document update
      const userRef = db.collection("users").doc(doc.id);
      batch.update(userRef, {
        overdueDays: Math.max(0, overdueDays),
        subscriptionStatus: subscriptionStatus,
        allowedFeatures: allowedFeatures,
        lastEvaluatedAt: admin.firestore.FieldValue.serverTimestamp()
      });

      // Send FCM push notification if token exists
      if (data.fcmToken && notificationTitle && notificationBody) {
        const message = {
          token: data.fcmToken,
          notification: {
            title: notificationTitle,
            body: notificationBody
          },
          data: {
            type: "SUBSCRIPTION_STATUS",
            subscriptionStatus: subscriptionStatus,
            overdueDays: String(Math.max(0, overdueDays))
          }
        };

        notificationPromises.push(
          admin.messaging().send(message).catch((err) => {
            console.error(`Failed to send FCM notification to user ${doc.id}:`, err);
          })
        );
      }
    });

    await batch.commit();
    await Promise.all(notificationPromises);
    console.log("Evaluation completed successfully.");
  } catch (error) {
    console.error("Error evaluating subscriptions:", error);
  }
});

/**
 * HTTP endpoint for manual or testing evaluation of a student's access state.
 */
exports.evaluateStudentAccess = onRequest(async (req, res) => {
  const { studentId } = req.query;
  if (!studentId) {
    return res.status(400).json({ error: "Missing studentId parameter" });
  }

  try {
    const userDoc = await db.collection("users").doc(studentId).get();
    if (!userDoc.exists) {
      return res.status(404).json({ error: "Student not found" });
    }

    const data = userDoc.data();
    const nextDueDate = data.nextDueDate ? data.nextDueDate.toDate() : new Date();
    const now = new Date();
    const diffTime = now.getTime() - nextDueDate.getTime();
    const overdueDays = Math.floor(diffTime / (1000 * 60 * 60 * 24));

    let subscriptionStatus = "active";
    let allowedFeatures = { liveVideo: true, whiteboard: true, materials: true };

    if (overdueDays < 0) {
      subscriptionStatus = "active";
    } else if (overdueDays === 0) {
      subscriptionStatus = "active";
    } else if (overdueDays <= 3) {
      subscriptionStatus = "grace_period";
    } else if (overdueDays < 7) {
      subscriptionStatus = "overdue_phase1";
      allowedFeatures.liveVideo = false;
    } else if (overdueDays < 15) {
      subscriptionStatus = "overdue_phase2";
      allowedFeatures.liveVideo = false;
      allowedFeatures.whiteboard = false;
      allowedFeatures.materials = false;
    } else {
      subscriptionStatus = "suspended";
      allowedFeatures.liveVideo = false;
      allowedFeatures.whiteboard = false;
      allowedFeatures.materials = false;
    }

    await userDoc.ref.update({
      overdueDays: Math.max(0, overdueDays),
      subscriptionStatus: subscriptionStatus,
      allowedFeatures: allowedFeatures,
      lastEvaluatedAt: admin.firestore.FieldValue.serverTimestamp()
    });

    return res.status(200).json({
      studentId,
      overdueDays: Math.max(0, overdueDays),
      subscriptionStatus,
      allowedFeatures
    });
  } catch (err) {
    console.error("Error in evaluateStudentAccess:", err);
    return res.status(500).json({ error: err.message });
  }
});
