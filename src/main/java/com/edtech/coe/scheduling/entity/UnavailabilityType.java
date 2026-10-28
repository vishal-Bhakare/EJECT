package com.edtech.coe.scheduling.entity;

public enum UnavailabilityType {

    LEAVE,          // Planned leave
    SICK,           // Sick leave
    OFFICIAL_DUTY,  // Away on other work for the organisation, such as a workshop elsewhere
    OTHER           // Anything else. The note explains it
}