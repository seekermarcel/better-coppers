# Gate vanilla's transport instead of rebuilding it

Jobless golems must keep vanilla's sorting exactly, including through Minecraft updates. An earlier version replaced the idle activity for all golems, which left jobless golems only wandering around. So the mod now adds its behaviours in front of vanilla's idle activity and uses a mixin to block vanilla's own `TransportItemsBetweenContainers` for golems with a job. A rebuilt copy of vanilla's behaviour would drift from the original with each update, and existing redstone builds that rely on vanilla copper golems would break.
