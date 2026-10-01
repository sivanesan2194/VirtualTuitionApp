package com.virtualtuition.app.models;

import com.google.firebase.Timestamp;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class MaterialItem implements Serializable {
    private String materialId;
    private String classroomId;
    private String title;
    private String description;
    private String fileUrl;
    private String fileName;
    private String fileType; // "pdf", "image", "doc", "other"
    private String uploadedBy;
    private String uploadedByName;
    private Timestamp timestamp;

    public MaterialItem() {
        this.timestamp = Timestamp.now();
    }

    public MaterialItem(String materialId, String classroomId, String title, String description,
                        String fileUrl, String fileName, String fileType,
                        String uploadedBy, String uploadedByName) {
        this.materialId = materialId;
        this.classroomId = classroomId;
        this.title = title;
        this.description = description;
        this.fileUrl = fileUrl;
        this.fileName = fileName;
        this.fileType = fileType;
        this.uploadedBy = uploadedBy;
        this.uploadedByName = uploadedByName;
        this.timestamp = Timestamp.now();
    }

    public String getMaterialId() {
        return materialId;
    }

    public void setMaterialId(String materialId) {
        this.materialId = materialId;
    }

    public String getClassroomId() {
        return classroomId;
    }

    public void setClassroomId(String classroomId) {
        this.classroomId = classroomId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public String getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(String uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public String getUploadedByName() {
        return uploadedByName;
    }

    public void setUploadedByName(String uploadedByName) {
        this.uploadedByName = uploadedByName;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("materialId", materialId);
        map.put("classroomId", classroomId);
        map.put("title", title);
        map.put("description", description);
        map.put("fileUrl", fileUrl);
        map.put("fileName", fileName);
        map.put("fileType", fileType);
        map.put("uploadedBy", uploadedBy);
        map.put("uploadedByName", uploadedByName);
        map.put("timestamp", timestamp);
        return map;
    }
}
