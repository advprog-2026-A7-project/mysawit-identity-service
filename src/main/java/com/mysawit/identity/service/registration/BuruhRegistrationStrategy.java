package com.mysawit.identity.service.registration;

import com.mysawit.identity.enums.Role;
import com.mysawit.identity.exception.InvalidMandorException;
import com.mysawit.identity.model.Buruh;
import com.mysawit.identity.model.Mandor;
import com.mysawit.identity.model.User;
import com.mysawit.identity.repository.MandorRepository;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class BuruhRegistrationStrategy implements UserRegistrationStrategy {

    private final MandorRepository mandorRepository;

    public BuruhRegistrationStrategy(MandorRepository mandorRepository) {
        this.mandorRepository = mandorRepository;
    }

    @Override
    public Role getRole() {
        return Role.BURUH;
    }

    @Override
    public User createUser(UserCreationContext context) {
        Buruh buruh = new Buruh();
        String mandorId = trimToNull(context.getMandorId());
        if (mandorId != null) {
            Mandor mandor = mandorRepository.findById(mandorId)
                    .orElseThrow(() -> new InvalidMandorException("Invalid mandorId"));
            buruh.setMandor(mandor);
        }
        return buruh;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
