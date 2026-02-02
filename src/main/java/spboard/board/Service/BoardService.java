package spboard.board.Service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spboard.board.Domain.Dto.BoardCntDto;
import spboard.board.Domain.Dto.BoardDto;
import spboard.board.Domain.MapperDTO.BoardDeleteMeta;
import spboard.board.Domain.entity.*;
import spboard.board.Domain.enum_class.BoardCategory;
import spboard.board.Domain.enum_class.UserRole;
import spboard.board.Repository.BoardMapper;
import spboard.board.Repository.UserMapper;
import spboard.board.Domain.Dto.BoardCreateRequest;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BoardService {

    private final UploadImageService uploadImageService;
    private final BoardMapper boardMapper;
    private final UserMapper userMapper;

    public Page<Board> getBoardList(BoardCategory category, PageRequest pageRequest, String searchType, String keyword) {
        int offset = (int)pageRequest.getOffset();
        int limit = pageRequest.getPageSize();

        // 1. 정렬 조건 추출 (Sort 객체 활용)
        // 기본값은 id로 설정하고, PageRequest에 담긴 첫 번째 정렬 조건을 꺼냄
        String orderBy = "id";
        if (pageRequest.getSort().isSorted()) {
            String sortProperty = pageRequest.getSort().iterator().next().getProperty();
            // 자바 필드명 -> db 컬럼명 매핑
            if ("createdAt".equals(sortProperty)) orderBy = "created_at";
            else if ("likeCnt".equals(sortProperty)) orderBy = "like_cnt";
            else if ("commentCnt".equals(sortProperty)) orderBy = "comment_cnt";

        }
        boolean hasSearch = (searchType != null && keyword != null && !keyword.isBlank());

        List<Board> content = boardMapper.findPageByCategoryExcludeRole(
                category,
                UserRole.ADMIN,
                hasSearch ? searchType : null,
                hasSearch ? keyword : null,
                orderBy,
                offset,
                limit
        );

        long total = boardMapper.countPageByCategoryExcludeRole(
                category,
                UserRole.ADMIN,
                hasSearch ? searchType : null,
                hasSearch ? keyword : null
        );

        return new PageImpl<>(content, pageRequest, total);
    }

    public List<Board> getNotice(BoardCategory category) {
        return boardMapper.findAllByCategoryAndUserRole(category, UserRole.ADMIN);
    }

    public BoardDto getBoard(Long boardId, String category) {
        Optional<Board> optBoard = boardMapper.findById(boardId);

        // id에 해당하는 게시글이 없거나 카테고리가 일치하지 않으면 null return 대문자 소문자 무시
        if (optBoard.isEmpty() || !optBoard.get().getCategory().toString().equalsIgnoreCase(category)){
            return null;
        }

        return BoardDto.of(optBoard.get());
    }

    @Transactional
    public Long writeBoard(BoardCreateRequest request, BoardCategory category, String loginId, Authentication auth) throws IOException {
        User loginUser = userMapper.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalArgumentException("user not found"));

        Board board = request.toEntity(category, loginUser);
        board.setCreatedAt(LocalDateTime.now());
        board.setLastModifiedAt(LocalDateTime.now());
        boardMapper.insert(board);


        UploadImage uploadImage = uploadImageService.saveImage(request.getUploadImage(), board);
        if (uploadImage != null) {
            boardMapper.updateUploadImageId(board.getId(), uploadImage.getId());
        }

        if (category.equals(BoardCategory.GREETING)) {
            userMapper.updateRole(loginUser.getId(), UserRole.SILVER);
        }

        return board.getId();
    }

    @Transactional
    public Long editBoard(Long boardId, String category, BoardDto dto, String loginId) throws IOException {
        BoardDeleteMeta meta = boardMapper.findDeleteMetaById(boardId).orElse(null);
        if (meta == null || !meta.category().name().equalsIgnoreCase(category)) return null;

        // ✅ 작성자 본인 or ADMIN만 허용
        validateOwnerOrAdmin(meta.userId(), loginId);

        Board board = boardMapper.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

        // 이미지 교체 로직(기존 그대로)
        if (dto.getNewImage() != null && !dto.getNewImage().isEmpty()) {
            if (board.getUploadImage() != null) {
                boardMapper.updateUploadImageId(boardId, null);
                uploadImageService.deleteImage(board.getUploadImage());
            }
        }

        UploadImage uploadImage = uploadImageService.saveImage(dto.getNewImage(), board);
        if (uploadImage != null) {
            boardMapper.updateUploadImageId(boardId, uploadImage.getId());
        }

        boardMapper.updateContent(board.getId(), dto.getTitle(), dto.getBody(), LocalDateTime.now());
        return board.getId();
    }

    @Transactional
    public Long deleteBoard(Long boardId, String category, String loginId)  {

        BoardCategory reqCategory = BoardCategory.of(category);
        if (reqCategory == null) return null;

        BoardDeleteMeta meta = boardMapper.findDeleteMetaById(boardId).orElse(null);
        if (meta == null || meta.category() != reqCategory) return null;

        // ✅ 작성자 본인 or ADMIN만 허용
        validateOwnerOrAdmin(meta.userId(), loginId);

        if (meta.uploadImageId() != null) {
            boardMapper.updateUploadImageId(boardId, null);
        }

        userMapper.decreaseReceivedLikeCnt(meta.userId(), meta.likeCnt());
        boardMapper.deleteById(boardId);

        return boardId;
    }

    public String getCategory(Long boardId) {
        Board board = boardMapper.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("board not found"));

        return board.getCategory().name().toLowerCase();
    }

    public List<Board> findMyBoard(String category, String loginId) {
        if ("board".equals(category)) {
            return boardMapper.findAllByUserLoginId(loginId);
        } else if ("like".equals(category)) {
            return boardMapper.findBoardsLikedByUser(loginId);
        } else if ("comment".equals(category)) {
            return boardMapper.findBoardsCommentByUser(loginId);
        }
        return new ArrayList<>();
    }

    public BoardCntDto getBoardCnt(){
        return BoardCntDto.builder()
                .totalBoardCnt(boardMapper.countBoard())
                .totalNoticeCnt(boardMapper.countAllByUserRole(UserRole.ADMIN))
                .totalGreetingCnt(boardMapper.countAllByCategoryExcludeRole(BoardCategory.GREETING, UserRole.ADMIN))
                .totalFreeCnt(boardMapper.countAllByCategoryExcludeRole(BoardCategory.FREE, UserRole.ADMIN))
                .totalGoldCnt(boardMapper.countAllByCategoryExcludeRole(BoardCategory.GOLD, UserRole.ADMIN))
                .build();
    }

    public int getLikeCount(Long boardId) {
        return boardMapper.getLikeCount(boardId);
    }

    private void validateOwnerOrAdmin(Long ownerUserId, String loginId) {
        User loginUser = userMapper.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalArgumentException("user not found"));

        boolean isOwner = loginUser.getId().equals(ownerUserId);
        boolean isAdmin = loginUser.getUserRole() == UserRole.ADMIN;

        if (!isOwner && !isAdmin) {
            throw new org.springframework.security.access.AccessDeniedException("not owner");
        }
    }

}
