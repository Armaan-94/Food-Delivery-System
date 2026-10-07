package com.fooddelivery.orderservice.dto;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;

/** Stable JSON shape for a page of results; Spring's own Page implementation is not meant to be serialized. */
public record PageDto<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <S, T> PageDto<T> from(Page<S> page, Function<S, T> mapper) {
        return new PageDto<>(page.getContent().stream().map(mapper).toList(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
