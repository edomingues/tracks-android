package ca.xvx.tracks;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import android.util.Log;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

public class ProjectXmlHandler extends DefaultHandler {
	private static final String TAG = "ProjectXmlHandler";
	
	private int _id;
	private String _name;
	private String _description;
	private int _position;
	private Project.ProjectState _state;
	private TodoContext _defaultContext;

	private final StringBuffer _text;

	private List<Integer> idsOnServer = new LinkedList<Integer>();
	
	public ProjectXmlHandler() {
		super();
		_text = new StringBuffer();
	}
	
	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) {
		if(qName.equals("project")) {
			_id = -1;
			_name = null;
			_state = Project.ProjectState.ACTIVE;
			_position = -1;
			_defaultContext = null;
		} else if(qName.equals("nil-classes")) {
			Project.deleteProjectsNotOnServer(Collections.<Integer>emptyList());
		}
		_text.setLength(0);
	}

	@Override
	public void endElement(String uri, String localName, String qName) {
		if(qName.equals("project")) {
			Project.save(new Project(_id, _name, _description, _position, _state, _defaultContext));
			idsOnServer.add(_id);
		} else if(qName.equals("id")) {
			_id = Integer.valueOf(_text.toString());
		} else if(qName.equals("name")) {
			_name = _text.toString();
		} else if(qName.equals("description")) {
			_description = _text.toString();
		} else if(qName.equals("state")) {
			String s = _text.toString();
			if(s.equals("active")) {
				_state = Project.ProjectState.ACTIVE;
			} else if(s.equals("completed")) {
				_state = Project.ProjectState.COMPLETED;
			} else if(s.equals("hidden")) {
				_state = Project.ProjectState.HIDDEN;
			}
		} else if(qName.equals("position")) {
			_position = Integer.valueOf(_text.toString());
		} else if(qName.equals("default-context-id") && _text.length() > 0) {
			try {
				_defaultContext = TodoContext.getContextById(Integer.valueOf(_text.toString()));
			} catch(NumberFormatException e) {
				Log.w(TAG, "Unexpected number format: " + _text.toString(), e);
				_defaultContext = null;
			} catch(IllegalArgumentException e) { // Invalid context id
				Log.w(TAG, "Invalid id: " + _text.toString(), e);
				_defaultContext = null;
			}
		} else if(qName.equals("projects")) {
			Project.deleteProjectsNotOnServer(idsOnServer);
		}
	}

	@Override
	public void characters(char[] ch, int start, int length) {
		_text.append(ch, start, length);
	}
}

