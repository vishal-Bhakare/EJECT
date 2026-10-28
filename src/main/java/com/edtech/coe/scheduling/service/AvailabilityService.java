package com.edtech.coe.scheduling.service;

import com.edtech.coe.iam.dto.AuthenticatedUser;
import com.edtech.coe.scheduling.dto.CreateUnavailabilityRequest;
import com.edtech.coe.scheduling.dto.UnavailabilityResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface AvailabilityService {

    // The response lists the SCHEDULED sessions that the absence leaves without a teacher
    UnavailabilityResponse recordUnavailability(CreateUnavailabilityRequest request,
                                                AuthenticatedUser caller);

    UnavailabilityResponse getUnavailabilityById(Long id, AuthenticatedUser caller);

    Page<UnavailabilityResponse> getUnavailability(Long coeId, Long mentorId, Boolean active,
                                                   LocalDate fromDate, LocalDate toDate,
                                                   Pageable pageable, AuthenticatedUser caller);

    // "Delete" cancels the record. The row is kept.
    void cancelUnavailability(Long id, AuthenticatedUser caller);
}