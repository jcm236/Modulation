package net.jcm.modulation.mixin;

import net.jcm.modulation.Modulation;
import net.jcm.modulation.attenuation.MaterialGrid;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelChunk.class)
public abstract class MixinLevelChunk {
    @Inject(method = "setBlockState", at = @At("RETURN"))
    private void modulation$onSetBlockState(
            BlockPos pos, BlockState state, boolean pIsMoving, CallbackInfoReturnable<BlockState> cir) {
        LevelChunk self = (LevelChunk) (Object) this;

        if (!(self.getLevel() instanceof ServerLevel serverLevel)) return;

        MaterialGrid grid = MaterialGrid.getInstance(serverLevel.dimension());
        if (grid == null) {
            Modulation.LOGGER.error("WTF Untracked Level {}", serverLevel.dimension());
            MaterialGrid.loadLevel(serverLevel.dimension());
        }
        grid = MaterialGrid.getInstance(serverLevel.dimension());
        grid.update(pos, state);
    }
}
