package ca.xvx.tracks;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.util.Log;

import ca.xvx.tracks.persistence.IHashDao;
import ca.xvx.tracks.persistence.ITodoContextDao;

public class TodoContext implements Comparable<TodoContext> {
	private static final String TAG = "TodoContext";
	
	private long dbKeyId = -1;
	private int _id;
	private String _name;
	private int _position;
	private boolean _hide;
	
	private boolean outdated = false;
	
	// Singleton list of contexts
	private static final Map<Long, TodoContext> CONTEXTS;
	
	private static ITodoContextDao todoContextDao = ITodoContextDao.NULL;
	private static IHashDao hashDao = IHashDao.NULL;
	
	static {
		CONTEXTS = new HashMap<Long, TodoContext>();
	}

	public TodoContext(String name, int position, boolean hide) {
		_id = -1;
		_name = name;
		_position = position;
		_hide = hide;
	}

	public TodoContext(int id, String name, int position, boolean hide) {
		this(name, position, hide);

		_id = id;
	}

	public long getDbKeyId() {
		return dbKeyId;
	}
	
	public void setDbKeyId(long id) {
		long oldId = dbKeyId;
		dbKeyId = id;
		if(oldId < 0) {
			if(!CONTEXTS.containsKey(id)) {
				CONTEXTS.put(id, this);
			}
		}
	}
	
	public int getId() {
		return _id;
	}
	
	public String getName() {
		return _name;
	}
	
	public int getPosition() {
		return _position;
	}
	
	public void setId(int id) {
		_id = id;
	}
	
	public String setName(String name) {
		String on = _name;
		_name = name;
		return on;
	}
	
	public int setPosition(int pos) {
		int op = _position;
		_position = pos;
		return op;
	}

	public boolean isHidden() {
		return _hide;
	}

	public boolean setHidden(boolean hide) {
		boolean oh = _hide;
		_hide = hide;
		return oh;
	}
	
	public boolean isOutdated() {
		return outdated;
	}
	
	public void setOutdated(boolean outdated) {
		this.outdated = outdated;
	}

	@Override
	public String toString() {
		return _name;
	}

	@Override
	public int compareTo(TodoContext c) {
		return _position - c._position;
	}

	// Singleton behavior
	public static TodoContext getContext(long id) {
		return CONTEXTS.get(id);
	}
	
	public static TodoContext getContextById(int id) {
		return todoContextDao.getById(id);
	}

	public static Collection<TodoContext> getAllContexts() {
		return CONTEXTS.values();
	}
	
	public static void loadAllContexts() {
		loadUpdatedContexts();
	}

	public static void save(TodoContext context) {				
		todoContextDao.save(context);
		CONTEXTS.put(context.dbKeyId, context);		
	}
	
	public static void checkConflictAndSave(TodoContext context) {
		Log.v(TAG, "checkConflictAndSave");
		int hash = hash(context);				
		try {	
			int prevHash = hashDao.get(context.getId());			
			if(prevHash != hash) { // Changed on server since last time
				Log.v(TAG, "Changed on server.");
				
				TodoContext contextLocal = todoContextDao.getById(context.getId());
				if(contextLocal != null && contextLocal.isOutdated()) { // Local changed
					Log.v(TAG, "Local changes, conflict.");
					// Duplicate local to resolve conflict
					contextLocal.dbKeyId = -1;
					contextLocal._id = -1;
					save(contextLocal);
					save(context);
				}
				else {
					save(context);
				}
			}
		} catch(IllegalArgumentException e) {
			// No previous hash on DB.
			Log.d(TAG, "No previous hash.", e);
			save(context);
		}		
		hashDao.put(context.getId(), hash);		
	}
	
	public static void saveServerContext(TodoContext context) {
		hashDao.put(context.getId(), hash(context));
	}
	
	public static void deleteContextsNotOnServer(List<Integer> contextIdsOnServer) {
		List<Long> deletedDbKeyIds = todoContextDao.deletedFromServer(contextIdsOnServer);
		for(Long dbKeyId:deletedDbKeyIds)
			CONTEXTS.remove(dbKeyId);
	}
	
	public static void setTodoContextDao(ITodoContextDao dao) {
		todoContextDao = dao;
	}
	
	public static void setHashDao(IHashDao dao) {
		hashDao = dao;
	}
	
	private static void loadUpdatedContexts() {
		List<TodoContext> contexts = todoContextDao.getAll();
	    for(TodoContext context:contexts)
			CONTEXTS.put(context.dbKeyId, context);
	}
	
	private static int hash(TodoContext context) {
		final int MULTIPLIER = 31;
		
		int hash = hashIfNotNull(context._id);
		hash = hash * MULTIPLIER + hashIfNotNull(context._name);
		hash = hash * MULTIPLIER + hashIfNotNull(context._position);
		hash = hash * MULTIPLIER + hashIfNotNull(context._hide);
		
		return hash;
	}
	
	private static int hashIfNotNull(Object object) {
		return (object == null ? 0 : object.hashCode());
	}	

}
