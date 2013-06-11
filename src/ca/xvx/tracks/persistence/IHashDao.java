package ca.xvx.tracks.persistence;

public interface IHashDao {
	public int get(int id);
	
	public void put(int id, int hash);
	
	public static final IHashDao NULL = new IHashDao() {

		@Override
		public int get(int id) {
			throw new IllegalStateException("NullObject DAO method invoked.");
		}

		@Override
		public void put(int id, int hash) {
		}
		
	};
}
