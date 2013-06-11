package ca.xvx.tracks.persistence;

import android.content.Context;
import android.database.sqlite.SQLiteOpenHelper;
import ca.xvx.tracks.Project;
import ca.xvx.tracks.Task;
import ca.xvx.tracks.TodoContext;

public class DatabaseManager {
	
	public static void configurePersistence(Context context) {	
		SQLiteOpenHelper helper = new TracksSQLiteOpenHelper(context);
		ITodoContextDao contextDao = new SqliteTodoContextDao(helper);
		IProjectDao projectDao = new SqliteProjectDao(helper, contextDao);
		ITaskDao taskDao = new SqliteTaskDao(helper, contextDao, projectDao);		
		TodoContext.setTodoContextDao(contextDao);
		Project.setProjectDao(projectDao);
		Task.setTaskDao(taskDao);
		Task.setHashDao(new SqliteHashDao(helper, TracksSQLiteOpenHelper.TABLE_HASHES_TASKS));
		TodoContext.setHashDao(new SqliteHashDao(helper, TracksSQLiteOpenHelper.TABLE_HASHES_CONTEXTS));	
	}	
}
