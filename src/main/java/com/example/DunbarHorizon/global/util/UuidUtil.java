package com.example.DunbarHorizon.global.util;

import com.fasterxml.uuid.Generators;
import java.util.UUID;

public class UuidUtil {

    public static UUID createV7() {
        return Generators.timeBasedEpochGenerator().generate();
    }
}