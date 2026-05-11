package com.mysawit.identity.service;

import com.mysawit.identity.dto.UserDetailResponse;
import com.mysawit.identity.enums.Role;
import com.mysawit.identity.exception.CannotDeleteAdminUtamaException;
import com.mysawit.identity.exception.CannotDeleteSelfException;
import com.mysawit.identity.exception.InvalidMandorException;
import com.mysawit.identity.exception.InvalidUserRoleException;
import com.mysawit.identity.exception.UserNotFoundException;
import com.mysawit.identity.model.Buruh;
import com.mysawit.identity.model.Mandor;
import com.mysawit.identity.model.Supir;
import com.mysawit.identity.model.User;
import com.mysawit.identity.repository.MandorRepository;
import com.mysawit.identity.repository.RefreshTokenRepository;
import com.mysawit.identity.repository.UserRepository;
import com.mysawit.identity.repository.UserSpecification;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final MandorRepository mandorRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${admin.utama.email:admin@mysawit.com}")
    private String adminUtamaEmail;

    public AdminService(
            UserRepository userRepository,
            MandorRepository mandorRepository,
            RefreshTokenRepository refreshTokenRepository
    ) {
        this.userRepository = userRepository;
        this.mandorRepository = mandorRepository;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    public List<UserDetailResponse> listUsers(String name, String email, Role role) {
        Specification<User> spec = UserSpecification.withFilters(name, email, role);
        return userRepository.findAll(spec).stream()
                .map(this::toDetailResponse)
                .toList();
    }

    public UserDetailResponse getUser(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        return toDetailResponse(user);
    }

    @Transactional
    public void assignBuruhToMandor(String buruhId, String mandorId) {
        User user = userRepository.findById(buruhId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (!(user instanceof Buruh buruh)) {
            throw new InvalidUserRoleException("User is not a BURUH");
        }

        Mandor mandor = mandorRepository.findById(mandorId)
                .orElseThrow(() -> new InvalidMandorException("Mandor not found"));

        buruh.setMandor(mandor);
        userRepository.save(buruh);
    }

    @Transactional
    public void unassignBuruhFromMandor(String buruhId) {
        User user = userRepository.findById(buruhId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (!(user instanceof Buruh buruh)) {
            throw new InvalidUserRoleException("User is not a BURUH");
        }

        buruh.setMandor(null);
        userRepository.save(buruh);
    }

    @Transactional
    public void deleteUser(String adminId, String targetUserId) {
        if (adminId.equals(targetUserId)) {
            throw new CannotDeleteSelfException("Cannot delete your own account");
        }

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (targetUser.getEmail().equals(adminUtamaEmail)) {
            throw new CannotDeleteAdminUtamaException("Cannot delete the Admin Utama account");
        }

        refreshTokenRepository.deleteByUserId(targetUserId);
        userRepository.delete(targetUser);
    }

    private UserDetailResponse toDetailResponse(User user) {
        UserDetailResponse.UserDetailResponseBuilder builder = UserDetailResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole().name())
                .createdAt(user.getCreatedAt())
                .googleLinked(StringUtils.hasText(user.getGoogleSub()))
                .hasPassword(StringUtils.hasText(user.getPassword()));

        if (user instanceof Buruh buruh && buruh.getMandor() != null) {
            builder.mandorId(buruh.getMandor().getId());
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
