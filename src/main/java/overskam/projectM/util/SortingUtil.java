package overskam.projectM.util;

import org.springframework.data.domain.Sort;
import overskam.projectM.exception.InvalidRequestException;

import java.util.Set;

public class SortingUtil {
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("name", "create_date", "last_modified_date");
    
    public static Sort sortGenerator(String sortBy, String sortDirection){
        if (!ALLOWED_SORT_FIELDS.contains(sortBy))
            throw new InvalidRequestException("Invalid sort field: " + sortBy);
        Sort sort = Sort.by(Sort.Order.by(sortBy));
        return sortDirection.equalsIgnoreCase("desc") ? sort.descending() : sort.ascending();
    }
}
