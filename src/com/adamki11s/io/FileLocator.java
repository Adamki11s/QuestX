package com.adamki11s.io;

import java.io.File;
import java.io.IOException;

public class FileLocator {

	public static final String root = "plugins" + File.separator + "QuestX", config_root = root + File.separator + "Configuration", data_root = root + File.separator + "Data",
			npc_data_root = data_root + File.separator + "NPCs", quest_data_root = data_root + File.separator + "Quests", rep_data_root = data_root + File.separator + "Reputation",

			dlgFile = "dialogue.dlg", propertyFile = "properties.txt", taskScript = "task.qxs", questScript = "quest_link.qxs", questRoot = "quest.qxs", dtFile = "death_trigger.qxs",
			customScript = "custom_script.qxs";

	public static File getQuestFile(String name) {
		return new File(quest_data_root + File.separator + name + File.separator + questRoot);
	}

	public static File getQuestProgressionPlayerFile(String qName, String pName) {
		return new File(quest_data_root + File.separator + qName + File.separator + "Progression" + File.separator + pName + ".prog");
	}

	public static File getPlayerRepFile(String name) {
		return new File(rep_data_root + File.separator + name + ".rep");
	}

	public static File getCurrentQuestFile() {
		return new File(quest_data_root + File.separator + "Current.qxs");
	}

	public static File getNPCRootDir(String npcName) {
		return new File(npc_data_root + File.separator + npcName);
	}

	public static File getNPCDlgFile(String npcName) {
		return new File(npc_data_root + File.separator + npcName + File.separator + dlgFile);
	}

	public static boolean doesNPCDlgFileExist(String npcName) {
		return new File(npc_data_root + File.separator + npcName + File.separator + dlgFile).exists();
	}

	public static File getNPCPropertiesFile(String npcName) {
		return new File(npc_data_root + File.separator + npcName + File.separator + propertyFile);
	}

	public static File getNPCTaskFile(String npcName) {
		return new File(npc_data_root + File.separator + npcName + File.separator + taskScript);
	}
	
	public static File getCustomTriggerFile(String npcName){
		return new File(npc_data_root + File.separator + npcName + File.separator + customScript);
	}
	
	public static File getCustomTriggerDefFile(String npcName, String rawFormat){
		String fileName = rawFormat.substring(2, rawFormat.length() - 1);
		return new File(npc_data_root + File.separator + npcName + File.separator + fileName);
	}
	
	public static File getNPCDeathTriggerFile(String npcName){
		return new File(npc_data_root + File.separator + npcName + File.separator + dtFile);
	}

	public static File getNPCQuestLinkFile(String npcName) {
		return new File(npc_data_root + File.separator + npcName + File.separator + questScript);
	}

	public static File getNPCTaskProgressionPlayerFile(String npcName, String playerName) {
		return new File(npc_data_root + File.separator + npcName + File.separator + "Progression" + File.separator + playerName + ".prog");
	}

	public static File getPopDensityDatabase() {
		return new File(data_root + File.separator + "pop_density_data.db");
	}

	public static File getWorldConfig() {
		return new File(config_root + File.separator + "npc_world_config.txt");
	}
	
	public static File getPollFile(){
		return new File(config_root + File.separator + "poll_config.txt");
	}
	
	public static File getGeneralConfig(){
		return new File(config_root + File.separator + "general_config.txt");
	}
	
	public static File getDatabaseConfig(){
		return new File(config_root + File.separator + "sql_logging_config.txt");
	}

	public static void createPlayerNPCProgressionFile(String npcName, String playerName) {
		try {
			new File(npc_data_root + File.separator + npcName + File.separator + "Progression" + File.separator + playerName + ".prog").createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static File getNPCFixedSpawnsFile() {
		return new File(data_root + File.separator + "fixed_spawns.qxs");
	}
	
	public static File getNPCPresetPathingFile() {
		return new File(data_root + File.separator + "preset_pathing.qxs");
	}
	
	public static File getHotspotFile(){
		return new File(data_root + File.separator + "hotspots.qxs");
	}

	public static boolean doesQuestNameExist(String name) {
		for (File f : new File(quest_data_root).listFiles()) {
			if (f.getName().equalsIgnoreCase(name)) {
				return true;
			}
		}
		return false;
	}

	public static boolean doesNPCNameExist(String npcName) {
		return (getNPCRootDir(npcName).exists());
	}

}
