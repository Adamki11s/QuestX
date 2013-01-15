package couk.adamki11s.questx;

import java.util.logging.Logger;

import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import couk.adamki11s.commands.QuestXCommands;
import couk.adamki11s.events.ConversationRegister;
import couk.adamki11s.events.MovementMonitor;
import couk.adamki11s.events.NPCDamageEvent;
import couk.adamki11s.events.NPCInteractEvent;
import couk.adamki11s.io.InitialSetup;
import couk.adamki11s.npcs.NPCHandler;
import couk.adamki11s.threads.ThreadController;

public class QuestX extends JavaPlugin {

	Logger log = Logger.getLogger("QuestX");

	NPCHandler handle;
	ThreadController tControl;
	
	NPCDamageEvent npcDamageEvent;
	NPCInteractEvent npcInteractEvent;
	ConversationRegister playerChatEvent;
	MovementMonitor playerMoveEvent;
	
	public static Plugin p;

	public NPCHandler getNPCHandler() {
		return this.handle;
	}

	@Override
	public void onEnable() {
		
		InitialSetup.run();
		
		handle = new NPCHandler(this);
		p = this;
		this.getCommand("QuestX").setExecutor(new QuestXCommands(this));
		this.tControl = new ThreadController(handle);
		this.tControl.initiateAsyncThread(10L);
		
		//register events
		
		npcDamageEvent = new NPCDamageEvent(this, handle);
		npcInteractEvent = new NPCInteractEvent(this, handle);
		playerChatEvent = new ConversationRegister(this, handle);
		playerMoveEvent = new MovementMonitor(this, handle);
		
		//register events
		
		
		//Updater updater = new Updater(this, "bukkitdev_slug", this.getFile(), Updater.UpdateType.DEFAULT, false);//Final boolean = show dl progress
	}

	@Override
	public void onDisable() {

	}

}
