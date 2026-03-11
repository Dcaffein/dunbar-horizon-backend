package com.example.DunbarHorizon.buzz.application.port.in;

import com.example.DunbarHorizon.buzz.application.port.in.command.CreateBuzzCommand;

import java.util.List;

public interface BuzzCommandUseCase {
    void createBuzz(CreateBuzzCommand command);
    void replyToBuzz(Long replierId, String buzzId, String text, List<String> imageUrls, boolean isPublic);
    void updateReply(Long requesterId, String buzzId, String replyId, String text, List<String> imageUrls);
    void deleteReply(Long requesterId, String buzzId, String replyId);
    void deleteBuzz(Long requesterId, String buzzId);
}
