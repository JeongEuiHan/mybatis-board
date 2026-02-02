package spboard.board.Repository;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import spboard.board.Domain.entity.UploadImage;

import java.util.Optional;

@Mapper
public interface UploadImageMapper {
    int insert(UploadImage uploadImage);

    Optional<UploadImage> findById(@Param("id") Long id);

    int deleteById(@Param("id") Long id);

    Optional<UploadImage> findBySavedFilename(@Param("savedFilename") String savedFilename);
}
