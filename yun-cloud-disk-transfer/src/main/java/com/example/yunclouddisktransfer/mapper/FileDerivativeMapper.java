package com.example.yunclouddisktransfer.mapper;

import com.example.yunclouddisktransfer.entity.FileDerivative;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FileDerivativeMapper {
    @Insert("INSERT INTO file_derivative (origin_file_id, type, size, s3_path, format, created_at) VALUES (#{originFileId}, #{type}, #{size}, #{s3Path}, #{format}, NOW())")
    void insert(FileDerivative fileDerivative);
} 