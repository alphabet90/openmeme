package com.memes.api.mappers;

import com.memes.api.models.ApiKey;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface ApiKeyMapper {
    Optional<ApiKey> findByKeyHash(@Param("hash") String hash);
    List<ApiKey> findAllActive();
    int countActiveAdminKeys();
    int insert(ApiKey apiKey);
    int deactivate(@Param("id") long id);
    int updateLastUsed(@Param("id") long id);
}
