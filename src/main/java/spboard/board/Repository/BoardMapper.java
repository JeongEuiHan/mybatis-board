package spboard.board.Repository;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import spboard.board.Domain.MapperDTO.BoardDeleteMeta;
import spboard.board.Domain.entity.Board;
import spboard.board.Domain.enum_class.BoardCategory;
import spboard.board.Domain.enum_class.UserRole;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Mapper
public interface BoardMapper {

    List<Board> findPageByCategoryExcludeRole(
            @Param("category")BoardCategory category,
            @Param("excludeRole")UserRole excludeRole,  // ADMIN 제외
            @Param("searchType") String searchType,
            @Param("keyword") String keyword,           // null/"" 허용
            @Param("orderBy") String orderBy,
            @Param("offset") int offset,
            @Param("limit") int limit
    );

    long countPageByCategoryExcludeRole(
            @Param("category") BoardCategory category,
            @Param("excludeRole") UserRole excludeRole,
            @Param("searchType") String searchType,
            @Param("keyword") String keyword
    );

    Optional<Board> findById(@Param("id") Long id);

    // 공지글(ADMINT)만
    List<Board> findAllByCategoryAndUserRole(
            @Param("category") BoardCategory category,
            @Param("userRole") UserRole userRole
    );

    long countAllByUserRole(@Param("userRole") UserRole userRole);

    long countAllByCategoryExcludeRole(
            @Param("category") BoardCategory category,
            @Param("excludeRole") UserRole userRole
    );

    long countBoard();

    // 기본 CRUD
    int insert(Board board);
    int update(Board board);
    int deleteById(@Param("id") Long id);
    int updateUploadImageId(@Param("boardId") Long boardId, @Param("uploadImageId") Long uploadImageId);
    int updateContent(@Param("id") Long id,
                      @Param("title") String title,
                      @Param("body") String body,
                      @Param("lastModifiedAt")LocalDateTime lastModifiedAt
                      );

    Optional<BoardDeleteMeta> findDeleteMetaById(@Param("id") Long id);

    int updateLikeCnt(@Param("id") Long id, @Param("likeCnt") int likeCnt);
    int updateCommentCnt(@Param("id") Long id, @Param("commentCnt") int commentCnt);

    int incrementCommentCount(@Param("id") Long id);
    int decrementCommentCount(@Param("id") Long id);

    int incrementLikeCount(@Param("id") Long id);
    int decrementLikeCount(@Param("id") Long id);
    List<Board> findAllByUserLoginId(@Param("loginId") String loginId);

    List<Board> findBoardsLikedByUser(@Param("loginId") String loginId);
    List<Board> findBoardsCommentByUser(@Param("loginId") String loginId);

    int decreaseLikeCountByUser(@Param("loginId") String loginId);
    int decreaseCommentCountByUser(@Param("loginId") String loginId);

    int getLikeCount(@Param("boardId") Long id);

    void updateLikeCount(@Param("boardId") Long boardId, @Param("amount") int amount);
}
