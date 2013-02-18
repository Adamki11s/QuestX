package com.adamki11s.dialogue;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.adamki11s.dialogue.triggers.Trigger;
import com.adamki11s.dialogue.triggers.TriggerType;
import com.adamki11s.display.StaticStrings;
import com.adamki11s.events.ConversationRegister;
import com.adamki11s.exceptions.InvalidDialogueException;
import com.adamki11s.exceptions.MissingTaskPropertyException;
import com.adamki11s.io.FileLocator;
import com.adamki11s.npcs.NPCHandler;
import com.adamki11s.npcs.SimpleNPC;
import com.adamki11s.npcs.tasks.NPCTalkTracker;
import com.adamki11s.npcs.tasks.TaskLoader;
import com.adamki11s.npcs.tasks.TaskManager;
import com.adamki11s.npcs.tasks.TaskRegister;
import com.adamki11s.quests.QuestLoader;
import com.adamki11s.quests.QuestManager;
import com.adamki11s.quests.QuestTask;
import com.adamki11s.questx.QuestX;

public class Conversation {

	ConversationData convoData;
	DialogueSet[] dialogue;
	String currentNode = "1";
	boolean conversing = false, indexSelected = false, parseSuccess = false;;

	final NPCHandler handle = new NPCHandler((JavaPlugin) QuestX.p, null);

	public void respond(String s) {
		int i = 1;
		try {
			i = Integer.parseInt(s);
			this.selectSpeechOption(i);
		} catch (NumberFormatException ex) {
			QuestX.logChat(this.convoData.getPlayer(), "Invalid option! Select an index. Number only. Eg '1'");
		}
	}

	public Conversation(String pName, SimpleNPC npc) {
		this.convoData = new ConversationData(pName, npc);
	}

	public void loadConversation() {
		// Find file
		// Invoke DLG Parser
		DLGParser parse = new DLGParser(this, FileLocator.getNPCDlgFile(this.convoData.getSimpleNpc().getName()), this.convoData.getSimpleNpc().getName());
		try {
			this.dialogue = parse.parse();
			parseSuccess = true;
		} catch (InvalidDialogueException e) {
			parseSuccess = false;
			QuestX.logError("-----REASON-----");
			e.printErrorReason();
			/*
			 * QuestX.logError("-----STACK-----"); e.printStackTrace();
			 * QuestX.logError("-----STACK END-----");
			 */
		}
	}

	public boolean wasParseSuccessful() {
		return this.parseSuccess;
	}

	public void startConversation() {
		Player p = this.getConvoData().getPlayer();
		StringBuilder build = new StringBuilder();

		String npcName = this.convoData.getSimpleNpc().getName();

		boolean taskEnabled = TaskRegister.isTaskEnabled(npcName), questEnabled = this.convoData.getSimpleNpc().doesLinkToQuest();

		QuestX.logChat(p, ChatColor.GREEN + "Conversation started with NPC " + ChatColor.ITALIC + "" + ChatColor.YELLOW + npcName);

		if (taskEnabled) {
			build.append("Task : ");
			if (TaskRegister.hasPlayerCompletedTask(npcName, p.getName())) {
				build.append(ChatColor.GREEN).append("Complete ");
			} else {
				build.append(ChatColor.DARK_RED).append("Incomplete ");
			}
		}

		if (questEnabled) {
			build.append(ChatColor.RESET).append("Quest : ");

			String qName = this.convoData.getSimpleNpc().getQuestName();

			if (QuestManager.hasQuestBeenSetup(qName)) {
				if (!QuestManager.isQuestLoaded(qName)) {
					QuestManager.loadQuest(qName);
				}
				QuestLoader ql = QuestManager.getQuestLoader(qName);
				ql.loadAndCheckPlayerProgress(p.getName());
				if (ql.isQuestComplete(p.getName())) {
					build.append(ChatColor.GREEN).append("Complete");
				} else {
					build.append(ChatColor.DARK_RED).append("Incomplete");
				}
			} else {
				build.append(ChatColor.DARK_RED).append("Not Setup");
			}
		}

		if (build.toString().length() > 5) {
			QuestX.logChat(p, build.toString());
		}
		this.conversing = true;
		this.displaySpeechOptions();
		ConversationRegister.playersConversing.add(this);
	}

