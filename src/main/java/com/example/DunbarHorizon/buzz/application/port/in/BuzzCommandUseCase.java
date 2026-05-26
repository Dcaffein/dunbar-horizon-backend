package com.example.DunbarHorizon.buzz.application.port.in;

import com.example.DunbarHorizon.buzz.application.port.in.command.CreateBuzzCommand;

import java.util.List;

public interface BuzzCommandUseCase {
    void createBuzz(CreateBuzzCommand command, List<String> imageKeys);
    void commentOnBuzz(Long commenterId, String buzzId, String text, List<String> imageKeys, boolean isPublic);
    void updateComment(Long requesterId, String buzzId, String commentId, String text, List<String> imageKeys);
    void deleteComment(Long requesterId, String buzzId, String commentId);
    void deleteBuzz(Long requesterId, String buzzId);
}
