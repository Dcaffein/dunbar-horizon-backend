package com.example.DunbarHorizon.buzz.application.port.in;

import com.example.DunbarHorizon.buzz.application.port.in.command.CreateBuzzCommand;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface BuzzCommandUseCase {
    void createBuzz(CreateBuzzCommand command, List<MultipartFile> images);
    void replyToBuzz(Long replierId, String buzzId, String text, List<MultipartFile> images, boolean isPublic);
    void updateReply(Long requesterId, String buzzId, String replyId, String text, List<MultipartFile> images);
    void deleteReply(Long requesterId, String buzzId, String replyId);
    void deleteBuzz(Long requesterId, String buzzId);
}
