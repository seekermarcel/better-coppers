# The vanilla rule rests on one predicate and its tests

Existing worlds and redstone builds must keep working with the mod installed, which makes a behaviour change on an untouched golem a failure even if it runs fine. Every check for "does this golem count as having a job" goes through `Upkeep.hasJob`, so there is one definition, and `VanillaGameTests` guards the outcome. Nothing in the structure enforces this, so a new behaviour or goal has to apply the predicate itself.

The one accepted exception is the axe dispenser, which scrapes any golem in front of it, because the player built that dispenser on purpose. With no golem there, it hands off to whichever dispenser behaviour was registered for axes before the mod's, so it follows vanilla if vanilla changes.
