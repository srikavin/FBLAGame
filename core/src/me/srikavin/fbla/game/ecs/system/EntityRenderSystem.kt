package me.srikavin.fbla.game.ecs.system

import com.artemis.ComponentMapper
import com.artemis.annotations.All
import com.artemis.annotations.One
import com.artemis.annotations.Wire
import com.artemis.systems.IteratingSystem
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import me.srikavin.fbla.game.ecs.component.*
import me.srikavin.fbla.game.graphics.scale_factor

/**
 * Responsible for rendering entities with the following components
 *  * [Animated]
 *  * [Sprite]
 *  * [SwitchableAnimation]
 */
@One(Animated::class, Sprite::class, SwitchableAnimation::class)
@All(Transform::class)
class EntityRenderSystem : IteratingSystem() {
    private lateinit var spriteMapper: ComponentMapper<Sprite>
    private lateinit var animatedMapper: ComponentMapper<Animated>
    private lateinit var switchableAnimationMapper: ComponentMapper<SwitchableAnimation>
    private lateinit var transformMapper: ComponentMapper<Transform>
    private lateinit var offsetMapper: ComponentMapper<SpriteOffset>
    private lateinit var scaleMapper: ComponentMapper<SpriteScale>
    private val recycledPosition = Vector2()
    private val defaultScale = Vector2(1f, 1f)

    @Wire
    private lateinit var batch: SpriteBatch

    private var stateTime = 0f

    override fun begin() {
        stateTime += Gdx.graphics.deltaTime
        batch.begin()
    }

    override fun process(entityId: Int) {
        recycledPosition.set(transformMapper[entityId].position)

        var mirrored = false

        val scale = when {
            scaleMapper.has(entityId) -> scaleMapper[entityId].scale
            else -> defaultScale
        }

        val sprite: TextureRegion = when {
            spriteMapper.has(entityId) -> spriteMapper[entityId].sprite
            switchableAnimationMapper.has(entityId) -> {
                val animated = switchableAnimationMapper[entityId]
                mirrored = animated.mirror
                animated.getCurrentAndUpdate(world.delta).getKeyFrame(stateTime, animated.looping)
            }
            animatedMapper.has(entityId) -> {
                val animated = animatedMapper[entityId]
                animated.animation.getKeyFrame(stateTime, animated.looping)
            }
            else -> throw RuntimeException("Entity does not have Sprite nor Animated; should never happen")
        }



        if (offsetMapper.has(entityId)) {
            recycledPosition.add(offsetMapper[entityId].offset)
        }

        val w = sprite.regionWidth * scale_factor * scale.x
        val h = sprite.regionHeight * scale_factor * scale.y


        if (mirrored) {
            batch.draw(sprite, recycledPosition.x + w, recycledPosition.y, -w, h)
        } else {
            batch.draw(sprite, recycledPosition.x, recycledPosition.y, w, h)
        }
    }

    override fun end() {
        batch.end()
    }
}
