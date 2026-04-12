package com.mysawit.identity.service;

import com.mysawit.identity.dto.InternalUserDetailResponse;
import com.mysawit.identity.exception.UserNotFoundException;
import com.mysawit.identity.model.Buruh;
import com.mysawit.identity.model.Mandor;
import com.mysawit.identity.model.Supir;
import com.mysawit.identity.model.User;
import com.mysawit.identity.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class InternalUserService {

    private final UserRepository userRepository;

    public InternalUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public InternalUserDetailResponse getUserById(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        InternalUserDetailResponse.InternalUserDetailResponseBuilder builder = InternalUserDetailResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().name());

        if (user instanceof Buruh buruh && buruh.getMandor() != null) {
            builder.mandorId(buruh.getMandor().getId());
            builder.mandorName(buruh.getMandor().getName());
        }
        if (user instanceof Mandor mandor) {
            builder.certificationNumber(mandor.getCertificationNumber());
        }
        if (user instanceof Supir supir) {
            builder.kebunId(supir.getKebunId());
        }

        return builder.build();
    }
}
