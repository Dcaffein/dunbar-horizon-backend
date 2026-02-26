package com.example.GooRoomBe.flag.application.port.in;

import com.example.GooRoomBe.flag.application.command.FlagEncoreCommand;
import com.example.GooRoomBe.flag.application.command.FlagHostCommand;

public interface FlagHostUseCase {
    Long hostFlag(FlagHostCommand command);
    Long encoreFlag(FlagEncoreCommand command);
}