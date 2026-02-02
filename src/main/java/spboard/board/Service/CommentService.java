package spboard.board.Service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spboard.board.Domain.MapperDTO.CommentMeta;
import spboard.board.Domain.entity.Board;
import spboard.board.Domain.entity.Comment;
import spboard.board.Domain.entity.User;
import spboard.board.Domain.enum_class.UserRole;
import spboard.board.Repository.BoardMapper;
import spboard.board.Repository.CommentMapper;
import spboard.board.Repository.UserMapper;
import spboard.board.Domain.Dto.CommentCreateRequest;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentMapper commentMapper;
    private final BoardMapper boardMapper;
    private final UserMapper userMapper;

    public void writeComment(Long boardId, CommentCreateRequest request, String loginId) {
        Board board = boardMapper.findById(boardId).orElseThrow(() -> new IllegalArgumentException("게시판 없음"));
        User user = userMapper.findByLoginId(loginId).orElseThrow(() -> new IllegalArgumentException(" 유저 없음"));

        boardMapper.incrementCommentCount(boardId);

        commentMapper.insert(request.toEntity(board, user, LocalDateTime.now(), LocalDateTime.now()));
    }

    public List<Comment> findAll(Long boardId) { return commentMapper.findAllByBoardId(boardId);
    }

    @Transactional
    public Long editComment(Long commentId, String newBody, String loginId) {
        CommentMeta meta = commentMapper.findMetaById(commentId).orElse(null);
        if (meta == null) return null;

        validateOwnerOrAdmin(meta.userId(), loginId);

        commentMapper.updateBody(commentId, newBody, LocalDateTime.now());
        return meta.boardId(); // 수정 후 어디로 돌아갈지에 유용
    }

    @Transactional
    public Long deleteComment(Long commentId, String loginId) {
        CommentMeta meta = commentMapper.findMetaById(commentId).orElse(null);
        if (meta == null) return null;

        validateOwnerOrAdmin(meta.userId(), loginId);

        commentMapper.deleteById(commentId);
        boardMapper.decrementCommentCount(meta.boardId());
        return meta.boardId();
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
