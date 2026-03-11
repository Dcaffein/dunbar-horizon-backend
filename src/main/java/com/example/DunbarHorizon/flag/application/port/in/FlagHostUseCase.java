package com.example.DunbarHorizon.flag.application.port.in;

import com.example.DunbarHorizon.flag.application.port.in.command.FlagEncoreCommand;
import com.example.DunbarHorizon.flag.application.port.in.command.FlagHostCommand;

public interface FlagHostUseCase {
    Long hostFlag(FlagHostCommand command);
    Long encoreFlag(FlagEncoreCommand command);
}