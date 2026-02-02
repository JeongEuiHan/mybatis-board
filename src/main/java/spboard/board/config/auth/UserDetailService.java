package spboard.board.config.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import spboard.board.Domain.entity.User;
import spboard.board.Domain.enum_class.UserStatus;
import spboard.board.Repository.UserMapper;

@Service
@RequiredArgsConstructor
public class UserDetailService implements UserDetailsService {

    private final UserMapper userMapper;
    @Override

    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userMapper.findByLoginId(username)
                .orElseThrow(() -> new UsernameNotFoundException("해당 유저를 찾을 수 없습니다."));

        if (user.getStatus() == UserStatus.DELETED) {
            throw new DisabledException("탈퇴한 계정입니다.");
        }
        //Security의 세션에 유저 정보가 저장됨
        return new UserDetail(user);
    }
}
