package com.dcriar.orderintegration.api.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Anotação de validação composta para código identificador de canal de marketplace.
 * <p>
 * Regras: Não nulo/em branco, tamanho entre 2 e 30 caracteres e caracteres alfanuméricos com sublinhado.
 */
@Documented
@Constraint(validatedBy = {})
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@NotBlank(message = "O código identificador do canal de marketplace é obrigatório.")
@Size(min = 2, max = 30, message = "O código do canal deve conter entre 2 e 30 caracteres.")
@Pattern(regexp = "^[A-Za-z0-9_]+$", message = "O código do canal deve conter apenas letras, números e sublinhados.")
public @interface ValidChannelCode {

    String message() default "Código identificador do canal de marketplace inválido.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
