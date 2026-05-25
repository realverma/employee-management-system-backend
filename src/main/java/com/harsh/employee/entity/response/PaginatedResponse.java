package com.harsh.employee.entity.response;

import java.util.List;

public record PaginatedResponse<T>(
        List<T> content,
        int currentPage,
        long totalElements,
        int totalPages
) {}