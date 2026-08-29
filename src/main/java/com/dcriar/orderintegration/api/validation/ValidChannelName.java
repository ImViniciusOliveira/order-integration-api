package com.dcriar.orderintegration.api.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Anotação de validação composta para nome descritivo de canal de marketplace.
 * <p>
 * Regras: Não nulo/em branco e tamanho entre 2 e 100 caracteres.
 */
@Documented
@Constraint(validatedBy = {})
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@NotBlank(message = "O nome descritivo do canal é obrigatório.")
@Size(min = 2, max = 100, message = "O nome do canal deve conter entre 2 e 100 caracteres.")
public @interface ValidChannelName {

    String message() default "Nome do canal de marketplace inválido.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
