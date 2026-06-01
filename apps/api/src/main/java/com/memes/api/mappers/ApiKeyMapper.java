package com.memes.api.mappers;

import com.memes.api.models.ApiKey;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Optional;

@Mapper
public interface ApiKeyMapper {

    @Select("SELECT id, key_hash, client_name, role, active, expires_at, created_at, last_used_at "
          + "FROM api_keys WHERE key_hash = #{keyHash}")
    Optional<ApiKey> selectByKeyHash(String keyHash);

    @Select("SELECT id, key_hash, client_name, role, active, expires_at, created_at, last_used_at "
          + "FROM api_keys WHERE active = true ORDER BY created_at DESC")
    List<ApiKey> selectAllActive();

    @Insert("INSERT INTO api_keys (key_hash, client_name, role, active, expires_at) "
          + "VALUES (#{keyHash}, #{clientName}, #{role}, #{active}, #{expiresAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    void insert(ApiKey apiKey);

    @Update("UPDATE api_keys SET last_used_at = NOW() WHERE id = #{id}")
    void updateLastUsed(Long id);

    @Update("UPDATE api_keys SET active = false WHERE id = #{id}")
    int deactivate(Long id);

    @Select("SELECT COUNT(*) FROM api_keys WHERE active = true AND role = 'ADMIN'")
    int countActiveAdminKeys();

    @Select("SELECT key_hash FROM api_keys WHERE id = #{id}")
    Optional<String> selectKeyHashById(Long id);
}
