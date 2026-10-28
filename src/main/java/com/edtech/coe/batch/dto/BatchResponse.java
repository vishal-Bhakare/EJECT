package com.edtech.coe.batch.dto;

import com.edtech.coe.batch.entity.Batch;
import com.edtech.coe.batch.entity.BatchStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchResponse {

    private Long id;
    private String batchCode;
    private Long coeId;
    private String name;
    private String trackName;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer maxLearners;
    private long enrolledCount;
    private int availableSeats;
    private BatchStatus status;
    private Instant createdAt;
    private Instant updatedAt;

    // The number of enrolled learners is not stored on the batch. The service counts it
    // and passes it in, so the response always shows the current figure.
    public static BatchResponse from(Batch batch, long enrolledCount) {
        // Seats can only be taken while the batch is open
        boolean open = batch.getStatus() == BatchStatus.PLANNED
                || batch.getStatus() == BatchStatus.ONGOING;
        long free = open ? Math.max(0, batch.getMaxLearners() - enrolledCount) : 0;

        return BatchResponse.builder()
                .id(batch.getId())
                .batchCode(batch.getBatchCode())
                .coeId(batch.getCoeId())
                .name(batch.getName())
                .trackName(batch.getTrackName())
                .description(batch.getDescription())
                .startDate(batch.getStartDate())
                .endDate(batch.getEndDate())
                .maxLearners(batch.getMaxLearners())
                .enrolledCount(enrolledCount)
                .availableSeats((int) free)
                .status(batch.getStatus())
                .createdAt(batch.getCreatedAt())
                .updatedAt(batch.getUpdatedAt())
                .build();
    }
}