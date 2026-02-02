package spboard.board.Controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import spboard.board.Service.BoardService;
import spboard.board.Service.LikeService;

@Controller
@RequestMapping("/likes")
@RequiredArgsConstructor
public class LikeController {

    private final LikeService likeService;
    private final BoardService boardService;

    @PostMapping("/add/{boardId}")
    public String addLike(@PathVariable Long boardId,
                          @RequestParam String category,
                          Authentication auth,
                          Model model) {
        likeService.addLike(auth.getName(), boardId);
        return "redirect:/boards/" + boardService.getCategory(boardId) + "/" + boardId;
    }

    @PostMapping("/delete/{boardId}")
    public String deleteLike(@PathVariable Long boardId,
                             @RequestParam String category,
                             Authentication auth,
                             Model model) {
        likeService.deleteLike(auth.getName(), boardId);
        return "redirect:/boards/" + boardService.getCategory(boardId) + "/" + boardId;
    }
}
