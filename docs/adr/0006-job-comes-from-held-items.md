# A golem's job comes from what it holds, not a stored field

A golem's job is derived from its held item, and for tool jobs a player-created attachment too, with no stored job field. This keeps state simple, because taking the item away ends the job and there is no second value to fall out of sync. The cost is that a held item can look like a job, so an item check alone is not enough: without the attachment, an untouched golem could gain a job by picking up the item from a chest.
