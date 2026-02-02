package spboard.board.Service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import spboard.board.Domain.entity.Board;
import spboard.board.Domain.entity.UploadImage;
import spboard.board.Repository.BoardMapper;
import spboard.board.Repository.UploadImageMapper;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3UploadService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final UploadImageMapper uploadImageMapper;
    private final BoardMapper boardMapper;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    public UploadImage saveImage(MultipartFile multipartFile, Board board) throws IOException {
        if (multipartFile.isEmpty()) {
            return null;
        }

        String originalFilename = multipartFile.getOriginalFilename();

        // 원본 파일명 -> 서버에 저장된 파일명(중복 x)
        // 파일명이 중복되지 않도록 UUID로 설정 + 확장자 유지
        String savedFilename = UUID.randomUUID() + "." + extractExt(originalFilename);

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(savedFilename)
                .contentType(multipartFile.getContentType())
                .contentLength(multipartFile.getSize())
                .build();
        // S3에 파일 업로드
       s3Client.putObject(
               request,
               RequestBody.fromInputStream(
                       multipartFile.getInputStream(),
                       multipartFile.getSize()
               )
       );

       // 객체 생성
       UploadImage uploadImage = UploadImage.builder()
               .originalFilename(originalFilename)
               .savedFilename(savedFilename)
               .build();

       // db 저장
        uploadImageMapper.insert(uploadImage);

        // board와 image 연결 (1:1 관계 업데이트)
        board.setUploadImage(uploadImage);
        boardMapper.updateUploadImageId(board.getId(), uploadImage.getId());

        return uploadImage;

    }

    // 확장자 추출
    private String extractExt(String originalFilename) {
        int pos = originalFilename.lastIndexOf(".");
        return originalFilename.substring(pos + 1);
    }

    public ResponseEntity<Void> downloadImage(Long boardId) {
        // boardId에 해당하는 게시글이 없으면 null return
        Board board = boardMapper.findById(boardId).get();
        if (board == null) {
            throw  new IllegalArgumentException("게시글 없음");
        }


        UploadImage image = board.getUploadImage();
        if (image == null) {
            return ResponseEntity.notFound().build();
        }

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(image.getSavedFilename())
                .build();
        // 업로드 한 파일명이 한글인 경우 아래 작업을 안해주면 깨질 수 있음
        GetObjectPresignRequest presignRequest =
                GetObjectPresignRequest.builder()
                        .signatureDuration(Duration.ofMinutes(3))
                        .getObjectRequest(getObjectRequest)
                        .build();

        PresignedGetObjectRequest presignedRequest =
                s3Presigner.presignGetObject(presignRequest);
        // header에 CONTENT_DISPOSITION 설정을 통해 클릭 시 다운로드 진행
        return ResponseEntity.ok()
                .header(HttpHeaders.LOCATION, presignedRequest.url().toString())
                .build();
    }

    // 해당 파일명에 해당하는 이미지의 S3 URL 주소 반환
    public String getFullPath(String filename) {
        return "https://" + bucket + ".s3.amazonaws.com." + filename;
    }

    @Transactional
    public void deleteImage(UploadImage uploadImage) {
        uploadImageMapper.deleteById(uploadImage.getId());

        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(uploadImage.getSavedFilename())
                .build();

        s3Client.deleteObject(request);
    }
}
