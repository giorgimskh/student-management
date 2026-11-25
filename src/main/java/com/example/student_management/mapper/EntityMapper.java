package com.example.student_management.mapper;

import java.util.Collection;
import java.util.List;

/**
 * Converts between a JPA entity and the request/response DTOs the API exposes.
 *
 * @param <E>   entity type
 * @param <Req> request DTO type
 * @param <Res> response DTO type
 */
public interface EntityMapper<E, Req, Res> {
    E toEntity(Req request);

    Res toResponse(E entity);

    default List<Res> toResponses(Collection<? extends E> entities) {
        return entities.stream().map(this::toResponse).toList();
    }
}
