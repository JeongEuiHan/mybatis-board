package spboard.board.Repository;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import spboard.board.Domain.entity.User;
import spboard.board.Domain.enum_class.UserRole;
import spboard.board.Domain.enum_class.UserStatus;

import java.util.List;
import java.util.Optional;

@Mapper
public interface UserMapper {
    Optional<User> findByLoginId(@Param("loginId") String loginId);

    boolean existsByLoginId(@Param("loginId") String loginId);

    boolean existsByNickname(@Param("nickname") String nickname);

    long countAllByUserRole(@Param("userRole") UserRole userRole);

    // JPA: findAllByNicknameContains(nickname, pageable)
    // MyBatis: page/size를 직접 받아서 LIMIT/OFFSET 처리
    List<User> findAllByNicknameContains(
            @Param("nickname") String nickname,
            @Param("userRole") UserRole userRole,
            @Param("offset") int offset,
            @Param("limit") int limit
    );

    long countAllByNicknameContains(@Param("nickname") String nickname);

    // 회원가입/수정용으로 흔히 필요한 기본 CRUD
    int insert(User user);

    int updateProfile(
            @Param("id") Long id,
            @Param("password") String password,
            @Param("nickname") String nickname
    );

    int updateRole(@Param("id") Long id, @Param("userRole") UserRole userRole);

    int updateReceivedLikeCnt(@Param("id") Long id, @Param("receivedLikeCnt") int receivedLikeCnt);

    Optional<User> findById(@Param("id") Long id);

    int updateStatus(
            @Param("id") Long id,
            @Param("status") UserStatus status
            );

    long countActive();

    int decreaseReceivedLikeCnt(@Param("userId") Long userId, @Param("delta") int delta);

    int incrementReceivedLikeCount(@Param("userId") Long userId);

    int decrementReceivedLikeCount(@Param("userId") Long userId);

    int countByLoginId(@Param("loginId") String loginId);

}
