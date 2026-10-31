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
    private static final float SNAP_DURATION = 0.18f;
    private static final float TWO_PI = (float) (Math.PI * 2.0);
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
        final boolean hasEffect = PlushieMovementSystem.hasPlushieEffect(effects);

        Snap snap = snaps.get(uuid);

        if (!crouching || !hasEffect) {
            final boolean finished = snap == null || snap.done || !hasEffect;
            if (finished) {
                snaps.remove(uuid);
                return;
            }
        }

        final Vector3d position = transform.getPosition();
        final Rotation3f rotation = transform.getRotation();

        if (snap == null) {
            if (!states.onGround) return;

            snap = new Snap(
                    position.x, position.z, rotation.y,
                    Math.floor(position.x) + 0.5,
                    Math.floor(position.z) + 0.5,
                    snapYaw(rotation.y));
            snaps.put(uuid, snap);
        }
        if (snap.done) return;

        assert states != null;
        if (!states.onGround) {
            snap.done = true;
            return;
        }

        snap.elapsed += dt;
        final float time = Math.min(1f, snap.elapsed / SNAP_DURATION);
        final float timeAfterEase = timeAfterEaseOutCubic(time);

        final double x = lerp(snap.startX, snap.targetX, timeAfterEase);
        final double z = lerp(snap.startZ, snap.targetZ, timeAfterEase);
        final float yaw = snap.startYaw + snap.yawDelta * timeAfterEase;

        velocity.addInstruction(new Vector3d(0,0,0), null, ChangeVelocityType.Set);

        final Ref<EntityStore> ref = chunk.getReferenceTo(index);
        final Teleport teleport = Teleport.createForPlayer(
                new Vector3d(x, position.y, z),
                new Rotation3f(rotation.x, yaw, 0f));
        buffer.addComponent(ref, Teleport.getComponentType(), teleport);

        if (time >= 1f) snap.done = true;
    }

    private static float snapYaw(float yaw) {
        return Math.round(yaw / QUARTER_TURN) * QUARTER_TURN;
    }

    private static float angleDelta(float a, float b) {
        float d = (b - a) % TWO_PI;
        if (d > Math.PI) d -= TWO_PI;
        if (d < -Math.PI) d += TWO_PI;
        return d;
    }

    private static float timeAfterEaseOutCubic(float t) {
        final float inv = 1f - t;
        return 1f - inv * inv * inv;
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private static final class Snap {
        private final double startX, startZ, targetX, targetZ;
        private final float startYaw, yawDelta;
        private float elapsed;
        private boolean done;

        private Snap(double startX, double startZ, float startYaw,
                     double targetX, double targetZ, float targetYaw) {
            this.startX = startX;
            this.startZ = startZ;
            this.startYaw = startYaw;
            this.targetX = targetX;
            this.targetZ = targetZ;
            this.yawDelta = angleDelta(startYaw, targetYaw);
        }
    }
}
