package spboard.board.Service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import spboard.board.Domain.entity.User;
import spboard.board.Domain.enum_class.UserRole;
import spboard.board.Domain.Dto.UserCntDto;
import spboard.board.Domain.Dto.UserDto;
import spboard.board.Domain.enum_class.UserStatus;
import spboard.board.Repository.BoardMapper;
import spboard.board.Repository.CommentMapper;
import spboard.board.Repository.LikeMapper;
import spboard.board.Repository.UserMapper;
import spboard.board.Domain.Dto.UserJoinRequest;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final BCryptPasswordEncoder encoder;
    private final UserMapper userMapper;
    private final LikeMapper likeMapper;
    private final CommentMapper commentMapper;
    private final BoardMapper boardMapper;

    public BindingResult joinValid(UserJoinRequest request, BindingResult bindingResult)
    {
        if (request.getLoginId().isEmpty()) {
            bindingResult.addError(new FieldError("request", "loginId", "아이디가 비어있습니다."));
        } else if (request.getLoginId().length() > 10) {
            bindingResult.rejectValue(
                    "loginId",
                    "length.exceeded",
                    "아이디가 10자가 넘습니다."
            );
        } else if (userMapper.existsByLoginId(request.getLoginId())) {
            bindingResult.rejectValue(
                    "loginId",
                    "loginId.duplicate",
                    "이미 사용 중인 아이디입니다."
            );
        }

        if (request.getPassword().isEmpty()) {
            bindingResult.rejectValue(
                    "password",
                    "password.empty",
                    "비밀번호가 비어있습니다."
            );
        }

        if (!request.getPassword().equals(request.getPasswordCheck())) {
            bindingResult.rejectValue(
                    "passwordCheck",
                    "password.mismatch",
                    "비밀번호가 일치하지 않습니다."
            );
        }

        if (request.getNickname().isEmpty()) {
            bindingResult.rejectValue(
                    "nickname",
                    "nickname.empty",
                    "닉네임이 비어있습니다."
            );
        } else if (request.getNickname().length() > 10) {
            bindingResult.rejectValue(
                    "nickname",
                    "length.exceeded",
                    "닉네임이 10자가 넘습니다."
            );
        } else if (userMapper.existsByNickname(request.getNickname())) {
            bindingResult.rejectValue(
                    "nickname",
                    "nickname.duplicate",
                    "닉네임이 중복됩니다."
            );
        }

        return bindingResult;
    }

    public void join(UserJoinRequest request) {
        userMapper.insert(request.toEntity(encoder.encode(request.getPassword())));
    }

    public User myInfo(String loginId) {
        return userMapper.findByLoginId(loginId).get();
    }

    public BindingResult editValid(UserDto dto, BindingResult bindingResult, String loginId) {
        User longinUser = userMapper.findByLoginId(loginId).get();

        if (dto.getNowPassword().isEmpty()) {
            bindingResult.addError(new FieldError("dto","nowPassword","현재 비밀번호가 비어 있습니다"));
        } else if (!encoder.matches(dto.getNowPassword(), longinUser.getPassword())) {
            bindingResult.addError(new FieldError("dto", "nowPassword", "현재 비밀번호가 틀렸습니다."));
        }

        if (!dto.getNewPassword().equals(dto.getNewPasswordCheck())) {
            bindingResult.addError(new FieldError("dto", "newPasswordCheck", "비밀번호가 일치하지 않습니다."));
        }

        if (dto.getNickname().isEmpty()) {
            bindingResult.addError(new FieldError("dto", "nickname", "닉네임이 비어있습니다."));
        } else if (dto.getNickname().length() > 10) {
            bindingResult.addError(new FieldError("dto", "nickname", "닉네임이 10자가 넘습니다."));
        } else if (!dto.getNickname().equals(longinUser.getNickname()) && userMapper.existsByNickname(dto.getNickname())) {
            bindingResult.addError(new FieldError("dto", "nickname", "닉네임이 중복됩니다."));
        }

        return bindingResult;
    }

    @Transactional
    public void edit(UserDto dto, String loginId) {
        User loginUser = userMapper.findByLoginId(loginId).get();

        if (dto.getNewPassword().equals("")) {
            userMapper.updateProfile(loginUser.getId(), loginUser.getPassword(), loginUser.getNickname());
        } else {
            userMapper.updateProfile(loginUser.getId(), encoder.encode(dto.getNewPassword()), dto.getNickname());
        }
    }


    @Transactional
    public Boolean delete(String loginId, String nowPassword) {
        User loginuser = userMapper.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다."));

        if(encoder.matches(nowPassword, loginuser.getPassword())) {
            //게시글 카운트 깎기
            boardMapper.decreaseLikeCountByUser(loginId);
            boardMapper.decreaseCommentCountByUser(loginId);

            //실제 댓글 데이터 지우기
            // commentMapper.deleteByLoginId(loginId);

            //실제 좋아요 데이터 지우기
            // likeMapper.deleteByLoginId(loginId);

            userMapper.updateStatus(loginuser.getId(), UserStatus.DELETED);
            return true;
        } else {
            return false;
        }
    }


    public Page<User> findAllByNickName(String keyword, PageRequest pageRequest) {
        int page = pageRequest.getPageNumber();
        int size = pageRequest.getPageSize();

        int offset = page*size;

        List<User> users = userMapper.findAllByNicknameContains(keyword, UserRole.ADMIN, offset, size);

        long total = userMapper.countAllByNicknameContains(keyword);

        return new PageImpl<>(users, pageRequest, total);
    }

    @Transactional
    public void changeRole(Long userId) {
        User user = userMapper.findById(userId).get();

        UserRole nextRole = user.changeRole();

        userMapper.updateRole(user.getId(), nextRole);

    }

    public UserCntDto getUserCnt() {
        return UserCntDto.builder()
                .totalUserCnt(userMapper.countActive())
                .totalAdminCnt(userMapper.countAllByUserRole(UserRole.ADMIN))
                .totalBronzeCnt(userMapper.countAllByUserRole(UserRole.BRONZE))
                .totalSilverCnt(userMapper.countAllByUserRole(UserRole.SILVER))
                .totalGoldCnt(userMapper.countAllByUserRole(UserRole.GOLD))
                .totalBlacklistCnt(userMapper.countAllByUserRole(UserRole.BLACKLIST))
                .build();
    }

    public boolean checkLoginIdDuplicate(String loginId) {
        return userMapper.countByLoginId(loginId) > 0;
    }
}
