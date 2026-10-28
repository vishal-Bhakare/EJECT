package com.edtech.coe.lab.controller;

import com.edtech.coe.iam.dto.AuthenticatedUser;
import com.edtech.coe.lab.dto.CreateLabRequest;
import com.edtech.coe.lab.dto.LabResponse;
import com.edtech.coe.lab.dto.UpdateLabRequest;
import com.edtech.coe.lab.entity.LabType;
import com.edtech.coe.lab.service.LabService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/labs")
@RequiredArgsConstructor
public class LabController {

    private final LabService labService;

    @PostMapping
    public ResponseEntity<LabResponse> createLab(@Valid @RequestBody CreateLabRequest request,
                                                 @AuthenticationPrincipal AuthenticatedUser caller) {
        return ResponseEntity.status(HttpStatus.CREATED).body(labService.createLab(request, caller));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LabResponse> getLabById(@PathVariable Long id,
                                                  @AuthenticationPrincipal AuthenticatedUser caller) {
        return ResponseEntity.ok(labService.getLabById(id, caller));
    }

    @GetMapping
    public ResponseEntity<Page<LabResponse>> getLabs(
            @RequestParam(required = false) Long coeId,
            @RequestParam(required = false) LabType labType,
            @RequestParam(required = false) Boolean active,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable,
            @AuthenticationPrincipal AuthenticatedUser caller) {
        return ResponseEntity.ok(labService.getLabs(coeId, labType, active, pageable, caller));
    }

    @PutMapping("/{id}")
    public ResponseEntity<LabResponse> updateLab(@PathVariable Long id,
                                                 @Valid @RequestBody UpdateLabRequest request,
                                                 @AuthenticationPrincipal AuthenticatedUser caller) {
        return ResponseEntity.ok(labService.updateLab(id, request, caller));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLab(@PathVariable Long id,
                                          @AuthenticationPrincipal AuthenticatedUser caller) {
        labService.deleteLab(id, caller);
        return ResponseEntity.noContent().build();
    }
}