package overskam.projectM;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MinIOContainer;
import overskam.projectM.common.enums.BuildStatus;
import overskam.projectM.dto.ArtifactResponse;
import overskam.projectM.exception.NotFoundException;
import overskam.projectM.model.PluginBuild;
import overskam.projectM.repository.jpa.PluginBuildRepository;
import overskam.projectM.service.PluginBuildService;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.BucketAlreadyOwnedByYouException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = "app.jwt.secret=dGVzdC1zZWNyZXQtZm9yLXVuaXQtdGVzdHMtb25seS0zMi1ieXRlcw")
class ArtifactDownloadIntegrationTest extends AbstractIntegrationTest {
    
    static final MinIOContainer minio = new MinIOContainer("minio/minio:latest");
    
    static {
        minio.start();
    }
    
    @DynamicPropertySource
    static void storageProperties(DynamicPropertyRegistry registry) {
        registry.add("app.storage.public-endpoint", minio::getS3URL);
        registry.add("app.storage.access-key", minio::getUserName);
        registry.add("app.storage.secret-key", minio::getPassword);
    }
    
    @Autowired
    private PluginBuildService pluginBuildService;
    
    @Autowired
    private PluginBuildRepository buildRepository;
    
    private final static String KEY = "builds/11bu22il33d/plugin.jar";
    private final static byte[] FAKE_JAR_BYTES = "fake jar".getBytes();
    
    @Test
    @DisplayName("Returns valid download link for S3 content")
    void returnsValidDownloadLinkForS3Content() throws Exception {
        PluginBuild build = saveBuild(BuildStatus.SUCCESS);
        uploadArtifact();
        
        ArtifactResponse artifact = pluginBuildService.getArtifact(build.getOwnerId(), build.getProjectId(), build.getId());
        
        HttpResponse<byte[]> response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create(artifact.url())).build(),
                HttpResponse.BodyHandlers.ofByteArray());
        
        assertEquals(200, response.statusCode());
        assertArrayEquals(FAKE_JAR_BYTES, response.body());
    }
    
    @Test
    @DisplayName("Refuses to hand out a link when the build has no artifact")
    void throwsNotFoundWhenThereIsNothingToDownLoad() {
        PluginBuild build = saveBuild(BuildStatus.QUEUED);
        
        assertThrowsExactly(NotFoundException.class, () ->
                pluginBuildService.getArtifact(build.getOwnerId(), build.getProjectId(), build.getId()));
    }
    
    private PluginBuild saveBuild(BuildStatus status) {
        UUID userId = UUID.randomUUID();
        String projectId = UUID.randomUUID().toString();
        PluginBuild build = new PluginBuild();
        build.setOwnerId(userId);
        build.setProjectId(projectId);
        build.setArtifactKey(KEY);
        build.setStatus(status);
        buildRepository.save(build);
        
        return build;
    }
    
    private void uploadArtifact() {
        S3Client s3 = S3Client.builder()
                .endpointOverride(URI.create(minio.getS3URL()))
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(minio.getUserName(), minio.getPassword())))
                .forcePathStyle(true)
                .build();
        
        try {
            s3.createBucket(b -> b.bucket("projectm-artifacts"));
        } catch (BucketAlreadyOwnedByYouException ignored) {}
        
        s3.putObject(r -> r.bucket("projectm-artifacts").key(KEY), RequestBody.fromBytes(FAKE_JAR_BYTES));
    }
}
