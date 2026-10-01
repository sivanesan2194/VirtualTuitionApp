package com.virtualtuition.app.models;

import com.google.firebase.Timestamp;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Classroom implements Serializable {
    private String classroomId;
    private String className;
    private String subject;
    private String teacherId;
    private String teacherName;
    private String classCode; // 6-character code
    private Timestamp createdAt;
    private List<String> studentIds;

    public Classroom() {
        this.studentIds = new ArrayList<>();
        this.createdAt = Timestamp.now();
    }

    public Classroom(String classroomId, String className, String subject, String teacherId, String teacherName, String classCode) {
        this.classroomId = classroomId;
        this.className = className;
        this.subject = subject;
        this.teacherId = teacherId;
        this.teacherName = teacherName;
        this.classCode = classCode;
        this.createdAt = Timestamp.now();
        this.studentIds = new ArrayList<>();
    }

    public String getClassroomId() {
        return classroomId;
    }

    public void setClassroomId(String classroomId) {
        this.classroomId = classroomId;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(String teacherId) {
        this.teacherId = teacherId;
    }

    public String getTeacherName() {
        return teacherName;
    }

    public void setTeacherName(String teacherName) {
        this.teacherName = teacherName;
    }

    public String getClassCode() {
        return classCode;
    }

    public void setClassCode(String classCode) {
        this.classCode = classCode;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public List<String> getStudentIds() {
        if (studentIds == null) {
            studentIds = new ArrayList<>();
        }
        return studentIds;
    }

    public void setStudentIds(List<String> studentIds) {
        this.studentIds = studentIds;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("classroomId", classroomId);
        map.put("className", className);
        map.put("subject", subject);
        map.put("teacherId", teacherId);
        map.put("teacherName", teacherName);
        map.put("classCode", classCode);
        map.put("createdAt", createdAt);
        map.put("studentIds", studentIds);
        return map;
    }
}
