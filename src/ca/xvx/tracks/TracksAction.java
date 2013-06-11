package ca.xvx.tracks;

import android.os.Handler;

public class TracksAction {
	public enum ActionType { FETCH_TASKS, 
							 COMPLETE_TASK, 
							 UPDATE_TASK, 
							 DELETE_TASK, 
							 UPDATE_CONTEXT, 
							 UPDATE_PROJECT,
							 UPLOAD_TASKS};

	ActionType type;
	Object target;
	public Handler notify;

	public TracksAction(ActionType t, Object o, Handler n) {
		type = t;
		target = o;
		notify = n;
	}
}
