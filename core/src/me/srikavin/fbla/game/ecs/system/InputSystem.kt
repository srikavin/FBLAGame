package me.srikavin.fbla.game.ecs.system

import com.artemis.ComponentMapper
import com.artemis.annotations.All
import com.artemis.annotations.Exclude
import com.artemis.annotations.Wire
import com.artemis.systems.IteratingSystem
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.Contact
import com.badlogic.gdx.physics.box2d.ContactImpulse
import com.badlogic.gdx.physics.box2d.ContactListener
import com.badlogic.gdx.physics.box2d.Manifold
import me.srikavin.fbla.game.GameActions
import me.srikavin.fbla.game.ecs.component.*
import me.srikavin.fbla.game.graphics.player_foot_fixture_id
import me.srikavin.fbla.game.physics.ContactListenerManager

private const val MAX_HORIZONTAL_VELOCITY = 8f
private val JUMP_IMPULSE = Vector2(0.0f, 22.0f)
private val JUMP_FALL_IMPULSE = Vector2(0.0f, -.5f)
private val LEFT_IMPULSE = Vector2(-1f, 0.0f)
private val RIGHT_IMPULSE = Vector2(1f, 0.0f)

/**
 * Responsible for handling player input and controlling jump timings
 */
@All(PlayerControlled::class, PhysicsBody::class, Transform::class)
@Exclude(DisableInput::class)
class InputSystem(private val listenerManager: ContactListenerManager) : IteratingSystem() {
    @Wire
    private lateinit var playerControlledMapper: ComponentMapper<PlayerControlled>

    @Wire
    private lateinit var physicsBodyMapper: ComponentMapper<PhysicsBody>

    @Wire
    private lateinit var deadMapper: ComponentMapper<Dead>

    private var allowJump: Int = 0

    inner class FootContactListener : ContactListener {
        override fun endContact(contact: Contact) {
            if (contact.fixtureA.userData == player_foot_fixture_id && !contact.fixtureB.isSensor ||
                    contact.fixtureB.userData == player_foot_fixture_id && !contact.fixtureA.isSensor) {
                allowJump -= 1
                if (allowJump < 0) {
                    allowJump = 0
                }
            }
        }

        override fun beginContact(contact: Contact) {
            if (contact.fixtureA.userData == player_foot_fixture_id && !contact.fixtureB.isSensor ||
                    contact.fixtureB.userData == player_foot_fixture_id && !contact.fixtureA.isSensor) {
                allowJump += 1
            }
        }

        override fun preSolve(contact: Contact, oldManifold: Manifold) {
        }

        override fun postSolve(contact: Contact, impulse: ContactImpulse) {
        }
    }

    override fun initialize() {
        super.initialize()
        listenerManager.addListener(FootContactListener())
    }


    override fun process(entityId: Int) {
        if (deadMapper.has(entityId)) {
            return
        }

        val body = physicsBodyMapper[entityId].body

        if (allowJump == 0 && body.linearVelocity.y < -1f && body.linearVelocity.y > -15f) {
            // Reduce 'floaty' feel of jumping by making falls faster
            body.applyLinearImpulse(JUMP_FALL_IMPULSE, body.position, true)
        }

        playerControlledMapper[entityId].bindings.bindings.forEach { (action, keyCode) ->
            if (Gdx.input.isKeyPressed(keyCode)) {
                when (action) {
                    GameActions.JUMP -> {
                        if (allowJump > 0) {
                            body.applyLinearImpulse(JUMP_IMPULSE, body.position, true)
                            allowJump = 0
                        }
                    }
                    GameActions.MOVE_LEFT -> {
                        if (body.linearVelocity.x > -MAX_HORIZONTAL_VELOCITY) {
                            body.applyLinearImpulse(LEFT_IMPULSE, body.position, true)
                        }
                    }
                    GameActions.MOVE_RIGHT -> {
                        if (body.linearVelocity.x < MAX_HORIZONTAL_VELOCITY) {
                            body.applyLinearImpulse(RIGHT_IMPULSE, body.position, true)
                        }
                    }
                    GameActions.QUIT -> {
                        Gdx.app.exit()
                    }
                    GameActions.USE -> {
                    }
                }
            }
        }
    }
}