package com.nebula.stationery.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.ObjectMetadata;
import com.nebula.stationery.common.BusinessException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * 图片存储：配置了 alioss.access-key-id 就走阿里云 OSS，否则落到本地 upload.dir。
 * application.yml 不进仓库，所以缺少凭证时必须有可用的本地兜底，老师克隆下来也能传图。
 */
@Slf4j
@Service
public class FileStorageService {

    private static final List<String> ALLOWED_EXTENSIONS = List.of("jpg", "jpeg", "png", "gif", "webp");
    private static final long MAX_SIZE = 5 * 1024 * 1024;
    private static final String OSS_DIR = "stationery";

    @Value("${upload.dir:./uploads}")
    private String uploadDir;

    @Value("${alioss.endpoint:}")
    private String endpoint;

    @Value("${alioss.access-key-id:}")
    private String accessKeyId;

    @Value("${alioss.access-key-secret:}")
    private String accessKeySecret;

    @Value("${alioss.bucket-name:}")
    private String bucketName;

    private OSS ossClient;

    @PostConstruct
    public void init() {
        if (!StringUtils.hasText(accessKeyId) || !StringUtils.hasText(bucketName)) {
            log.warn("未配置阿里云 OSS 凭证，图片将保存到本地目录：{}", uploadDir);
            return;
        }
        ossClient = new OSSClientBuilder().build(host(), accessKeyId, accessKeySecret);
        log.info("图片上传已启用阿里云 OSS：bucket={}", bucketName);
    }

    @PreDestroy
    public void destroy() {
        if (ossClient != null) ossClient.shutdown();
    }

    public String upload(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException("文件不能为空");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new BusinessException("图片大小不能超过 5MB");
        }

        String extension = getExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException("只支持 jpg/png/gif/webp 格式图片");
        }

        // 文件名由服务端生成，避免路径穿越与覆盖
        String datePath = LocalDate.now().toString();
        String fileName = UUID.randomUUID().toString().replace("-", "") + "." + extension;

        return ossClient != null
                ? uploadToOss(file, datePath, fileName)
                : uploadToLocal(datePath, fileName, file);
    }

    private String uploadToOss(MultipartFile file, String datePath, String fileName) {
        String key = OSS_DIR + "/review/" + datePath + "/" + fileName;
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(file.getSize());
        metadata.setContentType(StringUtils.hasText(file.getContentType())
                ? file.getContentType() : "image/" + fileName.substring(fileName.lastIndexOf('.') + 1));

        try (InputStream in = file.getInputStream()) {
            ossClient.putObject(bucketName, key, in, metadata);
        } catch (IOException e) {
            log.error("OSS 上传失败，key={}", key, e);
            throw new BusinessException("图片上传失败，请稍后重试");
        }
        return publicUrl(key);
    }

    private String uploadToLocal(String datePath, String fileName, MultipartFile file) {
        Path dir = Paths.get(uploadDir, datePath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(dir);
            try (var in = file.getInputStream()) {
                Files.copy(in, dir.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            log.error("本地保存失败，dir={}", dir, e);
            throw new BusinessException("图片保存失败，请稍后重试");
        }
        return "/uploads/" + datePath + "/" + fileName;
    }

    private String publicUrl(String key) {
        return "https://" + bucketName + "." + host() + "/" + key;
    }

    /** endpoint 可能带协议头，OSSClient 与拼 URL 都只要纯域名 */
    private String host() {
        return endpoint.replaceFirst("^https?://", "").replaceAll("/+$", "");
    }

    private String getExtension(String fileName) {
        if (fileName == null) return "";
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) return "";
        return fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
