package me.srikavin.fbla.game.ecs.system

import com.artemis.ComponentMapper
import com.artemis.EntitySubscription
import com.artemis.annotations.All
import com.artemis.managers.TagManager
import com.artemis.systems.IteratingSystem
import com.artemis.utils.IntBag
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.*
import ktx.assets.disposeSafely
import ktx.collections.GdxArray
import me.srikavin.fbla.game.ecs.component.FixedRotation
import me.srikavin.fbla.game.ecs.component.MapTrigger
import me.srikavin.fbla.game.ecs.component.PhysicsBody
import me.srikavin.fbla.game.ecs.component.Transform
import me.srikavin.fbla.game.graphics.player_foot_fixture_id
import me.srikavin.fbla.game.physics.ContactListenerManager

/**
 * Responsible for communicating and keeping entities in sync with Box2D. The physics engine uses a fixed timestep of
 * 60 steps a second.
 *
 * Handles entities which have both [Transform] and [PhysicsBody] components attached
 */
@All(Transform::class, PhysicsBody::class)
class PhysicsSystem(var physicsWorld: World, private val contactManager: ContactListenerManager) : IteratingSystem() {
    lateinit var transformMapper: ComponentMapper<Transform>
    lateinit var physicsMapper: ComponentMapper<PhysicsBody>
    lateinit var triggerMapper: ComponentMapper<MapTrigger>
    lateinit var fixedRotationMapper: ComponentMapper<FixedRotation>

    override fun initialize() {
        getSubscription().addSubscriptionListener(SubscriptionListener())
        physicsWorld.setContactListener(contactManager)
    }

    private inner class SubscriptionListener : EntitySubscription.SubscriptionListener {
        override fun inserted(entities: IntBag) {

            for (i in 0 until entities.size()) {
                val e = entities[i]

                val physics = physicsMapper[e]
                val transform = transformMapper[e]


                if (triggerMapper.has(e)) {
                    physics.isSensor = true
                }

                val bodyDef = BodyDef().apply {
                    type = physics.type
                    position.set(transform.position)
//                    linearDamping = physics.linearDamping
                }
                physics.body = physicsWorld.createBody(bodyDef).apply {
                    userData = e
                }

                if (fixedRotationMapper.has(e)) {
                    physics.body.isFixedRotation = true
                }

                val fixtures: GdxArray<Fixture>

                if (physics.fixtureDefs.isEmpty) {
                    fixtures = GdxArray(false, 1)

                    val fixtureDef = FixtureDef().apply {
                        this.friction = physics.friction
                        this.density = physics.density
                        this.restitution = physics.restitution
                        this.shape = physics.shape
                    }

                    val fixture = physics.body.createFixture(fixtureDef)
                    fixture.userData = e
                    fixture.isSensor = physics.isSensor

                    fixtures.add(fixture)
                } else {
                    fixtures = GdxArray(false, physics.fixtureDefs.size)

                    for (fixtureDef in physics.fixtureDefs) {
                        val fixture = physics.body.createFixture(fixtureDef)
                        fixture.userData = e
                        fixture.isSensor = physics.isSensor

                        fixtures.add(fixture)
                    }
                }

                val filter = Filter()
                filter.groupIndex = -1
                fixtures.forEach {
                    it.filterData = filter
                }


                if (e == world.getSystem(TagManager::class.java).getEntityId("PLAYER")) {
                    // Increased friction when touching ground without affecting wall sliding
                    val feet = FixtureDef().apply {
                        //                        this.isSensor = true
                        this.shape = PolygonShape().apply {
                            setAsBox(0.55f, 0.15f, Vector2(0f, -0.9f), 0f)
                        }
                        friction = .9f
                    }

                    fixtures.add(physics.body.createFixture(feet))

                    val feet2 = FixtureDef().apply {
                        //                        this.isSensor = true
                        this.shape = PolygonShape().apply {
                            setAsBox(0.6f, 0.05f, Vector2(0f, -1f), 0f)
                        }
                        friction = 0f
                    }

                    fixtures.add(physics.body.createFixture(feet2))

                    // Used to detect when the player is on the ground
                    val footBox = FixtureDef().apply {
                        //                        this.isSensor = true
                        this.shape = PolygonShape().apply {
                            setAsBox(0.55f, 0.05f, Vector2(0f, -1f), 0f)
                        }
                        friction = .9f
                    }

                    val fixture = physics.body.createFixture(footBox)
                    fixture.userData = player_foot_fixture_id
                    fixture.isSensor = true
                    fixtures.add(fixture)


                    // Apply a filter to allow for the death animation
                    filter.groupIndex = 1
                    fixtures.forEach {
                        it.filterData = filter
                    }
                }


                physics.fixtures = fixtures
            }
        }

        override fun removed(entities: IntBag) {
            for (i in 0 until entities.size()) {
                val e = entities[i]
                val physics = physicsMapper[e]

                physicsWorld.destroyBody(physics.body)
            }
        }
    }

    override fun begin() {
        physicsWorld.step(1 / 60f, 6, 6)
    }

    override fun process(entityId: Int) {
        // Update positions of physics entities
        transformMapper[entityId].position.set(physicsMapper[entityId].body.position)
    }

    override fun dispose() {
        super.dispose()
        physicsWorld.disposeSafely()
    }
}