package ca.xvx.tracks;

import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.util.Log;
import ca.xvx.tracks.persistence.IHashDao;
import ca.xvx.tracks.persistence.ITaskDao;

public class Task implements Comparable<Task> {
	private static final String TAG = "Task";
	
	private long dbKeyId = -1;
	private int _id;
	private String _description;
	private String _notes;
	private TodoContext _context;
	private Project _project;
	private Date _due;
	private Date _showFrom;
	private boolean _done;
	private boolean outdated = false;
	private boolean deleted = false;
	private Date completedAt;
	
	private boolean conflict = false;
	
	private static final Map<Long, Task> TASKS = new HashMap<Long, Task>();
	
	private static ITaskDao taskDao = ITaskDao.NULL;
	private static IHashDao hashDao = IHashDao.NULL;
	
	public Task(String desc, String notes, TodoContext context, Project project, Date due, Date showFrom) {
		_id = -1;
		_description = desc;
		_notes = notes;
		_context = context;
		_project = project;
		_due = due;
		_showFrom = showFrom;
	}

	public Task(int id, String desc, String notes, TodoContext context, Project project, Date due, Date showFrom) {
		this(desc, notes, context, project, due, showFrom);

		_id = id;
	}

	public long getDbKeyId() {
		return dbKeyId;
	}
	
	public void setDbKeyId(long id) {
		long tmp = dbKeyId;
		dbKeyId = id;
		if(tmp < 0) {
			if(_showFrom == null) {
				TASKS.put(id, this);
			} else {
				Calendar now = Calendar.getInstance();
				Calendar cmp = Calendar.getInstance();
				long nowm, cmpm;
				cmp.setTime(_showFrom);
				cmp.set(Calendar.HOUR_OF_DAY, 0);
				cmp.set(Calendar.MINUTE, 0);
				cmp.set(Calendar.SECOND, 0);
				cmp.set(Calendar.MILLISECOND, 0);
				now.set(Calendar.HOUR_OF_DAY, 0);
				now.set(Calendar.MINUTE, 0);
				now.set(Calendar.SECOND, 0);
				now.set(Calendar.MILLISECOND, 0);
				nowm = now.getTimeInMillis();
				cmpm = cmp.getTimeInMillis();
				
				if(cmpm <= nowm) {
					TASKS.put(id, this);
				}
			}
		}
	}
	
	public int getId() {
		return _id;
	}

	public String getDescription() {
		return _description;
	}

	public String getNotes() {
		return _notes;
	}

	public TodoContext getContext() {
		return _context;
	}

	public Project getProject() {
		return _project;
	}

	public Date getDue() {
		return _due;
	}

	public Date getShowFrom() {
		return _showFrom;
	}

	public boolean getDone() {
		return _done;
	}

	public int setId(int id) {
		int tmp = _id;
		_id = id;		
		return tmp;
	}

	public String setDescription(String description) {
		String tmp = _description;
		_description = description;
		return tmp;
	}

	public String setNotes(String notes) {
		String tmp = _notes;
		_notes = notes;
		return tmp;
	}

	public TodoContext setContext(TodoContext context) {
		TodoContext tmp = _context;
		_context = context;
		return tmp;
	}

	public Project setProject(Project project) {
		Project tmp = _project;
		_project = project;
		return tmp;
	}

	public Date setDue(Date due) {
		Date tmp = _due;
		_due = due;
		return tmp;
	}

	public Date setShowFrom(Date showFrom) {
		Date tmp = _showFrom;
		_showFrom = showFrom;

		if(_showFrom != null) {
			Calendar now = Calendar.getInstance();
			Calendar cmp = Calendar.getInstance();
			long nowm, cmpm;
			cmp.setTime(_showFrom);
			cmp.set(Calendar.HOUR_OF_DAY, 0);
			cmp.set(Calendar.MINUTE, 0);
			cmp.set(Calendar.SECOND, 0);
			cmp.set(Calendar.MILLISECOND, 0);
			now.set(Calendar.HOUR_OF_DAY, 0);
			now.set(Calendar.MINUTE, 0);
			now.set(Calendar.SECOND, 0);
			now.set(Calendar.MILLISECOND, 0);
			nowm = now.getTimeInMillis();
			cmpm = cmp.getTimeInMillis();
			
			if(cmpm > nowm) {
				if(TASKS.containsKey(dbKeyId)) {
					TASKS.remove(dbKeyId);
				}
			}
		}
		
		return tmp;
	}
	
