package ca.xvx.tracks.persistence;

import java.util.Date;
import java.util.List;

import android.content.ContentValues;
import android.database.Cursor;

public class SqliteUtils {
	
	public static Date getDate(Cursor cursor, int columnIndex) {
		Date date = null;
		if(!cursor.isNull(columnIndex))			
			date = new Date(cursor.getLong(columnIndex));
		return date;
	}
	
	public static boolean getBoolean(Cursor cursor, int columnIndex) {
		return cursor.getInt(columnIndex) != 0;
	}
	
	public static void putDate(Date date, ContentValues values, String key) {
		if(date == null)
			values.putNull(key);
		else
			values.put(key, date.getTime());
	}
	
	public static <T extends Enum<T>> Enum<T> getEnum(Class<T> enumType, Cursor cursor, int columnIndex) {
		Enum<T> rv = null;
		if(!cursor.isNull(columnIndex))
			rv = Enum.valueOf(enumType, cursor.getString(columnIndex));
		return rv;
	}
	
	public static void putEnum(Enum<?> e, ContentValues values, String key) {
		if(e == null)
			values.putNull(key);
		else
			values.put(key, e.name());
	}
	
	public static String selectionArgs(int numberOfArgs) {
		StringBuilder rv = new StringBuilder();
		if(numberOfArgs > 0)
			rv.append("?");
		for(int i=2; i<=numberOfArgs; i++)
			rv.append(",?");
		return rv.toString();
	}
	
	public static <T> String[] toStringArray(List<T> list) {
		String[] rv = new String[list.size()];
		for(int i=0; i<list.size(); i++)
			rv[i] = list.get(i).toString();
		return rv;
	}
}