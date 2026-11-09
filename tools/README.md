# Generators

## Textures
Single-file Java programs (run with `java`, no build step). Most read vanilla textures, such as `vanillaTexturesDir`: the `assets/minecraft/textures` folder of an extracted Minecraft client jar.

- `TrimGen` builds the golem trim textures from the vanilla armour trims.
- `SurveyGen` builds the Survey Table textures.
- `PadGen` builds the Charging Pad textures.
- `HandbookGen` builds the Golem Handbook item texture.
- `QuotaSlipGen` builds the Quota Slip item texture.
- `GolemEyesGen` builds the eye and antenna overlay textures.
- `AutoTexGen` builds the Automatisation add-on's textures.

```
java tools/TrimGen.java <vanillaTexturesDir> <outDir> <previewDir> [TOP|BOTTOM|SCALE|MID]
java tools/SurveyGen.java <vanillaTexturesDir> <outDir> <previewFile>
```

The other programs' arguments are in their source.

## Wiki
`python tools/gen_wiki_icons.py <minecraft-client.jar>`, run from the repository root with Pillow, writes `wiki/public/icons/`. Add an item the wiki should show to `EXTRA` in the script.

## Resources
Both overwrite the files they generate, so make changes in the scripts.

- `python tools/gen_guide.py`, run from the repository root, writes advancements, the handbook and all guide texts for both mods. Its lang entries are merged into the existing lang files.
- `python tools/gen_automatisation_resources.py automatisation/src/main/resources` writes the add-on's blockstates, models, item definitions, loot tables, recipes and tags.
