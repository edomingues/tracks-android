package ca.xvx.tracks.persistence;

import java.util.Collections;
import java.util.List;

import ca.xvx.tracks.Project;

public interface IProjectDao {

	public Project load(long id);
	
	void save(Project project);

	List<Project> getAll();
	
	Project getById(int id);

	public List<Long> deletedFromServer(List<Integer> idsOnServer);
	
	void deleteAll();
	
	public static final IProjectDao NULL = new IProjectDao() {

		@Override
		public Project load(long id) {
			throw new IllegalStateException("NullObject DAO method invoked.");
		}
		
		@Override
		public void save(Project project) {
		}

		@Override
		public List<Project> getAll() {
			return Collections.emptyList();
		}
		
		@Override
		public Project getById(int id) {
			throw new IllegalStateException("NullObject DAO method invoked.");
		};

		public java.util.List<Long> deletedFromServer(java.util.List<Integer> idsOnServer) {
			return Collections.emptyList();
		}
		
		@Override
		public void deleteAll() {
		}
		
	};

}
