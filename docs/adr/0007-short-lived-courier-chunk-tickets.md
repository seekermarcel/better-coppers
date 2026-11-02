# Couriers use short-lived chunk tickets

A courier keeps working far from any player by holding a chunk ticket around itself, like a thrown ender pearl. The ticket lasts two seconds and the courier keeps refreshing it, and only while it is travelling. We chose this over force-loading chunks so that a dead or stuck courier cannot leave chunks loaded forever: when the refreshing stops, the ticket expires.
