package org.example.order.ddd;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Transient;
import org.example.order.order.application.exception.ConstrainViolationException;
import org.example.order.order.application.exception.ErrorMessage;
import org.example.order.order.application.exception.UserError;

import java.util.List;

@MappedSuperclass
public abstract class DomainEntity<R extends AggregateRoot<R>> {

    @Transient
    private boolean isNew = true;

    @JsonIgnore
    public boolean isNew() {
        return this.isNew;
    }

    @PostLoad
    @PostPersist
    public void markNotNew() {
        this.isNew = false;
    }

    protected void checkRule(DomainRule rule) {
        if (rule.isBroken()) {
            throw new ConstrainViolationException(ErrorMessage.builder()
                    .addError(
                            UserError.builder()
                                    .code(rule.getClass().getSimpleName())
                                    .message(rule.message())
                                    .fields(List.of())
                                    .build())
                    .build());

        }
    }
}
