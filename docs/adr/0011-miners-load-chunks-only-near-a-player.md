# Miners load chunks only near a player

Miners hold the same short-lived chunk ticket as couriers (ADR 0007), but only while a player is near their Mine Entrance. A courier only moves items, so it may work with nobody around. A mine makes new items, and running it unwatched would turn it into an AFK ore farm and keep chunks busy. Tying the ticket to a nearby player keeps the mine working however far it reaches, without that. The cost: a miner frozen in an unloaded chunk stays frozen until a player comes close, and its section waits for its Claim to lapse.
