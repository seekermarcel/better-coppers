# PROTOTYPE: what each Mine Section looks like

Throwaway, for [#27](https://github.com/seekermarcel/better-coppers/issues/27) of the map [#25](https://github.com/seekermarcel/better-coppers/issues/25). Not built in game. Decided by an agent on Marcel's behalf; review welcome. The slices were printed by a throwaway script, so the numbers add up.

## How to read it

- Every section is drawn in its own frame, from the Open End it grows from: `f` is forward (away from the parent), `s` is side (`-` left, `+` right), `y` is up from the parent's floor (`y+0` is feet level).
- One block of ASCII per `y` layer, side by side, top layer first. In each layer the top row is the far end and the bottom row (`f-1`) is the parent's last slice.

| Char | Meaning |
|---|---|
| `#` | rock, left alone |
| `.` | dug out (air) |
| `o` | the parent's last slice, already dug |
| `_` | floor the golem makes solid (plank or filler where it isn't) |
| `F` | fence post |
| `P` | planks (beam or pillar) |
| `T` | wall torch |
| `=` | rail (only when the miner carries rails) |
| `^ < >` | Open End: the first block of the next section, and the way it faces |

## Rules for every section

- **The vanilla mineshaft look**: 3 wide, 3 high, oak fence posts carrying a plank beam, torches, rails down the middle. Supports in a chain of straights stay 4 blocks apart, as now (`SUPPORT_SPACING`).
- **Open End**: a block position and a facing. The parent leaves it as rock. The child's slice `f0` starts there, and its first stand is the parent's last slice, which is already dug and connected to the entrance. The child's 3 wide `s-1..s+1` lines up with the parent's opening.
- **Dig order** is today's `digSlice`, kept: stand in the middle of the slice behind, dig the middle column top down, then the left and the right column, then make the floor solid, then the supports, then the torch. Slice by slice, away from the parent.
- **A golem reaches 2 blocks above its feet** (`Golems.isInReach`). Every section is laid out so that each block is reachable from a block already dug: stairs down dig each step from the step above, stairs up finish each step's ceiling from the step after it.
- **Rails** go down last in each section, from its far end back to its Open End, each from the slice behind, so mobs can't walk in early (as now). With no rails in the supplies the miner skips them and the section is still finished.
- **Torches** are skipped where it is already bright, as now.
- **Water and lava** get filler while digging, as now. That is behaviour, not part of a template.
- **Headroom**: where the floor steps up, the lower slice is 4 high, so a player can jump the step (today's `STAIR_HEIGHT`).

## The sections

### Straight

The workhorse: 8 slices, two supports, one torch. One Open End straight ahead.

Blocks to dig ≈ 72 · fences 8 · planks 6 · torches 1 · rails 8 (optional)

```
y+2     y+1     y+0     y-1  
#####   #####   #####   #####
#####   #####   ##^##   #####
#...#   #...#   #.=.#   #___#
#PPP#   #F.F#   #F=F#   #___#
#...#   #...#   #.=.#   #___#
#...#   #...#   #.=.#   #___#
#.T.#   #...#   #.=.#   #___#
#PPP#   #F.F#   #F=F#   #___#
#...#   #...#   #.=.#   #___#
#...#   #...#   #.=.#   #___#
#ooo#   #ooo#   #ooo#   #####
```

- Torch at f3 s+0 y+2, facing forward (hangs on the beam at f2).
- Open End at f8 s+0 y+0, `^`.

### Corner (right; left is the mirror)

3 slices with the far wall left standing, open to one side. The rails curve by themselves. No support: it would block the turn, and the next straight brings its own.

Blocks to dig ≈ 27 · fences 0 · planks 0 · torches 0 · rails 3 (optional)

```
y+2      y+1      y+0      y-1   
######   ######   ######   ######
#...##   #...##   #...##   #___##
#...##   #...##   #.==>#   #___##
#...##   #...##   #.=.##   #___##
#ooo##   #ooo##   #ooo##   ######
```

- Open End at f1 s+2 y+0, `>`.

### Crossing

5 by 5, a plus, with a plank pillar at each inner corner like vanilla crossings. Open ahead, left and right; the rails run straight through, and a side branch gets its own.

Blocks to dig ≈ 63 · fences 0 · planks 12 · torches 1 · rails 5 (optional)

```
y+2         y+1         y+0         y-1      
#########   #########   #########   #########
#########   #########   ####^####   #########
###...###   ###...###   ###.=.###   ###___###
##.P.P.##   ##.P.P.##   ##.P=P.##   ##_____##
##.....##   ##.....##   #<..=..>#   ##_____##
##.PTP.##   ##.P.P.##   ##.P=P.##   ##_____##
###...###   ###...###   ###.=.###   ###___###
###ooo###   ###ooo###   ###ooo###   #########
```

- Torch at f1 s+0 y+2, facing left (hangs on the pillar at f1 s+1).
- Open End at f5 s+0 y+0, `^`.
- Open End at f2 s-3 y+0, `<`.
- Open End at f2 s+3 y+0, `>`.

### Small Hall

5 wide, 7 long, 3 high, two wide supports. Open ahead, left and right. The side openings fall between the supports, so no post stands in them.

Blocks to dig ≈ 105 · fences 8 · planks 10 · torches 2 · rails 7 (optional)

```
y+2         y+1         y+0         y-1      
#########   #########   #########   #########
#########   #########   ####^####   #########
##.....##   ##.....##   ##..=..##   ##_____##
##PPPPP##   ##F...F##   ##F.=.F##   ##_____##
##..T..##   ##.....##   ##..=..##   ##_____##
##.....##   ##.....##   #<..=..>#   ##_____##
##..T..##   ##.....##   ##..=..##   ##_____##
##PPPPP##   ##F...F##   ##F.=.F##   ##_____##
##.....##   ##.....##   ##..=..##   ##_____##
###ooo###   ###ooo###   ###ooo###   #########
```

- Torch at f2 s+0 y+2, facing forward (hangs on the beam at f1).
- Torch at f4 s+0 y+2, facing back (hangs on the beam at f5).
- Open End at f7 s+0 y+0, `^`.
- Open End at f3 s-3 y+0, `<`.
- Open End at f3 s+3 y+0, `>`.

### Stairs Down

4 steps down, 1 per slice, then a flat landing so the next section attaches at a normal 3 high slice, 4 lower. No supports (vanilla stairs have none). Rails become sloped rails by themselves; carts need powered rails to climb back, which stay the player's job. Dig order: each step from the step above, top block first.

Blocks to dig ≈ 57 · fences 0 · planks 0 · torches 1 · rails 5 (optional)

Side profile through the middle column (s 0); all three columns look the same:

```
y+3 #######
y+2 o.#####
y+1 o..####
y+0 o...###
y-1 #=...##
y-2 #_=...#
y-3 ##_=..#
y-4 ###_==>
y-5 ####__#
    -012345  <- f
```

```
y+2     y+1     y+0     y-1     y-2     y-3     y-4     y-5  
#####   #####   #####   #####   #####   #####   #####   #####
#####   #####   #####   #####   #####   #####   ##^##   #####
#####   #####   #####   #####   #...#   #...#   #.=.#   #___#
#####   #####   #####   #...#   #...#   #...#   #.=.#   #___#
#####   #####   #...#   #...#   #T..#   #.=.#   #___#   #####
#####   #...#   #...#   #...#   #.=.#   #___#   #####   #####
#...#   #...#   #...#   #.=.#   #___#   #####   #####   #####
#ooo#   #ooo#   #ooo#   #####   #####   #####   #####   #####
```

- Torch at f2 s-1 y-2, facing right (hangs on the left wall).
- Open End at f5 s+0 y-4, `^`.

### Stairs Up

A flat slice, 4 steps up, a flat landing, 4 higher. A golem standing on step `f-1` reaches only the two lower blocks of step `f`, so each step is dug in two passes: from behind, the two lower blocks; then, standing on the new step, its third block and the fourth (head room) block of the step behind. The flat first slice keeps that fourth block inside this section, so nothing is dug in the parent.

Blocks to dig ≈ 66 · fences 0 · planks 0 · torches 1 · rails 6 (optional)

Side profile through the middle column (s 0); all three columns look the same:

```
y+7 ########
y+6 ####...#
y+5 ###....#
y+4 ##...==>
y+3 #...=__#
y+2 o..=_###
y+1 o.=_####
y+0 o=_#####
y-1 #_######
    -0123456  <- f
```

```
y+6     y+5     y+4     y+3     y+2     y+1     y+0     y-1  
#####   #####   #####   #####   #####   #####   #####   #####
#####   #####   ##^##   #####   #####   #####   #####   #####
#...#   #...#   #.=.#   #___#   #####   #####   #####   #####
#...#   #...#   #.=.#   #___#   #####   #####   #####   #####
#...#   #...#   #T..#   #.=.#   #___#   #####   #####   #####
#####   #...#   #...#   #...#   #.=.#   #___#   #####   #####
#####   #####   #...#   #...#   #...#   #.=.#   #___#   #####
#####   #####   #####   #...#   #...#   #...#   #.=.#   #___#
#####   #####   #####   #####   #ooo#   #ooo#   #ooo#   #####
```

- Torch at f3 s-1 y+4, facing right (hangs on the left wall).
- Open End at f6 s+0 y+4, `^`.

### Dead End

4 slices, one support, a torch on the face, no Open End. See "Should there be a Dead End?" below.

Blocks to dig ≈ 36 · fences 4 · planks 3 · torches 1 · rails 3 (optional)

```
y+2     y+1     y+0     y-1  
#####   #####   #####   #####
#...#   #.T.#   #...#   #___#
#PPP#   #F.F#   #F=F#   #___#
#...#   #...#   #.=.#   #___#
#...#   #...#   #.=.#   #___#
#ooo#   #ooo#   #ooo#   #####
```

- Torch at f3 s+0 y+1, facing back (hangs on the face at f4).
- No Open End.

## Closing off a blocked Open End

When an Open End can't take any section (#32 decides when), the miner closes the opening inside the parent: three fence posts across it at feet level and a sign on the middle post. On a forward end that is the parent's last slice; its rail, if any, is taken up for the middle post. A side end of a Crossing or Small Hall is closed in the side column next to the opening.

```
y+1     y+0       (the last 2 slices of a Straight whose end is blocked)
#####   #####
##S##   #FFF#     S = sign standing on the middle post
#...#   #.=.#
```

## Should there be a Dead End?

Yes. A Crossing or Small Hall adds two Open Ends, Straight, Corner and both Stairs keep one, and only a Dead End takes one away. Without it every branch grows until it reaches the limits or gets blocked, and a mine with crossings in it keeps splitting until it fills the whole radius. A Dead End gives the tree leaves, and it looks like the short dead ends of vanilla mineshafts, not like the fenced end of a blocked Open End. Its weight (#28) sets how bushy a mine gets.

## Handed to other tickets

| Section | Open Ends | Footprint (f × s × y) | Drop | Fences | Planks | Torches | Rails |
|---|---|---|---|---|---|---|---|
| Straight | 1 | 8 × 3 × 3 | 0 | 8 | 6 | 1 | 8 |
| Corner | 1 | 3 × 3 × 3 | 0 | 0 | 0 | 0 | 3 |
| Crossing | 3 | 5 × 5 × 3 | 0 | 0 | 12 | 1 | 5 |
| Small Hall | 3 | 7 × 5 × 3 | 0 | 8 | 10 | 2 | 7 |
| Stairs Down | 1 | 5 × 3 × 7 | -4 | 0 | 0 | 1 | 5 |
| Stairs Up | 1 | 6 × 3 × 7 | +4 | 0 | 0 | 1 | 6 |
| Dead End | 0 | 4 × 3 × 3 | 0 | 4 | 3 | 1 | 3 |

- #28 (rolling): the Open End counts above, and that a Corner is rolled left or right.
- #32 (blocked): the footprint plus the floor layer and, presumably, one block of rock around it is what has to be free.
- #29 (saving): a section is fully described by its kind, its Open End (position, facing) and its mirroring, so the mine can store just that and rebuild the steps.
- #35 (add-ons): a section is a list of cells relative to its Open End, plus its Open Ends; the same shape works for registered sections.
