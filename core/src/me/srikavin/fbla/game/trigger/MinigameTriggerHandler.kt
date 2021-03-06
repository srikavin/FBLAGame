package me.srikavin.fbla.game.trigger

import com.artemis.World
import me.srikavin.fbla.game.ecs.component.MapTrigger
import me.srikavin.fbla.game.ecs.component.MinigameComponent
import me.srikavin.fbla.game.map.MapLoader
import me.srikavin.fbla.game.minigame.MinigameManager
import me.srikavin.fbla.game.util.EntityInt

/**
 * Handles triggers resulting from player collision with minigame triggers.
 */
class MinigameTriggerHandler : TriggerHandler {
    private val minigameManager = MinigameManager()

    override fun run(world: World, player: EntityInt, triggerEntity: EntityInt, trigger: MapTrigger) {
        // Remove entity
        world.delete(triggerEntity)

        val minigame = minigameManager.getMinigame(trigger.properties["minigame_type"] as String)
        minigame.reset(trigger.properties, world, world.getRegistered(MapLoader::class.java))

        world.createEntity().edit()
                .add(MinigameComponent().apply { this.minigame = minigame })

//        minigame.endMinigame()
    }
}