package com.edtech.coe.batch.entity;

public enum BatchStatus {

    PLANNED,     // Created, not started yet. Learners can be enrolled
    ONGOING,     // Training is running
    COMPLETED,   // Finished normally
    CANCELLED    // Stopped or never started. This is the "delete" of a batch
}