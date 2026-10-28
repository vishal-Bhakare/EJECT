package com.edtech.coe.iam.entity;

public enum Role {
	SUPER_ADMIN, // Platform admin: manages the whole platform and all COEs
	LEADERSHIP, // Higher authorities: read-only view across COEs, reports
	COE_ADMIN, // Manages one COE: users, labs, Practice Heads
	PRACTICE_HEAD, // Creates batches/sessions, schedules and reassigns mentors
	HR, // Monitors attendance, discipline and escalations for a COE
	MENTOR, // Takes sessions, generates QR, posts assignments, reviews learners
	LEARNER // Scans QR, submits assignments, views scores and reviews
}