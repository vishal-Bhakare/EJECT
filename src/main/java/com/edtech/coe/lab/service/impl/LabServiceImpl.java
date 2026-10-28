package com.edtech.coe.lab.service.impl;

import com.edtech.coe.coe.service.CoeService;
import com.edtech.coe.common.exception.BusinessException;
import com.edtech.coe.common.exception.ResourceNotFoundException;
import com.edtech.coe.iam.dto.AuthenticatedUser;
import com.edtech.coe.lab.dto.CreateLabRequest;
import com.edtech.coe.lab.dto.LabResponse;
import com.edtech.coe.lab.dto.UpdateLabRequest;
import com.edtech.coe.lab.entity.Lab;
import com.edtech.coe.lab.entity.LabType;
import com.edtech.coe.lab.repository.LabRepository;
import com.edtech.coe.lab.service.LabService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LabServiceImpl implements LabService {

	private final LabRepository labRepository;
	private final CoeService coeService;

	@Override
	@Transactional
	public LabResponse createLab(CreateLabRequest request, AuthenticatedUser caller) {
		Long coeId = request.getCoeId();

		// Scope first: a caller must not learn anything about other COEs
		requireOwnCoe(caller, coeId);

		if (!coeService.isActiveCoe(coeId)) {
			throw coeNotUsable(coeId);
		}

		String name = request.getName().trim();
		if (labRepository.existsByCoeIdAndName(coeId, name)) {
			throw duplicateName(name);
		}

		Lab lab = Lab.builder().labCode(temporaryCode()).coeId(coeId).name(name).labType(request.getLabType())
				.capacity(request.getCapacity()).location(trimToNull(request.getLocation()))
				.description(trimToNull(request.getDescription())).build();

		// First save gives us the auto-generated id
		Lab saved = labRepository.save(lab);

		// Build the real code from the id, e.g. LAB-0001
		saved.setLabCode(buildLabCode(saved.getId()));

		return LabResponse.from(saved);
	}

	@Override
	public LabResponse getLabById(Long id, AuthenticatedUser caller) {
		return LabResponse.from(findInScope(id, caller));
	}

	@Override
	public Page<LabResponse> getLabs(Long coeId, LabType labType, Boolean active, Pageable pageable,
			AuthenticatedUser caller) {
		Long effectiveCoeId = resolveListCoe(coeId, caller);
		return labRepository.search(effectiveCoeId, labType, active, pageable).map(LabResponse::from);
	}

	@Override
	@Transactional
	public LabResponse updateLab(Long id, UpdateLabRequest request, AuthenticatedUser caller) {
		Lab lab = findInScope(id, caller);
		String name = request.getName().trim();

		// The lab may keep its own name, but not take another lab's name in the same
		// COE
		if (labRepository.existsByCoeIdAndNameAndIdNot(lab.getCoeId(), name, id)) {
			throw duplicateName(name);
		}

		// A lab cannot be switched back on inside a COE that is switched off
		boolean reactivating = Boolean.TRUE.equals(request.getActive()) && !lab.isActive();
		if (reactivating && !coeService.isActiveCoe(lab.getCoeId())) {
			throw new BusinessException(HttpStatus.BAD_REQUEST, "Cannot activate this lab because its COE is inactive");
		}

		// Full replacement: optional fields that are not sent become null
		lab.setName(name);
		lab.setLabType(request.getLabType());
		lab.setCapacity(request.getCapacity());
		lab.setLocation(trimToNull(request.getLocation()));
		lab.setDescription(trimToNull(request.getDescription()));

		if (request.getActive() != null) {
			lab.setActive(request.getActive());
		}

		// saveAndFlush runs auditing now, so the response shows the new updatedAt
		// and a unique-key clash is reported inside this method
		return LabResponse.from(labRepository.saveAndFlush(lab));
	}

	@Override
	@Transactional
	public void deleteLab(Long id, AuthenticatedUser caller) {
		Lab lab = findInScope(id, caller);
		// Soft delete: keep the row so sessions and attendance history stay valid
		lab.setActive(false);
	}

	// ---------- scope rules ----------

	// Create: the caller names the COE, so a wrong COE is a clear "not allowed"
	private void requireOwnCoe(AuthenticatedUser caller, Long coeId) {
		if (caller.isPlatformWide()) {
			return;
		}
		if (caller.getCoeId() == null || !caller.getCoeId().equals(coeId)) {
			throw new BusinessException(HttpStatus.FORBIDDEN, "You can only manage labs in your own COE");
		}
	}

	// Get, update, delete: a lab outside the caller's COE looks like it does not
	// exist
	private Lab findInScope(Long id, AuthenticatedUser caller) {
		Lab lab = labRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Lab", id));

		if (!caller.isPlatformWide() && !Objects.equals(caller.getCoeId(), lab.getCoeId())) {
			throw new ResourceNotFoundException("Lab", id);
		}
		return lab;
	}

	// List: platform-wide roles may filter by any COE, everyone else is held to
	// their own
	private Long resolveListCoe(Long requestedCoeId, AuthenticatedUser caller) {
		if (caller.isPlatformWide()) {
			return requestedCoeId;
		}
		// Without this check, a missing coeId would turn into "no filter" and list
		// every COE
		if (caller.getCoeId() == null) {
			throw new BusinessException(HttpStatus.FORBIDDEN, "Your account is not linked to a COE");
		}
		if (requestedCoeId != null && !requestedCoeId.equals(caller.getCoeId())) {
			throw new BusinessException(HttpStatus.FORBIDDEN, "You can only view labs in your own COE");
		}
		return caller.getCoeId();
	}

	// ---------- private helpers ----------

	private BusinessException coeNotUsable(Long coeId) {
		return new BusinessException(HttpStatus.BAD_REQUEST, "COE with id " + coeId + " does not exist or is inactive");
	}

	private BusinessException duplicateName(String name) {
		return new BusinessException(HttpStatus.CONFLICT, "A lab named '" + name + "' already exists in this COE");
	}

	private String temporaryCode() {
		// max 30 chars: "TMP-" (4) + 20 = 24
		return "TMP-" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
	}

	private String buildLabCode(Long id) {
		return "LAB-" + String.format("%04d", id);
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