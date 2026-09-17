package overskam.projectM.repository.mongo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import overskam.projectM.AbstractIntegrationTest;
import overskam.projectM.model.Project;
import overskam.projectM.util.SortingUtil;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataMongoTest
class ProjectRepositoryTest extends AbstractIntegrationTest {
    
    @Autowired
    private ProjectRepository projectRepository;
    
    @Test
    @DisplayName("Returns only the owner's projects, newest first")
    void returnsOwnProjectsNewestFirst() {
        UUID ownerId = UUID.randomUUID();
        UUID notOwnerId = UUID.randomUUID();
        
        Project first = new Project();
        first.setOwnerId(ownerId);
        first.setId("abc123");
        first.setLastModifiedAt(LocalDateTime.of(2026, Month.APRIL, 10, 10, 10));
        
        Project second = new Project();
        second.setOwnerId(ownerId);
        second.setId("def456");
        second.setLastModifiedAt(LocalDateTime.of(2026, Month.MARCH, 10, 10, 10));
        
        Project third = new Project();
        third.setOwnerId(ownerId);
        third.setId("ghi789");
        third.setLastModifiedAt(LocalDateTime.of(2026, Month.MAY, 10, 10, 10));
        
        Project fourth = new Project();
        fourth.setOwnerId(notOwnerId);
        fourth.setId("jkl000");
        fourth.setLastModifiedAt(LocalDateTime.of(2026, Month.OCTOBER, 10, 10, 10));
        
        projectRepository.save(first);
        projectRepository.save(second);
        projectRepository.save(third);
        projectRepository.save(fourth);
        
        Page<ProjectRepository.ProjectNameView> result = projectRepository.findProjectNamesByOwnerId(
                ownerId,
                PageRequest.of(0, 10, SortingUtil.forProjects("updatedAt", "desc"))
        );
        
        List<String> ids = result.getContent().stream()
                .map(ProjectRepository.ProjectNameView::getId)
                .toList();
        
        assertEquals(List.of("ghi789", "abc123", "def456"), ids);
    }
}