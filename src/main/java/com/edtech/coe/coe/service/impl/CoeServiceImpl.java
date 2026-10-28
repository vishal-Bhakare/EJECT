package com.edtech.coe.coe.service.impl;

import com.edtech.coe.coe.dto.CoeResponse;
import com.edtech.coe.coe.dto.CreateCoeRequest;
import com.edtech.coe.coe.dto.UpdateCoeRequest;
import com.edtech.coe.coe.entity.Coe;
import com.edtech.coe.coe.repository.CoeRepository;
import com.edtech.coe.coe.service.CoeService;
import com.edtech.coe.common.exception.BusinessException;
import com.edtech.coe.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CoeServiceImpl implements CoeService {

    private final CoeRepository coeRepository;

    @Override
    @Transactional
    public CoeResponse createCoe(CreateCoeRequest request) {
        String name = request.getName().trim();

        if (coeRepository.existsByName(name)) {
            throw duplicateName(name);
        }

        Coe coe = Coe.builder()
                .coeCode(temporaryCode())
                .name(name)
                .description(trimToNull(request.getDescription()))
                .addressLine(trimToNull(request.getAddressLine()))
                .city(request.getCity().trim())
                .state(request.getState().trim())
                .pincode(request.getPincode())
                .contactEmail(trimToNull(request.getContactEmail()))
                .contactPhone(request.getContactPhone())
                .build();

        // First save gives us the auto-generated id
        Coe saved = coeRepository.save(coe);

        // Build the real code from the id, e.g. COE-0001
        saved.setCoeCode(buildCoeCode(saved.getId()));

        return CoeResponse.from(saved);
    }

    @Override
    public CoeResponse getCoeById(Long id) {
        return CoeResponse.from(findOrThrow(id));
    }

    @Override
    public Page<CoeResponse> getCoes(String city, String state, Boolean active, Pageable pageable) {
        return coeRepository
                .search(trimToNull(city), trimToNull(state), active, pageable)
                .map(CoeResponse::from);
    }

    @Override
    @Transactional
    public CoeResponse updateCoe(Long id, UpdateCoeRequest request) {
        Coe coe = findOrThrow(id);
        String name = request.getName().trim();

        // The COE may keep its own name, but not take another COE's name
        if (coeRepository.existsByNameAndIdNot(name, id)) {
            throw duplicateName(name);
        }

        // Full replacement: optional fields that are not sent become null
        coe.setName(name);
        coe.setDescription(trimToNull(request.getDescription()));
        coe.setAddressLine(trimToNull(request.getAddressLine()));
        coe.setCity(request.getCity().trim());
        coe.setState(request.getState().trim());
        coe.setPincode(request.getPincode());
        coe.setContactEmail(trimToNull(request.getContactEmail()));
        coe.setContactPhone(request.getContactPhone());

        if (request.getActive() != null) {
            coe.setActive(request.getActive());
        }

        // saveAndFlush runs auditing now, so the response shows the new updatedAt
        // and a unique-key clash is reported inside this method
        return CoeResponse.from(coeRepository.saveAndFlush(coe));
    }
    
    @Override
    public boolean isActiveCoe(Long id) {
        return coeRepository.existsByIdAndActiveTrue(id);
    }

    @Override
    @Transactional
    public void deleteCoe(Long id) {
        Coe coe = findOrThrow(id);
        // Soft delete: keep the row so users, labs and history stay valid
        coe.setActive(false);
    }

    // ---------- private helpers ----------

    private Coe findOrThrow(Long id) {
        return coeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("COE", id));
    }

    private BusinessException duplicateName(String name) {
        return new BusinessException(HttpStatus.CONFLICT,
                "A COE with name '" + name + "' already exists");
    }

    private String temporaryCode() {
        // max 30 chars: "TMP-" (4) + 20 = 24
        return "TMP-" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
    }

    private String buildCoeCode(Long id) {
        return "COE-" + String.format("%04d", id);
    }

    // Blank or whitespace-only text becomes null, so it is stored as "no value"
    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}