package com.oruuke.pillow.systems;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.protocol.ChangeVelocityType;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.joml.Vector3d;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlushieBlockSnapSystem extends EntityTickingSystem<EntityStore> {
    private static final double ARRIVE_DISTANCE = 0.04;
    private static final double SPEED_PER_BLOCK = 6.0;
    private static final double MAX_SPEED = 3.0;
    private static final double GIVE_UP_DISTANCE = 0.1;
    private static final float TIMEOUT = 1.5F;
    private static final float QUARTER_TURN = (float) (Math.PI / 2.0);

    private final Map<UUID, Snap> snaps = new ConcurrentHashMap<>();

    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(PlayerRef.getComponentType(), EffectControllerComponent.getComponentType(), MovementStatesComponent.getComponentType(), TransformComponent.getComponentType(), Velocity.getComponentType());
    }

    @Override
    public void tick(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> chunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> buffer) {
        final var playerRef = chunk.getComponent(index, PlayerRef.getComponentType());
        final var effects = chunk.getComponent(index, EffectControllerComponent.getComponentType());
        final var movementStates = chunk.getComponent(index, MovementStatesComponent.getComponentType());
        final var transform = chunk.getComponent(index, TransformComponent.getComponentType());
        final var velocity = chunk.getComponent(index, Velocity.getComponentType());
        if (playerRef == null || effects == null || movementStates == null || transform == null || velocity == null) return;

        final UUID uuid = playerRef.getUuid();
        final var states = movementStates.getMovementStates();
        final boolean crouching = states != null && states.crouching && !states.flying && !states.swimming && !states.mounting;
        if (!crouching || !PlushieMovementSystem.hasPlushieEffect(effects)) {
            snaps.remove(uuid);
            return;
        }

        final Vector3d position = transform.getPosition();
        final Rotation3f rotation = transform.getRotation();
        Snap snap = snaps.get(uuid);
        if (snap == null) {
            if (!states.onGround) return;

            snap = new Snap(Math.floor(position.x) + 0.5, Math.floor(position.z) + 0.5, snapYaw(rotation.y));
            snaps.put(uuid, snap);
        }
        if (snap.done) return;

        final double dx = snap.targetX - position.x;
        final double dz = snap.targetZ - position.z;
        final double distance = Math.sqrt(dx * dx + dz * dz);
        snap.elapsed += dt;

        final boolean arrived = distance <= ARRIVE_DISTANCE;
        final boolean pushedAway = distance > snap.closestDistance + GIVE_UP_DISTANCE;
        final boolean interrupted = pushedAway || !states.onGround;
        if (arrived || pushedAway || !states.onGround || snap.elapsed > TIMEOUT) {
            snap.done = true;
            if (interrupted) return;

            final Ref<EntityStore> ref = chunk.getReferenceTo(index);
            final Rotation3f target = new Rotation3f(rotation.x, snap.targetYaw, 0f);
            final Vector3d snapped = new Vector3d(snap.targetX, position.y, snap.targetZ);
            final Teleport teleport = Teleport.createForPlayer(snapped, target);
            buffer.addComponent(ref, Teleport.getComponentType(), teleport);

            return;
        }

        snap.closestDistance = Math.min(snap.closestDistance, distance);
        final double speed = Math.min(MAX_SPEED, distance * SPEED_PER_BLOCK);
        velocity.addInstruction(new Vector3d(dx / distance * speed, 0, dz / distance * speed), null, ChangeVelocityType.Set);
    }

    private static float snapYaw(float yaw) {
        return Math.round(yaw / QUARTER_TURN) * QUARTER_TURN;
    }

    private static final class Snap {
        private final double targetX;
        private final double targetZ;
        private final float targetYaw;
        private double closestDistance = Double.MAX_VALUE;
        private float elapsed;
        private boolean done;

        private Snap(double targetX, double targetZ, float targetYaw) {
            this.targetX = targetX;
            this.targetZ = targetZ;
            this.targetYaw = targetYaw;
        }
    }
}
