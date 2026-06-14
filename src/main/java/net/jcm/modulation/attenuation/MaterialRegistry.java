package net.jcm.modulation.attenuation;



import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

/**
 * Stores attenuation.
 */
public class MaterialRegistry {
    private ConcurrentHashMap<Block, MaterialProperties> blocks = new ConcurrentHashMap<>();
    private ConcurrentHashMap<TagKey<Block>, MaterialProperties> tags = new ConcurrentHashMap<>();
    private CopyOnWriteArrayList<Function<BlockState, MaterialProperties>> suppliers = new CopyOnWriteArrayList<>();

    public static final MaterialProperties AIR_PROPS = new MaterialProperties(0f, 0f);
    public static final MaterialProperties DEFAULT = new MaterialProperties(8f, 0.4f);

    private static final MaterialRegistry INSTANCE = new MaterialRegistry();

    public static MaterialRegistry getInstance() {
        return INSTANCE;
    }

    public void registerBlock(Block key, MaterialProperties properties) {
        this.blocks.put(key, properties);
    }

    public void registerTag(TagKey<Block> key, MaterialProperties properties) {
        this.tags.put(key, properties);
    }

    public void addSupplier(Function<BlockState, MaterialProperties> supplier){
        this.suppliers.add(supplier);
    }

    public static float getAttenuationFor(MaterialProperties props, int frequencyHz){
        return props.baseDb()+ props.frequencyExponent()
                * (float) Math.log10(frequencyHz / 1_000_000.0);
    }

    public float getAttenuationFor(BlockState state, int frequencyHz) {
        if (state.isAir()) return 0f;

        MaterialProperties props = this.getProperties(state);

        return getAttenuationFor(props, frequencyHz);
    }

    public MaterialProperties getProperties(BlockState state) {
        MaterialProperties props;

        props = blocks.get(state.getBlock());

        if (props == null)
            for (var entry : tags.entrySet()) {
                if (state.is(entry.getKey())) {
                    props = entry.getValue();
                    break;
                }
            }

        if (props == null)
            for (var supplier : suppliers) {
                props = supplier.apply(state);
                if (props != null) break;
            }

        if (props == null)
            props = state.isSolid() ? DEFAULT : AIR_PROPS;
        return props;
    }

    public record MaterialProperties(float baseDb, float frequencyExponent){}
}
