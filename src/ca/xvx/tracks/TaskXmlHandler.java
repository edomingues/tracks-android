package ca.xvx.tracks;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import android.util.Log;

public class TaskXmlHandler extends DefaultHandler {
	private static final String TAG = "TaskXmlHandler";
	
	private int _id;
	private String _description;
	private String _notes;
	private TodoContext _context;
	private Project _project;
	private Date _due;
	private Date _showFrom;
	private boolean _err;

	private final StringBuffer _text;
	private static final DateFormat DATEFORM = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

	private List<Integer> taskIdsOnServer = new LinkedList<Integer>();
	
	public TaskXmlHandler() {
		super();
		_text = new StringBuffer();
	}
	
	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) {
		if(qName.equals("todo")) {
			_id = -1;
			_description = null;
			_notes = null;
			_context = null;
			_project = null;
			_due = null;
			_showFrom = null;
			_err = false;
		} else if(qName.equals("nil-classes")) {
			Task.deleteTasksNotOnServer(Collections.<Integer>emptyList());
		}
		_text.setLength(0);
	}

	@Override
	public void endElement(String uri, String localName, String qName) {
		if(qName.equals("todo")) {
			if(!_err) {
				Task task = new Task(_id, _description, _notes, _context, _project, _due, _showFrom);
				taskIdsOnServer.add(_id);
				Task.checkConflictAndSave(task);
			}
		} else if(qName.equals("id")) {
			_id = Integer.valueOf(_text.toString());
		} else if(qName.equals("description")) {
			_description = _text.toString();
		} else if(qName.equals("notes")) {
			_notes = _text.toString();
		} else if(qName.equals("context-id")) {
			try {
				_context = TodoContext.getContextById(Integer.valueOf(_text.toString()));
			} catch(NumberFormatException e) {
				Log.w(TAG, "Unexpected number format: " + _text.toString(), e);
				_context = null;
				_err = true;
			} catch(IllegalArgumentException e) { // Invalid context id
				Log.w(TAG, "Invalid id: " + _text.toString(), e);
				_context = null;
				_err = true;
			}
		} else if(qName.equals("project-id")) {
			if(_text.length() > 0) {
				try {
					_project = Project.getProjectById(Integer.parseInt(_text.toString()));
				} catch (NumberFormatException e) {
					Log.w(TAG, "Unexpected number format: " + _text.toString(), e);
				} catch(IllegalArgumentException e) { // Invalid context id
					Log.w(TAG, "Invalid id: " + _text.toString(), e);
				}
			}
		} else if(qName.equals("due") && _text.length() > 0) {
			try {
				_due = DATEFORM.parse(_text.toString());
			} catch(ParseException e) {
				Log.w(TAG, "Unexpected date format: " + _text.toString(), e);
			}
		} else if(qName.equals("show-from") && _text.length() > 0) {
			try {
				_showFrom = DATEFORM.parse(_text.toString());
			} catch(ParseException e) {
				Log.w(TAG, "Unexpected date format: " + _text.toString(), e);
			}
		} else if(qName.equals("todos")) {
			Task.deleteTasksNotOnServer(taskIdsOnServer);
		}
	}

	@Override
	public void characters(char[] ch, int start, int length) {
		_text.append(ch, start, length);
	}
}

