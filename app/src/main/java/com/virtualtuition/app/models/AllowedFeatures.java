package com.virtualtuition.app.models;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class AllowedFeatures implements Serializable {
    private boolean liveVideo;
    private boolean whiteboard;
    private boolean materials;

    public AllowedFeatures() {
        // Default: full access
        this.liveVideo = true;
        this.whiteboard = true;
        this.materials = true;
    }

    public AllowedFeatures(boolean liveVideo, boolean whiteboard, boolean materials) {
        this.liveVideo = liveVideo;
        this.whiteboard = whiteboard;
        this.materials = materials;
    }

    public boolean isLiveVideo() {
        return liveVideo;
    }

    public void setLiveVideo(boolean liveVideo) {
        this.liveVideo = liveVideo;
    }

    public boolean isWhiteboard() {
        return whiteboard;
    }

    public void setWhiteboard(boolean whiteboard) {
        this.whiteboard = whiteboard;
    }

    public boolean isMaterials() {
        return materials;
    }

    public void setMaterials(boolean materials) {
        this.materials = materials;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("liveVideo", liveVideo);
        map.put("whiteboard", whiteboard);
        map.put("materials", materials);
        return map;
    }
}
