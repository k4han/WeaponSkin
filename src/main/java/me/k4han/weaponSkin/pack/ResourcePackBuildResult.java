package me.k4han.weaponSkin.pack;

import java.io.File;
import java.util.List;

public record ResourcePackBuildResult(
        File zipFile,
        String sha1,
        List<String> warnings
) {}

