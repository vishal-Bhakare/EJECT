package com.edtech.coe.batch.entity;

public enum EnrollmentStatus {

    ENROLLED,    // The learner is part of the batch
    COMPLETED,   // The learner finished the batch
    DROPPED      // The learner left, or was removed. This is the "delete" of an enrollment
}