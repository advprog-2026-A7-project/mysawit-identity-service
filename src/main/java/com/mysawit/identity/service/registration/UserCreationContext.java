package com.mysawit.identity.service.registration;

public class UserCreationContext {

    private final String certificationNumber;
    private final String mandorId;
    private final String kebunId;

    private UserCreationContext(Builder builder) {
        this.certificationNumber = builder.certificationNumber;
        this.mandorId = builder.mandorId;
        this.kebunId = builder.kebunId;
    }

    public String getCertificationNumber() {
        return certificationNumber;
    }

    public String getMandorId() {
        return mandorId;
    }

    public String getKebunId() {
        return kebunId;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String certificationNumber;
        private String mandorId;
        private String kebunId;

        public Builder certificationNumber(String certificationNumber) {
            this.certificationNumber = certificationNumber;
            return this;
        }

        public Builder mandorId(String mandorId) {
            this.mandorId = mandorId;
            return this;
        }

        public Builder kebunId(String kebunId) {
            this.kebunId = kebunId;
            return this;
        }

        public UserCreationContext build() {
            return new UserCreationContext(this);
        }
    }
}
