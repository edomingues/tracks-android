package ca.xvx.tracks.persistence;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import ca.xvx.tracks.Project;
import ca.xvx.tracks.TodoContext;

public class SqliteProjectDao implements IProjectDao {

	private SQLiteOpenHelper helper;
	private String[] allColumns = { TracksSQLiteOpenHelper.COLUMN_KEY_ID, 
						   TracksSQLiteOpenHelper.COLUMN_ID,
						   TracksSQLiteOpenHelper.COLUMN_NAME,
						   TracksSQLiteOpenHelper.COLUMN_DESCRIPTION,
						   TracksSQLiteOpenHelper.COLUMN_POSITION,
						   TracksSQLiteOpenHelper.COLUMN_STATE,
						   TracksSQLiteOpenHelper.COLUMN_CONTEXT,
						   TracksSQLiteOpenHelper.COLUMN_OUTDATED};
	
	private ITodoContextDao contextDao;
	
	public SqliteProjectDao(SQLiteOpenHelper helper, ITodoContextDao contextDao) {
		this.helper = helper;
		this.contextDao = contextDao;
	}
	
	@Override
	public Project load(long id) {
		SQLiteDatabase db = helper.getReadableDatabase();
		Cursor cursor = db.query(TracksSQLiteOpenHelper.TABLE_PROJECTS, allColumns, TracksSQLiteOpenHelper.COLUMN_KEY_ID + " = " + id, null, null, null, null);		
		try {
			if(cursor.getCount() == 0)
				throw new IllegalArgumentException("Invalid id='"+id+"'.");
			cursor.moveToFirst();
			Project project = cursorToProject(cursor);
			return project;
		} finally {		
			cursor.close();
			db.close();
		}
	}
	
	@Override
	public void save(Project project) {
		SQLiteDatabase db = helper.getWritableDatabase();
		
		int rowsAffected = 0;
		if(project.getDbKeyId() > -1)
			rowsAffected = db.update(TracksSQLiteOpenHelper.TABLE_PROJECTS, valuesFrom(project), TracksSQLiteOpenHelper.COLUMN_KEY_ID + "=?", new String[]{Long.toString(project.getDbKeyId())});
		else if(project.getId() > -1) {
			rowsAffected = db.update(TracksSQLiteOpenHelper.TABLE_PROJECTS, valuesFrom(project), TracksSQLiteOpenHelper.COLUMN_ID + "=?", new String[]{Integer.toString(project.getId())});
			if(rowsAffected > 0) {
				Cursor cursor = db.query(TracksSQLiteOpenHelper.TABLE_PROJECTS, allColumns, TracksSQLiteOpenHelper.COLUMN_ID + "=?", new String[]{Integer.toString(project.getId())}, null, null, null);
				try {				
					cursor.moveToFirst();
					project.setDbKeyId(cursorToProject(cursor).getDbKeyId());
				} finally {
					cursor.close();
				}
			}
		}
		
		if(rowsAffected == 0)
		{
			long id = db.insert(TracksSQLiteOpenHelper.TABLE_PROJECTS, null, valuesFrom(project));
			project.setDbKeyId(id);
		}
		
		db.close();
	}

	@Override
	public List<Project> getAll() {
		SQLiteDatabase db = helper.getReadableDatabase();
		Cursor cursor = db.query(TracksSQLiteOpenHelper.TABLE_PROJECTS, allColumns, null, null, null, null, null);
		
		List<Project> projects = projectsFromCursor(cursor);
	    
	    cursor.close();
		db.close();
		return projects;
	}
	
	@Override
	public Project getById(int id) {
		SQLiteDatabase db = helper.getReadableDatabase();
		Cursor cursor = db.query(TracksSQLiteOpenHelper.TABLE_PROJECTS, allColumns, TracksSQLiteOpenHelper.COLUMN_ID + " = " + id, null, null, null, null);		
		try {
			if(cursor.getCount() == 0)
				throw new IllegalArgumentException("Invalid id='"+id+"'.");
			cursor.moveToFirst();
			Project project = cursorToProject(cursor);
			return project;
		} finally {		
			cursor.close();
			db.close();
		}
	}
	
	@Override
	public List<Long> deletedFromServer(List<Integer> idsOnServer) {
		List<Long> dbKeyIds = new ArrayList<Long>();
		SQLiteDatabase db = helper.getWritableDatabase();
		try {
			String selection = TracksSQLiteOpenHelper.COLUMN_ID +">-1 AND " + TracksSQLiteOpenHelper.COLUMN_ID + " NOT IN (" + SqliteUtils.selectionArgs(idsOnServer.size()) + ")";
			Cursor cursor = db.query(TracksSQLiteOpenHelper.TABLE_PROJECTS, new String[]{TracksSQLiteOpenHelper.COLUMN_KEY_ID}, selection, SqliteUtils.toStringArray(idsOnServer), null, null, null);		
			try {
				for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext())
					dbKeyIds.add(cursor.getLong(0));
			} finally {
				cursor.close();
			}
			db.delete(TracksSQLiteOpenHelper.TABLE_PROJECTS, TracksSQLiteOpenHelper.COLUMN_KEY_ID + " IN ("+SqliteUtils.selectionArgs(dbKeyIds.size())+")", SqliteUtils.toStringArray(dbKeyIds));
		} finally {
			db.close();
		}
		return dbKeyIds;
	}

	@Override
	public void deleteAll() {
		SQLiteDatabase db = helper.getWritableDatabase();
		db.delete(TracksSQLiteOpenHelper.TABLE_PROJECTS, null, null);
		db.close();
	}
	
	private List<Project> projectsFromCursor(Cursor cursor) {
		List<Project> projects = new ArrayList<Project>(cursor.getCount());

	    cursor.moveToFirst();
	    while (!cursor.isAfterLast()) {
	      Project project = cursorToProject(cursor);
	      projects.add(project);
	      cursor.moveToNext();
	    }
	    cursor.close();
	    
	    return projects;
	}
	
	private Project cursorToProject(Cursor cursor) {
		long contextId = cursor.getInt(6);
		TodoContext context = null;
		if(contextId != -1) 
			context = contextDao.load(contextId);
		
		Project project = new Project(cursor.getInt(1),
						cursor.getString(2),
						cursor.getString(3),
						cursor.getInt(4),
						Project.ProjectState.valueOf(cursor.getString(5)),						
						context);
		project.setDbKeyId(cursor.getLong(0));
		project.setOutdated(SqliteUtils.getBoolean(cursor, 7));
		return project;
	}
	
	private ContentValues valuesFrom(Project project) {
		ContentValues values = new ContentValues();
		values.put(TracksSQLiteOpenHelper.COLUMN_ID, project.getId());
		values.put(TracksSQLiteOpenHelper.COLUMN_NAME, project.getName());
		values.put(TracksSQLiteOpenHelper.COLUMN_DESCRIPTION, project.getDescription());
		values.put(TracksSQLiteOpenHelper.COLUMN_POSITION, project.getPosition());
		values.put(TracksSQLiteOpenHelper.COLUMN_STATE, project.getState().name());
		long contextId = project.getDefaultContext() != null ? project.getDefaultContext().getDbKeyId() : -1;
		values.put(TracksSQLiteOpenHelper.COLUMN_CONTEXT, contextId);
		values.put(TracksSQLiteOpenHelper.COLUMN_OUTDATED, project.isOutdated());
		return values;
	}

}
