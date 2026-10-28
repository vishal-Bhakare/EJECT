package com.edtech.coe.scheduling.entity;

public enum SessionStatus {

    SCHEDULED,   // Planned. It holds the mentor, lab and batch for its time slot
    COMPLETED,   // The class took place
    CANCELLED    // The class will not happen. This is the "delete" of a session
}