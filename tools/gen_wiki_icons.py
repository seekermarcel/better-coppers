#!/usr/bin/env python3
"""Writes wiki/public/icons/<id>.png for every item the wiki shows.

Usage, from the repository root: python tools/gen_wiki_icons.py <minecraft-client.jar>
Needs Pillow. Blocks come out as isometric cubes of their faces, so shaped blocks are approximate.
"""
import glob
import io
import json
import sys
import zipfile
from pathlib import Path

from PIL import Image

RESOURCES = ["src/main/resources", "automatisation/src/main/resources"]
OUT = Path("wiki/public/icons")
SIZE = 64
# Items the articles mention that no recipe uses.
EXTRA = ["minecraft:" + i for i in """copper_pickaxe copper_axe copper_hoe bundle fishing_rod brush crafting_table
	shears iron_ingot gold_block diamond_block iron_chain copper_chain copper_golem_spawn_egg carved_pumpkin
	barrel oak_boat dispenser gold_ingot sugar_cane""".split()]
# Tags in recipes, shown as one of their members. Keep in step with the labels in recipes.data.mts.
TAGS = {"#minecraft:planks": "minecraft:oak_planks", "#minecraft:copper_chests": "minecraft:copper_chest", "#c:chests/wooden": "minecraft:chest"}
# Chests are drawn from entity textures and have no block model faces.
CHESTS = {"minecraft:copper_chest": "copper", "minecraft:chest": "normal"}

jar = zipfile.ZipFile(sys.argv[1])


def read(ns, path):
	"""Bytes of an asset from the mods' resources or the game jar."""
	for res in RESOURCES:
		for f in glob.glob(f"{res}/assets/{ns}/{path}"):
			return Path(f).read_bytes()
	return jar.read(f"assets/{ns}/{path}")


def split(ident, default="minecraft"):
	return ident.split(":", 1) if ":" in ident else (default, ident)


def texture(ident):
	ns, path = split(ident)
	img = Image.open(io.BytesIO(read(ns, f"textures/{path}.png"))).convert("RGBA")
	# Animated textures are vertical strips; the first frame is enough.
	return img.crop((0, 0, img.width, img.width))


def model_textures(ident):
	"""A model's textures with its parents' merged in, and its root parent."""
	ns, path = split(ident)
	model = json.loads(read(ns, f"models/{path}.json"))
	found = dict(model.get("textures", {}))
	root = ident
	if "parent" in model and not model["parent"].startswith("builtin/"):
		inherited, root = model_textures(model["parent"])
		found = {**inherited, **found}
	return found, root


def first_model(node):
	"""The first model reference in an item definition, which may nest conditions."""
	if isinstance(node, dict):
		if isinstance(node.get("model"), str):
			return node["model"]
		for value in node.values():
			if (found := first_model(value)):
				return found
	elif isinstance(node, list):
		for value in node:
			if (found := first_model(value)):
				return found


def pick(textures, *keys):
	for key in keys:
		value = textures.get(key)
		while value and value.startswith("#"):
			value = textures.get(value[1:])
		if value:
			return value


def shade(img, factor):
	r, g, b, a = img.split()
	return Image.merge("RGBA", (*(c.point(lambda v: int(v * factor)) for c in (r, g, b)), a))


def project(face, origin, across, down):
	"""A square face drawn onto a canvas as the parallelogram its origin and edges describe."""
	(ax, ay), (dx, dy) = across, down
	det = ax * dy - dx * ay
	w = face.width
	# AFFINE maps canvas pixels back to face pixels, so it takes the inverse of the edges.
	a, b, d, e = dy * w / det, -dx * w / det, -ay * w / det, ax * w / det
	c = -(a * origin[0] + b * origin[1])
	f = -(d * origin[0] + e * origin[1])
	return face.transform((SIZE, SIZE), Image.AFFINE, (a, b, c, d, e, f), Image.NEAREST)


def cube(top, left, right):
	"""An isometric cube, shaded the way inventories shade blocks."""
	s = SIZE
	canvas = Image.new("RGBA", (s, s))
	canvas.alpha_composite(project(top, (s / 2, 0), (s / 2, s / 4), (-s / 2, s / 4)))
	canvas.alpha_composite(project(shade(left, 0.8), (0, s / 4), (s / 2, s / 4), (0, s / 2)))
	canvas.alpha_composite(project(shade(right, 0.6), (s / 2, s / 2), (s / 2, -s / 4), (0, s / 2)))
	return canvas


def chest(kind):
	"""The front of a chest: the lid's front strip above the base's."""
	sheet = texture_full(f"minecraft:entity/chest/{kind}")
	scale = sheet.width // 64
	lid = sheet.crop((14 * scale, 14 * scale, 28 * scale, 19 * scale))
	base = sheet.crop((14 * scale, 33 * scale, 28 * scale, 43 * scale))
	front = Image.new("RGBA", (14 * scale, 15 * scale))
	front.paste(lid, (0, 0))
	front.paste(base, (0, 5 * scale))
	return front.resize((SIZE - 8, SIZE - 8), Image.NEAREST)


def texture_full(ident):
	ns, path = split(ident)
	return Image.open(io.BytesIO(read(ns, f"textures/{path}.png"))).convert("RGBA")


def icon(ident):
	if ident in CHESTS:
		return chest(CHESTS[ident])
	ns, name = split(ident)
	model = first_model(json.loads(read(ns, f"items/{name}.json")))
	textures, _ = model_textures(model)
	if "layer0" in textures:
		flat = Image.new("RGBA", (16, 16))
		for layer in sorted(k for k in textures if k.startswith("layer")):
			flat.alpha_composite(texture(pick(textures, layer)).resize((16, 16), Image.NEAREST))
		return flat.resize((SIZE, SIZE), Image.NEAREST)
	top = pick(textures, "up", "top", "end", "belt", "mouth", "all", "texture", "copper", "side", "particle")
	left = pick(textures, "north", "front", "side", "all", "texture", "copper", "particle")
	right = pick(textures, "east", "side", "all", "texture", "copper", "particle")
	return cube(texture(top), texture(left), texture(right))


def recipe_ids():
	for res in RESOURCES:
		for f in glob.glob(f"{res}/data/*/recipe/*.json"):
			recipe = json.loads(Path(f).read_text())
			yield recipe["result"]["id"]
			yield from recipe.get("key", {}).values()
			yield from recipe.get("ingredients", [])


def mod_ids():
	for res in RESOURCES:
		for f in glob.glob(f"{res}/assets/*/items/*.json"):
			yield f"{Path(f).parts[-3]}:{Path(f).stem}"


OUT.mkdir(parents=True, exist_ok=True)
for ident in sorted({TAGS.get(i, i) for i in [*recipe_ids(), *mod_ids(), *EXTRA]}):
	icon(ident).save(OUT / f"{split(ident)[1]}.png")
	print(ident)