	public void endConversation() {
		if (this.conversing) {
			Player p = this.getConvoData().getPlayer();
			if (p != null) {
				QuestX.logChat(p, ChatColor.RED + "Conversation ended.");
			}
			this.conversing = false;
			ConversationRegister.playersConversing.remove(this);
		}
	}

	public boolean isConversing() {
		return this.conversing;
	}

	public void displaySpeechOptions() {
		DialogueSet d = this.getDialogeSetFromNode(currentNode);
		DialogueItem[] items = d.getItems();
		Player p = this.convoData.getPlayer();
		int count = 1;
		QuestX.logChat(p, ChatColor.ITALIC + "" + ChatColor.BLUE + "Speech Options " + ChatColor.RESET + StaticStrings.separator.substring(13));
		for (DialogueItem di : items) {
			if (di.doesPlayerHaveRequiredRepLevel(p.getName())) {
				QuestX.logChat(p, "[#" + count + "] " + di.getSay());
			} else {
				QuestX.logChat(p, "[#" + count + "] Unavailable");
			}
			count += 1;
		}

	}

	public void selectSpeechOption(int index) {
		DialogueSet d = this.getDialogeSetFromNode(currentNode);
		DialogueItem[] items = d.getItems();
		if (index > items.length || index < 1) {
			QuestX.logChat(this.getConvoData().getPlayer(), "Invalid chat option!");
			this.displaySpeechOptions();
			return;
		}
		DialogueItem selected = items[index - 1];
		Player p = this.convoData.getPlayer();
		String npcName = this.convoData.getSimpleNpc().getName();
		if (selected.doesPlayerHaveRequiredRepLevel(p.getName())) {
			Trigger selTrigger = selected.getTrigger();
			QuestX.logChat(p, ChatColor.ITALIC + "" + ChatColor.YELLOW + "Response " + ChatColor.RESET + StaticStrings.separator.substring(8));
			if (selTrigger.getTriggerType() != TriggerType.QUEST) {
				DialogueResponse dr = d.getResponse();
				String response = dr.getResponses()[index - 1];
				QuestX.logChat(p, response);
			}

			this.currentNode = this.currentNode + index;
			if (selTrigger.getTriggerType() == TriggerType.END) {
				this.endConversation();
				return;
			} else if (selTrigger.getTriggerType() == TriggerType.TASK) {
				if (TaskRegister.isTaskEnabled(npcName)) {

					boolean alreadyDone = TaskRegister.hasPlayerCompletedTask(npcName, p.getName());
					if (alreadyDone) {
						QuestX.logChat(p, "You have already completed this task!");
						this.endConversation();
						return;
					}
					if (TaskRegister.doesPlayerHaveTask(p.getName())) {
						QuestX.logChat(p, ChatColor.RED + "You already have a task assigned!");
						QuestX.logChat(p, ChatColor.WHITE + "/questx task cancel" + ChatColor.RED + " to cancel current task.");
						this.endConversation();
						return;
					} else {
						TaskLoader tl = new TaskLoader(FileLocator.getNPCTaskFile(npcName), npcName);
						QuestX.logDebug("Loading task...");
						try {
							tl.load();
							QuestX.logDebug("Task Loaded!");
							TaskManager manage = new TaskManager(p.getName(), tl);
							TaskRegister.registerTask(manage);
							QuestX.logChat(p, ChatColor.ITALIC + tl.getTaskName() + ChatColor.RESET + ChatColor.GREEN + " task started!");
							QuestX.logChat(p, "Task description : " + tl.getTaskDescription());
						} catch (MissingTaskPropertyException e) {
							e.printErrorReason();
							QuestX.logChat(p, "Task failed to load, task file is incorrectly formatted. Check the server log for details.");
						}

						this.endConversation();
						return;
					}

				} else {
					QuestX.logChat(p, ChatColor.RED + "This task is not enabled.");
					this.endConversation();
					return;
				}
			} else if (selTrigger.getTriggerType() == TriggerType.QUEST) {
				QuestX.logDebug("Inside quest code");
				SimpleNPC npc = this.getConvoData().getSimpleNpc();
				if (npc.doesLinkToQuest()) {
					QuestX.logDebug("NPC links to quest = " + npc.getQuestName());
					String qName = npc.getQuestName();

					if (QuestManager.hasQuestBeenSetup(qName)) {

						if (!QuestManager.doesPlayerHaveQuest(p.getName())) {
							// start a quest
							QuestX.logDebug("Player does not have quest!");
							if (!QuestManager.isQuestLoaded(qName)) {
								QuestManager.loadQuest(qName);
							}
							QuestX.logDebug("QUEST LOADED ############");
							QuestLoader ql = QuestManager.getQuestLoader(qName);
							ql.loadAndCheckPlayerProgress(p.getName());
							if (ql.isQuestComplete(p.getName())) {
								QuestX.logChat(p, "You have already completed this quest!");
							} else {
								QuestManager.setCurrentPlayerQuest(p.getName(), qName);
								if (ql.getCurrentQuestNode(p.getName()) == 1) {
									QuestX.logChat(p, ql.getStartText());
								}
								QuestTask t = ql.getPlayerQuestTask(p.getName());
								t.sendWhatIsLeftToDo(p);
							}
							this.endConversation();
						} else {
							QuestX.logDebug("Player has quest!");
							if (npc.doesLinkToQuest()) {

								QuestTask qt = QuestManager.getCurrentQuestTask(p.getName());

								if (qt.isTalkNPC()) {
									QuestLoader ql = QuestManager.getQuestLoader(QuestManager.getCurrentQuestName(p.getName()));
									int curNode = ql.getCurrentQuestNode(p.getName());
									NPCTalkTracker track = (NPCTalkTracker) qt.getData();
									if (npc.getCompleteQuestNodes().contains(curNode)) {
										// npc can complete

										if (track.getNPCName().equalsIgnoreCase(npc.getName())) {
											// correct npc
											track.setTalkedTo();
											ql.incrementTaskProgress(p);
											if (ql.isQuestComplete(p.getName())) {
												QuestX.logChat(p, ql.getEndText());
												QuestManager.removeCurrentPlayerQuest(ql.getName(), p.getName());
											}
										} else {
											QuestX.logChat(p, "You need to speak to '" + track.getNPCName() + "' to complete this part of your quest.");
											// wrong npc
										}

									} else {
										QuestX.logChat(p, "You need to speak to '" + track.getNPCName() + "' to complete this part of your quest.");
									}
								}

							}

							// player already has quest
						}
						this.endConversation();
					} else {
						// quest has not been setup
						QuestX.logChat(p, "This quest has not yet been setup. /q setup " + qName);
						this.endConversation();
					}
				} else {
					QuestX.logDebug("NPC has no link to a quest");
					this.endConversation();
				}
				this.endConversation();
			} else if (selTrigger.getTriggerType() == TriggerType.CUSTOM) {
				this.convoData.getSimpleNpc().invokeCustomActions(p);
				this.endConversation();
			} else if (selTrigger.getTriggerType() == TriggerType.CUSTOM_DEFINED) {
				this.convoData.getSimpleNpc().invokeCustomDefAction(p, selTrigger.getTriggerScript());
				this.endConversation();
				// load and cache custom actions
			} else {
				this.displaySpeechOptions();
			}
		} else {
			QuestX.logChat(p, "You must have at least " + items[index - 1].getRequriedRep().getMinRep() + " reputation.");
		}
	}

	boolean canNPCCompleteQuestNode(String quest, String player) {
		SimpleNPC n = this.getConvoData().getSimpleNpc();
		int currentNode = QuestManager.getQuestLoader(quest).getCurrentQuestNode(player);
		return (n.getCompleteQuestNodes().contains(currentNode));
	}

	DialogueSet getDialogeSetFromNode(String node) {
		for (DialogueSet dSet : dialogue) {
			if (dSet.getDialogueID().equalsIgnoreCase(node)) {
				return dSet;
			}
		}
		return null;
	}

	public ConversationData getConvoData() {
		return this.convoData;
	}

}
