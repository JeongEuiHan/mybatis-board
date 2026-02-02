package spboard.board.Service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spboard.board.Domain.entity.Board;
import spboard.board.Domain.entity.User;
import spboard.board.Repository.BoardMapper;
import spboard.board.Repository.LikeMapper;
import spboard.board.Repository.UserMapper;

@Service
@RequiredArgsConstructor
public class LikeService {

    private final LikeMapper likeMapper;
    private final UserMapper userMapper;
    private final BoardMapper boardMapper;

    @Transactional
    public void addLike(String loginId, Long boardId) {
        Board board = boardMapper.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("게시판 없음"));
        User loginUser = userMapper.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalArgumentException("유저 없음"));
        User boardUser = board.getUser();

        // 자신이 누른 좋아요가 아니라면
        if (!boardUser.getId().equals(loginUser.getId())) {
            userMapper.incrementReceivedLikeCount(boardUser.getId());
        }

        boardMapper.incrementLikeCount(boardId);


        likeMapper.insert(loginUser.getId(), boardId);
    }

    @Transactional
    public void deleteLike(String loginId, Long boardId) {
        Board board = boardMapper.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("게시판 없음"));
        User loginUser = userMapper.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalArgumentException("유저 없음"));
        User boardUser = board.getUser();

        // 자신이 누른 좋아요가 아니라면
        if(!boardUser.getId().equals(loginUser.getId())) {
            userMapper.decrementReceivedLikeCount(boardUser.getId());
        }

        boardMapper.decrementLikeCount(boardId);

        likeMapper.deleteByUserLoginIdAndBoardId(loginId, boardId);
    }

    @Transactional(readOnly = true)
    public Boolean existsLike(String loginId, Long boardId) {
        return likeMapper.existsByUserLoginIdAndBoardId(loginId, boardId);
    }

    @Transactional // 중요! 두 작업이 하나로 묶여야 함
    public boolean toggleLike(String loginId, Long boardId) {
        User loginUser = userMapper.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalArgumentException("유저 없음"));
        Board board = boardMapper.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("유저 없음"));
        User boardUser = board.getUser();

        if (likeMapper.existsByUserLoginIdAndBoardId(loginId, boardId)) {
            // 이미 있으면 삭제
            likeMapper.deleteByUserLoginIdAndBoardId(loginId, boardId);
            boardMapper.updateLikeCount(boardId, -1); // 좋아요 수 감소
            if(!boardUser.getId().equals(loginUser.getId())) {
                userMapper.decrementReceivedLikeCount(boardUser.getId());
            }
            return false; // 이제 좋아요가 아님
        } else {
            // 없으면 추가
            likeMapper.insert(loginUser.getId(), boardId);
            boardMapper.updateLikeCount(boardId, 1); // 좋아요 수 증가
            if (!boardUser.getId().equals(loginUser.getId())) {
                userMapper.incrementReceivedLikeCount(boardUser.getId());
            }
            return true; // 이제 좋아요 상태임
        }
    }
}
