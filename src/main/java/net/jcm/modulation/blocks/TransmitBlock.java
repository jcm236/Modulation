package net.jcm.modulation.blocks;

import net.jcm.modulation.api.AbstractRadioField;
import net.jcm.modulation.api.signal.SignalEmission;
import net.jcm.modulation.api.tick.IBlockRadioTickSubscriber;
import net.jcm.modulation.impl.RadioManager;
import net.jcm.modulation.util.Utils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class TransmitBlock extends DirectionalBlock {

    private static final int FREQUENCY = 433_000_000; // 433 MHz (common ISM band for radios)
    private static final float POWER = 10f;            // 10 dBm = 10mW
    private static final int TICK_INTERVAL = 1;

    public TransmitBlock(Properties p_52591_) {
        super(p_52591_);
        this.registerDefaultState(this.defaultBlockState()
                .setValue(FACING, Direction.NORTH)
        );
    }

    @Override
    public void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING);
    }

    @Override
    public RenderShape getRenderShape(BlockState blockState) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction dir = ctx.getNearestLookingDirection().getOpposite();
        Player player = ctx.getPlayer();
        if (player != null && player.isShiftKeyDown()) {
            dir = dir.getOpposite();
        }
        return this.defaultBlockState()
                .setValue(BlockStateProperties.FACING, dir);
    }

    @Override
    public void onPlace(BlockState pState, Level pLevel, BlockPos pPos, BlockState pOldState, boolean pMovedByPiston) {
        super.onPlace(pState, pLevel, pPos, pOldState, pMovedByPiston);
        if (pLevel instanceof ServerLevel serverLevel) {
            RadioManager.getInstance().getOrCreateField(serverLevel)
                    .addBlockSubscriber(pPos, new IBlockRadioTickSubscriber() {
                        @Override
                        public void preTick(Vec3i pos, AbstractRadioField field) {}

                        @Override
                        public void postTick(Vec3i pos, AbstractRadioField field) {
                            String message = "Meow? Meow Meow Meow!";
                            byte[] encoded = Utils.hammingEncodeBytes(message.getBytes(StandardCharsets.UTF_8));
                            field.emit(new SignalEmission(
                                    pPos.getCenter(), FREQUENCY, encoded, POWER,
                                    pState.getValue(FACING).step()
                            ));
                        }
                    });
        }
    }

    @Override
    public void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pMovedByPiston) {
        if (!pState.is(pNewState.getBlock()) && pLevel instanceof ServerLevel serverLevel) {
            RadioManager.getInstance().getOrCreateField(serverLevel).removeBlockSubscriber(pPos);
        }
        super.onRemove(pState, pLevel, pPos, pNewState, pMovedByPiston);
    }
}