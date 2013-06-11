package ca.xvx.tracks.persistence;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import ca.xvx.tracks.Project;
import ca.xvx.tracks.Task;
import ca.xvx.tracks.TodoContext;

public class SqliteTaskDao implements ITaskDao {
	
	private SQLiteOpenHelper helper;
	private String[] allColumns = { TracksSQLiteOpenHelper.COLUMN_KEY_ID,
						   TracksSQLiteOpenHelper.COLUMN_ID,
						   TracksSQLiteOpenHelper.COLUMN_DESCRIPTION,
						   TracksSQLiteOpenHelper.COLUMN_NOTES,
						   TracksSQLiteOpenHelper.COLUMN_CONTEXT,
						   TracksSQLiteOpenHelper.COLUMN_PROJECT,
						   TracksSQLiteOpenHelper.COLUMN_DUE,
						   TracksSQLiteOpenHelper.COLUMN_SHOW_FROM,
						   TracksSQLiteOpenHelper.COLUMN_DONE,
						   TracksSQLiteOpenHelper.COLUMN_OUTDATED,
						   TracksSQLiteOpenHelper.COLUMN_DELETED};
	private ITodoContextDao contextDao;
	private IProjectDao projectDao;
	
	public SqliteTaskDao(SQLiteOpenHelper helper, ITodoContextDao contextDao, IProjectDao projectDao) {
		this.helper = helper;
		this.contextDao = contextDao;
		this.projectDao = projectDao;
	}

	@Override
	public void save(Task task) {
		SQLiteDatabase db = helper.getWritableDatabase();
		
		int rowsAffected = 0;
		if(task.getDbKeyId() > -1)
			rowsAffected = db.update(TracksSQLiteOpenHelper.TABLE_TASKS, valuesFrom(task), TracksSQLiteOpenHelper.COLUMN_KEY_ID + "=?", new String[]{Long.toString(task.getDbKeyId())});
		else if(task.getId() > -1) {
			rowsAffected = db.update(TracksSQLiteOpenHelper.TABLE_TASKS, valuesFrom(task), TracksSQLiteOpenHelper.COLUMN_ID + "=?", new String[]{Integer.toString(task.getId())});
			if(rowsAffected > 0) {
				Cursor cursor = db.query(TracksSQLiteOpenHelper.TABLE_TASKS, allColumns, TracksSQLiteOpenHelper.COLUMN_ID + "=?", new String[]{Integer.toString(task.getId())}, null, null, null);
				try {				
					cursor.moveToFirst();
					task.setDbKeyId(cursorToTask(cursor).getDbKeyId());
				} finally {
					cursor.close();
				}
			}
		}
		
		if(rowsAffected == 0) {
			long id = db.insert(TracksSQLiteOpenHelper.TABLE_TASKS, null, valuesFrom(task));
			task.setDbKeyId(id);
		}
		db.close();
	}

	@Override
	public Task getById(int id) {
		SQLiteDatabase db = helper.getReadableDatabase();
		Cursor cursor = db.query(TracksSQLiteOpenHelper.TABLE_TASKS, allColumns, TracksSQLiteOpenHelper.COLUMN_ID + " = " + id, null, null, null, null);		
		try {
			if(cursor.getCount() == 0)
				throw new IllegalArgumentException("Invalid id='"+id+"'.");
			cursor.moveToFirst();
			Task task = cursorToTask(cursor);
			return task;
		} finally {		
			cursor.close();
			db.close();
		}
	}
	
	@Override
	public List<Task> getAll() {
		SQLiteDatabase db = helper.getReadableDatabase();
		Cursor cursor = db.query(TracksSQLiteOpenHelper.TABLE_TASKS, allColumns, null, null, null, null, null);
		
		List<Task> tasks = tasksFromCursor(cursor);
	    
	    cursor.close();
		db.close();
		return tasks;
	}
	
