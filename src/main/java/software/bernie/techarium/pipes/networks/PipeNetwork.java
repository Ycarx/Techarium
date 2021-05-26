package software.bernie.techarium.pipes.networks;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import software.bernie.techarium.pipes.PipePosition;
import software.bernie.techarium.pipes.capability.PipeType;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter(AccessLevel.PACKAGE)
public abstract class PipeNetwork<Cap, ToTransport> {

    private UUID uuid;
    private List<BlockPos> pipeBlocks = new ArrayList<>();
    private List<PipePosition> inputs = new ArrayList<>();
    private List<PipePosition> either = new ArrayList<>();
    private List<PipePosition> outputs = new ArrayList<>();

    public abstract boolean isType(PipeType type);

    public void tick(ServerWorld world) {
        LazyOptional<Cap> input = getCapability(world, inputs.get(0));
        LazyOptional<Cap> output = getCapability(world, outputs.get(0));
        if (input.isPresent() && output.isPresent()) {
            Cap inputCap = input.orElseThrow(NullPointerException::new);
            Cap outputCap = output.orElseThrow(NullPointerException::new);
            for (int i = 0; i < getSlots(inputCap); i++) {
                ToTransport maxDrained = drain(inputCap, getMaxRemove(), i, true);
                if (isEmpty(maxDrained))
                    continue;
                ToTransport filled = fill(outputCap, maxDrained, false);
                if (isEmpty(filled))
                    break; // fill next thing to fill
                drainWith(inputCap, filled, i, false);
                break;
            }
        }
    }

    public LazyOptional<Cap> getCapability(ServerWorld world, PipePosition position) {
        if (world.getChunkProvider().isChunkLoaded(new ChunkPos(position.getPos()))) {
            TileEntity te =  world.getTileEntity(position.getPos().offset(position.getDirection()));
            if (te != null) {
                return te.getCapability(getDefaultCapability(), position.getDirection());
            }
        }
        return LazyOptional.empty();
    }

    public abstract boolean isEmpty(ToTransport toTransport);

    public abstract Capability<Cap> getDefaultCapability();

    public abstract int getMaxRemove();

    public abstract boolean canFill(Cap capability, ToTransport transport);

    public abstract ToTransport drain(Cap capability, int amount, int slot, boolean simulate);

    public abstract ToTransport drainWith(Cap capability, ToTransport drain, int slot, boolean simulate);

    public abstract ToTransport fill(Cap capability, ToTransport transport, boolean simulate);

    public abstract int getSlots(Cap capability);
}