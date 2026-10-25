# Texture tools

Single-file Java programs (run with `java`, no build step). Both read vanilla textures, so they need `vanillaTexturesDir`: the `assets/minecraft/textures` folder of an extracted Minecraft client jar.

- `TrimGen` builds the golem trim textures from the vanilla armour trims.
- `SurveyGen` builds the Survey Table textures.

```
java tools/TrimGen.java <vanillaTexturesDir> <outDir> <previewDir> [TOP|BOTTOM|SCALE|MID]
java tools/SurveyGen.java <vanillaTexturesDir> <outDir> <previewFile>
```
