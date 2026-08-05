package overskam.projectM.util;

import org.springframework.data.domain.Sort;
import overskam.projectM.exception.InvalidRequestException;

import java.util.Map;

public final class SortingUtil {
    private SortingUtil() {
    }
    
    private static final Map<String, String> PROJECT_SORT = Map.of(
            "name", "name",
            "createdAt", "createdAt",
            "updatedAt", "lastModifiedAt"
    );
    
    private static final Map<String, String> BUILD_SORT = Map.of(
            "status", "status",
            "createdAt", "createDate",
            "updatedAt", "lastModifiedDate"
    );
    
    public static Sort forProjects(String sortBy, String sortDirection) {
        return build(PROJECT_SORT, sortBy, sortDirection);
    }
    
    public static Sort forBuilds(String sortBy, String sortDirection) {
        return build(BUILD_SORT, sortBy, sortDirection);
    }
    
    private static Sort build(Map<String, String> allowed, String sortBy, String sortDirection) {
        String property = allowed.get(sortBy);
        if (property == null)
            throw new InvalidRequestException("Invalid sort field: " + sortBy);
        Sort sort = Sort.by(property);
        return "desc".equalsIgnoreCase(sortDirection) ? sort.descending() : sort.ascending();
    }
}
