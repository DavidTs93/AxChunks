package me.DMan16.AxChunks;

import me.Aldreda.AxUtils.AxUtils;
import me.Aldreda.AxUtils.Classes.Listener;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.event.NPCDespawnEvent;
import net.citizensnpcs.api.event.SpawnReason;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.trait.Spawned;
import org.bukkit.event.EventHandler;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

class CitizensListener extends Listener {
	
	public CitizensListener(JavaPlugin plugin) {
		register(plugin);
		new BukkitRunnable() {
			public void run() {
				CitizensAPI.getNPCRegistries().forEach(registry -> registry.forEach(npc -> {
					if (isKeepNPC(npc)) npc.spawn(npc.getStoredLocation(),SpawnReason.CHUNK_LOAD);
				}));
			}
		}.runTaskLater(plugin,10);
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onNPCDespawn(NPCDespawnEvent event) {
		if (isKeepNPC(event.getNPC())) event.setCancelled(true);
	}
	
	boolean isKeepNPC(NPC npc) {
		return npc.getOrAddTrait(Spawned.class).shouldSpawn() && AxChunks.isKeepRegion(AxUtils.getWorldGuardManager().getRegions(npc.getStoredLocation()));
	}
}