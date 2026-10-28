package com.edtech.coe.coe.controller;

import com.edtech.coe.coe.dto.CoeResponse;
import com.edtech.coe.coe.dto.CreateCoeRequest;
import com.edtech.coe.coe.dto.UpdateCoeRequest;
import com.edtech.coe.coe.service.CoeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/api/v1/coes")
@RequiredArgsConstructor
public class CoeController {

    private final CoeService coeService;

    @PostMapping
    public ResponseEntity<CoeResponse> createCoe(@Valid @RequestBody CreateCoeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(coeService.createCoe(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CoeResponse> getCoeById(@PathVariable Long id) {
        return ResponseEntity.ok(coeService.getCoeById(id));
    }

    @GetMapping
    public ResponseEntity<Page<CoeResponse>> getCoes(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) Boolean active,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(coeService.getCoes(city, state, active, pageable));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CoeResponse> updateCoe(@PathVariable Long id,
                                                 @Valid @RequestBody UpdateCoeRequest request) {
        return ResponseEntity.ok(coeService.updateCoe(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCoe(@PathVariable Long id) {
        coeService.deleteCoe(id);
        return ResponseEntity.noContent().build();
    }
}