	@Override
	public List<Long> deletedFromServer(List<Integer> taskIdsOnServer) {
		List<Long> dbKeyIds = new ArrayList<Long>();
		SQLiteDatabase db = helper.getWritableDatabase();
		try {
			String selection = TracksSQLiteOpenHelper.COLUMN_ID +">-1 AND " + TracksSQLiteOpenHelper.COLUMN_ID + " NOT IN (" + SqliteUtils.selectionArgs(taskIdsOnServer.size()) + ")";
			Cursor cursor = db.query(TracksSQLiteOpenHelper.TABLE_TASKS, new String[]{TracksSQLiteOpenHelper.COLUMN_KEY_ID}, selection, SqliteUtils.toStringArray(taskIdsOnServer), null, null, null);		
			try {
				for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext())
					dbKeyIds.add(cursor.getLong(0));
			} finally {
				cursor.close();
			}
			db.delete(TracksSQLiteOpenHelper.TABLE_TASKS, TracksSQLiteOpenHelper.COLUMN_KEY_ID + " IN ("+SqliteUtils.selectionArgs(dbKeyIds.size())+")", SqliteUtils.toStringArray(dbKeyIds));
		} finally {
			db.close();
		}
		return dbKeyIds;
	}

	@Override
	public void delete(long id) {
		SQLiteDatabase db = helper.getWritableDatabase();
		try {
			db.delete(TracksSQLiteOpenHelper.TABLE_TASKS, TracksSQLiteOpenHelper.COLUMN_KEY_ID + "=" + id, null);
		} finally {
			db.close();
		}
	}
	
	@Override
	public void deleteAll() {
		SQLiteDatabase db = helper.getWritableDatabase();
		try {
			db.delete(TracksSQLiteOpenHelper.TABLE_TASKS, null, null);
		} finally {
			db.close();
		}
	}
	
	private List<Task> tasksFromCursor(Cursor cursor) {
		List<Task> tasks = new ArrayList<Task>(cursor.getCount());

	    cursor.moveToFirst();
	    while (!cursor.isAfterLast()) {
	      Task task = cursorToTask(cursor);
	      tasks.add(task);
	      cursor.moveToNext();
	    }
	    cursor.close();
	    
	    return tasks;
	}
	
	private Task cursorToTask(Cursor cursor) {
		long contextId = cursor.getInt(4);
		TodoContext context = null;
		if(contextId != -1)
			context = contextDao.load(contextId);		
		long projectId = cursor.getInt(5);
		Project project = null;
		if(projectId != -1)
			project = projectDao.load(projectId);
		Task task = new Task(cursor.getInt(1),
						cursor.getString(2),
						cursor.getString(3),
						context,
						project,
						SqliteUtils.getDate(cursor, 6),
						SqliteUtils.getDate(cursor, 7));
		task.setDone(SqliteUtils.getBoolean(cursor, 8));
		task.setDbKeyId(cursor.getLong(0));
		task.setOutdated(SqliteUtils.getBoolean(cursor, 9));
		task.setDeleted(SqliteUtils.getBoolean(cursor, 10));
		return task;
	}
	
	private ContentValues valuesFrom(Task task) {
		ContentValues values = new ContentValues();
		values.put(TracksSQLiteOpenHelper.COLUMN_ID, task.getId());
		values.put(TracksSQLiteOpenHelper.COLUMN_DESCRIPTION, task.getDescription());
		values.put(TracksSQLiteOpenHelper.COLUMN_NOTES, task.getNotes());
		long contextId = task.getContext() != null ? task.getContext().getDbKeyId() : -1;
		long projectId = task.getProject() != null ? task.getProject().getDbKeyId() : -1;
		values.put(TracksSQLiteOpenHelper.COLUMN_CONTEXT, contextId);
		values.put(TracksSQLiteOpenHelper.COLUMN_PROJECT, projectId);
		SqliteUtils.putDate(task.getDue(), values, TracksSQLiteOpenHelper.COLUMN_DUE);
		SqliteUtils.putDate(task.getShowFrom(), values, TracksSQLiteOpenHelper.COLUMN_SHOW_FROM);		
		values.put(TracksSQLiteOpenHelper.COLUMN_DONE, task.getDone());
		values.put(TracksSQLiteOpenHelper.COLUMN_OUTDATED, task.isOutdated());
		values.put(TracksSQLiteOpenHelper.COLUMN_DELETED, task.isDeleted());
		return values;
	}

}
