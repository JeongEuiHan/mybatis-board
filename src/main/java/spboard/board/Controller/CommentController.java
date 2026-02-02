package spboard.board.Controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import spboard.board.Domain.Dto.CommentCreateRequest;
import spboard.board.Service.BoardService;
import spboard.board.Service.CommentService;

@Controller
@RequestMapping("/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;
    private final BoardService boardService;

    @PostMapping("/{boardId}")
    private String addComments(@PathVariable Long boardId, @ModelAttribute CommentCreateRequest request,
                               Authentication auth, Model model) {
        commentService.writeComment(boardId, request, auth.getName());

        model.addAttribute("message", "댓글이 추가되었습니다.");
        model.addAttribute("nextUrl", "/boards/" + boardService.getCategory(boardId) + "/" + boardId);
        return "printMessage";
    }

    @PostMapping("/{commentId}/edit")
    public String editComment(@PathVariable Long commentId, @ModelAttribute CommentCreateRequest request,
                            Authentication auth, Model model) {
        try {
            Long boardId = commentService.editComment(commentId, request.getBody(), auth.getName());

            if (boardId == null) {
                model.addAttribute("message", "댓글이 존재하지 않습니다.");
                model.addAttribute("nextUrl", "/boards");
            } else {
                model.addAttribute("message", "댓글이 수정되었습니다.");
                model.addAttribute("nextUrl", "/boards/" + boardService.getCategory(boardId) + "/" + boardId);
            }
            return "printMessage";

        } catch (org.springframework.security.access.AccessDeniedException e) {
            model.addAttribute("message", "댓글 수정 권한이 없습니다.");
            model.addAttribute("nextUrl", "/boards");
            return "printMessage";
        }
    }

    @GetMapping("/{commentId}/delete")
    public String deleteComment(@PathVariable Long commentId, Authentication auth, Model model) {
        try {
            Long boardId = commentService.deleteComment(commentId, auth.getName());

            if (boardId == null) {
                model.addAttribute("message", "댓글이 존재하지 않습니다.");
                model.addAttribute("nextUrl", "/boards");
            } else {
                model.addAttribute("message", "댓글이 삭제되었습니다.");
                model.addAttribute("nextUrl", "/boards/" + boardService.getCategory(boardId) + "/" + boardId);
            }
            return "printMessage";

        } catch (org.springframework.security.access.AccessDeniedException e) {
            model.addAttribute("message", "댓글 삭제 권한이 없습니다.");
            model.addAttribute("nextUrl", "/boards");
            return "printMessage";
        }
    }
}
