package net.jcm.modulation.attenuation;

import net.jcm.modulation.Modulation;
import net.jcm.modulation.impl.RadioManager;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


public class MaterialGrid {
    private final ConcurrentHashMap<Long, MaterialRegistry.MaterialProperties> cache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Set<Long>> chunkIndex = new ConcurrentHashMap<>();

    private static ConcurrentHashMap<ResourceKey<Level>, MaterialGrid> grids = new ConcurrentHashMap<>();

    public static MaterialGrid getInstance(ResourceKey<Level> key) {
        return grids.get(key);
    }

    public static void loadLevel(ResourceKey<Level> key) {
        Modulation.LOGGER.info("Loading MaterialGrid for {}", key);
        grids.put(key, new MaterialGrid());
    }

    public static void unloadLevel(ResourceKey<Level> key) {
        Modulation.LOGGER.info("Unloading MaterialGrid for {}", key);
        grids.remove(key);
    }

    public float getAttenuation(int x, int y, int z, int frequencyHz) {
        return this.getAttenuation(BlockPos.asLong(x, y, z), frequencyHz);
    }
    public float getAttenuation(long packedPos, int frequencyHz) {
        return MaterialRegistry.getAttenuationFor(cache.getOrDefault(packedPos, MaterialRegistry.AIR_PROPS), frequencyHz);
    }

    private void indexBlockPos(long packedPos) {
        long chunkKey = ChunkPos.asLong(BlockPos.getX(packedPos) >> 4, BlockPos.getZ(packedPos) >> 4);
        chunkIndex.computeIfAbsent(chunkKey, k -> ConcurrentHashMap.newKeySet()).add(packedPos);
    }

    private void deindexBlockPos(long packedPos) {
        long chunkKey = ChunkPos.asLong(BlockPos.getX(packedPos) >> 4, BlockPos.getZ(packedPos) >> 4);
        Set<Long> set = chunkIndex.get(chunkKey);
        if (set != null) set.remove(packedPos);
    }

    public void update(BlockPos pos, BlockState state) {
        long key = pos.asLong();
        MaterialRegistry.MaterialProperties props = MaterialRegistry.getInstance().getProperties(state);
        if (props.baseDb() == 0f && props.frequencyExponent() == 0f) {
            cache.remove(key);
            deindexBlockPos(key);
        } else {
            cache.put(key, props);
            indexBlockPos(key);
        }
    }

    public void loadSection(ChunkAccess chunk, int sectionY) {
        LevelChunkSection section = chunk.getSections()[chunk.getSectionIndex(sectionY << 4)];
        if (section == null || section.hasOnlyAir()) return;

        ChunkPos chunkPos = chunk.getPos();
        int baseX = chunkPos.x << 4;
        int baseZ = chunkPos.z << 4;
        int baseY = sectionY << 4;

        for (int lx = 0; lx < 16; lx++) {
            for (int ly = 0; ly < 16; ly++) {
                for (int lz = 0; lz < 16; lz++) {
                    BlockState state = section.getBlockState(lx, ly, lz);
                    if (state.isAir()) continue;
                    MaterialRegistry.MaterialProperties props = MaterialRegistry.getInstance().getProperties(state);
                    if (props.baseDb() <= 0f && props.frequencyExponent() <= 0f) continue;
                    long pos = BlockPos.asLong(baseX + lx, baseY + ly, baseZ + lz);
                    cache.put(pos, props);
                    indexBlockPos(pos);
                }
            }
        }
    }


    public void unloadChunk(int chunkX, int chunkZ) {
        long chunkKey = ChunkPos.asLong(chunkX, chunkZ);
        Set<Long> positions = chunkIndex.remove(chunkKey);
        if (positions != null) positions.forEach(cache::remove);
    }

    public void clear() {
        cache.clear();
        chunkIndex.clear();
    }

    public static void clearAll(){
        for (MaterialGrid grid :  grids.values()) {
            grid.clear();
        }
        grids.clear();
    }

    @Mod.EventBusSubscriber
    public static class EventHandler {
        @SubscribeEvent
        public static void onLevelLoad(LevelEvent.Load event) {
            if (event.getLevel() instanceof ServerLevel level) {
                MaterialGrid.loadLevel(level.dimension());
            }
        }

        @SubscribeEvent
        public static void onLevelUnload(LevelEvent.Unload event) {
            if (event.getLevel() instanceof ServerLevel level) {
                if (RadioManager.getInstance() == null) return;
                MaterialGrid.unloadLevel(level.dimension());
            }
        }

        @SubscribeEvent
        public static void onChunkLoad(ChunkEvent.Load event) {
            if (!(event.getLevel() instanceof ServerLevel level)) return;

            MaterialGrid grid = MaterialGrid.getInstance(level.dimension());
            if (grid == null) {
                Modulation.LOGGER.error("WTF Untracked Level while chunk load {}", level.dimension());
                MaterialGrid.loadLevel(level.dimension());
            }
            grid = MaterialGrid.getInstance(level.dimension());

            int minSection = level.getMinSection();
            int maxSection = level.getMaxSection();
            for (int s = minSection; s < maxSection; s++) {
                grid.loadSection(event.getChunk(), s);
            }
        }

        @SubscribeEvent
        public static void onChunkUnload(ChunkEvent.Unload event) {
            if (!(event.getLevel() instanceof ServerLevel level)) return;

            MaterialGrid grid = MaterialGrid.getInstance(level.dimension());
            if (grid == null) return;

            ChunkPos pos = event.getChunk().getPos();
            grid.unloadChunk(pos.x, pos.z);
        }
    }

}
