package ca.xvx.tracks.persistence;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class SqliteHashDao implements IHashDao {
		
	private SQLiteOpenHelper helper;
	private String tableName;
	private String[] allColumns = { TracksSQLiteOpenHelper.COLUMN_ID,
									TracksSQLiteOpenHelper.COLUMN_HASH};
	
	public SqliteHashDao(SQLiteOpenHelper helper, String tableName) {
		this.helper = helper;
		this.tableName = tableName;
	}
	
	@Override
	public int get(int id) {
		SQLiteDatabase db = helper.getReadableDatabase();
		Cursor cursor = db.query(tableName, allColumns, TracksSQLiteOpenHelper.COLUMN_ID + " = " + id, null, null, null, null);		
		try {
			if(cursor.getCount() == 0)
				throw new IllegalArgumentException("Invalid id='"+id+"'.");
			cursor.moveToFirst();
			return cursor.getInt(1);			
		} finally {		
			cursor.close();
			db.close();
		}
	}

	@Override
	public void put(int id, int hash) {		
		SQLiteDatabase db = helper.getWritableDatabase();
		int rowsAffected = 0;
		ContentValues values = valuesFromHash(id, hash);
		rowsAffected = db.update(tableName, values, TracksSQLiteOpenHelper.COLUMN_ID + "=" + id, null);
		if(rowsAffected == 0) {
			db.insert(tableName, null, values);			
		}
		db.close();
	}
	
	private static ContentValues valuesFromHash(int id, int hash) {
		ContentValues values = new ContentValues();
		values.put(TracksSQLiteOpenHelper.COLUMN_ID, id);
		values.put(TracksSQLiteOpenHelper.COLUMN_HASH, hash);
		return values;
	}

}
