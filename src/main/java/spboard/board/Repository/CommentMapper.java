package spboard.board.Repository;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import spboard.board.Domain.MapperDTO.CommentMeta;
import spboard.board.Domain.entity.Comment;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Mapper
public interface CommentMapper {
    int insert(Comment comment);

    Optional<Comment> findById(@Param("id") Long id);

    // 게시글별 댓글 목록
    List<Comment> findAllByBoardId(@Param("boardId") Long boardId);

    // 특정 유저가 작성한 댓글 목록
    List<Comment> findAllByUserLoginId(@Param("loginId") String loginId);

    int updateBody(@Param("id") Long id, @Param("body") String body,
                   @Param("lastModifiedAt")LocalDateTime lastModifiedAt);

    int deleteById(@Param("id") Long id);
    int deleteByLoginId(@Param("loginId") String loginId);

    Optional<CommentMeta> findMetaById(Long commentId);

}
