package com.virtualtuition.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.firebase.Timestamp;
import com.virtualtuition.app.models.UserProfile;
import com.virtualtuition.app.payment.AccessControlManager;
import org.junit.Test;
import java.util.Calendar;

public class AccessControlTest {

    @Test
    public void testActiveSubscription_FullAccess() {
        UserProfile student = new UserProfile("s1", "Student", "s@test.com", UserProfile.ROLE_STUDENT);
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, 10); // Due in 10 days
        student.setNextDueDate(new Timestamp(cal.getTime()));

        AccessControlManager.evaluateUserFeatures(student);

        assertEquals(0, student.getOverdueDays());
        assertEquals(UserProfile.STATUS_ACTIVE, student.getSubscriptionStatus());
        assertTrue(student.getAllowedFeatures().isLiveVideo());
        assertTrue(student.getAllowedFeatures().isWhiteboard());
        assertTrue(student.getAllowedFeatures().isMaterials());
        assertFalse(AccessControlManager.isSuspended(student));
    }

    @Test
    public void testGracePeriod_Days1To3_FullAccessWithGraceStatus() {
        UserProfile student = new UserProfile("s2", "Student", "s@test.com", UserProfile.ROLE_STUDENT);
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -2); // Overdue by 2 days
        student.setNextDueDate(new Timestamp(cal.getTime()));

        AccessControlManager.evaluateUserFeatures(student);

        assertEquals(2, student.getOverdueDays());
        assertEquals(UserProfile.STATUS_GRACE_PERIOD, student.getSubscriptionStatus());
        assertTrue(student.getAllowedFeatures().isLiveVideo());
        assertTrue(student.getAllowedFeatures().isWhiteboard());
        assertTrue(student.getAllowedFeatures().isMaterials());
        assertFalse(AccessControlManager.isSuspended(student));
    }

    @Test
    public void testPhase1Lock_Day4_LocksLiveVideoOnly() {
        UserProfile student = new UserProfile("s3", "Student", "s@test.com", UserProfile.ROLE_STUDENT);
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -4); // Overdue by 4 days
        student.setNextDueDate(new Timestamp(cal.getTime()));

        AccessControlManager.evaluateUserFeatures(student);

        assertEquals(4, student.getOverdueDays());
        assertEquals(UserProfile.STATUS_OVERDUE_PHASE1, student.getSubscriptionStatus());
        assertFalse(student.getAllowedFeatures().isLiveVideo());
        assertTrue(student.getAllowedFeatures().isWhiteboard());
        assertTrue(student.getAllowedFeatures().isMaterials());
        assertFalse(AccessControlManager.isSuspended(student));
    }

    @Test
    public void testPhase2Lock_Day7_LocksWhiteboardAndMaterials() {
        UserProfile student = new UserProfile("s4", "Student", "s@test.com", UserProfile.ROLE_STUDENT);
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -7); // Overdue by 7 days
        student.setNextDueDate(new Timestamp(cal.getTime()));

        AccessControlManager.evaluateUserFeatures(student);

        assertEquals(7, student.getOverdueDays());
        assertEquals(UserProfile.STATUS_OVERDUE_PHASE2, student.getSubscriptionStatus());
        assertFalse(student.getAllowedFeatures().isLiveVideo());
        assertFalse(student.getAllowedFeatures().isWhiteboard());
        assertFalse(student.getAllowedFeatures().isMaterials());
        assertFalse(AccessControlManager.isSuspended(student));
    }

    @Test
    public void testDay15_FullSuspension() {
        UserProfile student = new UserProfile("s5", "Student", "s@test.com", UserProfile.ROLE_STUDENT);
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -15); // Overdue by 15 days
        student.setNextDueDate(new Timestamp(cal.getTime()));

        AccessControlManager.evaluateUserFeatures(student);

        assertTrue(student.getOverdueDays() >= 15);
        assertEquals(UserProfile.STATUS_SUSPENDED, student.getSubscriptionStatus());
        assertFalse(student.getAllowedFeatures().isLiveVideo());
        assertFalse(student.getAllowedFeatures().isWhiteboard());
        assertFalse(student.getAllowedFeatures().isMaterials());
        assertTrue(AccessControlManager.isSuspended(student));
    }

    @Test
    public void testTeacherExemption_AlwaysFullAccess() {
        UserProfile teacher = new UserProfile("t1", "Teacher", "t@test.com", UserProfile.ROLE_TEACHER);
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -30); // Overdue by 30 days
        teacher.setNextDueDate(new Timestamp(cal.getTime()));

        AccessControlManager.evaluateUserFeatures(teacher);

        assertEquals(0, teacher.getOverdueDays());
        assertEquals(UserProfile.STATUS_ACTIVE, teacher.getSubscriptionStatus());
        assertTrue(teacher.getAllowedFeatures().isLiveVideo());
        assertTrue(teacher.getAllowedFeatures().isWhiteboard());
        assertTrue(teacher.getAllowedFeatures().isMaterials());
        assertFalse(AccessControlManager.isSuspended(teacher));
    }
}
