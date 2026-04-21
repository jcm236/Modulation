package net.jcm.modulation.api;

import net.jcm.modulation.api.signal.SignalEmission;
import net.jcm.modulation.api.signal.SignalSample;
import net.jcm.modulation.api.tick.IBlockRadioTickSubscriber;
import net.jcm.modulation.api.tick.IEntityRadioSubscriber;
import net.jcm.modulation.api.tick.IRadioTickSubscriber;
import net.jcm.modulation.impl.WorldRadioField;
import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class AbstractRadioField {

    protected final AtomicInteger time = new AtomicInteger(0);

    protected final ConcurrentHashMap<UUID, IEntityRadioSubscriber> entityRadioSubscribers = new ConcurrentHashMap<>();
    protected final ConcurrentHashMap<Vec3i, IBlockRadioTickSubscriber> blockRadioSubscribers = new ConcurrentHashMap<>();
    protected final Set<IRadioTickSubscriber> tickSubscribers = ConcurrentHashMap.newKeySet();

    public abstract void queueEmit(SignalEmission emission);

    public void addTickSubscriber(IRadioTickSubscriber subscriber) {
        this.tickSubscribers.add(subscriber);
    }

    public void removeTickSubscriber(IRadioTickSubscriber subscriber) {
        this.tickSubscribers.remove(subscriber);
    }

    public void addBlockSubscriber(Vec3i vec3, IBlockRadioTickSubscriber subscriber) {
        this.blockRadioSubscribers.put(vec3, subscriber);
    }

    public void removeBlockSubscriber(Vec3i vec3) {
        this.blockRadioSubscribers.remove(vec3);
    }

    public void addEntitySubscriber(UUID entityUUID, IEntityRadioSubscriber subscriber) {
        this.entityRadioSubscribers.put(entityUUID, subscriber);
    }

    public void removeEntitySubscriber(UUID uuid) {
        this.entityRadioSubscribers.remove(uuid);
    }

    public abstract void emit(SignalEmission emission);

    public abstract List<WorldRadioField.EmissionMetadata> sampleRaw(Vec3 position, int frequency, float bandwidth, Vector3f direction);

    public abstract SignalSample sample(Vec3 position, int frequency, float bandwidth, Vector3f direction);

    public int getTicks() {
        return time.get();
    }

    /*
     * subscribe here
     */
    protected void preTick() {
        for (IRadioTickSubscriber subscriber : tickSubscribers) {
            subscriber.preTick(this);
        }

        for (Map.Entry<Vec3i, IBlockRadioTickSubscriber> entry : blockRadioSubscribers.entrySet()) {
            entry.getValue().preTick(entry.getKey(), this);
        }

        for (Map.Entry<UUID, IEntityRadioSubscriber> entry : entityRadioSubscribers.entrySet()) {
            entry.getValue().preTick(entry.getKey(), this);
        }
    }
    /*
    * emit here
    */
    protected void postTick() {
        for (IRadioTickSubscriber subscriber : tickSubscribers) {
            subscriber.postTick(this);
        }

        for (Map.Entry<Vec3i, IBlockRadioTickSubscriber> entry : blockRadioSubscribers.entrySet()) {
            entry.getValue().postTick(entry.getKey(), this);
        }

        for (Map.Entry<UUID, IEntityRadioSubscriber> entry : entityRadioSubscribers.entrySet()) {
            entry.getValue().postTick(entry.getKey(), this);
        }
    }

    protected abstract void reset();


    public void tick(){
        this.preTick();
        this.reset();
        this.postTick();
        time.incrementAndGet();
    }
}
