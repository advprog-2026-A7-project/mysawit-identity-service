package com.mysawit.identity.service.registration;

import com.mysawit.identity.enums.Role;
import com.mysawit.identity.exception.DuplicateCertificationNumberException;
import com.mysawit.identity.exception.MissingMandorCertificationException;
import com.mysawit.identity.model.Mandor;
import com.mysawit.identity.model.User;
import com.mysawit.identity.repository.MandorRepository;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class MandorRegistrationStrategy implements UserRegistrationStrategy {

    private final MandorRepository mandorRepository;

    public MandorRegistrationStrategy(MandorRepository mandorRepository) {
        this.mandorRepository = mandorRepository;
    }

    @Override
    public Role getRole() {
        return Role.MANDOR;
    }

    @Override
    public User createUser(UserCreationContext context) {
        String certificationNumber = trimToNull(context.getCertificationNumber());
        if (certificationNumber == null) {
            throw new MissingMandorCertificationException("Certification number is required for MANDOR");
        }
        if (mandorRepository.existsByCertificationNumber(certificationNumber)) {
            throw new DuplicateCertificationNumberException("Certification number already exists");
        }
        Mandor mandor = new Mandor();
        mandor.setCertificationNumber(certificationNumber);
        return mandor;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
