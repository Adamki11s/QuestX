package com.adamki11s.dialogue.triggers;

import java.io.File;

import com.adamki11s.questx.QuestX;

public class CustomTrigger extends Trigger {

	public CustomTrigger(TriggerType type, File triggerScript) {
		
		super(type, triggerScript);
		QuestX.logMSG("Custom type = " + type.toString() + ", file = " + triggerScript.getName());
		// TODO Auto-generated constructor stub
	}

}
