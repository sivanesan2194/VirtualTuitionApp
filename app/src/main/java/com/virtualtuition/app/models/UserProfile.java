package com.virtualtuition.app.models;

import com.google.firebase.Timestamp;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class UserProfile implements Serializable {
    public static final String ROLE_TEACHER = "teacher";
    public static final String ROLE_STUDENT = "student";

    public static final String STATUS_ACTIVE = "active";
    public static final String STATUS_GRACE_PERIOD = "grace_period";
    public static final String STATUS_OVERDUE_PHASE1 = "overdue_phase1";
    public static final String STATUS_OVERDUE_PHASE2 = "overdue_phase2";
    public static final String STATUS_SUSPENDED = "suspended";

    private String uid;
    private String name;
    private String email;
    private String role; // "teacher" or "student"
    private Timestamp createdAt;
    private Timestamp lastPaymentDate;
    private Timestamp nextDueDate;
    private int overdueDays;
    private String subscriptionStatus;
    private AllowedFeatures allowedFeatures;
    private String fcmToken;

    public UserProfile() {
        this.allowedFeatures = new AllowedFeatures();
        this.subscriptionStatus = STATUS_ACTIVE;
        this.overdueDays = 0;
    }

    public UserProfile(String uid, String name, String email, String role) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.role = role;
        this.createdAt = Timestamp.now();
        this.allowedFeatures = new AllowedFeatures();
        this.subscriptionStatus = STATUS_ACTIVE;
        this.overdueDays = 0;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean isTeacher() {
        return ROLE_TEACHER.equalsIgnoreCase(this.role);
    }

    public boolean isStudent() {
        return ROLE_STUDENT.equalsIgnoreCase(this.role);
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getLastPaymentDate() {
        return lastPaymentDate;
    }

    public void setLastPaymentDate(Timestamp lastPaymentDate) {
        this.lastPaymentDate = lastPaymentDate;
    }

    public Timestamp getNextDueDate() {
        return nextDueDate;
    }

    public void setNextDueDate(Timestamp nextDueDate) {
        this.nextDueDate = nextDueDate;
    }

    public int getOverdueDays() {
        return overdueDays;
    }

    public void setOverdueDays(int overdueDays) {
        this.overdueDays = overdueDays;
    }

    public String getSubscriptionStatus() {
        return subscriptionStatus != null ? subscriptionStatus : STATUS_ACTIVE;
    }

    public void setSubscriptionStatus(String subscriptionStatus) {
        this.subscriptionStatus = subscriptionStatus;
    }

    public AllowedFeatures getAllowedFeatures() {
        if (allowedFeatures == null) {
            allowedFeatures = new AllowedFeatures();
        }
        return allowedFeatures;
    }

    public void setAllowedFeatures(AllowedFeatures allowedFeatures) {
        this.allowedFeatures = allowedFeatures;
    }

    public String getFcmToken() {
        return fcmToken;
    }

    public void setFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("uid", uid);
        map.put("name", name);
        map.put("email", email);
        map.put("role", role);
        map.put("createdAt", createdAt != null ? createdAt : Timestamp.now());
        map.put("lastPaymentDate", lastPaymentDate);
        map.put("nextDueDate", nextDueDate);
        map.put("overdueDays", overdueDays);
        map.put("subscriptionStatus", subscriptionStatus);
        map.put("allowedFeatures", allowedFeatures != null ? allowedFeatures.toMap() : new AllowedFeatures().toMap());
        map.put("fcmToken", fcmToken);
        return map;
    }
}
