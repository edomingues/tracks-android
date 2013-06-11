package ca.xvx.tracks.persistence;

import android.content.Context;

public class OutdatedTracksSQLiteOpenHelper extends TracksSQLiteOpenHelper {

	private static final int DATABASE_VERSION = 1;
	private static final String DATABASE_NAME = "outdatedTracks";
	private static final int PARENT_DATABASE_VERSION = 1;
	
	public OutdatedTracksSQLiteOpenHelper(Context context) {
		super(context, DATABASE_NAME, DATABASE_VERSION);
	}

}