	public boolean setDone(boolean done) {
		Log.d(TAG, "setDone("+done+")");
		boolean tmp = _done;
		_done = done;
		
		if(done && !tmp)
			completedAt = Calendar.getInstance().getTime();
		
		return tmp;
	}
	
	public boolean isOutdated() {
		return outdated;
	}
	
	public void setOutdated(boolean outdated) {
		this.outdated = outdated;
	}
	
	public boolean isDeleted() {
		return deleted;
	}
	
	public void setDeleted(boolean deleted) {
		this.deleted = deleted;
	}
	
	public boolean isConflict() {
		return conflict;
	}
	
	public Date completedAt() {
		return completedAt;
	}

	public void remove() {
		taskDao.delete(dbKeyId);
		TASKS.remove(dbKeyId);
	}
	
	public static Task getTask(long id) {
		return TASKS.get(id);
	}
	
	public static int getTaskCount() {
		return TASKS.size();
	}

	@Override
	public int compareTo(Task t) {
		if(_due != null) {
			if(t._due != null) {
				return _due.compareTo(t._due);
			} else {
				return -1;
			}
		} else {
			if(t._due != null) {
				return 1;
			} else {
				return t._id - _id;
			}
		}
	}

	public static Collection<Task> getAllTasks() {
		return TASKS.values();
	}
	
	public static void loadAllTasks() {
		List<Task> tasks = taskDao.getAll();
		for(Task task:tasks)
			TASKS.put(task.dbKeyId, task);	
	}

	public static void save(Task task) {			
		if(!deleted(task)) {
			taskDao.save(task);
			TASKS.put(task.dbKeyId, task);
		}
	}
	
	public static void checkConflictAndSave(Task task) {
		Log.v(TAG, "checkConflictAndSave");
		int hash = hash(task);				
		try {	
			int prevHash = hashDao.get(task.getId());			
			if(prevHash != hash) { // Task changed on server since last time
				Log.v(TAG, "Task changed on server.");
				
				Task taskLocal = taskDao.getById(task.getId());
				if(taskLocal != null && taskLocal.isOutdated()) { // Local task changed
					Log.v(TAG, "Local task changes, conflict.");
					// Duplicate local task to resolve conflict
					taskLocal.dbKeyId = -1;
					taskLocal._id = -1;
					save(taskLocal);
					save(task);
				}
				else {
					save(task);
				}
			}
		} catch(IllegalArgumentException e) {
			// No previous hash on DB.
			Log.d(TAG, "no previous hash", e);
			save(task);
		}		
		hashDao.put(task.getId(), hash);		
	}
	
	public static void saveServerTask(Task task) {
		hashDao.put(task.getId(), hash(task));
	}
	
	public static void deleteTasksNotOnServer(List<Integer> taskIdsOnServer) {
		List<Long> deletedDbKeyIds = taskDao.deletedFromServer(taskIdsOnServer);
		for(Long dbKeyId:deletedDbKeyIds)
			TASKS.remove(dbKeyId);
	}
	
	public static void setTaskDao(ITaskDao dao) {
		taskDao = dao;
	}
	
	public static void setHashDao(IHashDao dao) {
		hashDao = dao;
	}
	
	protected static void clear() {
		TASKS.clear();
	}
	
	private static boolean deleted(Task task) {
		boolean deleted = false;
		try {
			deleted = taskDao.getById(task.getId()).isDeleted();
		} catch(IllegalArgumentException e) {
			Log.w(TAG, "Invalid id: " + task.getId(), e);
		}
		return deleted;
	}
	
	private static int hash(Task task) {
		final int MULTIPLIER = 31;
		
		int hash = hashIfNotNull(task._id);
		hash = hash * MULTIPLIER + hashIfNotNull(task._description);
		hash = hash * MULTIPLIER + hashIfNotNull(task._notes);
		hash = hash * MULTIPLIER + (task._context == null ? 0 : task._context.getId());
		hash = hash * MULTIPLIER + (task._project == null ? 0 : task._project.getId());
		hash = hash * MULTIPLIER + hashIfNotNull(task._due);
		hash = hash * MULTIPLIER + hashIfNotNull(task._showFrom);
		hash = hash * MULTIPLIER + hashIfNotNull(task._done);
		
		return hash;
	}
	
	private static int hashIfNotNull(Object object) {
		return (object == null ? 0 : object.hashCode());
	}	
}