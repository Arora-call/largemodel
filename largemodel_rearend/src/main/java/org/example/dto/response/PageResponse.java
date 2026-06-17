package org.example.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {

    private List<T> content;
    private long totalElements;
    private int totalPages;
    private int currentPage;
    private int pageSize;
    private boolean first;
    private boolean last;

    public static <T> PageResponse<T> empty() {
        return PageResponse.<T>builder()
                .content(Collections.emptyList())
                .totalElements(0)
                .totalPages(0)
                .currentPage(0)
                .pageSize(0)
                .first(true)
                .last(true)
                .build();
    }
}
