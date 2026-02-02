package spboard.board.Repository;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import spboard.board.Domain.entity.Like;

import java.util.List;

@Mapper
public interface LikeMapper {

    int insert(@Param("userId") Long id, @Param("boardId") Long boardId);

    int deleteByUserLoginIdAndBoardId(@Param("loginId") String loginId,
                                      @Param("boardId") Long boardId);

    boolean existsByUserLoginIdAndBoardId(@Param("loginId") String logingId,
                                          @Param("boardId") Long boardId);

    List<Like> findAllByUserLoginId(@Param("loginId") String loginId);

    int deleteByLoginId(@Param("loginId") String loginId);


}
