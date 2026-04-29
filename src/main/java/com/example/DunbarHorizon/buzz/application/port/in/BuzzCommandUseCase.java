package com.example.DunbarHorizon.buzz.application.port.in;

import com.example.DunbarHorizon.buzz.application.port.in.command.CreateBuzzCommand;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface BuzzCommandUseCase {
    void createBuzz(CreateBuzzCommand command, List<MultipartFile> images);
    void commentOnBuzz(Long commenterId, String buzzId, String text, List<MultipartFile> images, boolean isPublic);
    void updateComment(Long requesterId, String buzzId, String commentId, String text, List<MultipartFile> images);
    void deleteComment(Long requesterId, String buzzId, String commentId);
    void deleteBuzz(Long requesterId, String buzzId);
}
