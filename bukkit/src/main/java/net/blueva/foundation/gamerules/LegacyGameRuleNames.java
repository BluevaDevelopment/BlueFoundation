package net.blueva.foundation.gamerules;

import java.util.HashMap;
import java.util.Map;

/**
 * Pre-1.21.11 camelCase game rule names mapped to the registry keys that replaced them.
 */
final class LegacyGameRuleNames {

    private static final Map<String, String> MODERN_KEYS = new HashMap<String, String>();

    static {
        put("announceAdvancements", "show_advancement_messages");
        put("commandBlockOutput", "command_block_output");
        put("commandBlocksEnabled", "command_blocks_work");
        put("enableCommandBlocks", "command_blocks_work");
        put("disablePlayerMovementCheck", "player_movement_check");
        put("disableElytraMovementCheck", "elytra_movement_check");
        put("disableRaids", "raids");
        put("doDaylightCycle", "advance_time");
        put("doEntityDrops", "entity_drops");
        put("doImmediateRespawn", "immediate_respawn");
        put("doInsomnia", "spawn_phantoms");
        put("doLimitedCrafting", "limited_crafting");
        put("doMobLoot", "mob_drops");
        put("doMobSpawning", "spawn_mobs");
        put("doPatrolSpawning", "spawn_patrols");
        put("doTileDrops", "block_drops");
        put("doTraderSpawning", "spawn_wandering_traders");
        put("doVinesSpread", "spread_vines");
        put("doWardenSpawning", "spawn_wardens");
        put("doWeatherCycle", "advance_weather");
        put("allowEnteringNetherUsingPortals", "allow_entering_nether_using_portals");
        put("blockExplosionDropDecay", "block_explosion_drop_decay");
        put("drowningDamage", "drowning_damage");
        put("enderPearlsVanishOnDeath", "ender_pearls_vanish_on_death");
        put("fallDamage", "fall_damage");
        put("fireDamage", "fire_damage");
        put("forgiveDeadPlayers", "forgive_dead_players");
        put("freezeDamage", "freeze_damage");
        put("globalSoundEvents", "global_sound_events");
        put("keepInventory", "keep_inventory");
        put("lavaSourceConversion", "lava_source_conversion");
        put("locatorBar", "locator_bar");
        put("logAdminCommands", "log_admin_commands");
        put("mobExplosionDropDecay", "mob_explosion_drop_decay");
        put("mobGriefing", "mob_griefing");
        put("naturalRegeneration", "natural_health_regeneration");
        put("projectilesCanBreakBlocks", "projectiles_can_break_blocks");
        put("reducedDebugInfo", "reduced_debug_info");
        put("sendCommandFeedback", "send_command_feedback");
        put("showDeathMessages", "show_death_messages");
        put("spawnMonsters", "spawn_monsters");
        put("spawnerBlocksEnabled", "spawner_blocks_work");
        put("spectatorsGenerateChunks", "spectators_generate_chunks");
        put("tntExplodes", "tnt_explodes");
        put("tntExplosionDropDecay", "tnt_explosion_drop_decay");
        put("universalAnger", "universal_anger");
        put("waterSourceConversion", "water_source_conversion");
        put("commandModificationBlockLimit", "max_block_modifications");
        put("maxCommandChainLength", "max_command_sequence_length");
        put("maxCommandForkCount", "max_command_forks");
        put("maxEntityCramming", "max_entity_cramming");
        put("minecartMaxSpeed", "max_minecart_speed");
        put("playersNetherPortalCreativeDelay", "players_nether_portal_creative_delay");
        put("playersNetherPortalDefaultDelay", "players_nether_portal_default_delay");
        put("playersSleepingPercentage", "players_sleeping_percentage");
        put("randomTickSpeed", "random_tick_speed");
        put("snowAccumulationHeight", "max_snow_accumulation_height");
        put("spawnRadius", "respawn_radius");
    }

    private LegacyGameRuleNames() {
    }

    /** Returns the modern key for a camelCase legacy name, or null when it was not renamed. */
    static String modernKey(String camelCaseName) {
        return MODERN_KEYS.get(camelCaseName);
    }

    private static void put(String legacy, String modern) {
        MODERN_KEYS.put(legacy, modern);
    }
}
