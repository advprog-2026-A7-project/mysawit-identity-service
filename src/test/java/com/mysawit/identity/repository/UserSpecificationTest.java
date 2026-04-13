package com.mysawit.identity.repository;

import com.mysawit.identity.enums.Role;
import com.mysawit.identity.model.User;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")
class UserSpecificationTest {

    private Root<User> root;
    private CriteriaQuery<?> query;
    private CriteriaBuilder criteriaBuilder;

    @BeforeEach
    void setUp() {
        root = mock(Root.class);
        query = mock(CriteriaQuery.class);
        criteriaBuilder = mock(CriteriaBuilder.class);
    }

    @Test
    void withFiltersReturnsEmptyPredicatesWhenAllNull() {
        Predicate conjunction = mock(Predicate.class);
        when(criteriaBuilder.and(any(Predicate[].class))).thenReturn(conjunction);

        Specification<User> spec = UserSpecification.withFilters(null, null, null);
        Predicate result = spec.toPredicate(root, query, criteriaBuilder);

        assertNotNull(result);
        verify(criteriaBuilder, never()).like(any(Expression.class), anyString());
        verify(criteriaBuilder, never()).equal(any(Expression.class), any());
    }

    @Test
    void withFiltersReturnsEmptyPredicatesWhenAllBlank() {
        Predicate conjunction = mock(Predicate.class);
        when(criteriaBuilder.and(any(Predicate[].class))).thenReturn(conjunction);

        Specification<User> spec = UserSpecification.withFilters("  ", "  ", null);
        Predicate result = spec.toPredicate(root, query, criteriaBuilder);

        assertNotNull(result);
        verify(criteriaBuilder, never()).like(any(Expression.class), anyString());
    }

    @Test
    void withFiltersAppliesNameFilter() {
        Path<String> namePath = mock(Path.class);
        Expression<String> lowerExpr = mock(Expression.class);
        Predicate likePredicate = mock(Predicate.class);
        Predicate conjunction = mock(Predicate.class);

        when(root.<String>get("name")).thenReturn(namePath);
        when(criteriaBuilder.lower(namePath)).thenReturn(lowerExpr);
        when(criteriaBuilder.like(lowerExpr, "%john%")).thenReturn(likePredicate);
        when(criteriaBuilder.and(any(Predicate[].class))).thenReturn(conjunction);

        Specification<User> spec = UserSpecification.withFilters("John", null, null);
        spec.toPredicate(root, query, criteriaBuilder);

        verify(criteriaBuilder).like(lowerExpr, "%john%");
    }

    @Test
    void withFiltersAppliesEmailFilter() {
        Path<String> emailPath = mock(Path.class);
        Expression<String> lowerExpr = mock(Expression.class);
        Predicate likePredicate = mock(Predicate.class);
        Predicate conjunction = mock(Predicate.class);

        when(root.<String>get("email")).thenReturn(emailPath);
        when(criteriaBuilder.lower(emailPath)).thenReturn(lowerExpr);
        when(criteriaBuilder.like(lowerExpr, "%test@mail%")).thenReturn(likePredicate);
        when(criteriaBuilder.and(any(Predicate[].class))).thenReturn(conjunction);

        Specification<User> spec = UserSpecification.withFilters(null, "test@mail", null);
        spec.toPredicate(root, query, criteriaBuilder);

        verify(criteriaBuilder).like(lowerExpr, "%test@mail%");
    }

    @Test
    void withFiltersAppliesRoleFilter() {
        Path<Role> rolePath = mock(Path.class);
        Predicate equalPredicate = mock(Predicate.class);
        Predicate conjunction = mock(Predicate.class);

        when(root.<Role>get("role")).thenReturn(rolePath);
        when(criteriaBuilder.equal(rolePath, Role.BURUH)).thenReturn(equalPredicate);
        when(criteriaBuilder.and(any(Predicate[].class))).thenReturn(conjunction);

        Specification<User> spec = UserSpecification.withFilters(null, null, Role.BURUH);
        spec.toPredicate(root, query, criteriaBuilder);

        verify(criteriaBuilder).equal(rolePath, Role.BURUH);
    }

    @Test
    void withFiltersAppliesAllFilters() {
        Path<String> namePath = mock(Path.class);
        Path<String> emailPath = mock(Path.class);
        Path<Role> rolePath = mock(Path.class);
        Expression<String> lowerName = mock(Expression.class);
        Expression<String> lowerEmail = mock(Expression.class);
        Predicate conjunction = mock(Predicate.class);

        when(root.<String>get("name")).thenReturn(namePath);
        when(root.<String>get("email")).thenReturn(emailPath);
        when(root.<Role>get("role")).thenReturn(rolePath);
        when(criteriaBuilder.lower(namePath)).thenReturn(lowerName);
        when(criteriaBuilder.lower(emailPath)).thenReturn(lowerEmail);
        when(criteriaBuilder.like(any(Expression.class), anyString())).thenReturn(mock(Predicate.class));
        when(criteriaBuilder.equal(any(Expression.class), any())).thenReturn(mock(Predicate.class));
        when(criteriaBuilder.and(any(Predicate[].class))).thenReturn(conjunction);

        Specification<User> spec = UserSpecification.withFilters("John", "test@mail", Role.MANDOR);
        spec.toPredicate(root, query, criteriaBuilder);

        verify(criteriaBuilder).like(lowerName, "%john%");
        verify(criteriaBuilder).like(lowerEmail, "%test@mail%");
        verify(criteriaBuilder).equal(rolePath, Role.MANDOR);
    }
}
