package spboard.board.config.auth;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import spboard.board.Domain.entity.User;
import spboard.board.Domain.enum_class.UserRole;
import spboard.board.Domain.enum_class.UserStatus;
import spboard.board.Repository.UserMapper;

import java.io.IOException;

@AllArgsConstructor
public class MyLoginSuccessHandler implements AuthenticationSuccessHandler {
    private final UserMapper userMapper;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        // 세션 유지 시간 = 3600초
        HttpSession session = request.getSession();
        session.setMaxInactiveInterval(3600);

        User loginUser = userMapper.findByLoginId(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("사용자 정보를 찾을 수 없습니다."));


        if (loginUser.getStatus() == UserStatus.DELETED) {
            session.invalidate(); // 생성된 세션 무효화
            response.sendRedirect("/users/login?fail=deleted"); // 탈퇴 유저 전용 에러 메시지
            return; // 이후 로직 실행 방지
        }

        // 성공 시 메시지 출력 후 홈 화면으로 redirect
        String prevPage = (String) session.getAttribute("prevPage");

        String redirectUrl = (prevPage != null) ? prevPage : "/";
        if (loginUser.getUserRole() == UserRole.BLACKLIST) {
            response.sendRedirect(redirectUrl + "?blacklist=true");
        } else {
            response.sendRedirect(redirectUrl + "?loginSuccess=true");
        }

    }
}
