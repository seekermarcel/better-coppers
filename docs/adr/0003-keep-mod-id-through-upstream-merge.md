# Keep the mod id through the upstream merge

The upstream repository renamed the mod. We reversed that rename before merging, keeping the `bettercoppers` mod id and package names, so that existing worlds still load and the merge did not fight rename conflicts. Upstream's add-on API and Automatisation add-on came across under these names. We may merge from upstream again, depending on how it develops, and any such merge needs the same reversal.
