package ca.xvx.tracks.persistence;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import ca.xvx.tracks.TodoContext;

public class SqliteTodoContextDao implements ITodoContextDao {
	
	private SQLiteOpenHelper helper;
	private String[] allColumns = { TracksSQLiteOpenHelper.COLUMN_KEY_ID, 
						   TracksSQLiteOpenHelper.COLUMN_ID,
						   TracksSQLiteOpenHelper.COLUMN_NAME,
						   TracksSQLiteOpenHelper.COLUMN_POSITION,
						   TracksSQLiteOpenHelper.COLUMN_HIDE,
						   TracksSQLiteOpenHelper.COLUMN_OUTDATED};
	
	public SqliteTodoContextDao(SQLiteOpenHelper helper) {
		this.helper = helper;
	}

	@Override
	public TodoContext load(long id) {
		SQLiteDatabase db = helper.getReadableDatabase();
		Cursor cursor = db.query(TracksSQLiteOpenHelper.TABLE_CONTEXTS, allColumns, TracksSQLiteOpenHelper.COLUMN_KEY_ID + " = " + id, null, null, null, null);
		try {
			if (cursor.getCount() == 0)
				throw new IllegalArgumentException("Invalid id='" + id + "'.");
			
			cursor.moveToFirst();
			TodoContext context = cursorToContext(cursor);
			
			return context;
		} finally {
			cursor.close();
			db.close();
		}		
	}
	
	@Override
	public void save(TodoContext context) {
		SQLiteDatabase db = helper.getWritableDatabase();
		
		int rowsAffected = 0;
		if(context.getDbKeyId() > -1)
			rowsAffected = db.update(TracksSQLiteOpenHelper.TABLE_CONTEXTS, valuesFrom(context), TracksSQLiteOpenHelper.COLUMN_KEY_ID + "=?", new String[]{Long.toString(context.getDbKeyId())});
		else if(context.getId() > -1) {
			rowsAffected = db.update(TracksSQLiteOpenHelper.TABLE_CONTEXTS, valuesFrom(context), TracksSQLiteOpenHelper.COLUMN_ID + "=?", new String[]{Integer.toString(context.getId())});
			if(rowsAffected > 0) {
				Cursor cursor = db.query(TracksSQLiteOpenHelper.TABLE_CONTEXTS, allColumns, TracksSQLiteOpenHelper.COLUMN_ID + "=?", new String[]{Integer.toString(context.getId())}, null, null, null);
				try {				
					cursor.moveToFirst();
					context.setDbKeyId(cursorToContext(cursor).getDbKeyId());
				} finally {
					cursor.close();
				}
			}
		}
		
		if(rowsAffected == 0) {
			long id = db.insert(TracksSQLiteOpenHelper.TABLE_CONTEXTS, null, valuesFrom(context));
			context.setDbKeyId(id);
		}		
		
		db.close();
	}
	
	@Override
	public void delete(long id) {
		SQLiteDatabase db = helper.getWritableDatabase();
		
		db.delete(TracksSQLiteOpenHelper.TABLE_CONTEXTS, TracksSQLiteOpenHelper.COLUMN_KEY_ID + "=?", new String[]{Long.toString(id)});
		
		db.close();
	};

	@Override
	public void deleteAll() {
		SQLiteDatabase db = helper.getWritableDatabase();
		db.delete(TracksSQLiteOpenHelper.TABLE_CONTEXTS, null, null);
		db.close();
	}

	@Override
	public List<TodoContext> getAll() {
		SQLiteDatabase db = helper.getReadableDatabase();
		Cursor cursor = db.query(TracksSQLiteOpenHelper.TABLE_CONTEXTS, allColumns, null, null, null, null, null);
		
		List<TodoContext> contexts = contextsFromCursor(cursor);
	    
	    cursor.close();
		db.close();
		return contexts;
	}
	
	@Override
	public TodoContext getById(int id) {
		SQLiteDatabase db = helper.getReadableDatabase();
		Cursor cursor = db.query(TracksSQLiteOpenHelper.TABLE_CONTEXTS, allColumns, TracksSQLiteOpenHelper.COLUMN_ID + "=?", new String[]{Integer.toString(id)}, null, null, null);
		try {
			if (cursor.getCount() == 0)
				throw new IllegalArgumentException("Invalid id='" + id + "'.");
			
			cursor.moveToFirst();
			TodoContext context = cursorToContext(cursor);
			
			return context;
		} finally {
			cursor.close();
			db.close();
		}		
	}
	
	@Override
	public List<Long> deletedFromServer(List<Integer> contextIdsOnServer) {
		List<Long> dbKeyIds = new ArrayList<Long>();
		SQLiteDatabase db = helper.getWritableDatabase();
		try {
			String selection = TracksSQLiteOpenHelper.COLUMN_ID +">-1 AND " + TracksSQLiteOpenHelper.COLUMN_ID + " NOT IN (" + SqliteUtils.selectionArgs(contextIdsOnServer.size()) + ")";
			Cursor cursor = db.query(TracksSQLiteOpenHelper.TABLE_CONTEXTS, new String[]{TracksSQLiteOpenHelper.COLUMN_KEY_ID}, selection, SqliteUtils.toStringArray(contextIdsOnServer), null, null, null);		
			try {
				for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext())
					dbKeyIds.add(cursor.getLong(0));
			} finally {
				cursor.close();
			}
			db.delete(TracksSQLiteOpenHelper.TABLE_CONTEXTS, TracksSQLiteOpenHelper.COLUMN_KEY_ID + " IN ("+SqliteUtils.selectionArgs(dbKeyIds.size())+")", SqliteUtils.toStringArray(dbKeyIds));
		} finally {
			db.close();
		}
		return dbKeyIds;
	}
	
	private List<TodoContext> contextsFromCursor(Cursor cursor) {
		List<TodoContext> contexts = new ArrayList<TodoContext>(cursor.getCount());

	    cursor.moveToFirst();
	    while (!cursor.isAfterLast()) {
	    	TodoContext context = cursorToContext(cursor);
	      contexts.add(context);
	      cursor.moveToNext();
	    }
	    cursor.close();
	    
	    return contexts;
	}
	
	private TodoContext cursorToContext(Cursor cursor) {
		TodoContext context = new TodoContext(cursor.getInt(1),
						cursor.getString(2),
						cursor.getInt(3),
						SqliteUtils.getBoolean(cursor, 4));
		context.setDbKeyId(cursor.getLong(0));
		context.setOutdated(SqliteUtils.getBoolean(cursor, 5));
		return context;
	}
	
	private ContentValues valuesFrom(TodoContext context) {
		ContentValues values = new ContentValues();
		values.put(TracksSQLiteOpenHelper.COLUMN_ID, context.getId());
		values.put(TracksSQLiteOpenHelper.COLUMN_NAME, context.getName());
		values.put(TracksSQLiteOpenHelper.COLUMN_POSITION, context.getPosition());
		values.put(TracksSQLiteOpenHelper.COLUMN_HIDE, context.isHidden());
		values.put(TracksSQLiteOpenHelper.COLUMN_OUTDATED, context.isOutdated());
		return values;
	}

}
