package com.anujbohre.uqlab.job;

import java.util.List;
import java.util.function.Function;

/**
 * One page of results plus the totals a client needs to build page links. Pages are numbered from 0.
 */
public record PageResult<T>(List<T> items, int page, int size, long totalItems, int totalPages) {

    public <R> PageResult<R> map(Function<? super T, ? extends R> mapper) {
        return new PageResult<>(items.stream().<R>map(mapper).toList(), page, size, totalItems, totalPages);
    }
}
