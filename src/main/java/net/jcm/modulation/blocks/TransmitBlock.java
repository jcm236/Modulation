package net.jcm.modulation.blocks;

import net.jcm.modulation.api.AbstractRadioField;
import net.jcm.modulation.api.SignalEmission;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class TransmitBlock extends DirectionalBlock {
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
        pLevel.scheduleTick(pPos, this, 10);
    }

    @Override
    public void tick(BlockState pState, @NotNull ServerLevel level, @NotNull BlockPos pPos, @NotNull RandomSource pRandom) {
        ByteBuffer buffer = ByteBuffer.allocate(20);
        buffer.putChar('v');
        String string = "Meow? Meow Meow Meow!";
        AbstractRadioField.getInstance(level).queueEmit(new SignalEmission(pPos.getCenter(), 50, string.getBytes(StandardCharsets.UTF_8),20 , pState.getValue(FACING).step()));
        System.out.println("ssss: " + buffer);
        level.scheduleTick(pPos, this, 10);
        super.tick(pState, level, pPos, pRandom);
    }
}
