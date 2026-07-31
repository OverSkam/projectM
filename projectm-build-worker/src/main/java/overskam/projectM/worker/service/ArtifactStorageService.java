package overskam.projectM.worker.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ArtifactStorageService {
    private final S3Client s3Client;
    
    @Value("${app.storage.bucket}")
    private String bucket;
    
    public String saveJar(UUID buildId, byte[] jarBytes) {
        String key = "builds/" + buildId + "/plugin.jar";
        
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType("application/java-archive")
                        .build(),
                RequestBody.fromBytes(jarBytes)
        );
        
        return key;
    }
    
    public void deleteByKey(String artifactKey) {
        s3Client.deleteObject(
                DeleteObjectRequest.builder()
                        .bucket(bucket)
                        .key(artifactKey)
                        .build()
        );
    }
}