package ca.xvx.tracks;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import ca.xvx.tracks.persistence.IProjectDao;

public class Project {
	public enum ProjectState { ACTIVE, HIDDEN, COMPLETED };
	
	private long dbKeyId = -1;
	
	private int _id;
	private String _name;
	private String _description;
	private int _position;
	private ProjectState _state;
	private TodoContext _defaultContext;
	
	private boolean outdated = false;
	
	// Singleton list of projects
	private static final Map<Long, Project> PROJECTS;
	
	private static IProjectDao projectDao = IProjectDao.NULL;
	
	static {
		PROJECTS = new HashMap<Long, Project>();
	}

	private Project() {
		_id = -1;
		_name = "<none>";
		_state = ProjectState.ACTIVE;
	}

	public Project(String name, String description, int position, ProjectState state, TodoContext defaultContext) {
		_id = -1;
		_name = name;
		_position = position;
		_state = state;
		_defaultContext = defaultContext;
	}
	
	public Project(int id, String name, String description, int position, ProjectState state, TodoContext defaultContext) {
		this(name, description, position, state, defaultContext);
		
		_id = id;
	}
	
	public long getDbKeyId() {
		return dbKeyId;
	}
	
	public void setDbKeyId(long id) {
		long oldId = dbKeyId;
		
		dbKeyId = id;
		
		if(oldId < 0) {
			if(!PROJECTS.containsKey(id)) {
				PROJECTS.put(id, this);
			}
		}
	}
	
	public int getId() {
		return _id;
	}
	public String getName() {
		return _name;
	}
	public String getDescription() {
		return _description;
	}
	public int getPosition() {
		return _position;
	}
	public ProjectState getState() {
		return _state;
	}
	public TodoContext getDefaultContext() {
		return _defaultContext;
	}
	public void setId(int id) {
		_id = id;		
	}
	public String setName(String name) {
		String on = _name;
		_name = name;
		return on;
	}
	public String setDescription(String description) {
		String od = _description;
		_description = description;
		return od;
	}
	public int setPosition(int position) {
		int op = _position;
		_position = position;
		return op;
	}
	public ProjectState setState(ProjectState state) {
		ProjectState ops = _state;
		_state = state;
		return ops;
	}
	public TodoContext setDefaultContext(TodoContext defaultContext) {
		TodoContext old = _defaultContext;
		_defaultContext = defaultContext;
		return old;
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
	public boolean equals(Object o) {
		if(!(o instanceof Project))
			return false;
		Project other = (Project)o;
		if(this==other)
			return true;
		if(this._id>=0 || other._id>=0)
			return this._id == other._id;
		if(this.dbKeyId>=0 && other.dbKeyId>=0)
			return this.dbKeyId == other.dbKeyId;
		return false;
	}
	
	@Override
	public int hashCode() {
		final int MASK = 0x3fffffff;
		int hash;
		if(_id>=0)
			hash =  _id & MASK;
		else if(dbKeyId>=0)
			hash = 0x40000000 | (((int)dbKeyId) & MASK);
		else
			hash = 0x80000000 | (super.hashCode() & MASK);
		return hash;
	}

	public static Project getProject(long id) {
		return PROJECTS.get(id);
	}
	
	public static Project getProjectById(int id) {
		return projectDao.getById(id);
	}

	public static Collection<Project> getAllProjects() {
		return PROJECTS.values();
	}

	public static Collection<Project> getActiveProjects() {
		Collection<Project> ret = PROJECTS.values();
		Collection<Project> rem = new Vector<Project>();

		for(Project p : ret) {
			if(p.getState() != ProjectState.ACTIVE) {
				rem.add(p);
			}
		}

		ret.removeAll(rem);

		return ret;
	}
	
	public static void loadAllProjects() {
		List<Project> projects = projectDao.getAll();
	    for(Project project:projects)
			PROJECTS.put(project.dbKeyId, project);
	}

	public static void save(Project project) {
		projectDao.save(project);
		PROJECTS.put(project.dbKeyId, project);		
	}
	
	public static void deleteProjectsNotOnServer(List<Integer> idsOnServer) {
		List<Long> deletedDbKeyIds = projectDao.deletedFromServer(idsOnServer);
		for(Long dbKeyId:deletedDbKeyIds)
			PROJECTS.remove(dbKeyId);
	}
	
	public static void setProjectDao(IProjectDao dao) {
		projectDao = dao;
	}

	protected static void clear() {
		PROJECTS.clear();
		projectDao.deleteAll();
		PROJECTS.put(-1l, new Project());
	}
}
