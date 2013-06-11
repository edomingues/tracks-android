package ca.xvx.tracks.persistence;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class TracksSQLiteOpenHelper extends SQLiteOpenHelper {

	public static final String TABLE_TASKS = "tasks";
	public static final String COLUMN_KEY_ID = "_ID";
	public static final String COLUMN_ID = "id";
	public static final String COLUMN_DESCRIPTION = "description";
	public static final String COLUMN_NOTES = "notes";
	public static final String COLUMN_CONTEXT = "context";
	public static final String COLUMN_PROJECT = "project";
	public static final String COLUMN_DUE = "due";
	public static final String COLUMN_SHOW_FROM = "showFrom";
	public static final String COLUMN_DONE = "done";
	public static final String COLUMN_DELETED = "deleted";
	
	public static final String TABLE_CONTEXTS = "contexts";
	public static final String COLUMN_NAME = "name";
	public static final String COLUMN_POSITION = "position";
	public static final String COLUMN_HIDE = "hide";
	public static final String COLUMN_OUTDATED = "outdated";
	
	public static final String TABLE_PROJECTS = "projects";
	public static final String COLUMN_STATE = "state";
	
	public static final String TABLE_HASHES_PREFIX = "hashes_";
	public static final String TABLE_HASHES_TASKS = TABLE_HASHES_PREFIX + TABLE_TASKS;
	public static final String TABLE_HASHES_CONTEXTS = TABLE_HASHES_PREFIX + TABLE_CONTEXTS;
	public static final String COLUMN_HASH = "hash";
	
	
	private static final int DATABASE_VERSION = 1;
	private static final String DATABASE_NAME = "tracks";
				
	private static final String TASKS_TABLE_CREATE = "CREATE TABLE " 
			+ TABLE_TASKS + "(" + COLUMN_KEY_ID + " integer primary key,"
								+ COLUMN_ID + " integer," 
								+ COLUMN_DESCRIPTION + " text,"
								+ COLUMN_NOTES + " text,"
								+ COLUMN_CONTEXT + " integer,"
								+ COLUMN_PROJECT + " integer,"
								+ COLUMN_DUE + " integer,"
								+ COLUMN_SHOW_FROM + " integer,"
								+ COLUMN_DONE + " integer,"
								+ COLUMN_OUTDATED + " integer,"
								+ COLUMN_DELETED + " integer);";
	
	private static final String CONTEXTS_TABLE_CREATE = "CREATE TABLE " 
			+ TABLE_CONTEXTS + "(" + COLUMN_KEY_ID + " integer primary key," 
            					   + COLUMN_ID + " integer," 
            					   + COLUMN_NAME + " text,"
								   + COLUMN_POSITION + " integer,"
								   + COLUMN_HIDE + " integer,"
								   + COLUMN_OUTDATED + " integer);";
	
	private static final String PROJECTS_TABLE_CREATE = "CREATE TABLE " 
			+ TABLE_PROJECTS + "(" + COLUMN_KEY_ID + " integer primary key,"
								   + COLUMN_ID + " integer," 
								   + COLUMN_NAME + " text,"
								   + COLUMN_DESCRIPTION + " text,"
								   + COLUMN_POSITION + " integer,"
								   + COLUMN_STATE + " text,"
								   + COLUMN_CONTEXT + " integer,"
								   + COLUMN_OUTDATED + " integer);";
		
	public TracksSQLiteOpenHelper(Context context) {
		this(context, DATABASE_NAME, DATABASE_VERSION);
	}
	
	public TracksSQLiteOpenHelper(Context context, String databaseName, int databaseVersion) {
		super(context, databaseName, null, databaseVersion);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(TASKS_TABLE_CREATE);
		db.execSQL(CONTEXTS_TABLE_CREATE);
		db.execSQL(PROJECTS_TABLE_CREATE);
		db.execSQL(hashTableCreateSql(TABLE_HASHES_TASKS));
		db.execSQL(hashTableCreateSql(TABLE_HASHES_CONTEXTS));
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
	}
	
	private static String hashTableCreateSql(String tableName) {
		return "CREATE TABLE " 
				+ tableName + "(" + COLUMN_ID + " integer,"
				   + COLUMN_HASH + " integer);";
	}

}
