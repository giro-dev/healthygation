package dev.agiro.healthygation.domain.exception;

import dev.agiro.healthygation.domain.Domain;

public class NotFound extends RuntimeException {

    private final Domain domain;

    public NotFound(Domain domain, String message) {
        super(message);
        this.domain = domain;
    }

    public Domain getDomain() {
        return domain;
    }

}
