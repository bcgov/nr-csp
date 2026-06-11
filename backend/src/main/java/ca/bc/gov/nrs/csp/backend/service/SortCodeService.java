package ca.bc.gov.nrs.csp.backend.service;

import ca.bc.gov.nrs.csp.backend.controller.dto.sortcode.CreateSortCodeRequest;
import ca.bc.gov.nrs.csp.backend.controller.dto.sortcode.UpdateSortCodeRequest;
import ca.bc.gov.nrs.csp.backend.exception.BadRequestException;
import ca.bc.gov.nrs.csp.backend.exception.ConflictException;
import ca.bc.gov.nrs.csp.backend.exception.ResourceNotFoundException;
import ca.bc.gov.nrs.csp.backend.repository.SortCodeRepository;
import ca.bc.gov.nrs.csp.backend.service.model.SortCode;
import ca.bc.gov.nrs.csp.backend.util.validation.CommonValidation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class SortCodeService {

    private static final Logger log = LoggerFactory.getLogger(SortCodeService.class);

    private final SortCodeRepository sortCodeRepository;
    private final CommonValidation commonValidation;

    public SortCodeService(SortCodeRepository sortCodeRepository, CommonValidation commonValidation) {
        this.sortCodeRepository = sortCodeRepository;
        this.commonValidation = commonValidation;
    }

    @Transactional(readOnly = true)
    public List<SortCode> listAll() {
        log.debug("Sort code list requested");
        List<SortCode> results = sortCodeRepository.findAll();
        log.debug("Sort code list returned {} result(s)", results.size());
        return results;
    }

    @Transactional(readOnly = true)
    public Page<SortCode> listAll(Pageable pageable) {
        log.debug("Sort code paged list requested page={} size={}", pageable.getPageNumber(), pageable.getPageSize());
        Page<SortCode> results = sortCodeRepository.findAll(pageable);
        log.debug("Sort code paged list returned {} of {} result(s)", results.getNumberOfElements(), results.getTotalElements());
        return results;
    }

    public SortCode create(CreateSortCodeRequest request) {
        log.debug("Sort code create requested");
        String code = request.sortCode().toUpperCase();

        if (sortCodeRepository.existsByCode(code)) {
            throw new ConflictException("Sort code '" + code + "' already exists.");
        }

        if (!commonValidation.isValidDateRange(request.effectiveDate(), request.expiryDate())) {
            throw new BadRequestException("Effective date must not be after expiry date.");
        }

        SortCode sc = new SortCode(code, request.description(), request.effectiveDate(), request.expiryDate(), LocalDate.now());
        sortCodeRepository.insert(sc);
        return sortCodeRepository.findByCode(code).orElseThrow();
    }

    public SortCode update(String code, UpdateSortCodeRequest request) {
        log.debug("Sort code update requested");
        String upperCode = code.toUpperCase();

        if (!sortCodeRepository.existsByCode(upperCode)) {
            throw new ResourceNotFoundException("Sort code '" + upperCode + "' not found.");
        }

        if (!commonValidation.isValidDateRange(request.effectiveDate(), request.expiryDate())) {
            throw new BadRequestException("Effective date must not be after expiry date.");
        }

        SortCode sc = new SortCode(upperCode, request.description(), request.effectiveDate(), request.expiryDate(), LocalDate.now());
        sortCodeRepository.update(upperCode, sc);
        return sortCodeRepository.findByCode(upperCode).orElseThrow();
    }

    public void delete(String code) {
        log.debug("Sort code delete requested");
        String upperCode = code.toUpperCase();

        if (!sortCodeRepository.existsByCode(upperCode)) {
            throw new ResourceNotFoundException("Sort code '" + upperCode + "' not found.");
        }

        sortCodeRepository.delete(upperCode);
    }

}
