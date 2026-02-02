package spboard.board.Service;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriUtils;
import spboard.board.Domain.entity.Board;
import spboard.board.Domain.entity.UploadImage;
import spboard.board.Repository.BoardMapper;
import spboard.board.Repository.UploadImageMapper;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UploadImageService {

    private final UploadImageMapper uploadImageMapper;
    private final BoardMapper boardMapper;
    private final String rootPath = System.getProperty("user.dir"); // 현재 프로젝트의 루트 경로
    private final String fileDir = rootPath + "/src/main/resources/static/upload-images/"; // 이미지 파일이 실제로 저장될 서버 내부 경로

    public String getFullPath(String filename) {
        return fileDir + filename;
    }

    public UploadImage saveImage(MultipartFile multipartFile, Board board) throws IOException {
        if (multipartFile.isEmpty()) {
            return null;
        }

        String originalFilename = multipartFile.getOriginalFilename(); // 클라이언트가 업로드한 실제 파일 이름
        // 원본 파일명 -> 서버에 저장된 파일명 ( 중복 X)
        // 파일명이 중복되지 않도록 UUID로 설정 + 확장자 유지
        String savedFilename = UUID.randomUUID() + "." + extractExt(originalFilename);

        // 파일 저장
        multipartFile.transferTo(new File(getFullPath(savedFilename)));

        UploadImage uploadImage = UploadImage.builder()
                .originalFilename(originalFilename)
                .savedFilename(savedFilename)
                .build();

        uploadImageMapper.insert(uploadImage);

        return uploadImage;
    }

    @Transactional
    public void deleteImage(UploadImage uploadImage) throws IOException {
        uploadImageMapper.deleteById(uploadImage.getId());
        Files.deleteIfExists(Paths.get(getFullPath(uploadImage.getSavedFilename())));
    }

    // 확장자 추출
    private String extractExt(String originalFilename){
        int pos = originalFilename.lastIndexOf("."); // 파일명에서 마지막 "."의 위치를 찾습니다.
        return originalFilename.substring(pos + 1); // "." 다음 문자부터 끝까지 잘라랩냅니다.
    }

    public ResponseEntity<UrlResource> downloadImage(Long boardId) throws MalformedURLException {
        // boardId에 해당하는 게시글이 없으면 null return
        Board board = boardMapper.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("board not found"));

        if (board == null || board.getUploadImage() == null) {
            return ResponseEntity.notFound().build();
        }

        UrlResource urlResource = new UrlResource("file:" + getFullPath(board.getUploadImage().getSavedFilename())); // 서버에 저장된 실제 파일을 가리키는 객체 생성

        // 업로드 한 파일명이 한글인 경우 아래 작업을 안해주면 한글이 깨질 수 있음
        String encodedUploadFileName = UriUtils.encode(board.getUploadImage().getOriginalFilename(), StandardCharsets.UTF_8);
        String contentDisposition = "attachment; filename=\"" + encodedUploadFileName + "\"";

        // header에 CONTENT_DISPOSITION 설정을 통해 클릭 시 다운로드 진행
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                .body(urlResource);
    }
}
